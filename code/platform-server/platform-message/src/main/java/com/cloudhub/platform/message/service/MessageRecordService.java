package com.cloudhub.platform.message.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.mapper.MessageRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class MessageRecordService extends ServiceImpl<MessageRecordMapper, MessageRecord> {
}
