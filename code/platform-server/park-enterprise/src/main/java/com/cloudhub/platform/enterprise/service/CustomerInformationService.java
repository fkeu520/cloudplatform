package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.CustomerInformation;

/**
 * 客户信息 Service (V58)
 *
 * <p>50+ 字段的拓展调查表, 重点是产业领域/知识产权/财务/需求/物理 5 大块。
 *
 * @author Sisyphus (csyh 迁移)
 */
public interface CustomerInformationService {

    IPage<CustomerInformation> pageList(Page<CustomerInformation> page, Long enterpriseId, Integer customerType, Integer status);

    CustomerInformation getById(Long id);

    CustomerInformation save(CustomerInformation entity);

    boolean update(CustomerInformation entity);

    boolean deleteById(Long id);
}
