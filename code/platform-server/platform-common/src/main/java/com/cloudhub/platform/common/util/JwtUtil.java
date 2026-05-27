package com.cloudhub.platform.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
public class JwtUtil {

    /** 盐值（生产环境从配置中心读取） */
    private static final String SECRET = "cloudhub-platform-secret-key-2024";

    /** HS256 密钥 */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成 Token
     *
     * @param subject   用户ID
     * @param expireSec 过期秒数
     * @return JWT Token
     */
    public static String generate(String subject, long expireSec) {
        return generate(subject, null, expireSec);
    }

    /**
     * 生成 Token（含扩展信息）
     *
     * @param subject   用户ID
     * @param username  用户名（可选）
     * @param expireSec 过期秒数
     * @return JWT Token
     */
    public static String generate(String subject, String username, long expireSec) {
        return generate(subject, username, null, expireSec);
    }

    /**
     * 生成 Token（含用户名、租户ID）
     */
    public static String generate(String subject, String username, Long tenantId, long expireSec) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        if (username != null) {
            claims.put("username", username);
        }
        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireSec * 1000))
                .signWith(KEY)
                .compact();
    }

    /**
     * 从 Token 获取租户ID
     */
    public static Long getTenantId(String token) {
        try {
            if (token == null || token.isBlank()) return null;
            return parse(token).get("tenantId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true=有效
     */
    public static boolean validate(String token) {
        try {
            Claims claims = parse(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public static String getUserId(String token) {
        return parse(token).getSubject();
    }

    /**
     * 从 Token 获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public static String getUsername(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            Claims claims = parse(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.warn("获取用户名失败: {}", e.getMessage());
            return null;
        }
    }
}