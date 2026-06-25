import request from './request'

export interface User {
  id?: string
  username: string
  password?: string
  nickname?: string
  mobile?: string
  email?: string
  gender?: number
  orgId?: string | null
  deptId?: string | null
  postId?: string | null
  status?: number
  roleIds?: string[]
}

export interface UserPageVO {
  id: string
  username: string
  nickname: string
  mobile: string
  email: string
  orgId: string | null
  deptId: string | null
  postId: string | null
  orgName: string
  deptName: string
  postName: string
  statusDesc: string
  userType: number
  userTypeDesc: string
  createTime: string
  lastLoginTime: string
}

export function login(data: { username: string; password: string }) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

export function getUserPage(params: {
  keyword?: string
  orgId?: number
  orgIds?: string
  deptId?: number
  postId?: number
  tenantId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({
    url: '/user/page',
    method: 'get',
    params
  })
}

export function getUserById(id: string) {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

export function createUser(data: User) {
  return request({
    url: '/user',
    method: 'post',
    data
  })
}

export function updateUser(id: string, data: Partial<User>) {
  return request({
    url: `/user/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id: string) {
  return request({
    url: `/user/${id}`,
    method: 'delete'
  })
}

export function toggleUserStatus(id: string) {
  return request({
    url: `/user/${id}/toggle-status`,
    method: 'post'
  })
}

export function assignUserRoles(id: string, roleIds: string[]) {
  return request({
    url: `/user/${id}/roles`,
    method: 'post',
    data: { roleIds }
  })
}

export function resetUserPassword(id: string, newPassword: string) {
  return request({
    url: `/user/${id}/reset-password`,
    method: 'post',
    data: { newPassword }
  })
}