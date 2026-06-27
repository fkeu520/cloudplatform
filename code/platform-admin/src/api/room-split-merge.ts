import request from './request'

export interface RoomSplitMerge {
  id?: string
  userId?: number
  userName?: string
  reasons?: string
  type?: number
  oldRoomId?: string
  oldRoomName?: string
  newRoomId?: string
  newRoomName?: string
  num?: number
  isExtend?: number
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getRoomSplitMergePage(params: {
  parkId?: string
  status?: number
  type?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room-split-merge/page', method: 'get', params })
}

export function getRoomSplitMergeById(id: string) {
  return request({ url: `/room-split-merge/${id}`, method: 'get' })
}

export function createRoomSplitMerge(data: any) {
  return request({ url: '/room-split-merge', method: 'post', data })
}

/** 合并多个房间为一个新房间 (C6 操作列调�? */
export function mergeRooms(data: {
  parkId?: string
  buildingId?: string
  floorId?: string
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
  oldRoomIds: string[]
}) {
  return request({ url: '/room-split-merge/merge', method: 'post', data })
}

/** 拆分一个房间为多个新房?(C6 操作列调? */
export function splitRoom(data: {
  parkId?: string
  buildingId?: string
  oldRoomId: string
  reasons?: string
  num: number
  roomList: Array<{ roomNo: string; roomName?: string; areaCovered?: number; buildArea?: number; billableArea?: number; monthlyRent?: number; unitPrice?: number }>
}) {
  return request({ url: '/room-split-merge/split', method: 'post', data })
}

/** 还原拆分合并 */
export function restoreSplitMerge(id: string, type: number) {
  return request({ url: `/room-split-merge/restore/${id}/${type}`, method: 'post' })
}