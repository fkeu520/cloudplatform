package com.cloudhub.platform.user.domain.service.impl;

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
    
    @Override
    public List<Dept> listByOrgId(Long orgId) {
        return deptMapper.selectByOrgId(orgId);
    }
    
    @Override
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
    public Dept update(Dept dept) {
        deptMapper.updateById(dept);
        return dept;
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        deptMapper.deleteById(id);
    }
}