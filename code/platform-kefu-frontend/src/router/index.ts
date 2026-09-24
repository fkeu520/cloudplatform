import { createRouter, createWebHashHistory } from 'vue-router'
import Knowledge from '@/views/Knowledge.vue'
import Chat from '@/views/Chat.vue'
import Dashboard from '@/views/Dashboard.vue'
import Sessions from '@/views/Sessions.vue'
import Faqs from '@/views/Faqs.vue'
import Import from '@/views/Import.vue'
import Settings from '@/views/Settings.vue'
import Evaluation from '@/views/Evaluation.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'chat', component: Chat },
    // 2026-08-18 修复: Iframe.vue 将 /kefu/chat 映射为 /kefu-frontend/#/chat,
    // 原 router 只有 path '/', #/chat 不匹配导致 Chat.vue 不挂载, 无法创建会话/发送
    { path: '/chat', name: 'chat-explicit', component: Chat },
    { path: '/knowledge', name: 'knowledge', component: Knowledge },
    { path: '/dashboard', name: 'dashboard', component: Dashboard },
    { path: '/sessions', name: 'sessions', component: Sessions },
    { path: '/faqs', name: 'faqs', component: Faqs },
    { path: '/import', name: 'import', component: Import },
    { path: '/settings', name: 'settings', component: Settings },
    { path: '/evaluation', name: 'evaluation', component: Evaluation },
  ]
})

export default router