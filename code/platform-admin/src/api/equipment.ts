import request from './request'

export interface Equipment {
  id?: string
  equipmentName?: string
  model?: string
  amount?: number
  kitId?: string
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getEquipmentPage(params: {
  keyword?: string
  parkId?: string
  kitId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/equipment/page', method: 'get', params })
}

export function getEquipmentById(id: string) {
  return request({ url: `/equipment/${id}`, method: 'get' })
}

export function createEquipment(data: any) {
  return request({ url: '/equipment', method: 'post', data })
}

export function updateEquipment(id: string, data: any) {
  return request({ url: `/equipment/${id}`, method: 'put', data })
}

export function deleteEquipment(id: string) {
  return request({ url: `/equipment/${id}`, method: 'delete' })
}

export function listByKit(kitId: string) {
  return request({ url: `/equipment/listByKit/${kitId}`, method: 'get' })
}

export function batchSaveByKit(kitId: string, data: any[]) {
  return request({ url: `/equipment/batchSave/${kitId}`, method: 'post', data })
}