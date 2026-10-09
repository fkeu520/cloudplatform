import { describe, it, expect } from 'vitest'
import { explainPermissionError } from '@/composables/usePerms'

/**
 * Smoke test: verify a real module from src loads and behaves correctly.
 * explainPermissionError is a pure function — ideal for a deterministic test.
 */
describe('kefu-frontend smoke test', () => {
  it('explainPermissionError returns guidance for a "Permission denied" detail', () => {
    const msg = explainPermissionError({
      response: { status: 403, data: { detail: 'Permission denied: kefu:chat' } }
    })
    expect(msg).toContain('kefu:chat')
    expect(msg).toContain('权限')
  })

  it('explainPermissionError falls back to generic 403 copy when detail is empty', () => {
    const msg = explainPermissionError({
      response: { status: 403, data: { detail: '' } }
    })
    expect(msg).toBe('您没有执行此操作的权限，请联系管理员开通。')
  })

  it('explainPermissionError maps 401 to a re-login message', () => {
    const msg = explainPermissionError({
      response: { status: 401, data: {} }
    })
    expect(msg).toContain('登录已过期')
  })

  it('explainPermissionError returns "操作失败" when nothing is useful', () => {
    expect(explainPermissionError(null)).toBe('操作失败')
  })
})
