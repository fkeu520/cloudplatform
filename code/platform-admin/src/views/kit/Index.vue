<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="配套名称" clearable /></el-form-item>
        <el-form-item label="园区">
          <el-select v-model="searchForm.parkId" placeholder="全部园区" clearable filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增配套</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="kitName" label="配套名称" min-width="200" />
        <el-table-column prop="amount" label="数量" width="100" align="right" />
        <el-table-column label="设备数" width="90" align="center">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.equipmentCount ?? '-' }}</el-tag>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="园区" prop="parkId">
          <el-select v-model="formData.parkId" placeholder="请选择园区" filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="配套名称" prop="kitName"><el-input v-model="formData.kitName" maxlength="64" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="formData.amount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="formData.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="设备清单">
          <el-button type="primary" size="small" @click="addEquipmentRow">+ 添加设备</el-button>
        </el-form-item>
        <el-form-item label=" " v-if="equipmentList.length > 0">
          <el-table :data="equipmentList" border size="small" style="width:100%">
            <el-table-column label="设备名称" min-width="160">
              <template #default="{ row, $index }">
                <el-input v-model="row.equipmentName" placeholder="设备名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="型号" min-width="140">
              <template #default="{ row, $index }">
                <el-input v-model="row.model" placeholder="型号" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="{ row, $index }">
                <el-input-number v-model="row.amount" :min="1" size="small" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" fixed="right">
              <template #default="{ $index }">
                <el-button type="danger" size="small" @click="equipmentList.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
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
import { getKitPage, getKitById, createKit, updateKit, deleteKit } from '@/api/kit'
import { listByKit, batchSaveByKit } from '@/api/equipment'
import { getParkList } from '@/api/park'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', parkId: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<number | null>(null); const submitting = ref(false); const formRef = ref()
const parkOptions = ref<any[]>([])
const equipmentList = ref<any[]>([])

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) parkOptions.value = res.data || [] } catch { /* ignore */ }
}

const defaultForm = { parkId: undefined as number | undefined, kitName: '', amount: 1, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = { parkId: [{ required: true, message: '请选择园区', trigger: 'change' }], kitName: [{ required: true, message: '请输入配套名称', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res: any = await getKitPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword=''; searchForm.parkId=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value=null; isEdit.value=false; equipmentList.value = [] }
function addEquipmentRow() { equipmentList.value.push({ equipmentName: '', model: '', amount: 1 }) }
function handleAdd() { resetForm(); dialogTitle.value='新增配套'; dialogVisible.value=true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value=true; currentId.value=row.id; dialogTitle.value='编辑配套'
  try {
    const res: any = await getKitById(row.id); if (res.code === 200) Object.assign(formData, res.data)
    const eqRes: any = await listByKit(row.id); if (eqRes.code === 200) equipmentList.value = (eqRes.data || []).map((e: any) => ({ equipmentName: e.equipmentName, model: e.model || '', amount: e.amount ?? 1 }))
  } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateKit(currentId.value, formData)
      if (res.code === 200) {
        await batchSaveByKit(currentId.value, equipmentList.value)
        ElMessage.success('更新成功'); dialogVisible.value=false; loadData()
      } else ElMessage.error(res.message||'更新失败')
    } else {
      const res: any = await createKit(formData)
      if (res.code === 200) {
        if (res.data?.id) await batchSaveByKit(res.data.id, equipmentList.value)
        ElMessage.success('新增成功'); dialogVisible.value=false; loadData()
      } else ElMessage.error(res.message||'新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除配套「${row.kitName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteKit(row.id)
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