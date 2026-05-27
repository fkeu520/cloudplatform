package com.cloudhub.platform.message.consumer;

import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.service.MessageRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMessageConsumer {

    private final MessageRecordService messageRecordService;

    @KafkaListener(topics = "${spring.kafka.topic.workflow-message:workflow-message}", groupId = "message-workflow-consumer")
    public void consume(WorkflowMessage message) {
        log.info("[Kafka消费] 收到流程任务通知: taskId={}, taskName={}, assignee={}",
                message.getTaskId(), message.getTaskName(), message.getAssignee());

        try {
            MessageRecord record = new MessageRecord();
            record.setTitle("待办任务通知");
            record.setContent("您有一条新的待办任务需要处理：\n" +
                    "任务名称：" + message.getTaskName() + "\n" +
                    "流程名称：" + (message.getProcessDefinitionName() != null ? message.getProcessDefinitionName() : "-") + "\n" +
                    "流程实例：" + message.getProcessInstanceId());
            record.setChannelCode("site");
            record.setReceiverId(message.getAssignee() != null ? Long.valueOf(message.getAssignee()) : null);
            record.setReceiverName(message.getAssignee());
            record.setBusinessType("workflow");
            record.setBusinessId(message.getProcessInstanceId());
            record.setSendStatus(2);
            messageRecordService.save(record);

            log.info("[消息中心] 已创建流程通知消息记录: recordId={}, assignee={}", record.getId(), message.getAssignee());
        } catch (Exception e) {
            log.error("[消息中心] 创建流程通知消息记录失败: taskId={}, error={}", message.getTaskId(), e.getMessage());
        }
    }
}
