import request from './request'

export function page(params: any) { return request.get('/app/page', { params }) }
export function list(appType?: number) { return request.get('/app/list', { params: { appType } }) }
export function getById(id: string) { return request.get(`/app/${id}`) }
export function create(data: any) { return request.post('/app', data) }
export function update(id: string, data: any) { return request.put(`/app/${id}`, data) }
export function remove(id: string) { return request.delete(`/app/${id}`) }
