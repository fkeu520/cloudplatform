import request from './request'

export interface Config {
  id?: string
  configName: string
  configKey: string
  configValue: string
  configType?: number
  remark?: string
}

export function getConfigPage(params: {
  keyword?: string
  configType?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/config/page', method: 'get', params })
}

export function getConfigById(id: string) {
  return request({ url: `/config/${id}`, method: 'get' })
}

export function getConfigByKey(configKey: string) {
  return request({ url: `/config/key/${configKey}`, method: 'get' })
}

export function createConfig(data: Config) {
  return request({ url: '/config', method: 'post', data })
}

export function updateConfig(id: string, data: Config) {
  return request({ url: `/config/${id}`, method: 'put', data })
}

export function deleteConfig(id: string) {
  return request({ url: `/config/${id}`, method: 'delete' })
}
