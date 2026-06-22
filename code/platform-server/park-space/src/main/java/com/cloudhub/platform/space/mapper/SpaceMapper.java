package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Space;
import org.apache.ibatis.annotations.Mapper;

/**
 * 空间 Mapper (park-space 业务)
 */
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {
}