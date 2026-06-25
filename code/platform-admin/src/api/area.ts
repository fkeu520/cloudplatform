import request from './request'

export interface Area {
  id?: string
  areaName?: string
  areaCovered?: number
  builtArea?: number
  functionArea?: string
  buildingAmount?: number
  roomAmount?: number
  isVirtual?: number
  sorting?: number
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getAreaPage(params: {
  keyword?: string
  parkId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/area/page', method: 'get', params })
}

export function getAreaById(id: string) {
  return request({ url: `/area/${id}`, method: 'get' })
}

export function createArea(data: any) {
  return request({ url: '/area', method: 'post', data })
}

export function updateArea(id: string, data: any) {
  return request({ url: `/area/${id}`, method: 'put', data })
}

export function deleteArea(id: string) {
  return request({ url: `/area/${id}`, method: 'delete' })
}

export function checkAreaName(parkId: string, areaName: string, excludeId?: string) {
  return request({ url: '/area/check-name', method: 'get', params: { parkId, areaName, excludeId } })
}
