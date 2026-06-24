import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * O5: 运营后台用户状态 Store（参考 platform-admin stores/user.ts）
 * 统一管理 token/userInfo/permissions，消除 localStorage 直接读写
 */
export const useUserStore = defineStore('ops-user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const permissions = ref<string[]>([])
  const menus = ref<any[]>([])

  const setToken = (t: string) => {
    token.value = t
    localStorage.setItem('token', t)
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
  }

  const setPermissions = (perms: string[]) => {
    permissions.value = perms
  }

  const setMenus = (menuList: any[]) => {
    menus.value = menuList
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    menus.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
  }

  const hasPermission = (permission: string): boolean => {
    if (!permission || permission.trim() === '') return false
    return permissions.value.includes(permission)
  }

  const hasAnyPermission = (permissionList: string[]): boolean => {
    if (!permissionList || permissionList.length === 0) return false
    return permissionList.some(perm => permissions.value.includes(perm))
  }

  return {
    token,
    userInfo,
    permissions,
    menus,
    setToken,
    setUserInfo,
    setPermissions,
    setMenus,
    logout,
    hasPermission,
    hasAnyPermission
  }
})
