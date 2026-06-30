package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseEntBind;
import com.cloudhub.platform.enterprise.mapper.EnterpriseEntBindMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业绑定关系 Service (park-enterprise Phase 1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseEntBindService {

    private final EnterpriseEntBindMapper bindMapper;

    /** 列出企业的所有绑定 */
    public Result<List<EnterpriseEntBind>> listByEnterprise(Long enterpriseId) {
        return Result.ok(bindMapper.selectList(
                new LambdaQueryWrapper<EnterpriseEntBind>()
                        .eq(EnterpriseEntBind::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseEntBind::getBindStatus, 1)
                        .orderByDesc(EnterpriseEntBind::getBindingAt)));
    }

    /** 列表所有 (admin) */
    public Result<List<EnterpriseEntBind>> listAll() {
        return Result.ok(bindMapper.selectList(
                new LambdaQueryWrapper<EnterpriseEntBind>().orderByDesc(EnterpriseEntBind::getBindingAt)));
    }

    /** 绑定 */
    public Result<EnterpriseEntBind> bind(EnterpriseEntBind bind, String createBy) {
        if (bind.getEnterpriseId() == null || bind.getBindType() == null || bind.getBindId() == null) {
            throw new IllegalArgumentException("enterpriseId, bindType, bindId 必填");
        }
        bind.setId(null);
        bind.setBindStatus(1);
        bind.setBindingAt(LocalDateTime.now());
        bind.setCreateBy(createBy);
        bind.setCreateTime(LocalDateTime.now());
        bindMapper.insert(bind);
        return Result.ok(bind);
    }

    /** 解绑 (bind_status=0 + unbound_at=now) */
    public Result<Void> unbind(Long id) {
        EnterpriseEntBind b = bindMapper.selectById(id);
        if (b == null) {
            throw new IllegalArgumentException("绑定关系不存在: " + id);
        }
        b.setBindStatus(0);
        b.setUnboundAt(LocalDateTime.now());
        bindMapper.updateById(b);
        return Result.ok();
    }

    /** 删除 (解绑后真的删) */
    public Result<Void> delete(Long id) {
        bindMapper.deleteById(id);
        return Result.ok();
    }
}
