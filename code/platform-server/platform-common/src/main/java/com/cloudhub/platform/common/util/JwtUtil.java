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
     * <p>permissions 字段 (Phase 1F Step 4): 登录时从 sys_role_menu + sys_user_menu 合并,
     * 静态嵌入 JWT claims, 业务服务通过 {@code JwtUtil.getAll(token).permissions()} 直接获取,
     * 无需跨服务 HTTP 调用 + Redis 缓存
     * (热更新走 Redis 黑名单 — 角色变化时把旧 token 加入 blacklist map, ParkAuthFilter 校验).</p>
     */
    public record JwtClaims(String userId, String username, Long tenantId, Integer userType, java.util.List<String> permissions) {}

    /**
     * 解析 Token 并返回全部载荷字段（只调用一次 parse）
     *
     * @param token JWT Token
     * @return JwtClaims 记录
     */
    public static JwtClaims getAll(String token) {
        Claims claims = parse(token);
        @SuppressWarnings("unchecked")
        java.util.List<String> perms = claims.get("permissions", java.util.List.class);
        return new JwtClaims(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("tenantId", Long.class),
                claims.get("userType", Integer.class),
                perms == null ? java.util.List.of() : perms
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
     * 生成 Token（含用户名、租户ID、用户类型 + 权限列表）
     * <p>主入口 — Phase 1F Step 4 修复后, 调用方 (UserService.login) 传入合并后的 permissions,
     * 业务服务 JwtAuthFilter 会自动写入 X-User-Permissions header 供 ParkAuthFilter 消费.</p>
     *
     * @param subject     用户ID
     * @param username    用户名
     * @param tenantId    租户ID
     * @param userType    用户类型 (0=普通 1=租户管理员 2=运营管理员)
     * @param permissions 合并后的权限列表 (sys_role_menu + sys_user_menu 去重), 允许 null
     * @param expireSec   过期秒数
     */
    public static String generate(String subject, String username, Long tenantId, Integer userType,
                                   java.util.List<String> permissions, long expireSec) {
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
        if (permissions != null && !permissions.isEmpty()) {
            claims.put("permissions", permissions);
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
     * 生成 Token 兼容重载 — Phase 1F Step 4 之前的老调用 (AppControllerTest 等)
     * <p>不嵌 permissions, 业务端 LoginUser.permissions 为空集 (切面会拒绝所有 @RequiresPermissions).
     * 新代码应该用 {@link #generate(String, String, Long, Integer, java.util.List, long)}.</p>
     */
    public static String generate(String subject, String username, Long tenantId, Integer userType, long expireSec) {
        return generate(subject, username, tenantId, userType, java.util.List.of(), expireSec);
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