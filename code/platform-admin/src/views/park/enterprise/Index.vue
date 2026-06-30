<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="企业名称/简称/统一信用代码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增企业</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="name" label="企业名称" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="alias" label="简称" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="creditCode" label="统一信用代码" width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="legalPersonName" label="法人" width="100" />
        <el-table-column prop="regCapital" label="注册资本" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="industry" label="行业" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="regStatus" label="经营状态" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-if="scope.row.status === 1" type="warning" size="small" @click="handleToggleStatus(scope.row, 0)">停用</el-button>
            <el-button v-else type="success" size="small" @click="handleToggleStatus(scope.row, 1)">启用</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" @close="resetForm" top="5vh">
      <el-form :model="formData" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="formData.name" maxlength="200" placeholder="必填" />
        </el-form-item>
        <el-form-item label="简称">
          <el-input v-model="formData.alias" maxlength="100" />
        </el-form-item>
        <el-form-item label="统一信用代码">
          <el-input v-model="formData.creditCode" maxlength="32" />
        </el-form-item>
        <el-form-item label="纳税人识别号">
          <el-input v-model="formData.taxNumber" maxlength="32" />
        </el-form-item>
        <el-form-item label="法人">
          <el-input v-model="formData.legalPersonName" maxlength="50" />
        </el-form-item>
        <el-form-item label="注册资本">
          <el-col :span="14"><el-input v-model="formData.regCapital" maxlength="64" placeholder="e.g. 1000万" /></el-col>
          <el-col :span="10"><el-input v-model="formData.regCapitalCurrency" maxlength="16" placeholder="币种 (默认人民币)" /></el-col>
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="formData.industry" maxlength="100" />
        </el-form-item>
        <el-form-item label="行业分类">
          <el-input v-model="formData.category" maxlength="100" placeholder="国民经济行业分类门类" />
        </el-form-item>
        <el-form-item label="注册地址">
          <el-input v-model="formData.regLocation" maxlength="255" />
        </el-form-item>
        <el-form-item label="经营状态">
          <el-input v-model="formData.regStatus" maxlength="32" placeholder="在营/吊销/注销" />
        </el-form-item>
        <el-form-item label="人员规模">
          <el-input v-model="formData.staffNumRange" maxlength="32" placeholder="e.g. 100-499 人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.phoneNumber" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="经营范围">
          <el-input v-model="formData.businessScope" type="textarea" :rows="3" maxlength="2000" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 企业档案 - 列表 (park-enterprise Phase 1)
 * 后端: /enterprise/page (EnterpriseController)
 * 菜单: sys_menu id=521 (perms: enterprise:list)
 *       sys_menu id=520 (parent, perms: enterprise:menu)
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEnterprisePage,
  getEnterpriseById,
  createEnterprise,
  updateEnterprise,
  deleteEnterprise,
  toggleEnterpriseStatus,
  type Enterprise,
} from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<Enterprise[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const searchForm = reactive({ keyword: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()

const defaultForm: Partial<Enterprise> = {
  name: '', alias: '', creditCode: '', taxNumber: '',
  legalPersonName: '', regCapital: '', regCapitalCurrency: '人民币',
  industry: '', category: '', regLocation: '', regStatus: '在营',
  staffNumRange: '', phoneNumber: '', email: '', businessScope: '',
  status: 1,
}
const formData = reactive<Partial<Enterprise>>({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getEnterprisePage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '加载企业列表失败')
    }
  } catch (e) {
    ElMessage.error('加载企业列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() {
  Object.assign(formData, { ...defaultForm })
  currentId.value = null
  isEdit.value = false
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增企业'
  dialogVisible.value = true
}

async function handleEdit(row: Enterprise) {
  resetForm()
  isEdit.value = true
  currentId.value = String(row.id)
  dialogTitle.value = '编辑企业'
  try {
    const res: any = await getEnterpriseById(String(row.id))
    if (res.code === 200) {
      Object.assign(formData, res.data)
    } else {
      ElMessage.error(res.message || '获取详情失败')
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    let res: any
    if (isEdit.value && currentId.value) {
      res = await updateEnterprise(currentId.value, formData)
    } else {
      res = await createEnterprise(formData)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || (isEdit.value ? '更新失败' : '新增失败'))
    }
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Enterprise) {
  try {
    await ElMessageBox.confirm(`确认删除企业「${row.name}」吗？此操作不可恢复。`, '警告', { type: 'warning' })
    const res: any = await deleteEnterprise(String(row.id))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch { /* cancelled */ }
}

async function handleToggleStatus(row: Enterprise, status: number) {
  const action = status === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${action}企业「${row.name}」吗？`, '提示', { type: 'warning' })
    const res: any = await toggleEnterpriseStatus(String(row.id), status)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadData()
    } else {
      ElMessage.error(res.message || `${action}失败`)
    }
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
