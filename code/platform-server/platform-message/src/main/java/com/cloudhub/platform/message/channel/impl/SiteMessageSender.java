package com.cloudhub.platform.message.channel.impl;

import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SiteMessageSender implements ChannelSender {

    @Override
    public String channelCode() {
        return "site";
    }

    @Override
    public void send(MessageRecord record) {
        log.info("[站内信] 发送站内信: title={}, receiverId={}, receiverName={}",
                record.getTitle(), record.getReceiverId(), record.getReceiverName());
    }
}
