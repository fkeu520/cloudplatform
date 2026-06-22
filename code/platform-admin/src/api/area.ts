import request from './request'

export interface Area {
  id?: number
  areaName?: string
  areaCovered?: number
  builtArea?: number
  functionArea?: string
  buildingAmount?: number
  roomAmount?: number
  isVirtual?: number
  sorting?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getAreaPage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/area/page', method: 'get', params })
}

export function getAreaById(id: number) {
  return request({ url: `/area/${id}`, method: 'get' })
}

export function createArea(data: any) {
  return request({ url: '/area', method: 'post', data })
}

export function updateArea(id: number, data: any) {
  return request({ url: `/area/${id}`, method: 'put', data })
}

export function deleteArea(id: number) {
  return request({ url: `/area/${id}`, method: 'delete' })
}