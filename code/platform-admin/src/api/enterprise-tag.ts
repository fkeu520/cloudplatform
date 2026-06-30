import request from './request'

/**
 * 企业标签 API
 */

export interface EnterpriseTag {
  id?: string
  enterpriseId?: string | number
  tagName?: string
  tagColor?: string
  sortOrder?: number
  remark?: string
  createBy?: string
  createTime?: string
}

export function listTagByEnterprise(enterpriseId: string | number) {
  return request({ url: '/enterprise/tag/list', method: 'get', params: { enterpriseId } })
}

export function listAllTags() {
  return request({ url: '/enterprise/tag/all', method: 'get' })
}

export function createTag(data: Partial<EnterpriseTag>) {
  return request({ url: '/enterprise/tag', method: 'post', data })
}

export function deleteTag(id: string) {
  return request({ url: `/enterprise/tag/${id}`, method: 'delete' })
}
