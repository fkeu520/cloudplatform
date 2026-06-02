package com.cloudhub.platform.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.message.domain.entity.MessageChannel;
import com.cloudhub.platform.message.service.MessageChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息渠道", description = "消息渠道配置管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/message/channel")
public class MessageChannelController {

    private final MessageChannelService messageChannelService;

    @Operation(summary = "渠道列表")
    @GetMapping("/list")
    public Result<List<MessageChannel>> list() {
        return Result.ok(messageChannelService.list());
    }

    @Operation(summary = "渠道分页")
    @GetMapping("/page")
    public Result<IPage<MessageChannel>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(messageChannelService.page(new Page<>(pageNum, pageSize)));
    }

    @Operation(summary = "渠道详情")
    @GetMapping("/{id}")
    public Result<MessageChannel> getById(@PathVariable Long id) {
        return Result.ok(messageChannelService.getById(id));
    }

    @Operation(summary = "新增渠道")
    @PostMapping
    public Result<Void> add(@RequestBody MessageChannel channel) {
        messageChannelService.save(channel);
        return Result.ok();
    }

    @Operation(summary = "编辑渠道")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MessageChannel channel) {
        channel.setId(id);
        messageChannelService.updateById(channel);
        return Result.ok();
    }

    @Operation(summary = "删除渠道")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageChannelService.removeById(id);
        return Result.ok();
    }
}
