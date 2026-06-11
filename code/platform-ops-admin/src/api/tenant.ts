import request from './request'

export function page(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/tenant/page', { params })
}

export function list(status?: number) {
  return request.get('/tenant/list', { params: { status } })
}

export function getById(id: number) {
  return request.get(`/tenant/${id}`)
}

export function create(data: Record<string, any>) {
  return request.post('/tenant', data)
}

export function update(id: number, data: Record<string, any>) {
  return request.put(`/tenant/${id}`, data)
}

export function remove(id: number) {
  return request.delete(`/tenant/${id}`)
}

export function toggleStatus(id: number) {
  return request.post(`/tenant/${id}/toggle-status`)
}

export function listOrgs(id: number) {
  return request.get(`/tenant/${id}/orgs`)
}

export function listAdmins(id: number) {
  return request.get(`/tenant/${id}/admins`)
}

export function createAdmin(id: number, data: Record<string, any>) {
  return request.post(`/tenant/${id}/admin`, data)
}

export function deleteAdmin(tenantId: number, userId: number) {
  return request.delete(`/tenant/${tenantId}/admin/${userId}`)
}

export function resetAdminPassword(tenantId: number, userId: number, newPassword: string) {
  return request.post(`/tenant/${tenantId}/admin/${userId}/reset-password`, { newPassword })
}
