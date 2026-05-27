package com.cloudhub.platform.message.channel.impl;

import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsSender implements ChannelSender {

    @Override
    public String channelCode() {
        return "sms";
    }

    @Override
    public void send(MessageRecord record) {
        log.info("[短信-占位] 发送短信: phone={}, content={}",
                record.getReceiverAddress(), record.getContent());
        log.info("[短信-占位] 正式生产环境将调用阿里云短信SDK发送");
    }
}
