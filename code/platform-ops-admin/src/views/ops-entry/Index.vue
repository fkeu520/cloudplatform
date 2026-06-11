<template>
  <div class="ops-entry-page">
    <h2>运维管理</h2>
    <p style="color:#909399;margin-bottom:20px">常用运维工具入口</p>

    <el-row :gutter="16">
      <el-col :span="6" v-for="tool in tools" :key="tool.name">
        <el-card class="tool-card" shadow="hover" @click="openTool(tool.url)">
          <div class="tool-icon">
            <i :class="tool.icon" style="font-size:32px" />
          </div>
          <div class="tool-name">{{ tool.name }}</div>
          <div class="tool-desc">{{ tool.desc }}</div>
          <div class="tool-url">{{ formatUrl(tool.url) }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
const host = window.location.hostname || 'localhost'

const tools = [
  { name: 'Grafana', icon: 'fas fa-chart-line', desc: '监控看板 (CPU/内存/容器)', url: `http://${host}:3000` },
  { name: 'Prometheus', icon: 'fas fa-tachometer-alt', desc: '指标采集与告警', url: `http://${host}:9090` },
  { name: 'Kibana', icon: 'fas fa-search', desc: 'ELK 日志检索', url: `http://${host}:5601` },
  { name: 'Nacos', icon: 'fas fa-cloud', desc: '服务注册与配置中心', url: `http://${host}:8848/nacos` },
  { name: 'Zipkin', icon: 'fas fa-project-diagram', desc: '链路追踪', url: `http://${host}:9411` },
  { name: 'MinIO', icon: 'fas fa-hdd', desc: '对象存储管理', url: `http://${host}:9001` },
  { name: 'Kafka-UI', icon: 'fas fa-envelope', desc: '消息队列管理', url: `http://${host}:8089` },
  { name: 'XXL-JOB', icon: 'fas fa-clock', desc: '分布式调度中心', url: `http://${host}:8088/xxl-job-admin` },
]

function formatUrl(url: string) {
  try { return new URL(url).host + new URL(url).pathname } catch { return url }
}

function openTool(url: string) {
  window.open(url, '_blank')
}
</script>

<style scoped>
.ops-entry-page { padding: 16px; }
.tool-card {
  margin-bottom: 16px; text-align: center; cursor: pointer;
  transition: transform .2s, box-shadow .2s;
}
.tool-card:hover { transform: translateY(-4px); }
.tool-icon { margin: 12px 0; color: #409eff; }
.tool-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.tool-desc { font-size: 12px; color: #606266; margin-bottom: 8px; }
.tool-url { font-size: 11px; color: #c0c4cc; word-break: break-all; }
</style>
