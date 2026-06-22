<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="区域名称" clearable /></el-form-item>
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
          <el-button type="success" @click="handleAdd">新增区域</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="areaName" label="区域名称" width="150" />
        <el-table-column label="园区" width="120">
          <template #default="scope">{{ parkMap[scope.row.parkId] || scope.row.parkId }}</template>
        </el-table-column>
        <el-table-column prop="areaCovered" label="占地面积(m²)" width="130" align="right">
          <template #default="scope">{{ scope.row.areaCovered ? scope.row.areaCovered.toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column prop="builtArea" label="建筑面积(m²)" width="130" align="right">
          <template #default="scope">{{ scope.row.builtArea ? scope.row.builtArea.toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column prop="functionArea" label="功能区域" width="150" />
        <el-table-column prop="buildingAmount" label="楼栋数" width="80" align="right" />
        <el-table-column prop="roomAmount" label="房间数" width="80" align="right" />
        <el-table-column prop="isVirtual" label="虚拟" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.isVirtual === 1 ? 'warning' : 'info'" size="small">
              {{ scope.row.isVirtual === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form :model="formData" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="园区" prop="parkId">
          <el-select v-model="formData.parkId" placeholder="请选择园区" filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域名称" prop="areaName"><el-input v-model="formData.areaName" maxlength="64" /></el-form-item>
        <el-form-item label="占地面积(m²)" prop="areaCovered"><el-input-number v-model="formData.areaCovered" :precision="2" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="建筑面积(m²)"><el-input-number v-model="formData.builtArea" :precision="2" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="功能区域" prop="functionArea"><el-input v-model="formData.functionArea" maxlength="64" /></el-form-item>
        <el-form-item label="楼栋数"><el-input-number v-model="formData.buildingAmount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="房间数"><el-input-number v-model="formData.roomAmount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="是否虚拟"><el-switch v-model="formData.isVirtual" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="formData.sorting" :min="0" style="width:100%" /></el-form-item>
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
import { getAreaPage, getAreaById, createArea, updateArea, deleteArea } from '@/api/area'
import { getParkList } from '@/api/park'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', parkId: undefined as number | undefined, status: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<number | null>(null); const submitting = ref(false); const formRef = ref()
const parkOptions = ref<any[]>([]); const parkMap = ref<Record<number, string>>({})

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) { parkOptions.value = res.data || []; parkOptions.value.forEach((p: any) => parkMap.value[p.id] = p.parkName) } } catch { /* ignore */ }
}

const defaultForm = { parkId: undefined as number | undefined, areaName: '', areaCovered: undefined as number | undefined, builtArea: undefined as number | undefined, functionArea: '', buildingAmount: 0, roomAmount: 0, isVirtual: 0, sorting: 0, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = {
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  areaName: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
  areaCovered: [{ required: true, message: '请输入占地面积', trigger: 'blur' }],
  functionArea: [{ required: true, message: '请输入功能区域', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getAreaPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword = ''; searchForm.parkId = undefined; searchForm.status = undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value = null; isEdit.value = false }
function handleAdd() { resetForm(); dialogTitle.value = '新增区域'; dialogVisible.value = true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value = true; currentId.value = row.id; dialogTitle.value = '编辑区域'
  try { const res: any = await getAreaById(row.id); if (res.code === 200) Object.assign(formData, res.data) } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateArea(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value = false; loadData() } else ElMessage.error(res.message || '更新失败')
    } else {
      const res: any = await createArea(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value = false; loadData() } else ElMessage.error(res.message || '新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除区域「${row.areaName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteArea(row.id)
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