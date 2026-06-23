import request from './request'

export interface Building {
  id?: number
  buildingName?: string
  buildingCode?: string
  buildingNo?: string
  parkId?: number
  areaId?: number
  floorNumber?: number
  underground?: number
  floors?: number
  areaCovered?: number
  totalArea?: number
  propertyRight?: number
  buildingSafety?: number
  shareArea?: number
  leaseMethod?: number
  sorting?: number
  certificate?: string
  image?: string
  buildYear?: number
  manager?: string
  managerPhone?: string
  status?: number
  remark?: string
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

export function checkBuildingCode(parkId: number, buildingCode: string, excludeId?: number) {
  return request({ url: '/building/check-code', method: 'get', params: { parkId, buildingCode, excludeId } })
}

export function getFloorListByBuilding(buildingId: number) {
  return request({ url: `/floor/page-by-building/${buildingId}`, method: 'get' })
}
