import request from './request'

export interface RoomRecord {
  id?: number
  roomId?: number
  customerId?: number
  covenantId?: number
  covenantType?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getRoomRecordPage(params: {
  roomId?: number
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-record/page', method: 'get', params })
}

export function getRoomRecordById(id: number) {
  return request({ url: `/room-record/${id}`, method: 'get' })
}

export function createRoomRecord(data: any) {
  return request({ url: '/room-record', method: 'post', data })
}