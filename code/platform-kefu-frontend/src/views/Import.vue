<template>
  <div class="import-page">
    <header>
      <h1>知识导入</h1>
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
        <span>知识导入</span>
      </div>
    </header>
    <div class="content">
      <!-- 上传区域 -->
      <div class="import-grid">
        <div class="upload-card">
          <h3>上传知识文件</h3>
          <div
            class="upload-area"
            :class="{ dragging: isDragging, uploading: uploading }"
            @click="triggerUpload"
            @drop.prevent="handleDrop"
            @dragover.prevent="isDragging = true"
            @dragleave="isDragging = false"
          >
            <input type="file" ref="fileInput" @change="handleFileChange" accept=".pdf,.docx,.doc,.md,.xlsx,.xls,.csv,.txt" style="display:none" />
            <div class="upload-icon">
              <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
                <rect x="4" y="4" width="28" height="28" rx="8" fill="#E5F1FF"/>
                <path d="M18 12v10M13 17l5-5 5 5" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M10 24v2a2 2 0 002 2h12a2 2 0 002-2v-2" stroke="#007AFF" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <p v-if="!uploading">点击或拖拽文件到此处<br><span>PDF · Word · Markdown · Excel · CSV · TXT</span></p>
            <p v-else class="uploading-text">上传处理中...</p>
          </div>
          <div class="upload-meta">
            <label class="meta-label">
              <span>导入到分类</span>
              <select v-model="importCategory">
                <option value="policy">政策</option>
                <option value="process">流程</option>
                <option value="faq">FAQ</option>
                <option value="business">业务实体</option>
              </select>
            </label>
            <label class="meta-label">
              <span>自动向量化</span>
              <label class="toggle-switch" :class="{ active: autoVectorize }">
                <input type="checkbox" v-model="autoVectorize" />
                <span class="toggle-track">
                  <span class="toggle-thumb" />
                </span>
              </label>
            </label>
          </div>
          <p v-if="uploadError" class="error-msg">{{ uploadError }}</p>
          <p v-if="uploadSuccess" class="success-msg">{{ uploadSuccess }}</p>
        </div>

        <div class="tips-card">
          <h3>导入提示</h3>
          <ul>
            <li>PDF / Word：自动分段（每段 ≤ 2000 字）</li>
            <li>Excel / CSV：第一列问题，第二列答案</li>
            <li>Markdown：按标题分段</li>
            <li>导入完成后自动向量化（通常 1-5 分钟）</li>
            <li>大文件（&gt;10MB）建议分批导入</li>
            <li>重复内容自动合并（按 hash）</li>
          </ul>
        </div>
      </div>

      <!-- 导入历史 -->
      <div class="history-card">
        <div class="history-header">
          <h3>导入历史</h3>
          <span class="history-count" v-if="history.length">共 {{ history.length }} 条</span>
        </div>
        <div class="table-wrap" v-if="history.length">
          <table>
            <thead>
              <tr>
                <th>文件名</th>
                <th>大小</th>
                <th>分类</th>
                <th>条目数</th>
                <th>状态</th>
                <th>导入时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in history" :key="item.id">
                <td class="file-name">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style="vertical-align:middle;margin-right:4px">
                    <path d="M3 2h6l4 4v8a1 1 0 01-1 1H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="#86868b" stroke-width="1.2"/>
                    <path d="M9 2v4h4" stroke="#86868b" stroke-width="1.2"/>
                  </svg>
                  {{ item.file_name }}
                </td>
                <td class="muted">{{ item.file_size }}</td>
                <td><span class="cat-badge" :class="item.category">{{ categoryLabel(item.category) }}</span></td>
                <td class="mono">{{ item.entry_count }}</td>
                <td><span :class="['status-badge', item.status]">{{ statusText(item.status) }}</span></td>
                <td class="muted">{{ formatDate(item.import_time) }}</td>
                <td class="actions">
                  <button class="btn-text" @click="viewDetail(item)" :disabled="item.status === 'importing'">查看</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="10" fill="#F2F2F7"/>
            <path d="M14 20h12M20 14v12" stroke="#C7C7CC" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <p>暂无导入记录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '@/api'

interface ImportRecord {
  id: string
  file_name: string
  file_size: string
  category: string
  entry_count: number
  status: string
  progress?: number
  import_time: string
}

const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const uploadError = ref('')
const uploadSuccess = ref('')
const isDragging = ref(false)
const importCategory = ref('policy')
const autoVectorize = ref(true)
const history = ref<ImportRecord[]>([])

const triggerUpload = () => fileInput.value?.click()

const handleFileChange = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) uploadFile(file)
}

const handleDrop = (e: DragEvent) => {
  isDragging.value = false
  const file = e.dataTransfer?.files[0]
  if (file) uploadFile(file)
}

const uploadFile = async (file: File) => {
  uploading.value = true
  uploadError.value = ''
  uploadSuccess.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', importCategory.value)
    formData.append('auto_vectorize', String(autoVectorize.value))
    // 调用知识库的上传 API（复用 knowledgeApi.uploadDoc）
    const { knowledgeApi } = await import('@/api')
    await knowledgeApi.uploadDoc(formData)
    uploadSuccess.value = `文件 "${file.name}" 上传成功，已加入向量化队列`
    await loadHistory()
  } catch (e: any) {
    uploadError.value = e.response?.data?.detail || '上传失败，请重试'
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const loadHistory = async () => {
  try {
    const res = await knowledgeApi.listDocs()
    // 用文档列表作为导入历史的数据源
    history.value = (res.data.items || []).map((doc: any) => ({
      id: doc.doc_id,
      file_name: doc.name,
      file_size: formatSize(doc.size_bytes),
      category: doc.category || 'policy',
      entry_count: doc.chunk_count || 0,
      status: doc.status === 'ready' ? 'completed' : doc.status === 'processing' ? 'importing' : 'failed',
      progress: doc.status === 'processing' ? 62 : 100,
      import_time: doc.upload_time,
    }))
  } catch (e) {
    console.error('加载导入历史失败', e)
  }
}

const formatSize = (bytes: number) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (date: string) => {
  if (!date) return '—'
  return new Date(date).toLocaleString('zh-CN')
}

const categoryLabel = (cat: string) => {
  const map: Record<string, string> = { policy: '政策', process: '流程', faq: 'FAQ', business: '业务实体' }
  return map[cat] || cat
}

const statusText = (status: string) => {
  const map: Record<string, string> = { completed: '已完成', importing: '导入中', failed: '失败', ready: '已完成', processing: '导入中' }
  return map[status] || status
}

const viewDetail = (item: ImportRecord) => {
  alert(`查看导入详情：${item.file_name}\n条目数：${item.entry_count}\n导入时间：${item.import_time}`)
}

onMounted(loadHistory)
</script>

<style scoped>
.import-page {
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
/* 导入网格 */
.import-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.upload-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.upload-card h3, .tips-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 16px;
}
.upload-area {
  border: 2px dashed #d1d1d6;
  border-radius: 14px;
  padding: 36px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.upload-area:hover, .upload-area.dragging {
  border-color: #007AFF;
  background: #e5f1ff;
}
.upload-area.uploading {
  border-color: #86868b;
  background: #f9f9fb;
}
.upload-icon {
  margin-bottom: 10px;
}
.upload-area p {
  font-size: 14px;
  color: #6e6e73;
  margin: 0;
  line-height: 1.5;
}
.upload-area p span {
  font-size: 12px;
  color: #86868b;
}
.uploading-text {
  color: #007AFF;
  font-weight: 500;
}
.upload-meta {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
.meta-label {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.meta-label span {
  font-size: 12px;
  color: #86868b;
  font-weight: 500;
}
.meta-label select {
  padding: 8px 10px;
  border: 1px solid #d1d1d6;
  border-radius: 8px;
  font-size: 14px;
  color: #1d1d1f;
  background: #ffffff;
  outline: none;
  cursor: pointer;
}
.meta-label select:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
.toggle-switch {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}
.toggle-switch input { display: none; }
.toggle-track {
  width: 44px;
  height: 24px;
  background: #c7c7cc;
  border-radius: 12px;
  position: relative;
  transition: background 0.2s;
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
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  transition: transform 0.2s;
}
.toggle-switch.active .toggle-thumb {
  transform: translateX(20px);
}
.error-msg {
  color: #ff3b30;
  font-size: 13px;
  margin-top: 10px;
}
.success-msg {
  color: #248a3d;
  font-size: 13px;
  margin-top: 10px;
}
/* 提示卡片 */
.tips-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.tips-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.tips-card li {
  padding: 10px 0;
  border-bottom: 1px solid #f2f2f7;
  font-size: 13px;
  color: #48484a;
  line-height: 1.5;
}
.tips-card li:last-child { border-bottom: none; }
.tips-card li::before {
  content: '💡';
  margin-right: 8px;
}
/* 历史记录 */
.history-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.history-header h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}
.history-count {
  font-size: 12px;
  color: #86868b;
}
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
.file-name {
  font-weight: 500;
  color: #1d1d1f;
}
.muted { color: #86868b; }
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, monospace;
  font-size: 13px;
}
.cat-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.cat-badge.policy { background: #E3F2FD; color: #1976D2; }
.cat-badge.process { background: #FFF3E0; color: #F57C00; }
.cat-badge.faq { background: #E8F5E9; color: #388E3C; }
.cat-badge.business { background: #F3E5F5; color: #7B1FA2; }
.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}
.status-badge.completed, .status-badge.ready { background: #d1f7d1; color: #248a3d; }
.status-badge.importing, .status-badge.processing { background: #fff3e0; color: #cc7a00; }
.status-badge.failed { background: #FFD9D9; color: #d32f2f; }
.actions { white-space: nowrap; }
.btn-text {
  background: none;
  border: none;
  color: #007AFF;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 3px 6px;
  border-radius: 6px;
  transition: background 0.2s;
}
.btn-text:hover { background: #e5f1ff; }
.btn-text:disabled { color: #c7c7cc; cursor: not-allowed; }
.empty-state {
  text-align: center;
  color: #86868b;
  padding: 48px;
  font-size: 14px;
}
.empty-state p {
  margin-top: 10px;
}
</style>