<script setup lang="ts">
/**
 * 租售控制管理 (P1#7 质量修复)
 *
 * 设计: 搜索栏 + 表格展示 + 编辑/批量/锁定弹窗
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/control/child-lease-sale/
 */
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRoomControlPage,
  updateRoomControl,
  batchUpdateRoomControl,
  lockRoom,
  unlockRoom,
  type RoomControl,
} from '@/api/room-control'
import { getParkList, type Park } from '@/api/park'
import { getBuildingPage, type Building } from '@/api/building'
import { listFloorByBuilding, type Floor } from '@/api/floor'

// ============== 状态 ==============
const loading = ref(false)
const tableData = ref<RoomControl[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const pageSizes = [10, 20, 50, 100]

// 园区/楼栋/楼层联动
const parkOptions = ref<Park[]>([])
const buildingOptions = ref<Building[]>([])
const floorOptions = ref<Floor[]>([])

const searchForm = reactive({
  parkId: undefined as number | undefined,
  buildingId: undefined as number | undefined,
  floorId: undefined as number | undefined,
  rentingSelling: undefined as number | undefined,
  isLock: undefined as number | undefined,
  keyword: '',
})

// 租售状态字典
const RENTING_SELLING_OPTIONS = [
  { value: 0, label: '可租' },
  { value: 1, label: '可售' },
  { value: 2, label: '可租售' },
  { value: 3, label: '自用' },
]

// ============== 弹窗 ==============
// 编辑弹窗
const editDialogVisible = ref(false)
const editForm = reactive({
  id: 0,
  roomNo: '',
  roomName: '',
  rentingSelling: undefined as number | undefined,
  leasePrice: undefined as number | undefined,
  salePrice: undefined as number | undefined,
  isOrder: 0,
})
const editPriceExpanded = ref(false)
const editSubmitting = ref(false)

// 批量弹窗
const batchDialogVisible = ref(false)
const batchForm = reactive({
  rentingSelling: undefined as number | undefined,
  leasePrice: undefined as number | undefined,
  salePrice: undefined as number | undefined,
})
const selectedIds = ref<number[]>([])
const batchSubmitting = ref(false)

// 锁定弹窗
const lockDialogVisible = ref(false)
const lockForm = reactive({
  roomId: 0,
  roomNo: '',
  reason: '',
  enterpriseName: '',
  days: undefined as number | undefined,
})
const lockSubmitting = ref(false)

// 解锁弹窗
const unlockDialogVisible = ref(false)
const unlockForm = reactive({
  roomId: 0,
  roomNo: '',
  reason: '',
})
const unlockSubmitting = ref(false)

// ============== 数据加载 ==============
async function loadParks() {
  try {
    const res: any = await getParkList()
    if (res.code === 200) {
      parkOptions.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadBuildings(parkId: number) {
  if (!parkId) {
    buildingOptions.value = []
    floorOptions.value = []
    return
  }
  try {
    const res: any = await getBuildingPage({ parkId, pageNum: 1, pageSize: 9999 } as any)
    if (res.code === 200) {
      buildingOptions.value = res.data?.records || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadFloors(buildingId: number) {
  if (!buildingId) {
    floorOptions.value = []
    return
  }
  try {
    const res: any = await listFloorByBuilding(buildingId)
    if (res.code === 200) {
      floorOptions.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRoomControlPage({
      parkId: searchForm.parkId || undefined,
      buildingId: searchForm.buildingId || undefined,
      floorId: searchForm.floorId || undefined,
      rentingSelling: searchForm.rentingSelling,
      isLock: searchForm.isLock,
      keyword: searchForm.keyword || undefined,
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

function handleReset() {
  searchForm.parkId = undefined
  searchForm.buildingId = undefined
  searchForm.floorId = undefined
  searchForm.rentingSelling = undefined
  searchForm.isLock = undefined
  searchForm.keyword = ''
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

// 联动
function onParkChange(parkId: number) {
  searchForm.buildingId = undefined
  searchForm.floorId = undefined
  loadBuildings(parkId)
  handleSearch()
}

function onBuildingChange(buildingId: number) {
  searchForm.floorId = undefined
  loadFloors(buildingId)
  handleSearch()
}

function onFloorChange() {
  handleSearch()
}

// ============== 辅助函数 ==============
function rentingSellingLabel(val: number | undefined): string {
  if (val === undefined || val === null) return '-'
  return RENTING_SELLING_OPTIONS.find(o => o.value === val)?.label || String(val)
}

function rentingSellingTagType(val: number | undefined): string {
  if (val === 0) return 'success'   // 可租
  if (val === 1) return 'warning'   // 可售
  if (val === 2) return 'primary'   // 可租售
  if (val === 3) return 'info'      // 自用
  return 'info'
}

function statusLabel(s: number | undefined): string {
  switch (s) {
    case 0: return '空置'
    case 1: return '已租'
    case 2: return '已售'
    case 3: return '锁定'
    case 4: return '预订'
    default: return '-'
  }
}

// ============== 编辑弹窗 ==============
function openEditDialog(row: RoomControl) {
  Object.assign(editForm, {
    id: row.id || 0,
    roomNo: row.roomNo || '',
    roomName: row.roomName || '',
    rentingSelling: row.rentingSelling,
    leasePrice: row.leasePrice,
    salePrice: row.salePrice,
    isOrder: row.isOrder || 0,
  })
  editPriceExpanded.value = editForm.leasePrice != null || editForm.salePrice != null
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  if (editForm.rentingSelling === undefined || editForm.rentingSelling === null) {
    ElMessage.warning('请选择租售状态')
    return
  }
  editSubmitting.value = true
  try {
    const res: any = await updateRoomControl(editForm.id, {
      rentingSelling: editForm.rentingSelling,
      leasePrice: editForm.leasePrice,
      salePrice: editForm.salePrice,
      isOrder: editForm.isOrder,
    })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } finally {
    editSubmitting.value = false
  }
}

// ============== 批量弹窗 ==============
function openBatchDialog() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先在表格中选择要批量更新的房间')
    return
  }
  batchForm.rentingSelling = undefined
  batchForm.leasePrice = undefined
  batchForm.salePrice = undefined
  batchDialogVisible.value = true
}

// 提供给 el-table selection-change
function onSelectionChange(rows: RoomControl[]) {
  selectedIds.value = rows.map(r => r.id!).filter(Boolean)
}

async function handleBatchSubmit() {
  if (batchForm.rentingSelling === undefined && batchForm.leasePrice === undefined && batchForm.salePrice === undefined) {
    ElMessage.warning('请至少设置一项')
    return
  }
  batchSubmitting.value = true
  try {
    const res: any = await batchUpdateRoomControl({
      ids: selectedIds.value,
      rentingSelling: batchForm.rentingSelling,
      leasePrice: batchForm.leasePrice,
      salePrice: batchForm.salePrice,
    })
    if (res.code === 200) {
      ElMessage.success(`批量更新成功 (${selectedIds.value.length} 条)`)
      batchDialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } finally {
    batchSubmitting.value = false
  }
}

// ============== 锁定/解锁 ==============
function openLockDialog(row: RoomControl) {
  if (row.isLock === 1) {
    ElMessage.warning('该房间已锁定')
    return
  }
  lockForm.roomId = row.id || 0
  lockForm.roomNo = row.roomNo || ''
  lockForm.reason = ''
  lockForm.enterpriseName = ''
  lockForm.days = undefined
  lockDialogVisible.value = true
}

async function handleLockSubmit() {
  if (!lockForm.reason) {
    ElMessage.warning('请输入锁定原因')
    return
  }
  lockSubmitting.value = true
  try {
    const res: any = await lockRoom({
      roomId: lockForm.roomId,
      reason: lockForm.reason,
      enterpriseName: lockForm.enterpriseName || undefined,
      days: lockForm.days,
    })
    if (res.code === 200) {
      ElMessage.success('锁定成功')
      lockDialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } finally {
    lockSubmitting.value = false
  }
}

function openUnlockDialog(row: RoomControl) {
  if (!row.isLock) {
    ElMessage.warning('该房间未锁定')
    return
  }
  unlockForm.roomId = row.id || 0
  unlockForm.roomNo = row.roomNo || ''
  unlockForm.reason = ''
  unlockDialogVisible.value = true
}

async function handleUnlockSubmit() {
  if (!unlockForm.reason) {
    ElMessage.warning('请输入解锁原因')
    return
  }
  unlockSubmitting.value = true
  try {
    const res: any = await unlockRoom({
      roomId: unlockForm.roomId,
      reason: unlockForm.reason,
    })
    if (res.code === 200) {
      ElMessage.success('解锁成功')
      unlockDialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } finally {
    unlockSubmitting.value = false
  }
}

// ============== 生命周期 ==============
onMounted(async () => {
  await loadParks()
  await loadData()
})

watch(() => searchForm.parkId, (val, oldVal) => {
  if (val !== oldVal) {
    searchForm.buildingId = undefined
    searchForm.floorId = undefined
    if (val) loadBuildings(val)
    handleSearch()
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="园区">
          <el-select v-model="searchForm.parkId" placeholder="请选择园区" clearable filterable style="width: 180px">
            <el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="Number(p.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="searchForm.buildingId" placeholder="全部楼栋" clearable filterable style="width: 180px" @change="onBuildingChange">
            <el-option v-for="b in buildingOptions" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="searchForm.floorId" placeholder="全部楼层" clearable filterable style="width: 160px" @change="onFloorChange">
            <el-option v-for="f in floorOptions" :key="f.id" :label="f.floorName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="租售状态">
          <el-select v-model="searchForm.rentingSelling" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in RENTING_SELLING_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="锁定状态">
          <el-select v-model="searchForm.isLock" placeholder="全部" clearable style="width: 120px">
            <el-option label="未锁定" :value="0" />
            <el-option label="已锁定" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="房号/名称" clearable style="width: 180px" @change="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" plain @click="openBatchDialog">批量设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        @selection-change="onSelectionChange"
        @row-dblclick="openEditDialog"
      >
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column prop="roomNo" label="房号" width="120" />
        <el-table-column prop="roomName" label="房间名称" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column label="房源状态" width="80" align="center">
          <template #default="scope">
            <span>{{ statusLabel(scope.row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="租售状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="rentingSellingTagType(scope.row.rentingSelling)" size="small">
              {{ rentingSellingLabel(scope.row.rentingSelling) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="leasePrice" label="租价(元/㎡/天)" width="140" align="right">
          <template #default="scope">
            {{ scope.row.leasePrice != null ? Number(scope.row.leasePrice).toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价(元/㎡)" width="140" align="right">
          <template #default="scope">
            {{ scope.row.salePrice != null ? Number(scope.row.salePrice).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="锁定" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isLock === 1 ? 'danger' : 'info'" size="small">
              {{ scope.row.isLock === 1 ? '已锁定' : '未锁定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建筑面积" width="100" align="right">
          <template #default="scope">
            {{ scope.row.areaCovered ? Number(scope.row.areaCovered).toFixed(1) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button
              v-if="scope.row.isLock !== 1"
              size="small"
              link
              type="warning"
              @click="openLockDialog(scope.row)"
            >锁定</el-button>
            <el-button
              v-else
              size="small"
              link
              type="success"
              @click="openUnlockDialog(scope.row)"
            >解锁</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="pageSizes"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑租售控制" width="500px" :close-on-click-modal="false">
      <el-form label-width="120px">
        <el-form-item label="房间">
          <span>{{ editForm.roomNo }} {{ editForm.roomName ? `(${editForm.roomName})` : '' }}</span>
        </el-form-item>
        <el-form-item label="租售状态">
          <el-radio-group v-model="editForm.rentingSelling">
            <el-radio v-for="o in RENTING_SELLING_OPTIONS" :key="o.value" :value="o.value">
              {{ o.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="价格详情">
          <el-button
            size="small"
            link
            :type="editPriceExpanded ? 'primary' : 'default'"
            @click="editPriceExpanded = !editPriceExpanded"
          >
            {{ editPriceExpanded ? '收起' : '展开' }}
            <el-icon :class="{ 'is-reverse': editPriceExpanded }" style="transition: transform 0.2s;">
              <ArrowDown />
            </el-icon>
          </el-button>
        </el-form-item>
        <template v-if="editPriceExpanded">
          <el-form-item label="租价(元/㎡/天)">
            <el-input-number v-model="editForm.leasePrice" :precision="2" :min="0" style="width: 100%" placeholder="输入租价" />
          </el-form-item>
          <el-form-item label="售价(元/㎡)">
            <el-input-number v-model="editForm.salePrice" :precision="2" :min="0" style="width: 100%" placeholder="输入售价" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量设置弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量设置租售控制" width="500px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        已选择 {{ selectedIds.length }} 个房间，将统一设置以下字段
      </el-alert>
      <el-form label-width="120px">
        <el-form-item label="租售状态">
          <el-radio-group v-model="batchForm.rentingSelling">
            <el-radio :value="0">可租</el-radio>
            <el-radio :value="1">可售</el-radio>
            <el-radio :value="2">可租售</el-radio>
            <el-radio :value="3">自用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="租价(元/㎡/天)">
          <el-input-number v-model="batchForm.leasePrice" :precision="2" :min="0" style="width: 100%" placeholder="留空不修改" />
        </el-form-item>
        <el-form-item label="售价(元/㎡)">
          <el-input-number v-model="batchForm.salePrice" :precision="2" :min="0" style="width: 100%" placeholder="留空不修改" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">批量更新</el-button>
      </template>
    </el-dialog>

    <!-- 锁定弹窗 -->
    <el-dialog v-model="lockDialogVisible" title="锁定房间" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="房间">
          <span>{{ lockForm.roomNo }}</span>
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="lockForm.enterpriseName" placeholder="预留客户名称(可选)" maxlength="100" />
        </el-form-item>
        <el-form-item label="锁定天数">
          <el-input-number v-model="lockForm.days" :min="1" :max="3650" style="width: 100%" placeholder="留空不限" />
        </el-form-item>
        <el-form-item label="锁定原因" required>
          <el-input v-model="lockForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入锁定原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lockDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="lockSubmitting" @click="handleLockSubmit">确认锁定</el-button>
      </template>
    </el-dialog>

    <!-- 解锁弹窗 -->
    <el-dialog v-model="unlockDialogVisible" title="解锁房间" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="房间">
          <span>{{ unlockForm.roomNo }}</span>
        </el-form-item>
        <el-form-item label="解锁原因" required>
          <el-input v-model="unlockForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入解锁原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unlockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="unlockSubmitting" @click="handleUnlockSubmit">确认解锁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
}
.search-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
