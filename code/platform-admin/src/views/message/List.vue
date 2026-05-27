<template>
  <div class="message-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <el-button type="primary" size="small" :disabled="unreadCount === 0" @click="handleMarkAllRead">全部已读</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <div class="tabs">
          <el-radio-group v-model="activeType" @change="onTypeChange" size="small">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="notice">通知公告</el-radio-button>
            <el-radio-button value="system">系统消息</el-radio-button>
            <el-radio-button value="interaction">互动消息</el-radio-button>
          </el-radio-group>
        </div>
        <div class="search">
          <el-input v-model="keyword" placeholder="搜索消息" clearable size="small" style="width:220px" @clear="onSearch" @keyup.enter="onSearch">
            <template #suffix>
              <el-icon style="cursor:pointer" @click="onSearch"><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <div v-if="messageList.length === 0 && !loading" class="empty">
        <el-empty description="暂无消息" />
      </div>

      <div v-else class="message-items">
        <div
          v-for="msg in messageList"
          :key="msg.id"
          class="message-item"
          :class="{ unread: msg.readStatus === 0 }"
          @click="goDetail(msg)"
        >
          <div class="msg-left">
            <div class="msg-dot" v-if="msg.readStatus === 0" />
            <div class="msg-icon">
              <el-icon v-if="msg.type === 'notice'" color="#409eff"><Bell /></el-icon>
              <el-icon v-else-if="msg.type === 'system'" color="#67c23a"><Setting /></el-icon>
              <el-icon v-else color="#e6a23c"><ChatLineSquare /></el-icon>
            </div>
          </div>
          <div class="msg-content">
            <div class="msg-title">
              <span>{{ msg.title }}</span>
              <el-tag v-if="msg.type === 'notice'" size="small" type="primary">通知</el-tag>
              <el-tag v-else-if="msg.type === 'system'" size="small" type="success">系统</el-tag>
              <el-tag v-else size="small" type="warning">互动</el-tag>
            </div>
            <div class="msg-desc">{{ msg.content?.slice(0, 100) }}{{ msg.content?.length > 100 ? '...' : '' }}</div>
            <div class="msg-meta">
              <span>{{ msg.senderName || '系统' }}</span>
              <span>{{ formatTime(msg.createTime) }}</span>
            </div>
          </div>
          <div class="msg-action" @click.stop>
            <el-button text type="danger" size="small" @click="handleDelete(msg)">删除</el-button>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px"
        @change="fetchMessages"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Setting, ChatLineSquare, Search } from '@element-plus/icons-vue'
import { getSiteMessagePage, markAllSiteMessageRead, deleteSiteMessage } from '@/api/message'

const router = useRouter()
const username = localStorage.getItem('username') || 'admin'

const messageList = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const activeType = ref('')
const keyword = ref('')
const unreadCount = ref(0)

function formatTime(dt: any): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getSiteMessagePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: activeType.value || undefined,
      keyword: keyword.value || undefined,
      userId: username
    }) as any
    const data = res?.data || res
    messageList.value = data?.records || []
    total.value = data?.total || 0
    const unread = messageList.value.filter((m: any) => m.readStatus === 0)
    unreadCount.value = unread.length
  } finally {
    loading.value = false
  }
}

function onTypeChange() {
  pageNum.value = 1
  fetchMessages()
}

function onSearch() {
  pageNum.value = 1
  fetchMessages()
}

async function goDetail(msg: any) {
  router.push(`/message/detail/${msg.id}`)
}

async function handleDelete(msg: any) {
  await ElMessageBox.confirm('确定要删除这条消息吗？', '提示', { type: 'warning' })
  try {
    await deleteSiteMessage(msg.id)
    ElMessage.success('删除成功')
    fetchMessages()
  } catch { /* cancelled */ }
}

async function handleMarkAllRead() {
  try {
    await markAllSiteMessageRead(username)
    ElMessage.success('已全部标记为已读')
    fetchMessages()
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchMessages()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.empty {
  padding: 40px 0;
}
.message-items {
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.message-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}
.message-item:last-child {
  border-bottom: none;
}
.message-item:hover {
  background: #f5f7fa;
}
.message-item.unread {
  background: #f0f7ff;
}
.message-item.unread:hover {
  background: #e6f0fc;
}
.msg-left {
  display: flex;
  align-items: center;
  margin-right: 12px;
}
.msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  margin-right: 8px;
  flex-shrink: 0;
}
.msg-icon {
  font-size: 20px;
}
.msg-content {
  flex: 1;
  min-width: 0;
}
.msg-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.msg-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-meta {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
  display: flex;
  gap: 16px;
}
.msg-action {
  flex-shrink: 0;
  margin-left: 8px;
}
</style>
