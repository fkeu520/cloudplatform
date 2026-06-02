package com.cloudhub.platform.common.config;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cloudhub.platform.common.annotation.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) return;
        log.debug("DataScope check: userId={}, alias={}", userId, dataScope.alias());
    }
}
