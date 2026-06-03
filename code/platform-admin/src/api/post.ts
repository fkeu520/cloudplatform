import request from './request'

export interface Post {
  id?: number
  orgId?: number
  deptId?: number
  name?: string
  code?: string
  level?: string
  sort?: number
  status?: number
}

export function getPostPage(params: { pageNum?: number; pageSize?: number; orgId?: number; deptId?: number }) {
  return request({
    url: '/post/page',
    method: 'get',
    params
  })
}

export function getPostById(id: number) {
  return request({
    url: `/post/${id}`,
    method: 'get'
  })
}

export function getPostByOrgId(orgId: number) {
  return request({
    url: `/post/org/${orgId}`,
    method: 'get'
  })
}

export function getPostByDeptId(deptId: number | string) {
  return request({
    url: `/post/dept/${deptId}`,
    method: 'get'
  })
}

export function createPost(data: Post) {
  return request({
    url: '/post',
    method: 'post',
    data
  })
}

export function updatePost(id: number, data: Post) {
  return request({
    url: `/post/${id}`,
    method: 'put',
    data
  })
}

export function deletePost(id: number) {
  return request({
    url: `/post/${id}`,
    method: 'delete'
  })
}