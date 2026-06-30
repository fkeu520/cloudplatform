import request from './request'

/**
 * 企业档案 API (park-enterprise 后端)
 * 后端端口: park-enterprise:8094
 * Gateway 路由: /enterprise/** → park-enterprise
 */

export interface Enterprise {
  id?: string
  name?: string          // 企业名称 (必填)
  alias?: string         // 简称
  creditCode?: string    // 统一社会信用代码
  taxNumber?: string     // 纳税人识别号
  industry?: string      // 行业
  category?: string      // 国民经济行业分类
  legalPersonName?: string  // 法人
  regCapital?: string    // 注册资本
  regCapitalCurrency?: string
  regLocation?: string   // 注册地址
  regStatus?: string     // 经营状态
  staffNumRange?: string // 人员规模
  businessScope?: string // 经营范围
  phoneNumber?: string   // 联系电话
  email?: string
  websiteList?: string
  logo?: string
  status?: number        // 1=启用 0=停用
  createTime?: string
  createBy?: string
  updateBy?: string
}

export function getEnterprisePage(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/enterprise/page', method: 'get', params })
}

export function getEnterpriseById(id: string) {
  return request({ url: `/enterprise/${id}`, method: 'get' })
}

export function createEnterprise(data: Partial<Enterprise>) {
  return request({ url: '/enterprise', method: 'post', data })
}

export function updateEnterprise(id: string, data: Partial<Enterprise>) {
  return request({ url: `/enterprise/${id}`, method: 'put', data })
}

export function deleteEnterprise(id: string) {
  return request({ url: `/enterprise/${id}`, method: 'delete' })
}

export function toggleEnterpriseStatus(id: string, status: number) {
  return request({ url: `/enterprise/${id}/status`, method: 'patch', data: { status } })
}
