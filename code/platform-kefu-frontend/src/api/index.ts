import axios from 'axios'

const API_BASE_URL = ''

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('kefu_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('kefu_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

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

export const chatApi = {
  ask: (question: string) => api.post('/api/kefu/ask', { question }),
  askStream: (question: string) => {
    const token = localStorage.getItem('kefu_token')
    return fetch(`${API_BASE_URL}/api/kefu/ask/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ question })
    })
  },
  history: (page = 1, pageSize = 20) => api.get('/api/kefu/ask/history', { params: { page, page_size: pageSize } }),
}

export default api
