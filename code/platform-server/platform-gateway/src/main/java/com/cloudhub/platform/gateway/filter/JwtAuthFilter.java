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

import java.util.List;

/**
 * JWT 鉴权过滤器
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

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
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Name", username == null ? "" : username)
                    .header("X-Tenant-Id", tenantId == null ? "" : String.valueOf(tenantId))
                    .header("X-User-Type", userType == null ? "" : String.valueOf(userType))
                    .header("X-User-Permissions", permsHeader)
                    .build();
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
}
