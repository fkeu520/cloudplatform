package com.cloudhub.platform.workflow.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.workflow-message:workflow-message}")
    private String topic;

    /**
     * 为每个收件人发送一份流程消息
     *
     * 之前只用 message.getAssignee() 作 partition key, 候选人任务 assignee=null → 消息丢失
     * 修复: 遍历 recipients, 每个收件人单独发一份, partition key 用 recipient
     */
    public void sendMessage(WorkflowMessage message) {
        List<String> recipients = message.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            log.warn("Workflow message has no recipients, taskId={}", message.getTaskId());
            return;
        }
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) continue;
            try {
                kafkaTemplate.send(topic, recipient, message);
                log.info("Workflow message sent: taskId={}, recipient={}", message.getTaskId(), recipient);
            } catch (Exception e) {
                log.warn("Failed to send workflow message: taskId={}, recipient={}, error={}",
                    message.getTaskId(), recipient, e.getMessage());
            }
        }
    }
}
