import request from './request'

export interface PlanUse {
  id?: string
  planUseCode?: string
  planUseName?: string
  color?: string
  status?: number
  parkId?: string
  tenantId?: string
  createTime?: string
}

export function getPlanUsePage(params: {
  keyword?: string
  parkId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/plan-use/page', method: 'get', params })
}

export function getPlanUseById(id: string) {
  return request({ url: `/plan-use/${id}`, method: 'get' })
}

export function createPlanUse(data: any) {
  return request({ url: '/plan-use', method: 'post', data })
}

export function updatePlanUse(id: string, data: any) {
  return request({ url: `/plan-use/${id}`, method: 'put', data })
}

export function deletePlanUse(id: string) {
  return request({ url: `/plan-use/${id}`, method: 'delete' })
}