package com.cloudhub.platform.message.channel.impl;

import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.service.MessageChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender implements ChannelSender {

    private final MessageChannelService messageChannelService;

    @Override
    public String channelCode() {
        return "sms";
    }

    @Override
    public void send(MessageRecord record) {
        String configJson = messageChannelService.lambdaQuery()
            .eq(com.cloudhub.platform.message.domain.entity.MessageChannel::getChannelCode, "sms")
            .oneOpt()
            .map(c -> c.getConfigJson())
            .orElse("{}");
        log.info("[短信] 发送短信 phone={}, content={}, channelConfig={}",
                record.getReceiverAddress(), record.getContent(), configJson);
        log.info("[短信] 正式生产将调用阿里云短信SDK: accessKey从channelConfig读取");
    }
}
