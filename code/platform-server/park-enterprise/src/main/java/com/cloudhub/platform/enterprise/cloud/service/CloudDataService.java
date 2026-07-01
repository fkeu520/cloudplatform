package com.cloudhub.platform.enterprise.cloud.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.mapper.CloudDataMapper;
import com.cloudhub.platform.enterprise.cloud.model.CloudData;
import com.cloudhub.platform.enterprise.cloud.model.CloudDataCategoryEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 云企库数据 Service (Phase 3, 手动维护)
 *
 * <p>通用 CRUD, 通过 {@code category} 区分业务类型.
 * 前端按 category 调用对应接口展示/编辑.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudDataService {

    private final CloudDataMapper cloudDataMapper;

    /** 按分类分页 */
    public Result<Page<CloudData>> page(String category, Long enterpriseId, String keyword,
                                         String dataYear, int pageNum, int pageSize) {
        LambdaQueryWrapper<CloudData> w = new LambdaQueryWrapper<CloudData>()
                .eq(CloudData::getCategory, category)
                .eq(enterpriseId != null, CloudData::getEnterpriseId, enterpriseId)
                .eq(StrUtil.isNotBlank(dataYear), CloudData::getDataYear, dataYear)
                .like(StrUtil.isNotBlank(keyword), CloudData::getEnterpriseName, keyword)
                .or(StrUtil.isNotBlank(keyword))
                .like(StrUtil.isNotBlank(keyword), CloudData::getRemark, keyword)
                .orderByAsc(CloudData::getSortOrder)
                .orderByDesc(CloudData::getCreateTime);
        return Result.ok(cloudDataMapper.selectPage(Page.of(pageNum, pageSize), w));
    }

    /** 详情 */
    public Result<CloudData> detail(Long id) {
        CloudData d = cloudDataMapper.selectById(id);
        if (d == null) throw new BizException("云数据不存在: " + id);
        return Result.ok(d);
    }

    /** 新增 */
    @Transactional
    public Result<CloudData> create(CloudData d, String createBy) {
        d.setId(null);
        if (d.getStatus() == null) d.setStatus(1);
        d.setCreateBy(createBy);
        cloudDataMapper.insert(d);
        return Result.ok(d);
    }

    /** 更新 */
    @Transactional
    public Result<CloudData> update(CloudData d, String updateBy) {
        if (d.getId() == null) throw new BizException("更新必须有 ID");
        CloudData exist = cloudDataMapper.selectById(d.getId());
        if (exist == null) throw new BizException("云数据不存在: " + d.getId());
        d.setUpdateBy(updateBy);
        cloudDataMapper.updateById(d);
        return Result.ok(d);
    }

    /** 删除 */
    @Transactional
    public Result<Void> delete(Long id) {
        if (cloudDataMapper.selectById(id) == null) {
            throw new BizException("云数据不存在: " + id);
        }
        cloudDataMapper.deleteById(id);
        return Result.ok();
    }
}
