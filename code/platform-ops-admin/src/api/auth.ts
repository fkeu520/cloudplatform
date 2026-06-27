import request from './request'

export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

/** OPA2-6: 登出 — 后端使 Token 失效 */
export function logout() {
  return request.post('/auth/logout')
}
