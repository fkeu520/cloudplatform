package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.FocusItem;

import java.util.List;

/**
 * 关注标签内容 Service (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
public interface FocusItemService {

    IPage<FocusItem> pageList(Page<FocusItem> page, Long focusId, Integer status);

    List<FocusItem> listByFocusId(Long focusId);

    FocusItem save(FocusItem entity);

    boolean update(FocusItem entity);

    boolean deleteById(Long id);
}
