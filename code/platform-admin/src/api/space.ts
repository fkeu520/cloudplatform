import request from './request'

export interface Space {
  id?: number
  spaceName?: string
  spaceDescribe?: string
  parkId?: number
  areaId?: number
  categoryId?: number
  status?: number
  tenantId?: number
  createTime?: string
}

export function getSpacePage(params: {
  keyword?: string
  parkId?: number
  areaId?: number
  categoryId?: number
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