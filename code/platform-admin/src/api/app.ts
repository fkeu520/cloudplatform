import request from './request'

/**
 * 应用/Module 定义 (W3 P0-4 新增)
 *
 * <p>对应后端 sys_app �? 前端 Layout.vue 顶部 tabs 数据�?</p>
 */

export interface App {
  /** 应用 ID (雪花) */
  id?: string
  /** 应用名称 (显示�? �?"系统管理") */
  appName?: string
  /** 应用编码 (唯一标识, �?"system" / "park-space") */
  appCode?: string
  /** 应用图标 (Font Awesome class, �?"fas fa-cog") */
  appIcon?: string
  /** 类型: 0=系统内置 1=业务应用 */
  appType?: number
  /** 状�? 0=启用 1=禁用 */
  status?: number
  /** 排序 (小的在前) */
  sort?: number
  /** 备注 */
  remark?: string
}

/**
 * 获取当前用户有权限的应用列表 (顶部 tab 数据�?
 *
 * <p>W3 阶段: 后端�?platform-ops 模块�?GET /app/user 端点,
 * 返回 List&lt;App&gt; �?sort 排序, 无权限的 app 不出�?</p>
 *
 * <p>调用�? Layout.vue onMounted 阶段, 渲染顶部 tabs.</p>
 *
 * @returns Promise&lt;App[]&gt; 用户能看到的应用列表
 */
export function getUserApps(): Promise<{ data: App[] }> {
  return request({
    url: '/app/user',
    method: 'get'
  }) as Promise<{ data: App[] }>
}

/**
 * 获取应用列表 (管理后台�? 全部 app)
 *
 * <p>W4 阶段: App.vue 管理页用, 当前阶段未调�?</p>
 */
export function getAppList(): Promise<{ data: App[] }> {
  return request({
    url: '/app/list',
    method: 'get'
  }) as Promise<{ data: App[] }>
}