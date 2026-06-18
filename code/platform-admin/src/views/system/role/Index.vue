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
        <el-table-column label="操作" width="auto" fixed="right">
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

    <!-- 权限配置弹窗 -->
    <el-dialog v-model="authDialogVisible" title="菜单权限配置" width="400px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        show-checkbox
        node-key="id"
        :default-expand-all="true"
        :check-strictly="true"
        :props="{ label: 'name', children: 'children' }"
      />
      <template #footer>
        <el-button @click="authDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAuthSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()
const menuTreeRef = ref()
const menuTreeData = ref<any[]>([])
const deptTree = ref<any[]>([])
const customDeptIdList = ref<number[]>([])

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
  const [menuRes, roleMenuRes]: [any, any] = await Promise.all([
    getMenuTree(),
    getRoleMenuIds(row.id!)
  ])
  if (menuRes.code === 200) {
    menuTreeData.value = menuRes.data
  }
  authDialogVisible.value = true
  if (roleMenuRes.code === 200) {
    setTimeout(() => {
      if (menuTreeRef.value) {
        menuTreeRef.value.setCheckedKeys([])
        menuTreeRef.value.setCheckedKeys(roleMenuRes.data)
      }
    }, 300)
  }
}

async function handleAuthSubmit() {
  const checkedKeys = menuTreeRef.value?.getCheckedKeys()
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys()
  const menuIds = [...checkedKeys, ...halfCheckedKeys]
  const res: any = await assignRoleMenus(currentId.value!, menuIds)
  if (res.code === 200) {
    ElMessage.success('权限分配成功')
    authDialogVisible.value = false
    
    const permRes = await getUserPermissions()
    if (permRes.data) {
      userStore.setPermissions(permRes.data)
    }
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
</style>
