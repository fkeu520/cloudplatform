import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('initial state is empty', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.permissions).toEqual([])
    expect(store.menus).toEqual([])
  })

  it('setToken saves to localStorage', () => {
    const store = useUserStore()
    store.setToken('test-token-123')
    expect(store.token).toBe('test-token-123')
    expect(localStorage.getItem('token')).toBe('test-token-123')
  })

  it('setPermissions updates permission list', () => {
    const store = useUserStore()
    const perms = ['system:user:list', 'system:role:list', 'workflow:leave:apply']
    store.setPermissions(perms)
    expect(store.permissions).toEqual(perms)
    expect(store.permissions.length).toBe(3)
  })

  it('hasPermission checks single permission', () => {
    const store = useUserStore()
    store.setPermissions(['system:user:list', 'system:role:list'])
    expect(store.hasPermission('system:user:list')).toBe(true)
    expect(store.hasPermission('system:user:add')).toBe(false)
    // F5: 空字符串/空白字符串返回 false（避免权限检查被绕过）
    expect(store.hasPermission('')).toBe(false)
    expect(store.hasPermission('   ')).toBe(false)
  })

  it('hasAnyPermission checks any of multiple permissions', () => {
    const store = useUserStore()
    store.setPermissions(['workflow:definition:deploy'])
    expect(store.hasAnyPermission(['system:user:list', 'workflow:definition:deploy'])).toBe(true)
    expect(store.hasAnyPermission(['system:user:list', 'system:role:add'])).toBe(false)
    // F5: 空数组返回 false（避免权限检查被绕过）
    expect(store.hasAnyPermission([])).toBe(false)
  })

  it('logout clears all state and localStorage', () => {
    const store = useUserStore()
    store.setToken('test-token')
    store.setPermissions(['perm1'])
    store.setMenus([{ id: 1, name: 'test' }])
    store.setUserInfo({ id: 1, username: 'admin' })

    store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.permissions).toEqual([])
    expect(store.menus).toEqual([])
    expect(localStorage.getItem('token')).toBeNull()
  })
})
