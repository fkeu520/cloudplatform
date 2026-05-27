package com.cloudhub.platform.message.channel;

import com.cloudhub.platform.message.domain.entity.MessageRecord;

public interface ChannelSender {
    String channelCode();
    void send(MessageRecord record);
}
