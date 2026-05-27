import request from './request'

export function getRecordPage(params: {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  sendStatus?: number
  tenantId?: number
  startTime?: string
  endTime?: string
  keyword?: string
}) {
  return request.get('/message/record/page', { params })
}

export function getRecordById(id: number) {
  return request.get(`/message/record/${id}`)
}

export function resendRecord(id: number) {
  return request.post(`/message/record/resend/${id}`)
}

export function deleteRecord(id: number) {
  return request.delete(`/message/record/${id}`)
}
