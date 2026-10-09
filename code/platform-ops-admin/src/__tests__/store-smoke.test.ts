import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

describe('ops-admin user store (smoke)', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('initial state is empty when no token in localStorage', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.permissions).toEqual([])
    expect(store.menus).toEqual([])
  })

  it('setToken persists to localStorage under key "token"', () => {
    const store = useUserStore()
    store.setToken('ops-test-token')
    expect(store.token).toBe('ops-test-token')
    expect(localStorage.getItem('token')).toBe('ops-test-token')
  })

  it('hasPermission rejects empty/whitespace strings and checks membership', () => {
    const store = useUserStore()
    store.setPermissions(['ops:tenant:list', 'ops:audit:view'])
    expect(store.hasPermission('ops:tenant:list')).toBe(true)
    expect(store.hasPermission('ops:tenant:add')).toBe(false)
    expect(store.hasPermission('')).toBe(false)
    expect(store.hasPermission('   ')).toBe(false)
  })

  it('logout clears state and removes token from localStorage', () => {
    const store = useUserStore()
    store.setToken('t1')
    store.setPermissions(['p1'])
    store.logout()
    expect(store.token).toBe('')
    expect(store.permissions).toEqual([])
    expect(localStorage.getItem('token')).toBeNull()
  })
})
