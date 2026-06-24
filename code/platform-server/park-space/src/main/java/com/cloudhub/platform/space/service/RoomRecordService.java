package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.RoomRecord;
import com.cloudhub.platform.space.mapper.RoomRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 房间记录 Service (park-space 业务)
 * <p>W3.6 阶段: 房间记录 (RoomRecord) append-only 简单 CRUD.</p>
 * <p>记录 sys_room 绑定/解绑客户+合同的历史. status: 0=添加 1=解除.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomRecordService {

    private final RoomRecordMapper roomRecordMapper;

    // ========== Query ==========

    public Result<PageResult<RoomRecord>> page(Long roomId, Long parkId, Integer status,
                                               int pageNum, int pageSize) {
        LambdaQueryWrapper<RoomRecord> w = new LambdaQueryWrapper<>();
        if (roomId != null) {
            w.eq(RoomRecord::getRoomId, roomId);
        }
        if (parkId != null) {
            w.eq(RoomRecord::getParkId, parkId);
        }
        if (status != null) {
            w.eq(RoomRecord::getStatus, status);
        }
        w.eq(RoomRecord::getDeleted, 0).orderByDesc(RoomRecord::getCreateTime);

        Page<RoomRecord> p = roomRecordMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<RoomRecord> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    public Result<RoomRecord> getById(Long id) {
        RoomRecord r = roomRecordMapper.selectById(id);
        if (r == null) throw new BizException("房间记录不存在");
        return Result.ok(r);
    }

    // ========== Create (append-only) ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        Long roomId = requiredLong(params, "roomId");

        RoomRecord r = new RoomRecord();
        r.setParkId(parkId);
        r.setRoomId(roomId);
        r.setCustomerId(params.get("customerId") != null
                ? ServiceUtils.toLong(params.get("customerId")) : null);
        r.setCovenantId(params.get("covenantId") != null
                ? ServiceUtils.toLong(params.get("covenantId")) : null);
        r.setCovenantType(params.get("covenantType") != null
                ? ServiceUtils.toInt(params.get("covenantType")) : 0);
        r.setStatus(params.get("status") != null
                ? ServiceUtils.toInt(params.get("status")) : 0);
        r.setTenantId(currentTenantId());

        roomRecordMapper.insert(r);
        log.info("[RoomRecordService] create: id={}, roomId={}, status={}", r.getId(), roomId, r.getStatus());
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