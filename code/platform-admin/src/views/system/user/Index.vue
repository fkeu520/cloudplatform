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
        <!-- F6: prop="orgName" 对应的是"组织"名，修正 label -->
        <el-table-column prop="orgName" label="组织" width="120" />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <TableActions :buttons="[
              { label: '编辑', props: { type: 'primary' }, handler: () => handleEdit(scope.row), permission: 'system:user:edit' },
              { label: '重置密码', props: { type: 'warning' }, handler: () => handleResetPwd(scope.row), permission: 'system:user:edit' },
              { label: scope.row.statusDesc === '启用' ? '禁用' : '启用', props: { type: scope.row.statusDesc === '启用' ? 'danger' : 'success' }, handler: () => handleToggleStatus(scope.row), permission: 'system:user:edit' },
              { label: '删除', props: { type: 'danger' }, handler: () => openDeleteStepUp(scope.row), permission: 'system:user:del' }
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
        <el-form-item label="组织">
          <el-select v-model="formData.orgId" placeholder="请选择组织" clearable filterable @change="onOrgChange">
            <el-option
              v-for="org in orgList"
              :key="org.id"
              :label="org.name"
              :value="org.id"
            />
          </el-select>
          <span v-if="isEdit && currentNames.orgName && !orgList.find(o => o.id === formData.orgId)" class="current-label">{{ currentNames.orgName }}</span>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="formData.deptId" placeholder="请选择部门" clearable filterable :disabled="!formData.orgId" @change="onDeptChange">
            <el-option
              v-for="dept in deptList"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
          <span v-if="isEdit && currentNames.deptName && !deptList.find(d => d.id === formData.deptId)" class="current-label">{{ currentNames.deptName }}</span>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="formData.postId" placeholder="请选择岗位" clearable filterable :disabled="!formData.deptId">
            <el-option
              v-for="post in postList"
              :key="post.id"
              :label="post.name"
              :value="post.id"
            />
          </el-select>
          <span v-if="isEdit && currentNames.postName && !postList.find(p => p.id === formData.postId)" class="current-label">{{ currentNames.postName }}</span>
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

    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="440px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="账号">
          <el-input :model-value="pwdTarget?.username" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少 6 位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="openResetPwdStepUp">下一步 (二次验证)</el-button>
      </template>
    </el-dialog>

    <!-- v8 P0-3: Step-up 二次鉴权弹窗 -->
    <StepUpDialog v-model="stepUpDeleteVisible" scope="user:delete"
                  description="删除用户 (含关联数据, 不可恢复)"
                  :on-success="onStepUpDeleteSuccess" />
    <StepUpDialog v-model="stepUpResetPwdVisible" scope="user:reset-pwd"
                  description="重置用户密码 (会强制下线)"
                  :on-success="onStepUpResetPwdSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, createUser, updateUser, deleteUser, toggleUserStatus, assignUserRoles, getUserById, resetUserPassword, changePassword } from '@/api/user'
import { getRoleList } from '@/api/role'
import { getOrgTree } from '@/api/org'
import { getDeptList } from '@/api/dept'
import { getPostByDeptId } from '@/api/post'
import { rsaEncrypt } from '@/api/crypto'
import TableActions from '@/components/TableActions.vue'
import StepUpDialog from '@/components/StepUpDialog.vue'
import type { User, UserPageVO } from '@/api/user'
import type { Role } from '@/api/role'

const loading = ref(false)
const tableData = ref<UserPageVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const roleList = ref<Role[]>([])
const orgList = ref<any[]>([])
const deptList = ref<any[]>([])
const postList = ref<any[]>([])
// 存储当前选中的组织/部门/岗位名称（供 ensureInList 在不重新请求时也能用）
const currentNames = reactive({ orgName: '', deptName: '', postName: '' })

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
  orgId: null,
  deptId: null,
  postId: null,
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
  formData.orgId = null
  formData.deptId = null
  formData.postId = null
  formData.status = 1
  formData.roleIds = []
  deptList.value = []
  postList.value = []
  currentNames.orgName = ''
  currentNames.deptName = ''
  currentNames.postName = ''
  currentId.value = null
}

/** 清理当前值对应的选项列表（在异步加载前调，避免加载完覆盖） */
function clearListsForCurrent() {
  deptList.value = []
  postList.value = []
}

// 组织/部门/岗位级联

/** 确保某个值存在于选项列表中，若不存在则追加（防止 el-select 显示 ID 而非名称） */
function ensureInList(list: any[], value: string | null | undefined, label: string | null | undefined) {
  if (value != null && label) {
    const exists = list.some((item: any) => item.id === value)
    if (!exists) {
      list.push({ id: value, name: label })
    }
  }
}

async function loadOrgTree() {
  const res: any = await getOrgTree()
  if (res.code === 200) {
    // 展平树结构为列表（el-select 用）
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
    orgList.value = flat.map((item: any) => ({ ...item, id: String(item.id) }))
  }
}

async function onOrgChange(orgId: string | null | undefined) {
  formData.deptId = null
  formData.postId = null
  deptList.value = []
  postList.value = []
  if (orgId) {
    loadDepts(orgId)
  }
}

async function loadDepts(orgId: string) {
  const res: any = await getDeptList(orgId)
  if (res.code === 200) {
    deptList.value = res.data || []
    // loadDepts 完成后，确保当前 deptId 仍在列表中（API 可能不包含已选择的项）
    ensureInList(deptList.value, formData.deptId, currentNames.deptName)
  }
}

async function onDeptChange(deptId: string | null | undefined) {
  formData.postId = null
  postList.value = []
  if (deptId) {
    loadPosts(deptId)
  }
}

async function loadPosts(deptId: string) {
  const res: any = await getPostByDeptId(deptId)
  if (res.code === 200) {
    postList.value = res.data || []
    ensureInList(postList.value, formData.postId, currentNames.postName)
  }
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
  try {
    const res: any = await getUserById(row.id)
    if (res.code === 200) {
      const user = res.data
      formData.username = user.username
      formData.nickname = user.nickname
      formData.mobile = user.mobile
      formData.email = user.email
      formData.gender = user.gender
      formData.status = user.status
      // IDs 来自 @JsonFormat(Shape.STRING) 已是字符串，保持 string 类型
      // 避免 Number() 转换导致 19 位雪花 ID 精度丢失 + el-select === 类型不匹配
      formData.orgId = user.orgId || null
      formData.deptId = user.deptId || null
      formData.postId = user.postId || null
      // 保存显示名称，供 ensureInList 在异步加载后使用
      currentNames.orgName = user.orgName || ''
      currentNames.deptName = user.deptName || ''
      currentNames.postName = user.postName || ''
      // 后端 UserVO.roleIds 为 Long[]，JS 中为 string[]
      formData.roleIds = (user.roleIds as string[]) || []
      // 确保当前值存在于选项列表中（防止 API 返回的 ID 与选项列表不匹配）
      ensureInList(orgList.value, formData.orgId, currentNames.orgName)
      ensureInList(deptList.value, formData.deptId, currentNames.deptName)
      ensureInList(postList.value, formData.postId, currentNames.postName)
      // 加载级联列表
      if (user.orgId) {
        loadDepts(user.orgId)
      }
      if (user.deptId) {
        loadPosts(user.deptId)
      }
    }
  } catch (e) {
    ElMessage.error('获取用户详情失败')
    return
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

// v8 P0-3: 高敏操作前弹出 StepUpDialog 二次鉴权
const stepUpDeleteVisible = ref(false)
const stepUpDeleteTarget = ref<UserPageVO | null>(null)
const stepUpResetPwdVisible = ref(false)
const stepUpChangePwdVisible = ref(false)

function openDeleteStepUp(row: UserPageVO) {
  stepUpDeleteTarget.value = row
  stepUpDeleteVisible.value = true
}

function openResetPwdStepUp() {
  // 先做密码长度校验
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  stepUpResetPwdVisible.value = true
}

async function onStepUpDeleteSuccess(stepUpToken: string) {
  const row = stepUpDeleteTarget.value
  if (!row) return
  try {
    const res: any = await deleteUser(row.id, stepUpToken)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch { /* request 拦截器已提示 */ }
}

async function onStepUpResetPwdSuccess(stepUpToken: string) {
  if (!pwdTarget.value) return
  try {
    const res: any = await resetUserPassword(pwdTarget.value.id, pwdForm.newPassword, stepUpToken)
    if (res.code === 200) {
      ElMessage.success(`用户 [${pwdTarget.value.username}] 密码已重置`)
      pwdDialogVisible.value = false
    }
  } catch { /* request 拦截器已提示 */ }
}

// 重置密码弹窗
const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdTarget = ref<UserPageVO | null>(null)
const pwdForm = reactive({ newPassword: '' })

function handleResetPwd(row: UserPageVO) {
  pwdTarget.value = row
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}

async function handlePwdSubmit() {
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (!pwdTarget.value) return
  pwdSubmitting.value = true
  try {
    const res: any = await resetUserPassword(pwdTarget.value.id, pwdForm.newPassword)
    if (res.code === 200) {
      ElMessage.success(`用户 [${pwdTarget.value.username}] 密码已重置`)
      pwdDialogVisible.value = false
    }
  } finally {
    pwdSubmitting.value = false
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
    // 新增用户时加密密码
    const submitData = { ...formData }
    if (submitData.password) {
      submitData.password = await rsaEncrypt(submitData.password)
    }
    const res: any = await createUser(submitData as User)
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
  loadOrgTree()
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
