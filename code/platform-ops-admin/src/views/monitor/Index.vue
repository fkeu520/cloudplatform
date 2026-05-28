<template>
  <div class="monitor-page">
    <h2>系统监控</h2>
    <el-row :gutter="16">
      <el-col :span="6" v-for="(info, name) in services" :key="name">
        <el-card :class="['service-card', info.status === 'UP' ? 'status-up' : 'status-down']">
          <div class="service-name">{{ name }}</div>
          <div class="service-status">
            <el-tag :type="info.status === 'UP' ? 'success' : 'danger'" size="large">
              {{ info.status }}
            </el-tag>
          </div>
          <div class="service-url">{{ info.url }}</div>
          <div v-if="info.error" class="service-error">{{ info.error }}</div>
        </el-card>
      </el-col>
    </el-row>
    <div style="margin-top:16px">
      <el-button type="primary" @click="fetchHealth" :loading="loading">
        <i class="fas fa-sync" style="margin-right:6px" />刷新
      </el-button>
      <span style="margin-left:12px;color:#909399;font-size:13px">上次更新: {{ lastUpdate }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getServiceHealth } from '../../api/monitor'

const loading = ref(false)
const services = ref<Record<string, any>>({})
const lastUpdate = ref('')

async function fetchHealth() {
  loading.value = true
  try {
    const res = await getServiceHealth()
    const data = res.data
    services.value = data?.details || {}
    lastUpdate.value = data?.timestamp ? new Date(data.timestamp).toLocaleString() : new Date().toLocaleString()
  } catch {
    services.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(fetchHealth)
</script>

<style scoped>
.monitor-page { padding: 16px; }
.service-card { margin-bottom: 16px; text-align: center; }
.service-name { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.service-status { margin-bottom: 8px; }
.service-url { font-size: 12px; color: #909399; word-break: break-all; }
.service-error { font-size: 12px; color: #f56c6c; margin-top: 4px; }
.status-up { border-top: 3px solid #67c23a; }
.status-down { border-top: 3px solid #f56c6c; }
</style>
