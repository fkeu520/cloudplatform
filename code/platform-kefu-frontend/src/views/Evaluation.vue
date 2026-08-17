<template>
  <div class="evaluation-page">
    <header>
      <h1>评估报告</h1>
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
        <span>评估报告</span>
      </div>
    </header>
    <div class="content">
      <!-- 统计卡片 -->
      <div class="stats-grid" v-if="stats">
        <div class="stat-card">
          <div class="stat-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="#E5F1FF"/>
              <path d="M6 18l4-4 4 4 8-8" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.total_sessions }}</div>
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
            <div class="stat-value">{{ stats.ai_resolution_rate }}%</div>
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
            <div class="stat-value">{{ stats.avg_satisfaction ? stats.avg_satisfaction.toFixed(1) : '-' }}</div>
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
            <div class="stat-value">{{ stats.recent_24h_sessions }}</div>
            <div class="stat-label">24h 新增</div>
          </div>
        </div>
      </div>

      <!-- 数据源命中 -->
      <div class="section-card" v-if="stats?.data_source_hit_counts && Object.keys(stats.data_source_hit_counts).length">
        <h3>数据源命中统计</h3>
        <div class="ds-hit-list">
          <div v-for="(count, source) in stats.data_source_hit_counts" :key="source" class="ds-hit-item">
            <span class="ds-name">{{ dsNameMap[source] || source }}</span>
            <div class="ds-bar-bg">
              <div class="ds-bar" :style="{ width: Math.min(count * 10, 100) + '%' }"></div>
            </div>
            <span class="ds-count">{{ count }}</span>
          </div>
        </div>
      </div>

      <!-- 会话评估查询 -->
      <div class="section-card">
        <h3>会话评估查询</h3>
        <div class="search-row">
          <input
            v-model="searchSessionId"
            placeholder="输入会话 ID..."
            @keyup.enter="searchEvaluations"
          />
          <button @click="searchEvaluations" :disabled="!searchSessionId.trim()">查询</button>
        </div>

        <div class="table-wrap" v-if="evaluations.length">
          <table>
            <thead>
              <tr>
                <th>消息 ID</th>
                <th>数据源</th>
                <th>评分</th>
                <th>评论</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ev in evaluations" :key="ev.eval_id">
                <td class="mono">{{ ev.msg_id.substring(0, 8) }}...</td>
                <td>{{ ev.source_id || '-' }}</td>
                <td class="stars">{{ renderStars(ev.score) }}</td>
                <td class="comment">{{ ev.comment || '-' }}</td>
                <td class="muted">{{ formatDate(ev.created_at) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <p>{{ searched ? '该会话暂无评估记录' : '输入会话 ID 查询评估记录' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { dashboardApi } from '@/api'

const stats = ref<any>(null)
const searchSessionId = ref('')
const evaluations = ref<any[]>([])
const searched = ref(false)

const dsNameMap: Record<string, string> = {
  'faq': 'FAQ 库',
  'knowledge': '知识库',
  'park-enterprise': '企业档案',
  'park-business': '招商管理',
  'park-space': '空间房源',
  'park-contract': '合同',
}

const loadStats = async () => {
  try {
    const res = await dashboardApi.stats()
    stats.value = res.data
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

const searchEvaluations = async () => {
  const sid = searchSessionId.value.trim()
  if (!sid) return
  try {
    const res = await dashboardApi.listEvaluations(sid)
    evaluations.value = res.data || []
    searched.value = true
  } catch (e) {
    console.error('查询评估失败', e)
    evaluations.value = []
    searched.value = true
  }
}

const renderStars = (score: number) => {
  return '★'.repeat(score) + '☆'.repeat(5 - score)
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(loadStats)
</script>

<style scoped>
.evaluation-page {
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
.search-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.search-row input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-row input:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
.search-row button {
  padding: 10px 20px;
  background: #007AFF;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s;
}
.search-row button:hover { background: #0056b3; }
.search-row button:disabled { opacity: 0.5; cursor: not-allowed; }
.table-wrap { overflow-x: auto; }
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
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, monospace;
  font-size: 13px;
}
.stars {
  color: #ff9500;
  font-size: 15px;
  letter-spacing: 1px;
}
.comment {
  max-width: 240px;
  color: #6e6e73;
}
.muted { color: #86868b; }
.empty-state {
  text-align: center;
  color: #86868b;
  padding: 48px;
  font-size: 14px;
}
</style>