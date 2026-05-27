package com.cloudhub.platform.workflow.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "流程定义管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/definition")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    @Operation(summary = "分页查询流程定义")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer suspensionState,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(workflowDefinitionService.page(name, category, suspensionState, pageNum, pageSize));
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{definitionId}")
    public Result<?> getById(@PathVariable String definitionId) {
        return Result.ok(workflowDefinitionService.getById(definitionId));
    }

    @Operation(summary = "部署流程（上传BPMN XML）")
    @PostMapping("/deploy")
    public Result<?> deploy(@RequestBody Map<String, String> params) {
        return Result.ok(workflowDefinitionService.deploy(
                params.get("processName"), params.get("bpmnXml")));
    }

    @Operation(summary = "挂起流程定义")
    @PostMapping("/{definitionId}/suspend")
    public Result<Void> suspend(@PathVariable String definitionId) {
        workflowDefinitionService.suspend(definitionId);
        return Result.ok();
    }

    @Operation(summary = "激活流程定义")
    @PostMapping("/{definitionId}/activate")
    public Result<Void> activate(@PathVariable String definitionId) {
        workflowDefinitionService.activate(definitionId);
        return Result.ok();
    }

    @Operation(summary = "删除流程定义")
    @DeleteMapping("/{definitionId}")
    public Result<Void> delete(@PathVariable String definitionId) {
        workflowDefinitionService.delete(definitionId);
        return Result.ok();
    }

    @Operation(summary = "获取BPMN XML")
    @GetMapping("/{definitionId}/xml")
    public Result<?> getBpmnXml(@PathVariable String definitionId) {
        return Result.ok(Map.of("bpmnXml", workflowDefinitionService.getBpmnXml(definitionId)));
    }
}
