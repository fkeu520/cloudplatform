package com.cloudhub.platform.workflow.notify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotifyMessage {
    private String taskId;
    private String taskName;
    private String assignee;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionName;
    private Date createTime;
}
