import axios from 'axios'

const API_BASE_URL = ''

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  // Token 来源兼容两种部署模式:
  // 1. 嵌入 platform-admin (同域名 cloud.hugh.sryze.cc): platform-admin 用 'token' 键
  // 2. 独立部署 kefu (不同子域名): 用 'kefu_token'
  // 共享 HS384 JWT_SECRET (commit a3a408f 已统一), 两边都能验签
  const token = localStorage.getItem('token') || localStorage.getItem('kefu_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 401 时只清 token, 不跳 /login — iframe 跳顶层 /login 会把 platform-admin
      // 已登录态破坏, 反而更糟. 让用户在 platform-admin 顶层 logout 再登录即可.
      localStorage.removeItem('kefu_token')
      console.error('[kefu] 会话已过期 (401), 请在 platform-admin 重新登录')
    }
    return Promise.reject(error)
  }
)

// === 知识库 ===
export const knowledgeApi = {
  listDocs: () => api.get('/api/kefu/docs'),
  uploadDoc: (formData: FormData) => api.post('/api/kefu/docs/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  deleteDoc: (docId: string) => api.delete(`/api/kefu/docs/${docId}`),
  reprocessDoc: (docId: string) => api.post(`/api/kefu/docs/${docId}/reprocess`),
  listChunks: (docId?: string, page = 1) => api.get('/api/kefu/knowledge/chunks', { params: { doc_id: docId, page } }),
  rebuildIndex: () => api.post('/api/kefu/knowledge/rebuild-index'),
  stats: () => api.get('/api/kefu/knowledge/stats'),
}

// === 会话 ===
export const sessionApi = {
  create: (data?: { customer_name?: string; contact?: string; channel?: string }) =>
    api.post('/api/kefu/sessions', data || {}),
  get: (sid: string) => api.get(`/api/kefu/sessions/${sid}`),
  list: (params?: { status?: string; channel?: string; limit?: number; offset?: number }) =>
    api.get('/api/kefu/sessions', { params }),
  transfer: (sid: string, agentId?: number, agentName?: string) =>
    api.post(`/api/kefu/sessions/${sid}/transfer`, null, { params: { agent_id: agentId, agent_name: agentName } }),
  close: (sid: string) => api.post(`/api/kefu/sessions/${sid}/close`),
  rate: (sid: string, satisfaction: number, comment?: string) =>
    api.post(`/api/kefu/sessions/${sid}/rate`, { satisfaction, comment }),
  getMessages: (sid: string, limit = 100) => api.get(`/api/kefu/sessions/${sid}/messages`, { params: { limit } }),
  sendMessage: (sid: string, content: string) =>
    api.post(`/api/kefu/sessions/${sid}/messages`, { content, role: 'customer' }),
}

// === 兼容旧 Chat 的 ask API（保留） ===
export const chatApi = {
  ask: (question: string) => api.post('/api/kefu/ask', { question }),
  history: (page = 1, pageSize = 20) => api.get('/api/kefu/ask/history', { params: { page, page_size: pageSize } }),
}

// === FAQ ===
export const faqApi = {
  list: (params?: { category?: string; enabled?: boolean; limit?: number; offset?: number }) =>
    api.get('/api/kefu/faqs', { params }),
  get: (faqId: string) => api.get(`/api/kefu/faqs/${faqId}`),
  create: (data: { question: string; answer: string; category?: string }) =>
    api.post('/api/kefu/faqs', data),
  update: (faqId: string, data: { question: string; answer: string; category?: string }) =>
    api.put(`/api/kefu/faqs/${faqId}`, data),
  delete: (faqId: string) => api.delete(`/api/kefu/faqs/${faqId}`),
}

// === 数据源 ===
export const dataSourceApi = {
  list: () => api.get('/api/kefu/data_sources'),
  get: (sourceId: string) => api.get(`/api/kefu/data_sources/${sourceId}`),
  update: (sourceId: string, data: { enabled?: boolean; sync_strategy?: string; intent_keywords?: string[]; config_json?: any }) =>
    api.put(`/api/kefu/data_sources/${sourceId}`, data),
  register: (data: { id: string; name: string; type: string; module_ref?: string; sync_strategy?: string; intent_keywords?: string[]; config_json?: any }) =>
    api.post('/api/kefu/data_sources/register', data),
  sync: (sourceId: string) => api.post(`/api/kefu/data_sources/${sourceId}/sync`),
}

// === 看板 + 评估 ===
export const dashboardApi = {
  stats: () => api.get('/api/kefu/dashboard'),
  createEvaluation: (data: { msg_id: string; source_id?: string; score: number; comment?: string }) =>
    api.post('/api/kefu/evaluations', data),
  listEvaluations: (sessionId: string) => api.get(`/api/kefu/evaluations/${sessionId}`),
}

export default api