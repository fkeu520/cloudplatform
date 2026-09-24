package com.cloudhub.platform.gateway.filter;

import com.cloudhub.platform.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权过滤器
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /** P0 security fix (2026-09-24): internal token contract between gateway and kefu.
     *  Gateway computes HMAC-SHA256(this_secret, "kefu-internal") and attaches it as
     *  X-Kefu-Internal-Token on every request forwarded to Kefu routes.
     *  KeFu middleware rejects X-User-* headers unless this token matches.
     *  Env var: KEFU_INTERNAL_TOKEN (must match platform-kefu's KEFU_INTERNAL_TOKEN). */
    private static final String KEFU_INTERNAL_TOKEN_SECRET =
            System.getenv("KEFU_INTERNAL_TOKEN") != null
                    ? System.getenv("KEFU_INTERNAL_TOKEN")
                    : "";

    private static final String KEFU_INTERNAL_HMAC_PAYLOAD = "kefu-internal";

    /** 无需鉴权的路径 (精确匹配, 需要前缀通配的用  pattern + "/" 后缀) */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/sms/send",
            "/auth/sms/login",
            "/auth/refresh",
            "/auth/public-key",
            "/message/sse",
            "/user/login",
            "/user/register",
            "/user/internal",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("[JwtAuth] path={}, method={}", path, exchange.getRequest().getMethod());

        // 白名单直接放行
        if (isWhitePath(path)) {
            log.debug("[JwtAuth] 白名单放行: {}", path);
            return chain.filter(exchange);
        }

        log.debug("[JwtAuth] 需要鉴权: {}", path);
        // 获取 Token
        String token = getToken(exchange.getRequest());
        if (token == null) {
            log.warn("[JwtAuth] 无Token, path={}", path);
            return unauthorized(exchange, "未登录，请先登录");
        }

        // 验证 Token
        try {
            if (!JwtUtil.validate(token)) {
                log.warn("[JwtAuth] Token无效, path={}", path);
                return unauthorized(exchange, "Token无效或已过期");
            }
            JwtUtil.JwtClaims claims = JwtUtil.getAll(token);
            String userId = claims.userId();
            String username = claims.username();
            Long tenantId = claims.tenantId();
            Integer userType = claims.userType();
            log.debug("[JwtAuth] Token有效, userId={}, username={}, tenantId={}, userType={}",
                    userId, username, tenantId, userType);

            // Phase 1F Step 4: 通过 JwtUtil.getAll() 解析嵌入的权限列表 (之前用 claims.get() 错误)
            JwtUtil.JwtClaims jwtClaims = JwtUtil.getAll(token);
            java.util.List<String> permsList = jwtClaims.permissions();
            String permsHeader = permsList == null ? "" : String.join(",", permsList);
            log.debug("[JwtAuth] 写入 X-User-Permissions count={}", permsList == null ? 0 : permsList.size());

            // 将用户上下文传递到后续服务（通过 Header）
            // 业务服务侧可通过 park-common 的 ParkAuthFilter 读取并写入 LoginContextHolder
            ServerHttpRequest.Builder mutate = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Name", username == null ? "" : username)
                    .header("X-Tenant-Id", tenantId == null ? "" : String.valueOf(tenantId))
                    .header("X-User-Type", userType == null ? "" : String.valueOf(userType))
                    .header("X-User-Permissions", permsHeader);

            // P0 security: scope X-Kefu-Internal-Token to kefu paths only
            if (isKefuPath(path)) {
                String kefuToken = computeKefuInternalToken();
                if (kefuToken != null) {
                    log.debug("[JwtAuth] attaching X-Kefu-Internal-Token for kefu path: {}", path);
                    mutate.header("X-Kefu-Internal-Token", kefuToken);
                }
            }

            ServerHttpRequest mutated = mutate.build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return unauthorized(exchange, "Token验证失败");
        }
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }

    /**
     * 判断路径是否在白名单中
     * <p>精确匹配: path == pattern
     * 前缀匹配: pattern 为 "/user/internal" 时匹配 "/user/internal" 和 "/user/internal/xxx",
     * 但不会误匹配 "/user/internal-admin".</p>
     */
    private boolean isWhitePath(String path) {
        return WHITE_LIST.stream().anyMatch(pattern ->
                path.equals(pattern) || path.startsWith(pattern + "/"));
    }

    private String getToken(org.springframework.http.server.reactive.ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String auth = headers.get(0);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        // P0 修复: 必须返回 setComplete() 的 Mono 才能把 401 状态码写入响应,
        // 之前 return Mono.empty() 会导致 NettyWriteResponseFilter 兜底写 200 空响应,
        // 前端拿到 HTTP 200 + Content-Length: 0 无法识别未鉴权状态。
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /** P0 security: check if path belongs to Kefu service (scope internal token header). */
    private boolean isKefuPath(String path) {
        return "/api/kefu".equals(path)
                || "/kefu".equals(path)
                || path.startsWith("/api/kefu/")
                || path.startsWith("/kefu/");
    }

    /** P0 security: compute HMAC-SHA256(KEFU_INTERNAL_TOKEN_SECRET, "kefu-internal").
     *  Returns null if secret is not configured (token header omitted for those routes). */
    private String computeKefuInternalToken() {
        if (KEFU_INTERNAL_TOKEN_SECRET == null || KEFU_INTERNAL_TOKEN_SECRET.isBlank()) {
            log.warn("[JwtAuth] KEFU_INTERNAL_TOKEN not configured — X-Kefu-Internal-Token will NOT be attached");
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    KEFU_INTERNAL_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] hash = mac.doFinal(KEFU_INTERNAL_HMAC_PAYLOAD.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("[JwtAuth] failed to compute X-Kefu-Internal-Token: {}", e.getMessage());
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
