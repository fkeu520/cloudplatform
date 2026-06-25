import request from './request'

export interface Floor {
  id?: string
  floorName?: string
  serialCode?: number
  floorCategory?: number
  coefficient?: number
  sorting?: number
  status?: number
  parkId?: string
  buildingId?: string
  tenantId?: string
  createTime?: string
}

export function getFloorPage(params: {
  keyword?: string
  parkId?: string
  buildingId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/floor/page', method: 'get', params })
}

export function getFloorById(id: string) {
  return request({ url: `/floor/${id}`, method: 'get' })
}

export function createFloor(data: any) {
  return request({ url: '/floor', method: 'post', data })
}

export function updateFloor(id: string, data: any) {
  return request({ url: `/floor/${id}`, method: 'put', data })
}

export function deleteFloor(id: string) {
  return request({ url: `/floor/${id}`, method: 'delete' })
}

export function listFloorByBuilding(buildingId: string) {
  return request({ url: `/floor/page-by-building/${buildingId}`, method: 'get' })
}
