<template>
  <div class="page-container">
    <div class="breadcrumb-bar">
      <el-button text size="small" @click="goBack">← 返回</el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>集成设置</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="page-title">集成设置</div>
    <p class="page-desc">管理第三方数据服务商配置，包括天眼查、启信宝、企查查、聚合数据。配置完成后系统自动同步企业数据。</p>

    <!-- 统计卡片 -->
    <el-row :gutter="12" class="stats-row">
      <el-col :span="4" v-for="s in stats" :key="s.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 服务商 Tabs -->
    <el-card shadow="never" class="provider-card">
      <el-tabs v-model="activeProvider">
        <el-tab-pane v-for="p in providers" :key="p.key" :name="p.key">
          <template #label>
            <span class="provider-tab-label">
              <span class="status-dot" :class="p.online ? 'online' : 'offline'"></span>
              {{ p.name }}
            </span>
          </template>

          <div class="provider-header">
            <div class="provider-info">
              <div class="provider-logo" :style="{ background: p.color }">{{ p.logo }}</div>
              <div>
                <div class="provider-name">{{ p.name }}</div>
                <div class="provider-desc">{{ p.desc }}</div>
              </div>
            </div>
            <div>
              <el-tag :type="p.online ? 'success' : 'danger'" size="small">
                {{ p.online ? '已连接' : '未连接' }}
              </el-tag>
            </div>
          </div>

          <div class="config-grid">
            <!-- 基础配置 -->
            <div class="config-section">
              <h4 class="section-title">🔌 基础配置</h4>
              <el-form label-width="100px" size="small">
                <el-form-item label="API Key">
                  <el-input v-model="p.config.apiKey" type="password" show-password placeholder="请输入 API Key" />
                </el-form-item>
                <el-form-item label="Secret Key">
                  <el-input v-model="p.config.secretKey" type="password" show-password placeholder="请输入 Secret Key" />
                </el-form-item>
                <el-form-item label="接口地址">
                  <el-input v-model="p.config.endpoint" placeholder="https://api.example.com" />
                </el-form-item>
                <el-form-item label="超时时间">
                  <el-input-number v-model="p.config.timeout" :min="1" :max="60" controls-position="right" style="width:120px" /> 秒
                </el-form-item>
              </el-form>
            </div>

            <!-- 同步配置 -->
            <div class="config-section">
              <h4 class="section-title">🔄 同步配置</h4>
              <el-form label-width="100px" size="small">
                <el-form-item label="自动同步">
                  <el-switch v-model="p.config.autoSync" />
                </el-form-item>
                <el-form-item label="同步频率">
                  <el-select v-model="p.config.syncInterval" style="width:160px" :disabled="!p.config.autoSync">
                    <el-option label="每 1 小时" :value="1" />
                    <el-option label="每 6 小时" :value="6" />
                    <el-option label="每 12 小时" :value="12" />
                    <el-option label="每天" :value="24" />
                  </el-select>
                </el-form-item>
                <el-form-item label="数据范围">
                  <el-checkbox-group v-model="p.config.dataScope">
                    <el-checkbox label="base" value="base">基本信息</el-checkbox>
                    <el-checkbox label="finance" value="finance">财务数据</el-checkbox>
                    <el-checkbox label="risk" value="risk">风险信息</el-checkbox>
                    <el-checkbox label="patent" value="patent">知识产权</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                <el-form-item label="企业筛选">
                  <el-select v-model="p.config.enterpriseFilter" style="width:160px">
                    <el-option label="全部企业" value="all" />
                    <el-option label="仅已绑定" value="bound" />
                    <el-option label="指定行业" value="industry" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>

            <!-- 用量配额 -->
            <div class="config-section">
              <h4 class="section-title">📊 用量配额</h4>
              <div class="quota-row"><span>日调用量</span><span>{{ p.config.dailyUsed }} / {{ p.config.dailyLimit }}</span></div>
              <el-progress :percentage="Math.round(p.config.dailyUsed / p.config.dailyLimit * 100)" :status="p.config.dailyUsed / p.config.dailyLimit > 0.8 ? 'exception' : undefined" />
              <div class="quota-row" style="margin-top:12px"><span>月调用量</span><span>{{ p.config.monthlyUsed }} / {{ p.config.monthlyLimit }}</span></div>
              <el-progress :percentage="Math.round(p.config.monthlyUsed / p.config.monthlyLimit * 100)" :status="p.config.monthlyUsed / p.config.monthlyLimit > 0.8 ? 'exception' : undefined" />
            </div>

            <!-- 高级配置 -->
            <div class="config-section">
              <h4 class="section-title">⚙️ 高级配置</h4>
              <el-form label-width="100px" size="small">
                <el-form-item label="重试次数">
                  <el-input-number v-model="p.config.retryCount" :min="0" :max="5" controls-position="right" style="width:120px" />
                </el-form-item>
                <el-form-item label="缓存时间">
                  <el-input-number v-model="p.config.cacheTTL" :min="0" :max="72" controls-position="right" style="width:120px" /> 小时
                </el-form-item>
                <el-form-item label="并发上限">
                  <el-input-number v-model="p.config.concurrency" :min="1" :max="20" controls-position="right" style="width:120px" />
                </el-form-item>
                <el-form-item label="日志级别">
                  <el-select v-model="p.config.logLevel" style="width:160px">
                    <el-option label="调试" value="debug" />
                    <el-option label="信息" value="info" />
                    <el-option label="警告" value="warn" />
                    <el-option label="错误" value="error" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <div style="display:flex;gap:8px;margin-top:16px">
            <el-button type="primary" @click="testConnection(p)">测试连接</el-button>
            <el-button @click="saveConfig(p)">保存配置</el-button>
            <el-button type="danger" plain @click="resetConfig(p)">重置</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 同步日志 -->
    <el-card shadow="never" class="log-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <strong>📋 同步日志</strong>
          <el-button size="small" @click="refreshLogs">刷新</el-button>
        </div>
      </template>
      <el-table :data="syncLogs" v-loading="logLoading" size="small" border stripe max-height="300">
        <el-table-column prop="time" label="时间" width="150" />
        <el-table-column prop="provider" label="服务商" width="100" />
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="200" :show-overflow-tooltip="true" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const activeProvider = ref('tianyancha')

const providers = reactive([
  {
    key: 'tianyancha', name: '天眼查', logo: '天', color: '#1890FF', online: true,
    desc: '企业工商信息、司法风险、经营状况等多维度数据',
    config: { apiKey: 'tyc_sk_************', secretKey: '********', endpoint: 'https://api.tianyancha.com', timeout: 30,
      autoSync: true, syncInterval: 6, dataScope: ['base', 'risk'], enterpriseFilter: 'all',
      dailyUsed: 3852, dailyLimit: 5000, monthlyUsed: 85200, monthlyLimit: 150000,
      retryCount: 3, cacheTTL: 12, concurrency: 10, logLevel: 'info' }
  },
  {
    key: 'qixinbao', name: '启信宝', logo: '启', color: '#00B96B', online: true,
    desc: '企业信用信息、司法协助、招投标数据',
    config: { apiKey: 'qxb_sk_************', secretKey: '********', endpoint: 'https://api.qixin.com', timeout: 30,
      autoSync: true, syncInterval: 12, dataScope: ['base', 'finance'], enterpriseFilter: 'all',
      dailyUsed: 2100, dailyLimit: 5000, monthlyUsed: 45000, monthlyLimit: 150000,
      retryCount: 3, cacheTTL: 6, concurrency: 5, logLevel: 'info' }
  },
  {
    key: 'qichacha', name: '企查查', logo: '企', color: '#FF6B35', online: false,
    desc: '企业风险监控、舆情分析、关联关系挖掘',
    config: { apiKey: '', secretKey: '', endpoint: 'https://api.qichacha.com', timeout: 20,
      autoSync: false, syncInterval: 24, dataScope: ['base'], enterpriseFilter: 'bound',
      dailyUsed: 0, dailyLimit: 3000, monthlyUsed: 12000, monthlyLimit: 90000,
      retryCount: 2, cacheTTL: 24, concurrency: 3, logLevel: 'warn' }
  },
  {
    key: 'juhuishuju', name: '聚合数据', logo: '聚', color: '#722ED1', online: true,
    desc: '企业基础信息核验、财务数据、API 聚合服务',
    config: { apiKey: 'jh_sk_************', secretKey: '********', endpoint: 'https://api.juhe.cn', timeout: 15,
      autoSync: false, syncInterval: 24, dataScope: ['base', 'patent'], enterpriseFilter: 'industry',
      dailyUsed: 856, dailyLimit: 3000, monthlyUsed: 18000, monthlyLimit: 90000,
      retryCount: 1, cacheTTL: 48, concurrency: 8, logLevel: 'error' }
  },
])

const stats = [
  { label: '已连接服务商', value: '3', color: '#34C759' },
  { label: '今日同步企业', value: '128', color: '#409EFF' },
  { label: '同步成功率', value: '98.2%', color: '#34C759' },
  { label: '本月调用量', value: '160,200', color: '#E6A23C' },
  { label: 'API 错误数', value: '23', color: '#F56C6C' },
  { label: '待同步企业', value: '45', color: '#909399' },
]

const logLoading = ref(false)
const syncLogs = ref([
  { time: '2026-07-01 14:32:10', provider: '天眼查', action: '批量同步', status: '成功', detail: '同步 15 家企业基础信息，耗时 3.2s' },
  { time: '2026-07-01 13:15:00', provider: '启信宝', action: '增量同步', status: '成功', detail: '同步 8 家企业风险数据，新增预警 2 条' },
  { time: '2026-07-01 11:00:00', provider: '天眼查', action: '全量同步', status: '成功', detail: '全量同步 128 家企业数据，耗时 28.5s' },
  { time: '2026-07-01 09:30:00', provider: '企查查', action: '连接测试', status: '失败', detail: 'API Key 验证失败：401 Unauthorized' },
  { time: '2026-07-01 08:00:00', provider: '聚合数据', action: '定时同步', status: '成功', detail: '同步 5 家企业专利数据，新增 12 条' },
  { time: '2026-06-30 23:00:00', provider: '天眼查', action: '定时同步', status: '成功', detail: '同步 120 家企业数据，耗时 25.1s' },
  { time: '2026-06-30 18:00:00', provider: '启信宝', action: '定时同步', status: '成功', detail: '同步 6 家企业风险数据' },
  { time: '2026-06-30 15:00:00', provider: '企查查', action: '重新连接', status: '成功', detail: 'API Key 更新后连接成功' },
])

function testConnection(p: any) {
  ElMessage.success(`${p.name} 连接测试成功 ✓`)
}

function saveConfig(p: any) {
  ElMessage.success(`${p.name} 配置已保存 ✓`)
}

function resetConfig(p: any) {
  ElMessage.info(`${p.name} 配置已重置`)
}

function refreshLogs() {
  logLoading.value = true
  setTimeout(() => { logLoading.value = false }, 500)
}

function goBack() {
  router.push({ name: 'EnterpriseIndex' })
}
</script>

<style scoped>
.page-container { padding: 16px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }
.page-title { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.page-desc { font-size: 13px; color: #909399; margin-bottom: 16px; }

.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 12px 0; }
.stat-value { font-size: 22px; font-weight: 700; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }

.provider-card { margin-bottom: 16px; }
.provider-tab-label { display: flex; align-items: center; gap: 6px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.status-dot.online { background: #34C759; }
.status-dot.offline { background: #C0C4CC; }

.provider-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
.provider-info { display: flex; align-items: center; gap: 12px; }
.provider-logo { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; color: #fff; }
.provider-name { font-size: 16px; font-weight: 600; }
.provider-desc { font-size: 12px; color: #909399; margin-top: 2px; }

.config-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.config-section { background: #f5f7fa; border-radius: 8px; padding: 16px; }
.config-section .section-title { font-size: 13px; font-weight: 600; margin: 0 0 12px; padding: 0; border: none; }
.config-section :deep(.el-form-item) { margin-bottom: 12px; }
.config-section :deep(.el-form-item__label) { font-size: 12px; }

.quota-row { display: flex; justify-content: space-between; font-size: 12px; color: #606266; margin-bottom: 4px; }

.log-card { margin-bottom: 16px; }
</style>