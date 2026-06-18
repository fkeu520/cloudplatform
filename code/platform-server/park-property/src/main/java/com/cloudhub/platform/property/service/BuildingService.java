package com.cloudhub.platform.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
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
 *
 * <p>W3.2 阶段: 完整 CRUD (与 RoomService 模式对齐, 复用同一套范式).</p>
 *
 * <p>W3+ 阶段:
 * <ul>
 *   <li>关联 sys_room: 查询某楼的所有房间 (1:N)</li>
 *   <li>关联 sys_contract: 楼宇入驻率统计</li>
 *   <li>工单系统: 报修/巡检记录</li>
 * </ul>
 *
 * @author csyh fusion W3.2
 * @since 2026-06-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingMapper buildingMapper;

    // ========== Query ==========

    /**
     * 分页查询楼宇列表
     */
    public Result<PageResult<Building>> page(String keyword, Integer status,
                                              int pageNum, int pageSize) {
        LambdaQueryWrapper<Building> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Building::getBuildingNo, keyword)
                    .or().like(Building::getBuildingName, keyword);
        }
        if (status != null) {
            w.eq(Building::getStatus, status);
        }
        w.eq(Building::getDeleted, 0).orderByAsc(Building::getBuildingNo);

        Page<Building> p = buildingMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Building> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[BuildingService] page keyword={}, status={} -> total={}",
                keyword, status, p.getTotal());
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
        b.setFloors(params.get("floors") != null ? ((Number) params.get("floors")).intValue() : 1);
        b.setTotalArea(params.get("totalArea") != null ? new BigDecimal(params.get("totalArea").toString()) : null);
        b.setBuildYear(params.get("buildYear") != null ? ((Number) params.get("buildYear")).intValue() : null);
        b.setManager((String) params.get("manager"));
        b.setManagerPhone((String) params.get("managerPhone"));
        b.setRemark((String) params.get("remark"));
        b.setStatus(params.get("status") != null ? ((Number) params.get("status")).intValue() : 1);
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

        if (params.containsKey("buildingNo")) b.setBuildingNo((String) params.get("buildingNo"));
        if (params.containsKey("buildingName")) b.setBuildingName((String) params.get("buildingName"));
        if (params.containsKey("floors")) b.setFloors(((Number) params.get("floors")).intValue());
        if (params.containsKey("totalArea")) b.setTotalArea(new BigDecimal(params.get("totalArea").toString()));
        if (params.containsKey("buildYear")) b.setBuildYear(((Number) params.get("buildYear")).intValue());
        if (params.containsKey("manager")) b.setManager((String) params.get("manager"));
        if (params.containsKey("managerPhone")) b.setManagerPhone((String) params.get("managerPhone"));
        if (params.containsKey("remark")) b.setRemark((String) params.get("remark"));
        if (params.containsKey("status")) b.setStatus(((Number) params.get("status")).intValue());

        // 编号变更: 重新校验唯一性
        if (params.containsKey("buildingNo")) {
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
        log.info("[BuildingService] update: id={}", id);
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
