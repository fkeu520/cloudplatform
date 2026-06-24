package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.Space;
import com.cloudhub.platform.space.mapper.SpaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 空间 Service (park-space 业务)
 * <p>W3.5 阶段: 空间 (Space) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceMapper spaceMapper;

    // ========== Query ==========

    public Result<PageResult<Space>> page(String keyword, Long parkId, Long areaId, Long categoryId,
                                           int pageNum, int pageSize) {
        LambdaQueryWrapper<Space> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Space::getSpaceName, keyword);
        }
        if (parkId != null) {
            w.eq(Space::getParkId, parkId);
        }
        if (areaId != null) {
            w.eq(Space::getAreaId, areaId);
        }
        if (categoryId != null) {
            w.eq(Space::getCategoryId, categoryId);
        }
        w.eq(Space::getDeleted, 0).orderByAsc(Space::getSpaceName);

        Page<Space> p = spaceMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Space> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[SpaceService] page keyword={}, parkId={} -> total={}", keyword, parkId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Space> getById(Long id) {
        Space s = spaceMapper.selectById(id);
        if (s == null) throw new BizException("空间不存在");
        return Result.ok(s);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String spaceName = requiredString(params, "spaceName");

        Long count = spaceMapper.selectCount(new LambdaQueryWrapper<Space>()
                .eq(Space::getParkId, parkId)
                .eq(Space::getSpaceName, spaceName)
                .eq(Space::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在空间 " + spaceName);
        }

        Space s = new Space();
        s.setParkId(parkId);
        s.setSpaceName(spaceName);
        s.setSpaceDescribe((String) params.get("spaceDescribe"));
        s.setAreaId(params.get("areaId") != null
                ? ((Number) params.get("areaId")).longValue() : null);
        s.setCategoryId(params.get("categoryId") != null
                ? ((Number) params.get("categoryId")).longValue() : null);
        s.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        s.setTenantId(currentTenantId());

        spaceMapper.insert(s);
        log.info("[SpaceService] create: id={}, parkId={}, spaceName={}", s.getId(), parkId, spaceName);
        return Result.ok(s.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Space s = spaceMapper.selectById(id);
        if (s == null) throw new BizException("空间不存在");
        if (s.getDeleted() != null && s.getDeleted() == 1) throw new BizException("空间已删除");

        if (params.containsKey("spaceName")) {
            String newName = (String) params.get("spaceName");
            if (newName != null && !newName.isBlank()) {
                Long count = spaceMapper.selectCount(new LambdaQueryWrapper<Space>()
                        .eq(Space::getParkId, s.getParkId())
                        .eq(Space::getSpaceName, newName)
                        .ne(Space::getId, id)
                        .eq(Space::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + s.getParkId() + " 已存在空间 " + newName);
                }
                s.setSpaceName(newName);
            }
        }
        if (params.containsKey("spaceDescribe"))
            s.setSpaceDescribe((String) params.get("spaceDescribe"));
        if (params.containsKey("areaId"))
            s.setAreaId(ServiceUtils.toLong(params.get("areaId")));
        if (params.containsKey("categoryId"))
            s.setCategoryId(ServiceUtils.toLong(params.get("categoryId")));
        if (params.containsKey("status"))
            s.setStatus(ServiceUtils.toInt(params.get("status")));

        spaceMapper.updateById(s);
        log.info("[SpaceService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Space s = spaceMapper.selectById(id);
        if (s == null) throw new BizException("空间不存在");
        s.setDeleted(1);
        spaceMapper.updateById(s);
        log.info("[SpaceService] delete: id={}", id);
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
     * 校验同园区+分类下空间名称唯一性
     */
    public Result<Boolean> checkName(Long parkId, Long categoryId, String spaceName, Long excludeId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Space> w = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Space>()
                .eq(Space::getParkId, parkId)
                .eq(Space::getCategoryId, categoryId)
                .eq(Space::getSpaceName, spaceName)
                .eq(Space::getDeleted, 0);
        if (excludeId != null) {
            w.ne(Space::getId, excludeId);
        }
        Long count = spaceMapper.selectCount(w);
        boolean available = count == null || count == 0;
        log.info("[SpaceService] checkName parkId={}, categoryId={}, spaceName={} -> available={}", parkId, categoryId, spaceName, available);
        return Result.ok(available);
    }
}