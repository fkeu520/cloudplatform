import request from './request'

export interface LandNature {
  id?: string
  landNatureName?: string
  landNatureCode?: string
  color?: string
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getLandNaturePage(params: {
  keyword?: string
  parkId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/land-nature/page', method: 'get', params })
}

export function getLandNatureById(id: string) {
  return request({ url: `/land-nature/${id}`, method: 'get' })
}

export function createLandNature(data: any) {
  return request({ url: '/land-nature', method: 'post', data })
}

export function updateLandNature(id: string, data: any) {
  return request({ url: `/land-nature/${id}`, method: 'put', data })
}

export function deleteLandNature(id: string) {
  return request({ url: `/land-nature/${id}`, method: 'delete' })
}