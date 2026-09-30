import { ref, computed } from 'vue'
import api from '@/api'

/**
 * 前端权限判断 (2026-09-30)
 *
 * 背景: kefu 后端每个写接口都做服务端校验 (app/core/access.py has_permission),
 * 但前端此前完全不做权限判断 —— 按钮对所有人显示, 点击后才拿 403,
 * 而 Knowledge.vue 把任何异常都显示成笼统的「上传失败」, 用户无从判断原因。
 *
 * 数据来源: GET /menu/perms -> platform-user 汇总该用户角色绑定的所有
 * sys_menu.perms。平台管理员 (userType=2) 走 has_permission 的绕过分支,
 * 拿不到也能操作, 所以这里对 userType=2 直接放行, 避免管理员被误挡。
 *
 * 注意: 这只是**体验层**的收敛, 不能替代服务端校验 —— 前端隐藏按钮不等于
 * 权限安全, 真正的闸门始终在 kefu / platform-user 后端。
 */

const perms = ref<string[]>([])
const loaded = ref(false)
const loading = ref(false)

// 与 platform-user 共享的 JWT 解析口径: 取不到就按非平台管理员处理 (保守)
const userType = ref<number | null>(null)

function decodeJwt(token: string): Record<string, any> | null {
  try {
    const payload = token.split('.')[1]
    const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4)
    return JSON.parse(atob(padded.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

export function usePerms() {
  /** 拉取一次权限列表, 之后走缓存。并发调用只会发一次请求。 */
  async function load(force = false): Promise<string[]> {
    if (loaded.value && !force) return perms.value
    if (loading.value) return perms.value
    loading.value = true
    try {
      const token = localStorage.getItem('token') || localStorage.getItem('kefu_token')
      if (token) {
        const claims = decodeJwt(token)
        if (claims) {
          // 优先取 claim 里的 userType; 取不到再从 permissions 长度猜
          userType.value = claims.userType != null ? Number(claims.userType) : null
        }
        const res = await api.get('/menu/perms')
        perms.value = (res.data?.data as string[]) || []
      } else {
        perms.value = []
      }
      loaded.value = true
    } catch {
      // 拉不到就退化成"什么都不给", 后端仍会兜底校验
      perms.value = []
      loaded.value = true
    } finally {
      loading.value = false
    }
    return perms.value
  }

  /** 是否拥有某权限 (平台管理员一律 true, 与后端绕过逻辑一致) */
  function can(perm: string | string[] | undefined | null): boolean {
    if (!perm) return true
    if (userType.value === 2) return true
    const list = Array.isArray(perm) ? perm : [perm]
    if (!list.length) return true
    return list.some((p) => perms.value.includes(p))
  }

  /** 任一权限即可 */
  function canAny(...permsList: string[]): boolean {
    return can(permsList)
  }

  return {
    perms,
    loaded,
    loading,
    userType,
    load,
    can,
    canAny,
    isPlatformAdmin: computed(() => userType.value === 2)
  }
}

/** 把后端 403 的 "Permission denied: xxx" 翻成用户能看懂的话 */
export function explainPermissionError(err: any): string {
  const status = err?.response?.status
  const detail = err?.response?.data?.detail || err?.response?.data?.message || ''
  const m = /Permission denied:\s*([\w:_-]+)/.exec(detail)
  if (m) {
    return `您没有「${m[1]}」权限，无法完成此操作。请联系管理员在「角色管理 → 设置权限」中为您开通。`
  }
  if (status === 403) {
    return '您没有执行此操作的权限，请联系管理员开通。'
  }
  if (status === 401) {
    return '登录已过期，请回到管理后台重新登录。'
  }
  return detail || '操作失败'
}
