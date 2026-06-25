import request from './request'

export interface RoomControl {
  id?: string
  parkId?: string
  buildingId?: string
  floorId?: string
  roomNo?: string
  roomName?: string
  areaCovered?: number
  buildArea?: number
  status?: number
  rentingSelling?: number
  leasePrice?: number
  salePrice?: number
  isLock?: number
  isOrder?: number
  createTime?: string
}

export function getRoomControlPage(params: {
  parkId?: string
  buildingId?: string
  floorId?: string
  rentingSelling?: number
  isLock?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room/control/page', method: 'get', params })
}

export function updateRoomControl(
  id: string,
  data: { rentingSelling?: number; leasePrice?: number; salePrice?: number; isOrder?: number }
) {
  return request({ url: `/room/control/${id}`, method: 'put', params: data })
}

export function batchUpdateRoomControl(data: {
  ids: number[]
  rentingSelling?: number
  leasePrice?: number
  salePrice?: number
}) {
  return request({ url: '/room/control/batch', method: 'put', data })
}

export function lockRoom(data: {
  roomId: string
  enterpriseId?: number
  enterpriseName?: string
  reason: string
  days?: number
}) {
  return request({ url: '/room/control/lock', method: 'post', data })
}

export function unlockRoom(data: {
  roomId: string
  reason: string
}) {
  return request({ url: '/room/control/unlock', method: 'post', data })
}
