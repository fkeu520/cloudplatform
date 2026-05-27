package com.cloudhub.platform.message.service;

import com.cloudhub.platform.message.channel.ChannelSender;
import com.cloudhub.platform.message.channel.ChannelSenderRegistry;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.model.MessageSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSendService {

    private final MessageRecordService messageRecordService;
    private final ChannelSenderRegistry senderRegistry;

    public void processSend(MessageSendRequest request) {
        MessageRecord record = messageRecordService.getById(request.getRecordId());
        if (record == null) {
            log.warn("消息记录不存在: {}", request.getRecordId());
            return;
        }
        record.setSendStatus(1);
        record.setSendTime(LocalDateTime.now());
        messageRecordService.updateById(record);

        try {
            ChannelSender sender = senderRegistry.getSender(record.getChannelCode());
            sender.send(record);
            record.setSendStatus(2);
        } catch (Exception e) {
            log.error("消息发送失败: recordId={}, error={}", record.getId(), e.getMessage());
            int retry = record.getRetryCount() + 1;
            record.setRetryCount(retry);
            record.setErrorMsg(e.getMessage());
            if (retry >= record.getMaxRetries()) {
                record.setSendStatus(3);
            }
        }
        messageRecordService.updateById(record);
    }
}
