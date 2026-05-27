package com.cloudhub.platform.user.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    
    List<Post> selectByOrgId(@Param("orgId") Long orgId);
    
    List<Post> selectByDeptId(@Param("deptId") Long deptId);
}