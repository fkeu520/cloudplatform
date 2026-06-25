<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="性质名称" clearable /></el-form-item>
        <el-form-item label="园区">
          <el-select v-model="searchForm.parkId" placeholder="全部园区" clearable filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增性质</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column label="园区" width="120"><template #default="scope">{{ parkMap[scope.row.parkId] || scope.row.parkId }}</template></el-table-column>
        <el-table-column prop="landNatureName" label="土地性质名称" min-width="200" />
        <el-table-column prop="landNatureCode" label="编号" width="150" />
        <el-table-column label="标的色" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.color" :color="scope.row.color" effect="dark" style="color:#fff;border:none">
              {{ scope.row.color }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="园区" prop="parkId">
          <el-select v-model="formData.parkId" placeholder="请选择园区" filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="性质名称" prop="landNatureName"><el-input v-model="formData.landNatureName" maxlength="256" /></el-form-item>
        <el-form-item label="编号"><el-input v-model="formData.landNatureCode" maxlength="32" /></el-form-item>
        <el-form-item label="标的色"><el-color-picker v-model="formData.color" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="formData.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLandNaturePage, getLandNatureById, createLandNature, updateLandNature, deleteLandNature } from '@/api/land-nature'
import { getParkList } from '@/api/park'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', parkId: undefined as string | undefined, status: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<string | null>(null); const submitting = ref(false); const formRef = ref()
const parkOptions = ref<any[]>([]); const parkMap = ref<Record<string, string>>({})

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) { parkOptions.value = res.data || []; parkOptions.value.forEach((p: any) => parkMap.value[String(p.id)] = p.parkName) } } catch { /* ignore */ }
}

const defaultForm = { parkId: undefined as string | undefined, landNatureName: '', landNatureCode: '', color: '#909399', status: 1 }
const formData = reactive({ ...defaultForm })
const rules = { parkId: [{ required: true, message: '请选择园区', trigger: 'change' }], landNatureName: [{ required: true, message: '请输入土地性质名称', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res: any = await getLandNaturePage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword=''; searchForm.parkId=undefined; searchForm.status=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value=null; isEdit.value=false }
function handleAdd() { resetForm(); dialogTitle.value='新增土地性质'; dialogVisible.value=true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value=true; currentId.value=row.id; dialogTitle.value='编辑土地性质'
  try { const res: any = await getLandNatureById(row.id); if (res.code === 200) Object.assign(formData, { ...res.data, parkId: res.data.parkId != null ? String(res.data.parkId) : undefined }) } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateLandNature(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'更新失败')
    } else {
      const res: any = await createLandNature(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除土地性质「${row.landNatureName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteLandNature(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { /* cancelled */ }
}

onMounted(() => { loadData(); loadParkOptions() })
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>