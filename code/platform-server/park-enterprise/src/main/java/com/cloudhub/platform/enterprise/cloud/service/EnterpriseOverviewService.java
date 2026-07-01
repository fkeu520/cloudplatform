package com.cloudhub.platform.enterprise.cloud.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.mapper.EnterpriseOverviewMapper;
import com.cloudhub.platform.enterprise.cloud.model.EnterpriseOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业概览数据 Service
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Service
@RequiredArgsConstructor
public class EnterpriseOverviewService {

    private final EnterpriseOverviewMapper overviewMapper;

    public Result<Page<EnterpriseOverview>> page(Long enterpriseId, String keyword,
                                                  int pageNum, int pageSize) {
        LambdaQueryWrapper<EnterpriseOverview> w = new LambdaQueryWrapper<EnterpriseOverview>()
                .eq(enterpriseId != null, EnterpriseOverview::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(keyword), EnterpriseOverview::getEnterpriseName, keyword)
                .orderByAsc(EnterpriseOverview::getSortOrder);
        return Result.ok(overviewMapper.selectPage(Page.of(pageNum, pageSize), w));
    }

    public Result<EnterpriseOverview> detail(Long id) {
        EnterpriseOverview e = overviewMapper.selectById(id);
        if (e == null) throw new BizException("概览数据不存在: " + id);
        return Result.ok(e);
    }

    @Transactional
    public Result<EnterpriseOverview> create(EnterpriseOverview e, String createBy) {
        e.setId(null);
        if (e.getStatus() == null) e.setStatus(1);
        e.setCreateBy(createBy);
        overviewMapper.insert(e);
        return Result.ok(e);
    }

    @Transactional
    public Result<EnterpriseOverview> update(EnterpriseOverview e, String updateBy) {
        if (e.getId() == null) throw new BizException("更新必须有 ID");
        EnterpriseOverview exist = overviewMapper.selectById(e.getId());
        if (exist == null) throw new BizException("概览数据不存在: " + e.getId());
        e.setUpdateBy(updateBy);
        overviewMapper.updateById(e);
        return Result.ok(e);
    }

    @Transactional
    public Result<Void> delete(Long id) {
        if (overviewMapper.selectById(id) == null) {
            throw new BizException("概览数据不存在: " + id);
        }
        overviewMapper.deleteById(id);
        return Result.ok();
    }
}
