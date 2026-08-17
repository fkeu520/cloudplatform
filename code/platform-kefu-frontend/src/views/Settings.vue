<template>
  <div class="settings-page">
    <header>
      <h1>数据源管理</h1>
      <div class="user">
        <nav>
          <router-link to="/">智能问答</router-link>
          <router-link to="/knowledge">知识库</router-link>
          <router-link to="/dashboard">数据看板</router-link>
          <router-link to="/sessions">会话管理</router-link>
          <router-link to="/faqs">FAQ管理</router-link>
          <router-link to="/import">知识导入</router-link>
          <router-link to="/settings">数据源</router-link>
          <router-link to="/evaluation">评估</router-link>
        </nav>
        <span>数据源管理</span>
      </div>
    </header>

    <div class="content">
      <!-- 操作栏 -->
      <div class="toolbar">
        <h2>数据源 ({{ dataSources.length }})</h2>
        <button class="btn-primary" @click="openRegisterModal">注册新数据源</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner" />
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!dataSources.length" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="#E5F1FF"/>
          <path d="M16 20h16M16 26h12M16 32h8" stroke="#007AFF" stroke-width="2" stroke-linecap="round"/>
          <path d="M24 14l-6 6h12l-6-6z" stroke="#007AFF" stroke-width="2" stroke-linejoin="round"/>
        </svg>
        <p>暂无数据源</p>
        <button class="btn-primary" @click="openRegisterModal">注册新数据源</button>
      </div>

      <!-- 数据源卡片列表 -->
      <div v-else class="source-list">
        <div
          v-for="ds in dataSources"
          :key="ds.id"
          class="source-card"
          :class="{ disabled: !ds.enabled }"
        >
          <div class="card-top">
            <div class="card-info">
              <div class="card-name">{{ ds.name }}</div>
              <div class="card-meta">
                <span class="type-badge" :class="ds.type">{{ typeLabel(ds.type) }}</span>
                <span class="meta-item" v-if="ds.module_ref">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path d="M3 3h8l1 1v7a1 1 0 01-1 1H3a1 1 0 01-1-1V4l1-1z" stroke="#86868b" stroke-width="1.2"/>
                    <path d="M4 6h6M4 9h4" stroke="#86868b" stroke-width="1.2" stroke-linecap="round"/>
                  </svg>
                  {{ ds.module_ref }}
                </span>
                <span class="meta-item" v-if="ds.last_sync_at">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <circle cx="7" cy="7" r="5" stroke="#86868b" stroke-width="1.2"/>
                    <path d="M7 4v3l2 1" stroke="#86868b" stroke-width="1.2" stroke-linecap="round"/>
                  </svg>
                  {{ formatDate(ds.last_sync_at) }}
                </span>
              </div>
            </div>
            <div class="card-actions-top">
              <label class="toggle-switch" :class="{ active: ds.enabled }">
                <input type="checkbox" :checked="ds.enabled" @change="toggleEnabled(ds)" />
                <span class="toggle-track">
                  <span class="toggle-thumb" />
                </span>
              </label>
            </div>
          </div>

          <!-- 关键词标签 -->
          <div class="card-section">
            <div class="section-label">关键词</div>
            <div class="keywords-row" @click="openKeywordsEdit(ds)" title="点击编辑关键词">
              <span v-if="!ds.intent_keywords || !ds.intent_keywords.length" class="no-keywords">暂无关键词</span>
              <span
                v-for="kw in ds.intent_keywords"
                :key="kw"
                class="keyword-tag"
              >{{ kw }}</span>
              <svg class="edit-icon" width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M10 1.5l2.5 2.5L5 11.5 2 12l.5-3 7.5-7.5z" stroke="#86868b" stroke-width="1.2" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>

          <!-- 同步策略 -->
          <div class="card-section">
            <div class="section-label">同步策略</div>
            <div class="strategy-row">
              <span class="strategy-badge" :class="ds.sync_strategy">
                {{ strategyLabel(ds.sync_strategy) }}
              </span>
              <span class="sync-status" v-if="ds.last_sync_status">
                <span :class="['status-dot', ds.last_sync_status]" />
                {{ syncStatusText(ds.last_sync_status) }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="card-actions">
            <button
              class="btn-action sync"
              :disabled="syncing[ds.id]"
              @click="syncSource(ds)"
            >
              <svg v-if="!syncing[ds.id]" width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M11 7a4 4 0 01-4 4 4 4 0 01-4-4 4 4 0 014-4c1.5 0 2.8.8 3.5 2" stroke="#007AFF" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M11 3v3H8" stroke="#007AFF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span v-if="syncing[ds.id]" class="spinner-btn" />
              {{ syncing[ds.id] ? '同步中...' : '同步' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 注册新数据源弹窗 -->
    <div v-if="showRegisterModal" class="modal-overlay" @click="showRegisterModal = false">
      <div class="modal-card" @click.stop>
        <div class="modal-header">
          <h3>注册新数据源</h3>
          <button class="modal-close" @click="showRegisterModal = false">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M5 5l10 10M15 5L5 15" stroke="#86868b" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="form-body">
          <div class="form-group">
            <label>ID</label>
            <input v-model="registerForm.id" placeholder="唯一标识符" />
          </div>
          <div class="form-group">
            <label>名称</label>
            <input v-model="registerForm.name" placeholder="数据源名称" />
          </div>
          <div class="form-group">
            <label>类型</label>
            <select v-model="registerForm.type">
              <option value="internal">内部 (internal)</option>
              <option value="http_api">HTTP API (http_api)</option>
              <option value="vector_search">向量搜索 (vector_search)</option>
              <option value="database_query">数据库查询 (database_query)</option>
            </select>
          </div>
          <div class="form-group">
            <label>模块引用</label>
            <input v-model="registerForm.module_ref" placeholder="如: ticket, order" />
          </div>
          <div class="form-group">
            <label>关键词 (逗号分隔)</label>
            <input v-model="registerForm.keywordsInput" placeholder="如: 工单, 报修, 投诉" />
          </div>
          <div class="form-group">
            <label>同步策略</label>
            <select v-model="registerForm.sync_strategy">
              <option value="realtime">实时同步 (realtime)</option>
              <option value="scheduled">定时同步 (scheduled)</option>
            </select>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="showRegisterModal = false">取消</button>
          <button class="btn-primary" @click="registerSource" :disabled="registering">
            {{ registering ? '注册中...' : '确认注册' }}
          </button>
        </div>
        <p v-if="registerError" class="error-msg">{{ registerError }}</p>
      </div>
    </div>

    <!-- 编辑关键词弹窗 -->
    <div v-if="showKeywordsModal" class="modal-overlay" @click="showKeywordsModal = false">
      <div class="modal-card modal-sm" @click.stop>
        <div class="modal-header">
          <h3>编辑关键词</h3>
          <button class="modal-close" @click="showKeywordsModal = false">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M5 5l10 10M15 5L5 15" stroke="#86868b" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="form-body" v-if="keywordsEditSource">
          <p class="hint">每行一个关键词，用于匹配用户意图</p>
          <textarea
            v-model="editingKeywords"
            rows="6"
            placeholder="输入关键词，每行一个"
          />
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showKeywordsModal = false">取消</button>
          <button class="btn-primary" @click="saveKeywords" :disabled="savingKeywords">
            {{ savingKeywords ? '保存中...' : '保存' }}
          </button>
        </div>
        <p v-if="keywordsError" class="error-msg">{{ keywordsError }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { dataSourceApi } from '@/api'

interface DataSourceItem {
  id: string
  name: string
  type: string
  module_ref?: string
  enabled: boolean
  sync_strategy: string
  sync_interval?: number
  intent_keywords: string[]
  last_sync_at?: string
  last_sync_status?: string
}

const dataSources = ref<DataSourceItem[]>([])
const loading = ref(true)
const syncing = ref<Record<string, boolean>>({})

// 注册弹窗
const showRegisterModal = ref(false)
const registering = ref(false)
const registerError = ref('')
const registerForm = reactive({
  id: '',
  name: '',
  type: 'internal',
  module_ref: '',
  keywordsInput: '',
  sync_strategy: 'realtime',
})

// 关键词编辑弹窗
const showKeywordsModal = ref(false)
const keywordsEditSource = ref<DataSourceItem | null>(null)
const editingKeywords = ref('')
const savingKeywords = ref(false)
const keywordsError = ref('')

const typeLabel = (type: string): string => {
  const map: Record<string, string> = {
    internal: '内部',
    http_api: 'HTTP API',
    vector_search: '向量搜索',
    database_query: '数据库查询',
  }
  return map[type] || type
}

const strategyLabel = (strategy: string): string => {
  const map: Record<string, string> = {
    realtime: '实时同步',
    scheduled: '定时同步',
  }
  return map[strategy] || strategy
}

const syncStatusText = (status: string): string => {
  const map: Record<string, string> = {
    success: '同步成功',
    failed: '同步失败',
    running: '同步中',
    pending: '等待同步',
  }
  return map[status] || status
}

const formatDate = (date: string): string => {
  return new Date(date).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await dataSourceApi.list()
    dataSources.value = res.data.items || []
  } catch (e) {
    console.error('加载数据源失败', e)
  } finally {
    loading.value = false
  }
}

const toggleEnabled = async (ds: DataSourceItem) => {
  const newVal = !ds.enabled
  try {
    await dataSourceApi.update(ds.id, { enabled: newVal })
    ds.enabled = newVal
  } catch (e: any) {
    alert(e.response?.data?.detail || '切换状态失败')
  }
}

const syncSource = async (ds: DataSourceItem) => {
  syncing.value[ds.id] = true
  try {
    const res = await dataSourceApi.sync(ds.id)
    ds.last_sync_at = res.data.last_sync_at
    ds.last_sync_status = 'success'
  } catch (e: any) {
    ds.last_sync_status = 'failed'
    alert(e.response?.data?.detail || '同步失败')
  } finally {
    syncing.value[ds.id] = false
  }
}

const openRegisterModal = () => {
  registerForm.id = ''
  registerForm.name = ''
  registerForm.type = 'internal'
  registerForm.module_ref = ''
  registerForm.keywordsInput = ''
  registerForm.sync_strategy = 'realtime'
  registerError.value = ''
  showRegisterModal.value = true
}

const registerSource = async () => {
  if (!registerForm.id.trim()) {
    registerError.value = '请输入数据源ID'
    return
  }
  if (!registerForm.name.trim()) {
    registerError.value = '请输入数据源名称'
    return
  }
  registering.value = true
  registerError.value = ''
  try {
    const keywords = registerForm.keywordsInput
      .split(/[,，]/)
      .map(k => k.trim())
      .filter(k => k.length > 0)
    await dataSourceApi.register({
      id: registerForm.id.trim(),
      name: registerForm.name.trim(),
      type: registerForm.type,
      module_ref: registerForm.module_ref.trim() || undefined,
      sync_strategy: registerForm.sync_strategy,
      intent_keywords: keywords.length ? keywords : undefined,
    })
    showRegisterModal.value = false
    await loadData()
  } catch (e: any) {
    registerError.value = e.response?.data?.detail || '注册失败'
  } finally {
    registering.value = false
  }
}

const openKeywordsEdit = (ds: DataSourceItem) => {
  keywordsEditSource.value = ds
  editingKeywords.value = (ds.intent_keywords || []).join('\n')
  keywordsError.value = ''
  showKeywordsModal.value = true
}

const saveKeywords = async () => {
  if (!keywordsEditSource.value) return
  savingKeywords.value = true
  keywordsError.value = ''
  try {
    const keywords = editingKeywords.value
      .split('\n')
      .map(k => k.trim())
      .filter(k => k.length > 0)
    await dataSourceApi.update(keywordsEditSource.value.id, { intent_keywords: keywords })
    keywordsEditSource.value.intent_keywords = keywords
    showKeywordsModal.value = false
  } catch (e: any) {
    keywordsError.value = e.response?.data?.detail || '保存失败'
  } finally {
    savingKeywords.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.settings-page {
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
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.toolbar h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0;
}
.btn-primary {
  padding: 8px 20px;
  background: #007AFF;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
}
.btn-primary:hover { background: #0056b3; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-cancel {
  padding: 8px 20px;
  background: #f2f2f7;
  color: #1d1d1f;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-cancel:hover { background: #e5e5ea; }

/* 加载 */
.loading-state {
  text-align: center;
  padding: 80px 0;
  color: #86868b;
  font-size: 14px;
}
.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e5e5ea;
  border-top-color: #007AFF;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  margin: 0 auto 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #86868b;
  font-size: 14px;
}
.empty-state svg {
  margin-bottom: 12px;
}
.empty-state p {
  margin: 0 0 16px;
}

/* 数据源卡片 */
.source-list {
  display: grid;
  gap: 14px;
}
.source-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
  transition: opacity 0.2s;
}
.source-card.disabled {
  opacity: 0.6;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}
.card-info {
  flex: 1;
  min-width: 0;
}
.card-name {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 6px;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  background: #e5f1ff;
  color: #007AFF;
}
.type-badge.http_api { background: #fff3e0; color: #cc7a00; }
.type-badge.vector_search { background: #e8f5e9; color: #248a3d; }
.type-badge.database_query { background: #f3e5f5; color: #7b1fa2; }
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #86868b;
}
.card-actions-top {
  flex-shrink: 0;
  margin-left: 16px;
}

/* Toggle 开关 */
.toggle-switch {
  position: relative;
  display: inline-block;
  cursor: pointer;
}
.toggle-switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.toggle-track {
  display: block;
  width: 44px;
  height: 24px;
  background: #e5e5ea;
  border-radius: 12px;
  transition: background 0.25s;
  position: relative;
}
.toggle-switch.active .toggle-track {
  background: #007AFF;
}
.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
  transition: left 0.25s;
}
.toggle-switch.active .toggle-thumb {
  left: 22px;
}

/* 卡片区块 */
.card-section {
  margin-bottom: 10px;
}
.section-label {
  font-size: 11px;
  font-weight: 600;
  color: #86868b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

/* 关键词 */
.keywords-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  cursor: pointer;
  padding: 4px 0;
  border-radius: 8px;
  transition: background 0.15s;
}
.keywords-row:hover {
  background: #f5f5f7;
  margin: 0 -6px;
  padding: 4px 6px;
}
.no-keywords {
  font-size: 13px;
  color: #c7c7cc;
  font-style: italic;
}
.keyword-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #f2f2f7;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  color: #1d1d1f;
}
.edit-icon {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}
.keywords-row:hover .edit-icon {
  opacity: 1;
}

/* 同步策略 */
.strategy-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.strategy-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}
.strategy-badge.realtime { background: #e5f1ff; color: #007AFF; }
.strategy-badge.scheduled { background: #f2f2f7; color: #6e6e73; }
.sync-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #86868b;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.status-dot.success { background: #248a3d; }
.status-dot.failed { background: #ff3b30; }
.status-dot.running { background: #007AFF; animation: pulse 1s infinite; }
.status-dot.pending { background: #c7c7cc; }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 操作按钮 */
.card-actions {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f2f2f7;
}
.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #007AFF;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-action:hover:not(:disabled) { background: #e5f1ff; }
.btn-action:disabled { color: #c7c7cc; cursor: not-allowed; }
.spinner-btn {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #e5e5ea;
  border-top-color: #007AFF;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  backdrop-filter: blur(4px);
}
.modal-card {
  background: #ffffff;
  border-radius: 18px;
  max-width: 520px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.02);
}
.modal-sm {
  max-width: 440px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.modal-header h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}
.modal-close {
  background: #f2f2f7;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}
.modal-close:hover { background: #e5e5ea; }
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.error-msg {
  color: #ff3b30;
  font-size: 13px;
  margin-top: 10px;
}

/* 表单 */
.form-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: #6e6e73;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.form-group input,
.form-group select {
  padding: 10px 12px;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  font-size: 14px;
  color: #1d1d1f;
  background: #ffffff;
  transition: border-color 0.2s;
  outline: none;
}
.form-group input:focus,
.form-group select:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
.form-group input::placeholder {
  color: #c7c7cc;
}
.hint {
  font-size: 13px;
  color: #86868b;
  margin: 0 0 8px;
}
textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  font-size: 14px;
  color: #1d1d1f;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}
textarea:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
textarea::placeholder {
  color: #c7c7cc;
}
</style>