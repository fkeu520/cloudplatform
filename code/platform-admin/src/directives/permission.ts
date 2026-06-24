import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

// 权限指令
// F5: 使用 display:none 替代 removeChild (可逆，权限变化后可通过 updated 恢复)
const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    applyPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    applyPermission(el, binding)
  }
}

function applyPermission(el: HTMLElement, binding: DirectiveBinding) {
  const { value } = binding
  const userStore = useUserStore()

  const hasAccess = value
    ? (value instanceof Array && value.length > 0
        ? userStore.hasAnyPermission(value)
        : typeof value === 'string'
          ? userStore.hasPermission(value)
          : false)
    : false

  el.style.display = hasAccess ? '' : 'none'
}

export default permission
