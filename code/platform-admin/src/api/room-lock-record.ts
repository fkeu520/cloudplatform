import request from './request'

export interface RoomLockRecord {
  id?: string
  roomId?: string
  isLock?: number
  enterpriseId?: number
  enterpriseName?: string
  operator?: string
  reason?: string
  days?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getRoomLockRecordPage(params: {
  roomId?: string
  parkId?: string
  isLock?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-lock-record/page', method: 'get', params })
}

export function getRoomLockRecordById(id: string) {
  return request({ url: `/room-lock-record/${id}`, method: 'get' })
}

export function createRoomLockRecord(data: any) {
  return request({ url: '/room-lock-record', method: 'post', data })
}