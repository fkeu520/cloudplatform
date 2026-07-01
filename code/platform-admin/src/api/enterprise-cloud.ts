import request from '@/api/request'

/** 云企库数据分类 */
export type CloudDataCategory =
  | 'business_risk'
  | 'business_situation'
  | 'ent_detail'
  | 'judicial_risk'
  | 'knowledge'

/** 云企库数据 */
export interface CloudData {
  id: string
  tenantId: string
  enterpriseId: string
  enterpriseName: string
  category: CloudDataCategory
  dataContent: string
  dataYear: string
  dataDate: string
  sortOrder: number
  status: number
  remark: string
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
}

/** 企业注册类型 */
export interface EntRegType {
  id: string
  code: string
  name: string
  parentCode: string
  sortOrder: number
  status: number
  remark: string
}

/** 国民经济行业分类 */
export interface NationalEconomy {
  id: string
  code: string
  name: string
  parentCode: string
  level: number
  sortOrder: number
  status: number
  remark: string
}

/** 企业概览数据 */
export interface EnterpriseOverview {
  id: string
  enterpriseId: string
  enterpriseName: string
  regCapital: string
  totalAssets: string
  annualRevenue: string
  employeeCount: number
  patentCount: number
  trademarkCount: number
  copyrightCount: number
  riskCount: number
  bidCount: number
  equityStructureJson: string
  overviewJson: string
  sortOrder: number
  status: number
  remark: string
}

// ──── 云企库数据 CloudData ────

export function cloudDataPage(params: {
  category: CloudDataCategory
  enterpriseId?: string
  keyword?: string
  dataYear?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<{ records: CloudData[]; total: number }>('/enterprise/cloud-data/page', { params })
}

export function cloudDataDetail(id: string) {
  return request.get<CloudData>(`/enterprise/cloud-data/${id}`)
}

export function cloudDataCreate(data: Partial<CloudData>) {
  return request.post<CloudData>('/enterprise/cloud-data', data)
}

export function cloudDataUpdate(id: string, data: Partial<CloudData>) {
  return request.put<CloudData>(`/enterprise/cloud-data/${id}`, data)
}

export function cloudDataDelete(id: string) {
  return request.delete(`/enterprise/cloud-data/${id}`)
}

// ──── 企业注册类型 EntRegType ────

export function regTypePage(params: {
  keyword?: string
  parentCode?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<{ records: EntRegType[]; total: number }>('/enterprise/reg-type/page', { params })
}

export function regTypeDetail(id: string) {
  return request.get<EntRegType>(`/enterprise/reg-type/${id}`)
}

export function regTypeCreate(data: Partial<EntRegType>) {
  return request.post<EntRegType>('/enterprise/reg-type', data)
}

export function regTypeUpdate(id: string, data: Partial<EntRegType>) {
  return request.put<EntRegType>(`/enterprise/reg-type/${id}`, data)
}

export function regTypeDelete(id: string) {
  return request.delete(`/enterprise/reg-type/${id}`)
}

// ──── 国民经济行业分类 NationalEconomy ────

export function economyPage(params: {
  keyword?: string
  level?: number
  parentCode?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<{ records: NationalEconomy[]; total: number }>('/enterprise/national-economy/page', { params })
}

export function economyDetail(id: string) {
  return request.get<NationalEconomy>(`/enterprise/national-economy/${id}`)
}

export function economyCreate(data: Partial<NationalEconomy>) {
  return request.post<NationalEconomy>('/enterprise/national-economy', data)
}

export function economyUpdate(id: string, data: Partial<NationalEconomy>) {
  return request.put<NationalEconomy>(`/enterprise/national-economy/${id}`, data)
}

export function economyDelete(id: string) {
  return request.delete(`/enterprise/national-economy/${id}`)
}

// ──── 企业概览 EnterpriseOverview ────

export function overviewPage(params: {
  enterpriseId?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<{ records: EnterpriseOverview[]; total: number }>('/enterprise/overview/page', { params })
}

export function overviewDetail(id: string) {
  return request.get<EnterpriseOverview>(`/enterprise/overview/${id}`)
}

export function overviewCreate(data: Partial<EnterpriseOverview>) {
  return request.post<EnterpriseOverview>('/enterprise/overview', data)
}

export function overviewUpdate(id: string, data: Partial<EnterpriseOverview>) {
  return request.put<EnterpriseOverview>(`/enterprise/overview/${id}`, data)
}

export function overviewDelete(id: string) {
  return request.delete(`/enterprise/overview/${id}`)
}

// ──── 云企库数据 category 名称映射 (csyh 模块名) ────
export const CLOUD_CATEGORY_LABELS: Record<CloudDataCategory, string> = {
  business_risk: '经营风险',
  business_situation: '经营状况',
  ent_detail: '企业详情',
  judicial_risk: '司法风险',
  knowledge: '企业知识',
}

export const CLOUD_CATEGORY_LIST: { value: CloudDataCategory; label: string }[] = [
  { value: 'business_risk', label: '经营风险' },
  { value: 'business_situation', label: '经营状况' },
  { value: 'ent_detail', label: '企业详情' },
  { value: 'judicial_risk', label: '司法风险' },
  { value: 'knowledge', label: '企业知识' },
]
