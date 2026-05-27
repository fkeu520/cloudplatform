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
        meta: { title: '租户管理', icon: 'UserFilled' }
      },
      {
        path: 'storage',
        name: 'StorageMgmt',
        component: () => import('../views/storage/Index.vue'),
        meta: { title: '对象存储', icon: 'FolderOpened' }
      },
      {
        path: 'gateway',
        name: 'GatewayMgmt',
        component: () => import('../views/gateway/Index.vue'),
        meta: { title: '服务网关', icon: 'Connection' }
      },
      {
        path: 'audit',
        name: 'AuditMgmt',
        component: () => import('../views/audit/Index.vue'),
        meta: { title: '日志审计', icon: 'List' }
      },
      {
        path: 'message/record',
        name: 'MessageRecord',
        component: () => import('../views/message/Record.vue'),
        meta: { title: '消息记录', icon: 'Bell' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) {
    next('/login')
  } else {
    next()
  }
})

export default router
