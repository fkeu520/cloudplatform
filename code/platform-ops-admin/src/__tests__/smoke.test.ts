import { describe, it, expect } from 'vitest'
import request from '@/api/request'

/**
 * Smoke test: verify the axios request instance and API modules load correctly
 * with the expected shape and defaults.
 */
describe('platform-ops-admin smoke test', () => {
  it('request instance has /api baseURL and 30s timeout', () => {
    expect(request.defaults.baseURL).toBe('/api')
    expect(request.defaults.timeout).toBe(30000)
  })

  it('axios module exposes expected HTTP verbs', () => {
    expect(typeof request.get).toBe('function')
    expect(typeof request.post).toBe('function')
    expect(typeof request.delete).toBe('function')
    expect(typeof request.put).toBe('function')
  })
})
