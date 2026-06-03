package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.user.domain.entity.Organization;
import com.cloudhub.platform.user.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织机构管理 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper orgMapper;

    /**
     * 获取组织树（按租户过滤）
     */
    public List<Map<String, Object>> tree(Long tenantId) {
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<Organization>()
                .eq(Organization::getDeleted, 0);
        if (tenantId != null) {
            wrapper.eq(Organization::getTenantId, tenantId);
        }
        wrapper.orderByAsc(Organization::getSort);
        List<Organization> all = orgMapper.selectList(wrapper);
        return buildTree(all, 0L);
    }

    /**
     * 根据ID查询
     */
    public Organization getById(Long id) {
        Organization org = orgMapper.selectById(id);
        if (org == null) {
            throw new BizException("组织不存在");
        }
        return org;
    }

    /**
     * 新增组织
     */
    @Transactional
    public void create(Map<String, Object> params) {
        String name = (String) params.get("name");
        if (name == null || name.isBlank()) {
            throw new BizException("组织名称不能为空");
        }

        Organization org = new Organization();
        org.setParentId(toLong(params.get("parentId"), 0L));
        org.setName(name);
        org.setCode((String) params.get("code"));
        org.setType(params.get("type") != null ? ((Number) params.get("type")).intValue() : 1);
        org.setSort(params.get("sort") != null ? ((Number) params.get("sort")).intValue() : 0);
        org.setStatus(params.get("status") != null ? ((Number) params.get("status")).intValue() : 1);
        org.setTenantId(params.get("tenantId") != null ? ((Number) params.get("tenantId")).longValue() : 1L);
        orgMapper.insert(org);
    }

    /**
     * 更新组织
     */
    @Transactional
    public void update(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        Organization exist = orgMapper.selectById(id);
        if (exist == null) {
            throw new BizException("组织不存在");
        }
        if (params.containsKey("name")) exist.setName((String) params.get("name"));
        if (params.containsKey("parentId")) exist.setParentId(toLong(params.get("parentId"), null));
        if (params.containsKey("code")) exist.setCode((String) params.get("code"));
        if (params.containsKey("type")) exist.setType(((Number) params.get("type")).intValue());
        if (params.containsKey("sort")) exist.setSort(((Number) params.get("sort")).intValue());
        if (params.containsKey("status")) exist.setStatus(((Number) params.get("status")).intValue());
        orgMapper.updateById(exist);
    }

    /**
     * 删除组织
     */
    @Transactional
    public void delete(Long id) {
        long childCount = orgMapper.selectCount(
                new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getParentId, id)
                        .eq(Organization::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new BizException("请先删除子组织");
        }
        orgMapper.deleteById(id);
    }

    private Long toLong(Object val, Long defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) return Long.parseLong((String) val);
        throw new IllegalArgumentException("Cannot convert to Long: " + val);
    }

    private List<Map<String, Object>> buildTree(List<Organization> orgs, Long parentId) {
        return orgs.stream()
                .filter(o -> o.getParentId().equals(parentId))
                .map(o -> {
                    Map<String, Object> node = new java.util.HashMap<>();
                    node.put("id", o.getId().toString());
                    node.put("name", o.getName());
                    node.put("code", o.getCode());
                    node.put("type", o.getType());
                    node.put("sort", o.getSort());
                    node.put("status", o.getStatus());
                    List<Map<String, Object>> children = buildTree(orgs, o.getId());
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }
}