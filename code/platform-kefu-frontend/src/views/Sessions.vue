<template>
  <div class="sessions-page">
    <header>
      <h1>会话管理</h1>
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
      </div>
    </header>

    <main class="content">
      <!-- 状态筛选 -->
      <div class="filter-bar">
        <div class="segmented">
          <button
            v-for="f in statusFilters"
            :key="f.value || 'all'"
            :class="['seg-btn', { on: activeFilter === f.value }]"
            @click="changeFilter(f.value)"
          >{{ f.label }}</button>
        </div>
        <span class="count-hint" v-if="sessions.length">共 {{ sessions.length }} 条会话</span>
      </div>

      <!-- 会话表格 -->
      <div class="card">
        <div v-if="loading" class="skeleton">
          <div v-for="n in 6" :key="n" class="sk-row"></div>
        </div>

        <div v-else-if="!sessions.length" class="empty">
          <svg viewBox="0 0 24 24" width="46" height="46" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="5" width="18" height="12" rx="2.5"></rect>
            <path d="M3 10h18M8 14h4"></path>
          </svg>
          <p>暂无会话记录</p>
          <span>当有客户开始对话后，会话将在这里展示</span>
        </div>

        <table v-else class="session-table">
          <thead>
            <tr>
              <th class="col-expand"></th>
              <th>客户名称</th>
              <th>联系方式</th>
              <th>渠道</th>
              <th>状态</th>
              <th>开始时间</th>
              <th>最后消息</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="s in sessions" :key="s.id">
              <tr
                class="session-row"
                :class="{ expanded: expandedSid === s.id, closed: s.status === 'CLOSED' }"
                @click="toggleExpand(s)"
              >
                <td class="col-expand">
                  <svg
                    class="chevron"
                    :class="{ open: expandedSid === s.id }"
                    viewBox="0 0 24 24" width="14" height="14" fill="none"
                    stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
                  >
                    <polyline points="9 18 15 12 9 6"></polyline>
                  </svg>
                </td>
                <td class="cell-name">
                  <span class="name">{{ s.customer_name || '游客' }}</span>
                  <span v-if="s.agent_name" class="agent-tag">@{{ s.agent_name }}</span>
                </td>
                <td class="cell-contact">{{ s.contact || '—' }}</td>
                <td><span class="channel">{{ channelLabel(s.channel) }}</span></td>
                <td><span :class="['status-badge', statusClass(s.status)]">{{ statusLabel(s.status) }}</span></td>
                <td class="cell-time">{{ formatTime(s.start_time) }}</td>
                <td class="cell-last">
                  <span v-if="s.last_message_preview" class="last-text">{{ s.last_message_preview }}</span>
                  <span v-else class="last-text muted">暂无消息</span>
                  <span v-if="s.last_message_at" class="last-time">{{ formatTime(s.last_message_at) }}</span>
                </td>
                <td class="col-actions" @click.stop>
                  <button
                    v-if="s.status === 'AI'"
                    class="mini-btn"
                    :disabled="isBusy(s.id, 'transfer')"
                    @click="transferSession(s)"
                  >{{ isBusy(s.id, 'transfer') ? '处理中' : '转人工' }}</button>
                  <button
                    v-if="s.status !== 'CLOSED'"
                    class="mini-btn danger"
                    :disabled="isBusy(s.id, 'close')"
                    @click="closeSession(s)"
                  >{{ isBusy(s.id, 'close') ? '处理中' : '结束' }}</button>
                  <button
                    class="mini-btn"
                    :class="{ rated: s.satisfaction != null }"
                    @click="openRating(s)"
                  >{{ s.satisfaction != null ? `已评 ${satisfactionText(s.satisfaction)}` : '评价' }}</button>
                </td>
              </tr>

              <!-- 展开：消息历史 -->
              <tr v-if="expandedSid === s.id" class="detail-row">
                <td colspan="8">
                  <div class="detail-panel">
                    <div class="detail-head">
                      <h3>消息记录</h3>
                      <span class="msg-count">{{ messagesBySid[s.id]?.length ?? 0 }} 条</span>
                    </div>
                    <div class="transcript">
                      <div
                        v-if="loadingMessages === s.id"
                        class="typing-bubble"
                      ><span></span><span></span><span></span></div>
                      <div
                        v-else-if="messageError && !messagesBySid[s.id]"
                        class="msg-error"
                      >{{ messageError }}</div>
                      <template v-else-if="messagesBySid[s.id]?.length">
                        <div
                          v-for="m in messagesBySid[s.id]"
                          :key="m.msg_id"
                          :class="['t-msg', m.role === 'notice' ? 'notice' : m.role]"
                        >
                          <div v-if="m.role === 'notice'" class="t-notice-pill">{{ m.content }}</div>
                          <template v-else>
                            <div class="t-meta">
                              <span class="t-role">{{ roleLabel(m.role) }}</span>
                              <span class="t-time">{{ formatTime(m.create_time) }}</span>
                              <span v-if="m.data_source_refs?.length" class="t-refs">{{ m.data_source_refs.length }} 个数据源</span>
                            </div>
                            <div class="t-bubble">{{ m.content }}</div>
                          </template>
                        </div>
                      </template>
                      <div v-else class="t-empty">该会话暂无消息</div>
                    </div>
                    <div v-if="s.status === 'CLOSED'" class="closed-footer">
                      <span>会话已结束</span>
                      <span v-if="s.end_time">于 {{ formatTime(s.end_time) }}</span>
                      <span v-if="s.satisfaction != null" class="sat">评分 {{ satisfactionText(s.satisfaction) }}{{ s.satisfaction_comment ? ` · “${s.satisfaction_comment}”` : '' }}</span>
                    </div>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>

        <div v-if="!loading && hasMore" class="load-more">
          <button @click="loadMore" :disabled="loadingMore">
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>
    </main>

    <!-- 满意度评分弹窗 -->
    <div v-if="showRating" class="rating-overlay" @click.self="closeRating">
      <div class="rating-card">
        <h3>会话评价</h3>
        <p class="rating-desc">请为 "{{ (rateTarget?.customer_name || '游客') }}" 的本次服务评分：</p>
        <div class="stars">
          <button
            v-for="n in 5"
            :key="n"
            :class="['star', { on: n <= ratingScore }]"
            :disabled="ratingSubmitting"
            @click="ratingScore = n"
            @mouseenter="hoverScore = n"
            @mouseleave="hoverScore = 0"
          >★</button>
        </div>
        <div class="star-labels">
          <span>非常不满意</span>
          <span v-if="hoverScore" class="live">当前 {{ hoverScore }} 星</span>
          <span>非常满意</span>
        </div>
        <textarea
          v-model="ratingComment"
          placeholder="补充意见（可选）"
          rows="3"
          :disabled="ratingSubmitting"
        ></textarea>
        <div class="rating-actions">
          <button class="action-btn" @click="closeRating" :disabled="ratingSubmitting">取消</button>
          <button class="action-btn primary" @click="submitRating" :disabled="ratingScore === 0 || ratingSubmitting">
            {{ ratingSubmitting ? '提交中...' : '提交评分' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 轻量提示 -->
    <transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { sessionApi } from '@/api'

type SessionStatus = 'AI' | 'HUMAN' | 'CLOSED'

interface SessionItem {
  id: string
  customer_name?: string | null
  contact?: string | null
  channel?: string
  status: string
  agent_name?: string | null
  start_time?: string | null
  end_time?: string | null
  satisfaction?: number | null
  satisfaction_comment?: string | null
  last_message_preview?: string | null
  last_message_at?: string | null
}

interface SessionMessage {
  msg_id: string
  session_id: string
  role: string
  content: string
  data_source_refs?: { [key: string]: any }[]
  create_time?: string | null
}

const PAGE_SIZE = 50

const statusFilters: { label: string; value: SessionStatus | undefined }[] = [
  { label: '全部', value: undefined },
  { label: 'AI中', value: 'AI' },
  { label: '人工接待', value: 'HUMAN' },
  { label: '已关闭', value: 'CLOSED' },
]

const sessions = ref<SessionItem[]>([])
const activeFilter = ref<SessionStatus | undefined>(undefined)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)

const expandedSid = ref<string | null>(null)
const messagesBySid = reactive<Record<string, SessionMessage[]>>({})
const loadingMessages = ref<string | null>(null)
const messageError = ref('')

const actionBusy = reactive<Record<string, boolean>>({})

const showRating = ref(false)
const rateTarget = ref<SessionItem | null>(null)
const ratingScore = ref(0)
const hoverScore = ref(0)
const ratingComment = ref('')
const ratingSubmitting = ref(false)

const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const showToast = (text: string) => {
  toast.value = text
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

const statusLabel = (st: string) =>
  ({ AI: 'AI 服务中', HUMAN: '人工接待', CLOSED: '已关闭' } as Record<string, string>)[st] || st || '—'
const statusClass = (st: string) =>
  ({ AI: 'status-ai', HUMAN: 'status-human', CLOSED: 'status-closed' } as Record<string, string>)[st] || 'status-closed'

const channelLabel = (c?: string) =>
  ({ web: '网页', wechat: '微信', app: 'APP', api: 'API', phone: '电话' } as Record<string, string>)[c || ''] || c || '—'

const roleLabel = (r: string) =>
  ({ user: '客户', assistant: '客服' } as Record<string, string>)[r] || r

const satisfactionText = (n?: number | null) => (n == null ? '—' : '★'.repeat(n))

const formatTime = (dt?: string | null): string => {
  if (!dt) return '—'
  const s = dt.includes('T') ? dt : dt.replace(' ', 'T')
  const d = new Date(s)
  if (isNaN(d.getTime())) return dt
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const isBusy = (sid: string, kind: string) => !!actionBusy[`${sid}:${kind}`]
const setBusy = (sid: string, kind: string, v: boolean) => {
  actionBusy[`${sid}:${kind}`] = v
}

/* ============ 列表加载 ============ */

const fetchSessions = async (append = false) => {
  if (append) loadingMore.value = true
  else loading.value = true
  try {
    const resp = await sessionApi.list({
      status: activeFilter.value || undefined,
      limit: PAGE_SIZE,
      offset: append ? sessions.value.length : 0,
    })
    const data = (resp.data || {}) as { total?: number; items?: SessionItem[] }
    const items = data.items || []
    sessions.value = append ? [...sessions.value, ...items] : items
    hasMore.value = items.length >= PAGE_SIZE
  } catch (e: any) {
    showToast('加载会话失败：' + (e.message || '未知错误'))
    if (!append && !sessions.value.length) sessions.value = []
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const changeFilter = (value: SessionStatus | undefined) => {
  if (activeFilter.value === value) return
  activeFilter.value = value
  expandedSid.value = null
  fetchSessions()
}

const loadMore = () => fetchSessions(true)

/* ============ 消息历史 ============ */

const toggleExpand = async (s: SessionItem) => {
  if (expandedSid.value === s.id) {
    expandedSid.value = null
    return
  }
  expandedSid.value = s.id
  if (!messagesBySid[s.id]) {
    loadingMessages.value = s.id
    messageError.value = ''
    try {
      const resp = await sessionApi.getMessages(s.id)
      messagesBySid[s.id] = (resp.data || []) as SessionMessage[]
    } catch (e: any) {
      messageError.value = '加载消息失败：' + (e.message || '未知错误')
    } finally {
      loadingMessages.value = null
    }
  }
}

/* ============ 会话操作 ============ */

const transferSession = async (s: SessionItem) => {
  if (s.status !== 'AI' || isBusy(s.id, 'transfer')) return
  setBusy(s.id, 'transfer', true)
  try {
    await sessionApi.transfer(s.id)
    s.status = 'HUMAN'
    appendNotice(s, '已转接人工客服')
    showToast('已转接人工客服')
  } catch (e: any) {
    showToast('转人工失败：' + (e.message || '未知错误'))
  } finally {
    setBusy(s.id, 'transfer', false)
  }
}

const closeSession = async (s: SessionItem) => {
  if (s.status === 'CLOSED' || isBusy(s.id, 'close')) return
  setBusy(s.id, 'close', true)
  try {
    await sessionApi.close(s.id)
    s.status = 'CLOSED'
    appendNotice(s, '会话已结束')
    showToast('会话已结束')
  } catch (e: any) {
    showToast('结束会话失败：' + (e.message || '未知错误'))
  } finally {
    setBusy(s.id, 'close', false)
  }
}

const appendNotice = (s: SessionItem, content: string) => {
  const msgs = messagesBySid[s.id]
  if (!msgs) return
  msgs.push({
    msg_id: `n-${Date.now()}`,
    session_id: s.id,
    role: 'notice',
    content,
    create_time: formatTime(new Date().toISOString()),
  })
}

/* ============ 评分 ============ */

const openRating = (s: SessionItem) => {
  rateTarget.value = s
  ratingScore.value = s.satisfaction || 0
  ratingComment.value = s.satisfaction_comment || ''
  showRating.value = true
}

const closeRating = () => {
  if (ratingSubmitting.value) return
  showRating.value = false
  rateTarget.value = null
}

const submitRating = async () => {
  const s = rateTarget.value
  if (!s || ratingScore.value === 0 || ratingSubmitting.value) return
  ratingSubmitting.value = true
  try {
    await sessionApi.rate(s.id, ratingScore.value, ratingComment.value)
    s.satisfaction = ratingScore.value
    s.satisfaction_comment = ratingComment.value
    showToast('评价已提交')
    showRating.value = false
    rateTarget.value = null
  } catch (e: any) {
    showToast('评价提交失败：' + (e.message || '未知错误'))
  } finally {
    ratingSubmitting.value = false
  }
}

const escClose = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && showRating.value) closeRating()
}

onMounted(() => {
  fetchSessions()
  window.addEventListener('keydown', escClose)
})

onUnmounted(() => {
  window.removeEventListener('keydown', escClose)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
.sessions-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f2f2f7;
}
header {
  flex-shrink: 0;
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
  gap: 10px;
}
nav {
  display: flex;
  gap: 2px;
  background: #f2f2f7;
  padding: 3px;
  border-radius: 10px;
}
nav a {
  color: #6e6e73;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  padding: 5px 11px;
  border-radius: 7px;
  white-space: nowrap;
  transition: background 0.2s, color 0.2s;
}
nav a:hover {
  color: #1d1d1f;
}
nav a.router-link-active {
  background: #ffffff;
  color: #007AFF;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.content {
  flex: 1;
  overflow-y: auto;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
}
/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.segmented {
  display: flex;
  gap: 2px;
  background: #e5e5ea;
  padding: 3px;
  border-radius: 10px;
}
.seg-btn {
  border: none;
  background: transparent;
  color: #6e6e73;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
}
.seg-btn:hover {
  color: #1d1d1f;
}
.seg-btn.on {
  background: #ffffff;
  color: #007AFF;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.14);
}
.count-hint {
  font-size: 13px;
  color: #86868b;
}
/* 卡片与表格 */
.card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}
.session-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.session-table thead th {
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #86868b;
  letter-spacing: 0.3px;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #e5e5ea;
  white-space: nowrap;
}
.session-table tbody td {
  padding: 13px 16px;
  border-bottom: 1px solid #f0f0f2;
  vertical-align: middle;
}
.session-table tbody tr:last-child td {
  border-bottom: none;
}
.session-row {
  cursor: pointer;
  transition: background 0.15s;
}
.session-row:hover {
  background: #f7f9ff;
}
.session-row.expanded {
  background: #f0f6ff;
}
.session-row.expanded:hover {
  background: #eaf1ff;
}
.session-row.closed .cell-name .name {
  color: #86868b;
}
.col-expand {
  width: 30px;
  padding-right: 0 !important;
}
.chevron {
  color: #c7c7cc;
  transition: transform 0.2s, color 0.2s;
}
.chevron.open {
  transform: rotate(90deg);
  color: #007AFF;
}
.cell-name .name {
  font-weight: 500;
  color: #1d1d1f;
}
.agent-tag {
  margin-left: 6px;
  font-size: 11px;
  font-weight: 500;
  color: #6e6e73;
  background: #f2f2f7;
  border-radius: 9999px;
  padding: 2px 8px;
}
.cell-contact {
  color: #48484a;
  font-variant-numeric: tabular-nums;
}
.channel {
  display: inline-block;
  font-size: 12px;
  font-weight: 500;
  color: #48484a;
  background: #f2f2f7;
  border-radius: 6px;
  padding: 3px 8px;
}
.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 9999px;
  white-space: nowrap;
  display: inline-block;
}
.status-ai {
  background: #e5f1ff;
  color: #007AFF;
}
.status-human {
  background: #fff4e0;
  color: #e88b00;
}
.status-closed {
  background: #e5e5ea;
  color: #6e6e73;
}
.cell-time {
  color: #48484a;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.cell-last {
  max-width: 240px;
}
.last-text {
  display: block;
  font-size: 13px;
  color: #1d1d1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.last-text.muted {
  color: #c7c7cc;
}
.last-time {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #86868b;
  font-variant-numeric: tabular-nums;
}
.col-actions {
  white-space: nowrap;
}
.mini-btn {
  margin-left: 6px;
  padding: 5px 12px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #1d1d1f;
  transition: background 0.2s, border-color 0.2s, color 0.2s, opacity 0.2s;
}
.mini-btn:hover:not(:disabled) {
  background: #e5e5ea;
}
.mini-btn.danger:hover:not(:disabled) {
  background: #ffe1e1;
  border-color: #ff7a7a;
  color: #d70015;
}
.mini-btn.rated {
  background: #e5f1ff;
  border-color: #b3d8ff;
  color: #007AFF;
}
.mini-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
/* 展开详情 */
.detail-row td {
  background: #fafbff;
  padding: 0 !important;
}
.detail-panel {
  border-top: 1px solid #e5e5ea;
  padding: 18px 20px 20px 46px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.detail-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
}
.msg-count {
  font-size: 12px;
  color: #86868b;
}
.transcript {
  max-height: 320px;
  overflow-y: auto;
  padding: 8px 4px;
  border: 1px solid #f0f0f2;
  border-radius: 12px;
  background: #f7f7f9;
}
.t-msg {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
}
.t-msg.notice {
  align-items: center;
  margin-bottom: 8px;
}
.t-notice-pill {
  font-size: 12px;
  color: #6e6e73;
  background: #e5e5ea;
  padding: 4px 14px;
  border-radius: 9999px;
}
.t-msg.user {
  align-items: flex-end;
}
.t-msg.assistant {
  align-items: flex-start;
}
.t-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
  font-size: 11px;
  color: #86868b;
}
.t-role {
  font-weight: 600;
  color: #6e6e73;
}
.t-refs {
  background: #e5f1ff;
  color: #007AFF;
  border-radius: 9999px;
  padding: 1px 8px;
}
.t-bubble {
  max-width: 78%;
  padding: 9px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.t-msg.user .t-bubble {
  background: #007AFF;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}
.t-msg.assistant .t-bubble {
  background: #ffffff;
  color: #1d1d1f;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.t-empty {
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
  color: #86868b;
}
.msg-error {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
  color: #d70015;
}
.typing-bubble {
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 16px;
}
.typing-bubble span {
  width: 7px;
  height: 7px;
  background: #c7c7cc;
  border-radius: 50%;
  animation: typing 1.2s ease-in-out infinite;
}
.typing-bubble span:nth-child(2) { animation-delay: 0.2s; }
.typing-bubble span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-5px); opacity: 1; }
}
.closed-footer {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-top: 12px;
  font-size: 12px;
  color: #86868b;
  flex-wrap: wrap;
}
.closed-footer .sat {
  color: #e88b00;
  font-weight: 600;
}
/* 加载 / 空状态 / 加载更多 */
.skeleton {
  padding: 8px 0;
}
.sk-row {
  height: 52px;
  margin: 0 20px;
  border-bottom: 1px solid #f2f2f7;
  background: linear-gradient(100deg, #f5f5f7 40%, #fbfbfd 50%, #f5f5f7 60%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
}
.sk-row:last-child { border-bottom: none; }
@keyframes shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.empty {
  padding: 72px 24px;
  text-align: center;
  color: #c7c7cc;
}
.empty p {
  margin: 14px 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #6e6e73;
}
.empty span {
  font-size: 13px;
  color: #a1a1a6;
}
.load-more {
  padding: 16px;
  text-align: center;
  border-top: 1px solid #f0f0f2;
}
.load-more button {
  padding: 7px 26px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  cursor: pointer;
  transition: background 0.2s;
}
.load-more button:hover:not(:disabled) {
  background: #e5e5ea;
}
.load-more button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* 评分弹窗 */
.rating-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  backdrop-filter: blur(4px);
}
.rating-card {
  width: min(360px, calc(100vw - 48px));
  background: #ffffff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}
.rating-card h3 {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
}
.rating-desc {
  margin: 0 0 14px;
  font-size: 13px;
  color: #6e6e73;
  word-break: break-all;
}
.stars {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: 6px;
}
.star {
  font-size: 30px;
  line-height: 1;
  background: none;
  border: none;
  cursor: pointer;
  color: #d1d1d6;
  transition: color 0.15s, transform 0.15s;
}
.star.on {
  color: #ffb400;
}
.star:hover:not(:disabled) {
  transform: scale(1.15);
}
.star-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #c7c7cc;
  margin-bottom: 14px;
}
.star-labels .live {
  color: #e88b00;
  font-weight: 600;
}
.rating-card textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #d1d1d6;
  border-radius: 12px;
  font-size: 14px;
  font-family: inherit;
  color: #1d1d1f;
  resize: none;
  outline: none;
  margin-bottom: 16px;
}
.rating-card textarea:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
.rating-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.action-btn {
  padding: 6px 14px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  white-space: nowrap;
  transition: background 0.2s, opacity 0.2s;
}
.action-btn:hover:not(:disabled) {
  background: #e5e5ea;
}
.action-btn.primary {
  background: #007AFF;
  border-color: #007AFF;
  color: #ffffff;
}
.action-btn.primary:hover:not(:disabled) {
  background: #0056b3;
}
.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
/* Toast */
.toast {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(29, 29, 31, 0.9);
  color: #ffffff;
  font-size: 13px;
  padding: 10px 18px;
  border-radius: 9999px;
  z-index: 200;
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}
</style>