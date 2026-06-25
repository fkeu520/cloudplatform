import request from './request'

export function page(params: any) { return request.get('/gateway-route/page', { params }) }
export function list(status?: number) { return request.get('/gateway-route/list', { params: { status } }) }
export function getById(id: string) { return request.get(`/gateway-route/${id}`) }
export function create(data: any) { return request.post('/gateway-route', data) }
export function update(id: string, data: any) { return request.put(`/gateway-route/${id}`, data) }
export function remove(id: string) { return request.delete(`/gateway-route/${id}`) }
export function toggleStatus(id: string) { return request.post(`/gateway-route/${id}/toggle-status`) }
