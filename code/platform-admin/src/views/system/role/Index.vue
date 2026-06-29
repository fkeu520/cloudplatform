<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="角色编码/名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd" v-permission="'system:role:add'">新增角色</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="数据范围" width="160">
          <template #default="scope">
            <el-tag :type="scope.row.dataScope === 1 ? 'success' : 'warning'" size="small">
              {{ DATA_SCOPE_LABELS[scope.row.dataScope ?? 1] || '全部' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <TableActions :buttons="[
              { label: '编辑', props: { type: 'primary' }, handler: () => handleEdit(scope.row), permission: 'system:role:edit' },
              { label: '权限', props: { type: 'warning' }, handler: () => handleAuth(scope.row), permission: 'system:role:edit' },
              { label: '删除', props: { type: 'danger' }, handler: () => handleDelete(scope.row), permission: 'system:role:del' }
            ]" />
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="formData.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option
              v-for="opt in DATA_SCOPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <div class="form-tip">
            控制角色下用户能看到的数据范围；admin (dataScope=1) 不受限制
          </div>
        </el-form-item>
        <el-form-item
          v-if="formData.dataScope === 5"
          label="自定义部门"
          prop="customDeptIds"
        >
          <el-cascader
            v-model="customDeptIdList"
            :options="deptTree"
            :props="{ checkStrictly: true, value: 'id', label: 'name', multiple: true, emitPath: false }"
            placeholder="选择自定义部门 (dataScope=5 时必填)"
            collapse-tags
            collapse-tags-tooltip
            clearable
            style="width: 100%"
            @change="handleCustomDeptChange"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sort" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 (参考 Linear + Notion: 默认收缩, max-height + 滚动, 搜索 + 计数 + 全选/清空) -->
    <el-dialog v-model="authDialogVisible" title="菜单权限配置" width="560px" align-center>
      <!-- 头部: 角色信息 + 选中计数 -->
      <div class="auth-dialog-header">
        <div class="auth-role-info">
          <el-icon><Key /></el-icon>
          <span class="auth-role-name">{{ currentRoleName || '角色' }}</span>
        </div>
        <div class="auth-summary">
          <span class="auth-counter">已选 <strong>{{ selectedMenuIds.length }}</strong> 项</span>
          <span class="auth-divider">/</span>
          <span class="auth-total">共 {{ totalMenuCount }} 项</span>
        </div>
      </div>

      <!-- 搜索 + 工具栏 -->
      <div class="auth-toolbar">
        <el-input
          v-model="authSearchKeyword"
          placeholder="搜索菜单 / 权限标识"
          clearable
          size="default"
          class="auth-search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="auth-actions">
          <el-button text :icon="Expand" size="small" @click="expandAllMenu">展开</el-button>
          <el-button text :icon="Fold" size="small" @click="collapseAllMenu">收起</el-button>
          <el-button text size="small" @click="selectAllMenu">全选</el-button>
          <el-button text size="small" @click="clearAllMenu">清空</el-button>
        </div>
      </div>

      <!-- 菜单树 (max-height + 内部滚动, 只展开第一层) -->
      <div class="auth-tree-wrapper">
        <el-tree
          v-if="filteredMenuTree.length > 0"
          ref="menuTreeRef"
          :data="filteredMenuTree"
          show-checkbox
          node-key="id"
          :default-expanded-keys="defaultExpandedMenuIds"
          :check-strictly="true"
          :props="{ label: 'name', children: 'children' }"
          :filter-node-method="filterMenuNode"
          class="auth-tree"
          empty-text="未找到匹配菜单"
        >
          <template #default="{ node, data }">
            <div class="auth-tree-node">
              <span class="auth-tree-label">{{ node.label }}</span>
              <el-tag v-if="data.type === 2 && data.perms" size="small" type="info" effect="plain" class="auth-tree-perm">
                {{ data.perms }}
              </el-tag>
              <el-tag v-else-if="data.type === 1" size="small" type="success" effect="plain" class="auth-tree-type">
                菜单
              </el-tag>
            </div>
          </template>
        </el-tree>
        <el-empty v-else description="加载中..." :image-size="60" />
      </div>

      <template #footer>
        <el-button @click="authDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="authSubmitting" @click="handleAuthSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Search, Expand, Fold } from '@element-plus/icons-vue'
import TableActions from '@/components/TableActions.vue'
import {
  getRolePage,
  getRoleById,
  createRole,
  updateRole,
  deleteRole,
  getRoleMenuIds,
  assignRoleMenus
} from '@/api/role'
import { getMenuTree, getUserPermissions } from '@/api/menu'
import { getDeptTree } from '@/api/dept'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/api/role'

// M5 P0-2 数据范围 (dataScope) 选项
const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部 (不受限)' },
  { value: 2, label: '本部门' },
  { value: 3, label: '本部门及下级' },
  { value: 4, label: '本人' },
  { value: 5, label: '自定义 (指定部门)' }
]

const DATA_SCOPE_LABELS: Record<number, string> = Object.fromEntries(
  DATA_SCOPE_OPTIONS.map(o => [o.value, o.label])
)

const loading = ref(false)
const tableData = ref<Role[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const userStore = useUserStore()

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

const dialogVisible = ref(false)
const authDialogVisible = ref(false)
const authSubmitting = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()
const menuTreeRef = ref()
const menuTreeData = ref<any[]>([])
const deptTree = ref<any[]>([])
const customDeptIdList = ref<number[]>([])

// 权限弹窗辅助状态 (用于交互改进)
const authSearchKeyword = ref('')
const currentRoleName = ref('')
// 默认只展开第一层菜单 (避免一进去全展开, 列表太长)
const defaultExpandedMenuIds = ref<number[]>([])

const selectedMenuIds = computed<number[]>(() => {
  return menuTreeRef.value?.getCheckedKeys() ?? []
})

// 总菜单数 (递归统计, 用于 "共 N 项")
const totalMenuCount = computed<number>(() => {
  const count = (nodes: any[]): number => {
    return nodes.reduce((sum, n) => sum + 1 + (n.children ? count(n.children) : 0), 0)
  }
  return count(menuTreeData.value)
})

// 过滤后的菜单树 (用于搜索)
const filteredMenuTree = computed<any[]>(() => {
  if (!authSearchKeyword.value.trim()) return menuTreeData.value
  const kw = authSearchKeyword.value.trim().toLowerCase()
  const filter = (nodes: any[]): any[] => {
    const result: any[] = []
    for (const n of nodes) {
      const matchSelf = n.name?.toLowerCase().includes(kw) || n.perms?.toLowerCase().includes(kw)
      const children = n.children ? filter(n.children) : []
      if (matchSelf || children.length > 0) {
        result.push({ ...n, children: children.length > 0 ? children : n.children })
      }
    }
    return result
  }
  return filter(menuTreeData.value)
})

// 节点过滤方法 (el-tree 的 filter-node-method, 仅当树为空搜索结果时由 el-tree 自己过滤)
const filterMenuNode = (value: string, data: any): boolean => {
  if (!value) return true
  const kw = value.toLowerCase()
  return data.name?.toLowerCase().includes(kw) || data.perms?.toLowerCase().includes(kw)
}

const formData = reactive<Partial<Role>>({
  code: '',
  name: '',
  sort: 0,
  remark: '',
  status: 1,
  dataScope: 1,
  customDeptIds: ''
})

const rules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }],
  customDeptIds: [{
    validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
      // dataScope=5 时 customDeptIds 必填
      if (formData.dataScope === 5 && !value) {
        callback(new Error('dataScope=5 (自定义) 时, customDeptIds 必填'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRolePage({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  loadData()
}

function handlePageChange(val: number) {
  pageNum.value = val
  loadData()
}

function resetForm() {
  formData.code = ''
  formData.name = ''
  formData.sort = 0
  formData.remark = ''
  formData.status = 1
  formData.dataScope = 1
  formData.customDeptIds = ''
  customDeptIdList.value = []
  currentId.value = null
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增角色'
  // 加载部门树 (dataScope=5 时需要)
  loadDeptTree()
  dialogVisible.value = true
}

async function handleEdit(row: Role) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  currentId.value = row.id!
  // 编辑时加载部门树 (dataScope=5 时用)
  loadDeptTree()
  try {
    const res: any = await getRoleById(row.id!)
    if (res.code === 200) {
      const role = res.data
      formData.code = role.code
      formData.name = role.name
      formData.sort = role.sort
      formData.remark = role.remark
      formData.status = role.status
      formData.dataScope = role.dataScope ?? 1
      formData.customDeptIds = role.customDeptIds ?? ''
      if (formData.customDeptIds) {
        customDeptIdList.value = formData.customDeptIds
          .split(',')
          .map(s => Number(s.trim()))
          .filter(n => !isNaN(n))
      }
    }
  } catch (e) {
    ElMessage.error('获取角色详情失败')
    return
  }
  dialogVisible.value = true
}

async function loadDeptTree() {
  try {
    const res: any = await getDeptTree()
    if (res.code === 200) {
      deptTree.value = res.data || []
    }
  } catch (e) {
    console.warn('加载部门树失败:', e)
  }
}

function handleCustomDeptChange(value: number | number[]) {
  // el-cascader 多个选中时, value 是数组; emitPath: false 已是数字
  const list = Array.isArray(value) ? value : (value !== null && value !== undefined ? [value] : [])
  formData.customDeptIds = list.join(',')
}

async function handleAuth(row: Role) {
  currentId.value = row.id!
  currentRoleName.value = row.name || '角色'
  authSearchKeyword.value = ''
  const [menuRes, roleMenuRes]: [any, any] = await Promise.all([
    getMenuTree(),
    getRoleMenuIds(row.id!)
  ])
  if (menuRes.code === 200) {
    menuTreeData.value = menuRes.data
    // 默认只展开第一层 (根级菜单, 即 parent_id=0)
    defaultExpandedMenuIds.value = (menuRes.data || [])
      .filter((m: any) => !m.parentId || m.parentId === 0)
      .map((m: any) => m.id)
  }
  authDialogVisible.value = true
  if (roleMenuRes.code === 200) {
    setTimeout(() => {
      if (menuTreeRef.value) {
        menuTreeRef.value.setCheckedKeys(roleMenuRes.data || [])
      }
    }, 300)
  }
}

// ============ 权限弹窗辅助操作 ============
function expandAllMenu() {
  const allIds: number[] = []
  const walk = (nodes: any[]) => {
    for (const n of nodes) {
      allIds.push(n.id)
      if (n.children) walk(n.children)
    }
  }
  walk(menuTreeData.value)
  allIds.forEach(id => menuTreeRef.value?.store?.nodesMap[id]?.expand())
}

function collapseAllMenu() {
  const allNodes = menuTreeRef.value?.store?.nodesMap
  if (!allNodes) return
  Object.values(allNodes).forEach((n: any) => n.collapse())
  // 展开默认第一层
  setTimeout(() => {
    defaultExpandedMenuIds.value.forEach((id: number) => {
      allNodes[id]?.expand()
    })
  }, 50)
}

function selectAllMenu() {
  const allIds: number[] = []
  const walk = (nodes: any[]) => {
    for (const n of nodes) {
      allIds.push(n.id)
      if (n.children) walk(n.children)
    }
  }
  walk(filteredMenuTree.value)
  menuTreeRef.value?.setCheckedKeys(allIds, false)
}

function clearAllMenu() {
  menuTreeRef.value?.setCheckedKeys([])
}

async function handleAuthSubmit() {
  const checkedKeys = menuTreeRef.value?.getCheckedKeys()
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys()
  const menuIds = [...checkedKeys, ...halfCheckedKeys]
  authSubmitting.value = true
  try {
    const res: any = await assignRoleMenus(currentId.value!, menuIds)
    if (res.code === 200) {
      ElMessage.success('权限分配成功')
      authDialogVisible.value = false

      const permRes = await getUserPermissions()
      if (permRes.data) {
        userStore.setPermissions(permRes.data)
      }
    }
  } finally {
    authSubmitting.value = false
  }
}

async function handleDelete(row: Role) {
  try {
    await ElMessageBox.confirm('确定删除该角色？', '提示', { type: 'warning' })
    const res: any = await deleteRole(row.id!)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // cancelled
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  if (isEdit.value && currentId.value) {
    const res: any = await updateRole(currentId.value, formData as Role)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadData()
    }
  } else {
    const res: any = await createRole(formData as Role)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadData()
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.search-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

/* ============ 权限配置弹窗 (Linear + Notion 风格) ============ */
.auth-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 16px;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.auth-role-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
}
.auth-role-info .el-icon {
  color: var(--el-color-primary);
  font-size: 18px;
}
.auth-role-name {
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.auth-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}
.auth-counter strong {
  color: var(--el-color-primary);
  font-weight: 600;
  font-size: 13px;
  margin: 0 2px;
}
.auth-divider {
  color: var(--el-text-color-placeholder);
}
.auth-total {
  color: var(--el-text-color-secondary);
}

.auth-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.auth-search {
  flex: 1;
  max-width: 280px;
}
.auth-actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
}

.auth-tree-wrapper {
  max-height: 480px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px 4px;
  background: var(--el-fill-color-blank);
  transition: border-color 0.2s cubic-bezier(0.2, 0, 0, 1);
}
.auth-tree-wrapper:focus-within {
  border-color: var(--el-color-primary-light-5);
}
.auth-tree {
  background: transparent;
}
.auth-tree :deep(.el-tree-node__content) {
  height: 32px;
  border-radius: 4px;
  margin: 1px 0;
  transition: background-color 0.15s cubic-bezier(0.2, 0, 0, 1);
}
.auth-tree :deep(.el-tree-node__content:hover) {
  background-color: var(--el-fill-color-light);
}
.auth-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
}
.auth-tree :deep(.el-checkbox) {
  margin-right: 6px;
}

.auth-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  padding-right: 8px;
}
.auth-tree-label {
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.auth-tree-perm,
.auth-tree-type {
  margin-left: auto;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
}

/* 滚动条美化 (Webkit) */
.auth-tree-wrapper::-webkit-scrollbar {
  width: 6px;
}
.auth-tree-wrapper::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 3px;
}
.auth-tree-wrapper::-webkit-scrollbar-thumb:hover {
  background: var(--el-text-color-placeholder);
}
</style>
