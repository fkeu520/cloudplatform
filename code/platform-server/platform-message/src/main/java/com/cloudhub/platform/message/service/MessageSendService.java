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
        // M4: 并发防护 — 原子条件更新，只有待发送(sendStatus=0) 才抢占成功
        boolean claimed = messageRecordService.lambdaUpdate()
                .eq(MessageRecord::getId, request.getRecordId())
                .eq(MessageRecord::getSendStatus, 0)
                .set(MessageRecord::getSendStatus, 1)
                .set(MessageRecord::getSendTime, LocalDateTime.now())
                .update();
        if (!claimed) {
            log.warn("消息已被处理或不存在，跳过: {}", request.getRecordId());
            return;
        }

        MessageRecord record = messageRecordService.getById(request.getRecordId());
        if (record == null) {
            log.warn("消息记录不存在: {}", request.getRecordId());
            return;
        }

        try {
            ChannelSender sender = senderRegistry.getSender(record.getChannelCode());
            sender.send(record);
            record.setSendStatus(2);
        } catch (Exception e) {
            log.error("消息发送失败: recordId={}, error={}", record.getId(), e.getMessage());
            int retry = record.getRetryCount() + 1;
            record.setRetryCount(retry);
            record.setErrorMsg(e.getMessage());
            // M3: 使用 > 而非 >=，maxRetries=0 时第一次失败即判死，maxRetries=1 时允许一次重试
            if (retry > record.getMaxRetries()) {
                record.setSendStatus(3);
            }
        }
        messageRecordService.updateById(record);
    }
}
