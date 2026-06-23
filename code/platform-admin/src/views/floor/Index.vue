<template>
  <div class="page-container">
    <el-alert
      class="migration-notice"
      type="info"
      :closable="false"
      show-icon
      title="楼层已支持在楼栋管理中按楼栋维护"
      description="Phase 8 (csyh 业务融合): 楼层数据已支持在 [楼栋管理 → 编辑/新增楼栋 → 楼层子表] 处 inline 编辑并保存。本页仍保留为全量楼层查询/编辑入口, 适用于跨楼栋批量管理场景。"
    />
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字"><el-input v-model="searchForm.keyword" placeholder="楼层名称" clearable /></el-form-item>
        <el-form-item label="园区">
          <el-select v-model="searchForm.parkId" placeholder="全部园区" clearable filterable>
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="searchForm.buildingId" placeholder="全部楼栋" clearable filterable>
            <el-option v-for="b in buildingOptions" :key="b.id" :label="b.buildingName" :value="b.id" />
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
          <el-button type="success" @click="handleAdd">新增楼层</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="floorName" label="楼层名称" width="120" />
        <el-table-column prop="serialCode" label="楼层序号" width="100" />
        <el-table-column label="楼层类型" width="120">
          <template #default="scope">{{ floorCategoryLabel(scope.row.floorCategory) }}</template>
        </el-table-column>
        <el-table-column prop="coefficient" label="楼层系数" width="100" align="right" />
        <el-table-column label="园区" width="120">
          <template #default="scope">{{ parkMap[scope.row.parkId] || scope.row.parkId }}</template>
        </el-table-column>
        <el-table-column label="楼栋" width="120">
          <template #default="scope">{{ buildingMap[scope.row.buildingId] || scope.row.buildingId }}</template>
        </el-table-column>
        <el-table-column prop="sorting" label="排序" width="80" />
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
        <el-form-item label="楼栋" prop="buildingId">
          <el-select v-model="formData.buildingId" placeholder="请选择楼栋" filterable>
            <el-option v-for="b in buildingOptions" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层名称" prop="floorName"><el-input v-model="formData.floorName" maxlength="64" /></el-form-item>
        <el-form-item label="楼层序号" prop="serialCode"><el-input-number v-model="formData.serialCode" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="楼层类型" prop="floorCategory">
          <el-select v-model="formData.floorCategory">
            <el-option label="地上" :value="0" /><el-option label="地下" :value="1" /><el-option label="夹层" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层系数" prop="coefficient"><el-input-number v-model="formData.coefficient" :precision="2" :step="0.1" :min="0" style="width:100%" /></el-form-item>
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
import { getFloorPage, getFloorById, createFloor, updateFloor, deleteFloor } from '@/api/floor'
import { getParkList } from '@/api/park'
import { getBuildingPage } from '@/api/building'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ keyword: '', parkId: undefined as number | undefined, buildingId: undefined as number | undefined, status: undefined as number | undefined })
const dialogVisible = ref(false); const dialogTitle = ref(''); const isEdit = ref(false)
const currentId = ref<number | null>(null); const submitting = ref(false); const formRef = ref()
const parkOptions = ref<any[]>([]); const parkMap = ref<Record<number, string>>({})
const buildingOptions = ref<any[]>([]); const buildingMap = ref<Record<number, string>>({})

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) { parkOptions.value = res.data || []; parkOptions.value.forEach((p: any) => parkMap.value[p.id] = p.parkName) } } catch { /* ignore */ }
}
async function loadBuildingOptions() {
  try { const res: any = await getBuildingPage({ pageNum: 1, pageSize: 9999 }); if (res.code === 200) { buildingOptions.value = res.data.records || []; buildingOptions.value.forEach((b: any) => buildingMap.value[b.id] = b.buildingName) } } catch { /* ignore */ }
}

const defaultForm = { parkId: undefined as number | undefined, buildingId: undefined as number | undefined, floorName: '', serialCode: 1, floorCategory: 0, coefficient: 1.0, sorting: 0, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = {
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  floorName: [{ required: true, message: '请输入楼层名称', trigger: 'blur' }],
  serialCode: [{ required: true, message: '请输入楼层序号', trigger: 'blur' }],
  floorCategory: [{ required: true, message: '请选择楼层类型', trigger: 'change' }],
  coefficient: [{ required: true, message: '请输入楼层系数', trigger: 'blur' }]
}

function floorCategoryLabel(c: number): string { return ['地上', '地下', '夹层'][c] || '-' }

async function loadData() {
  loading.value = true
  try {
    const res: any = await getFloorPage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.keyword = ''; searchForm.parkId = undefined; searchForm.buildingId = undefined; searchForm.status = undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }
function resetForm() { Object.assign(formData, { ...defaultForm }); currentId.value = null; isEdit.value = false }
function handleAdd() { resetForm(); dialogTitle.value = '新增楼层'; dialogVisible.value = true }
async function handleEdit(row: any) {
  resetForm(); isEdit.value = true; currentId.value = row.id; dialogTitle.value = '编辑楼层'
  try { const res: any = await getFloorById(row.id); if (res.code === 200) Object.assign(formData, res.data) } catch { ElMessage.error('获取详情失败') }
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updateFloor(currentId.value, formData)
      if (res.code === 200) { ElMessage.success('更新成功'); dialogVisible.value = false; loadData() } else ElMessage.error(res.message || '更新失败')
    } else {
      const res: any = await createFloor(formData)
      if (res.code === 200) { ElMessage.success('新增成功'); dialogVisible.value = false; loadData() } else ElMessage.error(res.message || '新增失败')
    }
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除楼层「${row.floorName}」吗？`, '警告', { type: 'warning' })
    const res: any = await deleteFloor(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { /* cancelled */ }
}

onMounted(() => { loadData(); loadParkOptions(); loadBuildingOptions() })
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.migration-notice {
  margin-bottom: 16px;
}
</style>