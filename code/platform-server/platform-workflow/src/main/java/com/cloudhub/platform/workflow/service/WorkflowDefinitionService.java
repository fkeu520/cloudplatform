package com.cloudhub.platform.workflow.service;

import com.cloudhub.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Transactional
    public Map<String, Object> deploy(String processName, String bpmnXml) {
        if (StringUtils.isBlank(bpmnXml)) {
            throw new BizException("BPMN XML 不能为空");
        }
        String name = StringUtils.isNotBlank(processName) ? processName : "未命名流程";
        Deployment deployment = repositoryService.createDeployment()
                .name(name)
                .addString(name + ".bpmn20.xml", bpmnXml)
                .deploy();
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
        return Map.of(
                "deploymentId", deployment.getId(),
                "deployTime", deployment.getDeploymentTime(),
                "processDefinitionId", pd != null ? pd.getId() : null
        );
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
