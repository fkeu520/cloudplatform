<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="空间名称" clearable /></el-form-item>
        <el-form-item label="园区">
          <el-select v-model="searchForm.parkId" placeholder="全部园区" clearable filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="searchForm.areaId" placeholder="全部区域" clearable filterable>
            <el-option v-for="a in areaOptions" :key="a.id" :label="a.areaName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchForm.categoryId" placeholder="全部类别" clearable filterable>
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.typeName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增空间</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="spaceName" label="空间名称" min-width="200" />
        <el-table-column prop="spaceDescribe" label="位置描述" min-width="250" :show-overflow-tooltip="true" />
        <el-table-column label="区域" width="120">
          <template #default="scope">{{ areaMap[scope.row.areaId] || scope.row.areaId }}</template>
        </el-table-column>
        <el-table-column label="类别" width="120">
          <template #default="scope">{{ categoryMap[scope.row.categoryId] || scope.row.categoryId }}</template>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="园区" prop="parkId">
          <el-select v-model="formData.parkId" placeholder="请选择园区" filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="空间名称" prop="spaceName"><el-input v-model="formData.spaceName" maxlength="100" /></el-form-item>
        <el-form-item label="位置描述"><el-input v-model="formData.spaceDescribe" type="textarea" :rows="2" maxlength="500" /></el-form-item>
        <el-form-item label="区域">
          <el-select v-model="formData.areaId" placeholder="请选择区域" filterable>
            <el-option v-for="a in areaOptions" :key="a.id" :label="a.areaName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="formData.categoryId" placeholder="请选择类别" filterable>
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.typeName" :value="c.id" />
          </el-select>
        </el-form-item>
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
import { getSpacePage, getSpaceById, createSpace, updateSpace, deleteSpace } from '@/api/space'
import { getParkList } from '@/api/park'
import { getAreaPage } from '@/api/area'
import { getSpaceCategoryPage } from '@/api/space-category'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', parkId: undefined as number | undefined, areaId: undefined as number | undefined, categoryId: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<number | null>(null); const submitting = ref(false); const formRef = ref()
const parkOptions = ref<any[]>([])
const areaOptions = ref<any[]>([]); const areaMap = ref<Record<number, string>>({})
const categoryOptions = ref<any[]>([]); const categoryMap = ref<Record<number, string>>({})

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) parkOptions.value = res.data || [] } catch { /* ignore */ }
}
async function loadAreaOptions() {
  try { const res: any = await getAreaPage({ pageNum: 1, pageSize: 9999 }); if (res.code === 200) { areaOptions.value = res.data.records || []; areaOptions.value.forEach((a: any) => areaMap.value[a.id] = a.areaName) } } catch { /* ignore */ }
}
async function loadCategoryOptions() {
  try { const res: any = await getSpaceCategoryPage({ pageNum: 1, pageSize: 9999 }); if (res.code === 200) { categoryOptions.value = res.data.records || []; categoryOptions.value.forEach((c: any) => categoryMap.value[c.id] = c.typeName) } } catch { /* ignore */ }
}

const defaultForm = { parkId: undefined as number | undefined, spaceName: '', spaceDescribe: '', areaId: undefined as number | undefined, categoryId: undefined as number | undefined, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = { parkId: [{ required: true, message: '请选择园区', trigger: 'change' }], spaceName: [{ required: true, message: '请输入空间名称', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res: any = await getSpacePage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword=''; searchForm.parkId=undefined; searchForm.areaId=undefined; searchForm.categoryId=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value=null; isEdit.value=false }
function handleAdd() { resetForm(); dialogTitle.value='新增空间'; dialogVisible.value=true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value=true; currentId.value=row.id; dialogTitle.value='编辑空间'
  try { const res: any = await getSpaceById(row.id); if (res.code === 200) Object.assign(formData, res.data) } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateSpace(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'更新失败')
    } else {
      const res: any = await createSpace(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value=false; loadData() } else ElMessage.error(res.message||'新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除空间「${row.spaceName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteSpace(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { /* cancelled */ }
}

onMounted(() => { loadData(); loadParkOptions(); loadAreaOptions(); loadCategoryOptions() })
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>