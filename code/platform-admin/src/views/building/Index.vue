<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="楼宇名称/编号" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增楼栋</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="buildingName" label="楼栋名称" min-width="160" />
        <el-table-column prop="buildingNo" label="楼栋编号" width="120" />
        <el-table-column prop="floors" label="楼层数" width="80" align="right" />
        <el-table-column prop="totalArea" label="总面积(m²)" width="120" align="right">
          <template #default="scope">{{ scope.row.totalArea ? scope.row.totalArea.toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column prop="buildYear" label="建成年份" width="100" />
        <el-table-column prop="manager" label="负责人" width="100" />
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
      <el-form :model="formData" label-width="110px" :rules="rules" ref="formRef">
        <el-form-item label="园区" prop="parkId">
          <el-select v-model="formData.parkId" placeholder="请选择园区" filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋名称" prop="buildingName"><el-input v-model="formData.buildingName" maxlength="64" /></el-form-item>
        <el-form-item label="楼栋编号"><el-input v-model="formData.buildingNo" maxlength="32" /></el-form-item>
        <el-form-item label="总楼层数"><el-input-number v-model="formData.floors" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="总面积(m²)"><el-input-number v-model="formData.totalArea" :precision="2" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="建成年份"><el-input-number v-model="formData.buildYear" :min="1900" :max="2100" style="width:100%" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="formData.manager" maxlength="32" /></el-form-item>
        <el-form-item label="负责人电话"><el-input v-model="formData.managerPhone" maxlength="20" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" /></el-form-item>
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
import { getBuildingPage, getBuildingById, createBuilding, updateBuilding, deleteBuilding } from '@/api/building'
import { getParkList } from '@/api/park'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', status: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<number | null>(null); const submitting = ref(false); const formRef = ref()
const parkOptions = ref<any[]>([])

const defaultForm = { parkId: undefined as number | undefined, buildingName: '', buildingNo: '', floors: 1, totalArea: undefined as number | undefined, buildYear: undefined as number | undefined, manager: '', managerPhone: '', remark: '', status: 1 }
const formData = reactive({ ...defaultForm })
const rules = {
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  buildingName: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }]
}

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) parkOptions.value = res.data || [] } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getBuildingPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword=''; searchForm.status=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value=null; isEdit.value=false }
function handleAdd() { resetForm(); dialogTitle.value='新增楼栋'; dialogVisible.value=true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value=true; currentId.value=row.id; dialogTitle.value='编辑楼栋'
  try { const res: any = await getBuildingById(row.id); if (res.code === 200) Object.assign(formData, res.data) } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateBuilding(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'更新失败')
    } else {
      const res: any = await createBuilding(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除楼栋「${row.buildingName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteBuilding(row.id)
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
