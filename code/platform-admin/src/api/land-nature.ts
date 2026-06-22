import request from './request'

export interface LandNature {
  id?: number
  landNatureName?: string
  landNatureCode?: string
  color?: string
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getLandNaturePage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/land-nature/page', method: 'get', params })
}

export function getLandNatureById(id: number) {
  return request({ url: `/land-nature/${id}`, method: 'get' })
}

export function createLandNature(data: any) {
  return request({ url: '/land-nature', method: 'post', data })
}

export function updateLandNature(id: number, data: any) {
  return request({ url: `/land-nature/${id}`, method: 'put', data })
}

export function deleteLandNature(id: number) {
  return request({ url: `/land-nature/${id}`, method: 'delete' })
}