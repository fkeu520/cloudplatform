import request from './request'

export interface Covenant {
  id?: number
  covenantId?: number
  covenantType?: number
  customerId?: number
  roomId?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getCovenantPage(params: {
  keyword?: string
  parkId?: number
  roomId?: number
  covenantType?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/covenant/page', method: 'get', params })
}

export function getCovenantById(id: number) {
  return request({ url: `/covenant/${id}`, method: 'get' })
}

export function createCovenant(data: any) {
  return request({ url: '/covenant', method: 'post', data })
}

export function updateCovenant(id: number, data: any) {
  return request({ url: `/covenant/${id}`, method: 'put', data })
}

export function deleteCovenant(id: number) {
  return request({ url: `/covenant/${id}`, method: 'delete' })
}