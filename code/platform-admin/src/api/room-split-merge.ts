import request from './request'

export interface RoomSplitMerge {
  id?: number
  userId?: number
  userName?: string
  reasons?: string
  type?: number
  oldRoomId?: number
  oldRoomName?: string
  newRoomId?: number
  newRoomName?: string
  num?: number
  isExtend?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getRoomSplitMergePage(params: {
  parkId?: number
  status?: number
  type?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-split-merge/page', method: 'get', params })
}

export function getRoomSplitMergeById(id: number) {
  return request({ url: `/room-split-merge/${id}`, method: 'get' })
}

export function createRoomSplitMerge(data: any) {
  return request({ url: '/room-split-merge', method: 'post', data })
}

/** 合并多个房间为一个新房间 (C6 操作列调用) */
export function mergeRooms(data: {
  parkId?: number
  buildingId?: number
  floorId?: number
  floor?: number
  roomNo: string
  roomName?: string
  roomType?: string
  areaCovered?: number
  buildArea?: number
  billableArea?: number
  unitPrice?: number
  monthlyRent?: number
  reasons?: string
  oldRoomIds: number[]
}) {
  return request({ url: '/room-split-merge/merge', method: 'post', data })
}

/** 拆分一个房间为多个新房间 (C6 操作列调用) */
export function splitRoom(data: {
  parkId?: number
  buildingId?: number
  oldRoomId: number
  reasons?: string
  num: number
  roomList: Array<{ roomNo: string; roomName?: string; areaCovered?: number; buildArea?: number; billableArea?: number; monthlyRent?: number; unitPrice?: number }>
}) {
  return request({ url: '/room-split-merge/split', method: 'post', data })
}

/** 还原拆分合并 */
export function restoreSplitMerge(id: number, type: number) {
  return request({ url: `/room-split-merge/restore/${id}/${type}`, method: 'post' })
}