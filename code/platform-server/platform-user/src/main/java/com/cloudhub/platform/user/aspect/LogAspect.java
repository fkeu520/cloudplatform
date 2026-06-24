package com.cloudhub.platform.user.aspect;

import com.alibaba.fastjson2.JSON;
import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.user.domain.entity.OperLog;
import com.cloudhub.platform.user.domain.vo.UserVO;
import com.cloudhub.platform.user.service.OperLogService;
import com.cloudhub.platform.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final OperLogService operLogService;
    private final UserService userService;

    // 记录开始时间
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Before("@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) {
        startTime.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "exception")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception exception) {
        handleLog(joinPoint, controllerLog, exception, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("无法获取当前请求上下文，跳过操作日志记录");
                startTime.remove();
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime.get();
            startTime.remove();

            // 创建日志对象
            OperLog operLog = new OperLog();
            operLog.setStatus(0);
            operLog.setCostTime(costTime);
            operLog.setOperTime(LocalDateTime.now());

            // 获取注解信息
            if (controllerLog != null) {
                operLog.setTitle(controllerLog.title());
                operLog.setBusinessType(controllerLog.businessType());
                operLog.setOperatorType(controllerLog.operatorType());
            }

            // 获取请求信息
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(getIpAddress(request));

            // 获取用户信息（从token中解析）
            try {
                String token = request.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                String username = JwtUtil.getUsername(token);
                operLog.setOperName(username);
                Long tenantId = JwtUtil.getTenantId(token);
                operLog.setTenantId(tenantId != null ? tenantId : 0L);

                // M5 P0-2 PR4: 查 sys_user 拿当前操作人的 dept_id, 用于按部门审计
                // 注: sys_user.dept_id 是当前部门, 若用户调岗则历史记录反映的是操作发生时的部门
                //     (生产环境通常操作和调岗是同一人, 此简化可接受; 严格场景需 sys_user_dept_history 表)
                if (username != null && !username.isBlank()) {
                    try {
                        UserVO userVO = userService.getByUsername(username);
                        if (userVO != null && userVO.getDeptId() != null) {
                            operLog.setDeptId(userVO.getDeptId());
                        }
                    } catch (Exception userEx) {
                        log.debug("获取用户deptId失败 (不影响日志记录): username={}, err={}", username, userEx.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.warn("获取用户信息失败", ex);
            }

            // 获取方法信息
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");

            // 获取请求参数
            if (controllerLog != null && controllerLog.isSaveRequestData()) {
                Object[] args = joinPoint.getArgs();
                operLog.setOperParam(Arrays.toString(args));
            }

            // 获取响应结果
            if (controllerLog != null && controllerLog.isSaveResponseData() && jsonResult != null) {
                operLog.setJsonResult(JSON.toJSONString(jsonResult));
            }

            // 异常信息
            if (e != null) {
                operLog.setStatus(1);
                String errorMsg = e.getMessage();
                operLog.setErrorMsg(errorMsg != null ? (errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg) : "未知异常");
            }

            // 保存日志
            operLogService.insertOperLog(operLog);
        } catch (Exception exp) {
            log.error("保存操作日志异常", exp);
        }
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
