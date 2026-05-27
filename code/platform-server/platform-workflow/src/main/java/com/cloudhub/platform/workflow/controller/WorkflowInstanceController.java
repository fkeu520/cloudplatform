package com.cloudhub.platform.workflow.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.workflow.service.WorkflowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "流程实例管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/instance")
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    @Operation(summary = "发起流程")
    @PostMapping("/start")
    public Result<?> start(@RequestBody Map<String, Object> params) {
        return Result.ok(workflowInstanceService.start(
                (String) params.get("processDefinitionKey"),
                (String) params.get("businessKey"),
                (Map<String, Object>) params.get("variables"),
                (String) params.get("userId")));
    }

    @Operation(summary = "分页查询流程实例")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(workflowInstanceService.page(
                processDefinitionKey, name, status, pageNum, pageSize));
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{processInstanceId}")
    public Result<?> getById(@PathVariable String processInstanceId) {
        return Result.ok(workflowInstanceService.getById(processInstanceId));
    }

    @Operation(summary = "删除流程实例")
    @DeleteMapping("/{processInstanceId}")
    public Result<Void> delete(@PathVariable String processInstanceId,
                                @RequestParam(required = false) String reason) {
        workflowInstanceService.delete(processInstanceId, reason);
        return Result.ok();
    }

    @Operation(summary = "获取流程时间线")
    @GetMapping("/{processInstanceId}/timeline")
    public Result<?> getTimeline(@PathVariable String processInstanceId) {
        return Result.ok(workflowInstanceService.getTimeline(processInstanceId));
    }
}
