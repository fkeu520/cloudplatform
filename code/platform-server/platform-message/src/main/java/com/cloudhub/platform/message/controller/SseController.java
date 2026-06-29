package com.cloudhub.platform.message.controller;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.message.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE 推送", description = "服务端事件推送（替代轮询）")
@RequiredArgsConstructor
@RestController
@RequestMapping("/message/sse")
public class SseController {

    private final SseService sseService;

    @Operation(summary = "订阅消息推送")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                                @RequestParam(value = "userId", required = false) String paramUserId,
                                HttpServletRequest request) {
        // WF2-2: 先确定当前用户 (从 JWT 或 TenantContext), 后续任何来源的 userId 必须与之匹配
        Long currentUserId = TenantContextHolder.getUserId();
        if (currentUserId == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String uid = JwtUtil.getUserId(auth.substring(7));
                if (uid != null) {
                    try {
                        currentUserId = Long.parseLong(uid);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (currentUserId == null) {
            throw new com.cloudhub.platform.common.exception.BizException("缺少用户身份, 无法订阅消息");
        }

        Long userId = null;
        // 优先从查询参数获取（EventSource 不支持自定义 Header）
        if (paramUserId != null && !paramUserId.isBlank()) {
            try {
                userId = Long.parseLong(paramUserId);
            } catch (NumberFormatException ignored) {}
            // WF2-2: 仅当已有用户身份时才校验一致, 无身份时信任 query param
            // (EventSource 不能发 Authorization 头, gateway 负责保护 /api/message 路径)
            if (userId != null && currentUserId != null && !userId.equals(currentUserId)) {
                throw new com.cloudhub.platform.common.exception.BizException("无权订阅其他用户的消息");
            }
        }
        // 其次从网关注入的 Header 获取 (gateway 已校验, 可信)
        if (userId == null && headerUserId != null && !headerUserId.isBlank()) {
            try {
                userId = Long.parseLong(headerUserId);
            } catch (NumberFormatException ignored) {}
            if (userId != null && !userId.equals(currentUserId)) {
                throw new com.cloudhub.platform.common.exception.BizException("无权订阅其他用户的消息");
            }
        }
        // 最终使用当前用户
        if (userId == null) {
            userId = currentUserId;
        }
        return sseService.subscribe(userId);
    }
}
