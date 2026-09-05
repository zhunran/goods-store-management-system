import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令：v-permission="'good:create'"
 * 无权限时移除该元素。
 */
export const permission: Directive = {
  mounted(el, binding) {
    const store = useUserStore()
    const code = binding.value as string
    if (code && !store.hasPermission(code)) {
      el.parentNode?.removeChild(el)
    }
  }
}
