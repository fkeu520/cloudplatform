package com.cloudhub.platform.message.consumer;

import com.cloudhub.platform.message.model.MessageSendRequest;
import com.cloudhub.platform.message.service.MessageSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendConsumer {

    private final MessageSendService messageSendService;

    @KafkaListener(topics = "${spring.kafka.topic.message-send:message-send}", groupId = "${spring.kafka.consumer.group-id:message-send-consumer}")
    public void consume(MessageSendRequest request) {
        log.info("[Kafka消费] 收到消息发送请求: recordId={}, channel={}", request.getRecordId(), request.getChannelCode());
        messageSendService.processSend(request);
    }
}
