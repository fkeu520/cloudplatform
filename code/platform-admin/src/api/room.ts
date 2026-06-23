import request from './request'

export interface Room {
  id?: number
  parkId?: number
  buildingId?: number
  floorId?: number
  floor?: number
  roomNo?: string
  roomName?: string
  roomType?: string
  houseStructure?: number
  areaCovered?: number
  buildArea?: number
  billableArea?: number
  unitPrice?: number
  totalPrice?: number
  monthlyRent?: number
  kitId?: number
  purposeId?: number
  image?: string
  sorting?: number
  introduce?: string
  status?: number
  remark?: string
  tenantId?: number
  createTime?: string
}

export function getRoomPage(params: {
  keyword?: string
  roomType?: string
  status?: number
  parkId?: number
  buildingId?: number
  floorId?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room/page', method: 'get', params })
}

export function getRoomById(id: number) {
  return request({ url: `/room/${id}`, method: 'get' })
}

export function createRoom(data: any) {
  return request({ url: '/room', method: 'post', data })
}

export function updateRoom(id: number, data: any) {
  return request({ url: `/room/${id}`, method: 'put', data })
}

export function deleteRoom(id: number) {
  return request({ url: `/room/${id}`, method: 'delete' })
}

export function updateRoomStatus(id: number, status: number) {
  return request({ url: `/room/${id}/status`, method: 'patch', params: { status } })
}

export function checkRoomNo(parkId: number, buildingId: number, roomNo: string, excludeId?: number) {
  return request({ url: '/room/check-no', method: 'get', params: { parkId, buildingId, roomNo, excludeId } })
}
