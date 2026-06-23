import request from './request'

export interface Floor {
  id?: number
  floorName?: string
  serialCode?: number
  floorCategory?: number
  coefficient?: number
  sorting?: number
  status?: number
  parkId?: number
  buildingId?: number
  tenantId?: number
  createTime?: string
}

export function getFloorPage(params: {
  keyword?: string
  parkId?: number
  buildingId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/floor/page', method: 'get', params })
}

export function getFloorById(id: number) {
  return request({ url: `/floor/${id}`, method: 'get' })
}

export function createFloor(data: any) {
  return request({ url: '/floor', method: 'post', data })
}

export function updateFloor(id: number, data: any) {
  return request({ url: `/floor/${id}`, method: 'put', data })
}

export function deleteFloor(id: number) {
  return request({ url: `/floor/${id}`, method: 'delete' })
}

export function listFloorByBuilding(buildingId: number) {
  return request({ url: `/floor/page-by-building/${buildingId}`, method: 'get' })
}
