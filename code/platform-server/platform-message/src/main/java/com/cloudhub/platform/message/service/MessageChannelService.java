package com.cloudhub.platform.message.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudhub.platform.message.domain.entity.MessageChannel;
import com.cloudhub.platform.message.mapper.MessageChannelMapper;
import org.springframework.stereotype.Service;

@Service
public class MessageChannelService extends ServiceImpl<MessageChannelMapper, MessageChannel> {
}
