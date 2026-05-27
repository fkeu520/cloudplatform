package com.cloudhub.platform.workflow.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.workflow.notify.TaskNotifyMessage;
import com.cloudhub.platform.workflow.notify.TaskNotifyProducer;
import com.cloudhub.platform.workflow.notify.WorkflowMessage;
import com.cloudhub.platform.workflow.notify.WorkflowMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTaskService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskNotifyProducer taskNotifyProducer;
    private final WorkflowMessageProducer workflowMessageProducer;

    public Map<String, Object> todoPage(String userId, String processName, int pageNum, int pageSize) {
        if (StringUtils.isBlank(userId)) {
            throw new BizException("用户ID不能为空");
        }
        TaskQuery query = taskService.createTaskQuery().taskAssignee(userId).active();
        if (StringUtils.isNotBlank(processName)) {
            query.processDefinitionNameLike("%" + processName + "%");
        }
        long total = query.count();
        List<Task> tasks = query.orderByTaskCreateTime().desc()
                .listPage((pageNum - 1) * pageSize, pageSize);
        return Map.of("records", tasks.stream().map(this::taskToMap).toList(),
                "total", total, "size", pageSize, "current", pageNum);
    }

    public Map<String, Object> donePage(String userId, String processName, int pageNum, int pageSize) {
        if (StringUtils.isBlank(userId)) {
            throw new BizException("用户ID不能为空");
        }
        org.flowable.task.api.history.HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery().taskAssignee(userId).finished();
        if (StringUtils.isNotBlank(processName)) {
            query.processDefinitionNameLike("%" + processName + "%");
        }
        long total = query.count();
        List<HistoricTaskInstance> tasks = query.orderByHistoricTaskInstanceEndTime().desc()
                .listPage((pageNum - 1) * pageSize, pageSize);
        return Map.of("records", tasks.stream().map(this::histTaskToMap).toList(),
                "total", total, "size", pageSize, "current", pageNum);
    }

    public Map<String, Object> getById(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new BizException("任务不存在");
        Map<String, Object> result = taskToMap(task);
        result.put("formVariables", taskService.getVariables(taskId));
        result.put("processVariables", runtimeService.getVariables(task.getProcessInstanceId()));
        return result;
    }

    @Transactional
    public void complete(String taskId, Map<String, Object> variables, String comment, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new BizException("任务不存在");
        if (!task.getAssignee().equals(userId)) {
            throw new BizException("非当前任务处理人");
        }
        if (variables != null) {
            taskService.setVariables(taskId, variables);
        }
        if (StringUtils.isNotBlank(comment)) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }
        taskService.complete(taskId);

        try {
            List<Task> nextTasks = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId()).active().list();
            for (Task next : nextTasks) {
                taskNotifyProducer.sendTaskNotify(new TaskNotifyMessage(
                        next.getId(), next.getName(), next.getAssignee(),
                        next.getProcessInstanceId(), next.getProcessDefinitionId(),
                        null, next.getCreateTime()));

                String processDefName = null;
                String businessKey = null;
                try {
                    var hpi = historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(next.getProcessInstanceId()).singleResult();
                    if (hpi != null) {
                        processDefName = hpi.getProcessDefinitionName();
                        businessKey = hpi.getBusinessKey();
                    }
                } catch (Exception ignored) {}

                workflowMessageProducer.sendMessage(new WorkflowMessage(
                        next.getId(), next.getName(), next.getAssignee(),
                        next.getProcessInstanceId(), next.getProcessDefinitionId(),
                        processDefName, businessKey,
                        next.getCreateTime() != null ? next.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null));
            }
        } catch (Exception e) {
            log.warn("Failed to notify next tasks: {}", e.getMessage());
        }
    }

    @Transactional
    public void reject(String taskId, String comment, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new BizException("任务不存在");
        if (!task.getAssignee().equals(userId)) {
            throw new BizException("非当前任务处理人");
        }
        taskService.addComment(taskId, task.getProcessInstanceId(),
                StringUtils.isNotBlank(comment) ? comment : "驳回");
        Map<String, Object> vars = new HashMap<>();
        vars.put("rejected", true);
        vars.put("rejectReason", comment);
        taskService.complete(taskId, vars);
    }

    @Transactional
    public void transfer(String taskId, String newAssignee, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new BizException("任务不存在");
        if (!task.getAssignee().equals(userId)) {
            throw new BizException("非当前任务处理人");
        }
        taskService.setAssignee(taskId, newAssignee);
        taskService.setOwner(taskId, userId);
    }

    @Transactional
    public void claim(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }

    @Transactional
    public void unclaim(String taskId) {
        taskService.unclaim(taskId);
    }

    private Map<String, Object> taskToMap(TaskInfo task) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", task.getId());
        m.put("name", task.getName());
        m.put("description", task.getDescription());
        m.put("assignee", task.getAssignee());
        m.put("owner", task.getOwner());
        m.put("createTime", task.getCreateTime());
        m.put("processInstanceId", task.getProcessInstanceId());
        m.put("processDefinitionId", task.getProcessDefinitionId());
        m.put("taskDefinitionKey", task.getTaskDefinitionKey());
        m.put("priority", task.getPriority());
        m.put("dueDate", task.getDueDate());
        if (task instanceof HistoricTaskInstance hti) {
            m.put("endTime", hti.getEndTime());
            m.put("durationInMillis", hti.getDurationInMillis());
            m.put("deleteReason", hti.getDeleteReason());
        }
        return m;
    }

    private Map<String, Object> histTaskToMap(HistoricTaskInstance hti) {
        Map<String, Object> m = taskToMap(hti);
        try {
            m.put("processDefinitionName", historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(hti.getProcessInstanceId()).singleResult()
                    .getProcessDefinitionName());
        } catch (Exception ignored) {}
        return m;
    }
}
