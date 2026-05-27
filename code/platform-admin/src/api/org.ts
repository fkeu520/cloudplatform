import request from './request'

export interface Organization {
  id?: number
  code?: string
  name: string
  parentId?: number
  type?: number
  sort?: number
  status?: number
  remark?: string
}

export function getOrgTree() {
  return request({
    url: '/org/tree',
    method: 'get'
  })
}

export function getOrgById(id: number) {
  return request({
    url: `/org/${id}`,
    method: 'get'
  })
}

export function createOrg(data: Organization) {
  return request({
    url: '/org',
    method: 'post',
    data
  })
}

export function updateOrg(id: number, data: Organization) {
  return request({
    url: `/org/${id}`,
    method: 'put',
    data
  })
}

export function deleteOrg(id: number) {
  return request({
    url: `/org/${id}`,
    method: 'delete'
  })
}
