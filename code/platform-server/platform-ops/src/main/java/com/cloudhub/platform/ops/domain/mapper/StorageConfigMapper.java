package com.cloudhub.platform.ops.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.ops.domain.entity.StorageConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StorageConfigMapper extends BaseMapper<StorageConfig> {
}
