<template>
  <div class="faqs-page">
    <header>
      <h1>FAQ 管理</h1>
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
        <span>FAQ 管理</span>
      </div>
    </header>

    <div class="content">
      <!-- 工具栏 -->
      <div class="toolbar">
        <button class="btn-primary" @click="openCreateModal">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          新增 FAQ
        </button>
        <div class="filter-group">
          <select v-model="selectedCategory" @change="loadFaqs">
            <option value="">全部分类</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>

      <!-- FAQ 列表 -->
      <div class="faq-card">
        <div class="table-wrap" v-if="faqs.length">
          <table>
            <thead>
              <tr>
                <th>问题</th>
                <th>答案</th>
                <th>分类</th>
                <th>命中次数</th>
                <th>满意度</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="faq in faqs" :key="faq.faq_id">
                <td class="faq-question">{{ faq.question }}</td>
                <td class="faq-answer">{{ summary(faq.answer) }}</td>
                <td><span class="category-tag">{{ faq.category || '-' }}</span></td>
                <td class="mono">{{ faq.hit_count }}</td>
                <td class="mono">{{ faq.sat_avg != null ? faq.sat_avg.toFixed(1) : '-' }}</td>
                <td>
                  <label class="toggle" @click.prevent="toggleEnabled(faq)">
                    <input type="checkbox" :checked="faq.enabled" />
                    <span class="toggle-slider"></span>
                  </label>
                </td>
                <td class="actions">
                  <button class="btn-text" @click="openEditModal(faq)">编辑</button>
                  <button class="btn-text danger" @click="confirmDelete(faq)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else-if="!loading" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect x="6" y="10" width="36" height="28" rx="4" stroke="#c7c7cc" stroke-width="2"/>
            <path d="M16 20h16M16 26h12M16 32h8" stroke="#c7c7cc" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <p>暂无 FAQ，点击上方按钮新增</p>
        </div>
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>
      </div>

      <!-- 新增/编辑弹窗 -->
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-card" @click.stop>
          <div class="modal-header">
            <h3>{{ editingFaq ? '编辑 FAQ' : '新增 FAQ' }}</h3>
            <button class="modal-close" @click="closeModal">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M5 5l10 10M15 5L5 15" stroke="#86868b" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>问题 <span class="required">*</span></label>
              <textarea
                v-model="form.question"
                placeholder="请输入常见问题"
                rows="3"
                :class="{ 'input-error': formErrors.question }"
              ></textarea>
              <p v-if="formErrors.question" class="field-error">{{ formErrors.question }}</p>
            </div>
            <div class="form-group">
              <label>答案 <span class="required">*</span></label>
              <textarea
                v-model="form.answer"
                placeholder="请输入答案内容"
                rows="5"
                :class="{ 'input-error': formErrors.answer }"
              ></textarea>
              <p v-if="formErrors.answer" class="field-error">{{ formErrors.answer }}</p>
            </div>
            <div class="form-group">
              <label>分类</label>
              <input
                v-model="form.category"
                placeholder="例如：订单、退款、账户"
                list="category-suggestions"
              />
              <datalist id="category-suggestions">
                <option v-for="c in categories" :key="c" :value="c" />
              </datalist>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="closeModal" :disabled="saving">取消</button>
            <button class="btn-primary" @click="saveFaq" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 删除确认 -->
      <div v-if="showDeleteConfirm" class="modal-overlay" @click="showDeleteConfirm = false">
        <div class="modal-card confirm-card" @click.stop>
          <div class="confirm-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <circle cx="14" cy="14" r="12" stroke="#ff3b30" stroke-width="2"/>
              <path d="M14 9v6M14 18v.01" stroke="#ff3b30" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <h3>确认删除</h3>
          <p>确定要删除这条 FAQ 吗？此操作不可恢复。</p>
          <div class="confirm-actions">
            <button class="btn-secondary" @click="showDeleteConfirm = false" :disabled="deleting">取消</button>
            <button class="btn-danger" @click="deleteFaq" :disabled="deleting">
              {{ deleting ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Toast -->
      <transition name="toast">
        <div v-if="toast" class="toast" :class="toastType">{{ toast }}</div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { faqApi } from '@/api'

interface FaqResponse {
  faq_id: string
  question: string
  answer: string
  category?: string
  hit_count: number
  sat_avg: number | null
  enabled: boolean
  created_at: string
  updated_at: string
}

interface FormErrors {
  question?: string
  answer?: string
}

const faqs = ref<FaqResponse[]>([])
const categories = ref<string[]>([])
const loading = ref(false)
const selectedCategory = ref('')

// Modal
const showModal = ref(false)
const editingFaq = ref<FaqResponse | null>(null)
const form = ref({ question: '', answer: '', category: '' })
const formErrors = ref<FormErrors>({})
const saving = ref(false)

// Delete confirm
const showDeleteConfirm = ref(false)
const deletingFaq = ref<FaqResponse | null>(null)
const deleting = ref(false)

// Toast
const toast = ref('')
const toastType = ref('success')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
  toast.value = msg
  toastType.value = type
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

const summary = (text: string) => {
  if (!text) return '-'
  const stripped = text.replace(/<[^>]+>/g, '')
  return stripped.length > 80 ? stripped.substring(0, 80) + '...' : stripped
}

const loadFaqs = async () => {
  loading.value = true
  try {
    const params: any = { limit: 50 }
    if (selectedCategory.value) params.category = selectedCategory.value
    const res = await faqApi.list(params)
    faqs.value = res.data.items || []
    // Extract unique categories
    const catSet = new Set<string>()
    for (const f of faqs.value) {
      if (f.category) catSet.add(f.category)
    }
    categories.value = Array.from(catSet).sort()
  } catch (e: any) {
    showToast('加载 FAQ 失败', 'error')
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  editingFaq.value = null
  form.value = { question: '', answer: '', category: '' }
  formErrors.value = {}
  showModal.value = true
}

const openEditModal = (faq: FaqResponse) => {
  editingFaq.value = faq
  form.value = {
    question: faq.question,
    answer: faq.answer,
    category: faq.category || ''
  }
  formErrors.value = {}
  showModal.value = true
}

const closeModal = () => {
  if (saving.value) return
  showModal.value = false
  editingFaq.value = null
  form.value = { question: '', answer: '', category: '' }
  formErrors.value = {}
}

const validate = (): boolean => {
  const errors: FormErrors = {}
  if (!form.value.question.trim()) {
    errors.question = '请输入问题'
  }
  if (!form.value.answer.trim()) {
    errors.answer = '请输入答案'
  }
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

const saveFaq = async () => {
  if (!validate()) return
  saving.value = true
  try {
    const data = {
      question: form.value.question.trim(),
      answer: form.value.answer.trim(),
      category: form.value.category.trim() || undefined
    }
    if (editingFaq.value) {
      await faqApi.update(editingFaq.value.faq_id, data)
      showToast('FAQ 已更新')
    } else {
      await faqApi.create(data)
      showToast('FAQ 已创建')
    }
    closeModal()
    await loadFaqs()
  } catch (e: any) {
    showToast(e.response?.data?.detail || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

const confirmDelete = (faq: FaqResponse) => {
  deletingFaq.value = faq
  showDeleteConfirm.value = true
}

const deleteFaq = async () => {
  if (!deletingFaq.value) return
  deleting.value = true
  try {
    await faqApi.delete(deletingFaq.value.faq_id)
    showToast('FAQ 已删除')
    showDeleteConfirm.value = false
    deletingFaq.value = null
    await loadFaqs()
  } catch (e: any) {
    showToast(e.response?.data?.detail || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

const toggleEnabled = async (faq: FaqResponse) => {
  const original = faq.enabled
  try {
    faq.enabled = !faq.enabled
    await faqApi.update(faq.faq_id, { question: faq.question, answer: faq.answer, category: faq.category })
  } catch (e: any) {
    faq.enabled = original
    showToast('状态更新失败', 'error')
  }
}

onMounted(loadFaqs)
</script>

<style scoped>
.faqs-page {
  min-height: 100vh;
  background: #f2f2f7;
}

/* Header */
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

/* Content */
.content {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 24px;
}

/* Toolbar */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-group select {
  padding: 8px 32px 8px 14px;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  font-size: 13px;
  color: #1d1d1f;
  background: #ffffff url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%2386868b' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") no-repeat right 12px center;
  appearance: none;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}
.filter-group select:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}

/* Buttons */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: #007AFF;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover:not(:disabled) { background: #0056b3; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-secondary {
  padding: 8px 18px;
  background: #f2f2f7;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-secondary:hover:not(:disabled) { background: #e5e5ea; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-danger {
  padding: 8px 18px;
  background: #ff3b30;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-danger:hover:not(:disabled) { background: #d70015; }
.btn-danger:disabled { opacity: 0.5; cursor: not-allowed; }

/* Card */
.faq-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.02);
  overflow: hidden;
}

/* Table */
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

.faq-question {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: #1d1d1f;
}
.faq-answer {
  max-width: 260px;
  color: #6e6e73;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, monospace;
  font-size: 13px;
}

.category-tag {
  display: inline-block;
  padding: 3px 10px;
  background: #e5f1ff;
  color: #007AFF;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

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
.btn-text.danger { color: #ff3b30; }
.btn-text.danger:hover { background: #fff1f0; }

/* Toggle Switch */
.toggle {
  position: relative;
  display: inline-block;
  width: 36px;
  height: 20px;
  cursor: pointer;
}
.toggle input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.toggle-slider {
  position: absolute;
  inset: 0;
  background: #c7c7cc;
  border-radius: 9999px;
  transition: background 0.25s;
}
.toggle-slider::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-radius: 50%;
  transition: transform 0.25s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.toggle input:checked + .toggle-slider {
  background: #34c759;
}
.toggle input:checked + .toggle-slider::before {
  transform: translateX(16px);
}

/* Empty & Loading */
.empty-state {
  text-align: center;
  color: #86868b;
  padding: 64px 48px;
  font-size: 14px;
}
.empty-state svg {
  margin-bottom: 12px;
}
.empty-state p {
  margin: 0;
}
.loading-state {
  text-align: center;
  padding: 64px;
  color: #86868b;
  font-size: 14px;
}
.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e5e5ea;
  border-top-color: #007AFF;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  margin: 0 auto 12px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
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
  max-width: 540px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.02);
}
.confirm-card {
  max-width: 380px;
  text-align: center;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.modal-body {
  margin-bottom: 20px;
}

/* Form */
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 6px;
}
.required { color: #ff3b30; }
.form-group textarea,
.form-group input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 14px;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  font-size: 14px;
  color: #1d1d1f;
  font-family: inherit;
  outline: none;
  resize: vertical;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.form-group textarea:focus,
.form-group input:focus {
  border-color: #007AFF;
  box-shadow: 0 0 0 3px rgba(0,122,255,0.15);
}
.form-group textarea::placeholder,
.form-group input::placeholder {
  color: #86868b;
}
.input-error {
  border-color: #ff3b30 !important;
}
.field-error {
  margin: 4px 0 0;
  font-size: 12px;
  color: #ff3b30;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Confirm */
.confirm-icon {
  margin-bottom: 12px;
}
.confirm-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px;
}
.confirm-card p {
  font-size: 14px;
  color: #6e6e73;
  margin: 0 0 20px;
  line-height: 1.5;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
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
.toast.error {
  background: rgba(215, 0, 21, 0.9);
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