import request from './request'

export interface Park {
  id?: string
  parkName: string
  province?: string
  city?: string
  district?: string
  address?: string
  longitude?: number
  latitude?: number
  description?: string
  landArea?: number
  buildingArea?: number
  status?: number
  createTime?: string
}

export interface RegionNode {
  code: string
  name: string
  children?: RegionNode[]
}

export function getParkPage(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({
    url: '/park/page',
    method: 'get',
    params
  })
}

export function getParkById(id: string) {
  return request({
    url: `/park/${id}`,
    method: 'get'
  })
}

export function createPark(data: Park) {
  return request({
    url: '/park',
    method: 'post',
    data
  })
}

export function updatePark(id: string, data: Park) {
  return request({
    url: `/park/${id}`,
    method: 'put',
    data
  })
}

export function deletePark(id: string) {
  return request({
    url: `/park/${id}`,
    method: 'delete'
  })
}

export function toggleParkStatus(id: string, status: number) {
  return request({
    url: `/park/${id}/status`,
    method: 'patch',
    params: { status }
  })
}

export function getParkList() {
  return request({
    url: '/park/list',
    method: 'get'
  })
}

export function getRegionTree() {
  return request({
    url: '/park/region/tree',
    method: 'get'
  })
}
