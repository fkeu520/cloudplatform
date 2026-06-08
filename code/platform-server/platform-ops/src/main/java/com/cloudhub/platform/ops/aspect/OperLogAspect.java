package com.cloudhub.platform.ops.aspect;

import com.alibaba.fastjson2.JSON;
import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.ops.domain.entity.OperLog;
import com.cloudhub.platform.ops.domain.mapper.OperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 运营管理操作日志切面
 *
 * 与 platform-user 模块的 LogAspect 功能相同, 但 ops 是独立服务, 需各自实现
 * 拦截 @Log 注解, 写入 sys_oper_log 表
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final OperLogMapper operLogMapper;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://platform-user:8081}")
    private String userServiceUrl;

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Before("@annotation(controllerLog)")
    public void doBefore(Log controllerLog) {
        startTime.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "exception")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception exception) {
        handleLog(joinPoint, controllerLog, exception, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            long costTime = System.currentTimeMillis() - startTime.get();
            startTime.remove();

            OperLog operLog = new OperLog();
            operLog.setStatus(0);
            operLog.setCostTime(costTime);
            operLog.setOperTime(LocalDateTime.now());

            if (controllerLog != null) {
                operLog.setTitle(controllerLog.title());
                operLog.setBusinessType(controllerLog.businessType());
                operLog.setOperatorType(controllerLog.operatorType());
            }

            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(getIpAddress(request));

            // 从 JWT 解析用户信息
            try {
                String token = request.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                String username = JwtUtil.getUsername(token);
                operLog.setOperName(username);

                // M5 P0-2 PR4: 调 user 服务拿 deptId, 用于按部门审计
                // 注: 这是同步阻塞调用, ops 日志写入路径, 失败时 deptId=null 不影响日志主流程
                if (username != null && !username.isBlank()) {
                    try {
                        String url = userServiceUrl + "/user/internal/by-username/" + username;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                        if (resp != null) {
                            Object data = resp.get("data");
                            if (data instanceof Map) {
                                Object deptId = ((Map<String, Object>) data).get("deptId");
                                if (deptId != null) {
                                    operLog.setDeptId(Long.valueOf(deptId.toString()));
                                }
                            }
                        }
                    } catch (Exception userEx) {
                        log.debug("[ops] 获取用户deptId失败 (不影响日志记录): username={}, err={}", username, userEx.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.warn("获取用户信息失败", ex);
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");

            if (controllerLog != null && controllerLog.isSaveRequestData()) {
                Object[] args = joinPoint.getArgs();
                operLog.setOperParam(JSON.toJSONString(args));
            }

            if (controllerLog != null && controllerLog.isSaveResponseData() && jsonResult != null) {
                operLog.setJsonResult(JSON.toJSONString(jsonResult));
            }

            if (e != null) {
                operLog.setStatus(1);
                String errorMsg = e.getMessage();
                operLog.setErrorMsg(errorMsg != null ? (errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg) : "未知异常");
            }

            operLogMapper.insert(operLog);
        } catch (Exception exp) {
            log.error("保存操作日志异常", exp);
        }
    }

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
