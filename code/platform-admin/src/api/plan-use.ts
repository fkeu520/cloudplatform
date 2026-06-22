import request from './request'

export interface PlanUse {
  id?: number
  planUseCode?: string
  planUseName?: string
  color?: string
  status?: number
  parkId?: number
  tenantId?: number
  createTime?: string
}

export function getPlanUsePage(params: {
  keyword?: string
  parkId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/plan-use/page', method: 'get', params })
}

export function getPlanUseById(id: number) {
  return request({ url: `/plan-use/${id}`, method: 'get' })
}

export function createPlanUse(data: any) {
  return request({ url: '/plan-use', method: 'post', data })
}

export function updatePlanUse(id: number, data: any) {
  return request({ url: `/plan-use/${id}`, method: 'put', data })
}

export function deletePlanUse(id: number) {
  return request({ url: `/plan-use/${id}`, method: 'delete' })
}