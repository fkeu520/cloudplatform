package com.cloudhub.platform.enterprise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseEntBind;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业绑定关系 Mapper (park-enterprise 业务)
 */
@Mapper
public interface EnterpriseEntBindMapper extends BaseMapper<EnterpriseEntBind> {
}
