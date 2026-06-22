package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.SpaceCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 空间类别 Mapper (park-space 业务)
 */
@Mapper
public interface SpaceCategoryMapper extends BaseMapper<SpaceCategory> {
}