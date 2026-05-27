package com.cloudhub.platform.user.domain.service;

import com.cloudhub.platform.user.domain.entity.Dept;

import java.util.List;

public interface DeptService {
    
    Dept getById(Long id);
    
    List<Dept> listByOrgId(Long orgId);
    
    List<Dept> listTreeByOrgId(Long orgId);
    
    Dept create(Dept dept);
    
    Dept update(Dept dept);
    
    void delete(Long id);
}