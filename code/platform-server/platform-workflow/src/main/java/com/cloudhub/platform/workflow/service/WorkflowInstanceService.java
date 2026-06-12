package com.cloudhub.platform.workflow.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.notify.WorkflowMessage;
import com.cloudhub.platform.workflow.notify.TaskNotifyMessage;
import com.cloudhub.platform.workflow.notify.TaskNotifyProducer;
import com.cloudhub.platform.workflow.notify.WorkflowMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final IdentityService identityService;
    private final TaskService taskService;
    private final TaskNotifyProducer taskNotifyProducer;
    private final WorkflowMessageProducer workflowMessageProducer;

    @Transactional
    public Map<String, Object> start(String processDefinitionKey, String businessKey,
                                       Map<String, Object> variables, String userId) {
        if (StringUtils.isBlank(processDefinitionKey)) {
            throw new BizException("流程定义Key不能为空");
        }
        identityService.setAuthenticatedUserId(userId);
        Map<String, Object> vars = variables != null ? new HashMap<>(variables) : new HashMap<>();
        vars.put("initiator", userId);
        ProcessInstance pi;
        if (StringUtils.isNotBlank(businessKey)) {
            pi = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, vars);
        } else {
            pi = runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
        }
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("processInstanceId", pi.getId());
        resultMap.put("processDefinitionId", pi.getProcessDefinitionId());
        resultMap.put("businessKey", pi.getBusinessKey());
        resultMap.put("name", pi.getName());
        resultMap.put("startTime", pi.getStartTime());

        try {
            List<Task> firstTasks = taskService.createTaskQuery()
                    .processInstanceId(pi.getId()).active().list();
            for (Task t : firstTasks) {
                List<String> recipients = collectTaskRecipients(t);
                taskNotifyProducer.sendTaskNotify(new TaskNotifyMessage(
                        t.getId(), t.getName(), String.join(",", recipients),
                        t.getProcessInstanceId(), t.getProcessDefinitionId(),
                        null, t.getCreateTime()));

                workflowMessageProducer.sendMessage(new WorkflowMessage(
                        t.getId(), t.getName(), recipients,
                        t.getProcessInstanceId(), t.getProcessDefinitionId(),
                        pi.getProcessDefinitionName(), pi.getBusinessKey(),
                        t.getCreateTime() != null ? t.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null));
            }
        } catch (Exception e) {
            log.warn("Failed to notify new tasks: {}", e.getMessage());
        }

        return resultMap;
    }

    public Map<String, Object> page(String processDefinitionKey, String name, Integer status,
                                     int pageNum, int pageSize) {
        org.flowable.engine.history.HistoricProcessInstanceQuery query = historyService
                .createHistoricProcessInstanceQuery();
        if (StringUtils.isNotBlank(processDefinitionKey)) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (StringUtils.isNotBlank(name)) {
            query.processInstanceNameLike("%" + name + "%");
        }
        if (status != null) {
            if (status == 1) query.finished();
            else query.unfinished();
        }
        long total = query.count();
        List<HistoricProcessInstance> list = query.orderByProcessInstanceStartTime().desc()
                .listPage((pageNum - 1) * pageSize, pageSize);

        List<Map<String, Object>> records = new ArrayList<>();
        for (HistoricProcessInstance hpi : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", hpi.getId());
            m.put("processDefinitionId", hpi.getProcessDefinitionId());
            m.put("processDefinitionKey", hpi.getProcessDefinitionKey());
            m.put("processDefinitionName", hpi.getProcessDefinitionName());
            m.put("businessKey", hpi.getBusinessKey());
            m.put("startTime", hpi.getStartTime());
            m.put("endTime", hpi.getEndTime());
            m.put("durationInMillis", hpi.getDurationInMillis());
            m.put("startUserId", hpi.getStartUserId());
            m.put("deleteReason", hpi.getDeleteReason());
            m.put("status", hpi.getEndTime() != null ? 1 : 0);
            Map<String, Object> vars = new LinkedHashMap<>();
            historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hpi.getId()).list().forEach(v ->
                            vars.put(v.getVariableName(), v.getValue()));
            m.put("variables", vars);
            records.add(m);
        }
        return Map.of("records", records, "total", total, "size", pageSize, "current", pageNum);
    }

    public Map<String, Object> getById(String processInstanceId) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hpi == null) throw new BizException("流程实例不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", hpi.getId());
        m.put("processDefinitionId", hpi.getProcessDefinitionId());
        m.put("processDefinitionKey", hpi.getProcessDefinitionKey());
        m.put("processDefinitionName", hpi.getProcessDefinitionName());
        m.put("businessKey", hpi.getBusinessKey());
        m.put("startTime", hpi.getStartTime());
        m.put("endTime", hpi.getEndTime());
        m.put("durationInMillis", hpi.getDurationInMillis());
        m.put("startUserId", hpi.getStartUserId());
        m.put("deleteReason", hpi.getDeleteReason());
        m.put("status", hpi.getEndTime() != null ? 1 : 0);
        m.put("variables", historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).list().stream()
                .map(v -> {
                    Map<String, Object> vm = new LinkedHashMap<>();
                    vm.put("name", v.getVariableName());
                    vm.put("value", v.getValue());
                    return vm;
                })
                .toList());
        return m;
    }

    @Transactional
    public void delete(String processInstanceId, String reason) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (pi != null) {
            runtimeService.deleteProcessInstance(processInstanceId,
                    StringUtils.isNotBlank(reason) ? reason : "手动删除");
        }
        historyService.deleteHistoricProcessInstance(processInstanceId);
    }

    public List<Map<String, Object>> getTimeline(String processInstanceId) {
        List<org.flowable.engine.history.HistoricActivityInstance> activities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        return activities.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("activityId", a.getActivityId());
            m.put("activityName", a.getActivityName());
            m.put("activityType", a.getActivityType());
            m.put("startTime", a.getStartTime());
            m.put("endTime", a.getEndTime());
            m.put("durationInMillis", a.getDurationInMillis());
            m.put("assignee", a.getAssignee());
            // 对 userTask 节点, 补充候选人信息 (供前端详情显示)
            if ("userTask".equals(a.getActivityType())) {
                try {
                    List<Task> activeTasks = taskService.createTaskQuery()
                            .processInstanceId(processInstanceId)
                            .taskDefinitionKey(a.getActivityId())
                            .list();
                    if (!activeTasks.isEmpty()) {
                        // 正在进行的任务: 返回候选人
                        m.put("candidateUsers", collectTaskRecipients(activeTasks.get(0)));
                    } else {
                        // 已完成的任务: 从 HistoricTaskInstance 取 assignee (即办理人)
                        var hti = historyService.createHistoricTaskInstanceQuery()
                                .processInstanceId(processInstanceId)
                                .taskDefinitionKey(a.getActivityId())
                                .finished()
                                .orderByHistoricTaskInstanceEndTime().desc()
                                .list()
                                .stream().findFirst().orElse(null);
                        if (hti != null) {
                            m.put("assignee", hti.getAssignee());
                        }
                    }
                } catch (Exception ignored) {}
            }
            return m;
        }).toList();
    }

    /**
     * 收集任务的收件人列表:
     *   - 直接 assignee (如有)
     *   - 候选用户 (从 identityLinks 读取 candidate user)
     * 用于解决候选人任务 assignee=null 导致 Kafka 消息丢失的问题
     */
    protected List<String> collectTaskRecipients(Task task) {
        List<String> recipients = new ArrayList<>();
        if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
            recipients.add(task.getAssignee());
        }
        try {
            List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
            for (IdentityLink link : links) {
                if ("candidate".equals(link.getType()) && link.getUserId() != null
                        && !recipients.contains(link.getUserId())) {
                    recipients.add(link.getUserId());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load identity links for task {}: {}", task.getId(), e.getMessage());
        }
        return recipients;
    }
}
