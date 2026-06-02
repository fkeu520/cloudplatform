import request from './request'

export function getAuthorizedAppIds(tenantId: number) { return request.get(`/tenant-app/${tenantId}/appIds`) }
export function authorizeApps(tenantId: number, appIds: number[]) { return request.post(`/tenant-app/${tenantId}/authorize`, appIds) }
