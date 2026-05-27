<template>
  <div class="record-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息发送记录</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="filterChannel" placeholder="渠道" clearable size="small" style="width:110px" @change="onFilter">
          <el-option label="站内信" value="site" />
          <el-option label="短信" value="sms" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="发送状态" clearable size="small" style="width:110px;margin-left:8px" @change="onFilter">
          <el-option label="待发送" :value="0" />
          <el-option label="发送中" :value="1" />
          <el-option label="成功" :value="2" />
          <el-option label="失败" :value="3" />
        </el-select>
        <el-date-picker v-model="filterTime" type="datetimerange" range-separator="至" start-placeholder="开始时间"
          end-placeholder="结束时间" size="small" style="margin-left:8px;width:340px" @change="onFilter" />
        <el-input v-model="filterKeyword" placeholder="搜索标题/内容" clearable size="small" style="width:200px;margin-left:8px" @clear="onFilter" @keyup.enter="onFilter" />
        <el-button size="small" style="margin-left:8px" @click="onFilter">查询</el-button>
      </div>

      <el-table :data="recordList" v-loading="loading" border stripe style="margin-top:12px">
        <el-table-column prop="id" label="记录ID" width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="消息标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="渠道" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.channelCode === 'sms'" type="warning" size="small">短信</el-tag>
            <el-tag v-else-if="row.channelCode === 'site'" type="primary" size="small">站内信</el-tag>
            <span v-else>{{ row.channelCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发送状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.sendStatus === 0" type="info" size="small">待发送</el-tag>
            <el-tag v-else-if="row.sendStatus === 1" size="small">发送中</el-tag>
            <el-tag v-else-if="row.sendStatus === 2" type="success" size="small">成功</el-tag>
            <el-tag v-else-if="row.sendStatus === 3" type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiverAddress" label="接收地址" width="140" show-overflow-tooltip />
        <el-table-column prop="receiverName" label="接收人" width="100" show-overflow-tooltip />
        <el-table-column prop="tenantName" label="所属租户" width="120" show-overflow-tooltip />
        <el-table-column prop="businessType" label="业务模块" width="100" show-overflow-tooltip />
        <el-table-column label="发送时间" width="170">
          <template #default="{ row }">{{ formatTime(row.sendTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.sendStatus === 3" type="warning" link @click="handleResend(row)">重发</el-button>
            <el-popconfirm title="确定删除此记录？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        style="margin-top:16px" @change="fetchRecords" />
    </el-card>

    <el-dialog v-model="detailVisible" title="消息详情" width="650px">
      <template v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录ID" :span="2">{{ currentRecord.id }}</el-descriptions-item>
          <el-descriptions-item label="消息标题">{{ currentRecord.title }}</el-descriptions-item>
          <el-descriptions-item label="渠道">
            <el-tag v-if="currentRecord.channelCode === 'sms'" type="warning" size="small">短信</el-tag>
            <el-tag v-else-if="currentRecord.channelCode === 'site'" type="primary" size="small">站内信</el-tag>
            <span v-else>{{ currentRecord.channelCode }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="发送状态">
            <el-tag v-if="currentRecord.sendStatus === 0" type="info" size="small">待发送</el-tag>
            <el-tag v-else-if="currentRecord.sendStatus === 1" size="small">发送中</el-tag>
            <el-tag v-else-if="currentRecord.sendStatus === 2" type="success" size="small">成功</el-tag>
            <el-tag v-else-if="currentRecord.sendStatus === 3" type="danger" size="small">失败</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接收地址">{{ currentRecord.receiverAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收人">{{ currentRecord.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属租户">{{ currentRecord.tenantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务模块">{{ currentRecord.businessType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务ID">{{ currentRecord.businessId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ currentRecord.retryCount }} / {{ currentRecord.maxRetries }}</el-descriptions-item>
          <el-descriptions-item label="发送时间" :span="2">{{ formatTime(currentRecord.sendTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatTime(currentRecord.createTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.errorMsg" label="失败原因" :span="2">
            <span style="color:#f56c6c">{{ currentRecord.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <div style="font-weight:500;margin-bottom:8px">消息内容：</div>
          <div class="content-box" v-html="formatContent(currentRecord.content)" />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecordPage, getRecordById, resendRecord, deleteRecord } from '@/api/message'

const loading = ref(false)
const recordList = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterChannel = ref('')
const filterStatus = ref<number | undefined>(undefined)
const filterTime = ref<any>([])
const filterKeyword = ref('')
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

function formatTime(dt: any): string {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatContent(content: string): string {
  if (!content) return '-'
  return content.replace(/\n/g, '<br/>')
}

async function fetchRecords() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      channelCode: filterChannel.value || undefined,
      sendStatus: filterStatus.value,
      keyword: filterKeyword.value || undefined
    }
    if (filterTime.value && filterTime.value.length === 2) {
      const pad = (n: number) => String(n).padStart(2, '0')
      const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      params.startTime = fmt(filterTime.value[0])
      params.endTime = fmt(filterTime.value[1])
    }
    const res = await getRecordPage(params) as any
    const data = res?.data || res
    recordList.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function onFilter() {
  pageNum.value = 1
  fetchRecords()
}

async function showDetail(row: any) {
  const res = await getRecordById(row.id) as any
  currentRecord.value = res?.data || res
  detailVisible.value = true
}

async function handleResend(row: any) {
  try {
    await resendRecord(row.id)
    ElMessage.success('已提交重发')
    fetchRecords()
  } catch { /* ignore */ }
}

async function handleDelete(row: any) {
  try {
    await deleteRecord(row.id)
    ElMessage.success('删除成功')
    fetchRecords()
  } catch { /* ignore */ }
}

onMounted(() => { fetchRecords() })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.content-box {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  max-height: 200px;
  overflow-y: auto;
}
</style>
