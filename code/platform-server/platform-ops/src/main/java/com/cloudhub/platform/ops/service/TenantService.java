package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.ops.domain.entity.Tenant;
import com.cloudhub.platform.ops.domain.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final RestTemplate restTemplate;

    @Value("${org.service.url:http://platform-user:8081}")
    private String userServiceUrl;

    public IPage<Tenant> page(String keyword, Integer status, Integer tenantType, int pageNum, int pageSize) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Tenant::getTenantName, keyword)
                    .or().like(Tenant::getTenantCode, keyword);
        }
        if (status != null) wrapper.eq(Tenant::getStatus, status);
        if (tenantType != null) wrapper.eq(Tenant::getTenantType, tenantType);
        wrapper.orderByAsc(Tenant::getSort);
        return tenantMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<Tenant> list(Integer status) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Tenant::getStatus, status);
        wrapper.orderByAsc(Tenant::getSort);
        return tenantMapper.selectList(wrapper);
    }

    public Tenant getById(Long id) {
        return tenantMapper.selectById(id);
    }

    @Transactional
    public void create(Map<String, Object> params) {
        Tenant tenant = new Tenant();
        tenant.setTenantName((String) params.get("tenantName"));
        tenant.setTenantCode((String) params.get("tenantCode"));
        tenant.setTenantType(params.get("tenantType") != null ? Integer.parseInt(params.get("tenantType").toString()) : 0);
        tenant.setContactPerson((String) params.get("contactPerson"));
        tenant.setContactMobile((String) params.get("contactMobile"));
        tenant.setContactEmail((String) params.get("contactEmail"));
        tenant.setAddress((String) params.get("address"));
        tenant.setDomain((String) params.get("domain"));
        tenant.setStatus(1);
        tenant.setMaxUserCount(params.get("maxUserCount") != null ? Integer.parseInt(params.get("maxUserCount").toString()) : 0);
        tenant.setSort(params.get("sort") != null ? Integer.parseInt(params.get("sort").toString()) : 0);
        tenant.setRemark((String) params.get("remark"));
        tenantMapper.insert(tenant);
        createRootOrg(tenant);
        createAdminUser(tenant, params);
    }

    private void createRootOrg(Tenant tenant) {
        try {
            Map<String, Object> orgReq = new HashMap<>();
            orgReq.put("name", tenant.getTenantName());
            orgReq.put("code", tenant.getTenantCode());
            orgReq.put("type", 1);
            orgReq.put("parentId", 0);
            orgReq.put("status", 1);
            orgReq.put("tenantId", tenant.getId());
            restTemplate.postForEntity(userServiceUrl + "/org", orgReq, String.class);
            log.info("租户[{}]根组织已创建: {}", tenant.getId(), tenant.getTenantName());
        } catch (Exception e) {
            log.error("租户[{}]根组织创建失败", tenant.getId(), e);
            throw new BizException("根组织创建失败: " + e.getMessage());
        }
    }

    private void createAdminUser(Tenant tenant, Map<String, Object> params) {
        String adminUsername = (String) params.get("adminUsername");
        String adminPassword = (String) params.get("adminPassword");
        if (StringUtils.isBlank(adminUsername) || StringUtils.isBlank(adminPassword)) {
            throw new BizException("新建租户必须设置管理员账号和密码");
        }
        try {
            Map<String, Object> userReq = new HashMap<>();
            userReq.put("username", adminUsername);
            userReq.put("password", adminPassword);
            userReq.put("nickname", params.getOrDefault("adminNickname", adminUsername));
            userReq.put("mobile", params.getOrDefault("adminMobile", ""));
            userReq.put("email", params.getOrDefault("adminEmail", ""));
            userReq.put("orgId", tenant.getId());
            userReq.put("tenantId", tenant.getId());
            userReq.put("status", 1);
            restTemplate.postForEntity(userServiceUrl + "/user", userReq, String.class);
            log.info("租户[{}]管理员已创建: {}", tenant.getId(), adminUsername);
        } catch (Exception e) {
            log.warn("租户[{}]管理员创建失败: {}", tenant.getId(), e.getMessage());
            throw new BizException("管理员创建失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> listAdmins(Long tenantId, int pageNum, int pageSize) {
        try {
            String url = userServiceUrl + "/user/page?tenantId=" + tenantId + "&userType=1&pageNum=" + pageNum + "&pageSize=" + pageSize;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.containsKey("data")) {
                return (Map<String, Object>) resp.get("data");
            }
        } catch (Exception e) {
            log.warn("查询租户[{}]管理员列表失败: {}", tenantId, e.getMessage());
        }
        return Map.of("records", Collections.emptyList(), "total", 0);
    }

    public void createAdmin(Long tenantId, Map<String, Object> params) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) throw new BizException("租户不存在");
        params.put("tenantId", tenantId);
        params.put("orgId", tenantId);
        params.put("userType", 1);
        params.put("status", 1);
        try {
            restTemplate.postForEntity(userServiceUrl + "/user", params, String.class);
            log.info("租户[{}]管理员已创建: {}", tenantId, params.get("username"));
        } catch (Exception e) {
            throw new BizException("管理员创建失败: " + e.getMessage());
        }
    }

    public void deleteAdmin(Long tenantId, Long userId) {
        try {
            restTemplate.delete(userServiceUrl + "/user/" + userId);
            log.info("租户[{}]管理员已删除: userId={}", tenantId, userId);
        } catch (Exception e) {
            throw new BizException("管理员删除失败: " + e.getMessage());
        }
    }

    public void resetAdminPassword(Long tenantId, Long userId, String newPassword) {
        if (StringUtils.isBlank(newPassword)) {
            throw new BizException("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        try {
            Map<String, String> body = new HashMap<>();
            body.put("newPassword", newPassword);
            restTemplate.postForEntity(
                    userServiceUrl + "/user/" + userId + "/reset-password", body, String.class);
            log.info("租户[{}]管理员密码已重置: userId={}", tenantId, userId);
        } catch (Exception e) {
            throw new BizException("密码重置失败: " + e.getMessage());
        }
    }

    public void update(Long id, Map<String, Object> params) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) throw new BizException("租户不存在");
        if (params.containsKey("tenantName")) tenant.setTenantName((String) params.get("tenantName"));
        if (params.containsKey("tenantType")) tenant.setTenantType(Integer.parseInt(params.get("tenantType").toString()));
        if (params.containsKey("contactPerson")) tenant.setContactPerson((String) params.get("contactPerson"));
        if (params.containsKey("contactMobile")) tenant.setContactMobile((String) params.get("contactMobile"));
        if (params.containsKey("contactEmail")) tenant.setContactEmail((String) params.get("contactEmail"));
        if (params.containsKey("address")) tenant.setAddress((String) params.get("address"));
        if (params.containsKey("domain")) tenant.setDomain((String) params.get("domain"));
        if (params.containsKey("remark")) tenant.setRemark((String) params.get("remark"));
        if (params.containsKey("sort")) tenant.setSort(Integer.parseInt(params.get("sort").toString()));
        if (params.containsKey("maxUserCount")) tenant.setMaxUserCount(Integer.parseInt(params.get("maxUserCount").toString()));
        tenantMapper.updateById(tenant);
    }

    public void delete(Long id) {
        tenantMapper.deleteById(id);
    }

    public void toggleStatus(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) throw new BizException("租户不存在");
        tenant.setStatus(tenant.getStatus() == 1 ? 0 : 1);
        tenantMapper.updateById(tenant);
    }

    public void checkUserCount(Long tenantId, long currentCount) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) throw new BizException("租户不存在");
        int max = tenant.getMaxUserCount() != null ? tenant.getMaxUserCount() : 0;
        if (max > 0 && currentCount >= max) {
            throw new BizException("租户用户数已达上限（" + max + "人），无法添加新用户");
        }
    }

    public boolean canAddSubsidiary(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return false;
        return tenant.getTenantType() != null && tenant.getTenantType() == 0;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listOrgs(Long tenantId) {
        try {
            String url = userServiceUrl + "/org/tree?tenantId=" + tenantId;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.containsKey("data")) {
                return (List<Map<String, Object>>) resp.get("data");
            }
        } catch (Exception e) {
            log.warn("查询租户[{}]组织列表失败: {}", tenantId, e.getMessage());
        }
        return Collections.emptyList();
    }
}
