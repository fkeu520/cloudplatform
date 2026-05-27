package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Tag(name = "认证代理", description = "透传登录请求到认证服务")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthProxyController {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://platform-auth:8082}")
    private String authServiceUrl;

    @SuppressWarnings("unchecked")
    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        try {
            return restTemplate.postForObject(authServiceUrl + "/auth/login", params, Map.class);
        } catch (Exception e) {
            log.warn("登录代理失败: {}", e.getMessage());
            return Map.of("code", 503, "message", "认证服务不可用: " + e.getMessage());
        }
    }
}
