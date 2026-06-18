package com.cloudhub.platform.user.domain.service.impl;

import com.cloudhub.platform.common.annotation.DataScope;
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

    /**
     * M5 PR2: 加 @DataScope 注解 (类方法, CGLIB 代理能识别), sys_post.dept_id 是标准字段
     * - scope=1 (全部) → 不加条件
     * - scope=3 (本部门+下级) → AND dept_id IN (CTE 收集的子部门)
     * - scope=5 (自定义) → AND dept_id IN (customDeptIds)
     * <p>
     * 注意: 必须加在 Impl 类方法上 (不能加接口), Spring AOP 默认 CGLIB 代理只扫描类方法注解
     */
    @Override
    @DataScope(deptAlias = "dept_id")
    public List<Post> listByOrgId(Long orgId) {
        return postMapper.selectByOrgId(orgId);
    }
    
    /**
     * ⚠️ 不加 @DataScope: 已被入参 deptId 过滤, 加注解会再按用户权限过滤,
     * 若入参 deptId 不在用户权限范围, 返回空, 破坏现有接口语义
     * M5 PR2 决策: 保持原行为, 业务方需传入用户权限范围内的 deptId
     */
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
    @DataScope(deptAlias = "dept_id")
    public Post update(Post post) {
        postMapper.updateById(post);
        return post;
    }

    @Override
    @Transactional
    @DataScope(deptAlias = "dept_id")
    public void delete(Long id) {
        postMapper.deleteById(id);
    }
}