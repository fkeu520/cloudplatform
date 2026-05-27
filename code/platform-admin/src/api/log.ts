import request from './request'

export function getOperLogPage(params: Record<string, any>) {
  return request({
    url: '/oper-log/page',
    method: 'get',
    params
  })
}

export function getOperLogById(id: string) {
  return request({
    url: `/oper-log/${id}`,
    method: 'get'
  })
}

export function deleteOperLog(id: string) {
  return request({
    url: `/oper-log/${id}`,
    method: 'delete'
  })
}

export function batchDeleteOperLog(ids: string[]) {
  return request({
    url: '/oper-log/batch',
    method: 'delete',
    data: ids
  })
}

export function clearOperLog() {
  return request({
    url: '/oper-log/clear',
    method: 'delete'
  })
}

export function getLoginLogPage(params: Record<string, any>) {
  return request({
    url: '/login-log/page',
    method: 'get',
    params
  })
}
