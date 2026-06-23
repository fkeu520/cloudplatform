package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Equipment;
import com.cloudhub.platform.space.domain.entity.Kit;
import com.cloudhub.platform.space.mapper.EquipmentMapper;
import com.cloudhub.platform.space.mapper.KitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 装修配套 Service (park-space 业务)
 * <p>W3.3 阶段: 装修配套 (Kit) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KitService {

    private final KitMapper kitMapper;
    private final EquipmentMapper equipmentMapper;

    // ========== Query ==========

    public Result<PageResult<Kit>> page(String keyword, Long parkId, Integer status,
                                         int pageNum, int pageSize) {
        LambdaQueryWrapper<Kit> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Kit::getKitName, keyword);
        }
        if (parkId != null) {
            w.eq(Kit::getParkId, parkId);
        }
        if (status != null) {
            w.eq(Kit::getStatus, status);
        }
        w.eq(Kit::getDeleted, 0).orderByAsc(Kit::getCreateTime);

        Page<Kit> p = kitMapper.selectPage(new Page<>(pageNum, pageSize), w);
        // 填充 equipmentCount
        List<Kit> records = p.getRecords();
        if (!records.isEmpty()) {
            Set<Long> kitIds = records.stream().map(Kit::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<Equipment> eqW = new LambdaQueryWrapper<Equipment>()
                    .in(Equipment::getKitId, kitIds)
                    .eq(Equipment::getDeleted, 0)
                    .select(Equipment::getKitId, Equipment::getAmount);
            List<Equipment> eqList = equipmentMapper.selectList(eqW);
            Map<Long, Integer> countMap = eqList.stream()
                    .collect(Collectors.groupingBy(Equipment::getKitId,
                            Collectors.summingInt(e -> e.getAmount() != null ? e.getAmount() : 0)));
            for (Kit k : records) {
                k.setEquipmentCount(countMap.getOrDefault(k.getId(), 0));
            }
        }
        PageResult<Kit> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[KitService] page keyword={}, parkId={} -> total={}", keyword, parkId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Kit> getById(Long id) {
        Kit k = kitMapper.selectById(id);
        if (k == null) throw new BizException("配套不存在");
        return Result.ok(k);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String kitName = requiredString(params, "kitName");

        Long count = kitMapper.selectCount(new LambdaQueryWrapper<Kit>()
                .eq(Kit::getParkId, parkId)
                .eq(Kit::getKitName, kitName)
                .eq(Kit::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在配套 " + kitName);
        }

        Kit k = new Kit();
        k.setParkId(parkId);
        k.setKitName(kitName);
        k.setAmount(params.get("amount") != null
                ? ((Number) params.get("amount")).intValue() : 0);
        k.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        k.setTenantId(currentTenantId());

        kitMapper.insert(k);
        log.info("[KitService] create: id={}, parkId={}, kitName={}", k.getId(), parkId, kitName);
        return Result.ok(k.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Kit k = kitMapper.selectById(id);
        if (k == null) throw new BizException("配套不存在");
        if (k.getDeleted() != null && k.getDeleted() == 1) throw new BizException("配套已删除");

        if (params.containsKey("kitName")) {
            String newName = (String) params.get("kitName");
            if (newName != null && !newName.isBlank()) {
                Long count = kitMapper.selectCount(new LambdaQueryWrapper<Kit>()
                        .eq(Kit::getParkId, k.getParkId())
                        .eq(Kit::getKitName, newName)
                        .ne(Kit::getId, id)
                        .eq(Kit::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + k.getParkId() + " 已存在配套 " + newName);
                }
                k.setKitName(newName);
            }
        }
        if (params.containsKey("amount"))
            k.setAmount(((Number) params.get("amount")).intValue());
        if (params.containsKey("status"))
            k.setStatus(((Number) params.get("status")).intValue());

        kitMapper.updateById(k);
        log.info("[KitService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Kit k = kitMapper.selectById(id);
        if (k == null) throw new BizException("配套不存在");
        k.setDeleted(1);
        kitMapper.updateById(k);
        log.info("[KitService] delete: id={}", id);
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

    /**
     * 校验同园区配套名称唯一性
     */
    public Result<Boolean> checkName(Long parkId, String kitName, Long excludeId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Kit> w = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Kit>()
                .eq(Kit::getParkId, parkId)
                .eq(Kit::getKitName, kitName)
                .eq(Kit::getDeleted, 0);
        if (excludeId != null) {
            w.ne(Kit::getId, excludeId);
        }
        Long count = kitMapper.selectCount(w);
        boolean available = count == null || count == 0;
        log.info("[KitService] checkName parkId={}, kitName={} -> available={}", parkId, kitName, available);
        return Result.ok(available);
    }
}