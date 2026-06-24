import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '@/stores/user' // O5: 统一状态管理

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    // O1: Token 存 localStorage (SPA 标准方案). XSS 防护依赖 CSP 头 + 输入过滤,
    //     生产环境请配置 Content-Security-Policy: default-src 'self' 等策略
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 401) {
      // O3: 统一 401 处理路径 → 清除状态并跳转
      const store = useUserStore()
      store.logout()
      router.push('/login')
      return Promise.reject(new Error('未登录或Token已过期'))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      // O3: HTTP 401 走与响应拦截器相同的逻辑，避免重复弹窗
      const store = useUserStore()
      store.logout()
      router.push('/login')
      return Promise.reject(new Error('未登录或Token已过期'))
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
