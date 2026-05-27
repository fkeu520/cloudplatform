package com.cloudhub.platform.user.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
    
    List<Dept> selectByOrgId(@Param("orgId") Long orgId);
    
    List<Dept> selectTreeByOrgId(@Param("orgId") Long orgId);
}