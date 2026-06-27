package com.cloudhub.platform.workflow.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.notify.WorkflowMessage;
import com.cloudhub.platform.workflow.notify.WorkflowMessageProducer;
import com.cloudhub.platform.workflow.util.TaskCandidateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final RepositoryService repositoryService;
    private final JdbcTemplate jdbcTemplate;
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
        // 2026-06-15 修复: 业务方启动流程时通常传 leaveDays, 但 conditionExpression
        // (e.g. ${day >= 3}) 用的是 day. JUEL 找不到 day → 500 "Unknown property used in expression"
        // 兜底: 若 day 缺失, 从 leaveDays 复制, 避免业务方每次都传双份
        if (!vars.containsKey("day") && vars.containsKey("leaveDays")) {
            vars.put("day", vars.get("leaveDays"));
            log.info("[WF-START] 自动补 day 变量 from leaveDays: day={}", vars.get("day"));
        }
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
                // 2026-06-12 修复: 绕过 Flowable IDM 引擎, 从 BPMN 模型读取候选人并直接写入
                // Flowable 6.8.1 即使 setDisableIdmEngine(true) + setIdmEngineConfigurator(null),
                // UserTaskActivityBehavior 仍会调 identityService.isUserManaged() 查 ACT_ID_USER,
                // 找不到就跳过 candidate link 创建 (ACT_ID_USER 为空)。
                // 这里显式从已部署的 BPMN 模型中解析 <userTask> 的 candidateUsers, 用
                // taskService.addCandidateUser() 写入 ACT_RU_IDENTITYLINK, 完全跳过 IDM 校验。
                TaskCandidateUtil.ensureTaskCandidates(t, pi.getProcessDefinitionId(), jdbcTemplate, taskService, repositoryService);

                List<String> recipients = TaskCandidateUtil.collectTaskRecipients(t, taskService);
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
                        m.put("candidateUsers", TaskCandidateUtil.collectTaskRecipients(activeTasks.get(0), taskService));
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
}
