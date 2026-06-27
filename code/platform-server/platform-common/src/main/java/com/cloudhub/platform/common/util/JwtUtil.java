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

    /** 盐值（从环境变量读取，fallback 为开发默认值） */
    private static final String SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : System.getProperty("jwt.secret", "cloudhub-platform-secret-key-2024-change-in-production");

    /** HS256 密钥（至少 256 位） */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    static {
        // C6: 启动时检测是否使用了硬编码默认密钥
        if (System.getenv("JWT_SECRET") == null && System.getProperty("jwt.secret") == null) {
            throw new IllegalStateException(
                    "JWT_SECRET not configured! Set env var JWT_SECRET or system property jwt.secret. " +
                    "Using the hardcoded default key is a security risk — anyone with source code can forge tokens."
            );
        }
    }

    /**
     * JWT 载荷记录 — 封装 Token 中的全部自定义字段
     */
    public record JwtClaims(String userId, String username, Long tenantId, Integer userType) {}

    /**
     * 解析 Token 并返回全部载荷字段（只调用一次 parse）
     *
     * @param token JWT Token
     * @return JwtClaims 记录
     */
    public static JwtClaims getAll(String token) {
        Claims claims = parse(token);
        return new JwtClaims(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("tenantId", Long.class),
                claims.get("userType", Integer.class)
        );
    }

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
     * 生成 Token（含用户名、租户ID、用户类型）
     */
    public static String generate(String subject, String username, Long tenantId, Integer userType, long expireSec) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        if (username != null) {
            claims.put("username", username);
        }
        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }
        if (userType != null) {
            claims.put("userType", userType);
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
     * 生成 Token（含用户名、租户ID）- 兼容旧调用
     */
    public static String generate(String subject, String username, Long tenantId, long expireSec) {
        return generate(subject, username, tenantId, null, expireSec);
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
        try {
            if (token == null || token.isBlank()) return null;
            return parse(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 获取用户类型
     */
    public static Integer getUserType(String token) {
        try {
            if (token == null || token.isBlank()) return null;
            return parse(token).get("userType", Integer.class);
        } catch (Exception e) {
            return null;
        }
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