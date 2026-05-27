package com.cloudhub.platform.workflow.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.workflow-message:workflow-message}")
    private String topic;

    public void sendMessage(WorkflowMessage message) {
        try {
            kafkaTemplate.send(topic, message.getAssignee(), message);
            log.info("Workflow message sent to message center: taskId={}, assignee={}", message.getTaskId(), message.getAssignee());
        } catch (Exception e) {
            log.warn("Failed to send workflow message: taskId={}, error={}", message.getTaskId(), e.getMessage());
        }
    }
}
