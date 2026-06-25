import request from './request'

export interface Organization {
  id?: string
  code?: string
  name: string
  parentId?: string
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

export function getOrgById(id: string) {
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

export function updateOrg(id: string, data: Organization) {
  return request({
    url: `/org/${id}`,
    method: 'put',
    data
  })
}

export function deleteOrg(id: string) {
  return request({
    url: `/org/${id}`,
    method: 'delete'
  })
}
