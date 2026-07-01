package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.enterprise.domain.entity.Focus;

import java.util.List;

/**
 * 关注标签 Service (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
public interface FocusService {

    IPage<Focus> pageList(Page<Focus> page, String keyword, Integer status);

    Focus getById(Long id);

    List<Focus> listEnabled();

    Focus save(Focus entity);

    boolean update(Focus entity);

    boolean deleteById(Long id);
}
