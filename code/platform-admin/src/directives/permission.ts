import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

// 权限指令
const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()

    if (value && value instanceof Array && value.length > 0) {
      const hasPermission = userStore.hasAnyPermission(value)
      if (!hasPermission) {
        el.parentNode?.removeChild(el)
      }
    } else if (value && typeof value === 'string') {
      const hasPermission = userStore.hasPermission(value)
      if (!hasPermission) {
        el.parentNode?.removeChild(el)
      }
    }
  }
}

export default permission
