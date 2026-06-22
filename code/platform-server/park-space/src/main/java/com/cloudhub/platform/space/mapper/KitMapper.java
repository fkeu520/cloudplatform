package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Kit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 装修配套 Mapper (park-space 业务)
 */
@Mapper
public interface KitMapper extends BaseMapper<Kit> {
}