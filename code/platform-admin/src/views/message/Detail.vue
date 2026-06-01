<template>
  <div class="message-detail">
    <el-card>
      <template #header>
        <div class="detail-header">
          <el-button text @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
        </div>
      </template>

      <div v-if="loading" class="loading-box">
        <el-skeleton :rows="6" animated />
      </div>

      <template v-else-if="message">
        <h2 class="detail-title">{{ message.title }}</h2>
        <div class="detail-meta">
          <span>发送人：{{ message.senderName || '系统' }}</span>
          <span>时间：{{ formatTime(message.createTime) }}</span>
          <el-tag v-if="message.type === 'notice'" size="small" type="primary">通知公告</el-tag>
          <el-tag v-else-if="message.type === 'system'" size="small" type="success">系统消息</el-tag>
          <el-tag v-else-if="message.type === 'interaction'" size="small" type="warning">互动消息</el-tag>
          <span v-else>{{ message.type }}</span>
        </div>
        <el-divider />
        <div class="detail-content" v-html="formatContent(message.content)" />
      </template>

      <div v-else class="empty-box">
        <el-empty description="消息不存在或已被删除" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getSiteMessageById, markSiteMessageRead } from '@/api/message'

const route = useRoute()
const router = useRouter()
const message = ref<any>(null)
const loading = ref(true)

function formatTime(dt: any): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatContent(content: string): string {
  if (!content) return ''
  // 先转义 HTML 特殊字符，防止 XSS
  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
  return escaped.replace(/\n/g, '<br/>')
}

function goBack() {
  router.push('/message/list')
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  try {
    const res = await getSiteMessageById(id) as any
    message.value = res?.data || res
    if (message.value && message.value.readStatus === 0) {
      await markSiteMessageRead(id)
    }
  } catch {
    message.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
}
.detail-title {
  font-size: 20px;
  color: #303133;
  margin: 0 0 12px 0;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}
.detail-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  min-height: 200px;
  padding: 8px 0;
}
.loading-box,
.empty-box {
  padding: 40px 0;
}
</style>
