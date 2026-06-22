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