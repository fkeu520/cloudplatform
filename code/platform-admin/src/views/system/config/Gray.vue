<template>
  <div class="page-container">
    <el-card class="header-card">
      <template #header>
        <div class="header-row">
          <div>
            <h3 class="page-title">灰度开关中心</h3>
            <div class="page-subtitle">
              统一展示 platform.* 灰度开关当前值 (Nacos 热生效, 修改见下方说明)
            </div>
          </div>
          <div class="header-actions">
            <el-tag :type="lastQueryTime ? 'success' : 'info'" size="small">
              上次查询: {{ lastQueryTime || '未查询' }}
            </el-tag>
            <el-button :loading="loading" @click="loadData" type="primary" plain>
              <i class="fas fa-sync-alt" /> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>
          灰度开关为只读视图. 修改开关请通过下方 "操作指南" 操作.
        </template>
        <div class="alert-content">
          1. <strong>DB 方式</strong>: 系统管理 → 参数配置 → 改 sys_config 中同名键 (需重启 Bean, 见 PR4 审计接入)
          <br />
          2. <strong>Nacos 方式</strong>: 配置中心 → common.yml → 改 platform.* 段 → 自动推送 (当前 @RefreshScope 支持 Bean 重建)
          <br />
          3. <strong>环境变量方式</strong>: docker-compose.yml 加 PLATFORM_*_ENABLED=true → 重启服务
        </div>
      </el-alert>
    </el-card>

    <el-card class="table-card">
      <el-table
        :data="groupedData"
        v-loading="loading"
        border
        :tree-props="{ children: 'children' }"
        row-key="key"
        default-expand-all
      >
        <el-table-column prop="key" label="配置键" min-width="280" />
        <el-table-column label="当前值" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.children" type="info" size="small">{{ row.children.length }} 项</el-tag>
            <el-switch
              v-else
              :model-value="row.value"
              disabled
              active-color="#67c23a"
              inactive-color="#dcdfe6"
            />
          </template>
        </el-table-column>
        <el-table-column prop="restartRequired" label="重启" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.children" type="info" size="small">-</el-tag>
            <el-tag v-else-if="row.restartRequired" type="danger" size="small">需重启</el-tag>
            <el-tag v-else type="success" size="small">热生效</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="380" />
      </el-table>
    </el-card>

    <el-card class="footer-card">
      <template #header>
        <div class="card-header">
          <i class="fas fa-book" /> 操作指南
        </div>
      </template>
      <ol class="guide-list">
        <li>
          <strong>查当前值</strong>: 点击右上 "刷新" 按钮, 从 platform-user 服务读取 PlatformToggleProperties (Nacos 推送后实时更新)
        </li>
        <li>
          <strong>改开关</strong>:
          <ul>
            <li>简单改值: <code>config/common.yml</code> → 推 Nacos → Bean 自动重建 (需服务已启动一次让 @RefreshScope 生效)</li>
            <li>紧急止血: docker-compose.yml 加 <code>PLATFORM_*_ENABLED=false</code> 环境变量 → 重启服务</li>
            <li>审计留痕: PR4 接入后通过 <code>sys_gray_audit</code> 表 + /gray/audit API 跟踪所有变更</li>
          </ul>
        </li>
        <li>
          <strong>PR 进展</strong>:
          <ul>
            <li>✅ PR1 (63c6678): common.yml 集中托管 platform.*</li>
            <li>✅ PR2 (3b48228): @RefreshScope 热生效改造</li>
            <li>🔵 PR3 (进行中): 本页 (统一视图)</li>
            <li>📋 PR4 (待启动): sys_gray_audit 审计</li>
          </ul>
        </li>
      </ol>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listGraySwitches, type GraySwitchVO } from '@/api/gray'

const loading = ref(false)
const switches = ref<GraySwitchVO[]>([])
const lastQueryTime = ref('')

/** 把扁平列表按 group 字段折叠为树形 (UI 友好) */
const groupedData = computed(() => {
  const map = new Map<string, GraySwitchVO & { children?: GraySwitchVO[] }>()
  for (const sw of switches.value) {
    if (!map.has(sw.group)) {
      map.set(sw.group, {
        key: sw.group,
        group: sw.group,
        value: false,
        description: '',
        restartRequired: false,
        children: []
      })
    }
    map.get(sw.group)!.children!.push(sw)
  }
  return Array.from(map.values())
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await listGraySwitches()
    if (res.code === 200) {
      switches.value = (res.data || []).map((sw: GraySwitchVO) => ({
        ...sw,
        queryTime: new Date().toLocaleString('zh-CN')
      }))
      lastQueryTime.value = new Date().toLocaleString('zh-CN')
    } else {
      ElMessage.error(res.msg || '加载灰度开关失败')
    }
  } catch (e: any) {
    ElMessage.error('请求失败: ' + (e.message || e))
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.page-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.alert-content {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
}

.alert-content code {
  background: #fff7e6;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #d4691a;
}

.table-card {
  margin-bottom: 20px;
}

.footer-card .card-header {
  font-weight: 600;
  font-size: 15px;
}

.guide-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.9;
  color: #606266;
  font-size: 14px;
}

.guide-list code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
  color: #d4691a;
  margin: 0 2px;
}

.guide-list ul {
  margin-top: 6px;
  padding-left: 24px;
}
</style>