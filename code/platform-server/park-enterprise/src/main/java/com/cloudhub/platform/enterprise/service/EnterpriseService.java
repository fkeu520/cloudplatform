package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.Enterprise;
import com.cloudhub.platform.enterprise.mapper.EnterpriseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 企业档案 Service (park-enterprise Phase 1)
 *
 * <p>csyh IEnterpriseService + EnterpriseServiceImpl 合并 — 平台标准单类风格.
 * <p>Phase 1: 基础 CRUD + 状态切换, 无云企库对接.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-06-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;

    /** 分页列表 */
    public Result<PageResult<Enterprise>> page(String keyword, Integer status,
                                                int pageNum, int pageSize) {
        LambdaQueryWrapper<Enterprise> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Enterprise::getName, keyword);
        }
        if (status != null) {
            w.eq(Enterprise::getStatus, status);
        }
        w.orderByDesc(Enterprise::getCreateTime);

        Page<Enterprise> page = enterpriseMapper.selectPage(
                Page.of(pageNum, pageSize), w);
        PageResult<Enterprise> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
        return Result.ok(result);
    }

    /** 详情 */
    public Result<Enterprise> detail(Long id) {
        Enterprise e = enterpriseMapper.selectById(id);
        if (e == null) {
            throw new BizException("企业不存在: " + id);
        }
        return Result.ok(e);
    }

    /** 新增 */
    @Transactional
    public Result<Enterprise> create(Enterprise e, String createBy) {
        validateRequiredFields(e);
        e.setId(null); // 雪花 ID 自动生成
        e.setStatus(1);  // 默认启用
        e.setIsSync(0);
        e.setIsFill(0);
        e.setCreateBy(createBy);
        e.setCreateTime(LocalDateTime.now());
        enterpriseMapper.insert(e);
        log.info("新增企业成功 id={} name={}", e.getId(), e.getName());
        return Result.ok(e);
    }

    /** 更新 (审计字段自动) */
    @Transactional
    public Result<Enterprise> update(Enterprise e, String updateBy) {
        if (e.getId() == null) {
            throw new BizException("更新必须有 ID");
        }
        if (enterpriseMapper.selectById(e.getId()) == null) {
            throw new BizException("企业不存在: " + e.getId());
        }
        e.setUpdateBy(updateBy);
        e.setUpdateTime(LocalDateTime.now());
        // 不更新 createBy/createTime
        enterpriseMapper.updateById(e);
        return Result.ok(e);
    }

    /** 逻辑删除 */
    @Transactional
    public Result<Void> delete(Long id) {
        if (enterpriseMapper.selectById(id) == null) {
            throw new BizException("企业不存在: " + id);
        }
        enterpriseMapper.deleteById(id); // MP 自动 logical delete
        log.info("逻辑删除企业 id={}", id);
        return Result.ok();
    }

    /** 启用/停用 */
    @Transactional
    public Result<Void> setStatus(Long id, Integer status) {
        Enterprise e = enterpriseMapper.selectById(id);
        if (e == null) {
            throw new BizException("企业不存在: " + id);
        }
        e.setStatus(status);
        enterpriseMapper.updateById(e);
        return Result.ok();
    }

    /** 内部: 校验必填字段 (csyh RequiredException 改造) */
    private void validateRequiredFields(Enterprise e) {
        if (e.getName() == null || e.getName().isBlank()) {
            throw new BizException("企业名称不能为空");
        }
        // 信用代码 / 纳税人识别号 / 法人 等可后续扩展为非空校验
    }
}
