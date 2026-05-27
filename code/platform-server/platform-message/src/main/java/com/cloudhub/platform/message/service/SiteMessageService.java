package com.cloudhub.platform.message.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.message.domain.entity.SysMessage;
import com.cloudhub.platform.message.mapper.MessageMapper;
import org.springframework.stereotype.Service;

@Service
public class SiteMessageService extends ServiceImpl<MessageMapper, SysMessage> {
}
