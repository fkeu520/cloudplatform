import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import Layout from '@/views/Layout.vue'
import Login from '@/views/Login.vue'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '数据看板', icon: 'fas fa-chart-bar' }
      },
      {
        path: 'system/user',
        name: 'UserMgmt',
        component: () => import('@/views/system/user/Index.vue'),
        meta: { title: '用户管理', icon: 'fas fa-users', parent: 'system' }
      },
      {
        path: 'system/role',
        name: 'RoleMgmt',
        component: () => import('@/views/system/role/Index.vue'),
        meta: { title: '角色管理', icon: 'fas fa-user-tag', parent: 'system' }
      },
      {
        path: 'system/menu',
        name: 'MenuMgmt',
        component: () => import('@/views/system/menu/Index.vue'),
        meta: { title: '菜单管理', icon: 'fas fa-bars', parent: 'system' }
      },
      {
        path: 'system/org',
        name: 'OrgMgmt',
        component: () => import('@/views/system/org/Index.vue'),
        meta: { title: '组织管理', icon: 'fas fa-building', parent: 'system' }
      },
      {
        path: 'system/dept',
        name: 'DeptMgmt',
        component: () => import('@/views/system/dept/Index.vue'),
        meta: { title: '部门管理', icon: 'fas fa-city', parent: 'system' }
      },
      {
        path: 'system/post',
        name: 'PostMgmt',
        component: () => import('@/views/system/post/Index.vue'),
        meta: { title: '岗位管理', icon: 'fas fa-address-card', parent: 'system' }
      },
      {
        path: 'system/dict',
        name: 'DictMgmt',
        component: () => import('@/views/system/dict/Index.vue'),
        meta: { title: '字典管理', icon: 'fas fa-book', parent: 'system' }
      },
      {
        path: 'system/config',
        name: 'ConfigMgmt',
        component: () => import('@/views/system/config/Index.vue'),
        meta: { title: '参数配置', icon: 'fas fa-cog', parent: 'system' }
      },
      {
        path: 'system/config/data-scope',
        name: 'DataScopeUpgrade',
        component: () => import('@/views/system/config/DataScopeUpgrade.vue'),
        meta: { title: '数据权限配置', icon: 'fas fa-shield-alt', parent: 'system' }
      },
      {
        path: 'system/log',
        name: 'OperLog',
        component: () => import('@/views/system/log/Index.vue'),
        meta: { title: '日志查看', icon: 'fas fa-file-alt', parent: 'system' }
      },
      {
        path: 'workflow/definition',
        name: 'WorkflowDefinition',
        component: () => import('@/views/workflow/Definition.vue'),
        meta: { title: '流程定义', icon: 'fas fa-folder-open', parent: 'workflow' }
      },
      {
        path: 'workflow/task',
        name: 'WorkflowTask',
        component: () => import('@/views/workflow/TaskTodo.vue'),
        meta: { title: '我的待办', icon: 'fas fa-tasks', parent: 'workflow' }
      },
      {
        path: 'workflow/monitor',
        name: 'WorkflowMonitor',
        component: () => import('@/views/workflow/Monitor.vue'),
        meta: { title: '流程监控', icon: 'fas fa-eye', parent: 'workflow' }
      },
      {
        path: 'workflow/leave',
        name: 'WorkflowLeave',
        component: () => import('@/views/workflow/Leave.vue'),
        meta: { title: '请假申请', icon: 'fas fa-paper-plane', parent: 'workflow' }
      },
      {
        path: 'message/list',
        name: 'MessageList',
        component: () => import('@/views/message/List.vue'),
        meta: { title: '消息列表', icon: 'fas fa-bell' }
      },
      {
        path: 'message/detail/:id',
        name: 'MessageDetail',
        component: () => import('@/views/message/Detail.vue'),
        meta: { title: '消息详情', icon: 'fas fa-info-circle' }
      },
      {
        path: 'message/channel',
        name: 'MessageChannel',
        component: () => import('@/views/message/Channel.vue'),
        meta: { title: '渠道配置', icon: 'fas fa-wrench' }
      },
      {
        path: 'message/template',
        name: 'MessageTemplate',
        component: () => import('@/views/message/Template.vue'),
        meta: { title: '短信模板', icon: 'fas fa-sms' }
      },
      {
        path: 'message/record',
        name: 'MessageRecord',
        component: () => import('@/views/message/Record.vue'),
        meta: { title: '消息记录', icon: 'fas fa-list' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const WHITE_LIST = ['/login']
let isDynamicRoutesAdded = false

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const token = userStore.token || localStorage.getItem('token')
  
  console.log('[Router Guard] to:', to.path, 'token:', token ? token.slice(0, 20) + '...' : null)

  if (token) {
    if (to.path === '/login') {
      next('/')
    } else {
      // 首次加载时动态添加路由（这里简化处理，实际可以根据后端返回的菜单动态生成）
      if (!isDynamicRoutesAdded) {
        isDynamicRoutesAdded = true
      }
      next()
    }
  } else {
    if (WHITE_LIST.includes(to.path)) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
