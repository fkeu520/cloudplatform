<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="园区"><el-select v-model="searchForm.parkId" placeholder="全部园区" clearable filterable><el-option v-for="p in parkOptions" :key="p.id" :label="p.parkName" :value="p.id" /></el-select></el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable>
            <el-option label="拆分" :value="1" /><el-option label="合并" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="合并" :value="0" /><el-option label="拆分" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        房间拆分/合并历史记录（append-only），只能新增和查询
      </el-alert>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column label="园区" width="120">
          <template #default="scope">{{ parkMap[scope.row.parkId] || scope.row.parkId }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="操作人" width="120" />
        <el-table-column prop="oldRoomName" label="原房源" min-width="150" />
        <el-table-column prop="newRoomName" label="新房源" min-width="150" />
        <el-table-column prop="num" label="拆分数量" width="100" align="right" />
        <el-table-column label="继承能源" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isExtend === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.isExtend === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'warning' : 'primary'" size="small">
              {{ scope.row.status === 1 ? '拆分' : '合并' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reasons" label="原因" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="createTime" label="操作时间" width="160" />
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getRoomSplitMergePage } from '@/api/room-split-merge'
import { getParkList } from '@/api/park'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10)
const searchForm = reactive({ parkId: undefined as string | undefined, type: undefined as number | undefined, status: undefined as number | undefined })
const parkOptions = ref<any[]>([]); const parkMap = ref<Record<string, string>>({})

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRoomSplitMergePage({ ...searchForm, pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}
function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { searchForm.parkId=undefined; searchForm.type=undefined; searchForm.status=undefined; handleSearch() }
function handleSizeChange(v: number) { pageSize.value = v; loadData() }
function handlePageChange(v: number) { pageNum.value = v; loadData() }

async function loadParkOptions() {
  try { const res: any = await getParkList(); if (res.code === 200) { parkOptions.value = res.data || []; parkOptions.value.forEach((p: any) => parkMap.value[String(p.id)] = p.parkName) } } catch { /* ignore */ }
}

onMounted(() => { loadData(); loadParkOptions() })
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; flex-direction: row-reverse; }
</style>