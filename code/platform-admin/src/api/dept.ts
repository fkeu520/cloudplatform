import request from './request'

export interface Dept {
  id?: string
  orgId?: number
  parentId?: string
  name?: string
  code?: string
  manager?: string
  phone?: string
  sort?: number
  status?: number
  children?: Dept[]
}

export function getDeptTree(orgId?: number) {
  return request({
    url: '/dept/tree',
    method: 'get',
    params: orgId ? { orgId } : {}
  })
}

export function getDeptList(orgId: number | string) {
  return request({
    url: `/dept/org/${orgId}`,
    method: 'get'
  })
}

export function getDeptById(id: string) {
  return request({
    url: `/dept/${id}`,
    method: 'get'
  })
}

export function createDept(data: Dept) {
  return request({
    url: '/dept',
    method: 'post',
    data
  })
}

export function updateDept(id: string, data: Dept) {
  return request({
    url: `/dept/${id}`,
    method: 'put',
    data
  })
}

export function deleteDept(id: string) {
  return request({
    url: `/dept/${id}`,
    method: 'delete'
  })
}