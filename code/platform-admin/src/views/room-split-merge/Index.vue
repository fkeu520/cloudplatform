<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="园区ID"><el-input-number v-model="searchForm.parkId" :min="0" clearable /></el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable>
            <el-option label="拆分" :value="1" /><el-option label="合并" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="合并" :value="0" /><el-option label="拆分" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增记录</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        房间拆分/合并历史记录（append-only），只能新增和查询
      </el-alert>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="userName" label="操作人" width="120" />
        <el-table-column prop="oldRoomName" label="原房源" min-width="150" />
        <el-table-column prop="newRoomName" label="新房源" min-width="150" />
        <el-table-column prop="num" label="拆分数量" width="100" align="right" />
        <el-table-column label="继承能源" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isExtend === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.isExtend === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'warning' : 'primary'" size="small">
              {{ scope.row.status === 1 ? '拆分' : '合并' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reasons" label="原因" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="createTime" label="操作时间" width="160" />
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="新增拆分合并记录" width="700px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="园区ID" prop="parkId"><el-input-number v-model="formData.parkId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="操作人"><el-input v-model="formData.userName" maxlength="64" /></el-form-item>
        <el-form-item label="原房源ID"><el-input-number v-model="formData.oldRoomId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="原房源名称"><el-input v-model="formData.oldRoomName" maxlength="64" /></el-form-item>
        <el-form-item label="新房源ID"><el-input-number v-model="formData.newRoomId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="新房源名称"><el-input v-model="formData.newRoomName" maxlength="64" /></el-form-item>
        <el-form-item label="拆分数量"><el-input-number v-model="formData.num" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="继承能源表"><el-switch v-model="formData.isExtend" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="formData.status"><el-radio :value="1">拆分</el-radio><el-radio :value="0">合并</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="formData.reasons" type="textarea" :rows="2" maxlength="500" /></el-form-item>
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
import { getRoomSplitMergePage, createRoomSplitMerge } from '@/api/room-split-merge'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ parkId: undefined as number | undefined, type: undefined as number | undefined, status: undefined as number | undefined })
const dialogVisible = ref(false); const submitting = ref(false); const formRef = ref()

const defaultForm = {
  parkId: 1, userId: undefined as number | undefined, userName: '', reasons: '',
  type: 1, oldRoomId: 1, oldRoomName: '', newRoomId: 1, newRoomName: '',
  num: 2, isExtend: 0, status: 1
}
const formData = reactive({ ...defaultForm })
const rules = {
  parkId: [{ required: true, message: '请输入园区ID', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRoomSplitMergePage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.parkId=undefined; searchForm.type=undefined; searchForm.status=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }) }
function handleAdd() { resetForm(); dialogVisible.value = true }
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    const res: any = await createRoomSplitMerge(formData)
    if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'新增失败')
  } finally { submitting.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; flex-direction: row-reverse; }
</style>