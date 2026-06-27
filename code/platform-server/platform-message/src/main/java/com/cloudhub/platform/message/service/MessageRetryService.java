package com.cloudhub.platform.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.model.MessageSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryService {

    private final MessageRecordService messageRecordService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 60000)
    public void retryPendingMessages() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);
        List<MessageRecord> pending = messageRecordService.list(new LambdaQueryWrapper<MessageRecord>()
            .in(MessageRecord::getSendStatus, 0, 1)
            .lt(MessageRecord::getCreateTime, deadline)
            .apply("retry_count < max_retries"));

        for (MessageRecord record : pending) {
            log.info("重试发送消息: recordId={}, retry={}/{}", record.getId(), record.getRetryCount(), record.getMaxRetries());
            record.setRetryCount(record.getRetryCount() + 1);
            record.setSendStatus(1);
            messageRecordService.updateById(record);
            kafkaTemplate.send("message-send", new MessageSendRequest(
                record.getId(), record.getChannelCode(), record.getTitle(),
                record.getContent(), record.getReceiverAddress(), record.getTemplateId()));
        }
        if (!pending.isEmpty()) {
            log.info("消息重试完成: {} 条已重新发送", pending.size());
        }
    }
}
