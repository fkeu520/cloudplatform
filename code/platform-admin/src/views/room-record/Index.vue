<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="房间ID"><el-input-number v-model="searchForm.roomId" :min="0" clearable /></el-form-item>
        <el-form-item label="园区ID"><el-input-number v-model="searchForm.parkId" :min="0" clearable /></el-form-item>
        <el-form-item label="操作">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="添加" :value="0" /><el-option label="解除" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增记录</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        房间绑定/解绑历史记录（append-only），只能新增和查询
      </el-alert>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="roomId" label="房间ID" width="100" />
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="covenantId" label="合同ID" width="100" />
        <el-table-column prop="covenantType" label="合同类型" width="100">
          <template #default="scope">{{ ['租赁', '销售', '其他'][scope.row.covenantType] || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 0 ? '添加' : '解除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="160" />
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="新增绑定记录" width="600px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="园区ID" prop="parkId"><el-input-number v-model="formData.parkId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="房间ID" prop="roomId"><el-input-number v-model="formData.roomId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="客户ID"><el-input-number v-model="formData.customerId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="合同ID"><el-input-number v-model="formData.covenantId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="formData.covenantType">
            <el-option label="租赁" :value="0" /><el-option label="销售" :value="1" /><el-option label="其他" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="formData.status"><el-radio :value="0">添加</el-radio><el-radio :value="1">解除</el-radio></el-radio-group>
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
import { ElMessage } from 'element-plus'
import { getRoomRecordPage, createRoomRecord } from '@/api/room-record'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ roomId: undefined as number | undefined, parkId: undefined as number | undefined, status: undefined as number | undefined })
const dialogVisible = ref(false); const submitting = ref(false); const formRef = ref()

const defaultForm = {
  parkId: 1, roomId: 1, customerId: 1, covenantId: 1, covenantType: 0, status: 0
}
const formData = reactive({ ...defaultForm })
const rules = {
  parkId: [{ required: true, message: '请输入园区ID', trigger: 'blur' }],
  roomId: [{ required: true, message: '请输入房间ID', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRoomRecordPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.roomId=undefined; searchForm.parkId=undefined; searchForm.status=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }) }
function handleAdd() { resetForm(); dialogVisible.value = true }
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    const res: any = await createRoomRecord(formData)
    if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'新增失败')
  } finally { submitting.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>