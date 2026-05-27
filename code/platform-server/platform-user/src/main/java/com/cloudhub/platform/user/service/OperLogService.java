package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.user.domain.entity.OperLog;
import com.cloudhub.platform.user.mapper.OperLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OperLogService extends ServiceImpl<OperLogMapper, OperLog> {

    public void insertOperLog(OperLog operLog) {
        save(operLog);
    }
}
