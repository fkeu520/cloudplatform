<script setup lang="ts">
/**
 * 分区管理 (park-space) - 列表重构
 *
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/area/area.vue
 *
 * 设计: 搜索栏 (园区筛选) + el-table 表格
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAreaPage,
  getAreaById,
  createArea,
  updateArea,
  deleteArea,
  checkAreaName,
  type Area,
} from '@/api/area'
import { getParkList, type Park } from '@/api/park'

// 状态
const loading = ref(false)
const parkOptions = ref<Park[]>([])
const areaGroups = ref<{ park: Park; areas: Area[] }[]>([])
const activeParkId = ref<string | null>(null)

// 新增/编辑 dialog
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  areaName: '',
  areaCovered: undefined as number | undefined,
  builtArea: undefined as number | undefined,
  functionArea: '',
  isVirtual: 0,
  sorting: 0,
  status: 1,
  parkId: undefined as string | undefined,
})

const formRules = {
  areaName: [
    { required: true, message: '请输入分区名称', trigger: 'blur' },
    { max: 64, message: '分区名称不超过 64 字符', trigger: 'blur' },
    {
      validator: async (rule: any, value: string, callback: any) => {
        if (!value || !form.parkId) return callback()
        try {
          const res: any = await checkAreaName(form.parkId, value, editingId.value || undefined)
          if (res.code === 200 && res.data === false) {
            return callback(new Error(`园区下已存在分区 "${value}"`))
          }
          return callback()
        } catch {
          return callback()
        }
      },
      trigger: 'blur',
    },
  ],
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  sorting: [{ type: 'number', message: '排序必须为数字', trigger: 'blur' }],
}

// 加载园区列表
async function loadParks() {
  try {
    const res: any = await getParkList()
    if (res.code === 200) {
      parkOptions.value = res.data || []
      if (parkOptions.value.length > 0 && !activeParkId.value) {
        activeParkId.value = parkOptions.value[0].id || null
      }
      await loadAllAreas()
    }
  } catch (e) {
    console.error('loadParks error', e)
  }
}

// 加载所有分区分组到园区
async function loadAllAreas() {
  loading.value = true
  try {
    const tasks = parkOptions.value.map(async (p) => {
      const res: any = await getAreaPage({
        parkId: p.id,
        pageNum: 1,
        pageSize: 9999,
      })
      return { park: p, areas: res.data?.records || [] }
    })
    areaGroups.value = await Promise.all(tasks)
  } catch (e) {
    console.error('loadAllAreas error', e)
  } finally {
    loading.value = false
  }
}

const flatAreas = computed(() => areaGroups.value.flatMap((g) => g.areas))

function getAreasByPark(parkId: string): Area[] {
  return areaGroups.value.find((g) => g.park.id === parkId)?.areas || []
}

function getParkName(parkId: string | undefined): string {
  if (!parkId) return '-'
  return parkOptions.value.find((p) => p.id === parkId)?.parkName || '-'
}

function fmtArea(v: number | undefined): string {
  if (v == null) return '-'
  return Number(v).toLocaleString()
}

// 新增
function handleAdd(parkId?: string) {
  dialogMode.value = 'add'
  editingId.value = null
  form.areaName = ''
  form.areaCovered = undefined
  form.builtArea = undefined
  form.functionArea = ''
  form.isVirtual = 0
  form.sorting = 0
  form.status = 1
  form.parkId = parkId ?? activeParkId.value ?? undefined
  dialogVisible.value = true
}

// 编辑
async function handleEdit(area: Area) {
  dialogMode.value = 'edit'
  editingId.value = area.id || null
  try {
    const res: any = await getAreaById(area.id!)
    if (res.code === 200) {
      const a = res.data
      form.areaName = a.areaName || ''
      form.areaCovered = a.areaCovered
      form.builtArea = a.builtArea
      form.functionArea = a.functionArea || ''
      form.isVirtual = a.isVirtual ?? 0
      form.sorting = a.sorting ?? 0
      form.status = a.status ?? 1
      form.parkId = a.parkId != null ? String(a.parkId) : undefined
      dialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('加载分区详情失败')
  }
}

// 删除
async function handleDelete(area: Area) {
  try {
    await ElMessageBox.confirm(
      `确认要删除分区 "${area.areaName}" 吗？\n（注：已产生业务数据时不可删除）`,
      '删除确认',
      { type: 'warning' }
    )
    const res: any = await deleteArea(area.id!)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadAllAreas()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

// 提交表单
async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      parkId: form.parkId,
      areaName: form.areaName,
      areaCovered: form.areaCovered,
      builtArea: form.builtArea,
      functionArea: form.functionArea,
      isVirtual: form.isVirtual,
      sorting: form.sorting,
      status: form.status,
    }
    const res: any = dialogMode.value === 'edit'
      ? await updateArea(editingId.value!, payload)
      : await createArea(payload)
    if (res.code === 200) {
      ElMessage.success(dialogMode.value === 'edit' ? '更新成功' : '新增成功')
      dialogVisible.value = false
      await loadAllAreas()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e: any) {
    if (e?.msg) ElMessage.error(e.msg)
  } finally {
    submitting.value = false
  }
}

// 添加楼栋/房间 (快捷按钮占位, 跳转到对应模块并预选 area)
function handleAddBuilding(area: Area) {
  ElMessage.info('请到 "楼宇列表" 页面 (property menu) 新建楼栋并关联此分区')
}
function handleAddRoom(area: Area) {
  ElMessage.info('请到 "房间管理" 页面 (room menu) 新建房间并关联此分区')
}

onMounted(() => {
  loadParks()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部操作栏 -->
    <div class="header-bar">
      <div>
        <h2 class="page-title">分区管理</h2>
      </div>
      <div class="header-actions">
        <el-select
          v-model="activeParkId"
          placeholder="全部园区"
          clearable
          filterable
          style="width: 200px"
          @change="loadAllAreas"
        >
          <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
        </el-select>
        <el-button type="primary" :icon="'Plus'" @click="handleAdd()">新增分区</el-button>
      </div>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="flatAreas" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="areaName" label="分区名称" min-width="140" />
        <el-table-column label="所属园区" width="140">
          <template #default="scope">{{ getParkName(scope.row.parkId) }}</template>
        </el-table-column>
        <el-table-column prop="functionArea" label="功能区域" width="140" :show-overflow-tooltip="true" />
        <el-table-column label="占地面积" width="120" align="right">
          <template #default="scope">{{ fmtArea(scope.row.areaCovered) }} ㎡</template>
        </el-table-column>
        <el-table-column label="建筑面积" width="120" align="right">
          <template #default="scope">{{ fmtArea(scope.row.builtArea) }} ㎡</template>
        </el-table-column>
        <el-table-column label="虚拟" width="80" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.isVirtual === 1" size="small" type="warning">虚拟</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="楼栋/房间" width="120" align="center">
          <template #default="scope">
            {{ scope.row.buildingAmount || 0 }}栋 / {{ scope.row.roomAmount || 0 }}间
          </template>
        </el-table-column>
        <el-table-column prop="sorting" label="排序" width="80" align="center" />
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
    </el-card>

    <!-- 新增/编辑 dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑分区' : '新增分区'"
      width="600px"
      :close-on-click-modal="false"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="所属园区" prop="parkId">
          <el-select v-model="form.parkId" placeholder="请选择园区" filterable style="width: 100%">
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分区名称" prop="areaName">
          <el-input v-model="form.areaName" maxlength="64" show-word-limit placeholder="尽量控制在 6 字以内" />
        </el-form-item>
        <el-form-item label="功能区域" prop="functionArea">
          <el-input v-model="form.functionArea" maxlength="64" placeholder="如：综合办公区、商务配套区" />
        </el-form-item>
        <el-form-item label="占地面积">
          <el-input v-model.number="form.areaCovered" type="number" placeholder="㎡">
            <template #append>㎡</template>
          </el-input>
        </el-form-item>
        <el-form-item label="建筑面积">
          <el-input v-model.number="form.builtArea" type="number" placeholder="㎡">
            <template #append>㎡</template>
          </el-input>
        </el-form-item>
        <el-form-item label="虚拟区域">
          <el-radio-group v-model="form.isVirtual">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sorting" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.table-card {
  flex: 1;
  overflow: auto;
}
</style>
