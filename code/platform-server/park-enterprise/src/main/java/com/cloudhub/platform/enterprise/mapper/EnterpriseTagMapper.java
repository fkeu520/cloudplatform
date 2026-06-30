package com.cloudhub.platform.enterprise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业标签 Mapper (park-enterprise 业务)
 */
@Mapper
public interface EnterpriseTagMapper extends BaseMapper<EnterpriseTag> {
}
