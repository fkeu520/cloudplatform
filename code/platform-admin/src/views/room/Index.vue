<script setup lang="ts">
/**
 * 房间管理 (park-space) - Phase 6 重构
 *
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/room/index.vue + bar/index.vue + detail.vue
 *
 * 设计: 左侧树形导航 (园区 → 楼栋 → 楼层) + 右侧卡片网格
 * 卡片: 状态徽章 + 房间名 + 用途标签 + 建筑面积 + 套内面积
 * 顶部: 房间导入按钮 + 状态 radio filter + 关键字搜索
 * 弹窗: 完整字段 (parkId/buildingId/floorId/roomNo/roomName/houseStructure/areaCovered/buildArea/...)
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoomPage,
  getRoomById,
  createRoom,
  updateRoom,
  deleteRoom,
  checkRoomNo,
  type Room,
} from '@/api/room'
import { getParkList, type Park } from '@/api/park'
import {
  getBuildingPage,
  type Building,
} from '@/api/building'
import { listFloorByBuilding, type Floor } from '@/api/floor'
import { getKitPage, type Kit } from '@/api/kit'
import { getRoomPurposePage, type RoomPurpose } from '@/api/room-purpose'
import { getDictDataByType } from '@/api/dict'

// ============== 状态 ==============
const loading = ref(false)
const cardsData = ref<Room[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(50)
const pageSizes = [10, 50, 100, 150]

// 树形导航状态
interface ParkTreeNode extends Park {
  $buildingList?: BuildingTreeNode[]
  $loading?: boolean
  $loaded?: boolean
}
interface BuildingTreeNode extends Building {
  $floorList?: Floor[]
  $loading?: boolean
  $loaded?: boolean
}
const parkTree = ref<ParkTreeNode[]>([])
const activeParkId = ref<number | null>(null)
const activeBuildingId = ref<number | null>(null)
const activeFloorId = ref<number | null>(null)

// 搜索/过滤
const filterStatus = ref<string>('')  // '' = 全部
const filterText = ref('')

// 字典
const dictMap = reactive<Record<string, Array<{ value: string; label: string }>>>({})

// 弹窗
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit' | 'view'>('add')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref()

// 配套/用途 options
const kitOptions = ref<Kit[]>([])
const purposeOptions = ref<RoomPurpose[]>([])

const defaultForm = () => ({
  id: undefined as number | undefined,
  parkId: undefined as number | undefined,
  buildingId: undefined as number | undefined,
  floorId: undefined as number | undefined,
  floor: 1,
  roomNo: '',
  roomName: '',
  houseStructure: undefined as number | undefined,
  areaCovered: undefined as number | undefined,
  buildArea: undefined as number | undefined,
  billableArea: undefined as number | undefined,
  unitPrice: undefined as number | undefined,
  totalPrice: undefined as number | undefined,
  monthlyRent: undefined as number | undefined,
  kitId: undefined as number | undefined,
  purposeId: undefined as number | undefined,
  sorting: 0,
  status: 0,
  remark: '',
})
const form = reactive(defaultForm())

const formRules = {
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  roomNo: [
    { required: true, message: '请输入房号', trigger: 'blur' },
    { max: 64, message: '房号不超过 64 字符', trigger: 'blur' },
    {
      validator: async (_rule: any, value: string, callback: any) => {
        if (!value || !form.parkId || !form.buildingId) return callback()
        try {
          const res: any = await checkRoomNo(form.parkId, form.buildingId, value, editingId.value || undefined)
          if (res.code === 200 && res.data === false) {
            return callback(new Error(`楼栋下已存在房号 "${value}"`))
          }
          return callback()
        } catch {
          return callback()
        }
      },
      trigger: 'blur',
    },
  ],
  roomName: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
  areaCovered: [{ required: true, message: '请输入建筑面积', trigger: 'blur' }],
}

// ============== 数据加载 ==============
async function loadParks() {
  try {
    const res: any = await getParkList({ pageNum: 1, pageSize: 9999 })
    if (res.code === 200) {
      parkTree.value = (res.data?.records || []).map((p: Park) => ({ ...p, $loaded: false }))
      if (parkTree.value.length > 0 && !activeParkId.value) {
        activeParkId.value = parkTree.value[0].id || null
        // 首次默认加载第一个园区的楼栋
        await loadBuildingsForPark(parkTree.value[0])
        if (activeBuildingId.value) {
          const building = parkTree.value[0].$buildingList?.find((b) => b.id === activeBuildingId.value)
          if (building && !building.$loaded) {
            await loadFloorsForBuilding(building)
          }
        }
      }
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadBuildingsForPark(park: ParkTreeNode) {
  if (park.$loaded) return
  park.$loading = true
  try {
    const res: any = await getBuildingPage({ parkId: park.id, pageNum: 1, pageSize: 9999 })
    if (res.code === 200) {
      park.$buildingList = (res.data?.records || []).map((b: Building) => ({ ...b, $loaded: false }))
      park.$loaded = true
      // 默认选中第一个楼栋
      if (park.$buildingList.length > 0 && !activeBuildingId.value) {
        activeBuildingId.value = park.$buildingList[0].id || null
        await loadFloorsForBuilding(park.$buildingList[0])
        // 默认选中第一个楼层
        if (activeFloorId.value === null && park.$buildingList[0].$floorList?.length) {
          activeFloorId.value = park.$buildingList[0].$floorList[0].id || null
        }
      }
    }
  } finally {
    park.$loading = false
  }
}

async function loadFloorsForBuilding(building: BuildingTreeNode) {
  if (building.$loaded) return
  building.$loading = true
  try {
    const res: any = await listFloorByBuilding(building.id!)
    if (res.code === 200) {
      building.$floorList = res.data || []
      building.$loaded = true
    }
  } finally {
    building.$loading = false
  }
}

// 节点点击处理
async function onParkClick(park: ParkTreeNode) {
  activeParkId.value = park.id || null
  if (!park.$loaded) {
    await loadBuildingsForPark(park)
  }
}

async function onBuildingClick(building: BuildingTreeNode) {
  activeBuildingId.value = building.id || null
  if (!building.$loaded) {
    await loadFloorsForBuilding(building)
  }
}

function onFloorClick(floor: Floor) {
  activeFloorId.value = floor.id || null
  pageNum.value = 1
  loadData()
}

async function loadDicts() {
  const types = ['room_status', 'room_structure', 'room_type']
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

async function loadKitsAndPurposes() {
  if (kitOptions.value.length === 0) {
    try {
      const res: any = await getKitPage({ pageNum: 1, pageSize: 9999 })
      if (res.code === 200) kitOptions.value = res.data?.records || []
    } catch (e) {
      console.error(e)
    }
  }
  if (purposeOptions.value.length === 0) {
    try {
      const res: any = await getRoomPurposePage({ pageNum: 1, pageSize: 9999 })
      if (res.code === 200) purposeOptions.value = res.data?.records || []
    } catch (e) {
      console.error(e)
    }
  }
}

async function loadData() {
  if (!activeFloorId.value) {
    cardsData.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res: any = await getRoomPage({
      keyword: filterText.value || undefined,
      status: filterStatus.value === '' ? undefined : Number(filterStatus.value),
      parkId: activeParkId.value || undefined,
      buildingId: activeBuildingId.value || undefined,
      floorId: activeFloorId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (res.code === 200) {
      cardsData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  loadData()
}

function onStatusChange() {
  pageNum.value = 1
  loadData()
}

function handleSizeChange(v: number) {
  pageSize.value = v
  loadData()
}

function handlePageChange(v: number) {
  pageNum.value = v
  loadData()
}

// ============== 卡片辅助函数 ==============
function statusLabel(s: number | undefined): string {
  if (s === undefined || s === null) return '空'
  return dictMap.room_status?.find((d) => d.value === String(s))?.label || '空'
}

function statusBadgeClass(s: number | undefined): string {
  if (s === 1) return 'bg-status-1'  // 已租
  if (s === 2) return 'bg-status-2'  // 装修中
  if (s === 3) return 'bg-status-3'  // 已售
  if (s === 4) return 'bg-status-4'  // 自用
  if (s === 5) return 'bg-status-5'  // 已预订
  return 'bg-status-0'  // 空置
}

function statusBadgeChar(s: number | undefined): string {
  if (s === 1) return '租'
  if (s === 2) return '装'
  if (s === 3) return '售'
  if (s === 4) return '己'
  if (s === 5) return '订'
  return '空'
}

function dictLabel(type: string, value: number | string | undefined): string {
  if (value === undefined || value === null) return '-'
  return dictMap[type]?.find((d) => d.value === String(value))?.label || String(value)
}

function purposeName(id: number | undefined): string {
  if (!id) return '-'
  return purposeOptions.value.find((p) => p.id === id)?.purposeName || '-'
}

function kitName(id: number | undefined): string {
  if (!id) return '-'
  return kitOptions.value.find((k) => k.id === id)?.kitName || '-'
}

function getRoomImage(r: Room): string {
  if (r.image) {
    try {
      const arr = JSON.parse(r.image)
      if (Array.isArray(arr) && arr.length > 0) return arr[0]
    } catch {
      if (r.image.startsWith('http')) return r.image
    }
  }
  return ''
}

// ============== 弹窗操作 ==============
async function handleAdd() {
  if (!activeBuildingId.value || !activeFloorId.value) {
    ElMessage.warning('请先在左侧选择楼栋和楼层')
    return
  }
  dialogMode.value = 'add'
  editingId.value = null
  Object.assign(form, defaultForm(), {
    parkId: activeParkId.value || undefined,
    buildingId: activeBuildingId.value || undefined,
    floorId: activeFloorId.value || undefined,
  })
  await loadKitsAndPurposes()
  dialogVisible.value = true
}

async function handleEdit(r: Room) {
  dialogMode.value = 'edit'
  editingId.value = r.id || null
  try {
    const res: any = await getRoomById(r.id!)
    if (res.code === 200) {
      Object.assign(form, defaultForm(), res.data)
      await loadKitsAndPurposes()
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载房间详情失败')
  }
}

async function handleView(r: Room) {
  dialogMode.value = 'view'
  editingId.value = r.id || null
  try {
    const res: any = await getRoomById(r.id!)
    if (res.code === 200) {
      Object.assign(form, defaultForm(), res.data)
      await loadKitsAndPurposes()
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载房间详情失败')
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (form.buildArea && form.areaCovered && Number(form.buildArea) > Number(form.areaCovered)) {
    ElMessage.error('套内面积不能大于建筑面积')
    return
  }
  submitting.value = true
  try {
    const payload: any = { ...form }
    delete payload.id
    const res: any = dialogMode.value === 'edit'
      ? await updateRoom(editingId.value!, payload)
      : await createRoom(payload)
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

async function handleDelete(r: Room) {
  try {
    await ElMessageBox.confirm(
      `确认要删除房间 "${r.roomName || r.roomNo}" 吗？\n（如房间存在合同/账单记录将无法删除）`,
      '删除确认',
      { type: 'warning' }
    )
    const res: any = await deleteRoom(r.id!)
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

function handleImport() {
  ElMessage.info('房间导入功能待实现 (Phase 7)')
  // TODO: 实现 Excel 导入弹窗, 调用 /room/import 接口
}

const dialogTitle = computed(() => {
  if (dialogMode.value === 'add') return '新增房间'
  if (dialogMode.value === 'edit') return '编辑房间'
  return '查看房间'
})

// 当前楼层名称
const currentFloorName = computed(() => {
  if (!activeBuildingId.value || !activeFloorId.value) return ''
  for (const park of parkTree.value) {
    const building = park.$buildingList?.find((b) => b.id === activeBuildingId.value)
    const floor = building?.$floorList?.find((f) => f.id === activeFloorId.value)
    if (floor) return floor.floorName
  }
  return ''
})

watch(activeFloorId, () => {
  if (activeFloorId.value) {
    pageNum.value = 1
    loadData()
  }
})

onMounted(async () => {
  await loadParks()
  await loadDicts()
  if (activeFloorId.value) {
    await loadData()
  }
})
</script>

<template>
  <div class="page-cover-container">
    <div class="common-flex">
      <!-- 左侧树形导航 -->
      <div class="basic-card-left" v-loading="false">
        <div class="left-bar-title">导航</div>
        <el-menu
          :default-active="String(activeFloorId || '')"
          unique-opened
          class="building-tree-list"
        >
          <el-submenu
            v-for="(park, pIdx) in parkTree"
            :key="pIdx"
            :index="`p-${park.id}`"
            class="first-menu"
            v-loading="park.$loading"
          >
            <template slot="title">
              <span class="common-ellipsis" :title="park.parkName">{{ park.parkName }}</span>
            </template>

            <template v-if="park.$buildingList && park.$buildingList.length">
              <el-submenu
                v-for="bld in park.$buildingList"
                :key="`b-${bld.id}`"
                :index="`b-${bld.id}`"
                class="second-menu"
                v-loading="bld.$loading"
              >
                <template slot="title">
                  <div class="common-ellipsis" :title="bld.buildingName" @click.stop="onBuildingClick(bld)">
                    {{ bld.buildingName }}
                  </div>
                </template>

                <template v-if="bld.$floorList && bld.$floorList.length">
                  <el-menu-item
                    v-for="floor in bld.$floorList"
                    :key="`f-${floor.id}`"
                    :index="`f-${floor.id}`"
                    @click="onFloorClick(floor)"
                  >
                    <div class="common-ellipsis" :title="floor.floorName">{{ floor.floorName }}</div>
                  </el-menu-item>
                </template>
                <div v-else class="data-null-text">该楼栋暂无楼层</div>
              </el-submenu>
            </template>

            <div v-else-if="park.$loaded" class="data-null-box">
              <div class="data-null-text">暂无楼栋</div>
            </div>
          </el-submenu>
        </el-menu>
      </div>

      <!-- 右侧内容区 -->
      <div class="basic-card-right" v-loading="loading">
        <!-- 头部 -->
        <div class="page-header">
          <span class="page-header-title">
            房间列表
            <span v-if="currentFloorName" class="current-floor">（{{ currentFloorName }}）</span>
          </span>
          <div class="page-header-actions">
            <el-button size="small" type="primary" plain icon="el-icon-download" @click="handleImport">
              房间导入
            </el-button>
            <el-radio-group v-model="filterStatus" class="status-list" @change="onStatusChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="0">空置</el-radio-button>
              <el-radio-button label="1">已租</el-radio-button>
              <el-radio-button label="2">装修中</el-radio-button>
              <el-radio-button label="3">已售</el-radio-button>
              <el-radio-button label="4">自用</el-radio-button>
            </el-radio-group>
            <el-input
              v-model="filterText"
              class="search-input"
              placeholder="房号/名称"
              clearable
              @change="onSearch"
              @clear="onSearch"
            >
              <template #suffix>
                <i class="el-input__icon el-icon-search" @click="onSearch"></i>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 卡片网格 -->
        <div class="page-list">
          <el-row :gutter="16" v-if="activeFloorId">
            <!-- 新增卡片 -->
            <el-col :xs="24" :md="12" :lg="8" :xl="6" class="custom-col">
              <div class="item-card-add" @click="handleAdd">
                <i class="el-icon-plus"></i>
                <span>新增房间</span>
              </div>
            </el-col>

            <!-- 房间卡片 -->
            <el-col
              v-for="(r, idx) in cardsData"
              :key="idx"
              :xs="24" :md="12" :lg="8" :xl="6"
              class="custom-col"
            >
              <div class="item-card" @click="handleView(r)">
                <div class="item-card-header">
                  <div class="c-status">
                    <span :class="statusBadgeClass(r.status)">{{ statusBadgeChar(r.status) }}</span>
                  </div>
                  <div class="c-title common-ellipsis" :title="r.roomName">{{ r.roomName || r.roomNo }}</div>
                  <div class="c-type common-ellipsis" :title="purposeName(r.purposeId)">{{ purposeName(r.purposeId) }}</div>
                  <div class="c-more-button">
                    <el-button type="text" icon="el-icon-edit-outline" title="编辑" @click.stop="handleEdit(r)"></el-button>
                    <el-button type="text" class="status-danger" icon="el-icon-delete" title="删除" @click.stop="handleDelete(r)"></el-button>
                  </div>
                </div>
                <div class="item-card-content">
                  <div class="c-area">
                    <span>房号</span>
                    <span>{{ r.roomNo || '-' }}</span>
                  </div>
                  <div class="c-area">
                    <span>建筑面积</span>
                    <span>{{ r.areaCovered || 0 }} ㎡</span>
                  </div>
                  <div class="c-area">
                    <span>套内面积</span>
                    <span>{{ r.buildArea || 0 }} ㎡</span>
                  </div>
                  <div class="c-area">
                    <span>单价(元/㎡/月)</span>
                    <span>{{ r.unitPrice || 0 }}</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>

          <el-empty v-else description="请在左侧选择楼层" />

          <!-- 分页 -->
          <el-pagination
            v-if="total >= 10"
            class="common-pagination"
            background
            layout="total, prev, pager, next, sizes, jumper"
            :total="total"
            :page-sizes="pageSizes"
            :page-size="pageSize"
            :current-page.sync="pageNum"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑/查看弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="900px"
      :close-on-click-modal="false"
      @close="Object.assign(form, defaultForm())"
    >
      <el-form
        :model="form"
        label-width="120px"
        :rules="formRules"
        ref="formRef"
        :disabled="dialogMode === 'view'"
        :validate-on-rule-change="false"
      >
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="所属园区" prop="parkId">
              <el-select v-model="form.parkId" placeholder="请选择园区" filterable>
                <el-option
                  v-for="p in parkTree"
                  :key="p.id"
                  :label="p.parkName"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属楼栋" prop="buildingId">
              <el-select v-model="form.buildingId" placeholder="请选择楼栋" filterable>
                <el-option
                  v-for="b in (parkTree.find(p => p.id === form.parkId)?.$buildingList || [])"
                  :key="b.id"
                  :label="b.buildingName"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属楼层" prop="floorId">
              <el-select v-model="form.floorId" placeholder="请选择楼层" filterable clearable>
                <el-option
                  v-for="f in (parkTree.find(p => p.id === form.parkId)?.$buildingList?.find(b => b.id === form.buildingId)?.$floorList || [])"
                  :key="f.id"
                  :label="f.floorName"
                  :value="f.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房间编号" prop="roomNo">
              <el-input v-model="form.roomNo" placeholder="如 A-101" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房间名称" prop="roomName">
              <el-input v-model="form.roomName" placeholder="请输入房间名称" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房屋结构" prop="houseStructure">
              <el-select v-model="form.houseStructure" placeholder="请选择房屋结构" clearable>
                <el-option
                  v-for="d in dictMap.room_structure || []"
                  :key="d.value"
                  :label="d.label"
                  :value="Number(d.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="建筑面积" prop="areaCovered">
              <el-input-number v-model="form.areaCovered" :precision="2" :min="0" style="width: 100%" placeholder="m²" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="套内面积" prop="buildArea">
              <el-input-number v-model="form.buildArea" :precision="2" :min="0" style="width: 100%" placeholder="m²" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计费面积" prop="billableArea">
              <el-input-number v-model="form.billableArea" :precision="2" :min="0" style="width: 100%" placeholder="m²" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :precision="2" :min="0" style="width: 100%" placeholder="元/m²/月" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总价" prop="totalPrice">
              <el-input-number v-model="form.totalPrice" :precision="2" :min="0" style="width: 100%" placeholder="元/月" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="月租金" prop="monthlyRent">
              <el-input-number v-model="form.monthlyRent" :precision="2" :min="0" style="width: 100%" placeholder="元" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房间配套" prop="kitId">
              <el-select v-model="form.kitId" placeholder="请选择房间配套" filterable clearable>
                <el-option
                  v-for="k in kitOptions"
                  :key="k.id"
                  :label="k.kitName"
                  :value="k.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房间用途" prop="purposeId">
              <el-select v-model="form.purposeId" placeholder="请选择房间用途" filterable clearable>
                <el-option
                  v-for="p in purposeOptions"
                  :key="p.id"
                  :label="p.purposeName"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序" prop="sorting">
              <el-input-number v-model="form.sorting" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态">
                <el-option
                  v-for="d in dictMap.room_status || []"
                  :key="d.value"
                  :label="d.label"
                  :value="Number(d.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="房间介绍" prop="introduce">
              <el-input v-model="(form as any).introduce" type="textarea" :rows="3" placeholder="房间详细介绍" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          v-if="dialogMode !== 'view'"
          type="primary"
          @click="handleSubmit"
          :loading="submitting"
        >确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-cover-container {
  position: absolute;
  height: calc(100% - 32px);
  width: 100%;
}

.common-flex {
  height: 100%;
  display: flex;
}

.basic-card-left {
  min-width: 280px;
  max-width: 360px;
  width: 28%;
  border-right: 1px solid #d8d8d8;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.basic-card-right {
  flex: 1;
  padding: 32px 40px;
  overflow: auto;
  background: #f5f7fa;
}

.left-bar-title {
  padding: 16px 24px 8px;
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.building-tree-list {
  flex: 1;
  overflow-y: auto;
}

.building-tree-list :deep(.el-menu) {
  background: none;
  border: none;
}

.building-tree-list .first-menu {
  margin-top: 8px;
}

.building-tree-list .first-menu > .el-submenu__title {
  background: rgba(0, 0, 0, 0.03);
  font-weight: 500;
}

.building-tree-list .first-menu.is-opened > .el-submenu__title {
  background: none;
}

.building-tree-list .first-menu > ul {
  margin: 8px 16px 8px 0;
  padding-right: 16px;
}

.building-tree-list .second-menu {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
  margin-bottom: 8px;
}

.building-tree-list .second-menu:last-child {
  margin-bottom: 0;
}

.building-tree-list .second-menu.is-opened > ul {
  border-top: 1px solid rgba(0, 0, 0, 0.03);
}

.building-tree-list .second-menu .is-active {
  background: rgba(64, 158, 255, 0.08);
  border-radius: 4px;
  color: #409eff;
}

.data-null-box {
  padding: 24px;
  text-align: center;
}

.data-null-text {
  color: rgba(0, 0, 0, 0.25);
  font-size: 12px;
  padding: 8px 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  white-space: nowrap;
  flex-wrap: wrap;
  gap: 12px;
}

.page-header .page-header-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin-right: 16px;
}

.page-header .current-floor {
  font-size: 14px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.65);
  margin-left: 8px;
}

.page-header .page-header-actions {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: flex-end;
  gap: 12px;
}

.page-header .status-list {
  display: inline-flex;
}

.page-header .status-list :deep(.el-radio-button__inner) {
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
}

.page-header .search-input {
  max-width: 240px;
  min-width: 180px;
  flex: 0 0 auto;
}

.page-list ul {
  margin: 0 -8px;
  display: flex;
  flex-wrap: wrap;
}

.page-list ul li {
  margin: 8px;
  width: 100%;
  height: 220px;
  cursor: pointer;
}

.page-list .item-card {
  width: 100%;
  height: 100%;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  border-radius: 6px;
  border: 1px solid #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.2s;
}

.page-list .item-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(64, 158, 255, 0.15);
}

.page-list .item-card .item-card-header {
  padding: 16px;
  background: rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  position: relative;
}

.page-list .item-card .item-card-content {
  padding: 12px 16px;
  flex: 1;
}

.page-list .item-card .c-status {
  width: 40px;
  height: 40px;
  min-width: 40px;
}

.page-list .item-card .c-status span {
  display: block;
  border-radius: 50%;
  color: #fff;
  line-height: 40px;
  text-align: center;
  font-weight: 700;
  font-size: 18px;
  width: 100%;
  height: 100%;
}

.page-list .item-card .c-status .bg-status-0 { background: #fdb800; }
.page-list .item-card .c-status .bg-status-1 { background: #409eff; }
.page-list .item-card .c-status .bg-status-2 { background: #ff9267; }
.page-list .item-card .c-status .bg-status-3 { background: #67c23a; }
.page-list .item-card .c-status .bg-status-4 { background: #5b6bff; }
.page-list .item-card .c-status .bg-status-5 { background: #e6a23c; }

.page-list .item-card .c-title {
  font-size: 15px;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 8px;
  font-weight: 500;
  flex: 1;
}

.page-list .item-card .c-type {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  padding: 2px 8px;
}

.page-list .item-card .c-more-button {
  position: absolute;
  right: 8px;
  top: 12px;
}

.page-list .item-card .c-more-button .el-button {
  font-size: 14px;
  padding: 4px;
}

.page-list .item-card .c-more-button .el-button + .el-button {
  margin-left: 0;
}

.page-list .item-card .c-area {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  line-height: 20px;
}

.page-list .item-card .c-area:last-child {
  margin-bottom: 0;
}

.page-list .item-card-add {
  border: 2px dashed rgba(0, 0, 0, 0.15);
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  font-size: 15px;
  color: rgba(0, 0, 0, 0.65);
  height: 220px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

.page-list .item-card-add i {
  margin-right: 8px;
  font-size: 18px;
}

.page-list .item-card-add:hover {
  font-weight: 600;
  background: rgba(64, 158, 255, 0.05);
  border-color: #409eff;
  color: #409eff;
}

.common-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

@media screen and (min-width: 1700px) {
  .custom-col {
    width: 25% !important;
  }
}
</style>
