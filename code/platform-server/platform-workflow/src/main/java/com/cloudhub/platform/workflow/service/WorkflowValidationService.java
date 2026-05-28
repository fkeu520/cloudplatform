package com.cloudhub.platform.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WorkflowValidationService {

    public List<Map<String, Object>> validateBpmn(String bpmnXml) {
        List<Map<String, Object>> errors = new ArrayList<>();
        if (bpmnXml == null || bpmnXml.isBlank()) {
            errors.add(Map.of("level", "ERROR", "message", "BPMN XML 内容为空"));
            return errors;
        }
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            byte[] bytes = bpmnXml.getBytes(StandardCharsets.UTF_8);
            BpmnModel model = converter.convertToBpmnModel(new InputStreamProvider() {
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(bytes);
                }
            }, false, false);
            if (model == null) {
                errors.add(Map.of("level", "ERROR", "message", "BPMN 模型解析失败，XML 格式不正确"));
                return errors;
            }
            List<Process> processes = model.getProcesses();
            if (processes == null || processes.isEmpty()) {
                errors.add(Map.of("level", "ERROR", "message", "未找到流程定义 (process)"));
                return errors;
            }
            for (Process process : processes) {
                if (process.getId() == null || process.getId().isBlank()) {
                    errors.add(Map.of("level", "ERROR", "message", "流程定义缺少 id 属性"));
                }
                if (process.getName() == null || process.getName().isBlank()) {
                    errors.add(Map.of("level", "WARN", "message", "流程定义缺少 name 属性"));
                }
                List<UserTask> userTasks = process.findFlowElementsOfType(UserTask.class);
                for (UserTask task : userTasks) {
                    if ((task.getAssignee() == null || task.getAssignee().isBlank()) &&
                        (task.getCandidateUsers() == null || task.getCandidateUsers().isEmpty()) &&
                        (task.getCandidateGroups() == null || task.getCandidateGroups().isEmpty())) {
                        errors.add(Map.of("level", "WARN", "message",
                            "用户任务 '" + (task.getName() != null ? task.getName() : task.getId()) + "' 未配置办理人"));
                    }
                }
                List<ExclusiveGateway> gateways = process.findFlowElementsOfType(ExclusiveGateway.class);
                for (ExclusiveGateway gw : gateways) {
                    List<SequenceFlow> outgoing = gw.getOutgoingFlows();
                    if (outgoing == null || outgoing.isEmpty()) {
                        errors.add(Map.of("level", "ERROR", "message",
                            "互斥网关 '" + (gw.getName() != null ? gw.getName() : gw.getId()) + "' 没有出口连线"));
                    }
                }
            }
            if (errors.isEmpty()) {
                errors.add(Map.of("level", "INFO", "message", "BPMN 验证通过"));
            }
        } catch (Exception e) {
            errors.add(Map.of("level", "ERROR", "message", "BPMN 解析异常: " + e.getMessage()));
        }
        return errors;
    }
}
