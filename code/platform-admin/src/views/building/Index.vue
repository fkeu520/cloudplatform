<script setup lang="ts">
/**
 * 楼宇管理 (park-property) - 列表重构
 *
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/area/building/index.vue + add.vue
 *
 * 设计: 搜索栏 + el-table 表格 + 弹窗(含楼层内嵌子表)
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBuildingPage,
  getBuildingById,
  createBuilding,
  updateBuilding,
  deleteBuilding,
  checkBuildingCode,
  type Building,
} from '@/api/building'
import { getAreaPage, type Area } from '@/api/area'
import { getParkList, type Park } from '@/api/park'
import {
  listFloorByBuilding,
  type Floor,
} from '@/api/floor'
import { getDictDataByType } from '@/api/dict'

// 状态
const loading = ref(false)
const tableData = ref<Building[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const parkOptions = ref<Park[]>([])
const areaOptions = ref<Area[]>([])
const dictMap = reactive<Record<string, Array<{ value: string; label: string }>>>({})
const activeParkId = ref<string | null>(null)

const searchForm = reactive({
  keyword: '',
  areaId: undefined as string | undefined,
  status: undefined as number | undefined,
})

// 新增/编辑 dialog
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()
const imageFiles = ref<string[]>([])

const defaultForm = () => ({
  parkId: undefined as string | undefined,
  areaId: undefined as string | undefined,
  buildingCode: '',
  buildingNo: '',
  buildingName: '',
  floorNumber: 1,
  underground: 0,
  areaCovered: undefined as number | undefined,
  propertyRight: undefined as number | undefined,
  buildingSafety: undefined as number | undefined,
  shareArea: undefined as number | undefined,
  leaseMethod: undefined as number | undefined,
  sorting: 0,
  certificate: '',
  buildYear: undefined as number | undefined,
  manager: '',
  managerPhone: '',
  remark: '',
  status: 1,
})
const form = reactive(defaultForm())

const formRules = {
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  areaId: [{ required: true, message: '请选择区域', trigger: 'change' }],
  buildingCode: [
    { required: true, message: '请输入楼栋编号', trigger: 'blur' },
    { max: 64, message: '楼栋编号不超过 64 字符', trigger: 'blur' },
    {
      validator: async (_rule: any, value: string, callback: any) => {
        if (!value || !form.parkId) return callback()
        try {
          const res: any = await checkBuildingCode(form.parkId, value, editingId.value || undefined)
          if (res.code === 200 && res.data === false) {
            return callback(new Error(`园区下已存在楼栋编号 "${value}"`))
          }
          return callback()
        } catch {
          return callback()
        }
      },
      trigger: 'blur',
    },
  ],
  buildingName: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }],
  floorNumber: [{ type: 'number', required: true, message: '请输入地上层数', trigger: 'blur' }],
  areaCovered: [{ required: true, message: '请输入建筑面积', trigger: 'blur' }],
}

// 楼层子表
const floorList = ref<Floor[]>([])
const floorLoading = ref(false)
const floorDialogVisible = ref(false)
const editingFloor = reactive({
  id: undefined as number | undefined,
  floorName: '',
  serialCode: 1,
  floorCategory: 0,
  coefficient: 1.0,
  sorting: 0,
})
const floorRules = {
  floorName: [{ required: true, message: '请输入楼层名称', trigger: 'blur' }],
  serialCode: [{ type: 'number', required: true, message: '请输入楼层编号', trigger: 'blur' }],
}

async function loadParks() {
  try {
    const res: any = await getParkList()
    if (res.code === 200) {
      parkOptions.value = res.data || []
      if (parkOptions.value.length > 0 && !activeParkId.value) {
        activeParkId.value = parkOptions.value[0].id || null
        // 触发首查
        await loadData()
      }
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadAreas(parkId: string) {
  if (!parkId) {
    areaOptions.value = []
    return
  }
  try {
    const res: any = await getAreaPage({ parkId, pageNum: 1, pageSize: 9999 })
    if (res.code === 200) areaOptions.value = res.data?.records || []
  } catch (e) {
    console.error(e)
  }
}

async function loadDicts() {
  const types = ['property_right', 'building_structure', 'lease_method']
  for (const t of types) {
    try {
      const res: any = await getDictDataByType(t)
      if (res.code === 200) {
        dictMap[t] = (res.data || []).map((d: any) => ({
          value: String(d.dictValue),
          label: d.dictLabel,
        }))
      }
    } catch (e) {
      console.error(e)
    }
  }
}

async function loadData() {
  if (!activeParkId.value) {
    tableData.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res: any = await getBuildingPage({
      ...searchForm,
      parkId: activeParkId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleParkChange(parkId: string) {
  activeParkId.value = parkId
  searchForm.areaId = undefined
  loadAreas(parkId)
  handleSearch()
}

function getAreaName(areaId: string | undefined): string {
  if (!areaId) return '-'
  return areaOptions.value.find((a) => a.id === areaId)?.areaName || '-'
}

// 新增
async function handleAdd() {
  dialogMode.value = 'add'
  editingId.value = null
  Object.assign(form, defaultForm())
  if (activeParkId.value) {
    form.parkId = activeParkId.value
    await loadAreas(activeParkId.value)
  }
  imageFiles.value = []
  floorList.value = []
  dialogVisible.value = true
}

// 编辑
async function handleEdit(b: Building) {
  dialogMode.value = 'edit'
  editingId.value = b.id || null
  try {
    const res: any = await getBuildingById(b.id!)
    if (res.code === 200) {
      // 后端 Area.parkId/areaId 已用 @JsonFormat(STRING) 序列化, 但保险起见显式 String()
      const data = {
        ...res.data,
        parkId: res.data.parkId != null ? String(res.data.parkId) : undefined,
        areaId: res.data.areaId != null ? String(res.data.areaId) : undefined,
      }
      Object.assign(form, defaultForm(), data)
      await loadAreas(form.parkId!)
      // 图片
      if (form.image) {
        try {
          imageFiles.value = JSON.parse(form.image)
        } catch {
          imageFiles.value = []
        }
      } else {
        imageFiles.value = []
      }
      // 加载楼层
      await loadFloorsByBuilding(b.id!)
      dialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('加载楼栋详情失败')
  }
}

async function loadFloorsByBuilding(buildingId: number) {
  floorLoading.value = true
  try {
    const res: any = await listFloorByBuilding(buildingId)
    if (res.code === 200) {
      floorList.value = res.data || []
    }
  } finally {
    floorLoading.value = false
  }
}

// 删除
async function handleDelete(b: Building) {
  try {
    await ElMessageBox.confirm(
      `确认要删除楼栋 "${b.buildingName}" 吗？\n（注：楼栋下已有房间数据时不可删除）`,
      '删除确认',
      { type: 'warning' }
    )
    const res: any = await deleteBuilding(b.id!)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

// 提交楼栋表单
async function handleSubmit() {
  await formRef.value?.validate()
  if (form.areaCovered && form.shareArea && Number(form.shareArea) > Number(form.areaCovered)) {
    ElMessage.error('公摊面积不能大于建筑面积')
    return
  }
  submitting.value = true
  try {
    const payload: any = { ...form }
    // 图片: 多张以 JSON 数组形式存储
    if (imageFiles.value.length > 0) {
      payload.image = JSON.stringify(imageFiles.value)
    } else {
      payload.image = null
    }
    // 总楼层数 = 地上 + 地下 (兼容旧字段)
    payload.floors = (payload.floorNumber || 0) + (payload.underground || 0)
    // 同步 buildingNo (兼容字段)
    if (payload.buildingCode && !payload.buildingNo) {
      payload.buildingNo = payload.buildingCode
    }
    const res: any = dialogMode.value === 'edit'
      ? await updateBuilding(editingId.value!, payload)
      : await createBuilding(payload)
    if (res.code === 200) {
      ElMessage.success(dialogMode.value === 'edit' ? '更新成功' : '新增成功')
      dialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e: any) {
    if (e?.msg) ElMessage.error(e.msg)
  } finally {
    submitting.value = false
  }
}

// 楼层子表 - 新增/编辑
function openFloorDialog(floor?: Floor) {
  Object.assign(editingFloor, {
    id: undefined,
    floorName: '',
    serialCode: 1,
    floorCategory: 0,
    coefficient: 1.0,
    sorting: 0,
  })
  if (floor) {
    Object.assign(editingFloor, floor)
  }
  floorDialogVisible.value = true
}

async function saveFloor() {
  if (!editingFloor.floorName) {
    ElMessage.warning('请输入楼层名称')
    return
  }
  // 前端 inline 保存: 直接更新本地 floorList, 在 dialog 关闭时统一提交
  if (editingFloor.id) {
    const idx = floorList.value.findIndex((f) => f.id === editingFloor.id)
    if (idx >= 0) Object.assign(floorList.value[idx], editingFloor)
  } else {
    const newFloor: Floor = {
      ...editingFloor,
      id: undefined,
      buildingId: editingId.value || 0,
      parkId: form.parkId,
      status: 1,
      createTime: undefined,
    } as any
    floorList.value.push(newFloor)
  }
  floorDialogVisible.value = false
  ElMessage.success('楼层已暂存 (提交楼栋时统一保存)')
}

function deleteFloor(floor: Floor) {
  ElMessageBox.confirm(`确认删除楼层 "${floor.floorName}"?`, '提示', { type: 'warning' })
    .then(() => {
      floorList.value = floorList.value.filter((f) => f !== floor)
    })
    .catch(() => {})
}

// 导入
function handleImport() {
  ElMessage.info('批量导入功能开发中, 敬请期待')
}

onMounted(async () => {
  await loadDicts()
  await loadParks()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="园区">
          <el-select
            :model-value="activeParkId"
            placeholder="全部园区"
            clearable
            filterable
            style="width: 200px"
            @change="handleParkChange"
          >
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="searchForm.areaId" placeholder="全部区域" clearable filterable style="width: 180px">
            <el-option v-for="a in areaOptions" :key="a.id" :label="a.areaName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="楼栋名称/编号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button @click="searchForm.keyword=''; searchForm.areaId=undefined; searchForm.status=undefined; handleSearch()">重置</el-button>
          <el-button type="success" :icon="'Plus'" @click="handleAdd">新增楼栋</el-button>
          <el-button type="warning" :icon="'Upload'" plain @click="handleImport">导入</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 楼栋表格 -->
    <el-card class="table-card" v-loading="loading">
      <el-empty v-if="!loading && tableData.length === 0" description="暂无楼栋" />
      <el-table :data="tableData" border stripe v-if="tableData.length > 0">
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="buildingName" label="楼栋名称" min-width="140" />
        <el-table-column label="楼栋编号" width="140">
          <template #default="scope">{{ scope.row.buildingCode || scope.row.buildingNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="所属园区" width="140">
          <template #default="scope">{{ parkOptions.find(p => p.id === String(scope.row.parkId))?.parkName || scope.row.parkId }}</template>
        </el-table-column>
        <el-table-column label="所属区域" width="120">
          <template #default="scope">{{ getAreaName(scope.row.areaId) }}</template>
        </el-table-column>
        <el-table-column label="楼层" width="120" align="center">
          <template #default="scope">
            地上 {{ scope.row.floorNumber ?? scope.row.floors ?? '-' }} 层
            <template v-if="scope.row.underground && scope.row.underground > 0">
              + 地下 {{ scope.row.underground }} 层
            </template>
          </template>
        </el-table-column>
        <el-table-column label="建筑面积" width="120" align="right">
          <template #default="scope">{{ scope.row.areaCovered ? Number(scope.row.areaCovered).toLocaleString() : '-' }} ㎡</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑 dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑楼栋' : '新增楼栋'"
      width="960px"
      :close-on-click-modal="false"
      top="5vh"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <h4 class="form-section-title">基础信息</h4>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属园区" prop="parkId">
              <el-select v-model="form.parkId" placeholder="请选择园区" filterable :disabled="dialogMode==='edit'" @change="loadAreas(form.parkId!)">
                <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属区域" prop="areaId">
              <el-select v-model="form.areaId" placeholder="请选择区域" filterable>
                <el-option v-for="a in areaOptions" :key="a.id" :label="a.areaName" :value="a.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="楼栋编号" prop="buildingCode">
              <el-input v-model="form.buildingCode" maxlength="64" show-word-limit placeholder="园区内唯一" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼栋名称" prop="buildingName">
              <el-input v-model="form.buildingName" maxlength="64" show-word-limit placeholder="如：A 座" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="地上层数" prop="floorNumber">
              <el-input-number v-model="form.floorNumber" :min="1" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="地下层数">
              <el-input-number v-model="form.underground" :min="0" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="建筑面积(㎡)" prop="areaCovered">
              <el-input-number v-model="form.areaCovered" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="产权性质">
              <el-select v-model="form.propertyRight" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in dictMap['property_right'] || []" :key="d.value" :label="d.label" :value="Number(d.value)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="建筑结构">
              <el-select v-model="form.buildingSafety" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in dictMap['building_structure'] || []" :key="d.value" :label="d.label" :value="Number(d.value)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="租赁方式">
              <el-select v-model="form.leaseMethod" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in dictMap['lease_method'] || []" :key="d.value" :label="d.label" :value="Number(d.value)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="公摊面积(㎡)">
              <el-input-number v-model="form.shareArea" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产权证号">
              <el-input v-model="form.certificate" maxlength="100" placeholder="产权证书或不动产权证号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sorting" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="建成年份">
              <el-input-number v-model="form.buildYear" :min="1900" :max="2100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="form.manager" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人电话">
              <el-input v-model="form.managerPhone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>

        <!-- 楼栋图片 -->
        <h4 class="form-section-title">楼栋图片 (支持多图上传)</h4>
        <el-form-item label="图片">
          <el-upload
            v-model:file-list="imageFiles"
            list-type="picture-card"
            :auto-upload="false"
            multiple
            accept="image/*"
            :on-change="(file: any) => { imageFiles = imageFiles.concat(file.url || file.response?.data || '') }"
            :on-remove="(file: any) => { imageFiles = imageFiles.filter((u: string) => u !== file.url) }"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="muted small">建议尺寸 800x600px, 支持 jpg/png, 最多 9 张</div>
        </el-form-item>

        <!-- 楼层子表 (内嵌) - 仅编辑模式加载 -->
        <template v-if="dialogMode === 'edit'">
          <h4 class="form-section-title flex-sb">
            <span>楼层管理 ({{ floorList.length }} 层)</span>
            <el-button type="primary" size="small" :icon="'Plus'" plain @click="openFloorDialog()">添加楼层</el-button>
          </h4>
          <el-table :data="floorList" border size="small" v-loading="floorLoading" max-height="240">
            <el-table-column label="序号" type="index" width="50" align="center" />
            <el-table-column prop="serialCode" label="楼层编号" width="100" align="center" />
            <el-table-column prop="floorName" label="楼层名称" min-width="120" />
            <el-table-column prop="floorCategory" label="楼层类型" width="100" align="center">
              <template #default="scope">
                <el-tag size="small" :type="scope.row.floorCategory === 0 ? '' : (scope.row.floorCategory === 1 ? 'info' : 'warning')">
                  {{ scope.row.floorCategory === 0 ? '地上' : (scope.row.floorCategory === 1 ? '地下' : '夹层') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="coefficient" label="楼层系数" width="100" align="center">
              <template #default="scope">{{ Number(scope.row.coefficient || 1).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="sorting" label="排序" width="80" align="center" />
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="scope">
                <el-button size="small" link type="primary" @click="openFloorDialog(scope.row)">编辑</el-button>
                <el-button size="small" link type="danger" @click="deleteFloor(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="muted small" style="margin-top: 8px">
            说明: 楼层数据为前端暂存, 后续可对接 /floor/batch-save 接口统一提交
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存楼栋</el-button>
      </template>
    </el-dialog>

    <!-- 楼层编辑小 dialog -->
    <el-dialog v-model="floorDialogVisible" title="编辑楼层" width="500px" append-to-body>
      <el-form :model="editingFloor" :rules="floorRules" label-width="100px">
        <el-form-item label="楼层名称" prop="floorName">
          <el-input v-model="editingFloor.floorName" maxlength="32" placeholder="如：1 楼 / B1 / M1" />
        </el-form-item>
        <el-form-item label="楼层编号" prop="serialCode">
          <el-input-number v-model="editingFloor.serialCode" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="楼层类型">
          <el-select v-model="editingFloor.floorCategory" style="width: 100%">
            <el-option label="地上" :value="0" />
            <el-option label="地下" :value="1" />
            <el-option label="夹层" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层系数">
          <el-input-number v-model="editingFloor.coefficient" :precision="2" :min="0.01" :step="0.01" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editingFloor.sorting" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="floorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFloor">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
.form-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.flex-sb {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.muted {
  color: #909399;
}
.muted.small {
  font-size: 12px;
}
</style>
