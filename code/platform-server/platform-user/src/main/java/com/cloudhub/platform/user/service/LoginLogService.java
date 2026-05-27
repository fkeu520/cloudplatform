package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        if (tenantId != null) {
            wrapper.eq(LoginLog::getTenantId, tenantId);
        }
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
