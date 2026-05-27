package com.cloudhub.platform.user.domain.service.impl;

import com.cloudhub.platform.user.domain.entity.Post;
import com.cloudhub.platform.user.domain.mapper.PostMapper;
import com.cloudhub.platform.user.domain.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    
    private final PostMapper postMapper;
    
    @Override
    public Post getById(Long id) {
        return postMapper.selectById(id);
    }
    
    @Override
    public List<Post> listByOrgId(Long orgId) {
        return postMapper.selectByOrgId(orgId);
    }
    
    @Override
    public List<Post> listByDeptId(Long deptId) {
        return postMapper.selectByDeptId(deptId);
    }
    
    @Override
    @Transactional
    public Post create(Post post) {
        postMapper.insert(post);
        return post;
    }
    
    @Override
    @Transactional
    public Post update(Post post) {
        postMapper.updateById(post);
        return post;
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        postMapper.deleteById(id);
    }
}