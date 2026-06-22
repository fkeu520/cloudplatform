<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="房号/备注" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.roomType" placeholder="全部" clearable>
            <el-option label="办公室" value="OFFICE" />
            <el-option label="会议室" value="MEETING" />
            <el-option label="仓储" value="STORAGE" />
            <el-option label="停车" value="PARKING" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="空置" :value="0" />
            <el-option label="已租" :value="1" />
            <el-option label="装修中" :value="2" />
            <el-option label="停用" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增房源</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="parkId" label="园区ID" width="100" />
        <el-table-column prop="buildingId" label="楼宇ID" width="100" />
        <el-table-column prop="floor" label="楼层" width="80" />
        <el-table-column prop="roomNo" label="房号" width="120" />
        <el-table-column prop="roomType" label="类型" width="100" />
        <el-table-column prop="area" label="面积(m²)" width="110" align="right">
          <template #default="scope">
            {{ scope.row.area ? scope.row.area.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="monthlyRent" label="月租(元)" width="110" align="right">
          <template #default="scope">
            {{ scope.row.monthlyRent ? scope.row.monthlyRent.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="roomStatusTagType(scope.row.status)" size="small">
              {{ roomStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="warning" size="small" @click="handleUpdateStatus(scope.row)">
              状态变更
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form :model="formData" label-width="120px" :rules="rules" ref="formRef" :validate-on-rule-change="false">
        <el-form-item label="园区ID" prop="parkId">
          <el-input-number v-model="formData.parkId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="楼宇ID" prop="buildingId">
          <el-input-number v-model="formData.buildingId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input-number v-model="formData.floor" :min="-10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="房号" prop="roomNo">
          <el-input v-model="formData.roomNo" placeholder="e.g. A-101" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="roomType">
          <el-select v-model="formData.roomType" placeholder="请选择">
            <el-option label="办公室" value="OFFICE" />
            <el-option label="会议室" value="MEETING" />
            <el-option label="仓储" value="STORAGE" />
            <el-option label="停车" value="PARKING" />
          </el-select>
        </el-form-item>
        <el-form-item label="面积(m²)" prop="area">
          <el-input-number v-model="formData.area" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月租(元)" prop="monthlyRent">
          <el-input-number v-model="formData.monthlyRent" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoomPage, getRoomById, createRoom, updateRoom, deleteRoom, updateRoomStatus
} from '@/api/room'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchForm = reactive({
  keyword: '', roomType: undefined as string | undefined, status: undefined as number | undefined
})
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref()

const defaultForm = {
  parkId: 1, buildingId: 0, floor: 1, roomNo: '', roomType: 'OFFICE',
  area: undefined as number | undefined, monthlyRent: undefined as number | undefined, remark: ''
}
const formData = reactive({ ...defaultForm })

const rules = {
  parkId: [{ required: true, message: '请输入园区ID', trigger: 'blur' }],
  roomNo: [{ required: true, message: '请输入房号', trigger: 'blur' }],
  roomType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function roomStatusLabel(s: number): string {
  return ['空置', '已租', '装修中', '停用'][s] || '-'
}
function roomStatusTagType(s: number): string {
  return ['success', 'primary', 'warning', 'info'][s] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRoomPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally { loading.value = false }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  searchForm.keyword = ''
  searchForm.roomType = undefined
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
  dialogTitle.value = '新增房源'
  dialogVisible.value = true
}
async function handleEdit(row: any) {
  resetForm()
  isEdit.value = true
  currentId.value = row.id
  dialogTitle.value = '编辑房源'
  try {
    const res: any = await getRoomById(row.id)
    if (res.code === 200) Object.assign(formData, res.data)
  } catch { ElMessage.error('获取房源详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateRoom(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value = false; loadData() }
      else ElMessage.error(res.message || '更新失败')
    } else {
      const res: any = await createRoom(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value = false; loadData() }
      else ElMessage.error(res.message || '新增失败')
    }
  } finally { submitting.value = false }
}
async function handleUpdateStatus(row: any) {
  try {
    const { value: status } = await ElMessageBox.prompt('输入新状态: 0=空置 1=已租 2=装修中 3=停用', '状态变更', {
      inputValue: String(row.status ?? 0),
      inputValidator: (v: string) => ['0', '1', '2', '3'].includes(v) || '请输入 0/1/2/3'
    })
    const res: any = await updateRoomStatus(row.id, Number(status))
    if (res.code === 200) { ElMessage.success('状态变更成功'); loadData() }
  } catch { /* cancelled */ }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除房源「${row.roomNo}」吗？删除后不可恢复。`, '警告', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning'
    })
    const res: any = await deleteRoom(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>