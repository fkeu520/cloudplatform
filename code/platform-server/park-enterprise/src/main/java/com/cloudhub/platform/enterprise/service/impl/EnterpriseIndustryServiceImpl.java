package com.cloudhub.platform.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseIndustry;
import com.cloudhub.platform.enterprise.mapper.EnterpriseIndustryMapper;
import com.cloudhub.platform.enterprise.service.EnterpriseIndustryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 企业行业类型 Service 实现 (V54)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Service
@RequiredArgsConstructor
public class EnterpriseIndustryServiceImpl implements EnterpriseIndustryService {

    private final EnterpriseIndustryMapper mapper;

    @Override
    public IPage<EnterpriseIndustry> pageList(Page<EnterpriseIndustry> page, String keyword, String category, Integer status) {
        LambdaQueryWrapper<EnterpriseIndustry> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(EnterpriseIndustry::getCode, keyword)
                    .or().like(EnterpriseIndustry::getCategory, keyword)
                    .or().like(EnterpriseIndustry::getCategoryBig, keyword)
                    .or().like(EnterpriseIndustry::getCategoryMiddle, keyword)
                    .or().like(EnterpriseIndustry::getCategorySmall, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(EnterpriseIndustry::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(EnterpriseIndustry::getStatus, status);
        }
        wrapper.orderByAsc(EnterpriseIndustry::getCode);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public EnterpriseIndustry getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public EnterpriseIndustry getByCode(String code) {
        return mapper.selectOne(new LambdaQueryWrapper<EnterpriseIndustry>().eq(EnterpriseIndustry::getCode, code));
    }

    @Override
    @Transactional
    public EnterpriseIndustry save(EnterpriseIndustry entity) {
        // code 全租户唯一校验
        EnterpriseIndustry exist = mapper.selectOne(
                new LambdaQueryWrapper<EnterpriseIndustry>()
                        .eq(EnterpriseIndustry::getCode, entity.getCode())
                        .eq(EnterpriseIndustry::getTenantId, entity.getTenantId()));
        if (exist != null) {
            throw new IllegalArgumentException("行业代码已存在: " + entity.getCode());
        }
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional
    public boolean update(EnterpriseIndustry entity) {
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        // 逻辑删除 (MyBatis-Plus @TableLogic 自动处理)
        return mapper.deleteById(id) > 0;
    }
}
