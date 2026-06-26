package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.Building;
import com.cloudhub.platform.space.mapper.BuildingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 园区楼宇 Service (park-space 业务, 从 park-property 迁移)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingMapper buildingMapper;

    // ========== Query ==========

    /**
     * 分页查询楼宇列表 (支持 parkId / areaId 级联过滤, 用于 4 实体联动下拉)
     */
    public Result<PageResult<Building>> page(String keyword, Long parkId, Long areaId, Integer status,
                                              int pageNum, int pageSize) {
        LambdaQueryWrapper<Building> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(w2 -> w2.like(Building::getBuildingNo, keyword)
                    .or().like(Building::getBuildingName, keyword));
        }
        if (parkId != null) {
            w.eq(Building::getParkId, parkId);
        }
        if (areaId != null) {
            w.eq(Building::getAreaId, areaId);
        }
        if (status != null) {
            w.eq(Building::getStatus, status);
        }
        w.eq(Building::getDeleted, 0).orderByAsc(Building::getBuildingNo);

        Page<Building> p = buildingMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Building> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[BuildingService] page keyword={}, parkId={}, areaId={}, status={} -> total={}",
                keyword, parkId, areaId, status, p.getTotal());
        return Result.ok(result);
    }

    public Result<Building> getById(Long id) {
        Building b = buildingMapper.selectById(id);
        if (b == null) throw new BizException("楼宇不存在");
        return Result.ok(b);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String buildingNo = requiredString(params, "buildingNo");

        // 校验: 同园区楼宇编号唯一
        Long count = buildingMapper.selectCount(new LambdaQueryWrapper<Building>()
                .eq(Building::getParkId, parkId)
                .eq(Building::getBuildingNo, buildingNo)
                .eq(Building::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在楼宇编号 " + buildingNo);
        }

        Building b = new Building();
        b.setParkId(parkId);
        b.setAreaId(ServiceUtils.toLong(params.get("areaId")));
        b.setBuildingCode((String) params.get("buildingCode"));
        b.setBuildingNo(buildingNo);
        b.setBuildingName((String) params.get("buildingName"));
        b.setFloorNumber(ServiceUtils.toIntOrDefault(params.get("floorNumber"), 1));
        b.setUnderground(ServiceUtils.toIntOrDefault(params.get("underground"), 0));
        b.setFloors(ServiceUtils.toIntOrDefault(params.get("floors"), 1));
        b.setAreaCovered(params.get("areaCovered") != null ? new BigDecimal(params.get("areaCovered").toString()) : null);
        b.setTotalArea(params.get("totalArea") != null ? new BigDecimal(params.get("totalArea").toString()) : null);
        b.setPropertyRight(ServiceUtils.toInt(params.get("propertyRight")));
        b.setBuildingSafety(ServiceUtils.toInt(params.get("buildingSafety")));
        b.setShareArea(params.get("shareArea") != null ? new BigDecimal(params.get("shareArea").toString()) : null);
        b.setLeaseMethod(ServiceUtils.toInt(params.get("leaseMethod")));
        b.setSorting(ServiceUtils.toIntOrDefault(params.get("sorting"), 0));
        b.setCertificate((String) params.get("certificate"));
        b.setImage((String) params.get("image"));
        b.setBuildYear(ServiceUtils.toInt(params.get("buildYear")));
        b.setManager((String) params.get("manager"));
        b.setManagerPhone((String) params.get("managerPhone"));
        b.setRemark((String) params.get("remark"));
        b.setStatus(ServiceUtils.toIntOrDefault(params.get("status"), 1));
        b.setTenantId(currentTenantId());

        buildingMapper.insert(b);
        log.info("[BuildingService] create: id={}, parkId={}, buildingNo={}", b.getId(), parkId, buildingNo);
        return Result.ok(b.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Building b = buildingMapper.selectById(id);
        if (b == null) throw new BizException("楼宇不存在");
        if (b.getDeleted() != null && b.getDeleted() == 1) throw new BizException("楼宇已删除");

        if (params.containsKey("parkId")) {
            b.setParkId(ServiceUtils.toLong(params.get("parkId")));
        }
        if (params.containsKey("areaId")) {
            b.setAreaId(ServiceUtils.toLong(params.get("areaId")));
        }
        if (params.containsKey("buildingCode")) {
            b.setBuildingCode((String) params.get("buildingCode"));
        }
        if (params.containsKey("buildingNo")) b.setBuildingNo((String) params.get("buildingNo"));
        if (params.containsKey("buildingName")) b.setBuildingName((String) params.get("buildingName"));
        if (params.containsKey("floorNumber")) b.setFloorNumber(ServiceUtils.toInt(params.get("floorNumber")));
        if (params.containsKey("underground")) b.setUnderground(ServiceUtils.toInt(params.get("underground")));
        if (params.containsKey("floors")) b.setFloors(ServiceUtils.toInt(params.get("floors")));
        if (params.containsKey("areaCovered") && params.get("areaCovered") != null) {
            b.setAreaCovered(new BigDecimal(params.get("areaCovered").toString()));
        }
        if (params.containsKey("totalArea") && params.get("totalArea") != null) {
            b.setTotalArea(new BigDecimal(params.get("totalArea").toString()));
        }
        if (params.containsKey("propertyRight")) b.setPropertyRight(ServiceUtils.toInt(params.get("propertyRight")));
        if (params.containsKey("buildingSafety")) b.setBuildingSafety(ServiceUtils.toInt(params.get("buildingSafety")));
        if (params.containsKey("shareArea") && params.get("shareArea") != null) {
            b.setShareArea(new BigDecimal(params.get("shareArea").toString()));
        }
        if (params.containsKey("leaseMethod")) b.setLeaseMethod(ServiceUtils.toInt(params.get("leaseMethod")));
        if (params.containsKey("sorting")) b.setSorting(ServiceUtils.toInt(params.get("sorting")));
        if (params.containsKey("certificate")) b.setCertificate((String) params.get("certificate"));
        if (params.containsKey("image")) b.setImage((String) params.get("image"));
        if (params.containsKey("buildYear")) b.setBuildYear(ServiceUtils.toInt(params.get("buildYear")));
        if (params.containsKey("manager")) b.setManager((String) params.get("manager"));
        if (params.containsKey("managerPhone")) b.setManager((String) params.get("managerPhone"));
        if (params.containsKey("remark")) b.setRemark((String) params.get("remark"));
        if (params.containsKey("status")) b.setStatus(ServiceUtils.toInt(params.get("status")));

        // 编号或园区变更: 重新校验 buildingNo 在当前 parkId 下的唯一性
        if (params.containsKey("buildingNo") || params.containsKey("parkId")) {
            Long count = buildingMapper.selectCount(new LambdaQueryWrapper<Building>()
                    .eq(Building::getParkId, b.getParkId())
                    .eq(Building::getBuildingNo, b.getBuildingNo())
                    .ne(Building::getId, id)
                    .eq(Building::getDeleted, 0));
            if (count != null && count > 0) {
                throw new BizException("园区 " + b.getParkId() + " 已存在楼宇编号 " + b.getBuildingNo());
            }
        }

        buildingMapper.updateById(b);
        log.info("[BuildingService] update: id={}, parkId={}", id, b.getParkId());
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Building b = buildingMapper.selectById(id);
        if (b == null) throw new BizException("楼宇不存在");
        if (b.getStatus() != null && b.getStatus() == 0) {
            throw new BizException("已停用的楼宇不可删除");
        }
        b.setDeleted(1);
        buildingMapper.updateById(b);
        log.info("[BuildingService] delete: id={}", id);
        return Result.ok();
    }

    // ========== Helpers ==========

    private Long requiredLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) throw new BizException("缺少必填字段: " + key);
        return ServiceUtils.toLong(v);
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
     * 校验同园区楼栋编号唯一性
     */
    public Result<Boolean> checkCode(Long parkId, String buildingCode, Long excludeId) {
        LambdaQueryWrapper<Building> w = new LambdaQueryWrapper<Building>()
                .eq(Building::getParkId, parkId)
                .eq(Building::getBuildingCode, buildingCode)
                .eq(Building::getDeleted, 0);
        if (excludeId != null) {
            w.ne(Building::getId, excludeId);
        }
        Long count = buildingMapper.selectCount(w);
        boolean available = count == null || count == 0;
        log.info("[BuildingService] checkCode parkId={}, buildingCode={} -> available={}", parkId, buildingCode, available);
        return Result.ok(available);
    }
}