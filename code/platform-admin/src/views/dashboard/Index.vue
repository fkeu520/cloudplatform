<template>
  <div class="dashboard">
    <!-- 顶部欢迎条 -->
    <div class="welcome-bar">
      <div class="welcome-text">
        <span class="welcome-greeting">{{ greeting }}</span>
        <span class="welcome-user">, {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}</span>
        <span class="welcome-tip">今天是 {{ todayStr }}</span>
      </div>
      <div class="welcome-summary">
        <span class="summary-item">
          <el-icon><List /></el-icon>
          <strong>{{ welcome.todoCount }}</strong> 待办
        </span>
        <span class="summary-item">
          <el-icon><Bell /></el-icon>
          <strong>{{ welcome.msgCount }}</strong> 未读
        </span>
        <span class="summary-item">
          <el-icon><Document /></el-icon>
          <strong>{{ welcome.myApplyCount }}</strong> 我的申请
        </span>
        <span class="summary-item">
          <el-icon><Notification /></el-icon>
          <strong>{{ welcome.sysNoticeCount }}</strong> 系统通知
        </span>
      </div>
    </div>

    <!-- 模块 A: 4 张统计卡 -->
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-icon blue">
          <el-icon><List /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ welcome.todoCount }}</p>
          <p class="stat-label">待办任务</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ welcome.msgCount }}</p>
          <p class="stat-label">未读消息</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon purple">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ welcome.myApplyCount }}</p>
          <p class="stat-label">我的申请</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon green">
          <el-icon><Notification /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ welcome.sysNoticeCount }}</p>
          <p class="stat-label">系统通知</p>
        </div>
      </el-card>
    </div>

    <!-- 模块 B + C: 待办 + 消息两栏 -->
    <div class="two-col">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>我的待办</span>
            <el-button text size="small" @click="$router.push('/workflow/task-todo')">查看全部</el-button>
          </div>
        </template>
        <div v-if="loading" class="loading-block">加载中...</div>
        <div v-else-if="todos.length === 0" class="empty-block">暂无待办, 今天可以早点下班 🎉</div>
        <el-timeline v-else>
          <el-timeline-item v-for="t in todos" :key="t.id" :timestamp="formatTime(t.createTime)">
            <span class="todo-title">{{ t.title }}</span>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>未读消息</span>
            <el-button text size="small" @click="$router.push('/message/list')">查看全部</el-button>
          </div>
        </template>
        <div v-if="loading" class="loading-block">加载中...</div>
        <div v-else-if="messages.length === 0" class="empty-block">暂无未读消息</div>
        <div v-else class="msg-list">
          <div v-for="m in messages" :key="m.id" class="msg-item">
            <span class="msg-dot" />
            <div class="msg-content">
              <div class="msg-title">{{ m.title }}</div>
              <div class="msg-meta">{{ formatTime(m.createTime) }}</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 模块 D: 快捷入口 (按当前 app + 用户权限动态) -->
    <el-card class="shortcut-card">
      <template #header>
        <div class="card-header">
          <span>快捷入口</span>
          <span class="card-sub">按当前应用 + 您的权限</span>
        </div>
      </template>
      <div v-if="loading" class="loading-block">加载中...</div>
      <div v-else-if="shortcuts.length === 0" class="empty-block">当前应用暂无快捷入口</div>
      <div v-else class="shortcut-grid">
        <div
          v-for="s in shortcuts"
          :key="s.id"
          class="shortcut-item"
          @click="$router.push(s.path)"
        >
          <i v-if="s.icon" :class="s.icon" class="shortcut-icon" />
          <span class="shortcut-label">{{ s.name }}</span>
        </div>
      </div>
    </el-card>

    <!-- 模块 E + F: 业务数据 + 公告 -->
    <div class="bottom-row">
      <el-card class="business-card">
        <template #header>
          <div class="card-header">
            <span>业务数据</span>
            <span class="card-sub">{{ businessAppCode || '全平台' }}</span>
          </div>
        </template>
        <div v-if="loading" class="loading-block">加载中...</div>
        <div v-else-if="businessStats.length === 0" class="empty-block">该应用暂无业务数据</div>
        <div v-else class="business-grid">
          <div v-for="stat in businessStats" :key="stat.key" class="business-item">
            <div class="business-label">{{ stat.label }}</div>
            <div class="business-value">{{ stat.value }}<span class="business-unit">{{ stat.unit || '' }}</span></div>
            <div class="business-trend" :class="stat.trend">
              {{ stat.trend === 'up' ? '↑' : stat.trend === 'down' ? '↓' : '—' }}
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="notice-card">
        <template #header>
          <div class="card-header">
            <span>系统公告</span>
            <el-button text size="small">查看全部</el-button>
          </div>
        </template>
        <div v-if="loading" class="loading-block">加载中...</div>
        <div v-else-if="announcements.length === 0" class="empty-block">暂无系统公告</div>
        <div v-else class="notice-list">
          <div v-for="n in announcements" :key="n.id" class="notice-item">
            <el-tag :type="noticeTagType(n.type)" size="small" class="notice-tag">{{ noticeTypeLabel(n.type) }}</el-tag>
            <div class="notice-title">{{ n.title }}</div>
            <div class="notice-time">{{ formatTime(n.publishTime) }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 模块 G: 系统资源 (仅运营管理员) -->
    <el-card v-if="systemStats && systemStats.source === 'prometheus'" class="system-card">
      <template #header>
        <div class="card-header">
          <span>系统资源 (来自 container-exporter)</span>
          <span class="card-sub">仅运营管理员可见</span>
        </div>
      </template>
      <div class="system-grid">
        <div class="system-item">
          <div class="system-label">CPU 使用率</div>
          <el-progress :percentage="formatPercent(systemStats.cpuUsage)" :color="progressColor(systemStats.cpuUsage)" />
          <div class="system-value">{{ formatPercent(systemStats.cpuUsage) }}%</div>
        </div>
        <div class="system-item">
          <div class="system-label">内存使用率</div>
          <el-progress :percentage="formatPercent(systemStats.memoryUsage)" :color="progressColor(systemStats.memoryUsage)" />
          <div class="system-value">{{ formatPercent(systemStats.memoryUsage) }}%</div>
        </div>
        <div class="system-item">
          <div class="system-label">磁盘使用率</div>
          <el-progress :percentage="formatPercent(systemStats.diskUsage)" :color="progressColor(systemStats.diskUsage)" />
          <div class="system-value">{{ formatPercent(systemStats.diskUsage) }}%</div>
        </div>
        <div class="system-item">
          <div class="system-label">容器数</div>
          <div class="system-big-value">{{ systemStats.containerCount }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Bell, List, Document, Notification } from '@element-plus/icons-vue'
import {
  getDashboardWelcome,
  getDashboardTodos,
  getDashboardMessages,
  getDashboardShortcuts,
  getDashboardBusinessStats,
  getDashboardSystemStats,
  type DashboardWelcome,
  type DashboardTodo,
  type DashboardMessage,
  type DashboardShortcut,
  type DashboardBusinessStat,
  type DashboardSystemStats
} from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ============ 状态 ============

const welcome = reactive<DashboardWelcome>({
  todoCount: 0,
  msgCount: 0,
  myApplyCount: 0,
  sysNoticeCount: 0
})

const todos = ref<DashboardTodo[]>([])
const messages = ref<DashboardMessage[]>([])
const shortcuts = ref<DashboardShortcut[]>([])
const businessStats = ref<DashboardBusinessStat[]>([])
const systemStats = ref<DashboardSystemStats | null>(null)

// 公告卡片数据 (W3+ 阶段直接调 /announcement/recent)
const announcements = ref<Array<{ id: number; type: number; title: string; publishTime: string }>>([
  // W3 阶段: 内置 mock 公告 (Dashboard.vue 重写后, 待 /announcement/recent 端点接入)
  { id: 101, type: 0, title: '【v7.5 平台升级】新增首页工作台 + 顶部菜单动态权限', publishTime: '2026-06-15T19:00:00' },
  { id: 102, type: 0, title: '【系统管理】菜单结构调整 W3 启动', publishTime: '2026-06-15T18:30:00' },
  { id: 103, type: 1, title: '【空间中心】csyh 业务融合 Phase 0 即将启动', publishTime: '2026-06-15T18:00:00' }
])

const businessAppCode = ref<string>('') // 当前 appCode (W3 阶段简化: 总是 'system')
const loading = ref<boolean>(false)

// ============ 计算属性 ============

const todayStr = computed(() => {
  const d = new Date()
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} 周${weekDays[d.getDay()]}`
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// ============ 数据加载 ============

async function loadWelcome() {
  try {
    const res: any = await getDashboardWelcome()
    Object.assign(welcome, res.data)
  } catch (e) { /* silent */ }
}

async function loadTodos() {
  try {
    const res: any = await getDashboardTodos(5)
    todos.value = res.data || []
  } catch (e) { /* silent */ }
}

async function loadMessages() {
  try {
    const res: any = await getDashboardMessages(5)
    messages.value = res.data || []
  } catch (e) { /* silent */ }
}

async function loadShortcuts() {
  try {
    const res: any = await getDashboardShortcuts()
    shortcuts.value = res.data || []
  } catch (e) { /* silent */ }
}

async function loadBusinessStats() {
  try {
    const res: any = await getDashboardBusinessStats('system')
    businessStats.value = res.data || []
  } catch (e) { /* silent */ }
}

async function loadSystemStats() {
  try {
    const res: any = await getDashboardSystemStats()
    if (res.data && Object.keys(res.data).length > 0) {
      systemStats.value = res.data
    }
  } catch (e) { /* silent */ }
}

async function loadAll() {
  loading.value = true
  await Promise.all([
    loadWelcome(),
    loadTodos(),
    loadMessages(),
    loadShortcuts(),
    loadBusinessStats(),
    loadSystemStats()
  ])
  loading.value = false
}

// ============ 辅助方法 ============

function formatTime(dt: any): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatPercent(val: number): number {
  return Math.round(val * 10) / 10
}

function progressColor(val: number): string {
  if (val < 60) return '#67c23a'
  if (val < 80) return '#e6a23c'
  return '#f56c6c'
}

function noticeTagType(type: number): 'primary' | 'success' | 'warning' {
  if (type === 1) return 'success'
  if (type === 2) return 'warning'
  return 'primary'
}

function noticeTypeLabel(type: number): string {
  if (type === 1) return '业务'
  if (type === 2) return '维护'
  return '平台'
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.dashboard { padding: 20px; }

/* 顶部欢迎条 */
.welcome-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.welcome-text { display: flex; gap: 8px; align-items: baseline; flex-wrap: wrap; }
.welcome-greeting { font-size: 18px; font-weight: 600; }
.welcome-user { font-size: 18px; }
.welcome-tip { font-size: 13px; opacity: 0.85; margin-left: 12px; }
.welcome-summary { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; }
.summary-item { display: inline-flex; gap: 4px; align-items: center; opacity: 0.95; }
.summary-item strong { font-size: 16px; }

/* 统计卡 4 张 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}
.stat-icon.blue { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.orange { background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%); }
.stat-icon.purple { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); color: #666; }
.stat-icon.green { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.stat-content { flex: 1; }
.stat-value { font-size: 28px; font-weight: bold; color: #333; margin: 0; }
.stat-label { font-size: 14px; color: #999; margin: 4px 0 0 0; }

/* 两栏 (待办 + 消息) */
.two-col {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
.info-card { min-height: 240px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.card-sub { font-size: 12px; color: #909399; font-weight: normal; }

/* 待办 */
.todo-title { font-size: 13px; color: #303133; }

/* 消息 */
.msg-list { display: flex; flex-direction: column; gap: 8px; }
.msg-item { display: flex; align-items: flex-start; gap: 8px; padding: 8px 4px; border-radius: 4px; }
.msg-item:hover { background: #f5f7fa; }
.msg-dot { width: 8px; height: 8px; border-radius: 50%; background: #409eff; margin-top: 6px; flex-shrink: 0; }
.msg-content { flex: 1; min-width: 0; }
.msg-title { font-size: 13px; color: #303133; }
.msg-meta { font-size: 11px; color: #909399; margin-top: 2px; }

/* 快捷入口 */
.shortcut-card { margin-bottom: 20px; }
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
}
.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.shortcut-item:hover { background: #f5f7fa; transform: translateY(-2px); }
.shortcut-icon { font-size: 22px; color: #409eff; }
.shortcut-label { font-size: 12px; color: #606266; text-align: center; }

/* 底部: 业务数据 + 公告 */
.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.business-card, .notice-card { min-height: 200px; }

.business-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.business-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}
.business-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.business-value { font-size: 20px; font-weight: bold; color: #303133; }
.business-unit { font-size: 12px; color: #909399; margin-left: 4px; }
.business-trend { font-size: 12px; margin-top: 4px; color: #909399; }

.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice-item {
  padding: 10px;
  background: #fafafa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}
.notice-tag { margin-bottom: 4px; }
.notice-title { font-size: 13px; color: #303133; line-height: 1.5; }
.notice-time { font-size: 11px; color: #909399; margin-top: 4px; }

/* 系统资源 (运营管理员) */
.system-card { margin-bottom: 20px; }
.system-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.system-item { text-align: center; padding: 16px 8px; background: #fafafa; border-radius: 8px; }
.system-label { font-size: 13px; color: #606266; margin-bottom: 12px; }
.system-value { font-size: 18px; font-weight: bold; color: #303133; margin-top: 8px; }
.system-big-value { font-size: 32px; font-weight: bold; color: #409eff; margin-top: 12px; }

/* 通用 */
.loading-block, .empty-block {
  padding: 32px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .two-col, .bottom-row { grid-template-columns: 1fr; }
  .shortcut-grid { grid-template-columns: repeat(4, 1fr); }
  .system-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>