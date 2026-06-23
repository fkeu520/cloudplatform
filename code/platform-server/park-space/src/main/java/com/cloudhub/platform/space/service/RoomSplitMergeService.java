package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import com.cloudhub.platform.space.mapper.RoomSplitMergeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 房间拆分合并记录 Service (park-space 业务)
 * <p>W3.6 阶段: 拆分合并记录 (RoomSplitMerge) append-only 简单 CRUD.</p>
 * <p>记录 sys_room 拆分/合并操作历史. status: 0=合并 1=拆分.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomSplitMergeService {

    private final RoomSplitMergeMapper roomSplitMergeMapper;

    // ========== Query ==========

    public Result<PageResult<RoomSplitMerge>> page(Long parkId, Integer status, Integer type,
                                                    int pageNum, int pageSize) {
        LambdaQueryWrapper<RoomSplitMerge> w = new LambdaQueryWrapper<>();
        if (parkId != null) {
            w.eq(RoomSplitMerge::getParkId, parkId);
        }
        if (status != null) {
            w.eq(RoomSplitMerge::getStatus, status);
        }
        if (type != null) {
            w.eq(RoomSplitMerge::getType, type);
        }
        w.eq(RoomSplitMerge::getDeleted, 0).orderByDesc(RoomSplitMerge::getCreateTime);

        Page<RoomSplitMerge> p = roomSplitMergeMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<RoomSplitMerge> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    public Result<RoomSplitMerge> getById(Long id) {
        RoomSplitMerge r = roomSplitMergeMapper.selectById(id);
        if (r == null) throw new BizException("拆分合并记录不存在");
        return Result.ok(r);
    }

    // ========== Create (append-only) ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");

        RoomSplitMerge r = new RoomSplitMerge();
        r.setParkId(parkId);
        r.setUserId(params.get("userId") != null
                ? ((Number) params.get("userId")).longValue() : null);
        r.setUserName((String) params.get("userName"));
        r.setReasons((String) params.get("reasons"));
        r.setType(params.get("type") != null
                ? ((Number) params.get("type")).intValue() : 0);
        r.setOldRoomId(params.get("oldRoomId") != null
                ? ((Number) params.get("oldRoomId")).longValue() : null);
        r.setOldRoomName((String) params.get("oldRoomName"));
        r.setNewRoomId(params.get("newRoomId") != null
                ? ((Number) params.get("newRoomId")).longValue() : null);
        r.setNewRoomName((String) params.get("newRoomName"));
        r.setNum(params.get("num") != null
                ? ((Number) params.get("num")).intValue() : null);
        r.setIsExtend(params.get("isExtend") != null
                ? ((Number) params.get("isExtend")).intValue() : 0);
        r.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 0);
        r.setTenantId(currentTenantId());

        roomSplitMergeMapper.insert(r);
        log.info("[RoomSplitMergeService] create: id={}, parkId={}, oldRoomId={}, newRoomId={}, status={}",
                r.getId(), parkId, r.getOldRoomId(), r.getNewRoomId(), r.getStatus());
        return Result.ok(r.getId());
    }

    // ========== Helpers ==========

    private Long requiredLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) throw new BizException("缺少必填字段: " + key);
        try {
            return Long.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new BizException("字段类型错误: " + key);
        }
    }

    private Long currentTenantId() {
        Long tid = TenantContextHolder.getTenantId();
        return tid != null ? tid : 1L;
    }
}