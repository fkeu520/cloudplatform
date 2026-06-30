<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="企业">
          <el-select
            v-model="searchForm.enterpriseId"
            placeholder="选择企业查看其绑定"
            clearable
            filterable
            remote
            :remote-method="searchEnterprises"
            :loading="enterpriseLoading"
            style="width: 320px"
          >
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" :disabled="!searchForm.enterpriseId" @click="handleAdd">新增绑定</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="bindType" label="绑定类型" width="120">
          <template #default="scope">
            <el-tag size="small" :type="bindTypeColor(scope.row.bindType)">
              {{ bindTypeLabel(scope.row.bindType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bindId" label="绑定对象 ID" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.bindStatus === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.bindStatus === 1 ? '有效' : '失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bindingAt" label="绑定时间" width="160" />
        <el-table-column prop="unboundAt" label="解绑时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.bindStatus === 1" type="warning" size="small" @click="handleUnbind(scope.row)">解绑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增绑定" width="500px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="企业">
          <el-input :model-value="enterpriseName" disabled />
        </el-form-item>
        <el-form-item label="绑定类型" prop="bindType">
          <el-select v-model="formData.bindType" placeholder="选择绑定类型" style="width:100%">
            <el-option label="园区" value="park" />
            <el-option label="楼栋" value="building" />
            <el-option label="租户" value="tenant" />
            <el-option label="房间" value="room" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象 ID" prop="bindId">
          <el-input-number v-model="formData.bindId" :min="1" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="255" />
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
 * 企业绑定关系 (park-enterprise Phase 1)
 * 后端: /enterprise/bind/list, /enterprise/bind, /enterprise/bind/{id}/unbind
 * 菜单: sys_menu id=523 (perms: enterprise:bind:list)
 */
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listBindByEnterprise, createBind, unbind, deleteBind, type EnterpriseEntBind, type BindType } from '@/api/enterprise-bind'
import { getEnterprisePage, type Enterprise } from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<EnterpriseEntBind[]>([])
const searchForm = reactive({ enterpriseId: undefined as string | undefined })

const enterpriseLoading = ref(false)
const enterpriseOptions = ref<Enterprise[]>([])
const enterpriseName = ref('')

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const defaultForm: Partial<EnterpriseEntBind> = {
  bindType: 'park' as BindType, bindId: 0, remark: '',
}
const formData = reactive<Partial<EnterpriseEntBind>>({ ...defaultForm })

const rules = {
  bindType: [{ required: true, message: '请选择绑定类型', trigger: 'change' }],
  bindId: [{ required: true, message: '请输入对象 ID', trigger: 'blur' }],
}

function bindTypeLabel(t?: string) {
  return ({ park: '园区', building: '楼栋', tenant: '租户', room: '房间' } as Record<string, string>)[t || ''] || t || '-'
}
function bindTypeColor(t?: string) {
  return ({ park: 'success', building: 'warning', tenant: 'info', room: 'primary' } as Record<string, string>)[t || ''] || ''
}

async function loadData() {
  if (!searchForm.enterpriseId) {
    tableData.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await listBindByEnterprise(searchForm.enterpriseId)
    if (res.code === 200) {
      tableData.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载绑定失败')
    }
  } catch { ElMessage.error('加载绑定失败') }
  finally { loading.value = false }
}

async function searchEnterprises(keyword: string) {
  enterpriseLoading.value = true
  try {
    const res: any = await getEnterprisePage({ keyword, pageNum: 1, pageSize: 50 })
    if (res.code === 200) {
      enterpriseOptions.value = res.data?.records || []
    }
  } catch { /* ignore */ }
  finally { enterpriseLoading.value = false }
}

function handleSearch() { loadData() }
function handleReset() { searchForm.enterpriseId = undefined; enterpriseName.value = ''; loadData() }

watch(() => searchForm.enterpriseId, (id) => {
  if (id) {
    const e = enterpriseOptions.value.find(x => String(x.id) === String(id))
    enterpriseName.value = e?.name || ''
  } else {
    enterpriseName.value = ''
  }
  loadData()
})

function resetForm() { Object.assign(formData, { ...defaultForm }) }

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: Partial<EnterpriseEntBind> = {
      ...formData,
      enterpriseId: searchForm.enterpriseId,
    }
    const res: any = await createBind(payload)
    if (res.code === 200) {
      ElMessage.success('绑定成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '绑定失败')
    }
  } catch { ElMessage.error('绑定失败') }
  finally { submitting.value = false }
}

async function handleUnbind(row: EnterpriseEntBind) {
  try {
    await ElMessageBox.confirm(`确认解绑该绑定关系吗？`, '提示', { type: 'warning' })
    const res: any = await unbind(String(row.id))
    if (res.code === 200) {
      ElMessage.success('解绑成功')
      loadData()
    } else {
      ElMessage.error(res.message || '解绑失败')
    }
  } catch { /* cancelled */ }
}

async function handleDelete(row: EnterpriseEntBind) {
  try {
    await ElMessageBox.confirm(`确认删除该绑定记录吗？`, '警告', { type: 'warning' })
    const res: any = await deleteBind(String(row.id))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch { /* cancelled */ }
}

onMounted(() => { searchEnterprises('') })
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
</style>
