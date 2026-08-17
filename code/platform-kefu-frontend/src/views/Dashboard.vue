<template>
  <div class="dashboard-page">
    <header>
      <h1>数据看板</h1>
      <div class="user">
        <nav>
          <router-link to="/">智能问答</router-link>
          <router-link to="/knowledge">知识库</router-link>
          <router-link to="/dashboard">数据看板</router-link>
          <router-link to="/sessions">会话管理</router-link>
          <router-link to="/faqs">FAQ管理</router-link>
          <router-link to="/settings">数据源</router-link>
          <router-link to="/evaluation">评估</router-link>
        </nav>
        <span>数据看板</span>
      </div>
    </header>
    <div class="content">
      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M8 20V12M13 20V8M18 20V14M23 20V10" stroke="#007AFF" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats?.document_count || 0 }}</div>
            <div class="stat-label">文档总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M7 18l5-5 4 4 5-7 5 5" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats?.chunk_count || 0 }}</div>
            <div class="stat-label">知识切片</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M14 8v6l4 2" stroke="#007AFF" stroke-width="2" stroke-linecap="round"/>
              <circle cx="14" cy="14" r="7" stroke="#007AFF" stroke-width="2"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalQuestions }}</div>
            <div class="stat-label">问答次数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M9 14h10M14 9l5 5-5 5" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgLatency }}ms</div>
            <div class="stat-label">平均响应</div>
          </div>
        </div>
      </div>

      <!-- 客服业务统计 -->
      <div class="stats-grid" v-if="dashboardStats" style="margin-top: 14px;">
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M6 18l4-4 4 4 8-8" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardStats.total_sessions }}</div>
            <div class="stat-label">总会话数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <circle cx="14" cy="14" r="7" stroke="#007AFF" stroke-width="2"/>
              <path d="M11 14l2 2 4-4" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardStats.ai_resolution_rate }}%</div>
            <div class="stat-label">AI 解决率</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M14 6l2.5 5.5L22 12l-4 4 1 6-5-3-5 3 1-6-4-4 5.5-.5L14 6z" stroke="#007AFF" stroke-width="2" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardStats.avg_satisfaction ? dashboardStats.avg_satisfaction.toFixed(1) : '-' }}</div>
            <div class="stat-label">平均满意度</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M14 6v8M14 14l4 4" stroke="#007AFF" stroke-width="2" stroke-linecap="round"/>
              <circle cx="14" cy="14" r="8" stroke="#007AFF" stroke-width="2"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardStats.recent_24h_sessions }}</div>
            <div class="stat-label">24h 新增</div>
          </div>
        </div>
      </div>

      <!-- 数据源命中 -->
      <div class="section-card" v-if="dashboardStats?.data_source_hit_counts && Object.keys(dashboardStats.data_source_hit_counts).length">
        <h3>数据源命中统计</h3>
        <div class="ds-hit-list">
          <div v-for="(count, source) in dashboardStats.data_source_hit_counts" :key="source" class="ds-hit-item">
            <span class="ds-name">{{ source }}</span>
            <div class="ds-bar-bg">
              <div class="ds-bar" :style="{ width: Math.min(count * 10, 100) + '%' }"></div>
            </div>
            <span class="ds-count">{{ count }}</span>
          </div>
        </div>
      </div>

      <!-- 问答历史 -->
      <div class="section-card">
        <h3>问答历史</h3>
        <div class="table-wrap" v-if="history.length">
          <table>
            <thead>
              <tr>
                <th>问题</th>
                <th>回答摘要</th>
                <th>模型</th>
                <th>响应时间</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in history" :key="item.ask_id">
                <td class="question">{{ item.question }}</td>
                <td class="answer">{{ item.answer.substring(0, 80) }}{{ item.answer.length > 80 ? '...' : '' }}</td>
                <td class="mono">{{ item.model }}</td>
                <td class="mono">{{ item.latency_ms }}ms</td>
                <td class="muted">{{ formatDate(item.timestamp) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <p>暂无问答记录</p>
        </div>
      </div>

      <!-- 文档列表 -->
      <div class="section-card">
        <h3>最近文档</h3>
        <div class="table-wrap" v-if="documents.length">
          <table>
            <thead>
              <tr>
                <th>文件名</th>
                <th>类型</th>
                <th>切片数</th>
                <th>状态</th>
                <th>上传时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in documents" :key="doc.doc_id">
                <td>{{ doc.name }}</td>
                <td class="muted">{{ doc.type }}</td>
                <td class="mono">{{ doc.chunk_count }}</td>
                <td>
                  <span :class="['status-badge', doc.status]">{{ statusText(doc.status) }}</span>
                </td>
                <td class="muted">{{ formatDate(doc.upload_time) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <p>暂无文档</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { knowledgeApi, chatApi, dashboardApi } from '@/api'

const stats = ref<any>(null)
const documents = ref<any[]>([])
const history = ref<any[]>([])
const dashboardStats = ref<any>(null)

const totalQuestions = computed(() => dashboardStats.value?.total_messages || history.value.length)
const avgLatency = computed(() => {
  if (!history.value.length) return 0
  const total = history.value.reduce((sum: number, h: any) => sum + h.latency_ms, 0)
  return Math.round(total / history.value.length)
})

const loadData = async () => {
  try {
    const [docsRes, statsRes, historyRes, dashRes] = await Promise.all([
      knowledgeApi.listDocs(),
      knowledgeApi.stats(),
      chatApi.history(),
      dashboardApi.stats()
    ])
    documents.value = (docsRes.data.items || []).slice(0, 10)
    stats.value = statsRes.data
    history.value = historyRes.data.items || []
    dashboardStats.value = dashRes.data
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const statusText = (status: string) => {
  const map: Record<string, string> = {
    processing: '处理中',
    ready: '就绪',
    failed: '失败'
  }
  return map[status] || status
}

onMounted(loadData)
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: #f2f2f7;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 32px;
  background: #ffffff;
  border-bottom: 1px solid #d2d2d7;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
header h1 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
}
.user {
  display: flex;
  align-items: center;
  gap: 12px;
}
nav {
  display: flex;
  gap: 4px;
  margin-right: 12px;
  background: #f2f2f7;
  padding: 3px;
  border-radius: 10px;
}
nav a {
  color: #6e6e73;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  padding: 5px 14px;
  border-radius: 7px;
  transition: background 0.2s, color 0.2s;
}
nav a:hover { color: #1d1d1f; }
nav a.router-link-active {
  background: #ffffff;
  color: #007AFF;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.user span {
  font-size: 13px;
  color: #6e6e73;
}
.user button {
  padding: 6px 16px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  transition: background 0.2s;
}
.user button:hover { background: #e5e5ea; }
.content {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 24px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}
.stat-card {
  background: #ffffff;
  padding: 20px 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.stat-info { flex: 1; }
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: -0.5px;
}
.stat-label {
  font-size: 12px;
  color: #86868b;
  margin-top: 2px;
  font-weight: 500;
}
.section-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.section-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 18px;
}
.table-wrap {
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
th, td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #f2f2f7;
}
th {
  color: #86868b;
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
tr:last-child td { border-bottom: none; }
tr:hover td { background: #f9f9fb; }
.question {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1d1d1f;
  font-weight: 500;
}
.answer {
  max-width: 280px;
  color: #6e6e73;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, monospace;
  font-size: 13px;
}
.muted { color: #86868b; }
.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}
.status-badge.ready { background: #d1f7d1; color: #248a3d; }
.status-badge.processing { background: #fff3e0; color: #cc7a00; }
.status-badge.failed { background: #FFD9D9; color: #d32f2f; }
.empty-state {
  text-align: center;
  color: #86868b;
  padding: 48px;
  font-size: 14px;
}
.ds-hit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ds-hit-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ds-name {
  width: 100px;
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
}
.ds-bar-bg {
  flex: 1;
  height: 24px;
  background: #f2f2f7;
  border-radius: 12px;
  overflow: hidden;
}
.ds-bar {
  height: 100%;
  background: linear-gradient(90deg, #007AFF, #4da3ff);
  border-radius: 12px;
  transition: width 0.5s ease;
}
.ds-count {
  width: 40px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  color: #007AFF;
}
</style>
