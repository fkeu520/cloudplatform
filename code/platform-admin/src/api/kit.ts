import request from './request'

export interface Kit {
  id?: number
  kitName?: string
  amount?: number
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getKitPage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/kit/page', method: 'get', params })
}

export function getKitById(id: number) {
  return request({ url: `/kit/${id}`, method: 'get' })
}

export function createKit(data: any) {
  return request({ url: '/kit', method: 'post', data })
}

export function updateKit(id: number, data: any) {
  return request({ url: `/kit/${id}`, method: 'put', data })
}

export function deleteKit(id: number) {
  return request({ url: `/kit/${id}`, method: 'delete' })
}