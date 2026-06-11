import request from './request'

export function getDefinitionPage(params: {
  name?: string
  category?: string
  suspensionState?: number
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/workflow/definition/page', { params })
}

export function getDefinitionById(definitionId: string) {
  return request.get(`/workflow/definition/${definitionId}`)
}

export function deployDefinition(data: { processName?: string; bpmnXml: string }) {
  return request.post('/workflow/definition/deploy', data)
}

export function suspendDefinition(definitionId: string) {
  return request.post(`/workflow/definition/${definitionId}/suspend`)
}

export function activateDefinition(definitionId: string) {
  return request.post(`/workflow/definition/${definitionId}/activate`)
}

export function deleteDefinition(definitionId: string) {
  return request.delete(`/workflow/definition/${definitionId}`)
}

export function getDefinitionByKey(key: string) {
  return request.get(`/workflow/definition/key/${key}`)
}

export function getDefinitionXml(definitionId: string) {
  return request.get(`/workflow/definition/${definitionId}/xml`)
}

export function startInstance(data: {
  processDefinitionKey: string
  businessKey?: string
  variables?: Record<string, any>
  userId?: string
}) {
  return request.post('/workflow/instance/start', data)
}

export function getInstancePage(params: {
  processDefinitionKey?: string
  name?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/workflow/instance/page', { params })
}

export function getInstanceById(processInstanceId: string) {
  return request.get(`/workflow/instance/${processInstanceId}`)
}

export function deleteInstance(processInstanceId: string, reason?: string) {
  return request.delete(`/workflow/instance/${processInstanceId}`, { params: { reason } })
}

export function getInstanceTimeline(processInstanceId: string) {
  return request.get(`/workflow/instance/${processInstanceId}/timeline`)
}

export function getTodoTasks(params: {
  userId: string
  processName?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/workflow/task/todo', { params })
}

export function getDoneTasks(params: {
  userId: string
  processName?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/workflow/task/done', { params })
}

export function getTaskById(taskId: string) {
  return request.get(`/workflow/task/${taskId}`)
}

export function completeTask(taskId: string, data?: Record<string, any>, comment?: string, userId?: string) {
  return request.post(`/workflow/task/${taskId}/complete`, data || {}, { params: { comment, userId } })
}

export function rejectTask(taskId: string, comment?: string, userId?: string) {
  return request.post(`/workflow/task/${taskId}/reject`, {}, { params: { comment, userId } })
}

export function transferTask(taskId: string, newAssignee: string, userId: string) {
  return request.post(`/workflow/task/${taskId}/transfer`, {}, { params: { newAssignee, userId } })
}

export function claimTask(taskId: string, userId: string) {
  return request.post(`/workflow/task/${taskId}/claim`, {}, { params: { userId } })
}

export function unclaimTask(taskId: string) {
  return request.post(`/workflow/task/${taskId}/unclaim`)
}

export function getUnreadNotifies(userId: string) {
  return request.get('/workflow/notify/unread', { params: { userId } })
}

export function markNotifyRead(taskId: string) {
  return request.post(`/workflow/notify/read/${taskId}`)
}

export function markAllNotifyRead(userId: string) {
  return request.post('/workflow/notify/read-all', null, { params: { userId } })
}
