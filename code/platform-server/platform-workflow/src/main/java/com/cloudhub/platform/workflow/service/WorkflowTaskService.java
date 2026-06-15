package com.cloudhub.platform.workflow.service;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.notify.WorkflowMessage;
import com.cloudhub.platform.workflow.notify.WorkflowMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final RepositoryService repositoryService;
    private final JdbcTemplate jdbcTemplate;
    private final WorkflowMessageProducer workflowMessageProducer;

    public Map<String, Object> todoPage(String userId, String processName, int pageNum, int pageSize) {
        if (StringUtils.isBlank(userId)) {
            throw new BizException("用户ID不能为空");
        }
        // 查找该用户的待办：包括直接指定办理人的任务 + 候选任务
        List<Task> tasks;
        long total;
        // 候选任务：直接查 ACT_RU_IDENTITYLINK 拿 task IDs（不依赖 ACT_ID_USER）
        // 原因：taskCandidateUser(userId) 会 JOIN ACT_ID_USER，若用户不在该表则返回空
        List<Task> candidateTasks = new ArrayList<>();
        try {
            List<String> taskIds = jdbcTemplate.queryForList(
                "SELECT TASK_ID_ FROM ACT_RU_IDENTITYLINK WHERE TYPE_ = 'candidate' AND USER_ID_ = ?",
                String.class, userId);
            if (!taskIds.isEmpty()) {
                candidateTasks = taskService.createTaskQuery()
                    .taskIds(new HashSet<>(taskIds))
                    .active()
                    .list();
            }
            log.info("[TODO-QUERY] userId={}, candidate taskIds={}, found {} tasks",
                userId, taskIds, candidateTasks.size());
        } catch (Exception e) {
            log.warn("Candidate query via JdbcTemplate failed, falling back to standard: {}", e.getMessage());
            candidateTasks = taskService.createTaskQuery().taskCandidateUser(userId).active().list();
        }
        // 直接指定办理人的任务
        List<Task> assignedTasks = taskService.createTaskQuery().taskAssignee(userId).active()
                .orderByTaskCreateTime().desc().list();
        // 合并去重
        Set<String> seen = new HashSet<>();
        tasks = new ArrayList<>();
        for (Task t : candidateTasks) { seen.add(t.getId()); tasks.add(t); }
        for (Task t : assignedTasks) { if (!seen.contains(t.getId())) tasks.add(t); }

        // 按创建时间排序
        tasks.sort((a, b) -> {
            Date ca = a.getCreateTime(), cb = b.getCreateTime();
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return cb.compareTo(ca);
        });

        // 按流程名称过滤
        if (StringUtils.isNotBlank(processName)) {
            tasks = tasks.stream()
                    .filter(t -> {
                        String pn = t.getProcessDefinitionId() != null ?
                            historyService.createHistoricProcessInstanceQuery()
                                .processInstanceId(t.getProcessInstanceId()).singleResult()
                                .getProcessDefinitionName() : "";
                        return pn.contains(processName);
                    })
                    .toList();
        }

        total = tasks.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, tasks.size());
        List<Task> page = from < tasks.size() ? tasks.subList(from, to) : List.of();
        return Map.of("records", page.stream().map(this::taskToMap).toList(),
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
        // 2026-06-12 修复: 候选人任务 assignee=null, 直接 complete 会 NPE
        // 前端 handleApprove 没有先 claim, 后端自动补 claim
        if (task.getAssignee() == null) {
            taskService.claim(taskId, userId);
            // claim 后重新查 task 拿到更新后的 assignee
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        }
        if (!task.getAssignee().equals(userId)) {
            throw new BizException("非当前任务处理人");
        }
        if (variables != null) {
            taskService.setVariables(taskId, variables);
        }
        if (StringUtils.isNotBlank(comment)) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }
        // 2026-06-12 修复: JDBC 直接插入的 ACT_RU_IDENTITYLINK 记录, Flowable 内部删除任务时
        // 没有正确清理, 导致外键约束 ACT_FK_TSKASS_TASK 失败
        // 在 complete 前手动删除 JDBC 插入的 candidate identity links
        jdbcTemplate.update(
            "DELETE FROM ACT_RU_IDENTITYLINK WHERE TASK_ID_ = ? AND TYPE_ = 'candidate'",
            taskId);
        taskService.complete(taskId);

        try {
            List<Task> nextTasks = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId()).active().list();
            for (Task next : nextTasks) {
                ensureTaskCandidates(next, next.getProcessDefinitionId());
                List<String> recipients = collectTaskRecipients(next);
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
                        next.getId(), next.getName(), recipients,
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

    /**
     * 收集任务的收件人列表 (直接 assignee + 候选用户)
     * 用于修复候选人任务 assignee=null 导致 Kafka 消息丢失的问题
     */
    /**
     * 确保任务的候选人已写入 ACT_RU_IDENTITYLINK (同 WorkflowInstanceService.ensureTaskCandidates, JDBC 直写)
     */
    private void ensureTaskCandidates(Task task, String processDefinitionId) {
        try {
            List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
            boolean hasCandidate = links.stream().anyMatch(l -> "candidate".equals(l.getType()));
            if (hasCandidate) return;
            org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) return;
            org.flowable.bpmn.model.FlowElement fe = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
            if (!(fe instanceof org.flowable.bpmn.model.UserTask userTask)) return;
            List<String> candidateIds = new ArrayList<>();
            if (userTask.getCandidateUsers() != null) {
                candidateIds.addAll(userTask.getCandidateUsers());
            }
            var extElements = userTask.getExtensionElements();
            if (extElements != null) {
                for (Map.Entry<String, List<org.flowable.bpmn.model.ExtensionElement>> entry : extElements.entrySet()) {
                    if (!"candidateUsers".equals(entry.getKey()) || entry.getValue() == null || entry.getValue().isEmpty()) continue;
                    String text = entry.getValue().get(0).getElementText();
                    if (text != null && !text.isBlank()) {
                        for (String id : text.split("[,， ]+")) {
                            id = id.trim();
                            if (!id.isEmpty() && !candidateIds.contains(id)) {
                                candidateIds.add(id);
                            }
                        }
                    }
                }
            }
            for (String userId : candidateIds) {
                if (userId == null || userId.isBlank()) continue;
                try {
                    jdbcTemplate.update(
                        "INSERT INTO ACT_RU_IDENTITYLINK (ID_, REV_, TYPE_, USER_ID_, TASK_ID_, PROC_INST_ID_) " +
                        "VALUES (?, 1, 'candidate', ?, ?, ?)",
                        java.util.UUID.randomUUID().toString().replace("-", ""),
                        userId, task.getId(), task.getProcessInstanceId());
                    log.debug("Inserted candidate identity link: userId={}, taskId={}", userId, task.getId());
                } catch (Exception ex) {
                    log.warn("Failed to insert candidate identity link for userId={}, taskId={}: {}",
                        userId, task.getId(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to ensure candidates for task {}: {}", task.getId(), e.getMessage());
        }
    }

    private List<String> collectTaskRecipients(Task task) {
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
        // 添加候选人信息
        List<String> candidateUsers = new ArrayList<>();
        if (task instanceof Task t) {
            List<IdentityLink> links = taskService.getIdentityLinksForTask(t.getId());
            for (IdentityLink link : links) {
                if ("candidate".equals(link.getType()) && link.getUserId() != null) {
                    candidateUsers.add(link.getUserId());
                }
            }
        }
        m.put("candidateUsers", candidateUsers);
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
