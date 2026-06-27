import request from './request'

/**
 * 灰度开关视图对象
 */
export interface GraySwitchVO {
  key: string
  group: string
  value: boolean
  description: string
  restartRequired: boolean
  queryTime?: string
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