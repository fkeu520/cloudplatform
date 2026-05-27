package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.ops.domain.entity.LoginLog;
import com.cloudhub.platform.ops.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Tag(name = "认证代理", description = "透传登录请求到认证服务")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthProxyController {

    private final RestTemplate restTemplate;
    private final LoginLogService loginLogService;

    @Value("${auth.service.url:http://platform-auth:8082}")
    private String authServiceUrl;

    @SuppressWarnings("unchecked")
    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String username = params.getOrDefault("username", "");
        try {
            Map<String, Object> result = restTemplate.postForObject(authServiceUrl + "/auth/login", params, Map.class);
            boolean success = result != null && (Integer) result.getOrDefault("code", 500) == 200;
            saveLoginLog(username, success ? 1 : 0, success ? "登录成功" : "认证失败", request);
            return result;
        } catch (Exception e) {
            log.warn("登录代理失败: {}", e.getMessage());
            saveLoginLog(username, 0, "认证服务不可用", request);
            return Map.of("code", 503, "message", "认证服务不可用: " + e.getMessage());
        }
    }

    private void saveLoginLog(String username, Integer status, String message, HttpServletRequest request) {
        try {
            LoginLog log = new LoginLog();
            log.setUsername(username);
            log.setUserType(2);
            log.setTenantId(0L);
            log.setLoginType(0);
            log.setIp(getIpAddress(request));
            log.setStatus(status);
            log.setMessage(message);
            log.setLoginTime(LocalDateTime.now());
            loginLogService.save(log);
        } catch (Exception e) {
            log.warn("保存登录日志失败", e);
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip;
    }
}
