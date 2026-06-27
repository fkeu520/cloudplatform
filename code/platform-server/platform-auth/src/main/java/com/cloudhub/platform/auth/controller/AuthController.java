package com.cloudhub.platform.auth.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.common.util.RsaUtil;
import com.cloudhub.platform.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证中心", description = "登录/登出/Token管理/短信验证码")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

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
}