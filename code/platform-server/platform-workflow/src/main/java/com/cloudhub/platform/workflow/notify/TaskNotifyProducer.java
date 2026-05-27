package com.cloudhub.platform.workflow.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskNotifyProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.task-notify:task-notify}")
    private String topic;

    public void sendTaskNotify(TaskNotifyMessage message) {
        try {
            kafkaTemplate.send(topic, message.getAssignee(), message);
            log.debug("Kafka message sent: taskId={}, assignee={}", message.getTaskId(), message.getAssignee());
        } catch (Exception e) {
            log.warn("Failed to send Kafka notification for task {}: {}", message.getTaskId(), e.getMessage());
        }
    }
}
