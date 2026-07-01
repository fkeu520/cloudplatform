<template>
  <div class="page-container">
    <el-card class="header-card">
      <div class="header-row">
        <div>
          <h3>企业评分规则</h3>
          <p class="tip">每租户 4 行规则 (1=优 2=良 3=中 4=差), 触发条件: 逾期次数/欠费金额/日期数。修改后保存会替换当前租户全部规则。</p>
        </div>
        <div class="actions">
          <el-button type="warning" @click="handleReset">重置默认</el-button>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column label="等级" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getLevelTagType(scope.row.level)">{{ getLevelLabel(scope.row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="逾期次数 (起)" width="140">
          <template #default="scope">
            <el-input-number v-model="scope.row.overdueMin" :min="0" :max="999" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="逾期次数 (止)" width="140">
          <template #default="scope">
            <el-input-number v-model="scope.row.overdueMax" :min="0" :max="999" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="欠费金额 (起) 元" width="180">
          <template #default="scope">
            <el-input-number v-model="scope.row.debtsMin" :min="0" :precision="2" :step="1000" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="欠费金额 (止) 元" width="180">
          <template #default="scope">
            <el-input-number v-model="scope.row.debtsMax" :min="0" :precision="2" :step="1000" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="日期数" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.dateNum" :min="1" :max="365" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="日期单位" width="120">
          <template #default="scope">
            <el-select v-model="scope.row.dateUnit" style="width: 100%">
              <el-option label="天" value="day" />
              <el-option label="月" value="month" />
              <el-option label="年" value="year" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRatingList, saveRatingBatch, resetRatingDefault, type Rating } from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<Rating[]>([])

const getLevelLabel = (l?: number) => {
  switch (l) {
    case 1: return '优'
    case 2: return '良'
    case 3: return '中'
    case 4: return '差'
    default: return '-'
  }
}

const getLevelTagType = (l?: number) => {
  switch (l) {
    case 1: return 'success'
    case 2: return ''
    case 3: return 'warning'
    case 4: return 'danger'
    default: return 'info'
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getRatingList()
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  try {
    await ElMessageBox.confirm('保存将替换当前租户全部评级规则, 继续?', '提示', { type: 'warning' })
    await saveRatingBatch(tableData.value)
    ElMessage.success('保存成功')
    loadData()
  } catch (e) {
    // user cancelled
  }
}

const handleReset = async () => {
  try {
    await ElMessageBox.confirm('重置将删除当前所有规则并恢复默认 4 等级, 继续?', '提示', { type: 'warning' })
    await resetRatingDefault()
    ElMessage.success('重置成功')
    loadData()
  } catch (e) {
    // user cancelled
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.header-card { margin-bottom: 16px; }
.header-row { display: flex; justify-content: space-between; align-items: flex-start; }
.header-row h3 { margin: 0 0 8px 0; }
.tip { color: #909399; font-size: 13px; margin: 0; }
.actions { display: flex; gap: 8px; }
.table-card { margin-bottom: 16px; }
</style>
