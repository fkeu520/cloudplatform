import request from './request'

export interface Massif {
  id?: number
  massifCode?: string
  massifName?: string
  massifArea?: number
  useYear?: number
  landNatureId?: number
  planUseId?: number
  assetType?: string
  massifDesc?: string
  address?: string
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getMassifPage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/massif/page', method: 'get', params })
}

export function getMassifById(id: number) {
  return request({ url: `/massif/${id}`, method: 'get' })
}

export function createMassif(data: any) {
  return request({ url: '/massif', method: 'post', data })
}

export function updateMassif(id: number, data: any) {
  return request({ url: `/massif/${id}`, method: 'put', data })
}

export function deleteMassif(id: number) {
  return request({ url: `/massif/${id}`, method: 'delete' })
}