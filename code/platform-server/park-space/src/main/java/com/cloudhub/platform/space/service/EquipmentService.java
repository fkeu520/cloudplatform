package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Equipment;
import com.cloudhub.platform.space.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 设备设施 Service (park-space 业务)
 * <p>W3.4 阶段: 设备 (Equipment) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;

    // ========== Query ==========

    public Result<PageResult<Equipment>> page(String keyword, Long parkId, Long kitId, Integer status,
                                               int pageNum, int pageSize) {
        LambdaQueryWrapper<Equipment> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Equipment::getEquipmentName, keyword);
        }
        if (parkId != null) {
            w.eq(Equipment::getParkId, parkId);
        }
        if (kitId != null) {
            w.eq(Equipment::getKitId, kitId);
        }
        if (status != null) {
            w.eq(Equipment::getStatus, status);
        }
        w.eq(Equipment::getDeleted, 0).orderByAsc(Equipment::getEquipmentName);

        Page<Equipment> p = equipmentMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Equipment> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[EquipmentService] page parkId={}, kitId={} -> total={}", parkId, kitId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Equipment> getById(Long id) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) throw new BizException("设备不存在");
        return Result.ok(e);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String equipmentName = requiredString(params, "equipmentName");

        // 校验: 同园区 + 同 kit 下设备名称唯一
        Long kitId = params.get("kitId") != null
                ? ((Number) params.get("kitId")).longValue() : null;
        Long count = equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getParkId, parkId)
                .eq(Equipment::getKitId, kitId)
                .eq(Equipment::getEquipmentName, equipmentName)
                .eq(Equipment::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在设备 " + equipmentName);
        }

        Equipment e = new Equipment();
        e.setParkId(parkId);
        e.setEquipmentName(equipmentName);
        e.setModel((String) params.get("model"));
        e.setAmount(params.get("amount") != null
                ? ((Number) params.get("amount")).intValue() : 1);
        e.setKitId(kitId);
        e.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        e.setTenantId(currentTenantId());

        equipmentMapper.insert(e);
        log.info("[EquipmentService] create: id={}, parkId={}, equipmentName={}",
                e.getId(), parkId, equipmentName);
        return Result.ok(e.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) throw new BizException("设备不存在");
        if (e.getDeleted() != null && e.getDeleted() == 1) throw new BizException("设备已删除");

        if (params.containsKey("equipmentName")) {
            String newName = (String) params.get("equipmentName");
            if (newName != null && !newName.isBlank()) {
                Long count = equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>()
                        .eq(Equipment::getParkId, e.getParkId())
                        .eq(Equipment::getKitId, e.getKitId())
                        .eq(Equipment::getEquipmentName, newName)
                        .ne(Equipment::getId, id)
                        .eq(Equipment::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + e.getParkId() + " 已存在设备 " + newName);
                }
                e.setEquipmentName(newName);
            }
        }
        if (params.containsKey("model"))
            e.setModel((String) params.get("model"));
        if (params.containsKey("amount"))
            e.setAmount(((Number) params.get("amount")).intValue());
        if (params.containsKey("kitId"))
            e.setKitId(((Number) params.get("kitId")).longValue());
        if (params.containsKey("status"))
            e.setStatus(((Number) params.get("status")).intValue());

        equipmentMapper.updateById(e);
        log.info("[EquipmentService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) throw new BizException("设备不存在");
        e.setDeleted(1);
        equipmentMapper.updateById(e);
        log.info("[EquipmentService] delete: id={}", id);
        return Result.ok();
    }

    // ========== Helpers ==========

    private Long requiredLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) throw new BizException("缺少必填字段: " + key);
        if (!(v instanceof Number)) throw new BizException("字段类型错误: " + key);
        return ((Number) v).longValue();
    }

    private String requiredString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null || v.toString().isBlank()) throw new BizException("缺少必填字段: " + key);
        return v.toString().trim();
    }

    private Long currentTenantId() {
        Long tid = TenantContextHolder.getTenantId();
        return tid != null ? tid : 1L;
    }
}