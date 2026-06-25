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
    // W3 P1-2: 默认跳工作台 /dashboard (不再跳 system/user)
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '工作台', icon: 'fas fa-home', appCode: '*' /* 工作台属于全局 */ }
      },
      {
        path: 'system/user',
        name: 'UserMgmt',
        component: () => import('@/views/system/user/Index.vue'),
        meta: { title: '用户管理', icon: 'fas fa-users', parent: 'system', appCode: 'system' }
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
        path: 'system/park',
        name: 'ParkMgmt',
        component: () => import('@/views/system/park/Index.vue'),
        meta: { title: '园区管理', icon: 'fas fa-building', parent: 'system' }
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
      },
      // ========== 房源管理 ==========
      {
        path: 'building/page',
        name: 'BuildingMgmt',
        component: () => import('@/views/building/Index.vue'),
        meta: { title: '楼栋管理', icon: 'fas fa-warehouse', parent: 'property' }
      },
      {
        path: 'room/page',
        name: 'RoomMgmt',
        component: () => import('@/views/room/Index.vue'),
        meta: { title: '房间管理', icon: 'fas fa-door-closed', parent: 'property' }
      },
      {
        path: 'room-control/page',
        name: 'RoomControlMgmt',
        component: () => import('@/views/room-control/Index.vue'),
        meta: { title: '租售控制', icon: 'fas fa-sliders-h', parent: 'property' }
      },
      // ========== W3.3 简单 CRUD 实体 (park-space 后端 V32) ==========
      {
        path: 'area/page',
        name: 'AreaMgmt',
        component: () => import('@/views/area/Index.vue'),
        meta: { title: '区域管理', icon: 'fas fa-location', parent: 'space' }
      },
      {
        path: 'floor/page',
        name: 'FloorMgmt',
        component: () => import('@/views/floor/Index.vue'),
        meta: { title: '楼层管理', icon: 'fas fa-building', parent: 'space' }
      },
      {
        path: 'kit/page',
        name: 'KitMgmt',
        component: () => import('@/views/kit/Index.vue'),
        meta: { title: '配套管理', icon: 'fas fa-box', parent: 'space' }
      },
      {
        path: 'room-purpose/page',
        name: 'RoomPurposeMgmt',
        component: () => import('@/views/room-purpose/Index.vue'),
        meta: { title: '房源用途', icon: 'fas fa-tags', parent: 'space' }
      },
      {
        path: 'plan-use/page',
        name: 'PlanUseMgmt',
        component: () => import('@/views/plan-use/Index.vue'),
        meta: { title: '规划用途', icon: 'fas fa-aim', parent: 'space' }
      },
      {
        path: 'land-nature/page',
        name: 'LandNatureMgmt',
        component: () => import('@/views/land-nature/Index.vue'),
        meta: { title: '土地性质', icon: 'fas fa-mountain', parent: 'space' }
      },
      // ========== W3.4 关联实体 (park-space 后端 V33) ==========
      {
        path: 'covenant/page',
        name: 'CovenantMgmt',
        component: () => import('@/views/covenant/Index.vue'),
        meta: { title: '合同房间', icon: 'fas fa-file-contract', parent: 'space' }
      },
      {
        path: 'energy/page',
        name: 'EnergyMgmt',
        component: () => import('@/views/energy/Index.vue'),
        meta: { title: '能耗管理', icon: 'fas fa-bolt', parent: 'space' }
      },
      {
        path: 'equipment/page',
        name: 'EquipmentMgmt',
        component: () => import('@/views/equipment/Index.vue'),
        meta: { title: '设备设施', icon: 'fas fa-tools', parent: 'space' }
      },
      // ========== W3.5 空间实体 (park-space 后端 V34) ==========
      {
        path: 'space-category/page',
        name: 'SpaceCategoryMgmt',
        component: () => import('@/views/space-category/Index.vue'),
        meta: { title: '空间类别', icon: 'fas fa-th-large', parent: 'space' }
      },
      {
        path: 'space/page',
        name: 'SpaceMgmt',
        component: () => import('@/views/space/Index.vue'),
        meta: { title: '空间管理', icon: 'fas fa-compass', parent: 'space' }
      },
      {
        path: 'massif/page',
        name: 'MassifMgmt',
        component: () => import('@/views/massif/Index.vue'),
        meta: { title: '地块管理', icon: 'fas fa-map', parent: 'space' }
      },
      // ========== W3.6 Room 子表 (park-space 后端 V35, append-only) ==========
      {
        path: 'room-split-merge/page',
        name: 'RoomSplitMergeMgmt',
        component: () => import('@/views/room-split-merge/Index.vue'),
        meta: { title: '拆分合并记录', icon: 'fas fa-object-ungroup', parent: 'space' }
      },
      // room-record (绑定记录), room-lock-record (锁定记录) 暂保留在 room 弹窗 tabs 内
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
  // F4: 统一使用 store 中的 token（单数据源），避免 store 与 localStorage 状态不一致
  const token = userStore.token
  
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
