package com.cloudhub.platform.auth.aspect;

import com.cloudhub.platform.auth.service.StepUpTokenService;
import com.cloudhub.platform.common.annotation.RequireStepUp;
import com.cloudhub.platform.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Step-up 鉴权 AOP 切面
 * <p>拦截 {@link RequireStepUp} 注解方法, 校验 {@code X-Step-Up-Token} 请求头.</p>
 * <p>验证通过后将 tokenId 写入 request 属性, 供 OperLogAspect 关联审计.</p>
 *
 * <h2>Order 约定</h2>
 * <p>Order = 100 (在 {@code @RequiresPermissions} 之后, 业务事务前).</p>
 *
 * <h2>异步/定时任务场景</h2>
 * <p>无 HTTP 请求上下文时, 跳过 step-up 校验 (业务上应避免在异步任务中调用 @RequireStepUp 方法).</p>
 *
 * @author cloudhub
 * @since v8.0 (2026-07)
 */
@Slf4j
@Aspect
@Component
@Order(100)
@RequiredArgsConstructor
public class StepUpAspect implements Ordered {

    private final StepUpTokenService stepUpService;

    @Around("@annotation(requireStepUp)")
    public Object around(ProceedingJoinPoint pjp, RequireStepUp requireStepUp) throws Throwable {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            // 异步/定时任务场景, 跳过 step-up (pjp.getSignature() 可能为 null, 避免 NPE)
            log.warn("@RequireStepUp invoked from non-HTTP context, skip step-up");
            return pjp.proceed();
        }

        String token = req.getHeader("X-Step-Up-Token");
        String ip = getClientIp(req);
        String ua = req.getHeader("User-Agent");

        try {
            Long tokenId = stepUpService.verifyAndConsume(
                    token, requireStepUp.scope(), ip, ua, requireStepUp.allowMultiUse());

            // 写入 request 属性, 供 OperLogAspect 关联审计
            req.setAttribute("stepUpTokenId", tokenId);
            req.setAttribute("requiresStepUp", true);

            log.debug("Step-up verified: scope={}, tokenId={}, method={}",
                    requireStepUp.scope(), tokenId, pjp.getSignature().toShortString());
        } catch (BizException e) {
            log.warn("Step-up rejected: scope={}, ip={}, reason={}",
                    requireStepUp.scope(), ip, e.getMessage());
            throw e;
        }

        return pjp.proceed();
    }

    @Override
    public int getOrder() {
        return 100;
    }

    /**
     * 获取当前 HTTP 请求. 无请求上下文 (异步/定时) 返回 null.
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /**
     * 解析客户端 IP (优先级: X-Forwarded-For > X-Real-IP > remoteAddr).
     * <p>与 OperLogAspect.getIpAddress 一致.</p>
     */
    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时取第一个
            int commaIdx = ip.indexOf(',');
            return commaIdx > 0 ? ip.substring(0, commaIdx).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) return ip;
        return req.getRemoteAddr();
    }
}
