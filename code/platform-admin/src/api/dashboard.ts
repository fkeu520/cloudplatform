import request from './request'

export function getUserCount() {
  return request.get('/user/page', { params: { pageNum: 1, pageSize: 1 } })
}

export function getTodayLoginCount() {
  const today = new Date()
  const startTime = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')} 00:00:00`
  return request.get('/login-log/page', { params: { startTime, pageNum: 1, pageSize: 1 } })
}

export function getRecentLogs() {
  return request.get('/login-log/page', { params: { pageNum: 1, pageSize: 5 } })
}
