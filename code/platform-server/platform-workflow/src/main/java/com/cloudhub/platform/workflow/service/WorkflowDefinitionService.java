package com.cloudhub.platform.workflow.service;

import com.cloudhub.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final RepositoryService repositoryService;

    public Map<String, Object> page(String name, String category, Integer suspensionState,
                                     int pageNum, int pageSize) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                .latestVersion();
        if (StringUtils.isNotBlank(name)) {
                query = query.processDefinitionNameLike("%" + name + "%");
        }
        if (StringUtils.isNotBlank(category)) {
            query = query.processDefinitionCategory(category);
        }
        if (suspensionState != null) {
            query = suspensionState == 1 ? query.active() : query.suspended();
        }
        long total = query.count();
        List<ProcessDefinition> defs = query.orderByProcessDefinitionName().asc()
                .listPage((pageNum - 1) * pageSize, pageSize);

        List<Map<String, Object>> list = new ArrayList<>();
        for (ProcessDefinition pd : defs) {
            list.add(toMap(pd));
        }
        return Map.of("records", list, "total", total, "size", pageSize, "current", pageNum);
    }

    public Map<String, Object> getById(String definitionId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (pd == null) throw new BizException("流程定义不存在");
        return toMap(pd);
    }

    public Map<String, Object> getByKey(String key) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key).latestVersion().singleResult();
        if (pd == null) throw new BizException("流程定义不存在");
        return toMap(pd);
    }

    @Transactional
    public Map<String, Object> deploy(String processName, String processKey, String bpmnXml) {
        if (StringUtils.isBlank(bpmnXml)) {
            throw new BizException("BPMN XML 不能为空");
        }
        String name = StringUtils.isNotBlank(processName) ? processName : "未命名流程";
        String key = StringUtils.isNotBlank(processKey) ? processKey : null;
        // 2026-06-15 修复: 校验 processKey 是合法 NCName (XML id 必须以字母/下划线开头)
        // 根因: 用户输入 "2121212212" 纯数字 processKey, Flowable 解析 BPMN 抛 SAXParseException
        //       但 Service.deploy 的 try/catch 只 catch FlowableException, SAX 异常未包装
        //       → 走 GlobalExceptionHandler.handleException → HTTP 500
        if (key != null && !isValidNCName(key)) {
            throw new BizException("流程 Key 不合法: '" + key + "' 必须以字母或下划线开头, 后续可包含字母数字下划线连字符");
        }
        String finalXml = bpmnXml;
        // 如果传入了 processKey，替换 XML 中的 process id 及 BPMNDiagram 引用
        if (key != null) {
            finalXml = bpmnXml.replaceAll(
                "(<bpmn:process\\s+id=\")[^\"]+\"",
                "$1" + Matcher.quoteReplacement(key) + "\""
            );
            // 同步更新 BPMNPlane 中的 bpmnElement 引用
            finalXml = finalXml.replaceAll(
                "(bpmnElement=\")" + "process" + "\"",
                "$1" + Matcher.quoteReplacement(key) + "\""
            );
        }
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name(name)
                    .addString(name + ".bpmn20.xml", finalXml)
                    .deploy();
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
            return Map.of(
                    "deploymentId", deployment.getId(),
                    "deployTime", deployment.getDeploymentTime(),
                    "processDefinitionId", pd != null ? pd.getId() : null
            );
        } catch (Exception e) {
            // 2026-06-15 修复: 扩展 catch 范围, 不只 catch FlowableException
            // SAXParseException / IllegalStateException 等都可能不被包装成 FlowableException
            // 不管哪种异常, 都转 BizException 给前端友好错误 (避免 HTTP 500)
            String msg = e.getMessage();
            String userMsg = extractFlowableError(msg);
            if (userMsg == null) {
                userMsg = "流程部署失败: " + (msg != null ? msg : "未知错误 (" + e.getClass().getSimpleName() + ")");
            }
            log.warn("流程部署失败, processKey={}, bpmnXml 长度={}, 错误类型={}, msg={}", 
                key, bpmnXml != null ? bpmnXml.length() : 0, e.getClass().getSimpleName(), msg);
            throw new BizException(userMsg);
        }
    }

    /**
     * 提取 Flowable 错误信息中的 Problem 描述 (用户友好版本)
     */
    private String extractFlowableError(String msg) {
        if (msg == null) return null;
        StringBuilder sb = new StringBuilder("流程定义验证失败:\n");
        for (String line : msg.split("\n")) {
            if (line.contains("Problem:") || line.contains("| Problem:")) {
                int idx = line.indexOf("| Problem: '");
                int end = line.indexOf("'", (idx > 0 ? idx : 0) + 11);
                if (idx > 0 && end > idx) {
                    sb.append("- ").append(line.substring(idx + 11, end)).append("\n");
                }
            }
        }
        if (sb.length() > 20) return sb.toString().trim();
        // SAXParseException 等也直接展示 (非 Flowable 格式但有用)
        if (msg.contains("SAXParseException") || msg.contains("not a valid value for")) {
            return "BPMN XML 格式错误: " + msg.split("\n")[0];
        }
        return null;
    }

    /**
     * 校验字符串是否为合法 NCName (XML Name)
     * 规则: 首字符 [A-Za-z_], 后续 [A-Za-z0-9_.\-]
     */
    private boolean isValidNCName(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!Character.isLetter(s.charAt(0)) && s.charAt(0) != '_') return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') return false;
        }
        return true;
    }

    @Transactional
    public void suspend(String definitionId) {
        repositoryService.suspendProcessDefinitionById(definitionId);
    }

    @Transactional
    public void activate(String definitionId) {
        repositoryService.activateProcessDefinitionById(definitionId);
    }

    @Transactional
    public void delete(String definitionId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (pd == null) throw new BizException("流程定义不存在");
        String processKey = pd.getKey();
        List<ProcessDefinition> allVersions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey).list();
        for (ProcessDefinition v : allVersions) {
            repositoryService.deleteDeployment(v.getDeploymentId(), true);
        }
    }

    public String getBpmnXml(String definitionId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (pd == null) throw new BizException("流程定义不存在");
        try (var is = repositoryService.getResourceAsStream(pd.getDeploymentId(),
                pd.getResourceName())) {
            if (is == null) return null;
            return new String(is.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toMap(ProcessDefinition pd) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pd.getId());
        map.put("key", pd.getKey());
        map.put("name", pd.getName());
        map.put("version", pd.getVersion());
        map.put("category", pd.getCategory());
        map.put("deploymentId", pd.getDeploymentId());
        map.put("suspended", pd.isSuspended());
        map.put("description", pd.getDescription());
        map.put("deployTime", repositoryService.createDeploymentQuery()
                .deploymentId(pd.getDeploymentId()).singleResult().getDeploymentTime());
        return map;
    }
}
