import request from './request'

export interface Space {
  id?: number
  parkId?: number
  areaId?: number
  categoryId?: number
  spaceName?: string
  spaceDescribe?: string
  status?: number
  tenantId?: number
  createTime?: string
}

export function getSpacePage(params: {
  keyword?: string
  parkId?: number
  categoryId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/space/page', method: 'get', params })
}

export function getSpaceById(id: number) {
  return request({ url: `/space/${id}`, method: 'get' })
}

export function createSpace(data: any) {
  return request({ url: '/space', method: 'post', data })
}

export function updateSpace(id: number, data: any) {
  return request({ url: `/space/${id}`, method: 'put', data })
}

export function deleteSpace(id: number) {
  return request({ url: `/space/${id}`, method: 'delete' })
}

export function checkSpaceName(parkId: number, categoryId: number, spaceName: string, excludeId?: number) {
  return request({ url: '/space/check-name', method: 'get', params: { parkId, categoryId, spaceName, excludeId } })
}
