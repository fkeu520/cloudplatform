import request from './request'

export function getServiceHealth() {
  return request.get('/ops/monitor/health')
}
