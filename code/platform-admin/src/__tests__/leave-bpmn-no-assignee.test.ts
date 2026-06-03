import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * Leave.vue 硬编码 BPMN 部署验证
 *
 * bug 背景:
 *   Leave.vue 的 LEAVE_BPMN 常量之前含 flowable:assignee="admin"
 *   - admin 用户在某些租户下不存在
 *   - 部署后 Flowable 任务分派给 admin, 但租户下没这个用户
 *   - 用户任务永远不会被处理
 *
 * 修复:
 *   移除所有 flowable:assignee 属性, 任务分派完全靠 candidateUsers
 */

describe('Leave.vue LEAVE_BPMN', () => {
  it('硬编码 LEAVE_BPMN 不应包含 flowable:assignee 属性', () => {
    const filePath = resolve(__dirname, '../views/workflow/Leave.vue')
    const content = readFileSync(filePath, 'utf-8')

    // 提取 LEAVE_BPMN 模板字符串内容 (从 const LEAVE_BPMN = `...` 到 `)
    const match = content.match(/const LEAVE_BPMN = `([\s\S]*?)`/)
    expect(match, 'LEAVE_BPMN constant should be defined').toBeTruthy()

    const bpmn = match![1]
    expect(bpmn).not.toContain('flowable:assignee',
      'LEAVE_BPMN 不应包含 flowable:assignee - 任务应通过 candidateUsers 分派')
    expect(bpmn).not.toContain('admin',
      'LEAVE_BPMN 不应硬编码 admin - 跨租户无法使用')
  })

  it('硬编码 LEAVE_BPMN 应保留 candidateUsers (分派给候选人池)', () => {
    const filePath = resolve(__dirname, '../views/workflow/Leave.vue')
    const content = readFileSync(filePath, 'utf-8')

    const match = content.match(/const LEAVE_BPMN = `([\s\S]*?)`/)
    expect(match).toBeTruthy()

    const bpmn = match![1]
    expect(bpmn).toContain('<flowable:candidateUsers>',
      'LEAVE_BPMN 应保留 candidateUsers 用于候选池')
  })

  it('LEAVE_BPMN 的 userTask 应没有指定办理人, 任务依赖候选人池', () => {
    const filePath = resolve(__dirname, '../views/workflow/Leave.vue')
    const content = readFileSync(filePath, 'utf-8')

    const match = content.match(/const LEAVE_BPMN = `([\s\S]*?)`/)
    const bpmn = match![1]

    // 提取所有 userTask
    const userTaskMatches = bpmn.match(/<bpmn:userTask[^>]*?>/g) || []
    expect(userTaskMatches.length).toBeGreaterThan(0)

    for (const userTask of userTaskMatches) {
      expect(userTask).not.toMatch(/flowable:assignee=/,
        `userTask 不应包含 flowable:assignee: ${userTask.trim()}`)
    }
  })
})

describe('ProcessDesigner/BpmnDesigner 不应输出 flowable:assignee', () => {
  it('ProcessDesigner.vue 不应出现 "flowable:assignee" 字符串', () => {
    const filePath = resolve(__dirname, '../components/ProcessDesigner.vue')
    const content = readFileSync(filePath, 'utf-8')
    expect(content).not.toContain('flowable:assignee',
      'ProcessDesigner 不应生成 flowable:assignee 属性')
  })

  it('BpmnDesigner.vue 不应出现 "flowable:assignee" 字符串', () => {
    const filePath = resolve(__dirname, '../components/BpmnDesigner.vue')
    const content = readFileSync(filePath, 'utf-8')
    expect(content).not.toContain('flowable:assignee',
      'BpmnDesigner 不应生成 flowable:assignee 属性')
  })
})
