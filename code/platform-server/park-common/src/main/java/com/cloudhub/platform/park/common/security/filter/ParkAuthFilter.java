package com.cloudhub.platform.park.common.security.filter;

import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.park.common.security.context.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 园区业务服务鉴权 Filter (Phase -1 W3 阶段)
 * <p>职责: 从 HTTP Header 中读取网关转发的用户上下文, 写入 {@link LoginContextHolder}.
 * 依赖 {@code platform-gateway} 的 {@code JwtAuthFilter} 已经完成 JWT 解析与 Header 注入.</p>
 * <p><b>Header 约定</b> (由 gateway 注入, 此处只读):
 * <ul>
 *   <li>{@code X-User-Id}: 用户 ID (必填)</li>
 *   <li>{@code X-User-Name}: 用户名</li>
 *   <li>{@code X-Tenant-Id}: 租户 ID</li>
 *   <li>{@code X-User-Type}: 用户类型 (0=普通 1=租户管理员 2=运营管理员)</li>
 * </ul>
 * <p><b>使用方式</b> (park-* 业务服务):
 * <pre>{@code
 * &#064;SpringBootApplication
 * public class ParkSpaceApplication { ... }
 * }</pre>
 * park-common 模块下此 Filter 标注 {@code @Component}, 业务服务启动时被自动注册.
 * 业务代码可通过 {@link LoginContextHolder#get()} 在任意位置访问当前 ParkUser.</p>
 * <p><b>禁用</b>: 设置 {@code platform.park.auth-filter.enabled=false} 可关闭 (默认开启).</p>
 * <p><b>W3 阶段增强</b>:
 * <ol>
 *   <li>调用 platform-user 的 {@code /user/internal/permissions/{userId}} 端点, 加载权限集合</li>
 *   <li>集成 Redis 缓存 (userId → permissions, TTL 5min)</li>
 *   <li>热更新: 用户权限变更时, 通过 Kafka 事件清除本地缓存</li>
 * </ol>
 * <p><b>关于 Spring Boot Filter 顺序</b>:
 * 使用 {@link Order} (HIGHEST_PRECEDENCE + 10 = -2147483648 + 10) 确保 Filter 在最早期执行,
 * 早于业务 Controller, 晚于 Servlet 容器标准 Filter.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "platform.park.auth-filter.enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ParkAuthFilter extends OncePerRequestFilter {

    /** Header 名常量 (与 platform-gateway JwtAuthFilter 对齐) */
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_TYPE = "X-User-Type";

    /** 内部调用标识 (跳过鉴权) */
    public static final String HEADER_FROM_IN = "from=in";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 内部调用直接放行 (无 Header, 跳过 LoginContextHolder 写入)
            String from = request.getHeader("from");
            if (HEADER_FROM_IN.equals(from)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 读取 Header, 构造 LoginUser
            String userIdStr = request.getHeader(HEADER_USER_ID);
            if (!StringUtils.hasText(userIdStr)) {
                // 无 userId → 未登录或网关未鉴权, 不写上下文
                // (下游可能自行鉴权, 例如内部 health check)
                filterChain.doFilter(request, response);
                return;
            }

            LoginUser user = parseLoginUser(userIdStr, request);
            LoginContextHolder.set(user);

            if (log.isDebugEnabled()) {
                log.debug("[ParkAuth] 写入 LoginContextHolder: userId={}, username={}, tenantId={}, userType={}",
                        user.getUserId(), user.getUsername(), user.getTenantId(), user.getRoles());
            }

            // 3. 放行业务
            filterChain.doFilter(request, response);

        } finally {
            // 4. 清理 (线程复用避免泄露)
            LoginContextHolder.clear();
        }
    }

    /**
     * 解析 Header 构造 LoginUser (W2.3 阶段: 仅基础字段, 权限集合为空)
     */
    private LoginUser parseLoginUser(String userIdStr, HttpServletRequest request) {
        Long userId = parseLong(userIdStr);
        String username = request.getHeader(HEADER_USER_NAME);
        Long tenantId = parseLong(request.getHeader(HEADER_TENANT_ID));
        Integer userType = parseInt(request.getHeader(HEADER_USER_TYPE));

        return LoginUser.builder()
                .userId(userId)
                .username(username)
                .tenantId(tenantId)
                // W2.3 阶段 permissions 为空, 业务模块需要时手动调用 LoginContextHolder.populatePermissions()
                .permissions(Collections.emptySet())
                .build();
    }

    private static Long parseLong(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
