<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="用户名/昵称" clearable />
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
          <el-button type="success" @click="handleAdd">新增运营管理员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="statusDesc" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.statusDesc === '启用' ? 'success' : 'danger'">
              {{ scope.row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleMenuAuth(scope.row)">菜单授权</el-button>
            <el-button type="success" link size="small" @click="handleResetPwd(scope.row)">重置密码</el-button>
            <el-button :type="scope.row.statusDesc === '启用' ? 'danger' : 'success'" link size="small"
              @click="handleToggleStatus(scope.row)">
              {{ scope.row.statusDesc === '启用' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(scope.row)" v-if="scope.row.id !== '1'">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="formData.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formData.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" title="菜单授权" width="450px">
      <div v-if="menuLoading" style="text-align:center;padding:30px">加载中...</div>
      <el-tree
        v-else
        ref="menuTreeRef"
        :data="menuTreeData"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        check-strictly
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMenuSubmit">保存授权</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePwdSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { page, getById, create, update, remove, toggleStatus, resetPassword, getMenuIds, assignMenus } from '@/api/ops-user'
import { getMenuTree } from '@/api/menu'
import type { OpsUser } from '@/api/ops-user'
import type { Menu } from '@/api/menu'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<string | null>(null)
const formRef = ref()
const formData = reactive({
  username: '',
  password: '',
  nickname: '',
  mobile: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const menuDialogVisible = ref(false)
const menuLoading = ref(false)
const menuTreeData = ref<Menu[]>([])
const menuTreeRef = ref()
const menuUserId = ref<string | null>(null)

const pwdDialogVisible = ref(false)
const pwdForm = reactive({ newPassword: '' })
const pwdUserId = ref<string | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res: any = await page({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      tableData.value = res.data.records || res.data?.records || []
      total.value = res.data.total || res.data?.total || 0
    } else {
      tableData.value = res.records || []
      total.value = res.total || 0
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
  formData.username = ''
  formData.password = ''
  formData.nickname = ''
  formData.mobile = ''
  formData.email = ''
  currentId.value = null
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增运营管理员'
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑运营管理员'
  currentId.value = row.id
  const res: any = await getById(row.id)
  const user = res.code === 200 ? res.data : res
  formData.username = user.username || ''
  formData.nickname = user.nickname || ''
  formData.mobile = user.mobile || ''
  formData.email = user.email || ''
  dialogVisible.value = true
}

async function handleMenuAuth(row: any) {
  menuUserId.value = row.id
  menuDialogVisible.value = true
  menuLoading.value = true
  try {
    const res: any = await getMenuTree()
    const menus = res.code === 200 ? res.data : res
    menuTreeData.value = Array.isArray(menus) ? menus : []
    const menuRes: any = await getMenuIds(row.id)
    const menuIds = menuRes.code === 200 ? menuRes.data : []
    if (menuTreeRef.value && Array.isArray(menuIds)) {
      menuTreeRef.value.setCheckedKeys(menuIds)
    }
  } finally {
    menuLoading.value = false
  }
}

async function handleMenuSubmit() {
  if (!menuUserId.value) return
  const checkedIds = menuTreeRef.value?.getCheckedKeys() || []
  await assignMenus(menuUserId.value, checkedIds)
  ElMessage.success('授权成功')
  menuDialogVisible.value = false
}

function handleResetPwd(row: any) {
  pwdUserId.value = row.id
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}

async function handlePwdSubmit() {
  if (!pwdForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (pwdUserId.value) {
    await resetPassword(pwdUserId.value, pwdForm.newPassword)
    ElMessage.success('密码重置成功')
    pwdDialogVisible.value = false
  }
}

async function handleToggleStatus(row: any) {
  try {
    await ElMessageBox.confirm(`确定${row.statusDesc === '启用' ? '禁用' : '启用'}该用户？`, '提示', { type: 'warning' })
    const res: any = await toggleStatus(row.id)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadData()
    }
  } catch { /* cancelled */ }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该运营管理员？', '提示', { type: 'warning' })
    await remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handleSubmit() {
  await formRef.value.validate()
  if (isEdit.value && currentId.value) {
    const res: any = await update(currentId.value, formData)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadData()
    }
  } else {
    const res: any = await create(formData)
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
.page-container { padding: 20px; }
.search-card { margin-bottom: 20px; }
.table-card { margin-bottom: 20px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
