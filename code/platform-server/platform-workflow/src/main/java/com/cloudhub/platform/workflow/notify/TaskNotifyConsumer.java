package com.cloudhub.platform.workflow.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskNotifyConsumer {

    private final TaskNotifyStore taskNotifyStore;

    @KafkaListener(topics = "${spring.kafka.topic.task-notify:task-notify}", groupId = "workflow-task-notify")
    public void onTaskNotify(TaskNotifyMessage message) {
        log.info("Kafka consumed: taskId={}, taskName={}, assignee={}",
                message.getTaskId(), message.getTaskName(), message.getAssignee());
        taskNotifyStore.add(message);
    }
}
