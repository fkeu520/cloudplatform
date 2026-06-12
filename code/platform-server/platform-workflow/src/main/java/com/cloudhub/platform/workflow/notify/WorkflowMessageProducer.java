package com.cloudhub.platform.workflow.notify;

import com.cloudhub.platform.common.notify.WorkflowMessage;
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
            // 2026-06-12 修复: 用 whenComplete 回调确认 Kafka ACK, 失败时记 ERROR 不静默
            // 之前 fire-and-forget, Kafka 慢/挂时 workflow service 不知道, 消息静默丢失
            var future = kafkaTemplate.send(topic, recipient, message);
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send workflow message to Kafka: taskId={}, recipient={}, error={}",
                            message.getTaskId(), recipient, ex.getMessage(), ex);
                    } else if (log.isDebugEnabled() && result != null) {
                        log.debug("Workflow message acked: taskId={}, recipient={}, offset={}",
                            message.getTaskId(), recipient, result.getRecordMetadata().offset());
                    }
                });
            }
            log.info("Workflow message dispatched: taskId={}, recipient={}", message.getTaskId(), recipient);
        }
    }
}
