package com.cloudhub.platform.message.consumer;

import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.service.MessageRecordService;
import com.cloudhub.platform.message.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMessageConsumer {

    private final MessageRecordService messageRecordService;
    private final SseService sseService;

    @KafkaListener(topics = "${spring.kafka.topic.workflow-message:workflow-message}", groupId = "message-workflow-consumer")
    public void consume(WorkflowMessage message) {
        log.info("[Kafka消费] 收到流程任务通知: taskId={}, taskName={}, recipients={}",
                message.getTaskId(), message.getTaskName(), message.getRecipients());

        // 兼容旧的 assignee 字段: 如果有 assignee 但无 recipients, 用 assignee
        List<String> recipients = message.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            // 反序列化兜底: 某些情况下 recipients 字段可能因兼容性变成 null
            log.warn("Workflow message has no recipients, skip. taskId={}", message.getTaskId());
            return;
        }

        String content = "您有一条新的待办任务需要处理：\n" +
                "任务名称：" + message.getTaskName() + "\n" +
                "流程名称：" + (message.getProcessDefinitionName() != null ? message.getProcessDefinitionName() : "-") + "\n" +
                "流程实例：" + message.getProcessInstanceId();

        for (String recipient : recipients) {
            try {
                MessageRecord record = new MessageRecord();
                record.setTitle("待办任务通知");
                record.setContent(content);
                record.setChannelCode("site");
                try {
                    record.setReceiverId(Long.valueOf(recipient));
                } catch (NumberFormatException nfe) {
                    log.warn("Invalid recipient id, skip: {}", recipient);
                    continue;
                }
                record.setReceiverName(recipient);
                record.setBusinessType("workflow");
                record.setBusinessId(message.getProcessInstanceId());
                record.setSendStatus(2);
                messageRecordService.save(record);

                sseService.sendToUser(record.getReceiverId(), "workflow-notify", Map.of(
                    "id", record.getId(),
                    "title", record.getTitle(),
                    "businessType", "workflow",
                    "businessId", record.getBusinessId()
                ));

                log.info("[消息中心] 已创建流程通知消息记录: recordId={}, recipient={}", record.getId(), recipient);
            } catch (Exception e) {
                log.error("[消息中心] 创建流程通知消息记录失败: taskId={}, recipient={}, error={}",
                    message.getTaskId(), recipient, e.getMessage());
            }
        }
    }
}
