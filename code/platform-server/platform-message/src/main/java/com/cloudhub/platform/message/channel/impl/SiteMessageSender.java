package com.cloudhub.platform.message.channel.impl;

import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.domain.entity.SysMessage;
import com.cloudhub.platform.message.service.SiteMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteMessageSender implements ChannelSender {

    private final SiteMessageService siteMessageService;

    @Override
    public String channelCode() {
        return "site";
    }

    @Override
    public void send(MessageRecord record) {
        SysMessage msg = new SysMessage();
        msg.setTitle(record.getTitle());
        msg.setContent(record.getContent());
        msg.setType("system");
        msg.setSenderId(record.getSenderId());
        msg.setSenderName(record.getSenderName());
        msg.setReceiverId(record.getReceiverId());
        msg.setReceiverName(record.getReceiverName());
        msg.setReadStatus(0);
        msg.setBusinessType(record.getBusinessType());
        msg.setBusinessId(record.getBusinessId());
        siteMessageService.save(msg);
        log.info("[站内信] 已保存: title={}, receiverId={}", record.getTitle(), record.getReceiverId());
    }
}
