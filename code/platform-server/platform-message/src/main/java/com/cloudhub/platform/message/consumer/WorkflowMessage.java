package com.cloudhub.platform.message.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMessage implements Serializable {
    private String taskId;
    private String taskName;
    private String assignee;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionName;
    private String businessKey;
    private LocalDateTime createTime;
}
