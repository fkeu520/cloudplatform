import request from './request'

// ============ 类型定义 ============

export interface DashboardWelcome {
  todoCount: number
  msgCount: number
  myApplyCount: number
  sysNoticeCount: number
}

export interface DashboardTodo {
  id: number
  title: string
  status: string
  createTime: string
}

export interface DashboardMessage {
  id: number
  title: string
  sender: string
  createTime: string
}

export interface DashboardShortcut {
  id: number
  name: string
  path: string
  icon?: string
}

export interface DashboardBusinessStat {
  key: string
  label: string
  value: number
  unit?: string
  trend?: 'up' | 'down' | 'flat'
}

export interface DashboardSystemStats {
  source: string
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  containerCount: number
}

// ============ API 函数 ============

/** 获取工作台欢迎/概览数据 */
export function getDashboardWelcome() {
  return request.get('/dashboard/welcome')
}

/** 获取待办列表 */
export function getDashboardTodos(limit = 5) {
  return request.get('/dashboard/todos', { params: { limit } })
}

/** 获取消息列表 */
export function getDashboardMessages(limit = 5) {
  return request.get('/dashboard/messages', { params: { limit } })
}

/** 获取快捷入口 */
export function getDashboardShortcuts() {
  return request.get('/dashboard/shortcuts')
}

/** 获取业务数据统计 */
export function getDashboardBusinessStats(appCode?: string) {
  return request.get('/dashboard/business-stats', { params: { appCode } })
}

/** 获取系统资源监控 (仅运营管理员) */
export function getDashboardSystemStats() {
  return request.get('/dashboard/system-stats')
}
