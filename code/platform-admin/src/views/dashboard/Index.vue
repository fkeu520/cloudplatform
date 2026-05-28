<template>
  <div class="dashboard">
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-icon blue">
          <UserFilled />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.totalUsers }}</p>
          <p class="stat-label">用户总数</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon green">
          <UserFilled />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.todayLogin }}</p>
          <p class="stat-label">今日登录</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon orange">
          <Bell />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.unreadMessages }}</p>
          <p class="stat-label">未读消息</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon purple">
          <List />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.pendingTasks }}</p>
          <p class="stat-label">待办任务</p>
        </div>
      </el-card>
    </div>

    <div class="info-cards">
      <el-card class="info-card">
        <template #header><span>最近活动</span></template>
        <el-timeline v-if="recentLogs.length > 0">
          <el-timeline-item
            v-for="log in recentLogs"
            :key="log.id"
            :timestamp="log.loginTime"
            placement="top"
          >
            <el-card size="small">
              {{ log.message || (log.status === 1 ? '登录成功' : '登录失败') }}
              <span style="color:#909399;font-size:12px"> — {{ log.username }}</span>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="no-data">暂无活动记录</div>
      </el-card>
      <el-card class="info-card">
        <template #header><span>快速操作</span></template>
        <div class="quick-actions">
          <el-button type="primary" size="small" @click="$router.push('/system/user')">新增用户</el-button>
          <el-button size="small" @click="$router.push('/system/role')">角色管理</el-button>
          <el-button size="small" @click="$router.push('/system/config')">系统配置</el-button>
          <el-button size="small" @click="$router.push('/system/dict')">字典管理</el-button>
        </div>
      </el-card>
    </div>
    <el-card class="chart-card">
      <template #header><span>登录趋势（近7天）</span></template>
      <div ref="chartRef" style="height:260px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { UserFilled, Bell, List } from '@element-plus/icons-vue'
import { getUserCount, getTodayLoginCount, getRecentLogs } from '../../api/dashboard'
import { getUnreadSiteMessageCount } from '../../api/message'
import { getTodoTasks } from '../../api/workflow'
import * as echarts from 'echarts'

const username = localStorage.getItem('username') || 'admin'

const stats = reactive({
  totalUsers: 0,
  todayLogin: 0,
  unreadMessages: 0,
  pendingTasks: 0
})

const recentLogs = ref<any[]>([])
const loading = ref(false)

async function fetchStats() {
  loading.value = true
  try {
    const [userRes, loginRes, msgRes, taskRes, logsRes] = await Promise.all([
      getUserCount().catch(() => ({ data: { total: 0 } })),
      getTodayLoginCount().catch(() => ({ data: { total: 0 } })),
      getUnreadSiteMessageCount(username).catch(() => ({ data: 0 })),
      getTodoTasks({ userId: username, pageNum: 1, pageSize: 1 }).catch(() => ({ data: { total: 0 } })),
      getRecentLogs().catch(() => ({ data: { records: [] } }))
    ])
    stats.totalUsers = (userRes as any).data?.total || 0
    stats.todayLogin = (loginRes as any).data?.total || 0
    stats.unreadMessages = (msgRes as any).data ?? 0
    stats.pendingTasks = (taskRes as any).data?.total || 0
    recentLogs.value = (logsRes as any).data?.records?.slice(0, 5) || []
  } catch { /* ignore */ } finally { loading.value = false }
}

onMounted(fetchStats)

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function initChart() {
  nextTick(() => {
    if (!chartRef.value) return
    chart = echarts.init(chartRef.value)
    const now = new Date()
    const days = Array.from({ length: 7 }, (_, i) => {
      const d = new Date(now)
      d.setDate(d.getDate() - (6 - i))
      return `${d.getMonth() + 1}/${d.getDate()}`
    })
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: days },
      yAxis: { type: 'value' },
      series: [{
        name: '登录次数', type: 'bar', data: days.map(() => Math.floor(Math.random() * 50)),
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
      }]
    })
  })
}

onMounted(initChart)
onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

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

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #666;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin: 4px 0 0 0;
}

.info-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-card {
  min-height: 200px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chart-card {
  margin-top: 20px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .info-cards {
    grid-template-columns: 1fr;
  }
}
</style>