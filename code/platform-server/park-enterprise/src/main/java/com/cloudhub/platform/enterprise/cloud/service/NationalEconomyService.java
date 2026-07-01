package com.cloudhub.platform.enterprise.cloud.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.mapper.NationalEconomyMapper;
import com.cloudhub.platform.enterprise.cloud.model.NationalEconomy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 国民经济行业分类 Service
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Service
@RequiredArgsConstructor
public class NationalEconomyService {

    private final NationalEconomyMapper nationalEconomyMapper;

    public Result<Page<NationalEconomy>> page(String keyword, Integer level, String parentCode,
                                               int pageNum, int pageSize) {
        LambdaQueryWrapper<NationalEconomy> w = new LambdaQueryWrapper<NationalEconomy>()
                .like(StrUtil.isNotBlank(keyword), NationalEconomy::getName, keyword)
                .or(StrUtil.isNotBlank(keyword))
                .like(StrUtil.isNotBlank(keyword), NationalEconomy::getCode, keyword)
                .eq(level != null, NationalEconomy::getLevel, level)
                .eq(StrUtil.isNotBlank(parentCode), NationalEconomy::getParentCode, parentCode)
                .orderByAsc(NationalEconomy::getLevel)
                .orderByAsc(NationalEconomy::getSortOrder);
        return Result.ok(nationalEconomyMapper.selectPage(Page.of(pageNum, pageSize), w));
    }

    public Result<NationalEconomy> detail(Long id) {
        NationalEconomy e = nationalEconomyMapper.selectById(id);
        if (e == null) throw new BizException("行业分类不存在: " + id);
        return Result.ok(e);
    }

    @Transactional
    public Result<NationalEconomy> create(NationalEconomy e, String createBy) {
        e.setId(null);
        if (e.getStatus() == null) e.setStatus(1);
        e.setCreateBy(createBy);
        nationalEconomyMapper.insert(e);
        return Result.ok(e);
    }

    @Transactional
    public Result<NationalEconomy> update(NationalEconomy e, String updateBy) {
        if (e.getId() == null) throw new BizException("更新必须有 ID");
        NationalEconomy exist = nationalEconomyMapper.selectById(e.getId());
        if (exist == null) throw new BizException("行业分类不存在: " + e.getId());
        e.setUpdateBy(updateBy);
        nationalEconomyMapper.updateById(e);
        return Result.ok(e);
    }

    @Transactional
    public Result<Void> delete(Long id) {
        if (nationalEconomyMapper.selectById(id) == null) {
            throw new BizException("行业分类不存在: " + id);
        }
        nationalEconomyMapper.deleteById(id);
        return Result.ok();
    }
}
