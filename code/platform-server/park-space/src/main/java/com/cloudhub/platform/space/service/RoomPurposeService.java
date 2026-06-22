package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomPurpose;
import com.cloudhub.platform.space.mapper.RoomPurposeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 房源用途 Service (park-space 业务)
 * <p>W3.6 阶段: 房源用途 (RoomPurpose) 简单 CRUD + 同园区名称唯一校验.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPurposeService {

    private final RoomPurposeMapper roomPurposeMapper;

    // ========== Query ==========

    public Result<PageResult<RoomPurpose>> page(String keyword, Long parkId, Integer status,
                                                 int pageNum, int pageSize) {
        LambdaQueryWrapper<RoomPurpose> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(RoomPurpose::getPurposeName, keyword);
        }
        if (parkId != null) {
            w.eq(RoomPurpose::getParkId, parkId);
        }
        if (status != null) {
            w.eq(RoomPurpose::getStatus, status);
        }
        w.eq(RoomPurpose::getDeleted, 0).orderByAsc(RoomPurpose::getPurposeName);

        Page<RoomPurpose> p = roomPurposeMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<RoomPurpose> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        return Result.ok(result);
    }

    public Result<RoomPurpose> getById(Long id) {
        RoomPurpose p = roomPurposeMapper.selectById(id);
        if (p == null) throw new BizException("房源用途不存在");
        return Result.ok(p);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        String purposeName = requiredString(params, "purposeName");

        Long count = roomPurposeMapper.selectCount(new LambdaQueryWrapper<RoomPurpose>()
                .eq(RoomPurpose::getParkId, parkId)
                .eq(RoomPurpose::getPurposeName, purposeName)
                .eq(RoomPurpose::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BizException("园区 " + parkId + " 已存在用途 " + purposeName);
        }

        RoomPurpose p = new RoomPurpose();
        p.setParkId(parkId);
        p.setPurposeName(purposeName);
        p.setStatus(params.get("status") != null
                ? ((Number) params.get("status")).intValue() : 1);
        p.setTenantId(currentTenantId());

        roomPurposeMapper.insert(p);
        log.info("[RoomPurposeService] create: id={}, parkId={}, purposeName={}", p.getId(), parkId, purposeName);
        return Result.ok(p.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        RoomPurpose p = roomPurposeMapper.selectById(id);
        if (p == null) throw new BizException("房源用途不存在");
        if (p.getDeleted() != null && p.getDeleted() == 1) throw new BizException("房源用途已删除");

        if (params.containsKey("purposeName")) {
            String newName = (String) params.get("purposeName");
            if (newName != null && !newName.isBlank()) {
                Long count = roomPurposeMapper.selectCount(new LambdaQueryWrapper<RoomPurpose>()
                        .eq(RoomPurpose::getParkId, p.getParkId())
                        .eq(RoomPurpose::getPurposeName, newName)
                        .ne(RoomPurpose::getId, id)
                        .eq(RoomPurpose::getDeleted, 0));
                if (count != null && count > 0) {
                    throw new BizException("园区 " + p.getParkId() + " 已存在用途 " + newName);
                }
                p.setPurposeName(newName);
            }
        }
        if (params.containsKey("status"))
            p.setStatus(((Number) params.get("status")).intValue());

        roomPurposeMapper.updateById(p);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        RoomPurpose p = roomPurposeMapper.selectById(id);
        if (p == null) throw new BizException("房源用途不存在");
        p.setDeleted(1);
        roomPurposeMapper.updateById(p);
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