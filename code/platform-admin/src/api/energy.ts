import request from './request'

export interface Energy {
  id?: number
  meterId?: number
  meterClassId?: number
  roomId?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getEnergyPage(params: {
  parkId?: number
  roomId?: number
  meterId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/energy/page', method: 'get', params })
}

export function getEnergyById(id: number) {
  return request({ url: `/energy/${id}`, method: 'get' })
}

export function createEnergy(data: any) {
  return request({ url: '/energy', method: 'post', data })
}

export function updateEnergy(id: number, data: any) {
  return request({ url: `/energy/${id}`, method: 'put', data })
}

export function deleteEnergy(id: number) {
  return request({ url: `/energy/${id}`, method: 'delete' })
}