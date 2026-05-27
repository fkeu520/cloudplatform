package com.cloudhub.platform.workflow.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.workflow.notify.TaskNotifyStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "流程通知")
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/notify")
public class WorkflowNotifyController {

    private final TaskNotifyStore taskNotifyStore;

    @Operation(summary = "获取未读通知")
    @GetMapping("/unread")
    public Result<?> unread(@RequestParam(defaultValue = "") String userId) {
        return Result.ok(taskNotifyStore.getUnread(userId));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/read/{taskId}")
    public Result<Void> markRead(@PathVariable String taskId) {
        taskNotifyStore.markRead(taskId);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam(defaultValue = "") String userId) {
        taskNotifyStore.markAllRead(userId);
        return Result.ok();
    }
}
