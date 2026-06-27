import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000 // F10: 从 10s 提升到 30s，适应大数据量+data_scope CTE 查询
})

request.interceptors.request.use((config) => {
  // F1: Token 存 localStorage (SPA 标准方案). XSS 防护依赖 CSP 头 + 输入过滤,
  //     生产环境请配置 Content-Security-Policy: default-src 'self' 等策略
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // F3: 401 时清除 store 中的残留状态，避免页面卡循环重定向
      const store = useUserStore()
      store.logout()
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      // A2-4: 移除 ElMessage 弹窗，避免与组件 catch 双重提示
      // 错误信息由组件 catch 自行处理，或通过浏览器控制台查看
      const msg = error.response?.data?.message || error.message || '请求失败'
      console.warn('[API Error]', msg)
    }
    return Promise.reject(error)
  }
)

export default request