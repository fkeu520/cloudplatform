import request from './request'

/**
 * M5 P0-2 数据权限: 角色上的 dataScope 字段
 * 1=全部 2=本部门 3=本部门及下级 4=本人 5=自定义 (customDeptIds 必填)
 */
export interface Role {
  id?: number
  code: string
  name: string
  status?: number
  sort?: number
  remark?: string
  dataScope?: number
  customDeptIds?: string
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
