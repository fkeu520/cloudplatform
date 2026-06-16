package com.cloudhub.platform.ops.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.ops.domain.entity.App;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用 Mapper
 *
 * <p>W3 阶段新增 {@link #selectUserApps(Long, Long)} 方法,
 * 顶部 tab 数据源. 详见 W3.1 设计.</p>
 */
@Mapper
public interface AppMapper extends BaseMapper<App> {

    /**
     * 查询用户有权限的应用列表（顶部 tab 用）
     *
     * <p>SQL 逻辑:
     * <ol>
     *   <li>用户通过 sys_role_menu 或 sys_user_menu 间接授权的 menu.app_id</li>
     *   <li>按 sys_app.status=1 + sys_menu.status=1 + deleted=0 过滤</li>
     *   <li>如果 tenantId 不为空, 额外按 sys_tenant_app 过滤租户授权</li>
     *   <li>结果去重 + 按 sort 排序</li>
     * </ol>
     * </p>
     *
     * <p>用户类型处理 (透明):
     * <ul>
     *   <li>userType=0 (普通用户): 通过 sys_user_role 找 menu.app_id</li>
     *   <li>userType=1 (租户管理员): 一定有 sys_tenant_app 授权, tenantId 必传</li>
     *   <li>userType=2 (运营管理员): sys_user_role 通常空, 返回空列表 (行为符合预期, 走 platform-ops-admin)</li>
     * </ul>
     * </p>
     *
     * @param userId   用户 ID (从 JWT 解析, 必传)
     * @param tenantId 租户 ID (从 TenantContextHolder 取, 可空 — 空时不过滤租户授权)
     * @return 用户有权限的应用列表 (按 sort 排序)
     */
    List<App> selectUserApps(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}