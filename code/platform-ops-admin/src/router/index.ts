import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: Layout,
    redirect: '/tenant',
    children: [
      {
        path: 'tenant',
        name: 'TenantMgmt',
        component: () => import('../views/tenant/Index.vue'),
        meta: { title: '租户管理', icon: 'fas fa-building' }
      },
      {
        path: 'storage',
        name: 'StorageMgmt',
        component: () => import('../views/storage/Index.vue'),
        meta: { title: '对象存储', icon: 'fas fa-database' }
      },
      {
        path: 'gateway',
        name: 'GatewayMgmt',
        component: () => import('../views/gateway/Index.vue'),
        meta: { title: '服务网关', icon: 'fas fa-plug' }
      },
      {
        path: 'audit',
        name: 'AuditMgmt',
        component: () => import('../views/audit/Index.vue'),
        meta: { title: '日志审计', icon: 'fas fa-clipboard-list' }
      },
      {
        path: 'message/record',
        name: 'MessageRecord',
        component: () => import('../views/message/Record.vue'),
        meta: { title: '消息记录', icon: 'fas fa-bell' }
      },
      {
        path: 'ops-user',
        name: 'OpsUserMgmt',
        component: () => import('../views/ops-user/Index.vue'),
        meta: { title: '用户管理', icon: 'fas fa-users-cog' }
      },
      {
        path: 'monitor',
        name: 'SystemMonitor',
        component: () => import('../views/monitor/Index.vue'),
        meta: { title: '系统监控', icon: 'fas fa-heartbeat' }
      },
      {
        path: 'ops-entry',
        name: 'OpsEntry',
        component: () => import('../views/ops-entry/Index.vue'),
        meta: { title: '运维管理', icon: 'fas fa-tools' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  // O4: 已登录用户访问 /login 直接跳首页；未登录用户访问其他页跳登录
  if (to.path === '/login') {
    if (localStorage.getItem('token')) {
      next('/')
    } else {
      next()
    }
  } else if (!localStorage.getItem('token')) {
    next('/login')
  } else {
    next()
  }
})

export default router
