package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.ops.domain.entity.App;
import com.cloudhub.platform.ops.domain.mapper.AppMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppService {

    private final AppMapper appMapper;

    public IPage<App> page(String keyword, Integer appType, int pageNum, int pageSize) {
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(App::getAppName, keyword).or().like(App::getAppCode, keyword);
        }
        if (appType != null) wrapper.eq(App::getAppType, appType);
        wrapper.orderByAsc(App::getSort);
        return appMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<App> list(Integer appType) {
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<>();
        if (appType != null) wrapper.eq(App::getAppType, appType);
        wrapper.eq(App::getStatus, 1);
        wrapper.orderByAsc(App::getSort);
        return appMapper.selectList(wrapper);
    }

    public App getById(Long id) {
        return appMapper.selectById(id);
    }

    /**
     * 查询当前用户有权限的应用列表 (顶部 tab 数据源)
     *
     * <p>W3 阶段新增, 详见 AppMapper.selectUserApps SQL 注释.
     * 三种 userType 透明处理 (普通用户 / 租户管理员 / 运营管理员).</p>
     *
     * @param userId   用户 ID (从 JWT 解析)
     * @param tenantId 租户 ID (从 TenantContextHolder 取, 始终过滤 — null 时返回空列表, fail-closed)
     * @param userType 用户类型 (0=普通用户 1=租户管理员 2=运营管理员)
     * @return 用户有权限的应用列表, 按 sort 排序, 启用状态过滤
     */
    public List<App> userApps(Long userId, Long tenantId, Integer userType) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        if (userType != null && userType == 1) {
            // 租户管理员: 跳过角色关联, 直接查 sys_tenant_app
            if (tenantId == null) {
                return java.util.Collections.emptyList();
            }
            return appMapper.selectTenantAdminApps(tenantId);
        }
        return appMapper.selectUserApps(userId, tenantId);
    }

    public void create(Map<String, Object> params) {
        App app = new App();
        app.setAppName((String) params.get("appName"));
        app.setAppCode((String) params.get("appCode"));
        app.setAppIcon((String) params.get("appIcon"));
        app.setAppType(params.get("appType") != null ? Integer.parseInt(params.get("appType").toString()) : 0);
        app.setStatus(1);
        app.setSort(params.get("sort") != null ? Integer.parseInt(params.get("sort").toString()) : 0);
        app.setRemark((String) params.get("remark"));
        appMapper.insert(app);
    }

    public void update(Long id, Map<String, Object> params) {
        App app = appMapper.selectById(id);
        if (app == null) throw new BizException("应用不存在");
        if (params.containsKey("appName")) app.setAppName((String) params.get("appName"));
        if (params.containsKey("appCode")) app.setAppCode((String) params.get("appCode"));
        if (params.containsKey("appIcon")) app.setAppIcon((String) params.get("appIcon"));
        if (params.containsKey("remark")) app.setRemark((String) params.get("remark"));
        if (params.containsKey("sort")) app.setSort(Integer.parseInt(params.get("sort").toString()));
        appMapper.updateById(app);
    }

    public void delete(Long id) {
        appMapper.deleteById(id);
    }
}
