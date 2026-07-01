package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseIndustry;

/**
 * 企业行业类型 Service (V54)
 *
 * @author Sisyphus (csyh 迁移)
 */
public interface EnterpriseIndustryService {

    /**
     * 分页查询行业类型
     */
    IPage<EnterpriseIndustry> pageList(Page<EnterpriseIndustry> page, String keyword, String category, Integer status);

    /**
     * 按 ID 查询
     */
    EnterpriseIndustry getById(Long id);

    /**
     * 按 code 查询
     */
    EnterpriseIndustry getByCode(String code);

    /**
     * 新增
     */
    EnterpriseIndustry save(EnterpriseIndustry entity);

    /**
     * 更新
     */
    boolean update(EnterpriseIndustry entity);

    /**
     * 软删
     */
    boolean deleteById(Long id);
}
