import request from './request'

export function getOperLogPage(params: any) {
  return request({
    url: '/oper-log/page',
    method: 'get',
    params
  })
}

export function getOperLogById(id: number) {
  return request({
    url: `/oper-log/${id}`,
    method: 'get'
  })
}

export function deleteOperLog(id: number) {
  return request({
    url: `/oper-log/${id}`,
    method: 'delete'
  })
}

export function batchDeleteOperLog(ids: number[]) {
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
