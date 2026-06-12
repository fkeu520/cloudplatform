package com.cloudhub.platform.workflow.notify;

import com.cloudhub.platform.common.notify.WorkflowMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * WorkflowMessageProducer 单元测试
 *
 * 核心测试点:
 * - 为每个 recipient 单独发送一条 Kafka 消息
 * - 候选人任务 (recipients 是多用户列表) 也能正确发出
 * - 避免之前用 null 作 partition key 导致消息丢失
 */
@ExtendWith(MockitoExtension.class)
class WorkflowMessageProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private WorkflowMessageProducer producer;

    @BeforeEach
    void setUp() {
        producer = new WorkflowMessageProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "topic", "workflow-message");
        // 2026-06-12: producer 现在用 .whenComplete(...) 处理 ACK, mock send 返回 CompletableFuture
        // 避免 NPE: "Cannot invoke whenComplete because the return value of send is null"
        org.mockito.Mockito.when(kafkaTemplate.send(
                org.mockito.ArgumentMatchers.any(String.class),
                org.mockito.ArgumentMatchers.any(Object.class),
                org.mockito.ArgumentMatchers.any(Object.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("单收件人: 应发送 1 条 Kafka 消息, partition key = recipient")
    void singleRecipient_shouldSendOneMessage() {
        WorkflowMessage msg = new WorkflowMessage(
            "task1", "部门审批", Arrays.asList("1"),
            "proc1", "def1", "请假审批", "biz1", null
        );

        producer.sendMessage(msg);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("workflow-message"), keyCaptor.capture(), eq(msg));
        assertEquals("1", keyCaptor.getValue(),
            "Kafka partition key 必须是 recipient, 不能是 null");
    }

    @Test
    @DisplayName("多收件人 (候选人池任务): 应为每个候选人发送 1 条")
    void multipleRecipients_shouldSendOneMessagePerCandidate() {
        // 模拟候选人任务: 3 个候选人
        WorkflowMessage msg = new WorkflowMessage(
            "task2", "人事审批", Arrays.asList("1", "2", "3"),
            "proc2", "def1", "请假审批", "biz2", null
        );

        producer.sendMessage(msg);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(3)).send(eq("workflow-message"), keyCaptor.capture(), eq(msg));
        List<String> keys = keyCaptor.getAllValues();
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList("1", "2", "3")),
            "每个候选人都应有独立消息");
    }

    @Test
    @DisplayName("recipients 为空: 不应发送 Kafka 消息 (避免无效消息)")
    void emptyRecipients_shouldNotSendMessage() {
        WorkflowMessage msg = new WorkflowMessage(
            "task3", "无候选人", Arrays.asList(),
            "proc3", "def1", "请假审批", "biz3", null
        );

        producer.sendMessage(msg);

        verify(kafkaTemplate, times(0)).send(any(), any(), any());
    }

    @Test
    @DisplayName("recipients 为 null: 不应发送, 也不应抛异常")
    void nullRecipients_shouldNotThrow() {
        WorkflowMessage msg = new WorkflowMessage(
            "task4", "空任务", null,
            "proc4", "def1", "请假审批", "biz4", null
        );

        // 不应抛 NullPointerException
        assertDoesNotThrow(() -> producer.sendMessage(msg));
        verify(kafkaTemplate, times(0)).send(any(), any(), any());
    }

    @Test
    @DisplayName("反例: 修复前用 String assignee, 候选人任务会导致消息丢失")
    void oldSingleAssigneeField_problematicForCandidateTasks() {
        // 模拟旧的设计: 候选人任务 assignee=null
        // 旧版 sendMessage 用 message.getAssignee() 作 partition key
        // KafkaTemplate.send(topic, null, message) 消息可能丢失或分到默认分区

        // 验证: 新设计用 recipients 列表, 即使是候选人池也能正确发送
        WorkflowMessage candidateTaskMsg = new WorkflowMessage();
        candidateTaskMsg.setTaskId("task5");
        candidateTaskMsg.setTaskName("候选人审批");
        // 旧版: candidateTaskMsg.getAssignee() = null
        // 新版: candidateTaskMsg.getRecipients() = ["1", "2"]
        candidateTaskMsg.setRecipients(Arrays.asList("1", "2"));
        candidateTaskMsg.setProcessInstanceId("proc5");

        producer.sendMessage(candidateTaskMsg);

        // 验证: 新版本发出 2 条, partition key 是每个候选人
        verify(kafkaTemplate, times(2)).send(eq("workflow-message"), any(String.class), eq(candidateTaskMsg));
    }
}
