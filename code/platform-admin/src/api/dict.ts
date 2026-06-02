import request from './request'

export interface DictType {
  id?: string
  dictName: string
  dictType: string
  status?: number
  remark?: string
}

export interface DictData {
  id?: string
  dictType: string
  dictLabel: string
  dictValue: string
  dictSort?: number
  status?: number
  cssClass?: string
  remark?: string
}

export function getDictTypePage(params: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/dict/type/page', method: 'get', params })
}

export function getDictTypeList() {
  return request({ url: '/dict/type/list', method: 'get' })
}

export function getDictTypeById(id: string) {
  return request({ url: `/dict/type/${id}`, method: 'get' })
}

export function createDictType(data: DictType) {
  return request({ url: '/dict/type', method: 'post', data })
}

export function updateDictType(id: string, data: DictType) {
  return request({ url: `/dict/type/${id}`, method: 'put', data })
}

export function deleteDictType(id: string) {
  return request({ url: `/dict/type/${id}`, method: 'delete' })
}

export function getDictDataPage(params: {
  dictType: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/dict/data/page', method: 'get', params })
}

export function getDictDataByType(dictType: string) {
  return request({ url: `/dict/data/type/${dictType}`, method: 'get' })
}

export function getDictDataById(id: string) {
  return request({ url: `/dict/data/${id}`, method: 'get' })
}

export function createDictData(data: DictData) {
  return request({ url: '/dict/data', method: 'post', data })
}

export function updateDictData(id: string, data: DictData) {
  return request({ url: `/dict/data/${id}`, method: 'put', data })
}

export function deleteDictData(id: string) {
  return request({ url: `/dict/data/${id}`, method: 'delete' })
}
