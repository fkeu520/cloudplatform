import request from './request'

export interface Role {
  id?: number
  code: string
  name: string
  status?: number
  sort?: number
  remark?: string
}

export function getRolePage(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({
    url: '/role/page',
    method: 'get',
    params
  })
}

export function getRoleList(status?: number) {
  return request({
    url: '/role/list',
    method: 'get',
    params: { status }
  })
}

export function getRoleById(id: number) {
  return request({
    url: `/role/${id}`,
    method: 'get'
  })
}

export function createRole(data: Role) {
  return request({
    url: '/role',
    method: 'post',
    data
  })
}

export function updateRole(id: number, data: Role) {
  return request({
    url: `/role/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id: number) {
  return request({
    url: `/role/${id}`,
    method: 'delete'
  })
}

export function getRoleMenuIds(id: number) {
  return request({
    url: `/role/${id}/menuIds`,
    method: 'get'
  })
}

export function assignRoleMenus(id: number, menuIds: number[]) {
  return request({
    url: `/role/${id}/menus`,
    method: 'post',
    data: menuIds
  })
}
