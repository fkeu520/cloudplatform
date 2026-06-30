import request from './request'

/**
 * 企业绑定关系 API
 */

export type BindType = 'park' | 'building' | 'tenant' | 'room'

export interface EnterpriseEntBind {
  id?: string
  enterpriseId?: string | number
  bindType?: BindType
  bindId?: number
  bindStatus?: number
  bindingAt?: string
  unboundAt?: string
  remark?: string
  createBy?: string
}

export function listBindByEnterprise(enterpriseId: string | number) {
  return request({ url: '/enterprise/bind/list', method: 'get', params: { enterpriseId } })
}

export function listAllBinds() {
  return request({ url: '/enterprise/bind/all', method: 'get' })
}

export function createBind(data: Partial<EnterpriseEntBind>) {
  return request({ url: '/enterprise/bind', method: 'post', data })
}

export function unbind(id: string) {
  return request({ url: `/enterprise/bind/${id}/unbind`, method: 'patch' })
}

export function deleteBind(id: string) {
  return request({ url: `/enterprise/bind/${id}`, method: 'delete' })
}
