import request from './request'

export interface Menu {
  id?: string
  name: string
  path?: string
  component?: string
  icon?: string
  type: number
  sort?: number
  status?: number
  parentId?: string
  perms?: string
}

export function getMenuTree() {
  return request({
    url: '/menu/tree',
    method: 'get'
  })
}

export function getMenuNav() {
  return request({
    url: '/menu/nav',
    method: 'get'
  })
}

export function getUserMenus(appId?: number) {
  return request({
    url: '/menu/user',
    method: 'get',
    params: { appId }
  })
}

export function getUserPermissions() {
  return request({
    url: '/menu/perms',
    method: 'get'
  })
}

export function getMenuById(id: string) {
  return request({
    url: `/menu/${id}`,
    method: 'get'
  })
}

export function createMenu(data: Menu) {
  return request({
    url: '/menu',
    method: 'post',
    data
  })
}

export function updateMenu(id: string, data: Menu) {
  return request({
    url: `/menu/${id}`,
    method: 'put',
    data
  })
}

export function deleteMenu(id: string) {
  return request({
    url: `/menu/${id}`,
    method: 'delete'
  })
}
