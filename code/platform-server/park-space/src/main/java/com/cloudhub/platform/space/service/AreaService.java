package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.Area;
import com.cloudhub.platform.space.mapper.AreaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 区域 Service (park-space 业务)
 * <p>W3.3 阶段: 区域 (Area) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaMapper areaMapper;

    // ========== Query ==========

    public Result<PageResult<Area>> page(String keyword, Long parkId, Integer status,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<Area> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Area::getAreaName, keyword);
        }
        if (parkId != null) {
            w.eq(Area::getParkId, parkId);
        }
        if (status != null) {
            w.eq(Area::getStatus, status);
        }
        w.eq(Area::getDeleted, 0).orderByAsc(Area::getSorting).orderByAsc(Area::getCreateTime);

        Page<Area> p = areaMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Area> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[AreaService] page keyword={}, parkId={}, status={} -> total={}",
                keyword, parkId, status, p.getTotal());
        return Result.ok(result);
    }

    public Result<Area> getById(Long id) {
        Area a = areaMapper.selectById(id);
        if (a == null) throw new BizException("区域不存在");
        return Result.ok(a);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String areaName = requiredString(params, "areaName");

        // 校验: 同园区区域名称唯一
        Long count = areaMapper.selectCount(new LambdaQueryWrapper<Area>()
                .eq(Area::getParkId, parkId)
                .eq(Area::getAreaName, areaName)
                .eq(Area::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在区域 " + areaName);
        }

        Area a = new Area();
        a.setParkId(parkId);
        a.setAreaName(areaName);
        a.setAreaCovered(params.get("areaCovered") != null
                ? new BigDecimal(params.get("areaCovered").toString()) : null);
        a.setBuiltArea(params.get("builtArea") != null
                ? new BigDecimal(params.get("builtArea").toString()) : null);
        a.setFunctionArea((String) params.get("functionArea"));
        a.setBuildingAmount(params.get("buildingAmount") != null
                ? ServiceUtils.toInt(params.get("buildingAmount")) : 0);
        a.setRoomAmount(params.get("roomAmount") != null
                ? ServiceUtils.toInt(params.get("roomAmount")) : 0);
        a.setIsVirtual(params.get("isVirtual") != null
                ? ServiceUtils.toInt(params.get("isVirtual")) : 0);
        a.setSorting(params.get("sorting") != null
                ? ServiceUtils.toInt(params.get("sorting")) : 0);
        a.setStatus(params.get("status") != null
                ? ServiceUtils.toInt(params.get("status")) : 1);
        a.setTenantId(currentTenantId());

        areaMapper.insert(a);
        log.info("[AreaService] create: id={}, parkId={}, areaName={}", a.getId(), parkId, areaName);
        return Result.ok(a.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Area a = areaMapper.selectById(id);
        if (a == null) throw new BizException("区域不存在");
        if (a.getDeleted() != null && a.getDeleted() == 1) throw new BizException("区域已删除");

        if (params.containsKey("areaName")) {
            String newName = (String) params.get("areaName");
            if (newName != null && !newName.isBlank()) {
                // 名称变更: 重新校验唯一性
                Long count = areaMapper.selectCount(new LambdaQueryWrapper<Area>()
                        .eq(Area::getParkId, a.getParkId())
                        .eq(Area::getAreaName, newName)
                        .ne(Area::getId, id)
                        .eq(Area::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + a.getParkId() + " 已存在区域 " + newName);
                }
                a.setAreaName(newName);
            }
        }
        if (params.containsKey("areaCovered"))
            a.setAreaCovered(new BigDecimal(params.get("areaCovered").toString()));
        if (params.containsKey("builtArea"))
            a.setBuiltArea(new BigDecimal(params.get("builtArea").toString()));
        if (params.containsKey("functionArea"))
            a.setFunctionArea((String) params.get("functionArea"));
        if (params.containsKey("buildingAmount"))
            a.setBuildingAmount(ServiceUtils.toInt(params.get("buildingAmount")));
        if (params.containsKey("roomAmount"))
            a.setRoomAmount(ServiceUtils.toInt(params.get("roomAmount")));
        if (params.containsKey("isVirtual"))
            a.setIsVirtual(ServiceUtils.toInt(params.get("isVirtual")));
        if (params.containsKey("sorting"))
            a.setSorting(ServiceUtils.toInt(params.get("sorting")));
        if (params.containsKey("status"))
            a.setStatus(ServiceUtils.toInt(params.get("status")));

        areaMapper.updateById(a);
        log.info("[AreaService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Area a = areaMapper.selectById(id);
        if (a == null) throw new BizException("区域不存在");
        a.setDeleted(1);
        areaMapper.updateById(a);
        log.info("[AreaService] delete: id={}", id);
        return Result.ok();
    }


    /**
     * 校验同园区区域名称唯一性
     * @param parkId 园区 ID
     * @param areaName 区域名称
     * @param excludeId 排除的 ID (编辑时传自身)
     * @return true=名称可用, false=已存在
     */
    public Result<Boolean> checkName(Long parkId, String areaName, Long excludeId) {
        LambdaQueryWrapper<Area> w = new LambdaQueryWrapper<Area>()
                .eq(Area::getParkId, parkId)
                .eq(Area::getAreaName, areaName)
                .eq(Area::getDeleted, 0);
        if (excludeId != null) {
            w.ne(Area::getId, excludeId);
        }
        Long count = areaMapper.selectCount(w);
        boolean available = count == null || count == 0;
        log.info("[AreaService] checkName parkId={}, areaName={} -> available={}", parkId, areaName, available);
        return Result.ok(available);
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