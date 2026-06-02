package com.cloudhub.platform.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.message.channel.ChannelSenderRegistry;
import com.cloudhub.platform.message.domain.entity.MessageRecord;
import com.cloudhub.platform.message.model.MessageSendRequest;
import com.cloudhub.platform.message.model.SendTestRequest;
import com.cloudhub.platform.message.service.MessageRecordService;
import com.cloudhub.platform.message.service.MessageSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "消息发送", description = "消息发送与记录查询")
@RequiredArgsConstructor
@RestController
@RequestMapping("/message/record")
public class MessageRecordController {

    private final MessageRecordService messageRecordService;
    private final MessageSendService messageSendService;
    private final ChannelSenderRegistry senderRegistry;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Operation(summary = "消息记录分页")
    @GetMapping("/page")
    public Result<IPage<MessageRecord>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) Integer sendStatus,
            @RequestParam(required = false) Integer tenantId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<MessageRecord> query = new QueryWrapper<>();
        if (channelCode != null && !channelCode.isBlank()) query.eq("channel_code", channelCode);
        if (sendStatus != null) query.eq("send_status", sendStatus);
        if (tenantId != null) query.eq("tenant_id", tenantId);
        if (startTime != null && endTime != null) query.between("send_time", startTime, endTime);
        if (keyword != null && !keyword.isBlank())
            query.and(w -> w.like("title", keyword).or().like("content", keyword));
        query.orderByDesc("create_time");
        return Result.ok(messageRecordService.page(new Page<>(pageNum, pageSize), query));
    }

    @Operation(summary = "消息记录详情")
    @GetMapping("/{id}")
    public Result<MessageRecord> getById(@PathVariable Long id) {
        return Result.ok(messageRecordService.getById(id));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<Void> send(@RequestBody Map<String, Object> params) {
        String channelCode = (String) params.get("channelCode");
        String title = (String) params.getOrDefault("title", "");
        String content = (String) params.getOrDefault("content", "");
        String receiverAddress = (String) params.getOrDefault("receiverAddress", "");
        Long templateId = params.get("templateId") != null ? Long.valueOf(params.get("templateId").toString()) : null;
        Integer tenantId = params.get("tenantId") != null ? Integer.valueOf(params.get("tenantId").toString()) : null;
        String tenantName = (String) params.getOrDefault("tenantName", null);

        MessageRecord record = new MessageRecord();
        record.setTitle(title);
        record.setContent(content);
        record.setChannelCode(channelCode);
        record.setTemplateId(templateId);
        record.setReceiverAddress(receiverAddress);
        record.setTenantId(tenantId);
        record.setTenantName(tenantName);
        record.setSendStatus(0);
        record.setRetryCount(0);
        record.setMaxRetries(3);
        messageRecordService.save(record);

        kafkaTemplate.send("message-send", new MessageSendRequest(record.getId(), channelCode, title, content, receiverAddress, templateId));
        return Result.ok();
    }

    @Operation(summary = "发送测试消息")
    @PostMapping("/test-send")
    public Result<Void> testSend(@RequestBody SendTestRequest request) {
        MessageRecord record = new MessageRecord();
        record.setTitle("测试消息");
        record.setContent(request.getContent());
        record.setChannelCode(request.getChannelCode());
        record.setReceiverAddress(request.getReceiverAddress());
        record.setSendStatus(0);
        record.setRetryCount(0);
        record.setMaxRetries(1);
        messageRecordService.save(record);

        kafkaTemplate.send("message-send", new MessageSendRequest(record.getId(), request.getChannelCode(), "测试消息", request.getContent(), request.getReceiverAddress(), null));
        return Result.ok();
    }

    @Operation(summary = "重发失败消息")
    @PostMapping("/resend/{id}")
    public Result<Void> resend(@PathVariable Long id) {
        MessageRecord record = messageRecordService.getById(id);
        if (record == null) return Result.error("消息记录不存在");
        record.setSendStatus(0);
        record.setErrorMsg(null);
        record.setSendTime(LocalDateTime.now());
        messageRecordService.updateById(record);
        kafkaTemplate.send("message-send", new MessageSendRequest(record.getId(), record.getChannelCode(), record.getTitle(), record.getContent(), record.getReceiverAddress(), record.getTemplateId()));
        return Result.ok();
    }

    @Operation(summary = "删除记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageRecordService.removeById(id);
        return Result.ok();
    }
}
