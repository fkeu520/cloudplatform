package com.cloudhub.platform.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.Focus;
import com.cloudhub.platform.enterprise.mapper.FocusMapper;
import com.cloudhub.platform.enterprise.service.FocusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 关注标签 Service 实现 (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Service
@RequiredArgsConstructor
public class FocusServiceImpl implements FocusService {

    private final FocusMapper mapper;

    @Override
    public IPage<Focus> pageList(Page<Focus> page, String keyword, Integer status) {
        LambdaQueryWrapper<Focus> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Focus::getName, keyword);
        }
        if (status != null) {
            wrapper.eq(Focus::getStatus, status);
        }
        wrapper.orderByAsc(Focus::getSorting);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public Focus getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Focus> listEnabled() {
        return mapper.selectList(
                new LambdaQueryWrapper<Focus>()
                        .eq(Focus::getStatus, 1)
                        .orderByAsc(Focus::getSorting));
    }

    @Override
    @Transactional
    public Focus save(Focus entity) {
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional
    public boolean update(Focus entity) {
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }
}
