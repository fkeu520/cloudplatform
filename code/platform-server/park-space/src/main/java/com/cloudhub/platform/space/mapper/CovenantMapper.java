package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Covenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同房间关联 Mapper (park-space 业务)
 */
@Mapper
public interface CovenantMapper extends BaseMapper<Covenant> {
}