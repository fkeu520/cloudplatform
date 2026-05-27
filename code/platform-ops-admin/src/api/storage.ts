import request from './request'

export function page(params: any) { return request.get('/storage/page', { params }) }
export function list() { return request.get('/storage/list') }
export function getById(id: number) { return request.get(`/storage/${id}`) }
export function create(data: any) { return request.post('/storage', data) }
export function update(id: number, data: any) { return request.put(`/storage/${id}`, data) }
export function remove(id: number) { return request.delete(`/storage/${id}`) }
export function testConnection(id: number) { return request.post(`/storage/${id}/test`) }
export function listBuckets(id: number) { return request.get(`/storage/${id}/buckets`) }
export function createBucket(id: number, bucketName: string) { return request.post(`/storage/${id}/buckets`, null, { params: { bucketName } }) }
