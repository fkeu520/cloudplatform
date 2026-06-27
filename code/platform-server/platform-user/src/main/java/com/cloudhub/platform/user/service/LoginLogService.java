package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.user.domain.entity.LoginLog;
import com.cloudhub.platform.user.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginLogService extends ServiceImpl<LoginLogMapper, LoginLog> {

    public Page<LoginLog> page(String username, Integer userType, Long tenantId, Integer status,
                                String startTime, String endTime, int pageNum, int pageSize) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(LoginLog::getUsername, username);
        }
        if (userType != null) {
            wrapper.eq(LoginLog::getUserType, userType);
        }
        // U2-4: 租户隔离 — 如果未传 tenantId, 从上下文获取, 都没有则抛错 (禁止返回所有租户日志)
        Long effectiveTenantId = tenantId;
        if (effectiveTenantId == null) {
            effectiveTenantId = LoginContextHolder.getTenantId();
        }
        if (effectiveTenantId == null) {
            throw new com.cloudhub.platform.common.exception.BizException("缺少租户上下文, 无权查询登录日志");
        }
        wrapper.eq(LoginLog::getTenantId, effectiveTenantId);
        if (status != null && status >= 0) {
            wrapper.eq(LoginLog::getStatus, status);
        }
        if (startTime != null && endTime != null) {
            wrapper.between(LoginLog::getLoginTime, startTime, endTime);
        }
        wrapper.orderByDesc(LoginLog::getLoginTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
