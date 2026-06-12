package com.cloudhub.platform.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.message.domain.entity.SysMessage;
import com.cloudhub.platform.message.service.SiteMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "站内信", description = "站内消息收件箱")
@RequiredArgsConstructor
@RestController
@RequestMapping("/message/site")
public class SiteMessageController {

    private final SiteMessageService siteMessageService;

    @Operation(summary = "分页查询站内信")
    @GetMapping("/page")
    public Result<IPage<SysMessage>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "") String userId
    ) {
        QueryWrapper<SysMessage> query = new QueryWrapper<>();
        if (type != null && !type.isBlank()) query.eq("type", type);
        if (readStatus != null) query.eq("read_status", readStatus);
        if (keyword != null && !keyword.isBlank())
            query.and(w -> w.like("title", keyword).or().like("content", keyword));
        if (userId != null && !userId.isBlank())
            query.and(w -> w.eq("receiver_id", userId).or().eq("receiver_name", userId).or().isNull("receiver_id"));
        query.orderByDesc("create_time");
        return Result.ok(siteMessageService.page(new Page<>(pageNum, pageSize), query));
    }

    @Operation(summary = "获取未读站内信数量")
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@RequestParam(defaultValue = "") String userId) {
        QueryWrapper<SysMessage> query = new QueryWrapper<>();
        query.eq("read_status", 0);
        if (userId != null && !userId.isBlank())
            query.and(w -> w.eq("receiver_id", userId).or().eq("receiver_name", userId).or().isNull("receiver_id"));
        return Result.ok((int) siteMessageService.count(query));
    }

    @Operation(summary = "查询站内信详情")
    @GetMapping("/{id}")
    public Result<SysMessage> getById(@PathVariable Long id) {
        return Result.ok(siteMessageService.getById(id));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setReadStatus(1);
        siteMessageService.updateById(msg);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestBody Map<String, Object> params) {
        String userId = (String) params.getOrDefault("userId", "");
        // 2026-06-12 三次修复: 真根因是 MySQL 8.0 严格模式
        //   SELECT 路径容忍 BIGINT = 'string' (隐式转 0)
        //   UPDATE 路径严格: 'zhangs' 转 BIGINT 失败 → "Truncated incorrect DOUBLE value" 500
        //   修复: userId 转 Long, 数字传 Long, 非数字跳过 receiver_id 只用 receiver_name
        Long receiverId = tryParseLong(userId);
        UpdateWrapper<SysMessage> uw = new UpdateWrapper<>();
        uw.set("read_status", 1);
        uw.eq("read_status", 0);
        if (userId != null && !userId.isBlank()) {
            uw.and(w -> buildUserScope(w, receiverId, userId));
        }
        siteMessageService.update(uw);
        return Result.ok();
    }

    /**
     * 构造用户范围条件 (receiver_id / receiver_name / IS NULL)
     * <p>注意: receiver_id 是 BIGINT, 必须传 Long, 不能传 String,
     * 否则 MySQL 8.0 严格模式拒绝隐式转数字, UPDATE 报
     * "Data truncation: Truncated incorrect DOUBLE value" 500。
     */
    private void buildUserScope(UpdateWrapper<SysMessage> w, Long receiverId, String userId) {
        boolean first = true;
        if (receiverId != null) {
            w.eq("receiver_id", receiverId);
            first = false;
        }
        if (userId != null && !userId.isBlank()) {
            if (!first) w.or();
            w.eq("receiver_name", userId);
            first = false;
        }
        if (!first) w.or();
        w.isNull("receiver_id");
    }

    private static Long tryParseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    @Operation(summary = "删除站内信")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        siteMessageService.removeById(id);
        return Result.ok();
    }
}
