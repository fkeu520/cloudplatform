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
        Long userId = null;
        // 优先从查询参数获取（EventSource 不支持自定义 Header）
        if (paramUserId != null && !paramUserId.isBlank()) {
            try {
                userId = Long.parseLong(paramUserId);
            } catch (NumberFormatException ignored) {}
        }
        // 其次从网关注入的 Header 获取
        if (userId == null && headerUserId != null && !headerUserId.isBlank()) {
            try {
                userId = Long.parseLong(headerUserId);
            } catch (NumberFormatException ignored) {}
        }
        // 其次从 TenantContext 获取
        if (userId == null) {
            userId = TenantContextHolder.getUserId();
        }
        // 最后从 JWT Token 解析
        if (userId == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String uid = JwtUtil.getUserId(auth.substring(7));
                if (uid != null) {
                    try {
                        userId = Long.parseLong(uid);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (userId == null) {
            throw new com.cloudhub.platform.common.exception.BizException("缺少用户标识，无法订阅消息");
        }
        return sseService.subscribe(userId);
    }
}
