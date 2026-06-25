import request from './request'

export function getAuthorizedAppIds(tenantId: string) { return request.get(`/tenant-app/${tenantId}/appIds`) }
export function authorizeApps(tenantId: string, appIds: number[]) { return request.post(`/tenant-app/${tenantId}/authorize`, appIds) }
