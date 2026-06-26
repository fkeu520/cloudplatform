package com.cloudhub.platform.message.channel;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChannelSenderRegistry {

    private final List<ChannelSender> senders;
    private final Map<String, ChannelSender> senderMap = new ConcurrentHashMap<>();

    public ChannelSenderRegistry(List<ChannelSender> senders) {
        this.senders = senders;
    }

    @PostConstruct
    public void init() {
        for (ChannelSender sender : senders) {
            senderMap.put(sender.channelCode(), sender);
            log.info("注册消息渠道: {}", sender.channelCode());
        }
    }

    public ChannelSender getSender(String channelCode) {
        ChannelSender sender = senderMap.get(channelCode);
        if (sender == null) {
            throw new IllegalArgumentException("不支持的渠道编码: " + channelCode);
        }
        return sender;
    }
}
