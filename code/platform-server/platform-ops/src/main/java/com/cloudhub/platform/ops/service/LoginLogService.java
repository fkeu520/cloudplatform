package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.ops.domain.entity.LoginLog;
import com.cloudhub.platform.ops.domain.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public IPage<LoginLog> page(String username, Integer status, String startTime, String endTime, int pageNum, int pageSize) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(LoginLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(LoginLog::getStatus, status);
        }
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            wrapper.between(LoginLog::getLoginTime, startTime, endTime);
        }
        wrapper.orderByDesc(LoginLog::getLoginTime);
        return loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<LoginLog> list(String username, Integer status) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) wrapper.like(LoginLog::getUsername, username);
        if (status != null) wrapper.eq(LoginLog::getStatus, status);
        wrapper.orderByDesc(LoginLog::getLoginTime);
        return loginLogMapper.selectList(wrapper);
    }

    public void save(LoginLog log) {
        loginLogMapper.insert(log);
    }
}
