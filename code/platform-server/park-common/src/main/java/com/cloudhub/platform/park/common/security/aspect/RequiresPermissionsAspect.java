package com.cloudhub.platform.park.common.security.aspect;

import com.cloudhub.platform.park.common.base.exception.AccessDeniedException;
import com.cloudhub.platform.park.common.security.annotation.Logical;
import com.cloudhub.platform.park.common.security.annotation.RequiresPermissions;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.park.common.security.context.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * {@link RequiresPermissions} 切面 (W2.3 阶段完整实现)
 *
 * <p>从 {@link LoginContextHolder} 读取当前 LoginUser, 校验 {@code @RequiresPermissions}
 * 声明的权限是否被当前用户持有, 否则抛 {@link AccessDeniedException}.</p>
 *
 * <p><b>W2.3 阶段策略</b>:
 * <ol>
 *   <li>读 LoginContextHolder 当前 LoginUser</li>
 *   <li>无 LoginUser → 视为"匿名请求", 抛 403</li>
 *   <li>有 LoginUser → 校验 permissions ⊇ required (AND) / ∩ ≠ ∅ (OR)</li>
 *   <li>不满足 → 抛 403 AccessDeniedException</li>
 *   <li>满足 → pjp.proceed() 继续</li>
 * </ol>
 *
 * <p><b>类级 + 方法级注解</b>: 支持 {@code @RequiresPermissions} 加在类上 (默认权限) + 方法上 (额外权限).
 * 方法级权限会与类级权限合并 (AND), 业务方按需选择.</p>
 *
 * <p><b>W3 阶段计划</b>:
 * <ol>
 *   <li>在 platform-auth 增加 JwtAuthFilter, 解析 JWT → 写 LoginContextHolder</li>
 *   <li>本切面不动, 仅依赖 holder 即可工作</li>
 *   <li>可选: 引入 spring-security, 用 {@code @PreAuthorize("hasAuthority('X')")} 替换本注解</li>
 * </ol>
 *
 * <p>使用示例:
 * <pre>{@code
 * // 类级: 该类所有方法都需要 user:view
 * {@code @RestController}
 * {@code @RequestMapping("/api/user")}
 * {@code @RequiresPermissions("user:view")}
 * public class UserController {
 *
 *     // 方法级: 额外需要 user:add
 *     {@code @PostMapping}
 *     {@code @RequiresPermissions("user:add")}
 *     public Result<Void> create(...) { ... }
 * }
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.3
 * @since 2026-06-16 (升级 2026-06-18)
 * @see org.apache.shiro.authz.annotation.RequiresPermissions (源类)
 * @see com.cloudhub.platform.park.common.security.context.LoginContextHolder
 */
@Slf4j
@Aspect
@Component
public class RequiresPermissionsAspect {

    /**
     * 切点: 方法上有 @RequiresPermissions 注解
     */
    @Pointcut("@annotation(com.cloudhub.platform.park.common.security.annotation.RequiresPermissions)")
    public void methodLevel() {}

    /**
     * 切点: 类上有 @RequiresPermissions 注解 (类级)
     */
    @Pointcut("@within(com.cloudhub.platform.park.common.security.annotation.RequiresPermissions)")
    public void classLevel() {}

    /**
     * 环绕通知: 同时处理方法级 + 类级
     */
    @Around("methodLevel() || classLevel()")
    public Object check(ProceedingJoinPoint pjp) throws Throwable {
        // 1. 解析注解 (方法级优先, 否则取类级)
        RequiresPermissions perm = resolveAnnotation(pjp);
        if (perm == null) {
            return pjp.proceed();
        }

        String[] required = perm.value();
        Logical logical = perm.logical();

        // 2. 空权限直接放行
        if (required == null || required.length == 0) {
            return pjp.proceed();
        }

        // 3. 读 LoginContextHolder
        LoginUser user = LoginContextHolder.get();
        if (user == null) {
            log.warn("[RequiresPermissions] 未登录, 拒绝访问: 方法={} 需要权限={}",
                    pjp.getSignature().toShortString(), Arrays.toString(required));
            throw new AccessDeniedException("未登录或登录已过期");
        }

        // 4. 校验权限
        Set<String> owned = user.getPermissions();
        boolean granted = checkPermissions(owned, required, logical);

        if (!granted) {
            log.warn("[RequiresPermissions] 权限不足: 用户={} 已有={} 需要={} 关系={}",
                    user.getUsername(), owned, Arrays.toString(required), logical);
            throw new AccessDeniedException(
                    String.format("权限不足, 需要 %s 权限 %s", logical, Arrays.toString(required)));
        }

        if (log.isDebugEnabled()) {
            log.debug("[RequiresPermissions] 通过: 用户={} 已有={} 需要={} 关系={}",
                    user.getUsername(), owned, Arrays.toString(required), logical);
        }

        return pjp.proceed();
    }

    /**
     * 解析注解: 方法级优先, 否则取类级
     */
    private RequiresPermissions resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();

        // 方法级
        RequiresPermissions methodPerm = method.getAnnotation(RequiresPermissions.class);
        if (methodPerm != null) {
            return methodPerm;
        }

        // 类级
        Class<?> targetClass = method.getDeclaringClass();
        if (targetClass.isAnnotationPresent(RequiresPermissions.class)) {
            return targetClass.getAnnotation(RequiresPermissions.class);
        }

        return null;
    }

    /**
     * 校验权限集合 ⊇ required
     */
    private boolean checkPermissions(Set<String> owned, String[] required, Logical logical) {
        if (logical == Logical.OR) {
            // 任一满足
            for (String perm : required) {
                if (owned.contains(perm)) return true;
            }
            return false;
        }
        // AND: 全部满足
        for (String perm : required) {
            if (!owned.contains(perm)) return false;
        }
        return true;
    }
}
