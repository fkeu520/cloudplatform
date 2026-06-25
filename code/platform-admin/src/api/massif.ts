import request from './request'

export interface Massif {
  id?: string
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
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getMassifPage(params: {
  keyword?: string
  parkId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/massif/page', method: 'get', params })
}

export function getMassifById(id: string) {
  return request({ url: `/massif/${id}`, method: 'get' })
}

export function createMassif(data: any) {
  return request({ url: '/massif', method: 'post', data })
}

export function updateMassif(id: string, data: any) {
  return request({ url: `/massif/${id}`, method: 'put', data })
}

export function deleteMassif(id: string) {
  return request({ url: `/massif/${id}`, method: 'delete' })
}