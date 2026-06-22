import request from './request'

export interface Building {
  id?: number
  buildingName?: string
  buildingCode?: string
  parkId?: number
  areaId?: number
  totalFloor?: number
  totalArea?: number
  status?: number
  tenantId?: number
  createTime?: string
}

export function getBuildingPage(params: {
  keyword?: string
  areaId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/building/page', method: 'get', params })
}

export function getBuildingById(id: number) {
  return request({ url: `/building/${id}`, method: 'get' })
}

export function createBuilding(data: any) {
  return request({ url: '/building', method: 'post', data })
}

export function updateBuilding(id: number, data: any) {
  return request({ url: `/building/${id}`, method: 'put', data })
}

export function deleteBuilding(id: number) {
  return request({ url: `/building/${id}`, method: 'delete' })
}