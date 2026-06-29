<script setup lang="ts">
/**
 * 房间管理 (park-space) - 列表重构
 *
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/room/index.vue + bar/index.vue + detail.vue
 *
 * 设计: 左侧树形导航 (园区 → 楼栋 → 楼层) + 右侧 el-table 表格
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
import { getAreaPage, type Area } from '@/api/area'
import { getRoomLockRecordPage } from '@/api/room-lock-record'
import { getRoomRecordPage } from '@/api/room-record'
import { getRoomSplitMergePage, mergeRooms, splitRoom } from '@/api/room-split-merge'
import { lockRoom, unlockRoom } from '@/api/room-control'

// ============== 状态 ==============
const loading = ref(false)
const tableData = ref<Room[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(50)
const pageSizes = [10, 50, 100, 150]

// 树形导航状态 (4 层: 园区 → 分区 → 楼栋 → 楼层)
interface ParkTreeNode extends Park {
  $areaList?: AreaTreeNode[]
  $loading?: boolean
  $loaded?: boolean
}
interface AreaTreeNode extends Area {
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
const activeParkId = ref<string | null>(null)
const activeAreaId = ref<string | null>(null)
const activeBuildingId = ref<string | null>(null)
const activeFloorId = ref<string | null>(null)

// el-tree 引用和配置
const treeRef = ref()
const treeProps = { children: 'children', label: 'label' }

// 将 parkTree 转为 el-tree 格式
const treeData = computed(() => {
  return parkTree.value.map(park => ({
    id: `p-${park.id}`,
    label: park.parkName,
    children: (park.$loaded && park.$areaList)
      ? park.$areaList.map(area => ({
          id: `a-${area.id}`,
          label: area.areaName,
          children: (area.$loaded && area.$buildingList)
            ? area.$buildingList.map(bld => ({
                id: `b-${bld.id}`,
                label: bld.buildingName,
                children: (bld.$loaded && bld.$floorList)
                  ? bld.$floorList.map(floor => ({
                      id: `f-${floor.id}`,
                      label: floor.floorName,
                    }))
                  : undefined,
              }))
            : undefined,
        }))
      : undefined,
  }))
})

// 当前高亮节点
const currentNodeKey = computed<string>(() => {
  if (activeFloorId.value) return `f-${activeFloorId.value}`
  if (activeBuildingId.value) return `b-${activeBuildingId.value}`
  if (activeAreaId.value) return `a-${activeAreaId.value}`
  if (activeParkId.value) return `p-${activeParkId.value}`
  return ''
})

// 默认展开: 第一个 park + 第一个 area + 第一个 building
const defaultExpandedKeys = computed<string[]>(() => {
  const result: string[] = []
  const firstPark = parkTree.value[0]
  if (firstPark) {
    result.push(`p-${firstPark.id}`)
    const firstArea = firstPark.$areaList?.[0]
    if (firstArea) {
      result.push(`a-${firstArea.id}`)
      const firstBuilding = firstArea.$buildingList?.[0]
      if (firstBuilding) {
        result.push(`b-${firstBuilding.id}`)
      }
    }
  }
  return result
})

// 搜索/过滤
const filterStatus = ref<string>('')  // '' = 全部
const filterText = ref('')

// C6: 列表多选状态 (合并/批量操作用)
const selectedRows = ref<Room[]>([])

function onSelectionChange(rows: Room[]) {
  selectedRows.value = rows
}

function clearSelection() {
  selectedRows.value = []
}

// 字典
const dictMap = reactive<Record<string, Array<{ value: string; label: string }>>>({})

// 弹窗
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit' | 'view'>('add')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()

// 配套/用途 options
const kitOptions = ref<Kit[]>([])
const purposeOptions = ref<RoomPurpose[]>([])

// 查看模式 tabs 状态
type TabName = 'lock' | 'split' | 'record'
const activeTabName = ref<TabName>('lock')
const tabLoading = reactive<Record<TabName, boolean>>({ lock: false, split: false, record: false })
const tabData = reactive<Record<TabName, any[]>>({ lock: [], split: [], record: [] })
const tabTotal = reactive<Record<TabName, number>>({ lock: 0, split: 0, record: 0 })
const tabPage = reactive<Record<TabName, number>>({ lock: 1, split: 1, record: 1 })
const tabSize = reactive<Record<TabName, number>>({ lock: 10, split: 10, record: 10 })
const tabLoaded = reactive<Record<TabName, boolean>>({ lock: false, split: false, record: false })

const defaultForm = () => ({
  id: undefined as string | undefined,
  parkId: undefined as string | undefined,
  areaId: undefined as string | undefined,
  buildingId: undefined as string | undefined,
  floorId: undefined as string | undefined,
  floor: 1,
  roomNo: '',
  roomName: '',
  roomType: '',
  houseStructure: undefined as number | undefined,
  areaCovered: undefined as number | undefined,
  buildArea: undefined as number | undefined,
  billableArea: undefined as number | undefined,
  unitPrice: undefined as number | undefined,
  totalPrice: undefined as number | undefined,
  monthlyRent: undefined as number | undefined,
  kitId: undefined as string | undefined,
  purposeId: undefined as string | undefined,
  rentingSelling: 2,
  leasePrice: undefined as number | undefined,
  salePrice: undefined as number | undefined,
  isLock: 0,
  isOrder: 0,
  image: '',
  introduce: '',
  sorting: 0,
  status: 0,
  remark: '',
})
const form = reactive(defaultForm())

const formRules = {
  parkId: [{ required: true, message: '请选择园区', trigger: 'change' }],
  areaId: [{ required: true, message: '请选择分区', trigger: 'change' }],
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
    const res: any = await getParkList()
    if (res.code === 200) {
      parkTree.value = (res.data || []).map((p: Park) => ({ ...p, $loaded: false }))
      if (parkTree.value.length > 0 && !activeParkId.value) {
        activeParkId.value = parkTree.value[0].id || null
        // 首次默认加载第一个园区的分区
        await loadAreasForPark(parkTree.value[0])
        if (activeAreaId.value) {
          const area = parkTree.value[0].$areaList?.find((a) => a.id === activeAreaId.value)
          if (area && !area.$loaded) {
            await loadBuildingsForArea(area)
          }
        }
        if (activeBuildingId.value) {
          const area = parkTree.value[0].$areaList?.find((a) => a.id === activeAreaId.value)
          const building = area?.$buildingList?.find((b) => b.id === activeBuildingId.value)
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

async function loadAreasForPark(park: ParkTreeNode) {
  if (park.$loaded) return
  park.$loading = true
  try {
    const res: any = await getAreaPage({ parkId: park.id, pageNum: 1, pageSize: 9999 })
    if (res.code === 200) {
      park.$areaList = (res.data?.records || []).map((a: Area) => ({ ...a, $loaded: false }))
      park.$loaded = true
      // 默认选中第一个分区
      if (park.$areaList.length > 0 && !activeAreaId.value) {
        const firstArea = park.$areaList[0]
        activeAreaId.value = firstArea.id || null
        await loadBuildingsForArea(firstArea)
        // 默认选中第一个楼栋
        if (firstArea.$buildingList && firstArea.$buildingList.length > 0 && !activeBuildingId.value) {
          const firstBld = firstArea.$buildingList[0]
          activeBuildingId.value = firstBld.id || null
          await loadFloorsForBuilding(firstBld)
          // 默认选中第一个楼层
          if (activeFloorId.value === null && firstBld.$floorList?.length) {
            activeFloorId.value = firstBld.$floorList[0].id || null
          }
        }
      }
    }
  } finally {
    park.$loading = false
  }
}

async function loadBuildingsForArea(area: AreaTreeNode) {
  if (area.$loaded) return
  area.$loading = true
  try {
    const res: any = await getBuildingPage({ areaId: area.id, pageNum: 1, pageSize: 9999 })
    if (res.code === 200) {
      area.$buildingList = (res.data?.records || []).map((b: Building) => ({ ...b, $loaded: false }))
      area.$loaded = true
    }
  } catch (e) {
    console.error(e)
  } finally {
    area.$loading = false
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

// 节点点击处理 — 统一入口，根据树层级判断
function onTreeNodeClick(data: any) {
  const id = data.id as string
  const prefix = id.charAt(0)

  if (prefix === 'p') {
    activeParkId.value = id.slice(2)
    activeAreaId.value = activeBuildingId.value = activeFloorId.value = null
    const park = parkTree.value.find(p => p.id === activeParkId.value)
    if (park && !park.$loaded) loadAreasForPark(park)
  } else if (prefix === 'a') {
    activeAreaId.value = id.slice(2)
    activeBuildingId.value = activeFloorId.value = null
    // 找到 area 对象，若未加载则加载
    for (const park of parkTree.value) {
      const area = park.$areaList?.find(a => a.id === activeAreaId.value)
      if (area && !area.$loaded) { loadBuildingsForArea(area); break }
    }
  } else if (prefix === 'b') {
    activeBuildingId.value = id.slice(2)
    activeFloorId.value = null
    for (const park of parkTree.value) {
      for (const area of park.$areaList || []) {
        const bld = area.$buildingList?.find(b => b.id === activeBuildingId.value)
        if (bld && !bld.$loaded) { loadFloorsForBuilding(bld); break }
      }
    }
  } else if (prefix === 'f') {
    activeFloorId.value = id.slice(2)
    // 从树中回溯查找楼栋和分区
    for (const park of parkTree.value) {
      for (const area of park.$areaList || []) {
        for (const bld of area.$buildingList || []) {
          if (bld.$floorList?.some((f: any) => f.id === activeFloorId.value)) {
            activeAreaId.value = area.id || null
            activeBuildingId.value = bld.id || null
            break
          }
        }
        if (activeBuildingId.value) break
      }
      if (activeBuildingId.value) break
    }
  }

  // 触发右侧房间列表按当前选中节点过滤
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
  // 4 级树: park/area/building/floor 任一级都可作为查询入参
  // 不强制要求 floor_id — 选园区/分区/楼栋也能看到该范围内的房间
  // 没选园区则不查询 (避免返回所有租户的全量数据)
  if (!activeParkId.value) {
    tableData.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res: any = await getRoomPage({
      keyword: filterText.value || undefined,
      status: filterStatus.value === '' ? undefined : Number(filterStatus.value),
      parkId: activeParkId.value || undefined,
      areaId: activeAreaId.value || undefined,
      buildingId: activeBuildingId.value || undefined,
      floorId: activeFloorId.value || undefined,
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

// ============== 辅助函数 ==============
function statusLabel(s: number | undefined): string {
  if (s === undefined || s === null) return '空'
  return dictMap.room_status?.find((d) => d.value === String(s))?.label || '空'
}

function statusTagType(s: number | undefined): string {
  if (s === 0) return 'warning'
  if (s === 1) return 'primary'
  if (s === 2) return 'danger'
  if (s === 3) return 'warning'
  if (s === 4) return 'primary'
  return 'info'
}

function rentingSellingLabel(val: number | undefined): string {
  const map: Record<number, string> = { 0: '可租', 1: '可售', 2: '可租售', 3: '自用' }
  return val != null ? map[val] || '-' : '-'
}

function purposeName(id: string | undefined): string {
  if (!id) return '-'
  return purposeOptions.value.find((p) => p.id === id)?.purposeName || '-'
}

function kitName(id: string | undefined): string {
  if (!id) return '-'
  return kitOptions.value.find((k) => k.id === id)?.kitName || '-'
}

function parkNameOf(id: string | undefined): string {
  if (!id) return '-'
  return parkTree.value.find((p) => p.id === id)?.parkName || '-'
}

function buildingNameOf(parkId: string | undefined, buildingId: string | undefined): string {
  if (!parkId || !buildingId) return '-'
  for (const park of parkTree.value) {
    if (park.id !== parkId) continue
    const building = park.$buildingList?.find((b) => b.id === buildingId)
    if (building) return building.buildingName || '-'
  }
  return '-'
}

function floorNameOf(parkId: string | undefined, buildingId: string | undefined, floorId: string | undefined): string {
  if (!parkId || !buildingId || !floorId) return '-'
  for (const park of parkTree.value) {
    if (park.id !== parkId) continue
    const building = park.$buildingList?.find((b) => b.id === buildingId)
    const floor = building?.$floorList?.find((f) => f.id === floorId)
    if (floor) return floor.floorName || '-'
  }
  return '-'
}

function splitMergeTypeLabel(type: number | undefined): string {
  if (type === 0) return '合并'
  if (type === 1) return '拆分'
  if (type === 2) return '还原'
  return '-'
}

function recordTypeLabel(covenantType: number | undefined, status: number | undefined): string {
  // covenantType 区分来源 (1=绑定/合同), status 区分动作 (0/1)
  if (covenantType === 1) return status === 1 ? '绑定' : '解绑'
  if (covenantType === 2) return status === 1 ? '关联' : '取消'
  return '-'
}

function fmtArea(v: number | undefined, unit = '㎡'): string {
  if (v === undefined || v === null) return '-'
  return `${Number(v).toLocaleString()} ${unit}`
}

function fmtPrice(v: number | undefined, unit = '元'): string {
  if (v === undefined || v === null) return '-'
  return `${Number(v).toLocaleString()} ${unit}`
}

// ============== 查看模式 tabs 加载 ==============
function resetTabState() {
  activeTabName.value = 'lock'
  ;(['lock', 'split', 'record'] as TabName[]).forEach((k) => {
    tabLoading[k] = false
    tabData[k] = []
    tabTotal[k] = 0
    tabPage[k] = 1
    tabSize[k] = 10
    tabLoaded[k] = false
  })
}

async function loadLockRecords() {
  if (!editingId.value) return
  tabLoading.lock = true
  try {
    const res: any = await getRoomLockRecordPage({
      roomId: editingId.value,
      pageNum: tabPage.lock,
      pageSize: tabSize.lock,
    })
    if (res.code === 200) {
      tabData.lock = res.data?.records || []
      tabTotal.lock = res.data?.total || 0
      tabLoaded.lock = true
    }
  } catch (e) {
    console.error('loadLockRecords error', e)
  } finally {
    tabLoading.lock = false
  }
}

async function loadSplitRecords() {
  if (!editingId.value) return
  tabLoading.split = true
  try {
    // /room-split-merge/page 无 roomId 参数, 用 parkId 加载后客户端过滤
    // (split/merge 记录数有限, 性能可接受)
    const res: any = await getRoomSplitMergePage({
      parkId: form.parkId,
      pageNum: tabPage.split,
      pageSize: tabSize.split * 5, // 多取一些以补偿客户端过滤
    })
    if (res.code === 200) {
      const roomId = editingId.value
      const all = res.data?.records || []
      tabData.split = all.filter(
        (r: any) => r.oldRoomId === roomId || r.newRoomId === roomId
      )
      tabTotal.split = tabData.split.length
      tabLoaded.split = true
    }
  } catch (e) {
    console.error('loadSplitRecords error', e)
  } finally {
    tabLoading.split = false
  }
}

async function loadRoomRecords() {
  if (!editingId.value) return
  tabLoading.record = true
  try {
    const res: any = await getRoomRecordPage({
      roomId: editingId.value,
      pageNum: tabPage.record,
      pageSize: tabSize.record,
    })
    if (res.code === 200) {
      tabData.record = res.data?.records || []
      tabTotal.record = res.data?.total || 0
      tabLoaded.record = true
    }
  } catch (e) {
    console.error('loadRoomRecords error', e)
  } finally {
    tabLoading.record = false
  }
}

function onTabClick(tab: any) {
  const name = tab.props.name as TabName
  if (name === 'lock' && !tabLoaded.lock) loadLockRecords()
  if (name === 'split' && !tabLoaded.split) loadSplitRecords()
  if (name === 'record' && !tabLoaded.record) loadRoomRecords()
}

function onTabSizeChange(name: TabName, v: number) {
  tabSize[name] = v
  tabPage[name] = 1
  if (name === 'lock') loadLockRecords()
  if (name === 'split') loadSplitRecords()
  if (name === 'record') loadRoomRecords()
}

function onTabPageChange(name: TabName, v: number) {
  tabPage[name] = v
  if (name === 'lock') loadLockRecords()
  if (name === 'split') loadSplitRecords()
  if (name === 'record') loadRoomRecords()
}

// ============== 弹窗操作 ==============
// 表单级联: 切换园区时清空下级 + 加载分区选项
async function onFormParkChange(parkId: string) {
  form.areaId = undefined
  form.buildingId = undefined
  form.floorId = undefined
  const park = parkTree.value.find((p) => p.id === parkId)
  if (park && !park.$loaded) await loadAreasForPark(park)
}

async function onFormAreaChange(_areaId: string) {
  form.buildingId = undefined
  form.floorId = undefined
}

async function onFormBuildingChange(buildingId: string) {
  form.floorId = undefined
  const bld = (parkTree.value
    .flatMap((p) => p.$areaList || [])
    .flatMap((a) => a.$buildingList || [])
    .find((b) => b.id === buildingId))
  if (bld && !bld.$loaded) await loadFloorsForBuilding(bld)
}

async function onFormFloorChange(_floorId: string) {
  // 楼层变化不需要清空其他字段, 房间号等信息独立
}

async function handleAdd() {
  if (!activeParkId.value) {
    ElMessage.warning('请先在左侧选择园区')
    return
  }
  dialogMode.value = 'add'
  editingId.value = null
  Object.assign(form, defaultForm(), {
    parkId: activeParkId.value || undefined,
    areaId: activeAreaId.value || undefined,
    buildingId: activeBuildingId.value || undefined,
    floorId: activeFloorId.value || undefined,
  })
  // 加载该园区下的分区选项 (供下拉选择)
  const park = parkTree.value.find((p) => p.id === activeParkId.value)
  if (park && !park.$loaded) await loadAreasForPark(park)
  await loadKitsAndPurposes()
  dialogVisible.value = true
}

async function handleEdit(r: Room) {
  dialogMode.value = 'edit'
  editingId.value = r.id || null
  try {
    const res: any = await getRoomById(r.id!)
    if (res.code === 200) {
      // 后端 Room.*Id 已用 @JsonFormat(STRING) 序列化, 显式 String() 保险
      const data = {
        ...res.data,
        parkId: res.data.parkId != null ? String(res.data.parkId) : undefined,
        areaId: res.data.areaId != null ? String(res.data.areaId) : undefined,
        buildingId: res.data.buildingId != null ? String(res.data.buildingId) : undefined,
        floorId: res.data.floorId != null ? String(res.data.floorId) : undefined,
        kitId: res.data.kitId != null ? String(res.data.kitId) : undefined,
        purposeId: res.data.purposeId != null ? String(res.data.purposeId) : undefined,
      }
      Object.assign(form, defaultForm(), data)
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
      const data = {
        ...res.data,
        parkId: res.data.parkId != null ? String(res.data.parkId) : undefined,
        areaId: res.data.areaId != null ? String(res.data.areaId) : undefined,
        buildingId: res.data.buildingId != null ? String(res.data.buildingId) : undefined,
        floorId: res.data.floorId != null ? String(res.data.floorId) : undefined,
        kitId: res.data.kitId != null ? String(res.data.kitId) : undefined,
        purposeId: res.data.purposeId != null ? String(res.data.purposeId) : undefined,
      }
      Object.assign(form, defaultForm(), data)
      await loadKitsAndPurposes()
      resetTabState()
      dialogVisible.value = true
      // 预加载默认 tab (锁定记录)
      loadLockRecords()
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

// ============== C6: 锁定/解锁/拆分/合并 操作 ==============

async function handleLock(r: Room) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入锁定原因', `锁定房间 ${r.roomNo}`, {
      confirmButtonText: '锁定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '锁定原因不能为空',
    })
    const res: any = await lockRoom({ roomId: r.id!, reason })
    if (res.code === 200) {
      ElMessage.success('已锁定')
      await loadData()
    } else {
      ElMessage.error(res.msg || '锁定失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleUnlock(r: Room) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入解锁原因', `解锁房间 ${r.roomNo}`, {
      confirmButtonText: '解锁',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '解锁原因不能为空',
    })
    const res: any = await unlockRoom({ roomId: r.id!, reason })
    if (res.code === 200) {
      ElMessage.success('已解锁')
      await loadData()
    } else {
      ElMessage.error(res.msg || '解锁失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleSplit(r: Room) {
  // csyh 模式: 打开拆分 dialog, 用户输入数量 → 生成可编辑子表 → 确认提交
  splitDialogOld.value = { ...r }
  splitForm.num = 2
  splitForm.reasons = ''
  splitForm.roomList = []
  splitDialogVisible.value = true
}

async function handleMerge() {
  // csyh 模式: 打开合并 dialog, 已选房间列表 + 自动 sum 面积 → 用户确认
  if (selectedRows.value.length < 2) {
    ElMessage.warning('合并至少选择 2 个房间')
    return
  }
  const first = selectedRows.value[0]
  // 自动 sum 面积 (csyh 没有自动 sum, 用户要求)
  const totalArea = selectedRows.value.reduce((s, r) => s + (Number(r.areaCovered) || 0), 0)
  const totalBuild = selectedRows.value.reduce((s, r) => s + (Number(r.buildArea) || 0), 0)
  mergeForm.parkId = first.parkId
  mergeForm.buildingId = first.buildingId
  mergeForm.floorId = first.floorId
  mergeForm.floor = first.floor
  mergeForm.roomType = first.roomType || 'OFFICE'
  mergeForm.roomNo = ''
  mergeForm.roomName = ''
  mergeForm.areaCovered = Number(totalArea.toFixed(2))
  mergeForm.buildArea = Number(totalBuild.toFixed(2))
  mergeForm.billableArea = selectedRows.value.reduce((s, r) => s + (Number(r.billableArea) || 0), 0)
  mergeForm.unitPrice = first.unitPrice
  mergeForm.monthlyRent = selectedRows.value.reduce((s, r) => s + (Number(r.monthlyRent) || 0), 0)
  mergeForm.reasons = ''
  mergeForm.oldRoomIds = selectedRows.value.map((r) => r.id!)
  mergeDialogVisible.value = true
}

// ============== 拆分 dialog 逻辑 ==============

const splitDialogVisible = ref(false)
const splitDialogOld = ref<Room | null>(null)
const splitSubmitting = ref(false)
const splitForm = reactive({
  num: 2,
  reasons: '',
  roomList: [] as Array<{ roomNo: string; roomName?: string; areaCovered?: number; buildArea?: number; billableArea?: number; monthlyRent?: number; unitPrice?: number }>,
})

function generateSplitPreview() {
  const old = splitDialogOld.value
  if (!old) return
  const num = Number(splitForm.num)
  if (!num || num < 2 || num > 99) {
    ElMessage.warning('拆分数量必须在 2-99 之间')
    return
  }
  // 自动平均面积 + 末房抹平尾差 (用户要求)
  const oldArea = Number(old.areaCovered) || 0
  const oldBuild = Number(old.buildArea) || 0
  const oldBill = Number(old.billableArea) || 0
  const avgArea = Number((oldArea / num).toFixed(2))
  const avgBuild = Number((oldBuild / num).toFixed(2))
  const avgBill = Number((oldBill / num).toFixed(2))
  const lastArea = Number((oldArea - avgArea * (num - 1)).toFixed(2))
  const lastBuild = Number((oldBuild - avgBuild * (num - 1)).toFixed(2))
  const lastBill = Number((oldBill - avgBill * (num - 1)).toFixed(2))

  splitForm.roomList = []
  for (let i = 1; i <= num; i++) {
    const isLast = i === num
    splitForm.roomList.push({
      roomNo: `${old.roomNo}-${i}`,
      roomName: `${old.roomName || old.roomNo}-${i}`,
      areaCovered: isLast ? lastArea : avgArea,
      buildArea: isLast ? lastBuild : avgBuild,
      billableArea: isLast ? lastBill : avgBill,
      monthlyRent: old.monthlyRent,
      unitPrice: old.unitPrice,
    })
  }
}

function resetSplitPreview() {
  generateSplitPreview()
}

function validateSplitForm(): string | null {
  if (!splitForm.num || splitForm.num < 2) return '拆分数量至少为 2'
  if (!splitForm.roomList || splitForm.roomList.length !== Number(splitForm.num)) {
    return '请先生成拆分预览 (点击「拆分」按钮)'
  }
  for (let i = 0; i < splitForm.roomList.length; i++) {
    const r = splitForm.roomList[i]
    if (!r.roomNo?.trim()) return `第 ${i + 1} 行房间号不能为空`
    if (!r.roomName?.trim()) return `第 ${i + 1} 行房间名称不能为空`
  }
  return null
}

async function confirmSplit() {
  const old = splitDialogOld.value
  if (!old) return
  const err = validateSplitForm()
  if (err) {
    ElMessage.warning(err)
    return
  }
  splitSubmitting.value = true
  try {
    const res: any = await splitRoom({
      parkId: old.parkId,
      buildingId: old.buildingId,
      oldRoomId: old.id!,
      num: Number(splitForm.num),
      reasons: splitForm.reasons || undefined,
      roomList: splitForm.roomList,
    })
    if (res.code === 200) {
      ElMessage.success('拆分成功')
      splitDialogVisible.value = false
      clearSelection()
      await loadData()
    } else {
      ElMessage.error(res.msg || '拆分失败')
    }
  } finally {
    splitSubmitting.value = false
  }
}

// ============== 合并 dialog 逻辑 ==============

const mergeDialogVisible = ref(false)
const mergeSubmitting = ref(false)
const mergeForm = reactive({
  parkId: undefined as string | undefined,
  buildingId: undefined as string | undefined,
  floorId: undefined as string | undefined,
  floor: undefined as number | undefined,
  roomType: 'OFFICE',
  roomNo: '',
  roomName: '',
  areaCovered: 0,
  buildArea: 0,
  billableArea: 0,
  unitPrice: undefined as number | undefined,
  monthlyRent: 0,
  reasons: '',
  oldRoomIds: [] as string[],
})

async function confirmMerge() {
  if (!mergeForm.roomNo?.trim()) {
    ElMessage.warning('新房间号不能为空')
    return
  }
  if (!mergeForm.oldRoomIds || mergeForm.oldRoomIds.length < 2) {
    ElMessage.warning('至少选择 2 个房间')
    return
  }
  mergeSubmitting.value = true
  try {
    const res: any = await mergeRooms({
      parkId: mergeForm.parkId,
      buildingId: mergeForm.buildingId,
      floorId: mergeForm.floorId,
      floor: mergeForm.floor,
      roomNo: mergeForm.roomNo,
      roomName: mergeForm.roomName || mergeForm.roomNo,
      roomType: mergeForm.roomType,
      areaCovered: mergeForm.areaCovered,
      buildArea: mergeForm.buildArea,
      billableArea: mergeForm.billableArea,
      unitPrice: mergeForm.unitPrice,
      monthlyRent: mergeForm.monthlyRent,
      reasons: mergeForm.reasons || undefined,
      oldRoomIds: mergeForm.oldRoomIds,
    })
    if (res.code === 200) {
      ElMessage.success('合并成功')
      mergeDialogVisible.value = false
      clearSelection()
      await loadData()
    } else {
      ElMessage.error(res.msg || '合并失败')
    }
  } finally {
    mergeSubmitting.value = false
  }
}

const dialogTitle = computed(() => {
  if (dialogMode.value === 'add') return '新增房间'
  if (dialogMode.value === 'edit') return '编辑房间'
  return '查看房间'
})

// 当前楼层名称 (4 层树: park → area → building → floor)
const currentFloorName = computed(() => {
  if (!activeFloorId.value) return ''
  for (const park of parkTree.value) {
    for (const area of park.$areaList || []) {
      for (const building of area.$buildingList || []) {
        const floor = building.$floorList?.find((f) => f.id === activeFloorId.value)
        if (floor) return floor.floorName
      }
    }
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
      <!-- 左侧树形导航 (el-tree) -->
      <div class="basic-card-left">
        <div class="left-bar-title">导航</div>
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          highlight-current
          :current-node-key="currentNodeKey"
          :default-expanded-keys="defaultExpandedKeys"
          @node-click="onTreeNodeClick"
          class="building-tree"
        >
          <template #default="{ data }">
            <span class="common-ellipsis" :title="data.label">{{ data.label }}</span>
          </template>
        </el-tree>
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
            <el-button size="small" type="primary" icon="el-icon-plus" @click="handleAdd" v-if="activeFloorId">
              新增房间
            </el-button>
            <!-- C6: 合并按钮 (2+ 选中时显示) -->
            <el-button
              size="small"
              type="success"
              icon="el-icon-merge"
              :disabled="selectedRows.length < 2"
              @click="handleMerge"
              v-if="activeFloorId"
            >
              合并 ({{ selectedRows.length }})
            </el-button>
            <el-button size="small" type="primary" plain icon="el-icon-download" @click="handleImport">
              房间导入
            </el-button>
            <el-radio-group v-model="filterStatus" class="status-list" @change="onStatusChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="0">空置</el-radio-button>
              <el-radio-button label="1">已租</el-radio-button>
              <el-radio-button label="2">已售</el-radio-button>
              <el-radio-button label="3">锁定</el-radio-button>
              <el-radio-button label="4">预订</el-radio-button>
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

        <!-- 房间表格 -->
        <div class="page-table">
          <el-empty v-if="!loading && activeFloorId && tableData.length === 0" description="暂无房间" />
          <el-empty v-else-if="!activeFloorId" description="请在左侧选择楼层" />

            <el-table :data="tableData" border stripe v-loading="loading"
              @selection-change="onSelectionChange"
              v-if="activeFloorId && tableData.length > 0">
              <el-table-column type="selection" width="48" />
              <el-table-column prop="roomNo" label="房号" width="120" />
              <el-table-column prop="roomName" label="房间名称" min-width="120" show-overflow-tooltip />
              <el-table-column label="锁定" width="60" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.isLock === 1" type="warning" size="small">已锁</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="statusTagType(scope.row.status)" size="small">
                    {{ statusLabel(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="房间用途" width="100" show-overflow-tooltip>
                <template #default="scope">{{ purposeName(scope.row.purposeId) }}</template>
              </el-table-column>
              <el-table-column label="建筑面积" width="100" align="right">
                <template #default="scope">{{ scope.row.areaCovered ? Number(scope.row.areaCovered).toLocaleString() : '-' }} ㎡</template>
              </el-table-column>
              <el-table-column label="套内面积" width="100" align="right">
                <template #default="scope">{{ scope.row.buildArea ? Number(scope.row.buildArea).toLocaleString() : '-' }} ㎡</template>
              </el-table-column>
              <el-table-column label="计费面积" width="100" align="right">
                <template #default="scope">{{ scope.row.billableArea ? Number(scope.row.billableArea).toLocaleString() : '-' }} ㎡</template>
              </el-table-column>
              <el-table-column label="单价" width="110" align="right">
                <template #default="scope">{{ scope.row.unitPrice ? Number(scope.row.unitPrice).toLocaleString() : '-' }} 元/㎡/月</template>
              </el-table-column>
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="scope">
                  <el-button link type="primary" size="small" @click="handleView(scope.row)">查看</el-button>
                  <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                  <el-button link type="primary" size="small" @click="handleSplit(scope.row)">拆分</el-button>
                  <el-button v-if="scope.row.isLock === 1" link type="warning" size="small" @click="handleUnlock(scope.row)">解锁</el-button>
                  <el-button v-else link type="primary" size="small" @click="handleLock(scope.row)">锁定</el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

          <!-- 分页 -->
          <el-pagination
            v-if="total >= 10"
            class="common-pagination"
            background
            layout="total, sizes, prev, pager, next, jumper"
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
      :width="dialogMode === 'view' ? '1100px' : '900px'"
      :style="{ maxWidth: '90vw' }"
      :close-on-click-modal="false"
      @close="Object.assign(form, defaultForm())"
    >
      <!-- 查看模式: 概要 + 3 个子表 tabs -->
      <template v-if="dialogMode === 'view'">
        <el-descriptions
          class="room-detail-descriptions"
          :column="3"
          border
          size="default"
          title="基本信息"
        >
          <el-descriptions-item label="所属园区">{{ parkNameOf(form.parkId) }}</el-descriptions-item>
          <el-descriptions-item label="所属楼栋">{{ buildingNameOf(form.parkId, form.buildingId) }}</el-descriptions-item>
          <el-descriptions-item label="所属楼层">{{ floorNameOf(form.parkId, form.buildingId, form.floorId) }}</el-descriptions-item>
          <el-descriptions-item label="房间编号">{{ form.roomNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房间名称">{{ form.roomName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(form.status)" size="small">{{ statusLabel(form.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="建筑面积">{{ fmtArea(form.areaCovered) }}</el-descriptions-item>
          <el-descriptions-item label="套内面积">{{ fmtArea(form.buildArea) }}</el-descriptions-item>
          <el-descriptions-item label="计费面积">{{ fmtArea(form.billableArea) }}</el-descriptions-item>
          <el-descriptions-item label="单价">{{ fmtPrice(form.unitPrice, '元/㎡/月') }}</el-descriptions-item>
          <el-descriptions-item label="总价">{{ fmtPrice(form.totalPrice, '元/月') }}</el-descriptions-item>
          <el-descriptions-item label="月租金">{{ fmtPrice(form.monthlyRent) }}</el-descriptions-item>
          <el-descriptions-item label="房间配套">{{ kitName(form.kitId) }}</el-descriptions-item>
          <el-descriptions-item label="房间用途">{{ purposeName(form.purposeId) }}</el-descriptions-item>
          <el-descriptions-item label="房屋结构">{{ dictMap.room_structure?.find(d => Number(d.value) === form.houseStructure)?.label || '-' }}</el-descriptions-item>
          <el-descriptions-item label="租售状态">{{ rentingSellingLabel(form.rentingSelling) }}</el-descriptions-item>
          <el-descriptions-item label="租价">{{ form.leasePrice != null ? Number(form.leasePrice).toFixed(2) + ' 元/㎡/天' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="售价">{{ form.salePrice != null ? Number(form.salePrice).toLocaleString() + ' 元/㎡' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="排序">{{ form.sorting ?? 0 }}</el-descriptions-item>
          <el-descriptions-item v-if="(form as any).introduce" label="房间介绍" :span="3">
            {{ (form as any).introduce }}
          </el-descriptions-item>
          <el-descriptions-item v-if="form.remark" label="备注" :span="3">
            {{ form.remark }}
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTabName" class="room-detail-tabs" @tab-click="onTabClick">
          <!-- 锁定记录 -->
          <el-tab-pane label="锁定记录" name="lock">
            <el-table :data="tabData.lock" v-loading="tabLoading.lock" border stripe size="small">
              <el-table-column prop="createTime" label="操作时间" width="170" />
              <el-table-column prop="operator" label="操作人" width="120" />
              <el-table-column label="操作类型" width="90" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.isLock === 1 ? 'warning' : 'success'" size="small">
                    {{ scope.row.isLock === 1 ? '锁定' : '解锁' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="enterpriseName" label="关联企业" min-width="140" />
              <el-table-column prop="days" label="锁定天数" width="90" align="right" />
              <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
              <template #empty>
                <el-empty description="暂无锁定记录" :image-size="60" />
              </template>
            </el-table>
            <el-pagination
              v-if="tabTotal.lock >= 10"
              class="common-pagination"
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="tabTotal.lock"
              :page-sizes="[10, 20, 50]"
              :page-size="tabSize.lock"
              :current-page.sync="tabPage.lock"
              @size-change="(v: number) => onTabSizeChange('lock', v)"
              @current-change="(v: number) => onTabPageChange('lock', v)"
            />
          </el-tab-pane>

          <!-- 拆分合并 -->
          <el-tab-pane label="拆分合并" name="split">
            <el-table :data="tabData.split" v-loading="tabLoading.split" border stripe size="small">
              <el-table-column prop="createTime" label="操作时间" width="170" />
              <el-table-column prop="userName" label="操作人" width="120" />
              <el-table-column label="类型" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.type === 1 ? 'primary' : (scope.row.type === 2 ? 'info' : 'warning')" size="small">
                    {{ splitMergeTypeLabel(scope.row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="oldRoomName" label="原房间" min-width="120" />
              <el-table-column prop="newRoomName" label="新房间" min-width="120" />
              <el-table-column prop="num" label="数量" width="80" align="right" />
              <el-table-column prop="reasons" label="原因" min-width="160" show-overflow-tooltip />
              <template #empty>
                <el-empty description="暂无拆分合并记录" :image-size="60" />
              </template>
            </el-table>
            <el-pagination
              v-if="tabTotal.split >= 10"
              class="common-pagination"
              background
              layout="total, prev, pager, next, jumper"
              :total="tabTotal.split"
              :page-size="tabSize.split"
              :current-page.sync="tabPage.split"
              @current-change="(v: number) => onTabPageChange('split', v)"
            />
          </el-tab-pane>

          <!-- 房间记录 (绑定/解绑历史) -->
          <el-tab-pane label="房间记录" name="record">
            <el-table :data="tabData.record" v-loading="tabLoading.record" border stripe size="small">
              <el-table-column prop="createTime" label="操作时间" width="170" />
              <el-table-column label="操作" width="100" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                    {{ recordTypeLabel(scope.row.covenantType, scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="customerId" label="客户 ID" width="100" align="right" />
              <el-table-column prop="covenantId" label="合同 ID" width="100" align="right" />
              <template #empty>
                <el-empty description="暂无房间记录" :image-size="60" />
              </template>
            </el-table>
            <el-pagination
              v-if="tabTotal.record >= 10"
              class="common-pagination"
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="tabTotal.record"
              :page-sizes="[10, 20, 50]"
              :page-size="tabSize.record"
              :current-page.sync="tabPage.record"
              @size-change="(v: number) => onTabSizeChange('record', v)"
              @current-change="(v: number) => onTabPageChange('record', v)"
            />
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 新增/编辑模式: 原表单 -->
      <el-form
        v-else
        :model="form"
        label-width="120px"
        :rules="formRules"
        ref="formRef"
        :validate-on-rule-change="false"
      >
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="所属园区" prop="parkId">
              <el-select v-model="form.parkId" placeholder="请选择园区" filterable @change="onFormParkChange">
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
            <el-form-item label="所属分区" prop="areaId">
              <el-select v-model="form.areaId" placeholder="请选择分区" filterable clearable @change="onFormAreaChange">
                <el-option
                  v-for="a in (parkTree.find(p => p.id === form.parkId)?.$areaList || [])"
                  :key="a.id"
                  :label="a.areaName"
                  :value="a.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属楼栋" prop="buildingId">
              <el-select v-model="form.buildingId" placeholder="请选择楼栋" filterable @change="onFormBuildingChange">
                <el-option
                  v-for="b in (parkTree.find(p => p.id === form.parkId)?.$areaList?.find(a => a.id === form.areaId)?.$buildingList || parkTree.find(p => p.id === form.parkId)?.$buildingList || [])"
                  :key="b.id"
                  :label="b.buildingName"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属楼层" prop="floorId">
              <el-select v-model="form.floorId" placeholder="请选择楼层" filterable clearable @change="onFormFloorChange">
                <el-option
                  v-for="f in (parkTree.find(p => p.id === form.parkId)?.$areaList?.find(a => a.id === form.areaId)?.$buildingList?.find(b => b.id === form.buildingId)?.$floorList || parkTree.find(p => p.id === form.parkId)?.$buildingList?.find(b => b.id === form.buildingId)?.$floorList || [])"
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
          <el-col :span="24">
            <el-form-item label="房屋状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="d in dictMap.room_status || []" :key="d.value" :value="Number(d.value)">
                  {{ d.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="租售状态" prop="rentingSelling">
              <el-radio-group v-model="form.rentingSelling">
                <el-radio :value="0">可租</el-radio>
                <el-radio :value="1">可售</el-radio>
                <el-radio :value="2">可租售</el-radio>
                <el-radio :value="3">自用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="租价" prop="leasePrice">
              <el-input-number v-model="form.leasePrice" :precision="2" :min="0" style="width: 100%" placeholder="元/㎡/天" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number v-model="form.salePrice" :precision="2" :min="0" style="width: 100%" placeholder="元/㎡" />
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
        <el-button @click="dialogVisible = false">{{ dialogMode === 'view' ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="dialogMode !== 'view'"
          type="primary"
          @click="handleSubmit"
          :loading="submitting"
        >确定</el-button>
      </template>
    </el-dialog>

    <!-- ============ 拆分 dialog (csyh 模式: 子表 + 末房抹平尾差) ============ -->
    <el-dialog
      v-model="splitDialogVisible"
      title="房源拆分"
      width="900px"
      :close-on-click-modal="false"
      @close="splitDialogOld = null"
    >
      <div v-if="splitDialogOld" class="form-bg-box">
        <el-form-item label="原房间号">{{ splitDialogOld.roomNo }}</el-form-item>
        <el-form-item label="建筑面积">{{ splitDialogOld.areaCovered ?? 0 }} ㎡</el-form-item>
        <el-form-item label="套内面积">{{ splitDialogOld.buildArea ?? 0 }} ㎡</el-form-item>
      </div>
      <el-form label-width="100px">
        <el-form-item label="拆分数量">
          <el-input-number v-model="splitForm.num" :min="2" :max="99" controls-position="right" style="width: 180px" />
          <el-button type="primary" plain style="margin-left: 12px" @click="generateSplitPreview">拆 分</el-button>
        </el-form-item>
        <div v-if="splitForm.roomList.length" class="subtable-card">
          <div class="subtable-card-title">
            拆分后房间
            <el-button link type="primary" size="small" @click="resetSplitPreview">重置成平均</el-button>
          </div>
          <el-table :data="splitForm.roomList" border size="small" class="form-subtable">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column label="新房间号">
              <template #default="{ row }">
                <el-input v-model="row.roomNo" size="small" placeholder="新房间号" />
              </template>
            </el-table-column>
            <el-table-column label="新房间名称">
              <template #default="{ row }">
                <el-input v-model="row.roomName" size="small" placeholder="新房间名称" />
              </template>
            </el-table-column>
            <el-table-column label="建筑面积(㎡)">
              <template #default="{ row }">
                <el-input-number v-model="row.areaCovered" :precision="2" :min="0" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="套内面积(㎡)">
              <template #default="{ row }">
                <el-input-number v-model="row.buildArea" :precision="2" :min="0" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-form-item label="拆分说明" style="margin-top: 16px">
          <el-input v-model="splitForm.reasons" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入拆分说明 (可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="splitDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="splitSubmitting" @click="confirmSplit">确认拆分</el-button>
      </template>
    </el-dialog>

    <!-- ============ 合并 dialog (csyh 模式: 已选择子表 + 合并后表单) ============ -->
    <el-dialog
      v-model="mergeDialogVisible"
      title="房源合并"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="subtable-card">
        <div class="subtable-card-title">已选择房源 ({{ mergeForm.oldRoomIds.length }})</div>
        <el-table :data="selectedRows" border size="small" class="form-subtable">
          <el-table-column prop="roomNo" label="原房间号" />
          <el-table-column prop="roomName" label="原房间名称" />
          <el-table-column label="建筑面积(㎡)">
            <template #default="{ row }">{{ row.areaCovered ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="套内面积(㎡)">
            <template #default="{ row }">{{ row.buildArea ?? '-' }}</template>
          </el-table-column>
        </el-table>
      </div>
      <el-form label-width="100px" style="margin-top: 16px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="新房间号" required>
              <el-input v-model="mergeForm.roomNo" placeholder="新房间号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新房间名称">
              <el-input v-model="mergeForm.roomName" placeholder="新房间名称 (默认同房号)" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新建筑面积">
              <el-input-number v-model="mergeForm.areaCovered" :precision="2" :min="0" style="width: 100%" />
              <span class="hint-text">(已自动 sum = {{ mergeForm.areaCovered }} ㎡，可修改)</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新套内面积">
              <el-input-number v-model="mergeForm.buildArea" :precision="2" :min="0" style="width: 100%" />
              <span class="hint-text">(已自动 sum = {{ mergeForm.buildArea }} ㎡，可修改)</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="合并说明" style="margin-top: 8px">
          <el-input v-model="mergeForm.reasons" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入合并说明 (可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="mergeSubmitting" @click="confirmMerge">确认合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-cover-container {
  height: 100%;
}

.common-flex {
  height: 100%;
  width: 100%;
  display: flex;
  min-width: 0;
}

.basic-card-left {
  width: 220px;
  flex: 0 0 220px;
  border-right: 1px solid #d8d8d8;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.basic-card-right {
  flex: 1 1 0;
  min-width: 0;
  max-width: 100%;
  padding: 16px 16px;
  overflow-y: auto;
  background: #f5f7fa;
  box-sizing: border-box;
}

.left-bar-title {
  padding: 16px 24px 8px;
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
}

.building-tree {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 0;
}

/* 树节点文本溢出省略 */
.common-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 100%;
  vertical-align: middle;
}

/* 房间详情查看模式 (el-descriptions + el-tabs) */
.room-detail-descriptions {
  margin-bottom: 16px;
}

.room-detail-descriptions :deep(.el-descriptions__title) {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.room-detail-tabs {
  margin-top: 8px;
}

.room-detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.room-detail-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.room-detail-tabs :deep(.el-table) {
  font-size: 13px;
}

/* ============ 拆分/合并 dialog 样式 (csyh 移植) ============ */
.form-bg-box {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 2px;
  padding: 16px 24px;
  margin-bottom: 16px;
  display: flex;
  gap: 32px;
  white-space: nowrap;
}
.subtable-card {
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 2px;
  margin-bottom: 16px;
}
.subtable-card-title {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
}
.form-subtable :deep(.el-table) {
  font-size: 13px;
}
.hint-text {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.room-detail-tabs :deep(.common-pagination) {
  margin-top: 12px;
  text-align: right;
}

.data-null-text {
  color: rgba(0, 0, 0, 0.25);
  font-size: 12px;
  padding: 8px 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
  row-gap: 12px;
}

.page-header .page-header-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin-right: 16px;
  flex: 0 0 auto;
  white-space: nowrap;
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
  flex: 1 1 auto;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.page-header .status-list {
  display: inline-flex;
  flex-wrap: wrap;
}

.page-header .status-list :deep(.el-radio-button__inner) {
  border: 1px solid #dcdfe6;
  padding: 6px 10px;
  font-size: 12px;
}

.page-header .search-input {
  width: 200px;
  flex: 0 0 auto;
}

.page-table {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
}

.page-table :deep(.el-table) {
  width: 100%;
}

.page-table :deep(.el-table .cell) {
  word-break: break-all;
  white-space: normal;
}

.page-table :deep(.el-table td:not(:first-child):not(:nth-child(2)):not(:nth-child(3)) .cell) {
  font-variant-numeric: tabular-nums;
}

.common-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
