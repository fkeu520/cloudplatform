package com.cloudhub.platform.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseFocus;
import com.cloudhub.platform.enterprise.mapper.EnterpriseFocusMapper;
import com.cloudhub.platform.enterprise.service.EnterpriseFocusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业关注标签 Service 实现 (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Service
@RequiredArgsConstructor
public class EnterpriseFocusServiceImpl implements EnterpriseFocusService {

    private final EnterpriseFocusMapper mapper;

    @Override
    public IPage<EnterpriseFocus> pageList(Page<EnterpriseFocus> page, Long enterpriseId, Long focusId) {
        LambdaQueryWrapper<EnterpriseFocus> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(EnterpriseFocus::getEnterpriseId, enterpriseId);
        }
        if (focusId != null) {
            wrapper.eq(EnterpriseFocus::getFocusId, focusId);
        }
        wrapper.orderByDesc(EnterpriseFocus::getCreateTime);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public EnterpriseFocus save(EnterpriseFocus entity) {
        // 解析 focus_item_names 从 focus_items ID 列表
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional
    public boolean update(EnterpriseFocus entity) {
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean remove(LambdaQueryWrapper<EnterpriseFocus> wrapper) {
        return mapper.delete(wrapper) > 0;
    }
}
