package com.cloudhub.platform.user.domain.service;

import com.cloudhub.platform.user.domain.entity.Post;

import java.util.List;

public interface PostService {
    
    Post getById(Long id);
    
    List<Post> listByOrgId(Long orgId);
    
    List<Post> listByDeptId(Long deptId);
    
    Post create(Post post);
    
    Post update(Post post);
    
    void delete(Long id);
}