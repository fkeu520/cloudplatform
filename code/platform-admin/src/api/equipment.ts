import request from './request'

export interface Equipment {
  id?: number
  equipmentName?: string
  model?: string
  amount?: number
  kitId?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getEquipmentPage(params: {
  keyword?: string
  parkId?: number
  kitId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/equipment/page', method: 'get', params })
}

export function getEquipmentById(id: number) {
  return request({ url: `/equipment/${id}`, method: 'get' })
}

export function createEquipment(data: any) {
  return request({ url: '/equipment', method: 'post', data })
}

export function updateEquipment(id: number, data: any) {
  return request({ url: `/equipment/${id}`, method: 'put', data })
}

export function deleteEquipment(id: number) {
  return request({ url: `/equipment/${id}`, method: 'delete' })
}