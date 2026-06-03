package com.cloudhub.platform.message.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台消息服务接收的流程任务消息 DTO
 *
 * 注意: 字段名必须与发送方 (platform-workflow 的 WorkflowMessage) 完全一致
 *   发送方已把 assignee (String) 改为 recipients (List&lt;String&gt;)
 *   字段名不一致会导致反序列化失败或字段为空
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMessage implements Serializable {
    private String taskId;
    private String taskName;
    private List<String> recipients = new ArrayList<>();
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionName;
    private String businessKey;
    private LocalDateTime createTime;
}
