package com.cloudhub.platform.enterprise.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.enterprise.cloud.model.CloudData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 云企库数据 Mapper (Phase 3)
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Mapper
public interface CloudDataMapper extends BaseMapper<CloudData> {
}