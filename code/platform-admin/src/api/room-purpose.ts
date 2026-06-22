import request from './request'

export interface RoomPurpose {
  id?: number
  purposeName?: string
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getRoomPurposePage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-purpose/page', method: 'get', params })
}

export function getRoomPurposeById(id: number) {
  return request({ url: `/room-purpose/${id}`, method: 'get' })
}

export function createRoomPurpose(data: any) {
  return request({ url: '/room-purpose', method: 'post', data })
}

export function updateRoomPurpose(id: number, data: any) {
  return request({ url: `/room-purpose/${id}`, method: 'put', data })
}

export function deleteRoomPurpose(id: number) {
  return request({ url: `/room-purpose/${id}`, method: 'delete' })
}