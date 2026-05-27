import request from './request'

export interface PageParams {
  pageNum?: number
  pageSize?: number
  type?: string
  readStatus?: number
  keyword?: string
  userId?: string
}

export interface ChannelPageParams {
  pageNum?: number
  pageSize?: number
}

export interface TemplatePageParams {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  keyword?: string
}

export interface RecordPageParams {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  sendStatus?: number
  tenantId?: number
  startTime?: string
  endTime?: string
  keyword?: string
}

export interface SendTestParams {
  channelCode: string
  receiverAddress: string
  content: string
}

// ====== 站内信 ======

export function getSiteMessagePage(params: PageParams) {
  return request.get('/message/site/page', { params })
}

export function getUnreadSiteMessageCount(userId: string) {
  return request.get('/message/site/unread-count', { params: { userId } })
}

export function getSiteMessageById(id: number) {
  return request.get(`/message/site/${id}`)
}

export function markSiteMessageRead(id: number) {
  return request.post(`/message/site/read/${id}`)
}

export function markAllSiteMessageRead(userId: string) {
  return request.post('/message/site/read-all', { userId })
}

export function deleteSiteMessage(id: number) {
  return request.delete(`/message/site/${id}`)
}

// ====== 渠道管理 ======

export function getChannelList() {
  return request.get('/message/channel/list')
}

export function getChannelPage(params: ChannelPageParams) {
  return request.get('/message/channel/page', { params })
}

export function getChannelById(id: number) {
  return request.get(`/message/channel/${id}`)
}

export function createChannel(data: any) {
  return request.post('/message/channel', data)
}

export function updateChannel(id: number, data: any) {
  return request.put(`/message/channel/${id}`, data)
}

export function deleteChannel(id: number) {
  return request.delete(`/message/channel/${id}`)
}

// ====== 模板管理 ======

export function getTemplatePage(params: TemplatePageParams) {
  return request.get('/message/template/page', { params })
}

export function getTemplateById(id: number) {
  return request.get(`/message/template/${id}`)
}

export function createTemplate(data: any) {
  return request.post('/message/template', data)
}

export function updateTemplate(id: number, data: any) {
  return request.put(`/message/template/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete(`/message/template/${id}`)
}

// ====== 消息发送记录 ======

export function getRecordPage(params: RecordPageParams) {
  return request.get('/message/record/page', { params })
}

export function getRecordById(id: number) {
  return request.get(`/message/record/${id}`)
}

export function sendMessage(data: any) {
  return request.post('/message/record/send', data)
}

export function sendTestMessage(data: SendTestParams) {
  return request.post('/message/record/test-send', data)
}

export function resendRecord(id: number) {
  return request.post(`/message/record/resend/${id}`)
}

export function deleteRecord(id: number) {
  return request.delete(`/message/record/${id}`)
}
