package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.Massif;
import com.cloudhub.platform.space.mapper.MassifMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 地块 Service (park-space 业务)
 * <p>W3.5 阶段: 地块 (Massif) 简单 CRUD + 同园区地块编号唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MassifService {

    private final MassifMapper massifMapper;

    // ========== Query ==========

    public Result<PageResult<Massif>> page(String keyword, Long parkId, Integer status,
                                            int pageNum, int pageSize) {
        LambdaQueryWrapper<Massif> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Massif::getMassifName, keyword)
                    .or().like(Massif::getMassifCode, keyword);
        }
        if (parkId != null) {
            w.eq(Massif::getParkId, parkId);
        }
        if (status != null) {
            w.eq(Massif::getStatus, status);
        }
        w.eq(Massif::getDeleted, 0).orderByAsc(Massif::getMassifCode);

        Page<Massif> p = massifMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Massif> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[MassifService] page keyword={}, parkId={} -> total={}", keyword, parkId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Massif> getById(Long id) {
        Massif m = massifMapper.selectById(id);
        if (m == null) throw new BizException("地块不存在");
        return Result.ok(m);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String massifCode = requiredString(params, "massifCode");

        // 校验: 同园区地块编号唯一
        Long count = massifMapper.selectCount(new LambdaQueryWrapper<Massif>()
                .eq(Massif::getParkId, parkId)
                .eq(Massif::getMassifCode, massifCode)
                .eq(Massif::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在地块编号 " + massifCode);
        }

        Massif m = new Massif();
        m.setParkId(parkId);
        m.setMassifCode(massifCode);
        m.setMassifName((String) params.get("massifName"));
        m.setMassifArea(params.get("massifArea") != null
                ? new BigDecimal(params.get("massifArea").toString()) : null);
        m.setUseYear(params.get("useYear") != null
                ? ServiceUtils.toInt(params.get("useYear")) : null);
        m.setLandNatureId(params.get("landNatureId") != null
                ? ServiceUtils.toLong(params.get("landNatureId")) : null);
        m.setPlanUseId(params.get("planUseId") != null
                ? ServiceUtils.toLong(params.get("planUseId")) : null);
        m.setAssetType((String) params.get("assetType"));
        m.setMassifDesc((String) params.get("massifDesc"));
        m.setAddress((String) params.get("address"));
        m.setStatus(params.get("status") != null
                ? ServiceUtils.toInt(params.get("status")) : 1);
        m.setTenantId(currentTenantId());

        massifMapper.insert(m);
        log.info("[MassifService] create: id={}, parkId={}, massifCode={}", m.getId(), parkId, massifCode);
        return Result.ok(m.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Massif m = massifMapper.selectById(id);
        if (m == null) throw new BizException("地块不存在");
        if (m.getDeleted() != null && m.getDeleted() == 1) throw new BizException("地块已删除");

        if (params.containsKey("massifCode")) {
            String newCode = (String) params.get("massifCode");
            if (newCode != null && !newCode.isBlank()) {
                Long count = massifMapper.selectCount(new LambdaQueryWrapper<Massif>()
                        .eq(Massif::getParkId, m.getParkId())
                        .eq(Massif::getMassifCode, newCode)
                        .ne(Massif::getId, id)
                        .eq(Massif::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + m.getParkId() + " 已存在地块编号 " + newCode);
                }
                m.setMassifCode(newCode);
            }
        }
        if (params.containsKey("massifName"))
            m.setMassifName((String) params.get("massifName"));
        if (params.containsKey("massifArea") && params.get("massifArea") != null)
            m.setMassifArea(new BigDecimal(params.get("massifArea").toString()));
        if (params.containsKey("useYear"))
            m.setUseYear(ServiceUtils.toInt(params.get("useYear")));
        if (params.containsKey("landNatureId"))
            m.setLandNatureId(ServiceUtils.toLong(params.get("landNatureId")));
        if (params.containsKey("planUseId"))
            m.setPlanUseId(ServiceUtils.toLong(params.get("planUseId")));
        if (params.containsKey("assetType"))
            m.setAssetType((String) params.get("assetType"));
        if (params.containsKey("massifDesc"))
            m.setMassifDesc((String) params.get("massifDesc"));
        if (params.containsKey("address"))
            m.setAddress((String) params.get("address"));
        if (params.containsKey("status"))
            m.setStatus(ServiceUtils.toInt(params.get("status")));

        massifMapper.updateById(m);
        log.info("[MassifService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Massif m = massifMapper.selectById(id);
        if (m == null) throw new BizException("地块不存在");
        m.setDeleted(1);
        massifMapper.updateById(m);
        log.info("[MassifService] delete: id={}", id);
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
}