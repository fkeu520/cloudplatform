package com.cloudhub.platform.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.property.domain.entity.Building;
import com.cloudhub.platform.property.mapper.BuildingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 园区楼宇 Service (park-property 业务)
 * <p>W3.2 阶段: 完整 CRUD (与 RoomService 模式对齐, 复用同一套范式).</p>
 * <p>W3+ 阶段:
 * <ul>
 *   <li>关联 sys_room: 查询某楼的所有房间 (1:N)</li>
 *   <li>关联 sys_contract: 楼宇入驻率统计</li>
 *   <li>工单系统: 报修/巡检记录</li>
 * </ul>
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
            w.like(Building::getBuildingNo, keyword)
                    .or().like(Building::getBuildingName, keyword);
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
        b.setBuildingNo(buildingNo);
        b.setBuildingName((String) params.get("buildingName"));
        b.setFloors(ServiceUtils.toIntOrDefault(params.get("floors"), 1));
        b.setTotalArea(params.get("totalArea") != null ? new BigDecimal(params.get("totalArea").toString()) : null);
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

        // 园区变更: 支持跨园区迁移 (前端 el-select 改动)
        // 园区变更后, 现有 buildingNo 在新园区可能冲突, 下面统一重新校验
        if (params.containsKey("parkId")) {
            b.setParkId(ServiceUtils.toLong(params.get("parkId")));
        }
        if (params.containsKey("buildingNo")) b.setBuildingNo((String) params.get("buildingNo"));
        if (params.containsKey("buildingName")) b.setBuildingName((String) params.get("buildingName"));
        if (params.containsKey("floors")) b.setFloors(ServiceUtils.toInt(params.get("floors")));
        if (params.containsKey("totalArea") && params.get("totalArea") != null) b.setTotalArea(new BigDecimal(params.get("totalArea").toString()));
        if (params.containsKey("buildYear")) b.setBuildYear(ServiceUtils.toInt(params.get("buildYear")));
        if (params.containsKey("manager")) b.setManager((String) params.get("manager"));
        if (params.containsKey("managerPhone")) b.setManagerPhone((String) params.get("managerPhone"));
        if (params.containsKey("remark")) b.setRemark((String) params.get("remark"));
        if (params.containsKey("status")) b.setStatus(ServiceUtils.toInt(params.get("status")));

        // 编号或园区变更: 都要重新校验 buildingNo 在当前 parkId 下的唯一性
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
     * 校验同园区楼栋编号唯一性 (V36 唯一索引 uk_park_building_code)
     */
    public Result<Boolean> checkCode(Long parkId, String buildingCode, Long excludeId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building> w = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building>()
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
