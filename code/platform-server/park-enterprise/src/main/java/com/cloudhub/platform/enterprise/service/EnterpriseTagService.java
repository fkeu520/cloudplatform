package com.cloudhub.platform.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseTag;
import com.cloudhub.platform.enterprise.mapper.EnterpriseTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业标签 Service (park-enterprise Phase 1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseTagService {

    private final EnterpriseTagMapper tagMapper;

    /** 列出企业的所有标签 */
    public Result<List<EnterpriseTag>> listByEnterprise(Long enterpriseId) {
        return Result.ok(tagMapper.selectList(
                new LambdaQueryWrapper<EnterpriseTag>()
                        .eq(EnterpriseTag::getEnterpriseId, enterpriseId)
                        .orderByAsc(EnterpriseTag::getSortOrder)));
    }

    /** 列表所有 (用于字典下拉) */
    public Result<List<EnterpriseTag>> listAll() {
        return Result.ok(tagMapper.selectList(
                new LambdaQueryWrapper<EnterpriseTag>().orderByAsc(EnterpriseTag::getTagName)));
    }

    /** 新增 */
    public Result<EnterpriseTag> create(EnterpriseTag tag, String createBy) {
        if (tag.getEnterpriseId() == null || tag.getTagName() == null) {
            throw new IllegalArgumentException("enterpriseId 与 tagName 必填");
        }
        tag.setId(null);
        tag.setCreateBy(createBy);
        tag.setCreateTime(LocalDateTime.now());
        tagMapper.insert(tag);
        log.info("新增企业标签 enterpriseId={} tag={}", tag.getEnterpriseId(), tag.getTagName());
        return Result.ok(tag);
    }

    /** 删除 */
    public Result<Void> delete(Long id) {
        tagMapper.deleteById(id);
        return Result.ok();
    }
}
