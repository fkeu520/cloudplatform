package com.cloudhub.platform.workflow.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.workflow.service.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "流程任务管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/task")
public class WorkflowTaskController {

    private final WorkflowTaskService workflowTaskService;

    @Operation(summary = "我的待办")
    @GetMapping("/todo")
    public Result<?> todo(
            @RequestParam String userId,
            @RequestParam(required = false) String processName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(workflowTaskService.todoPage(userId, processName, pageNum, pageSize));
    }

    @Operation(summary = "我的已办")
    @GetMapping("/done")
    public Result<?> done(
            @RequestParam String userId,
            @RequestParam(required = false) String processName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(workflowTaskService.donePage(userId, processName, pageNum, pageSize));
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/{taskId}")
    public Result<?> getById(@PathVariable String taskId) {
        return Result.ok(workflowTaskService.getById(taskId));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{taskId}/complete")
    public Result<Void> complete(@PathVariable String taskId,
                                  @RequestBody(required = false) Map<String, Object> params,
                                  @RequestParam(required = false) String comment,
                                  @RequestParam String userId) {
        workflowTaskService.complete(taskId, params, comment, userId);
        return Result.ok();
    }

    @Operation(summary = "驳回")
    @PostMapping("/{taskId}/reject")
    public Result<Void> reject(@PathVariable String taskId,
                                @RequestParam(required = false) String comment,
                                @RequestParam String userId) {
        workflowTaskService.reject(taskId, comment, userId);
        return Result.ok();
    }

    @Operation(summary = "转办")
    @PostMapping("/{taskId}/transfer")
    public Result<Void> transfer(@PathVariable String taskId,
                                  @RequestParam String newAssignee,
                                  @RequestParam String userId) {
        workflowTaskService.transfer(taskId, newAssignee, userId);
        return Result.ok();
    }

    @Operation(summary = "签收任务")
    @PostMapping("/{taskId}/claim")
    public Result<Void> claim(@PathVariable String taskId, @RequestParam String userId) {
        workflowTaskService.claim(taskId, userId);
        return Result.ok();
    }

    @Operation(summary = "取消签收")
    @PostMapping("/{taskId}/unclaim")
    public Result<Void> unclaim(@PathVariable String taskId) {
        workflowTaskService.unclaim(taskId);
        return Result.ok();
    }
}
