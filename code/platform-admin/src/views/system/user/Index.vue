<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="用户名/昵称/手机号" clearable />
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
          <el-button type="success" @click="handleAdd" v-permission="'system:user:add'">新增用户</el-button>
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
        <el-table-column prop="orgName" label="部门" width="120" />
        <el-table-column prop="userTypeDesc" label="用户类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.userType === 2 ? 'danger' : scope.row.userType === 1 ? 'warning' : 'info'" size="small">
              {{ scope.row.userTypeDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.statusDesc === '启用' ? 'success' : 'danger'">
              {{ scope.row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column label="操作" width="auto" fixed="right">
          <template #default="scope">
            <TableActions :buttons="[
              { label: '编辑', props: { type: 'primary' }, handler: () => handleEdit(scope.row), permission: 'system:user:edit' },
              { label: scope.row.statusDesc === '启用' ? '禁用' : '启用', props: { type: scope.row.statusDesc === '启用' ? 'danger' : 'success' }, handler: () => handleToggleStatus(scope.row), permission: 'system:user:edit' },
              { label: '删除', props: { type: 'danger' }, handler: () => handleDelete(scope.row), permission: 'system:user:del' }
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
        <el-form-item label="性别">
          <el-select v-model="formData.gender" placeholder="请选择">
            <el-option label="未知" :value="0" />
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色">
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, createUser, updateUser, deleteUser, toggleUserStatus, assignUserRoles, getUserById } from '@/api/user'
import { getRoleList } from '@/api/role'
import TableActions from '@/components/TableActions.vue'
import type { User, UserPageVO } from '@/api/user'
import type { Role } from '@/api/role'

const loading = ref(false)
const tableData = ref<UserPageVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const roleList = ref<Role[]>([])

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<string | null>(null)
const formRef = ref()

const formData = reactive<Partial<User>>({
  username: '',
  password: '',
  nickname: '',
  mobile: '',
  email: '',
  gender: 0,
  status: 1,
  roleIds: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getUserPage({
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

async function loadRoles() {
  const res: any = await getRoleList(1)
  if (res.code === 200) {
    roleList.value = res.data
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
  formData.gender = 0
  formData.status = 1
  formData.roleIds = []
  currentId.value = null
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

async function handleEdit(row: UserPageVO) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  currentId.value = row.id
  const res: any = await getUserById(row.id)
  if (res.code === 200) {
    const user = res.data
    formData.username = user.username
    formData.nickname = user.nickname
    formData.mobile = user.mobile
    formData.email = user.email
    formData.gender = user.gender
    formData.status = user.status
    // 后端 UserVO.roleIds 为 Long[]，JS 中为 string[]
    formData.roleIds = (user.roleIds as string[]) || []
  }
  dialogVisible.value = true
}

async function handleToggleStatus(row: UserPageVO) {
  try {
    await ElMessageBox.confirm(
      `确定${row.statusDesc === '启用' ? '禁用' : '启用'}该用户？`,
      '提示',
      { type: 'warning' }
    )
    const res: any = await toggleUserStatus(row.id)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadData()
    }
  } catch {
    // cancelled
  }
}

async function handleDelete(row: UserPageVO) {
  try {
    await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
    const res: any = await deleteUser(row.id)
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
    const res: any = await updateUser(currentId.value, formData as User)
    if (res.code === 200) {
      if (formData.roleIds && formData.roleIds.length > 0) {
        await assignUserRoles(currentId.value, formData.roleIds)
      }
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadData()
    }
  } else {
    const res: any = await createUser(formData as User)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadData()
    }
  }
}

onMounted(() => {
  loadData()
  loadRoles()
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
