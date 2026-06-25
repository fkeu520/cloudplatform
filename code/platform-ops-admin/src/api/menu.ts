import request from './request'

export interface Menu {
  id: string
  parentId: string
  name: string
  path?: string
  component?: string
  type: number
  icon?: string
  sort: number
  perms?: string
  status: number
  children?: Menu[]
}

export function getMenuTree() {
  return request.get('/ops-user/menu/tree')
}
