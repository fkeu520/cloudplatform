package com.cloudhub.platform.auth.controller;

import com.cloudhub.platform.auth.service.AuthService;
import com.cloudhub.platform.auth.service.StepUpTokenService;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.common.util.RsaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "认证中心", description = "登录/登出/Token管理/短信验证码/Step-up二次鉴权")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final StepUpTokenService stepUpTokenService;

    /**
     * AU2-7: 剥除 "Bearer " 前缀, 返回纯 JWT
     */
    private String stripBearer(String header) {
        if (header == null) return null;
        String h = header.trim();
        if (h.startsWith(BEARER_PREFIX)) return h.substring(BEARER_PREFIX.length());
        return h;
    }

    @Operation(summary = "获取RSA公钥（前端加密密码用）")
    @GetMapping("/public-key")
    public Result<Map<String, String>> publicKey() {
        return Result.ok(Map.of("publicKey", RsaUtil.getPublicKey()));
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        return Result.ok(authService.loginByPassword(username, password));
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<?> sendSms(@RequestBody Map<String, String> params) {
        String mobile = params.get("mobile");
        authService.sendSmsCode(mobile);
        return Result.ok("验证码已发送");
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/sms/login")
    public Result<?> smsLogin(@RequestBody Map<String, String> params) {
        String mobile = params.get("mobile");
        String code = params.get("code");
        return Result.ok(authService.loginBySms(mobile, code));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<?> refresh(@RequestHeader(value = "Authorization", required = false) String token) {
        return Result.ok(authService.refreshToken(stripBearer(token)));
    }

    @Operation(summary = "验证Token有效性")
    @GetMapping("/validate")
    public Result<?> validate(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.validateToken(stripBearer(token));
        return Result.ok("Token有效");
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(stripBearer(token));
        return Result.ok();
    }

    // ========== v8 P0-3 Step-up 二次鉴权 (高敏操作前) ==========

    /**
     * 签发 step-up token
     * <p>需重新输密码 (强鉴权) → 颁发 5 分钟短期 token, 绑定 IP/UA.</p>
     * <p>配套: doc/plan/v8-P0-tenant-protection-plan.md ADR-008</p>
     */
    @Operation(summary = "签发 step-up token (高敏操作前的二次鉴权)")
    @PostMapping("/step-up/issue")
    public Result<Map<String, Object>> issueStepUp(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {
        // 1. 从 JWT 解析 userId + tenantId
        String token = stripBearer(authHeader);
        if (token == null) {
            throw new BizException("未登录");
        }
        Long userId;
        Long tenantId;
        try {
            JwtUtil.JwtClaims claims = JwtUtil.getAll(token);
            userId = Long.parseLong(claims.userId());
            tenantId = claims.tenantId();
        } catch (Exception e) {
            log.warn("Step-up JWT 解析失败: {}", e.getMessage());
            throw new BizException("Token 无效");
        }

        // 2. 取 body 参数
        String encryptedPassword = (String) body.get("password");
        String scope = (String) body.get("scope");
        Boolean singleUse = body.get("singleUse") == null
                ? Boolean.TRUE : (Boolean) body.get("singleUse");
        if (scope == null || scope.isBlank()) {
            throw new BizException("scope 不能为空");
        }

        // 3. Controller 负责 RSA 解密 (Service 只接明文)
        String plainPassword;
        try {
            plainPassword = RsaUtil.decrypt(encryptedPassword);
        } catch (Exception e) {
            log.warn("Step-up RSA 解密失败: userId={}, err={}", userId, e.getMessage());
            throw new BizException("密码格式错误");
        }

        // 4. 调 Service 签发 (Service 内部会再调 user service 验密码)
        StepUpTokenService.IssueResult r = stepUpTokenService.issue(
                userId, tenantId, scope, singleUse, plainPassword,
                getClientIp(req), req.getHeader("User-Agent"));

        Map<String, Object> resp = new HashMap<>();
        resp.put("stepUpToken", r.token());
        resp.put("expiresAt", r.expiresAt().toString());
        resp.put("singleUse", r.singleUse());
        resp.put("scope", r.scope());
        return Result.ok(resp);
    }

    /**
     * 撤销 step-up token (改密码 / 主动踢下时调用)
     */
    @Operation(summary = "撤销当前用户所有未使用的 step-up token")
    @PostMapping("/step-up/revoke")
    public Result<Map<String, Object>> revokeStepUp(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, Object> body) {
        String token = stripBearer(authHeader);
        if (token == null) {
            throw new BizException("未登录");
        }
        Long userId;
        try {
            userId = Long.parseLong(JwtUtil.getUserId(token));
        } catch (Exception e) {
            throw new BizException("Token 无效");
        }
        String reason = body == null ? "用户主动撤销"
                : (String) body.getOrDefault("reason", "用户主动撤销");
        int count = stepUpTokenService.revokeAllUnused(userId, reason);
        Map<String, Object> resp = new HashMap<>();
        resp.put("revokedCount", count);
        return Result.ok(resp);
    }

    /**
     * 解析客户端 IP (X-Forwarded-For > X-Real-IP > remoteAddr)
     */
    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int commaIdx = ip.indexOf(',');
            return commaIdx > 0 ? ip.substring(0, commaIdx).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) return ip;
        return req.getRemoteAddr();
    }
}