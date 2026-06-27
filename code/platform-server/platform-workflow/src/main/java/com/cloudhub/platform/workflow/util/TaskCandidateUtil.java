package com.cloudhub.platform.workflow.util;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class TaskCandidateUtil {

    private TaskCandidateUtil() {}

    public static void ensureTaskCandidates(Task task, String processDefinitionId,
                                             JdbcTemplate jdbcTemplate,
                                             TaskService taskService,
                                             RepositoryService repositoryService) {
        try {
            if (hasExistingCandidates(task, taskService)) return;

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) return;

            FlowElement fe = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
            if (!(fe instanceof UserTask userTask)) return;

            List<String> candidateIds = extractCandidateIds(userTask);
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

    private static boolean hasExistingCandidates(Task task, TaskService taskService) {
        try {
            List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
            return links.stream().anyMatch(l -> "candidate".equals(l.getType()));
        } catch (Exception e) {
            log.warn("Failed to check existing identity links for task {}: {}", task.getId(), e.getMessage());
            return false;
        }
    }

    public static List<String> extractCandidateIds(UserTask userTask) {
        List<String> candidateIds = new ArrayList<>();
        if (userTask.getCandidateUsers() != null) {
            candidateIds.addAll(userTask.getCandidateUsers());
        }
        Map<String, List<ExtensionElement>> extElements = userTask.getExtensionElements();
        if (extElements != null) {
            for (Map.Entry<String, List<ExtensionElement>> entry : extElements.entrySet()) {
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
        return candidateIds;
    }

    public static List<String> collectTaskRecipients(Task task, TaskService taskService) {
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