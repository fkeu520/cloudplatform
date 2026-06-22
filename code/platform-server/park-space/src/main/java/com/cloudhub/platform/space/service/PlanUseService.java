package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.PlanUse;
import com.cloudhub.platform.space.mapper.PlanUseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 规划用途 Service (park-space 业务)
 * <p>W3.3 阶段: 规划用途 (PlanUse) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanUseService {

    private final PlanUseMapper planUseMapper;

    // ========== Query ==========

    public Result<PageResult<PlanUse>> page(String keyword, Long parkId, Integer status,
                                              int pageNum, int pageSize) {
        LambdaQueryWrapper<PlanUse> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(PlanUse::getPlanUseName, keyword);
        }
        if (parkId != null) {
            w.eq(PlanUse::getParkId, parkId);
        }
        if (status != null) {
            w.eq(PlanUse::getStatus, status);
        }
        w.eq(PlanUse::getDeleted, 0).orderByAsc(PlanUse::getPlanUseCode);

        Page<PlanUse> p = planUseMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<PlanUse> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[PlanUseService] page keyword={}, parkId={} -> total={}", keyword, parkId, p.getTotal());
        return Result.ok(result);
    }

    public Result<PlanUse> getById(Long id) {
        PlanUse p = planUseMapper.selectById(id);
        if (p == null) throw new BizException("规划用途不存在");
        return Result.ok(p);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String planUseName = requiredString(params, "planUseName");

        Long count = planUseMapper.selectCount(new LambdaQueryWrapper<PlanUse>()
                .eq(PlanUse::getParkId, parkId)
                .eq(PlanUse::getPlanUseName, planUseName)
                .eq(PlanUse::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在规划用途 " + planUseName);
        }

        PlanUse p = new PlanUse();
        p.setParkId(parkId);
        p.setPlanUseName(planUseName);
        p.setPlanUseCode((String) params.get("planUseCode"));
        p.setColor((String) params.get("color"));
        p.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        p.setTenantId(currentTenantId());

        planUseMapper.insert(p);
        log.info("[PlanUseService] create: id={}, parkId={}, planUseName={}", p.getId(), parkId, planUseName);
        return Result.ok(p.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        PlanUse p = planUseMapper.selectById(id);
        if (p == null) throw new BizException("规划用途不存在");
        if (p.getDeleted() != null && p.getDeleted() == 1) throw new BizException("规划用途已删除");

        if (params.containsKey("planUseName")) {
            String newName = (String) params.get("planUseName");
            if (newName != null && !newName.isBlank()) {
                Long count = planUseMapper.selectCount(new LambdaQueryWrapper<PlanUse>()
                        .eq(PlanUse::getParkId, p.getParkId())
                        .eq(PlanUse::getPlanUseName, newName)
                        .ne(PlanUse::getId, id)
                        .eq(PlanUse::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + p.getParkId() + " 已存在规划用途 " + newName);
                }
                p.setPlanUseName(newName);
            }
        }
        if (params.containsKey("planUseCode"))
            p.setPlanUseCode((String) params.get("planUseCode"));
        if (params.containsKey("color"))
            p.setColor((String) params.get("color"));
        if (params.containsKey("status"))
            p.setStatus(((Number) params.get("status")).intValue());

        planUseMapper.updateById(p);
        log.info("[PlanUseService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        PlanUse p = planUseMapper.selectById(id);
        if (p == null) throw new BizException("规划用途不存在");
        p.setDeleted(1);
        planUseMapper.updateById(p);
        log.info("[PlanUseService] delete: id={}", id);
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