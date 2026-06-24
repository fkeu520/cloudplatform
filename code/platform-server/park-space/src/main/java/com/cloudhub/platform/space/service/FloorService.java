package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
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
        f.setSerialCode(ServiceUtils.toInt(params.get("serialCode")));
        f.setFloorCategory(ServiceUtils.toIntOrDefault(params.get("floorCategory"), 0));
        f.setCoefficient(params.get("coefficient") != null
                ? new BigDecimal(params.get("coefficient").toString()) : new BigDecimal("1.00"));
        f.setSorting(ServiceUtils.toIntOrDefault(params.get("sorting"), 0));
        f.setStatus(ServiceUtils.toIntOrDefault(params.get("status"), 1));
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

        // 园区变更: 支持从其他园区迁入 (前端 el-select 改动)
        if (params.containsKey("parkId")) {
            f.setParkId(ServiceUtils.toLong(params.get("parkId")));
        }
        // 楼栋变更: 支持换楼栋 (新楼栋可能在同园区或不同园区)
        if (params.containsKey("buildingId")) {
            f.setBuildingId(ServiceUtils.toLong(params.get("buildingId")));
        }

        if (params.containsKey("floorName")) {
            String newName = (String) params.get("floorName");
            if (newName != null && !newName.isBlank()) {
                // 名称变更: 重新校验唯一性 (按当前 buildingId)
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
            f.setSerialCode(ServiceUtils.toInt(params.get("serialCode")));
        if (params.containsKey("floorCategory"))
            f.setFloorCategory(ServiceUtils.toInt(params.get("floorCategory")));
        if (params.containsKey("coefficient"))
            f.setCoefficient(new BigDecimal(params.get("coefficient").toString()));
        if (params.containsKey("sorting"))
            f.setSorting(ServiceUtils.toInt(params.get("sorting")));
        if (params.containsKey("status"))
            f.setStatus(ServiceUtils.toInt(params.get("status")));

        floorMapper.updateById(f);
        log.info("[FloorService] update: id={}, parkId={}, buildingId={}", id, f.getParkId(), f.getBuildingId());
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
     * 按楼栋查询楼层列表 (无分页, Building 弹窗内嵌子表用, 按 sorting 升序)
     */
    public Result<java.util.List<com.cloudhub.platform.space.domain.entity.Floor>> listByBuilding(Long buildingId) {
        java.util.List<com.cloudhub.platform.space.domain.entity.Floor> list = floorMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.cloudhub.platform.space.domain.entity.Floor>()
                        .eq(com.cloudhub.platform.space.domain.entity.Floor::getBuildingId, buildingId)
                        .eq(com.cloudhub.platform.space.domain.entity.Floor::getDeleted, 0)
                        .orderByAsc(com.cloudhub.platform.space.domain.entity.Floor::getSorting)
                        .orderByAsc(com.cloudhub.platform.space.domain.entity.Floor::getSerialCode));
        log.info("[FloorService] listByBuilding buildingId={} -> count={}", buildingId, list.size());
        return Result.ok(list);
    }
}