import request from './request'

export interface OpsUser {
  id?: string
  username: string
  password?: string
  nickname?: string
  mobile?: string
  email?: string
  status?: number
  createTime?: string
}

export function page(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/ops-user/page', { params })
}

export function getById(id: string) {
  return request.get(`/ops-user/${id}`)
}

export function create(data: Record<string, any>) {
  return request.post('/ops-user', data)
}

export function update(id: string, data: Record<string, any>) {
  return request.put(`/ops-user/${id}`, data)
}

export function remove(id: string) {
  return request.delete(`/ops-user/${id}`)
}

export function toggleStatus(id: string) {
  return request.post(`/ops-user/${id}/toggle-status`)
}

export function resetPassword(id: string, newPassword: string) {
  return request.post(`/ops-user/${id}/reset-password`, { newPassword })
}

export function getMenuIds(id: string) {
  return request.get(`/ops-user/${id}/menuIds`)
}

export function assignMenus(id: string, menuIds: number[]) {
  return request.post(`/ops-user/${id}/menus`, { menuIds })
}
