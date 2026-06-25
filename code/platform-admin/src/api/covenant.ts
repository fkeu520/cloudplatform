import request from './request'

export interface Covenant {
  id?: string
  covenantId?: number
  covenantType?: number
  customerId?: number
  roomId?: string
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getCovenantPage(params: {
  keyword?: string
  parkId?: string
  roomId?: string
  covenantType?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/covenant/page', method: 'get', params })
}

export function getCovenantById(id: string) {
  return request({ url: `/covenant/${id}`, method: 'get' })
}

export function createCovenant(data: any) {
  return request({ url: '/covenant', method: 'post', data })
}

export function updateCovenant(id: string, data: any) {
  return request({ url: `/covenant/${id}`, method: 'put', data })
}

export function deleteCovenant(id: string) {
  return request({ url: `/covenant/${id}`, method: 'delete' })
}