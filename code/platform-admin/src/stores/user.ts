import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
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
  }

  // 检查是否有某个权限
  // F5: 空字符串/空白字符串返回 false（避免权限检查被绕过）
  const hasPermission = (permission: string): boolean => {
    if (!permission || permission.trim() === '') return false
    return permissions.value.includes(permission)
  }

  // 检查是否有任意一个权限
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
