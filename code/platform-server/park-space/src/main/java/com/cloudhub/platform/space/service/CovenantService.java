package com.cloudhub.platform.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.park.common.base.util.ServiceUtils;
import com.cloudhub.platform.space.domain.entity.Covenant;
import com.cloudhub.platform.space.mapper.CovenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 合同房间关联 Service (park-space 业务)
 * <p>W3.4 阶段: 合同-房间关联 (Covenant) 简单 CRUD.</p>
 * <p>W3 阶段不做跨模块合同校验 (park-contract 模块合同有效性).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CovenantService {

    private final CovenantMapper covenantMapper;

    // ========== Query ==========

    public Result<PageResult<Covenant>> page(String keyword, Long parkId, Long roomId, Integer covenantType,
                                              int pageNum, int pageSize) {
        LambdaQueryWrapper<Covenant> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            // covenant 表无名称字段, 仅支持 covenantId/customerId 数值模糊
            try {
                Long id = Long.parseLong(keyword);
                w.eq(Covenant::getCovenantId, id).or().eq(Covenant::getCustomerId, id);
            } catch (NumberFormatException ignored) {
                // 非数字 keyword 直接忽略
            }
        }
        if (parkId != null) {
            w.eq(Covenant::getParkId, parkId);
        }
        if (roomId != null) {
            w.eq(Covenant::getRoomId, roomId);
        }
        if (covenantType != null) {
            w.eq(Covenant::getCovenantType, covenantType);
        }
        w.eq(Covenant::getDeleted, 0).orderByDesc(Covenant::getCreateTime);

        Page<Covenant> p = covenantMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<Covenant> result = new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
        log.info("[CovenantService] page parkId={}, roomId={} -> total={}", parkId, roomId, p.getTotal());
        return Result.ok(result);
    }

    public Result<Covenant> getById(Long id) {
        Covenant c = covenantMapper.selectById(id);
        if (c == null) throw new BizException("合同关联不存在");
        return Result.ok(c);
    }

    // ========== Create ==========

    @Transactional
    public Result<Long> create(Map<String, Object> params) {
        Long parkId = requiredLong(params, "parkId");
        Long roomId = requiredLong(params, "roomId");
        Long covenantId = requiredLong(params, "covenantId");

        Covenant c = new Covenant();
        c.setParkId(parkId);
        c.setRoomId(roomId);
        c.setCovenantId(covenantId);
        c.setCovenantType(params.get("covenantType") != null
                ? ServiceUtils.toInt(params.get("covenantType")) : 0);
        c.setCustomerId(params.get("customerId") != null
                ? ServiceUtils.toLong(params.get("customerId")) : null);
        c.setStatus(params.get("status") != null
                ? ServiceUtils.toInt(params.get("status")) : 1);
        c.setTenantId(currentTenantId());

        covenantMapper.insert(c);
        log.info("[CovenantService] create: id={}, parkId={}, roomId={}, covenantId={}",
                c.getId(), parkId, roomId, covenantId);
        return Result.ok(c.getId());
    }

    // ========== Update ==========

    @Transactional
    public Result<Void> update(Long id, Map<String, Object> params) {
        Covenant c = covenantMapper.selectById(id);
        if (c == null) throw new BizException("合同关联不存在");
        if (c.getDeleted() != null && c.getDeleted() == 1) throw new BizException("合同关联已删除");

        if (params.containsKey("covenantId"))
            c.setCovenantId(ServiceUtils.toLong(params.get("covenantId")));
        if (params.containsKey("covenantType"))
            c.setCovenantType(ServiceUtils.toInt(params.get("covenantType")));
        if (params.containsKey("customerId"))
            c.setCustomerId(ServiceUtils.toLong(params.get("customerId")));
        if (params.containsKey("roomId"))
            c.setRoomId(ServiceUtils.toLong(params.get("roomId")));
        if (params.containsKey("status"))
            c.setStatus(ServiceUtils.toInt(params.get("status")));

        covenantMapper.updateById(c);
        log.info("[CovenantService] update: id={}", id);
        return Result.ok();
    }

    // ========== Delete (软删除) ==========

    @Transactional
    public Result<Void> delete(Long id) {
        Covenant c = covenantMapper.selectById(id);
        if (c == null) throw new BizException("合同关联不存在");
        c.setDeleted(1);
        covenantMapper.updateById(c);
        log.info("[CovenantService] delete: id={}", id);
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

    private Long currentTenantId() {
        Long tid = TenantContextHolder.getTenantId();
        return tid != null ? tid : 1L;
    }
}