import request from './request'

/**
 * 灰度开关视图对象
 * value 类型: PR1-4 用 boolean; PR5 新增 dimension/tenants/users (String) / percent (Integer)
 */
export interface GraySwitchVO {
  key: string
  group: string
  value: boolean | string | number
  description: string
  restartRequired: boolean
  queryTime?: string
}

/**
 * 灰度审计实体
 */
export interface GrayAuditVO {
  id: number
  switchKey: string
  groupName: string
  oldValue: string
  newValue: string
  opType: string
  operatorId: number
  operatorName: string
  reason: string
  serviceName: string
  instanceIp: string
  createTime: string
}

/**
 * 获取所有 platform.* 灰度开关当前值
 * 调用 platform-user GrayController.list
 */
export function listGraySwitches() {
  return request({
    url: '/gray/list',
    method: 'get'
  })
}

/**
 * 分页查询灰度审计历史
 */
export function pageGrayAudit(params: { switchKey?: string; pageNum: number; pageSize: number }) {
  return request({
    url: '/gray/audit/page',
    method: 'get',
    params
  })
}