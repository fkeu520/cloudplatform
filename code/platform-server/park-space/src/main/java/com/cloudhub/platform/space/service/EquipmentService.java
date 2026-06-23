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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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

    // ========== List by Kit ==========

    public Result<List<Equipment>> listByKitId(Long kitId) {
        LambdaQueryWrapper<Equipment> w = new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getKitId, kitId)
                .eq(Equipment::getDeleted, 0)
                .orderByAsc(Equipment::getEquipmentName);
        List<Equipment> list = equipmentMapper.selectList(w);
        return Result.ok(list);
    }

    // ========== Batch Save ==========

    @Transactional
    public Result<Void> batchSave(Long kitId, List<Map<String, Object>> equipmentList) {
        // 先获取当前 kit 的所有设备
        List<Equipment> existing = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                        .eq(Equipment::getKitId, kitId)
                        .eq(Equipment::getDeleted, 0));

        // 提取前端传过来的 id（有 id 的是更新，没有的是新增）
        List<Long> incomingIds = equipmentList.stream()
                .map(m -> m.get("id"))
                .filter(id -> id != null)
                .map(id -> ((Number) id).longValue())
                .collect(toList());

        // 删除前端没传的（已删除的行）
        for (Equipment eq : existing) {
            if (!incomingIds.contains(eq.getId())) {
                eq.setDeleted(1);
                equipmentMapper.updateById(eq);
            }
        }

        // 新增或更新
        for (Map<String, Object> m : equipmentList) {
            String equipmentName = (String) m.get("equipmentName");
            if (equipmentName == null || equipmentName.isBlank()) continue;

            Object idObj = m.get("id");
            if (idObj != null) {
                // 更新
                Long id = ((Number) idObj).longValue();
                Equipment eq = equipmentMapper.selectById(id);
                if (eq != null && eq.getDeleted() != 1) {
                    eq.setEquipmentName(equipmentName);
                    eq.setModel((String) m.get("model"));
                    eq.setAmount(m.get("amount") != null ? ((Number) m.get("amount")).intValue() : 1);
                    equipmentMapper.updateById(eq);
                }
            } else {
                // 新增
                Equipment eq = new Equipment();
                eq.setKitId(kitId);
                eq.setEquipmentName(equipmentName);
                eq.setModel((String) m.get("model"));
                eq.setAmount(m.get("amount") != null ? ((Number) m.get("amount")).intValue() : 1);
                eq.setParkId(m.get("parkId") != null ? ((Number) m.get("parkId")).longValue() : null);
                eq.setStatus(1);
                eq.setTenantId(currentTenantId());
                equipmentMapper.insert(eq);
            }
        }
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
        try {
            return Long.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new BizException("字段类型错误: " + key);
        }
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