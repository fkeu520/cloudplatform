<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>流程实例监控</span>
          <el-button @click="fetchData">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item label="流程定义Key">
          <el-input v-model="query.processDefinitionKey" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:120px">
            <el-option label="进行中" :value="0" />
            <el-option label="已完成" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border stripe @row-click="handleRowClick">
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-timeline v-if="timelineMap[row.id]" style="margin:12px">
              <el-timeline-item v-for="act in timelineMap[row.id]" :key="act.activityId"
                :timestamp="act.startTime" :type="act.endTime ? 'primary' : 'warning'">
                {{ act.activityName }} ({{ act.activityType }}) — {{ act.assignee || '未分配' }}
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="加载中..." />
          </template>
        </el-table-column>
        <el-table-column prop="processDefinitionName" label="流程名称" min-width="140" />
        <el-table-column prop="processDefinitionKey" label="流程Key" width="100" />
        <el-table-column prop="startUserId" label="发起人" width="80" />
        <el-table-column prop="businessKey" label="业务Key" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已完成' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status !== 1" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total>0" v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize" :total="total" :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next" style="margin-top:16px" @change="fetchData" />
    </el-card>

    <el-dialog v-model="detailVisible" title="流程详情" width="700px">
      <el-descriptions v-if="currentInstance" :column="2" border>
        <el-descriptions-item label="流程名称">{{ currentInstance.processDefinitionName }}</el-descriptions-item>
        <el-descriptions-item label="流程Key">{{ currentInstance.processDefinitionKey }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ currentInstance.startUserId }}</el-descriptions-item>
        <el-descriptions-item label="业务Key">{{ currentInstance.businessKey }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentInstance.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentInstance.endTime || '进行中' }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ currentInstance.durationInMillis }}</el-descriptions-item>
        <el-descriptions-item label="删除原因">{{ currentInstance.deleteReason || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>流程变量</el-divider>
      <el-table v-if="currentInstance?.variables?.length" :data="currentInstance.variables" border size="small">
        <el-table-column prop="name" label="变量名" />
        <el-table-column prop="value" label="值" />
      </el-table>
      <el-empty v-else description="暂无变量" />
      <template #footer>
        <el-button type="primary" @click="detailVisible=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInstancePage, getInstanceById, deleteInstance, getInstanceTimeline } from '../../api/workflow'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ processDefinitionKey: '', status: undefined as number | undefined, pageNum: 1, pageSize: 10 })

const timelineMap = ref<Record<string, any[]>>({})
const detailVisible = ref(false)
const currentInstance = ref<any>(null)

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const r = await getInstancePage(query)
    list.value = r.data.records || []
    total.value = r.data.total || 0
  } finally { loading.value = false }
}

async function handleRowClick(row: any) {
  if (!timelineMap.value[row.id]) {
    try {
      const r = await getInstanceTimeline(row.id)
      timelineMap.value[row.id] = r.data || []
    } catch { timelineMap.value[row.id] = [] }
  }
}

async function handleViewDetail(row: any) {
  const r = await getInstanceById(row.id)
  currentInstance.value = r.data
  detailVisible.value = true
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除此流程实例？', '提示')
  await deleteInstance(row.id, '手动删除')
  ElMessage.success('已删除')
  fetchData()
}
</script>
