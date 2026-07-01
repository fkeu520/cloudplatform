package com.cloudhub.platform.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.FocusItem;
import com.cloudhub.platform.enterprise.mapper.FocusItemMapper;
import com.cloudhub.platform.enterprise.service.FocusItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 关注标签内容 Service 实现 (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Service
@RequiredArgsConstructor
public class FocusItemServiceImpl implements FocusItemService {

    private final FocusItemMapper mapper;

    @Override
    public IPage<FocusItem> pageList(Page<FocusItem> page, Long focusId, Integer status) {
        LambdaQueryWrapper<FocusItem> wrapper = new LambdaQueryWrapper<>();
        if (focusId != null) {
            wrapper.eq(FocusItem::getFocusId, focusId);
        }
        if (status != null) {
            wrapper.eq(FocusItem::getStatus, status);
        }
        wrapper.orderByAsc(FocusItem::getSorting);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public List<FocusItem> listByFocusId(Long focusId) {
        return mapper.selectList(
                new LambdaQueryWrapper<FocusItem>()
                        .eq(FocusItem::getFocusId, focusId)
                        .eq(FocusItem::getStatus, 1)
                        .orderByAsc(FocusItem::getSorting));
    }

    @Override
    @Transactional
    public FocusItem save(FocusItem entity) {
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional
    public boolean update(FocusItem entity) {
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }
}
