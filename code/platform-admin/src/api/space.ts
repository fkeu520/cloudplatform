import request from './request'

export interface Space {
  id?: string
  parkId?: string
  areaId?: string
  categoryId?: string
  spaceName?: string
  spaceDescribe?: string
  status?: number
  tenantId?: string
  createTime?: string
}

export function getSpacePage(params: {
  keyword?: string
  parkId?: string
  categoryId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/space/page', method: 'get', params })
}

export function getSpaceById(id: string) {
  return request({ url: `/space/${id}`, method: 'get' })
}

export function createSpace(data: any) {
  return request({ url: '/space', method: 'post', data })
}

export function updateSpace(id: string, data: any) {
  return request({ url: `/space/${id}`, method: 'put', data })
}

export function deleteSpace(id: string) {
  return request({ url: `/space/${id}`, method: 'delete' })
}

export function checkSpaceName(parkId: string, categoryId: string, spaceName: string, excludeId?: string) {
  return request({ url: '/space/check-name', method: 'get', params: { parkId, categoryId, spaceName, excludeId } })
}
