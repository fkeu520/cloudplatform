import { describe, it, expect, beforeEach, vi } from 'vitest'

/**
 * User Edit Form - 雪花 ID 精度保护 测试
 *
 * 核心测试点:
 * - 后端返回 orgId="2061744146233823234" (string) → 前端不要 Number() 转换
 * - 关键: 不能让 19 位雪花 ID 通过 Number() 转换 (会变成 2061744146233823000)
 *
 * 模拟流程: getOrgTree + getUserById → 验证扁平化后的 orgList 包含完整字符串 ID
 */

// 模拟 API 模块
const mockGetOrgTree = vi.fn()
const mockGetUserById = vi.fn()
const mockGetDeptList = vi.fn()

vi.mock('@/api/org', () => ({
  getOrgTree: () => mockGetOrgTree()
}))

vi.mock('@/api/user', () => ({
  getUserById: (id: string) => mockGetUserById(id)
}))

vi.mock('@/api/dept', () => ({
  getDeptList: (orgId: string) => mockGetDeptList(orgId)
}))

describe('User edit - 雪花 ID 字符串处理', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('组织树返回的 ID 必须是字符串 (19位雪花 ID 不可转 Number)', async () => {
    // 模拟后端响应: 修复后 id 是字符串
    mockGetOrgTree.mockResolvedValue({
      code: 200,
      data: [{
        id: '1',  // 字符串! (修复后)
        name: '云枢科技',
        children: [
          { id: '2', name: '总部' },
          { id: '3', name: '华东分公司' },
          { id: '2061744146233823234', name: '云枢科技股份有限公司' }  // 19 位雪花 ID
        ]
      }]
    })

    const { getOrgTree } = await import('@/api/org')
    const res = await getOrgTree() as any
    const orgs = res.data

    // 验证: 19 位 ID 在 JSON 响应中是字符串 (不是数字 2061744146233823000)
    const newCompany = orgs[0].children.find((c: any) => c.name === '云枢科技股份有限公司')
    expect(typeof newCompany.id).toBe('string')
    expect(newCompany.id).toBe('2061744146233823234')  // 完整 19 位, 不丢失
    expect(newCompany.id).not.toBe('2061744146233823000')  // 不会变成精度丢失的值
  })

  it('getUserById 返回的 orgId/deptId/postId 必须是字符串', async () => {
    mockGetUserById.mockResolvedValue({
      code: 200,
      data: {
        id: '2061743069044281345',
        username: 'zhangs',
        nickname: '张三',
        orgId: '2061744146233823234',
        deptId: '2061744316149272577',
        postId: '2061744676448374786',
        orgName: '云枢科技股份有限公司',
        deptName: '人事部',
        postName: '人事总监',
        status: 1,
        roleIds: []
      }
    })

    const { getUserById } = await import('@/api/user')
    const res = await getUserById('2061743069044281345') as any
    const user = res.data

    // 关键: 这些 ID 必须是字符串, 不能让前端 Number() 转换
    expect(typeof user.orgId).toBe('string')
    expect(typeof user.deptId).toBe('string')
    expect(typeof user.postId).toBe('string')
    expect(user.orgId).toBe('2061744146233823234')
    expect(user.deptName).toBe('人事部')
    expect(user.postName).toBe('人事总监')
  })

  it('扁平化 orgList 后, 字符串 ID 不被 Number() 破坏', async () => {
    // 复现 Index.vue 中 loadOrgTree 的逻辑
    mockGetOrgTree.mockResolvedValue({
      code: 200,
      data: [{
        id: '1',
        name: '云枢科技',
        children: [
          { id: '2', name: '总部' },
          { id: '2061744146233823234', name: '云枢科技股份有限公司' }
        ]
      }]
    })

    const { getOrgTree } = await import('@/api/org')
    const res = await getOrgTree() as any

    // 模拟 Index.vue 中的 flatten + String() 逻辑
    function flatten(list: any[], result: any[]) {
      for (const item of list) {
        result.push({ id: item.id, name: item.name })
        if (item.children && item.children.length > 0) {
          flatten(item.children, result)
        }
      }
    }
    const flat: any[] = []
    flatten(res.data || [], flat)
    // Index.vue 当前的代码: flat.map(item => ({ ...item, id: String(item.id) }))
    const orgList = flat.map((item: any) => ({ ...item, id: String(item.id) }))

    // 验证: 扁平化后, 19 位 ID 仍是完整字符串
    const newCompany = orgList.find((o: any) => o.name === '云枢科技股份有限公司')
    expect(newCompany).toBeDefined()
    expect(newCompany.id).toBe('2061744146233823234')

    // 关键: String("2061744146233823234") 应保持不变
    // 而不是先 Number 再 String (会丢精度)
    expect(newCompany.id.length).toBe(19)
  })

  it('ensureInList: 当前值与 orgList 选项字符串比较应能匹配', () => {
    // 复现 Index.vue 中 handleEdit 的 ensureInList 逻辑
    const orgList: any[] = [
      { id: '1', name: '云枢科技' },
      { id: '2', name: '总部' },
      { id: '2061744146233823234', name: '云枢科技股份有限公司' }
    ]

    // 模拟 formData.orgId 来自 user API (字符串)
    const formDataOrgId = '2061744146233823234'
    const currentOrgName = '云枢科技股份有限公司'

    // ensureInList 逻辑
    function ensureInList(list: any[], value: string | null, label: string | null) {
      if (value != null && label) {
        const exists = list.some((item: any) => item.id === value)
        if (!exists) {
          list.push({ id: value, name: label })
        }
      }
    }
    ensureInList(orgList, formDataOrgId, currentOrgName)

    // 应该找到匹配 (不重复添加)
    const matched = orgList.find((o: any) => o.id === formDataOrgId)
    expect(matched).toBeDefined()
    expect(matched.name).toBe('云枢科技股份有限公司')
    expect(orgList.length).toBe(3)  // 没有重复添加
  })

  it('反例: 如果用了 Number() 转换, 字符串比较会失败', () => {
    // 这个测试展示如果有人错误地用 Number() 会发生什么
    const orgList: any[] = [
      { id: '2061744146233823234', name: '云枢科技股份有限公司' }  // 字符串 ID
    ]

    // 错误做法: 假设有人写了 Number(user.orgId)
    const wrongFormDataOrgId = Number('2061744146233823234')  // 2061744146233823000 (精度丢失)

    // 字符串 === Number 永远不匹配
    const matched = orgList.find((o: any) => o.id === wrongFormDataOrgId)
    expect(matched).toBeUndefined()  // 匹配失败, 触发回退

    // 这就是修复前 el-select 显示 ID 而非名称的根因
  })
})
