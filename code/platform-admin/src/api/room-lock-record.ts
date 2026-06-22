import request from './request'

export interface RoomLockRecord {
  id?: number
  roomId?: number
  isLock?: number
  enterpriseId?: number
  enterpriseName?: string
  operator?: string
  reason?: string
  days?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getRoomLockRecordPage(params: {
  roomId?: number
  parkId?: number
  isLock?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-lock-record/page', method: 'get', params })
}

export function getRoomLockRecordById(id: number) {
  return request({ url: `/room-lock-record/${id}`, method: 'get' })
}

export function createRoomLockRecord(data: any) {
  return request({ url: '/room-lock-record', method: 'post', data })
}