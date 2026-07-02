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
  historyNames?: string  // 曾用名
  historyNameList?: string
  engName?: string       // 英文名
  taxNumber?: string     // 纳税人识别号
  creditCode?: string    // 统一社会信用代码
  industry?: string      // 行业
  category?: string      // 国民经济行业分类
  categoryBig?: string   // 大类
  categoryMiddle?: string // 中类
  categorySmall?: string // 小类
  legalPersonName?: string  // 法人
  type?: number          // 法人类型
  companyOrgType?: string
  regCapital?: string    // 注册资本
  regCapitalCurrency?: string
  actualCapital?: string // 实收资本
  actualCapitalCurrency?: string
  regNumber?: string     // 注册号
  orgNumber?: string     // 组织机构代码
  estiblishTime?: string // 成立日期
  fromTime?: string
  toTime?: string
  approvedTime?: string
  revokeDate?: string
  cancelDate?: string
  base?: string          // 省份
  city?: string          // 市
  district?: string      // 区
  regLocation?: string   // 注册地址
  regInstitute?: string  // 登记机关
  regStatus?: string     // 经营状态
  isMicroEnt?: number
  staffNumRange?: string // 人员规模
  socialStaffNum?: number
  businessScope?: string // 经营范围
  phoneNumber?: string   // 联系电话
  email?: string
  websiteList?: string
  tags?: string          // 标签列表
  percentileScore?: number // 评分
  bondNum?: string
  bondName?: string
  usedBondName?: string
  bondType?: string
  logo?: string
  originId?: string
  isSync?: number
  isFill?: number
  tenantId?: number
  status?: number        // 1=启用 0=停用
  createTime?: string
  createBy?: string
  updateBy?: string
}

export function getEnterprisePage(params: {
  keyword?: string
  creditCode?: string
  industry?: string
  regStatus?: string
  hasBind?: number
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

// ==================== 批量操作 + Excel ====================

export function batchUpdateStatus(ids: string[], status: number) {
  return request({ url: '/enterprise/batch/status', method: 'put', data: { ids, status } })
}

export function batchDeleteEnterprise(ids: string[]) {
  return request({ url: '/enterprise/batch', method: 'delete', data: { ids } })
}

export function exportEnterpriseExcel(params?: any) {
  return request({ url: '/enterprise/excel/export', method: 'get', params, responseType: 'blob' })
}

export function downloadTemplateExcel() {
  return request({ url: '/enterprise/excel/template', method: 'get', responseType: 'blob' })
}

export function importEnterpriseExcel(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/enterprise/excel/import', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}

// ==================== V54: 企业行业类型 API ====================

export interface EnterpriseIndustry {
  id?: string
  tenantId?: number
  code?: string         // 行业代码 (GB/T 4754-2017)
  category?: string     // 门类
  categoryBig?: string  // 大类
  categoryMiddle?: string
  categorySmall?: string
  status?: number       // 1=启用 0=停用
  createTime?: string
}

export function getIndustryPage(params: {
  current?: number
  size?: number
  keyword?: string
  category?: string
  status?: number
}) {
  return request({ url: '/enterprise/industry/page', method: 'get', params })
}

export function getIndustryById(id: string) {
  return request({ url: `/enterprise/industry/${id}`, method: 'get' })
}

export function createIndustry(data: Partial<EnterpriseIndustry>) {
  return request({ url: '/enterprise/industry', method: 'post', data })
}

export function updateIndustry(id: string, data: Partial<EnterpriseIndustry>) {
  return request({ url: `/enterprise/industry/${id}`, method: 'put', data })
}

export function deleteIndustry(id: string) {
  return request({ url: `/enterprise/industry/${id}`, method: 'delete' })
}

// ==================== V56: 企业评分规则 API ====================

export interface Rating {
  id?: string
  tenantId?: number
  parkId?: number
  level?: number        // 1=优 2=良 3=中 4=差
  overdueMin?: number
  overdueMax?: number
  debtsMin?: number
  debtsMax?: number
  dateNum?: number
  dateUnit?: string     // day/month/year
  status?: number
}

export function getRatingList(tenantId?: number) {
  return request({ url: '/enterprise/rating/list', method: 'get', params: { tenantId } })
}

export function saveRatingBatch(ratings: Rating[]) {
  return request({ url: '/enterprise/rating/save', method: 'put', data: ratings })
}

export function resetRatingDefault(tenantId?: number) {
  return request({ url: '/enterprise/rating/reset', method: 'post', params: { tenantId } })
}

// ==================== V58: 客户信息 API ====================

export interface CustomerInformation {
  id?: string
  enterpriseId?: number
  code?: string
  customerType?: number        // 1=潜在 2=意向 3=已签约
  area?: number
  settleAddress?: string
  url?: string
  name?: string                // 负责人
  phone?: string
  managerPhone?: string
  email?: string
  registTime?: string
  registMoney?: number
  rentalStandard?: string
  industrialField?: number      // 1-6
  industrialFieldOther?: string
  mainBusiness?: string
  companyStrengths?: string     // 多选 1-10, 逗号分隔
  companyStrengthsOther?: string
  validIntellectualProperty?: number
  inventionPatents?: number
  utilityModelPatent?: number
  industrialDesignPatents?: number
  trademark?: number
  softwareCopyright?: number
  newPlantVariety?: number
  integratedCircuitLayout?: number
  purchaseForeignPatents?: number
  otherPatents?: number
  businessIncome?: number
  lastYearTax?: number
  totalInvestmentAmount?: number
  totalFinancingAmount?: number
  companyDifficulties?: string
  companyDifficultiesOther?: string
  supportingServices?: string
  technicalConsultingServices?: string
  managementServices?: string
  managementServicesOther?: string
  technologyPlatformServices?: string
  technologyPlatformServicesOther?: string
  investmentServices?: string
  investmentServicesOther?: string
  suggest?: string
  structuralLoad?: number
  floorHeight?: number
  capacitance?: number
  supplyAndDrainage?: string
  freshAirSmokeExhaust?: number
  elevatorLength?: number
  elevatorWidth?: number
  elevatorHeight?: number
  elevatorLoad?: number
  status?: number
}

export function getCustomerPage(params: {
  current?: number
  size?: number
  customerType?: number
  status?: number
}) {
  return request({ url: '/enterprise/customer/page', method: 'get', params })
}

export function getCustomerById(id: string) {
  return request({ url: `/enterprise/customer/${id}`, method: 'get' })
}

export function createCustomer(data: Partial<CustomerInformation>) {
  return request({ url: '/enterprise/customer', method: 'post', data })
}

export function updateCustomer(id: string, data: Partial<CustomerInformation>) {
  return request({ url: `/enterprise/customer/${id}`, method: 'put', data })
}

export function deleteCustomer(id: string) {
  return request({ url: `/enterprise/customer/${id}`, method: 'delete' })
}

// ==================== V57: 关注标签 + 内容 API ====================

export interface Focus {
  id?: string
  name?: string
  sorting?: number
  status?: number
}

export interface FocusItem {
  id?: string
  focusId?: number
  name?: string
  sorting?: number
  status?: number
}

export function getFocusPage(params: {
  current?: number
  size?: number
  keyword?: string
  status?: number
}) {
  return request({ url: '/enterprise/focus/page', method: 'get', params })
}

export function createFocus(data: Partial<Focus>) {
  return request({ url: '/enterprise/focus', method: 'post', data })
}

export function updateFocus(id: string, data: Partial<Focus>) {
  return request({ url: `/enterprise/focus/${id}`, method: 'put', data })
}

export function deleteFocus(id: string) {
  return request({ url: `/enterprise/focus/${id}`, method: 'delete' })
}

export function getFocusItemByFocusId(focusId: number | string) {
  return request({ url: `/enterprise/focus-item/by-focus/${focusId}`, method: 'get' })
}

export function createFocusItem(data: Partial<FocusItem>) {
  return request({ url: '/enterprise/focus-item', method: 'post', data })
}

export function updateFocusItem(id: string, data: Partial<FocusItem>) {
  return request({ url: `/enterprise/focus-item/${id}`, method: 'put', data })
}

export function deleteFocusItem(id: string) {
  return request({ url: `/enterprise/focus-item/${id}`, method: 'delete' })
}
