<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="企业">
          <el-select
            v-model="searchForm.enterpriseId"
            placeholder="选择企业查看其标签"
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
          <el-button type="success" :disabled="!searchForm.enterpriseId" @click="handleAdd">新增标签</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="tagName" label="标签名" min-width="150" />
        <el-table-column label="颜色" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.tagColor" :color="scope.row.tagColor" effect="dark" size="small">
              {{ scope.row.tagColor }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="remark" label="备注" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增标签" width="500px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="企业">
          <el-input :model-value="enterpriseName" disabled />
        </el-form-item>
        <el-form-item label="标签名" prop="tagName">
          <el-input v-model="formData.tagName" maxlength="50" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="formData.tagColor" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
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
 * 企业标签管理 (park-enterprise Phase 1)
 * 后端: /enterprise/tag/list, /enterprise/tag/all, /enterprise/tag, /enterprise/tag/{id}
 * 菜单: sys_menu id=522 (perms: enterprise:tag:list)
 */
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTagByEnterprise, createTag, deleteTag, type EnterpriseTag } from '@/api/enterprise-tag'
import { getEnterprisePage, type Enterprise } from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<EnterpriseTag[]>([])
const searchForm = reactive({ enterpriseId: undefined as string | undefined })

const enterpriseLoading = ref(false)
const enterpriseOptions = ref<Enterprise[]>([])
const enterpriseName = ref('')

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const defaultForm: Partial<EnterpriseTag> = {
  tagName: '', tagColor: '#1890ff', sortOrder: 0, remark: '',
}
const formData = reactive<Partial<EnterpriseTag>>({ ...defaultForm })

const rules = {
  tagName: [{ required: true, message: '请输入标签名', trigger: 'blur' }],
}

async function loadData() {
  if (!searchForm.enterpriseId) {
    tableData.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await listTagByEnterprise(searchForm.enterpriseId)
    if (res.code === 200) {
      tableData.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载标签失败')
    }
  } catch {
    ElMessage.error('加载标签失败')
  } finally {
    loading.value = false
  }
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
    const payload: Partial<EnterpriseTag> = {
      ...formData,
      enterpriseId: searchForm.enterpriseId,
    }
    const res: any = await createTag(payload)
    if (res.code === 200) {
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '新增失败')
    }
  } catch { ElMessage.error('新增失败') }
  finally { submitting.value = false }
}

async function handleDelete(row: EnterpriseTag) {
  try {
    await ElMessageBox.confirm(`确认删除标签「${row.tagName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteTag(String(row.id))
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
