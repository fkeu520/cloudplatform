import request from './request'

export interface Kit {
  id?: string
  kitName?: string
  amount?: number
  status?: number
  tenantId?: string
  equipmentCount?: number
  createTime?: string
}

export interface Equipment {
  id?: string
  kitId?: string
  equipmentName?: string
  model?: string
  amount?: number
  status?: number
  parkId?: string
  tenantId?: string
}

export function getKitPage(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/kit/page', method: 'get', params })
}

export function getKitById(id: string) {
  return request({ url: `/kit/${id}`, method: 'get' })
}

export function createKit(data: any) {
  return request({ url: '/kit', method: 'post', data })
}

export function updateKit(id: string, data: any) {
  return request({ url: `/kit/${id}`, method: 'put', data })
}

export function deleteKit(id: string) {
  return request({ url: `/kit/${id}`, method: 'delete' })
}

export function checkKitName(kitName: string, excludeId?: string) {
  return request({ url: '/kit/check-name', method: 'get', params: { kitName, excludeId } })
}

export function listEquipmentByKit(kitId: string) {
  return request({ url: `/equipment/list-by-kit-id/${kitId}`, method: 'get' })
}

export function batchSaveEquipment(data: Equipment[]) {
  return request({ url: '/equipment/batch-save', method: 'post', data })
}
