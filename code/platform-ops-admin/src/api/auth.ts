import request from './request'

export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

/** OPA2-6: 登出 — 后端使 Token 失效 */
export function logout() {
  return request.post('/auth/logout')
}

/**
 * 签发 step-up token (v8 P0-3 高敏操作前)
 * <p>需要重新输密码 (强鉴权), 返回 5 分钟短期 token, 绑定 IP/UA.</p>
 * @param data.password  RSA 加密后的密码 (前端用 crypto.rsaEncrypt 加密)
 * @param data.scope     操作范围 (如 'tenant:delete')
 * @param data.singleUse 是否单次有效 (默认 true)
 */
export function issueStepUp(data: {
  password: string
  scope: string
  singleUse?: boolean
}) {
  return request.post('/auth/step-up/issue', data)
}

/**
 * 撤销当前用户所有未使用的 step-up token (改密码/主动踢下时用)
 */
export function revokeStepUp(reason?: string) {
  return request.post('/auth/step-up/revoke', { reason })
}
