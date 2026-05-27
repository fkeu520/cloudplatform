package com.cloudhub.platform.workflow.notify;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskNotifyStore {

    private final List<NotifyRecord> records = new CopyOnWriteArrayList<>();
    private static final int MAX_SIZE = 100;

    @Data
    public static class NotifyRecord {
        private String id;
        private String taskId;
        private String taskName;
        private String assignee;
        private String processInstanceId;
        private long createTime;
        private boolean read;

        public NotifyRecord(String id, String taskId, String taskName, String assignee, String processInstanceId) {
            this.id = id;
            this.taskId = taskId;
            this.taskName = taskName;
            this.assignee = assignee;
            this.processInstanceId = processInstanceId;
            this.createTime = System.currentTimeMillis();
            this.read = false;
        }
    }

    public void add(TaskNotifyMessage msg) {
        NotifyRecord rec = new NotifyRecord(
                msg.getTaskId(),
                msg.getTaskId(),
                msg.getTaskName(),
                msg.getAssignee(),
                msg.getProcessInstanceId()
        );
        records.add(0, rec);
        if (records.size() > MAX_SIZE) {
            records.remove(records.size() - 1);
        }
        log.debug("Notification stored: taskId={}, assignee={}", msg.getTaskId(), msg.getAssignee());
    }

    public Map<String, Object> getUnread(String userId) {
        List<NotifyRecord> unread = records.stream()
                .filter(r -> !r.isRead() && (userId == null || userId.equals(r.getAssignee())))
                .collect(Collectors.toList());
        long count = unread.size();
        List<Map<String, Object>> list = unread.stream().map(r -> Map.<String, Object>of(
                "id", r.getId(), "taskId", r.getTaskId(), "taskName", r.getTaskName(),
                "assignee", r.getAssignee(), "processInstanceId", r.getProcessInstanceId(),
                "createTime", r.getCreateTime()
        )).collect(Collectors.toList());
        return Map.of("count", count, "records", list);
    }

    public void markRead(String taskId) {
        records.stream()
                .filter(r -> r.getTaskId().equals(taskId))
                .forEach(r -> r.setRead(true));
    }

    public void markAllRead(String userId) {
        records.stream()
                .filter(r -> !r.isRead() && (userId == null || userId.equals(r.getAssignee())))
                .forEach(r -> r.setRead(true));
    }
}
