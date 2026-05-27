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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" />
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
import { useUserStore } from '@/stores/user'
import type { Role } from '@/api/role'

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

const formData = reactive<Partial<Role>>({
  code: '',
  name: '',
  sort: 0,
  remark: '',
  status: 1
})

const rules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
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
  currentId.value = null
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

async function handleEdit(row: Role) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  currentId.value = row.id!
  const res: any = await getRoleById(row.id!)
  if (res.code === 200) {
    const role = res.data
    formData.code = role.code
    formData.name = role.name
    formData.sort = role.sort
    formData.remark = role.remark
    formData.status = role.status
  }
  dialogVisible.value = true
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
</style>
