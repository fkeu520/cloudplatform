<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="地块编号/名称" clearable /></el-form-item>
        <el-form-item label="园区ID"><el-input-number v-model="searchForm.parkId" :min="0" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="可用" :value="1" /><el-option label="已卖" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增地块</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="massifCode" label="地块编号" width="150" />
        <el-table-column prop="massifName" label="地块名称" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="massifArea" label="地块面积(m²)" width="130" align="right">
          <template #default="scope">{{ scope.row.massifArea ? scope.row.massifArea.toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column prop="useYear" label="使用年限" width="100" />
        <el-table-column prop="landNatureId" label="土地性质ID" width="120" />
        <el-table-column prop="planUseId" label="规划用途ID" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '可用' : '已卖' }}
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="园区ID" prop="parkId"><el-input-number v-model="formData.parkId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="地块编号" prop="massifCode"><el-input v-model="formData.massifCode" maxlength="32" /></el-form-item>
        <el-form-item label="地块名称"><el-input v-model="formData.massifName" maxlength="256" /></el-form-item>
        <el-form-item label="地块面积(m²)"><el-input-number v-model="formData.massifArea" :precision="2" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="使用年限"><el-input-number v-model="formData.useYear" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="土地性质ID"><el-input-number v-model="formData.landNatureId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="规划用途ID"><el-input-number v-model="formData.planUseId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="资产类型"><el-input v-model="formData.assetType" placeholder="国土资源" /></el-form-item>
        <el-form-item label="地块描述"><el-input v-model="formData.massifDesc" type="textarea" :rows="2" maxlength="256" /></el-form-item>
        <el-form-item label="地块地址"><el-input v-model="formData.address" maxlength="255" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="formData.status"><el-radio :value="1">可用</el-radio><el-radio :value="0">已卖</el-radio></el-radio-group></el-form-item>
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
import { getMassifPage, getMassifById, createMassif, updateMassif, deleteMassif } from '@/api/massif'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', parkId: undefined as number | undefined, status: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<number | null>(null); const submitting = ref(false); const formRef = ref()

const defaultForm = {
  parkId: 1, massifCode: '', massifName: '', massifArea: undefined as number | undefined,
  useYear: 40, landNatureId: 1, planUseId: 1, assetType: '国土资源',
  massifDesc: '', address: '', status: 1
}
const formData = reactive({ ...defaultForm })
const rules = {
  parkId: [{ required: true, message: '请输入园区ID', trigger: 'blur' }],
  massifCode: [{ required: true, message: '请输入地块编号', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMassifPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword=''; searchForm.parkId=undefined; searchForm.status=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value=null; isEdit.value=false }
function handleAdd() { resetForm(); dialogTitle.value='新增地块'; dialogVisible.value=true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value=true; currentId.value=row.id; dialogTitle.value='编辑地块'
  try { const res: any = await getMassifById(row.id); if (res.code === 200) Object.assign(formData, res.data) } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateMassif(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'更新失败')
    } else {
      const res: any = await createMassif(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除地块「${row.massifName || row.massifCode}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteMassif(row.id)
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