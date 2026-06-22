package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.LandNature;
import com.cloudhub.platform.space.mapper.LandNatureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 土地性质 Service (park-space 业务)
 * <p>W3.3 阶段: 土地性质 (LandNature) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LandNatureService {

    private final LandNatureMapper landNatureMapper;

    // ========== Query ==========

    public Result<PageResult<LandNature>> page(String keyword, Long parkId, Integer status,
                                                int pageNum, int pageSize) {
        LambdaQueryWrapper<LandNature> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(LandNature::getLandNatureName, keyword);
        }
        if (parkId != null) {
            w.eq(LandNature::getParkId, parkId);
        }
        if (status != null) {
            w.eq(LandNature::getStatus, status);
        }
        w.eq(LandNature::getDeleted, 0).orderByAsc(LandNature::getLandNatureCode);

        Page<LandNature> p = landNatureMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<LandNature> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[LandNatureService] page keyword={}, parkId={} -> total={}", keyword, parkId, p.getTotal());
        return Result.ok(result);
    }

    public Result<LandNature> getById(Long id) {
        LandNature l = landNatureMapper.selectById(id);
        if (l == null) throw new BizException("土地性质不存在");
        return Result.ok(l);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String landNatureName = requiredString(params, "landNatureName");

        Long count = landNatureMapper.selectCount(new LambdaQueryWrapper<LandNature>()
                .eq(LandNature::getParkId, parkId)
                .eq(LandNature::getLandNatureName, landNatureName)
                .eq(LandNature::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在土地性质 " + landNatureName);
        }

        LandNature l = new LandNature();
        l.setParkId(parkId);
        l.setLandNatureName(landNatureName);
        l.setLandNatureCode((String) params.get("landNatureCode"));
        l.setColor((String) params.get("color"));
        l.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        l.setTenantId(currentTenantId());

        landNatureMapper.insert(l);
        log.info("[LandNatureService] create: id={}, parkId={}, landNatureName={}",
                l.getId(), parkId, landNatureName);
        return Result.ok(l.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        LandNature l = landNatureMapper.selectById(id);
        if (l == null) throw new BizException("土地性质不存在");
        if (l.getDeleted() != null && l.getDeleted() == 1) throw new BizException("土地性质已删除");

        if (params.containsKey("landNatureName")) {
            String newName = (String) params.get("landNatureName");
            if (newName != null && !newName.isBlank()) {
                Long count = landNatureMapper.selectCount(new LambdaQueryWrapper<LandNature>()
                        .eq(LandNature::getParkId, l.getParkId())
                        .eq(LandNature::getLandNatureName, newName)
                        .ne(LandNature::getId, id)
                        .eq(LandNature::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + l.getParkId() + " 已存在土地性质 " + newName);
                }
                l.setLandNatureName(newName);
            }
        }
        if (params.containsKey("landNatureCode"))
            l.setLandNatureCode((String) params.get("landNatureCode"));
        if (params.containsKey("color"))
            l.setColor((String) params.get("color"));
        if (params.containsKey("status"))
            l.setStatus(((Number) params.get("status")).intValue());

        landNatureMapper.updateById(l);
        log.info("[LandNatureService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        LandNature l = landNatureMapper.selectById(id);
        if (l == null) throw new BizException("土地性质不存在");
        l.setDeleted(1);
        landNatureMapper.updateById(l);
        log.info("[LandNatureService] delete: id={}", id);
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