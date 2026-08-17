<template>
  <div class="knowledge-page">
    <header>
      <h1>知识库管理</h1>
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
        <span>知识库管理</span>
      </div>
    </header>
    <div class="content">
      <!-- 上传区域 -->
      <div class="upload-card">
        <h3>上传文档</h3>
        <div
          class="upload-area"
          :class="{ dragging: isDragging, uploading: uploading }"
          @click="triggerUpload"
          @drop.prevent="handleDrop"
          @dragover.prevent="isDragging = true"
          @dragleave="isDragging = false"
        >
          <input type="file" ref="fileInput" @change="handleFileChange" accept=".pdf,.docx,.doc,.md,.txt" style="display:none" />
          <div class="upload-icon">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <path d="M16 6v14M10 12l6-6 6 6" stroke="#007AFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M6 22v4a2 2 0 002 2h16a2 2 0 002-2v-4" stroke="#007AFF" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <p v-if="!uploading">点击或拖拽上传<br><span>PDF · Word · Markdown · TXT</span></p>
          <p v-else>上传处理中...</p>
        </div>
        <p v-if="uploadError" class="error-msg">{{ uploadError }}</p>
      </div>

      <!-- 统计 -->
      <div class="stats-row" v-if="stats">
        <div class="stat-pill">
          <span class="stat-num">{{ stats.document_count }}</span>
          <span class="stat-lbl">文档数</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ stats.chunk_count }}</span>
          <span class="stat-lbl">切片数</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ stats.vector_count }}</span>
          <span class="stat-lbl">向量数</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ stats.total_size_mb }}MB</span>
          <span class="stat-lbl">总大小</span>
        </div>
      </div>

      <!-- 文档列表 -->
      <div class="doc-card">
        <h3>文档列表</h3>
        <div class="table-wrap" v-if="documents.length">
          <table>
            <thead>
              <tr>
                <th>文件名</th>
                <th>类型</th>
                <th>大小</th>
                <th>切片数</th>
                <th>状态</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in documents" :key="doc.doc_id">
                <td class="doc-name">{{ doc.name }}</td>
                <td class="muted">{{ doc.type }}</td>
                <td class="muted">{{ formatSize(doc.size_bytes) }}</td>
                <td class="mono">{{ doc.chunk_count }}</td>
                <td><span :class="['status-badge', doc.status]">{{ statusText(doc.status) }}</span></td>
                <td class="muted">{{ formatDate(doc.upload_time) }}</td>
                <td class="actions">
                  <button class="btn-text" @click="viewChunks(doc.doc_id)">查看切片</button>
                  <button class="btn-text" @click="reprocessDoc(doc.doc_id)" :disabled="doc.status === 'processing'">重新处理</button>
                  <button class="btn-text danger" @click="deleteDoc(doc.doc_id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <p>暂无文档，请上传</p>
        </div>
      </div>

      <!-- 切片弹窗 -->
      <div v-if="showChunksModal" class="modal-overlay" @click="showChunksModal = false">
        <div class="modal-card" @click.stop>
          <div class="modal-header">
            <h3>切片详情</h3>
            <button class="modal-close" @click="showChunksModal = false">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M5 5l10 10M15 5L5 15" stroke="#86868b" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="chunks-list">
            <div v-for="chunk in chunks" :key="chunk.chunk_id" class="chunk-item">
              <div class="chunk-meta">#{{ chunk.index_in_doc }} · {{ chunk.token_count }} tokens</div>
              <div class="chunk-content">{{ chunk.content }}</div>
            </div>
          </div>
          <button class="modal-btn" @click="showChunksModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '@/api'
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const uploadError = ref('')
const documents = ref<any[]>([])
const stats = ref<any>(null)
const chunks = ref<any[]>([])
const showChunksModal = ref(false)
const isDragging = ref(false)

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
  try {
    const formData = new FormData()
    formData.append('file', file)
    await knowledgeApi.uploadDoc(formData)
    await loadData()
  } catch (e: any) {
    uploadError.value = e.response?.data?.detail || '上传失败'
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const loadData = async () => {
  try {
    const [docsRes, statsRes] = await Promise.all([
      knowledgeApi.listDocs(),
      knowledgeApi.stats()
    ])
    documents.value = docsRes.data.items || []
    stats.value = statsRes.data
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const deleteDoc = async (docId: string) => {
  if (!confirm('确定删除该文档？')) return
  try {
    await knowledgeApi.deleteDoc(docId)
    await loadData()
  } catch (e: any) {
    alert(e.response?.data?.detail || '删除失败')
  }
}

const reprocessDoc = async (docId: string) => {
  try {
    await knowledgeApi.reprocessDoc(docId)
    await loadData()
  } catch (e: any) {
    alert(e.response?.data?.detail || '重新处理失败')
  }
}

const viewChunks = async (docId: string) => {
  try {
    const res = await knowledgeApi.listChunks(docId)
    chunks.value = res.data.items || []
    showChunksModal.value = true
  } catch (e) {
    alert('加载切片失败')
  }
}

const formatSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
.knowledge-page {
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
.upload-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.upload-card h3 {
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
.error-msg {
  color: #ff3b30;
  font-size: 13px;
  margin-top: 10px;
}
.stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.stat-pill {
  flex: 1;
  background: #ffffff;
  padding: 16px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05), 0 0 0 1px rgba(0,0,0,0.02);
}
.stat-num {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #007AFF;
  letter-spacing: -0.3px;
}
.stat-lbl {
  display: block;
  font-size: 11px;
  color: #86868b;
  font-weight: 500;
  margin-top: 2px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.doc-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
}
.doc-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 18px;
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
.doc-name {
  font-weight: 500;
  color: #1d1d1f;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.muted { color: #86868b; }
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, monospace;
  font-size: 13px;
}
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
.btn-text.danger { color: #ff3b30; }
.btn-text.danger:hover { background: #fff1f0; }
.empty-state {
  text-align: center;
  color: #86868b;
  padding: 48px;
  font-size: 14px;
}
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
  max-width: 820px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.02);
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
.chunks-list { margin-bottom: 18px; }
.chunk-item {
  padding: 14px 16px;
  background: #f5f5f7;
  border-radius: 10px;
  margin-bottom: 8px;
}
.chunk-meta {
  font-size: 11px;
  color: #86868b;
  font-weight: 600;
  letter-spacing: 0.3px;
  margin-bottom: 6px;
  text-transform: uppercase;
}
.chunk-content {
  font-size: 13px;
  line-height: 1.7;
  color: #1d1d1f;
}
.modal-btn {
  width: 100%;
  padding: 12px;
  background: #007AFF;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.modal-btn:hover { background: #0056b3; }
</style>
