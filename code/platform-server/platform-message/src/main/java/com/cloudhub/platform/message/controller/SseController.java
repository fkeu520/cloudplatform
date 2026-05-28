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
    public SseEmitter subscribe(@RequestParam(value = "userId", required = false) Long userId,
                                HttpServletRequest request) {
        if (userId == null) {
            userId = TenantContextHolder.getUserId();
        }
        if (userId == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String uid = JwtUtil.getUserId(auth.substring(7));
                if (uid != null) userId = Long.parseLong(uid);
            }
        }
        if (userId == null) userId = 0L;
        return sseService.subscribe(userId);
    }
}
