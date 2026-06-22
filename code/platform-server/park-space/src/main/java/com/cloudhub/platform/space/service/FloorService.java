package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Floor;
import com.cloudhub.platform.space.mapper.FloorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 楼层 Service (park-space 业务)
 * <p>W3.3 阶段: 楼层 (Floor) 简单 CRUD + 同楼栋名称/序号唯一校验.</p>
 * <p>W3 阶段不做楼层类型联动 (地上/地下 → 更新楼栋计数) 等复杂逻辑.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FloorService {

    private final FloorMapper floorMapper;

    // ========== Query ==========

    public Result<PageResult<Floor>> page(String keyword, Long parkId, Long buildingId, Integer status,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<Floor> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Floor::getFloorName, keyword);
        }
        if (parkId != null) {
            w.eq(Floor::getParkId, parkId);
        }
        if (buildingId != null) {
            w.eq(Floor::getBuildingId, buildingId);
        }
        if (status != null) {
            w.eq(Floor::getStatus, status);
        }
        w.eq(Floor::getDeleted, 0)
                .orderByAsc(Floor::getBuildingId)
                .orderByAsc(Floor::getSorting);

        Page<Floor> p = floorMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Floor> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[FloorService] page keyword={}, buildingId={} -> total={}", keyword, buildingId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Floor> getById(Long id) {
        Floor f = floorMapper.selectById(id);
        if (f == null) throw new BizException("楼层不存在");
        return Result.ok(f);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        Long buildingId = requiredLong(params, "buildingId");
        String floorName = requiredString(params, "floorName");

        // 校验: 同楼栋楼层名称唯一
        Long count = floorMapper.selectCount(new LambdaQueryWrapper<Floor>()
                .eq(Floor::getBuildingId, buildingId)
                .eq(Floor::getFloorName, floorName)
                .eq(Floor::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("楼栋 " + buildingId + " 已存在楼层 " + floorName);
        }

        Floor f = new Floor();
        f.setParkId(parkId);
        f.setBuildingId(buildingId);
        f.setFloorName(floorName);
        f.setSerialCode(params.get("serialCode") != null
                ? ((Number) params.get("serialCode")).intValue() : null);
        f.setFloorCategory(params.get("floorCategory") != null
                ? ((Number) params.get("floorCategory")).intValue() : 0);
        f.setCoefficient(params.get("coefficient") != null
                ? new BigDecimal(params.get("coefficient").toString()) : new BigDecimal("1.00"));
        f.setSorting(params.get("sorting") != null
                ? ((Number) params.get("sorting")).intValue() : 0);
        f.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        f.setTenantId(currentTenantId());

        floorMapper.insert(f);
        log.info("[FloorService] create: id={}, buildingId={}, floorName={}", f.getId(), buildingId, floorName);
        return Result.ok(f.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Floor f = floorMapper.selectById(id);
        if (f == null) throw new BizException("楼层不存在");
        if (f.getDeleted() != null && f.getDeleted() == 1) throw new BizException("楼层已删除");

        if (params.containsKey("floorName")) {
            String newName = (String) params.get("floorName");
            if (newName != null && !newName.isBlank()) {
                Long count = floorMapper.selectCount(new LambdaQueryWrapper<Floor>()
                        .eq(Floor::getBuildingId, f.getBuildingId())
                        .eq(Floor::getFloorName, newName)
                        .ne(Floor::getId, id)
                        .eq(Floor::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("楼栋 " + f.getBuildingId() + " 已存在楼层 " + newName);
                }
                f.setFloorName(newName);
            }
        }
        if (params.containsKey("serialCode"))
            f.setSerialCode(((Number) params.get("serialCode")).intValue());
        if (params.containsKey("floorCategory"))
            f.setFloorCategory(((Number) params.get("floorCategory")).intValue());
        if (params.containsKey("coefficient"))
            f.setCoefficient(new BigDecimal(params.get("coefficient").toString()));
        if (params.containsKey("sorting"))
            f.setSorting(((Number) params.get("sorting")).intValue());
        if (params.containsKey("status"))
            f.setStatus(((Number) params.get("status")).intValue());

        floorMapper.updateById(f);
        log.info("[FloorService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Floor f = floorMapper.selectById(id);
        if (f == null) throw new BizException("楼层不存在");
        // W4+ 阶段: 校验楼层下是否有房间, 有房间不允许删除 (W3 不实现)
        f.setDeleted(1);
        floorMapper.updateById(f);
        log.info("[FloorService] delete: id={}", id);
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