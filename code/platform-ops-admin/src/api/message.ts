import request from './request'

export function getRecordPage(params: {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  sendStatus?: number
  tenantId?: string
  startTime?: string
  endTime?: string
  keyword?: string
}) {
  return request.get('/message/record/page', { params })
}

export function getRecordById(id: string) {
  return request.get(`/message/record/${id}`)
}

export function resendRecord(id: string) {
  return request.post(`/message/record/resend/${id}`)
}

export function deleteRecord(id: string) {
  return request.delete(`/message/record/${id}`)
}
