<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>租户管理</span>
          <el-button type="primary" @click="handleAdd">新增租户</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="tenantName" label="租户名称" width="140" />
        <el-table-column prop="tenantCode" label="编码" width="90" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.tenantType===1 ? 'success' : 'warning'">
              {{ row.tenantType===1 ? '单组织' : '集团型' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="90" />
        <el-table-column prop="contactMobile" label="电话" width="120" />
        <el-table-column prop="maxUserCount" label="用户上限" width="90">
          <template #default="{ row }">{{ row.maxUserCount > 0 ? row.maxUserCount : '不限' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-switch :model-value="row.status===1" @change="handleToggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleAdmins(row)">管理员</el-button>
            <el-button type="success" link @click="handleOrgs(row)">组织</el-button>
            <el-button type="warning" link @click="handleAuthorize(row)">授权应用</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
        :total="total" :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end"
        @change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑租户' : '新增租户'" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="form.tenantName" />
        </el-form-item>
        <el-form-item label="租户编码" prop="tenantCode">
          <el-input v-model="form.tenantCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="租户类型" prop="tenantType">
          <el-radio-group v-model="form.tenantType">
            <el-radio :value="0">集团型（可建分子公司）</el-radio>
            <el-radio :value="1">单组织（不可拆分）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户上限">
          <el-input-number v-model="form.maxUserCount" :min="0" placeholder="0=不限" style="width:200px" />
          <span style="margin-left:8px;color:#999">0 表示不限</span>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactMobile" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
        <template v-if="!isEdit">
          <el-divider>管理员信息（新建租户时自动创建）</el-divider>
          <el-form-item label="管理员账号" prop="adminUsername">
            <el-input v-model="form.adminUsername" placeholder="默认：admin" />
          </el-form-item>
          <el-form-item label="管理员密码" prop="adminPassword">
            <el-input v-model="form.adminPassword" type="password" show-password placeholder="默认：123456" />
          </el-form-item>
          <el-form-item label="管理员昵称">
            <el-input v-model="form.adminNickname" placeholder="默认同账号" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orgDialogVisible" title="组织架构" width="600px">
      <el-tree :data="orgTree" :props="orgProps" default-expand-all node-key="id"
               empty-text="暂无组织（创建租户时将自动创建根组织）" />
    </el-dialog>

    <el-dialog v-model="authDialogVisible" title="应用授权" width="500px">
      <el-checkbox-group v-model="authAppIds">
        <el-checkbox v-for="app in allApps" :key="app.id" :label="app.id" style="margin-bottom:8px">
          {{ app.appName }}（{{ app.appCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="authDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="authSubmitting" @click="handleAuthSubmit">保存授权</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminDialogVisible" :title="'管理员管理 - ' + adminTenantName" width="600px">
      <el-button size="small" type="primary" style="margin-bottom:12px" @click="handleAddAdmin">新增管理员</el-button>
      <el-table :data="adminList" v-loading="adminLoading" border stripe>
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="mobile" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定移除该管理员？" @confirm="handleDeleteAdmin(row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="addAdminDialogVisible" title="新增管理员" width="400px">
      <el-form label-width="100px">
        <el-form-item label="账号" required>
          <el-input v-model="newAdmin.username" placeholder="管理员登录账号" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="newAdmin.password" type="password" show-password placeholder="默认：123456" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="newAdmin.nickname" placeholder="默认同账号" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="newAdmin.mobile" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addAdminDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="addAdminSubmitting" @click="handleAddAdminSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { page, create, update, remove, toggleStatus, listOrgs, listAdmins, createAdmin, deleteAdmin } from '../../api/tenant'
import { list as listApps } from '../../api/app'
import { getAuthorizedAppIds, authorizeApps } from '../../api/tenantApp'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })
const form = reactive({
  tenantName: '', tenantCode: '', tenantType: 1, maxUserCount: 0,
  contactPerson: '', contactMobile: '', contactEmail: '', address: '', remark: '',
  adminUsername: '', adminPassword: '', adminNickname: ''
})
const rules = {
  tenantName: [{ required: true, message: '请输入租户名称' }],
  tenantCode: [{ required: true, message: '请输入租户编码' }],
  adminUsername: [{ required: true, message: '请输入管理员账号' }],
  adminPassword: [{ required: true, message: '请输入管理员密码' }]
}

const authDialogVisible = ref(false)
const authSubmitting = ref(false)
const allApps = ref<any[]>([])
const authAppIds = ref<number[]>([])
const authTenantId = ref<number>(0)

const orgDialogVisible = ref(false)
const orgTree = ref<any[]>([])
const orgProps = { children: 'children', label: 'name' }

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await page(query)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

function handleAdd() {
  isEdit.value = false; editId.value = null
  Object.assign(form, { tenantName: '', tenantCode: '', tenantType: 1, maxUserCount: 0, contactPerson: '', contactMobile: '', contactEmail: '', address: '', remark: '', adminUsername: '', adminPassword: '', adminNickname: '' })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该租户吗？', '提示')
  await remove(row.id)
  ElMessage.success('删除成功'); fetchData()
}

async function handleToggleStatus(row: any) {
  await toggleStatus(row.id)
  ElMessage.success('状态已更新'); fetchData()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await update(editId.value, form); ElMessage.success('更新成功')
    } else {
      await create(form); ElMessage.success('创建成功')
    }
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleOrgs(row: any) {
  const res = await listOrgs(row.id)
  orgTree.value = res.data || []
  orgDialogVisible.value = true
}

async function handleAuthorize(row: any) {
  authTenantId.value = row.id
  const [appsRes, authRes] = await Promise.all([listApps(0), getAuthorizedAppIds(row.id)])
  allApps.value = appsRes.data || []
  authAppIds.value = authRes.data || []
  authDialogVisible.value = true
}

async function handleAuthSubmit() {
  authSubmitting.value = true
  try {
    await authorizeApps(authTenantId.value, authAppIds.value)
    ElMessage.success('授权已保存')
    authDialogVisible.value = false
  } finally { authSubmitting.value = false }
}

const adminDialogVisible = ref(false)
const adminTenantId = ref<number>(0)
const adminTenantName = ref('')
const adminList = ref<any[]>([])
const adminLoading = ref(false)
const addAdminDialogVisible = ref(false)
const addAdminSubmitting = ref(false)
const newAdmin = reactive({ username: '', password: '', nickname: '', mobile: '' })

async function handleAdmins(row: any) {
  adminTenantId.value = row.id
  adminTenantName.value = row.tenantName
  await fetchAdmins()
  adminDialogVisible.value = true
}

async function fetchAdmins() {
  adminLoading.value = true
  try {
    const res = await listAdmins(adminTenantId.value!)
    adminList.value = res?.data?.records || []
  } finally { adminLoading.value = false }
}

function handleAddAdmin() {
  Object.assign(newAdmin, { username: '', password: '123456', nickname: '', mobile: '' })
  addAdminDialogVisible.value = true
}

async function handleAddAdminSubmit() {
  if (!newAdmin.username) { ElMessage.warning('请输入账号'); return }
  addAdminSubmitting.value = true
  try {
    await createAdmin(adminTenantId.value, newAdmin)
    ElMessage.success('管理员已创建')
    addAdminDialogVisible.value = false
    fetchAdmins()
  } finally { addAdminSubmitting.value = false }
}

async function handleDeleteAdmin(row: any) {
  try {
    await deleteAdmin(adminTenantId.value, row.id)
    ElMessage.success('管理员已删除')
    fetchAdmins()
  } catch { /* ignore */ }
}
</script>
