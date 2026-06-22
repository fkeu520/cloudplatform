package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.SpaceCategory;
import com.cloudhub.platform.space.mapper.SpaceCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 空间类别 Service (park-space 业务)
 * <p>W3.5 阶段: 空间类别 (SpaceCategory) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceCategoryService {

    private final SpaceCategoryMapper spaceCategoryMapper;

    // ========== Query ==========

    public Result<PageResult<SpaceCategory>> page(String keyword, Long parkId, Integer status,
                                                    int pageNum, int pageSize) {
        LambdaQueryWrapper<SpaceCategory> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(SpaceCategory::getTypeName, keyword);
        }
        if (parkId != null) {
            w.eq(SpaceCategory::getParkId, parkId);
        }
        if (status != null) {
            w.eq(SpaceCategory::getStatus, status);
        }
        w.eq(SpaceCategory::getDeleted, 0).orderByAsc(SpaceCategory::getTypeName);

        Page<SpaceCategory> p = spaceCategoryMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<SpaceCategory> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[SpaceCategoryService] page keyword={}, parkId={} -> total={}", keyword, parkId, p.getTotal());
        return Result.ok(result);
    }

    public Result<SpaceCategory> getById(Long id) {
        SpaceCategory sc = spaceCategoryMapper.selectById(id);
        if (sc == null) throw new BizException("空间类别不存在");
        return Result.ok(sc);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String typeName = requiredString(params, "typeName");

        Long count = spaceCategoryMapper.selectCount(new LambdaQueryWrapper<SpaceCategory>()
                .eq(SpaceCategory::getParkId, parkId)
                .eq(SpaceCategory::getTypeName, typeName)
                .eq(SpaceCategory::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在空间类别 " + typeName);
        }

        SpaceCategory sc = new SpaceCategory();
        sc.setParkId(parkId);
        sc.setTypeName(typeName);
        sc.setTypeDescribe((String) params.get("typeDescribe"));
        sc.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        sc.setTenantId(currentTenantId());

        spaceCategoryMapper.insert(sc);
        log.info("[SpaceCategoryService] create: id={}, parkId={}, typeName={}", sc.getId(), parkId, typeName);
        return Result.ok(sc.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        SpaceCategory sc = spaceCategoryMapper.selectById(id);
        if (sc == null) throw new BizException("空间类别不存在");
        if (sc.getDeleted() != null && sc.getDeleted() == 1) throw new BizException("空间类别已删除");

        if (params.containsKey("typeName")) {
            String newName = (String) params.get("typeName");
            if (newName != null && !newName.isBlank()) {
                Long count = spaceCategoryMapper.selectCount(new LambdaQueryWrapper<SpaceCategory>()
                        .eq(SpaceCategory::getParkId, sc.getParkId())
                        .eq(SpaceCategory::getTypeName, newName)
                        .ne(SpaceCategory::getId, id)
                        .eq(SpaceCategory::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + sc.getParkId() + " 已存在空间类别 " + newName);
                }
                sc.setTypeName(newName);
            }
        }
        if (params.containsKey("typeDescribe"))
            sc.setTypeDescribe((String) params.get("typeDescribe"));
        if (params.containsKey("status"))
            sc.setStatus(((Number) params.get("status")).intValue());

        spaceCategoryMapper.updateById(sc);
        log.info("[SpaceCategoryService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        SpaceCategory sc = spaceCategoryMapper.selectById(id);
        if (sc == null) throw new BizException("空间类别不存在");
        sc.setDeleted(1);
        spaceCategoryMapper.updateById(sc);
        log.info("[SpaceCategoryService] delete: id={}", id);
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