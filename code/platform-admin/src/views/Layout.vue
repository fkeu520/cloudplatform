<template>
  <el-container class="layout-container">
    <!-- 顶部: logo + 应用 tabs + 用户区 -->
    <el-header class="layout-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/dashboard')">云枢中台</div>
        <el-tabs v-model="activeApp" class="app-tabs" @tab-change="onAppChange">
          <el-tab-pane
            v-for="app in appList"
            :key="app.appCode"
            :name="app.appCode"
            :label="app.appName"
          />
        </el-tabs>
      </div>
      <div class="header-right">
        <div class="notify-wrapper" @mouseenter="showNotifyPopover" @mouseleave="hideNotifyDelayed">
          <el-badge :value="notifyCount" :hidden="notifyCount === 0" class="notify-badge">
            <el-button text size="large" style="border:none;font-size:20px;padding:4px">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>

          <el-popover ref="notifyPopoverRef" :visible="notifyVisible" placement="bottom-end" :width="360" trigger="manual" @show="onNotifyShow" @hide="onNotifyHide">
            <template #reference>
              <span></span>
            </template>
            <div class="notify-panel" @mouseenter="onPanelEnter" @mouseleave="hideNotifyDelayed">
              <div class="notify-header">
                <span>消息通知</span>
                <el-button v-if="notifyCount > 0" text size="small" @click="markAllRead">全部已读</el-button>
              </div>

              <div v-if="siteNotifyList.length > 0" class="notify-section">
                <div class="notify-section-title">系统消息</div>
                <div v-for="n in siteNotifyList" :key="'site-'+n.id" class="notify-item" @click="goSiteDetail(n)">
                  <div class="notify-dot notify-dot-blue" v-if="n.readStatus === 0" />
                  <div class="notify-dot notify-dot-read" v-else />
                  <div class="notify-content">
                    <div class="notify-title">{{ n.title }}</div>
                    <div class="notify-meta">{{ n.senderName || '系统' }} · {{ formatTime(n.createTime) }}</div>
                  </div>
                </div>
              </div>

              <div v-if="notifyList.length > 0" class="notify-section">
                <div class="notify-section-title">流程通知</div>
                <div v-for="n in notifyList" :key="'wf-'+n.id" class="notify-item" @click="gotoTask(n)">
                  <div class="notify-dot notify-dot-orange" />
                  <div class="notify-content">
                    <div class="notify-title">{{ n.taskName }}</div>
                    <div class="notify-meta">流程实例: {{ n.processInstanceId?.slice(0, 12) }}...</div>
                  </div>
                </div>
              </div>

              <div v-if="notifyList.length === 0 && siteNotifyList.length === 0" class="notify-empty">暂无新通知</div>

              <div class="notify-footer">
                <el-button text size="small" @click="goMessageList">
                  查看更多
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </el-popover>
        </div>

        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <!-- F7: 使用用户头像（如有），否则用空白默认头像避免 CDN 破图 -->
            <el-avatar :size="32" :src="avatarSrc" @error="onAvatarError">{{ avatarInitial }}</el-avatar>
            <span class="username">{{ userStore.userInfo?.nickname || displayName || userStore.userInfo?.username || '管理员' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧: 当前 app 下的菜单 (动态加载) -->
      <el-aside width="220px">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <template v-for="item in menuList" :key="item.path || item.id">
            <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
              <template #title>
                <i v-if="item.iconClass" :class="item.iconClass" style="margin-right:6px;width:16px;text-align:center" />
                <span>{{ item.name }}</span>
              </template>
              <template v-for="child in item.children" :key="child.path || child.id">
                <el-menu-item :index="child.path">
                  <i v-if="child.iconClass" :class="child.iconClass" style="margin-right:6px;width:16px;text-align:center" />
                  <span>{{ child.name }}</span>
                </el-menu-item>
              </template>
            </el-sub-menu>
            <el-menu-item v-else :index="item.path">
              <i v-if="item.iconClass" :class="item.iconClass" style="margin-right:6px;width:16px;text-align:center" />
              <span>{{ item.name }}</span>
            </el-menu-item>
          </template>
          <!-- 空状态: 当前 app 无菜单 -->
          <div v-if="menuList.length === 0 && appsLoaded" class="menu-empty">
            暂无菜单权限
          </div>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '@fortawesome/fontawesome-free/css/all.min.css'
import { Bell, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import { getUserMenus, getUserPermissions } from '@/api/menu'
import { getUserApps, type App } from '@/api/app'  // W3 P0-4: 新增顶部 tabs 数据源
import { getUnreadSiteMessageCount, getSiteMessagePage, markSiteMessageRead, markAllSiteMessageRead } from '@/api/message'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = () => route.path

// ========== W3 P1-1: 顶部应用 tabs + 左侧菜单 (数据驱动) ==========
const appList = ref<App[]>([])             // 用户有权限的应用列表 (顶部 tabs)
const menuList = ref<any[]>([])            // 当前 app 下的菜单树 (左侧)
const activeApp = ref<string>('')          // 当前选中的 app (顶部 tab)
const appsLoaded = ref<boolean>(false)     // app 列表是否已加载 (用于空状态判断)

const username = localStorage.getItem('username') || 'admin'
// 页面刷新后 userStore.userInfo 为 null, 用 localStorage 中存储的 nickname/username 做 fallback
const displayName = localStorage.getItem('nickname') || localStorage.getItem('username') || ''

// F7: 头像——优先使用用户头像 URL，否则显示首字
const avatarSrc = computed(() => userStore.userInfo?.avatar || '')
const avatarInitial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '管')[0])
function onAvatarError() { /* fallback: initials displayed via {{ avatarInitial }} */ }

// 通知相关 (保留原有逻辑)
const notifyCount = ref(0)
const notifyList = ref<any[]>([])
const siteNotifyList = ref<any[]>([])
const notifyVisible = ref(false)
const notifyPopoverRef = ref()
let prevCount = 0
let pollTimer: any = null
let sseSource: EventSource | null = null

const faIconMap: Record<string, string> = {
  DataAnalysis: 'fas fa-chart-bar',
  Setting: 'fas fa-cog',
  Grid: 'fas fa-th-large',
  OfficeBuilding: 'fas fa-building',
  Notebook: 'fas fa-book',
  Files: 'fas fa-file',
  FileText: 'fas fa-file-alt',
  Collection: 'fas fa-folder-open',
  Edit: 'fas fa-tasks',
  Monitor: 'fas fa-eye',
  Building: 'fas fa-city',
  Folder: 'fas fa-folder',
  User: 'fas fa-users',
  UserFilled: 'fas fa-user',
  List: 'fas fa-list',
  Role: 'fas fa-user-tag',
  Menu: 'fas fa-bars',
  Bell: 'fas fa-bell',
  Avatar: 'fas fa-avatar',
  Connection: 'fas fa-plug',
  Document: 'fas fa-file-alt'
}

// ========== W3 P1-1: 加载顶部应用列表 ==========
async function loadApps() {
  try {
    const res: any = await getUserApps()
    appList.value = res.data || []
    appsLoaded.value = true

    if (appList.value.length === 0) {
      // 用户没有任何 app 权限, 顶部 tabs 空
      activeApp.value = ''
      menuList.value = []
      return
    }

    // 默认选中: localStorage 记忆 > 第一个 app
    const saved = localStorage.getItem('activeApp')
    const exists = appList.value.find(a => a.appCode === saved)
    activeApp.value = exists ? saved! : appList.value[0].appCode!

    // 立即加载默认 app 的菜单
    await loadMenu(activeApp.value)
  } catch (e) {
    ElMessage.error('加载应用列表失败')
  }
}

// ========== W3 P1-1: 切换 tab → 加载该 app 的左侧菜单 ==========
async function loadMenu(appCode: string) {
  if (!appCode) {
    menuList.value = []
    return
  }
  try {
    // 找到当前 app 的 id, 传给后端过滤
    const currentApp = appList.value.find(a => a.appCode === appCode)
    const res: any = await getUserMenus(currentApp?.id)
    menuList.value = processMenus(res.data || [])
    // A2-1: 同步到 store，供路由守卫校验
    userStore.setMenus(menuList.value)
    localStorage.setItem('activeApp', appCode)
  } catch (e) {
    ElMessage.error('加载菜单失败')
    menuList.value = []
    userStore.setMenus([])
  }
}

// 切换 tab 回调
function onAppChange(newCode: string) {
  if (newCode) {
    loadMenu(newCode)
  }
}

// 监听路由变化, 记忆 activeApp (如果 URL 暗示某个 app)
watch(() => route.path, (newPath) => {
  // 可选: 根据 URL 推断 app (W4 阶段)
})

// ========== 通知铃铛相关 (保留) ==========
async function pollNotifies() {
  try {
    const [msgCountRes, msgListRes] = await Promise.all([
      getUnreadSiteMessageCount(username),
      getSiteMessagePage({ pageNum: 1, pageSize: 5, readStatus: 0, userId: username })
    ]) as any[]

    const messageCount = (msgCountRes?.data ?? msgCountRes) ?? 0

    notifyCount.value = typeof messageCount === 'number' ? messageCount : 0
    notifyList.value = []
    const siteData = msgListRes?.data || msgListRes
    siteNotifyList.value = siteData?.records || []

    const currentCount = notifyCount.value
    if (currentCount > prevCount && prevCount > 0) {
      const diff = currentCount - prevCount
      ElNotification({
        title: '新的通知',
        message: `您有 ${diff} 条新消息`,
        type: 'warning',
        duration: 5000
      })
    }
    prevCount = currentCount
  } catch { /* ignore */ }
}

let hideTimer: any = null

function showNotifyPopover() {
  if (hideTimer) clearTimeout(hideTimer)
  pollNotifies()
  notifyVisible.value = true
}

function hideNotifyDelayed() {
  hideTimer = setTimeout(() => { notifyVisible.value = false }, 200)
}

function onPanelEnter() {
  if (hideTimer) clearTimeout(hideTimer)
}

function onNotifyShow() {}

function onNotifyHide() {
  notifyVisible.value = false
}

function goMessageList() {
  notifyVisible.value = false
  router.push('/message/list')
}

function goSiteDetail(n: any) {
  notifyVisible.value = false
  router.push(`/message/detail/${n.id}`)
}

function formatTime(dt: any): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function gotoTask(n: any) {
  await markSiteMessageRead(n.id)
  notifyCount.value = Math.max(0, notifyCount.value - 1)
  notifyVisible.value = false
  router.push(n.businessId ? `/workflow/task-todo?processInstanceId=${n.businessId}` : '/workflow/task-todo')
}

async function markAllRead() {
  await markAllSiteMessageRead(username)
  notifyList.value = []
  siteNotifyList.value = []
  notifyVisible.value = false
  notifyCount.value = 0
}

// ========== 权限加载 + 菜单处理 (保留) ==========
const loadPermissions = async () => {
  try {
    const permRes: any = await getUserPermissions()
    if (permRes.data) userStore.setPermissions(permRes.data)
  } catch { /* ignore */ }
}

const processMenus = (menus: any[]): any[] => {
  return menus.map(item => {
    if (item.icon) {
      item.iconClass = item.icon.startsWith('fa') ? item.icon : (faIconMap[item.icon] || '')
    } else {
      item.iconClass = ''
    }
    delete item.icon
    if (item.children && item.children.length > 0) item.children = processMenus(item.children)
    return item
  })
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出')
  }
}

function connectSse() {
  const userId = localStorage.getItem('userId')
  if (!userId) return
  const base = import.meta.env.VITE_API_BASE || '/api'
  sseSource = new EventSource(`${base}/message/sse/subscribe?userId=${userId}`)
  sseSource.addEventListener('connected', () => {
    log('SSE 已连接')
  })
  sseSource.addEventListener('workflow-notify', (e: MessageEvent) => {
    try {
      const data = JSON.parse(e.data)
      notifyCount.value++
      ElNotification({ title: '新的流程通知', message: data.title || '待办任务', type: 'warning', duration: 5000 })
      pollNotifies()
    } catch { /* ignore */ }
  })
  sseSource.addEventListener('site-notify', (e: MessageEvent) => {
    try {
      const data = JSON.parse(e.data)
      notifyCount.value++
      ElNotification({ title: '新消息', message: data.title || '系统消息', type: 'info', duration: 5000 })
      pollNotifies()
    } catch { /* ignore */ }
  })
  sseSource.onerror = () => {
    log('SSE 连接异常，切换轮询模式')
    sseSource?.close()
    sseSource = null
    if (!pollTimer) pollTimer = setInterval(pollNotifies, 15000)
  }
}

function log(msg: string) {
  if (import.meta.env.DEV) console.log('[SSE]', msg)
}

onMounted(async () => {
  // W3 P1-1: 加载顶部应用 + 默认 app 菜单
  await loadApps()
  // 加载权限 (保留原有)
  await loadPermissions()
  // 通知相关 (保留)
  prevCount = 0
  await pollNotifies()
  try { connectSse() } catch { /* fallback to polling */ }
  if (!sseSource) pollTimer = setInterval(pollNotifies, 15000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (sseSource) {
    sseSource.removeEventListener('workflow-notify', () => {})
    sseSource.removeEventListener('site-notify', () => {})
    sseSource.onerror = null
    sseSource.close()
    sseSource = null
  }
})
</script>

<style scoped>
.layout-container { height: 100%; }
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .08);
  padding: 0 16px;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.logo {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  white-space: nowrap;
  width: 204px;
  text-align: center;
  flex-shrink: 0;
  cursor: pointer;
}
.app-tabs {
  flex: 1;
  min-width: 0;
}
.app-tabs :deep(.el-tabs__header) {
  margin: 0;
}
.app-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}
.app-tabs :deep(.el-tabs__item) {
  height: 60px;
  line-height: 60px;
  font-size: 14px;
}
.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-info :deep(.el-avatar) { box-shadow: 0 0 0 1px rgba(0,0,0,0.1); }
.username { color: #333; font-variant-numeric: tabular-nums; }
.el-aside { background-color: #304156; }
.sidebar-menu { border-right: none; }
.el-main { background: #f5f7fa; }
.menu-empty {
  padding: 24px 16px;
  color: #909399;
  text-align: center;
  font-size: 13px;
}
.notify-badge { line-height: 1; }
.notify-badge :deep(.el-badge__content) { top: 4px; right: 4px; font-size: 10px; padding: 0 4px; height: 16px; line-height: 16px; border: none; }
.notify-wrapper { position: relative; display: inline-flex; align-items: center; min-width: 40px; min-height: 40px; justify-content: center; }
.notify-panel { max-height: 400px; overflow-y: auto; }
.notify-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; font-weight: 600; font-size: 14px; }
.notify-section { margin-top: 4px; }
.notify-section-title { font-size: 12px; color: #909399; padding: 6px 4px 2px; font-weight: 500; }
.notify-empty { text-align: center; padding: 30px 0; color: #909399; font-size: 13px; }
.notify-list { margin-top: 4px; }
.notify-item { display: flex; align-items: flex-start; gap: 8px; padding: 10px 4px; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.2s; border-radius: 4px; }
.notify-item:hover { background: #f5f7fa; }
.notify-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.notify-dot-orange { background: #e6a23c; }
.notify-dot-blue { background: #409eff; }
.notify-dot-read { background: #c0c4cc; }
.notify-content { flex: 1; min-width: 0; }
.notify-title { font-size: 13px; color: #303133; }
.notify-meta { font-size: 11px; color: #909399; margin-top: 2px; }
.notify-footer { text-align: center; padding: 8px 0 0; border-top: 1px solid #ebeef5; margin-top: 8px; }
</style>