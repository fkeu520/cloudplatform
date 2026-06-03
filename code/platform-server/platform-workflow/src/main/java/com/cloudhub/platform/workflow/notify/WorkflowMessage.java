package com.cloudhub.platform.workflow.notify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程消息通知
 *
 * recipients: 收件人列表
 *   - 直接指定 assignee 的任务: 含 assignee 单人
 *   - 候选人池任务: 含所有 candidate user
 * 之前用 assignee 字段 (String), 但候选人任务无 assignee, 导致:
 *   1. KafkaTemplate.send 用 null 作 partition key, 消息丢失
 *   2. 消息中心无法入库 (recipient 为 null)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMessage implements Serializable {
    private String taskId;
    private String taskName;
    /** 收件人列表 (直接 assignee + 所有候选用户) */
    private List<String> recipients = new ArrayList<>();
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionName;
    private String businessKey;
    private LocalDateTime createTime;
}
