import request from './request'

export interface SpaceCategory {
  id?: number
  typeName?: string
  typeDescribe?: string
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getSpaceCategoryPage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/space-category/page', method: 'get', params })
}

export function getSpaceCategoryById(id: number) {
  return request({ url: `/space-category/${id}`, method: 'get' })
}

export function createSpaceCategory(data: any) {
  return request({ url: '/space-category', method: 'post', data })
}

export function updateSpaceCategory(id: number, data: any) {
  return request({ url: `/space-category/${id}`, method: 'put', data })
}

export function deleteSpaceCategory(id: number) {
  return request({ url: `/space-category/${id}`, method: 'delete' })
}