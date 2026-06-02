import request from './request'

export function loginLogPage(params: any) { return request.get('/audit/login-log/page', { params }) }
export function operLogPage(params: any) { return request.get('/audit/oper-log/page', { params }) }
export function searchElk(params: any) { return request.get('/audit/elk/search', { params }) }
