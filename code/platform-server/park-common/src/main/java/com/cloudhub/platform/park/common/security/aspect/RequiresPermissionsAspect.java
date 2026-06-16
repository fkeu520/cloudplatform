package com.cloudhub.platform.park.common.security.aspect;

import com.cloudhub.platform.park.common.security.annotation.RequiresPermissions;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * {@link RequiresPermissions} 切面 (W2.3 阶段最小实现)
 *
 * <p><b>W2 阶段策略</b>: 注解壳已就位, 切面默认放行 + DEBUG 日志, 不阻塞业务.</p>
 *
 * <p><b>W3 阶段计划</b>:
 * <ol>
 *   <li>在 platform-common 增加 {@code LoginContextHolder} (ThreadLocal, 含 userId/roles/permissions)</li>
 *   <li>在 platform-auth 增加 {@code JwtAuthFilter} (解析 JWT → 写入 LoginContextHolder)</li>
 *   <li>本切面改造: 读 LoginContextHolder, 校验用户权限集合 ⊇ required</li>
 *   <li>最终方案: 引入 spring-security, 用 {@code @PreAuthorize("hasAuthority('X')")} 替换本注解</li>
 * </ol>
 *
 * <p><b>设计取舍</b>: W2 阶段不引入 spring-security (5MB+ 依赖, 与现状 JwtUtil 重复),
 * 不阻塞 W3 业务模块翻译. W3 阶段再决定 spring-security vs 自研.</p>
 *
 * <p>使用示例:
 * <pre>{@code
 * @RequiresPermissions("user:add")
 * @PostMapping("/user")
 * public Result<Void> createUser(...) { ... }
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.3
 * @since 2026-06-16
 */
@Slf4j
@Aspect
@Component
public class RequiresPermissionsAspect {

    @Around("@annotation(perm)")
    public Object check(ProceedingJoinPoint pjp, RequiresPermissions perm) throws Throwable {
        String[] required = perm.value();
        if (required == null || required.length == 0) {
            return pjp.proceed();
        }

        // W2 阶段: 仅记录, 不真正鉴权 (避免阻塞 W3 业务翻译)
        // 业务方应自行保证 web 层 (Filter/Interceptor) 已做身份校验
        if (log.isDebugEnabled()) {
            log.debug("[W2 阶段 @RequiresPermissions 放行] 方法={} 权限={} 关系={}",
                    pjp.getSignature().toShortString(),
                    Arrays.toString(required),
                    perm.logical());
        }

        return pjp.proceed();
    }
}

