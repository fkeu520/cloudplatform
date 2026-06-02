package com.cloudhub.platform.message.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.message.domain.entity.MessageTemplate;
import com.cloudhub.platform.message.mapper.MessageTemplateMapper;
import org.springframework.stereotype.Service;

@Service
public class MessageTemplateService extends ServiceImpl<MessageTemplateMapper, MessageTemplate> {
}
