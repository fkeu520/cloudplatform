package com.cloudhub.platform.enterprise.cloud.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.mapper.EntRegTypeMapper;
import com.cloudhub.platform.enterprise.cloud.model.EntRegType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业注册类型 Service
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Service
@RequiredArgsConstructor
public class EntRegTypeService {

    private final EntRegTypeMapper entRegTypeMapper;

    public Result<Page<EntRegType>> page(String keyword, String parentCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<EntRegType> w = new LambdaQueryWrapper<EntRegType>()
                .like(StrUtil.isNotBlank(keyword), EntRegType::getName, keyword)
                .or(StrUtil.isNotBlank(keyword))
                .like(StrUtil.isNotBlank(keyword), EntRegType::getCode, keyword)
                .eq(StrUtil.isNotBlank(parentCode), EntRegType::getParentCode, parentCode)
                .orderByAsc(EntRegType::getSortOrder);
        return Result.ok(entRegTypeMapper.selectPage(Page.of(pageNum, pageSize), w));
    }

    public Result<EntRegType> detail(Long id) {
        EntRegType e = entRegTypeMapper.selectById(id);
        if (e == null) throw new BizException("注册类型不存在: " + id);
        return Result.ok(e);
    }

    @Transactional
    public Result<EntRegType> create(EntRegType e, String createBy) {
        e.setId(null);
        if (e.getStatus() == null) e.setStatus(1);
        e.setCreateBy(createBy);
        entRegTypeMapper.insert(e);
        return Result.ok(e);
    }

    @Transactional
    public Result<EntRegType> update(EntRegType e, String updateBy) {
        if (e.getId() == null) throw new BizException("更新必须有 ID");
        EntRegType exist = entRegTypeMapper.selectById(e.getId());
        if (exist == null) throw new BizException("注册类型不存在: " + e.getId());
        e.setUpdateBy(updateBy);
        entRegTypeMapper.updateById(e);
        return Result.ok(e);
    }

    @Transactional
    public Result<Void> delete(Long id) {
        if (entRegTypeMapper.selectById(id) == null) {
            throw new BizException("注册类型不存在: " + id);
        }
        entRegTypeMapper.deleteById(id);
        return Result.ok();
    }
}
