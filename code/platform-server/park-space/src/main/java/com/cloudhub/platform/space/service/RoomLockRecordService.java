package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomLockRecord;
import com.cloudhub.platform.space.mapper.RoomLockRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 房间锁定记录 Service (park-space 业务)
 * <p>W3.6 阶段: 锁定/解锁操作历史. append-only, 不允许修改/删除 (审计日志性质).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomLockRecordService {

    private final RoomLockRecordMapper roomLockRecordMapper;

    // ========== Query (只读) ==========

    public Result<PageResult<RoomLockRecord>> page(Long roomId, Long parkId, Integer isLock,
                                                    int pageNum, int pageSize) {
        LambdaQueryWrapper<RoomLockRecord> w = new LambdaQueryWrapper<>();
        if (roomId != null) {
            w.eq(RoomLockRecord::getRoomId, roomId);
        }
        if (parkId != null) {
            w.eq(RoomLockRecord::getParkId, parkId);
        }
        if (isLock != null) {
            w.eq(RoomLockRecord::getIsLock, isLock);
        }
        w.eq(RoomLockRecord::getDeleted, 0).orderByDesc(RoomLockRecord::getCreateTime);

        Page<RoomLockRecord> p = roomLockRecordMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<RoomLockRecord> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    public Result<RoomLockRecord> getById(Long id) {
        RoomLockRecord r = roomLockRecordMapper.selectById(id);
        if (r == null) throw new BizException("锁定记录不存在");
        return Result.ok(r);
    }

    // ========== Create (append-only) ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        Long roomId = requiredLong(params, "roomId");

        RoomLockRecord r = new RoomLockRecord();
        r.setParkId(parkId);
        r.setRoomId(roomId);
        r.setIsLock(params.get("isLock") != null
                ? ((Number) params.get("isLock")).intValue() : 0);
        r.setEnterpriseId(params.get("enterpriseId") != null
                ? ((Number) params.get("enterpriseId")).longValue() : null);
        r.setEnterpriseName((String) params.get("enterpriseName"));
        r.setOperator((String) params.get("operator"));
        r.setReason((String) params.get("reason"));
        r.setDays(params.get("days") != null
                ? ((Number) params.get("days")).intValue() : null);
        r.setTenantId(currentTenantId());

        roomLockRecordMapper.insert(r);
        log.info("[RoomLockRecordService] create: id={}, roomId={}, isLock={}",
                r.getId(), roomId, r.getIsLock());
        return Result.ok(r.getId());
    }

    // ========== Helpers ==========

    private Long requiredLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) throw new BizException("缺少必填字段: " + key);
        if (!(v instanceof Number)) throw new BizException("字段类型错误: " + key);
        return ((Number) v).longValue();
    }

    private Long currentTenantId() {
        Long tid = TenantContextHolder.getTenantId();
        return tid != null ? tid : 1L;
    }
}