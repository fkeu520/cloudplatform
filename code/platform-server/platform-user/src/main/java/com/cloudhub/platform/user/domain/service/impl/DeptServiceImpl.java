package com.cloudhub.platform.user.domain.service.impl;

import com.cloudhub.platform.common.annotation.DataScope;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.mapper.DeptMapper;
import com.cloudhub.platform.user.domain.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {
    
    private final DeptMapper deptMapper;
    
    @Override
    public Dept getById(Long id) {
        return deptMapper.selectById(id);
    }

    /**
     * M5 PR2: 加 @DataScope 注解 (类方法, CGLIB 代理), sys_dept 无 dept_id 列, 用主键 id
     * - scope=1 → 不加条件
     * - scope=3 → AND id IN (CTE 收集的子部门)
     * - scope=5 → AND id IN (customDeptIds)
     */
    @Override
    @DataScope(deptAlias = "id")
    public List<Dept> listByOrgId(Long orgId) {
        return deptMapper.selectByOrgId(orgId);
    }

    /**
     * M5 PR2: 同 listByOrgId, 在 org 范围内按用户权限过滤, buildTree 在 Java 层
     */
    @Override
    @DataScope(deptAlias = "id")
    public List<Dept> listTreeByOrgId(Long orgId) {
        List<Dept> depts = deptMapper.selectByOrgId(orgId);
        return buildTree(depts, 0L);
    }
    
    private List<Dept> buildTree(List<Dept> list, Long parentId) {
        return list.stream()
                .filter(d -> d.getParentId().equals(parentId))
                .peek(d -> d.setChildren(buildTree(list, d.getId())))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Dept create(Dept dept) {
        deptMapper.insert(dept);
        return dept;
    }

    @Override
    @Transactional
    @DataScope(deptAlias = "id")
    public Dept update(Dept dept) {
        deptMapper.updateById(dept);
        return dept;
    }

    @Override
    @Transactional
    @DataScope(deptAlias = "id")
    public void delete(Long id) {
        deptMapper.deleteById(id);
    }
}