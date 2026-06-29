import { createRouter, createWebHashHistory } from 'vue-router'
import Knowledge from '@/views/Knowledge.vue'
import Chat from '@/views/Chat.vue'
import Dashboard from '@/views/Dashboard.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'chat', component: Chat },
    { path: '/knowledge', name: 'knowledge', component: Knowledge },
    { path: '/dashboard', name: 'dashboard', component: Dashboard },
  ]
})

export default router
