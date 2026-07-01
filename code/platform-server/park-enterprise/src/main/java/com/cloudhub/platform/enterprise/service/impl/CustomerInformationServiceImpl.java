package com.cloudhub.platform.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.CustomerInformation;
import com.cloudhub.platform.enterprise.mapper.CustomerInformationMapper;
import com.cloudhub.platform.enterprise.service.CustomerInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户信息 Service 实现 (V58)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Service
@RequiredArgsConstructor
public class CustomerInformationServiceImpl implements CustomerInformationService {

    private final CustomerInformationMapper mapper;

    @Override
    public IPage<CustomerInformation> pageList(Page<CustomerInformation> page, Long enterpriseId, Integer customerType, Integer status) {
        LambdaQueryWrapper<CustomerInformation> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(CustomerInformation::getEnterpriseId, enterpriseId);
        }
        if (customerType != null) {
            wrapper.eq(CustomerInformation::getCustomerType, customerType);
        }
        if (status != null) {
            wrapper.eq(CustomerInformation::getStatus, status);
        }
        wrapper.orderByDesc(CustomerInformation::getCreateTime);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public CustomerInformation getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public CustomerInformation save(CustomerInformation entity) {
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional
    public boolean update(CustomerInformation entity) {
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }
}
