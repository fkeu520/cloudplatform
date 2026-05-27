package com.cloudhub.platform.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.message.domain.entity.MessageTemplate;
import com.cloudhub.platform.message.service.MessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息模板", description = "消息模板管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/message/template")
public class MessageTemplateController {

    private final MessageTemplateService messageTemplateService;

    @Operation(summary = "模板分页")
    @GetMapping("/page")
    public Result<IPage<MessageTemplate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<MessageTemplate> query = new QueryWrapper<>();
        if (channelCode != null && !channelCode.isBlank()) query.eq("channel_code", channelCode);
        if (keyword != null && !keyword.isBlank())
            query.and(w -> w.like("template_name", keyword).or().like("template_code", keyword));
        query.orderByDesc("create_time");
        return Result.ok(messageTemplateService.page(new Page<>(pageNum, pageSize), query));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public Result<MessageTemplate> getById(@PathVariable Long id) {
        return Result.ok(messageTemplateService.getById(id));
    }

    @Operation(summary = "新增模板")
    @PostMapping
    public Result<Void> add(@RequestBody MessageTemplate template) {
        messageTemplateService.save(template);
        return Result.ok();
    }

    @Operation(summary = "编辑模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MessageTemplate template) {
        template.setId(id);
        messageTemplateService.updateById(template);
        return Result.ok();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageTemplateService.removeById(id);
        return Result.ok();
    }
}
