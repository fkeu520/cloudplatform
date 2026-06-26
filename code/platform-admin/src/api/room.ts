import request from './request'

export interface Room {
  id?: string
  parkId?: string
  areaId?: string
  buildingId?: string
  floorId?: string
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
  kitId?: string
  purposeId?: string
  rentingSelling?: number
  leasePrice?: number
  salePrice?: number
  isLock?: number
  isOrder?: number
  image?: string
  sorting?: number
  introduce?: string
  status?: number
  remark?: string
  tenantId?: string
  createTime?: string
}

export function getRoomPage(params: {
  keyword?: string
  roomType?: string
  status?: number
  parkId?: string
  buildingId?: string
  floorId?: string
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/room/page', method: 'get', params })
}

export function getRoomById(id: string) {
  return request({ url: `/room/${id}`, method: 'get' })
}

export function createRoom(data: any) {
  return request({ url: '/room', method: 'post', data })
}

export function updateRoom(id: string, data: any) {
  return request({ url: `/room/${id}`, method: 'put', data })
}

export function deleteRoom(id: string) {
  return request({ url: `/room/${id}`, method: 'delete' })
}

export function updateRoomStatus(id: string, status: number) {
  return request({ url: `/room/${id}/status`, method: 'patch', params: { status } })
}

export function checkRoomNo(parkId: string, buildingId: string, roomNo: string, excludeId?: string) {
  return request({ url: '/room/check-no', method: 'get', params: { parkId, buildingId, roomNo, excludeId } })
}
