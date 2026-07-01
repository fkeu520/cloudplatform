package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseFocus;

/**
 * 企业关注标签 Service (V57)
 *
 * <p>企业选择关注标签, 同时选择该标签下 1-N 个内容项。
 *
 * @author Sisyphus (csyh 迁移)
 */
public interface EnterpriseFocusService {

    IPage<EnterpriseFocus> pageList(Page<EnterpriseFocus> page, Long enterpriseId, Long focusId);

    EnterpriseFocus save(EnterpriseFocus entity);

    boolean update(EnterpriseFocus entity);

    boolean deleteById(Long id);

    boolean remove(LambdaQueryWrapper<EnterpriseFocus> wrapper);
}
