import request from './request'

export interface RoomRecord {
  id?: string
  roomId?: string
  customerId?: number
  covenantId?: number
  covenantType?: number
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getRoomRecordPage(params: {
  roomId?: string
  parkId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-record/page', method: 'get', params })
}

export function getRoomRecordById(id: string) {
  return request({ url: `/room-record/${id}`, method: 'get' })
}

export function createRoomRecord(data: any) {
  return request({ url: '/room-record', method: 'post', data })
}