<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="园区名称/地址" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd" v-permission="'system:park:add'">新增园区</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="parkName" label="园区名称" width="160" />
        <el-table-column label="省市区" width="200">
          <template #default="scope">
            {{ [scope.row.province, scope.row.city, scope.row.district].filter(Boolean).join(' / ') }}
          </template>
        </el-table-column>
        <el-table-column prop="address" label="详细地址" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="landArea" label="占地面积(m²)" width="130" align="right">
          <template #default="scope">
            {{ scope.row.landArea ? scope.row.landArea.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="buildingArea" label="建筑面积(m²)" width="130" align="right">
          <template #default="scope">
            {{ scope.row.buildingArea ? scope.row.buildingArea.toLocaleString() : '-' }}
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
            <el-button type="primary" size="small" @click="handleEdit(scope.row)" v-permission="'system:park:edit'">编辑</el-button>
            <el-button
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(scope.row)"
              v-permission="'system:park:edit'"
            >
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)" v-permission="'system:park:del'">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form :model="formData" label-width="120px" :rules="rules" ref="formRef" :validate-on-rule-change="false">
        <el-form-item label="园区名称" prop="parkName">
          <el-input v-model="formData.parkName" placeholder="请输入园区名称" maxlength="100" />
        </el-form-item>

        <el-form-item label="所属区域" prop="region">
          <el-cascader
            v-model="regionValue"
            :options="regionOptions"
            :props="{ label: 'name', value: 'name', children: 'children', checkStrictly: false, expandTrigger: 'hover' }"
            placeholder="请选择省/市/区"
            clearable
            style="width: 100%"
            @change="onRegionChange"
          />
        </el-form-item>

        <el-form-item label="详细地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入详细地址" maxlength="255" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="formData.longitude" :precision="7" :step="0.01" :min="-180" :max="180" placeholder="经度" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="formData.latitude" :precision="7" :step="0.01" :min="-90" :max="90" placeholder="纬度" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="占地面积(m²)" prop="landArea">
          <el-input-number v-model="formData.landArea" :precision="2" :step="100" :min="0" placeholder="占地面积" style="width: 100%" />
        </el-form-item>

        <el-form-item label="建筑面积(m²)" prop="buildingArea">
          <el-input-number v-model="formData.buildingArea" :precision="2" :step="100" :min="0" placeholder="建筑面积" style="width: 100%" />
        </el-form-item>

        <el-form-item label="园区简介" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入园区简介" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
  getParkPage,
  getParkById,
  createPark,
  updatePark,
  deletePark,
  toggleParkStatus,
  getRegionTree,
  type Park,
  type RegionNode
} from '@/api/park'

// ========== 状态 ==========
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchForm = reactive({ keyword: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()
const regionOptions = ref<RegionNode[]>([])
const regionValue = ref<string[]>([])

const defaultForm = {
  parkName: '',
  province: undefined,
  city: undefined,
  district: undefined,
  address: '',
  longitude: undefined,
  latitude: undefined,
  description: '',
  landArea: undefined,
  buildingArea: undefined,
  status: 1
}
const formData = reactive<Park>({ ...defaultForm })

const rules = {
  parkName: [
    { required: true, message: '请输入园区名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过100个字符', trigger: 'blur' }
  ],
  address: [{ max: 255, message: '地址不能超过255个字符', trigger: 'blur' }],
  longitude: [{ type: 'number', message: '经度必须为数字', trigger: 'blur' }],
  latitude: [{ type: 'number', message: '纬度必须为数字', trigger: 'blur' }],
  landArea: [{ type: 'number', message: '占地面积必须为数字', trigger: 'blur' }],
  buildingArea: [{ type: 'number', message: '建筑面积必须为数字', trigger: 'blur' }]
}

// ========== 加载省市区数据 ==========
async function loadRegionTree() {
  try {
    const res: any = await getRegionTree()
    regionOptions.value = res.data || []
  } catch (e) {
    console.warn('加载省市区数据失败', e)
    regionOptions.value = []
  }
}

// ========== 数据加载 ==========
async function loadData() {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    const res: any = await getParkPage(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  loadData()
}

function handlePageChange(val: number) {
  pageNum.value = val
  loadData()
}

// ========== 弹窗操作 ==========
function resetForm() {
  Object.assign(formData, { ...defaultForm })
  regionValue.value = []
  currentId.value = null
  isEdit.value = false
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增园区'
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  resetForm()
  isEdit.value = true
  currentId.value = row.id
  dialogTitle.value = '编辑园区'
  try {
    const res: any = await getParkById(row.id)
    if (res.code === 200) {
      const data = res.data
      Object.assign(formData, {
        parkName: data.parkName,
        province: data.province,
        city: data.city,
        district: data.district,
        address: data.address,
        longitude: data.longitude,
        latitude: data.latitude,
        description: data.description,
        landArea: data.landArea,
        buildingArea: data.buildingArea,
        status: data.status ?? 1
      })
      // 回填省市区级联
      regionValue.value = [data.province, data.city, data.district].filter(Boolean)
    }
  } catch (e) {
    ElMessage.error('获取园区详情失败')
  }
  dialogVisible.value = true
}

function onRegionChange(val: string[]) {
  if (val && val.length > 0) {
    formData.province = val[0] || undefined
    formData.city = val[1] || undefined
    formData.district = val[2] || undefined
  } else {
    formData.province = undefined
    formData.city = undefined
    formData.district = undefined
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && currentId.value) {
      const res: any = await updatePark(currentId.value, formData as Park)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.message || '更新失败')
      }
    } else {
      const res: any = await createPark(formData as Park)
      if (res.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.message || '新增失败')
      }
    }
  } finally {
    submitting.value = false
  }
}

// ========== 行操作 ==========
async function handleToggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${label}园区「${row.parkName}」吗？`, '提示')
    const res: any = await toggleParkStatus(row.id, newStatus)
    if (res.code === 200) {
      ElMessage.success(`${label}成功`)
      loadData()
    }
  } catch {
    // cancelled
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除园区「${row.parkName}」吗？删除后不可恢复。`, '警告', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res: any = await deletePark(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // cancelled
  }
}

// ========== 初始化 ==========
onMounted(() => {
  loadData()
  loadRegionTree()
})
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
