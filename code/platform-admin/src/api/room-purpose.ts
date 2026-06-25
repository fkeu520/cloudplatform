import request from './request'

export interface RoomPurpose {
  id?: string
  purposeName?: string
  status?: number
  tenantId?: string
  createTime?: string
}

export function getRoomPurposePage(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-purpose/page', method: 'get', params })
}

export function getRoomPurposeById(id: string) {
  return request({ url: `/room-purpose/${id}`, method: 'get' })
}

export function createRoomPurpose(data: any) {
  return request({ url: '/room-purpose', method: 'post', data })
}

export function updateRoomPurpose(id: string, data: any) {
  return request({ url: `/room-purpose/${id}`, method: 'put', data })
}

export function deleteRoomPurpose(id: string) {
  return request({ url: `/room-purpose/${id}`, method: 'delete' })
}