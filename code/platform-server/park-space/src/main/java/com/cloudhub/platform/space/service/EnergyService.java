package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.Energy;
import com.cloudhub.platform.space.mapper.EnergyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 能源房间关联 Service (park-space 业务)
 * <p>W3.4 阶段: 能耗 (Energy) 简单 CRUD.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyService {

    private final EnergyMapper energyMapper;

    // ========== Query ==========

    public Result<PageResult<Energy>> page(Long parkId, Long roomId, Long meterId, Integer status,
                                             int pageNum, int pageSize) {
        LambdaQueryWrapper<Energy> w = new LambdaQueryWrapper<>();
        if (parkId != null) {
            w.eq(Energy::getParkId, parkId);
        }
        if (roomId != null) {
            w.eq(Energy::getRoomId, roomId);
        }
        if (meterId != null) {
            w.eq(Energy::getMeterId, meterId);
        }
        if (status != null) {
            w.eq(Energy::getStatus, status);
        }
        w.eq(Energy::getDeleted, 0).orderByDesc(Energy::getCreateTime);

        Page<Energy> p = energyMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Energy> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[EnergyService] page parkId={}, roomId={} -> total={}", parkId, roomId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Energy> getById(Long id) {
        Energy e = energyMapper.selectById(id);
        if (e == null) throw new BizException("能源关联不存在");
        return Result.ok(e);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        Long roomId = requiredLong(params, "roomId");

        Energy e = new Energy();
        e.setParkId(parkId);
        e.setRoomId(roomId);
        e.setMeterId(params.get("meterId") != null
                ? ServiceUtils.toLong(params.get("meterId")) : null);
        e.setMeterClassId(params.get("meterClassId") != null
                ? ServiceUtils.toLong(params.get("meterClassId")) : null);
        e.setStatus(params.get("status") != null
                ? ServiceUtils.toInt(params.get("status")) : 1);
        e.setTenantId(currentTenantId());

        energyMapper.insert(e);
        log.info("[EnergyService] create: id={}, parkId={}, roomId={}", e.getId(), parkId, roomId);
        return Result.ok(e.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Energy e = energyMapper.selectById(id);
        if (e == null) throw new BizException("能源关联不存在");
        if (e.getDeleted() != null && e.getDeleted() == 1) throw new BizException("能源关联已删除");

        if (params.containsKey("meterId"))
            e.setMeterId(ServiceUtils.toLong(params.get("meterId")));
        if (params.containsKey("meterClassId"))
            e.setMeterClassId(ServiceUtils.toLong(params.get("meterClassId")));
        if (params.containsKey("roomId"))
            e.setRoomId(ServiceUtils.toLong(params.get("roomId")));
        if (params.containsKey("status"))
            e.setStatus(ServiceUtils.toInt(params.get("status")));

        energyMapper.updateById(e);
        log.info("[EnergyService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Energy e = energyMapper.selectById(id);
        if (e == null) throw new BizException("能源关联不存在");
        e.setDeleted(1);
        energyMapper.updateById(e);
        log.info("[EnergyService] delete: id={}", id);
        return Result.ok();
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