<script setup lang="ts">
/**
 * 分区管理 (park-space) - Phase 4 重构
 *
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/area/area.vue
 *
 * 设计: 左侧 el-collapse 园区折叠面板 + 右侧 el-row 分区卡片网格
 * 卡片内容: 房屋图标 + 分区名称 + 功能区域描述 + 占地面积 + 建筑面积 + 楼栋数/房间数
 * 卡片底部: 添加楼栋 + 添加房间 快捷按钮
 * 右上角悬浮: 编辑 / 删除
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
const activeParkId = ref<number | null>(null)

// 新增/编辑 dialog
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingId = ref<number | null>(null)
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
  parkId: undefined as number | undefined,
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

function getAreasByPark(parkId: number): Area[] {
  return areaGroups.value.find((g) => g.park.id === parkId)?.areas || []
}

function getParkName(parkId: number | undefined): string {
  if (!parkId) return '-'
  return parkOptions.value.find((p) => p.id === parkId)?.parkName || '-'
}

function fmtArea(v: number | undefined): string {
  if (v == null) return '-'
  return Number(v).toLocaleString()
}

// 新增
function handleAdd(parkId?: number) {
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
      form.parkId = a.parkId
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
  <div class="page-container area-page">
    <!-- 顶部操作栏 -->
    <div class="header-bar">
      <div>
        <h2 class="page-title">分区管理</h2>
        <span class="page-subtitle">共 {{ flatAreas.length }} 个分区</span>
      </div>
      <el-button type="primary" :icon="'Plus'" @click="handleAdd()">新增分区</el-button>
    </div>

    <div class="area-body">
      <!-- 左侧: 园区折叠面板 -->
      <div class="park-panel">
        <el-collapse v-model="activeParkId" accordion>
          <el-collapse-item
            v-for="group in areaGroups"
            :key="group.park.id"
            :name="group.park.id"
          >
            <template #title>
              <span class="park-title">{{ group.park.parkName }}</span>
              <el-badge :value="group.areas.length" class="park-badge" type="primary" />
            </template>
            <div class="park-content">
              <el-button
                link
                type="primary"
                size="small"
                @click="handleAdd(group.park.id)"
              >
                <el-icon><Plus /></el-icon> 添加分区
              </el-button>
            </div>
          </el-collapse-item>
          <el-empty v-if="!loading && areaGroups.length === 0" description="暂无园区" />
        </el-collapse>
      </div>

      <!-- 右侧: 分区卡片网格 -->
      <div class="card-grid">
        <div v-loading="loading">
          <el-empty
            v-if="flatAreas.length === 0"
            description="暂无分区"
            style="margin-top: 80px"
          />
          <div v-for="group in areaGroups" :key="group.park.id">
            <h3 class="group-title">{{ group.park.parkName }} ({{ group.areas.length }})</h3>
            <el-row :gutter="20" v-if="group.areas.length > 0">
              <el-col
                v-for="area in group.areas"
                :key="area.id"
                :xs="24" :sm="12" :md="8" :lg="6" :xl="6"
                class="card-col"
              >
                <el-card class="area-card" shadow="hover">
                  <!-- 顶部: 图标 + 名称 + 更多操作 -->
                  <div class="card-header">
                    <div class="card-header-left">
                      <el-icon class="card-icon"><OfficeBuilding /></el-icon>
                      <span class="card-title">{{ area.areaName }}</span>
                    </div>
                    <el-dropdown trigger="click" @command="(cmd: string) => {
                      if (cmd === 'edit') handleEdit(area)
                      else if (cmd === 'delete') handleDelete(area)
                    }">
                      <el-icon class="more-icon"><MoreFilled /></el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="edit">
                            <el-icon><Edit /></el-icon> 编辑
                          </el-dropdown-item>
                          <el-dropdown-item command="delete" divided>
                            <el-icon><Delete /></el-icon> 删除
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>

                  <!-- 功能区域描述 -->
                  <div class="card-tag">
                    <el-tag v-if="area.functionArea" size="small" type="info">
                      {{ area.functionArea }}
                    </el-tag>
                    <el-tag v-if="area.isVirtual === 1" size="small" type="warning" effect="plain">
                      虚拟
                    </el-tag>
                  </div>

                  <!-- 面积信息 -->
                  <div class="card-metrics">
                    <div class="metric">
                      <div class="metric-label">占地面积</div>
                      <div class="metric-value">{{ fmtArea(area.areaCovered) }}<span class="metric-unit">㎡</span></div>
                    </div>
                    <div class="metric">
                      <div class="metric-label">建筑面积</div>
                      <div class="metric-value">{{ fmtArea(area.builtArea) }}<span class="metric-unit">㎡</span></div>
                    </div>
                  </div>

                  <div class="card-counts">
                    <span>建筑 <b>{{ area.buildingAmount || 0 }}</b> 栋</span>
                    <span class="divider">|</span>
                    <span>房间 <b>{{ area.roomAmount || 0 }}</b> 间</span>
                  </div>

                  <!-- 底部快捷按钮 -->
                  <div class="card-actions">
                    <el-button size="small" plain :icon="'OfficeBuilding'" @click="handleAddBuilding(area)">
                      添加楼栋
                    </el-button>
                    <el-button size="small" plain :icon="'House'" @click="handleAddRoom(area)">
                      添加房间
                    </el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>
    </div>

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
.area-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}
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
  display: inline-block;
  margin-right: 12px;
}
.page-subtitle {
  color: #909399;
  font-size: 13px;
}
.area-body {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
}
.park-panel {
  width: 280px;
  background: #fff;
  border-radius: 8px;
  overflow-y: auto;
  flex-shrink: 0;
}
.park-title {
  font-weight: 500;
  margin-right: 8px;
}
.park-badge {
  margin-left: 4px;
}
.park-content {
  padding: 0 12px 8px;
}
.card-grid {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.group-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 12px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.card-col {
  margin-bottom: 16px;
}
.area-card {
  border-radius: 8px;
  transition: all 0.2s;
}
.area-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-icon {
  font-size: 20px;
  color: #409eff;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.more-icon {
  cursor: pointer;
  color: #909399;
  padding: 4px;
}
.more-icon:hover {
  color: #409eff;
}
.card-tag {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  min-height: 22px;
}
.card-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
}
.metric-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.metric-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.metric-unit {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 2px;
}
.card-counts {
  text-align: center;
  font-size: 13px;
  color: #606266;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}
.card-counts .divider {
  color: #dcdfe6;
  margin: 0 8px;
}
.card-counts b {
  color: #409eff;
  font-weight: 600;
}
.card-actions {
  display: flex;
  gap: 8px;
}
.card-actions :deep(.el-button) {
  flex: 1;
}
</style>
