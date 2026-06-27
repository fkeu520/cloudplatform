package com.cloudhub.platform.message.consumer;

import com.cloudhub.platform.common.notify.WorkflowMessage;
import com.cloudhub.platform.message.channel.impl.SiteMessageSender;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.service.MessageRecordService;
import com.cloudhub.platform.message.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMessageConsumer {

    private final MessageRecordService messageRecordService;
    private final SseService sseService;
    // 2026-06-12 修复: 必须注入 SiteMessageSender 写 sys_message 表 (站内信),
    // 之前只写 sys_message_record (发送记录), 前端 SiteMessageController 直接查
    // sys_message, 导致铃铛/未读列表看不到工作流通知, 消息沉默丢失
    // 根因: 跳过 MessageSendService.processSend 的 ChannelSender 链路
    private final SiteMessageSender siteMessageSender;

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

        // WF2-11: HTML 转义, 防止 XSS (任务名称/流程名称可能包含 HTML)
        String content = "您有一条新的待办任务需要处理：\n" +
                "任务名称：" + HtmlUtils.htmlEscape(message.getTaskName()) + "\n" +
                "流程名称：" + (message.getProcessDefinitionName() != null ? HtmlUtils.htmlEscape(message.getProcessDefinitionName()) : "-") + "\n" +
                "流程实例：" + HtmlUtils.htmlEscape(message.getProcessInstanceId());

        for (String recipient : recipients) {
            MessageRecord record = new MessageRecord();
            try {
                record.setTitle("待办任务通知");
                record.setContent(content);
                record.setChannelCode("site");
                try {
                    record.setReceiverId(Long.valueOf(recipient));
                } catch (NumberFormatException nfe) {
                    log.warn("Invalid recipient id, skip. taskId={}, recipient={}", message.getTaskId(), recipient);
                    continue;
                }
                record.setReceiverName(recipient);
                record.setBusinessType("workflow");
                record.setBusinessId(message.getProcessInstanceId());
                record.setSendStatus(2);
                // 2026-06-12 修复: 保留 sys_message_record 记录 (审计用, sendStatus=2)
                messageRecordService.save(record);

                // 2026-06-12 修复: 必须显式调用 SiteMessageSender.send() 写 sys_message 表
                // 前端铃铛/未读列表/详情页都查 sys_message (SiteMessageController),
                // 跳过这一行 → record 在 sys_message 找不到, 铃铛/未读看不到
                // 失败也不影响 record 落库, 走 try-catch 单条隔离
                try {
                    siteMessageSender.send(record);
                } catch (Exception ex) {
                    log.warn("Failed to save sys_message for workflow notify: taskId={}, recipient={}, error={}",
                        message.getTaskId(), recipient, ex.getMessage());
                }

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
