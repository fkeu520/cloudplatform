import request from './request'

export function page(params: any) { return request.get('/storage/page', { params }) }
export function list() { return request.get('/storage/list') }
export function getById(id: string) { return request.get(`/storage/${id}`) }
export function create(data: any) { return request.post('/storage', data) }
export function update(id: string, data: any) { return request.put(`/storage/${id}`, data) }
export function remove(id: string, stepUpToken?: string) {
  return request.delete(`/storage/${id}`, {
    headers: stepUpToken ? { 'X-Step-Up-Token': stepUpToken } : undefined
  })
}
export function testConnection(id: string) { return request.post(`/storage/${id}/test`) }
export function listBuckets(id: string) { return request.get(`/storage/${id}/buckets`) }
export function createBucket(id: string, bucketName: string) { return request.post(`/storage/${id}/buckets`, null, { params: { bucketName } }) }
