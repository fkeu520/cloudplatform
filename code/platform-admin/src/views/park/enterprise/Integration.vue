<template>
  <div class="page-container">
    <!-- ═══ Breadcrumb ═══ -->
    <div class="breadcrumb-bar">
      <el-button text size="small" @click="goBack">← 返回</el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>集成设置</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- ═══ Page Header ═══ -->
    <div class="page-header">
      <div>
        <div class="page-title">集成设置</div>
        <p class="page-desc">配置第三方企业数据服务商，对接企业信息查询、工商变更、风险监控等数据同步</p>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="triggerFullSync">🔄 触发全量同步</el-button>
        <el-button size="small" @click="exportLogs">📄 导出日志</el-button>
      </div>
    </div>

    <!-- ═══ Usage Statistics ═══ -->
    <el-card shadow="never" class="stats-card">
      <div class="stats-grid">
        <div class="stat-item" v-for="s in currentStats" :key="s.label">
          <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-sub" :class="s.trendClass || ''">{{ s.sub || '' }}</div>
        </div>
      </div>
    </el-card>

    <!-- ═══ Provider Tabs ═══ -->
    <el-card shadow="never" class="provider-card">
      <el-tabs v-model="activeProvider" @tab-change="onTabChange">
        <el-tab-pane v-for="p in providers" :key="p.key" :name="p.key" :label="p.name">
          <!-- Provider Header -->
          <div class="provider-header">
            <div class="provider-info">
              <div class="provider-logo" :style="{ background: p.color }">{{ p.logo }}</div>
              <div>
                <div class="provider-name">{{ p.name }}</div>
                <div class="provider-desc">{{ p.config.online ? '已连接 · 剩余 ' + p.quota.remaining + ' 次' : '未连接 · 需配置密钥' }}</div>
              </div>
            </div>
            <el-tag :type="p.config.enabled ? 'success' : 'warning'" size="small" effect="plain" round>
              {{ p.config.enabled ? '● 已启用' : '● 未启用' }}
            </el-tag>
          </div>

          <div class="config-grid">
            <!-- ─── Block 1: 基础配置 ─── -->
            <div class="config-section">
              <h4 class="section-title"><span>🔧</span> 基础配置</h4>
              <el-form label-width="90px" size="small">
                <el-form-item label="API 地址">
                  <el-input v-model="p.config.apiUrl" placeholder="https://api.example.com" :disabled="p.key === 'juhuishuju' ? false : true" />
                </el-form-item>
                <el-form-item label="App Key">
                  <el-input v-model="p.config.appKey" type="password" show-password :placeholder="p.config.appKey ? '' : '请输入 AppKey'" />
                </el-form-item>
                <el-form-item label="Secret Key">
                  <el-input v-model="p.config.secretKey" type="password" show-password :placeholder="p.config.secretKey ? '' : '请输入 SecretKey'" />
                </el-form-item>
                <el-form-item label="企业名称">
                  <el-input v-model="p.config.companyName" placeholder="云枢科技" disabled />
                </el-form-item>
                <el-form-item label="回调地址">
                  <el-input v-model="p.config.callbackUrl" />
                </el-form-item>
                <el-form-item label="启用状态">
                  <el-switch v-model="p.config.enabled" />
                </el-form-item>
              </el-form>
              <el-button type="primary" size="small" class="test-btn" @click="testConnection(p)">🔗 连接测试</el-button>
            </div>

            <!-- ─── Block 2: 同步配置 ─── -->
            <div class="config-section">
              <h4 class="section-title"><span>🔄</span> 同步配置</h4>
              <el-form label-width="90px" size="small">
                <el-form-item label="同步频率">
                  <el-select v-model="p.sync.frequency" style="width:100%">
                    <el-option label="实时" value="realtime" />
                    <el-option label="每5分钟" value="5min" />
                    <el-option label="每15分钟" value="15min" />
                    <el-option label="每小时" value="hourly" />
                    <el-option label="每天" value="daily" />
                    <el-option label="每周" value="weekly" />
                    <el-option label="每月" value="monthly" />
                  </el-select>
                </el-form-item>
                <el-form-item label="同步范围">
                  <el-checkbox-group v-model="p.sync.scopes">
                    <el-checkbox value="entInfo" label="企业信息" />
                    <el-checkbox value="bizInfo" label="工商信息" />
                    <el-checkbox value="risk" label="司法风险" />
                    <el-checkbox value="ip" label="知识产权" />
                    <el-checkbox value="news" label="新闻舆情" />
                  </el-checkbox-group>
                </el-form-item>
                <el-form-item label="数据模式">
                  <el-radio-group v-model="p.sync.dataMode">
                    <el-radio value="incremental">增量更新</el-radio>
                    <el-radio value="full">全量覆盖</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="自动同步">
                  <el-switch v-model="p.sync.autoSync" />
                </el-form-item>
                <el-form-item label="上次同步">
                  <el-input :model-value="p.sync.lastSyncTime" disabled />
                </el-form-item>
                <el-form-item label="同步状态">
                  <el-input :model-value="p.sync.lastSyncStatus" disabled :style="{ color: p.sync.lastSyncStatus.includes('成功') ? '#34C759' : p.sync.lastSyncStatus.includes('失败') ? '#FF3B30' : '#909399' }" />
                </el-form-item>
              </el-form>
            </div>

            <!-- ─── Block 3: 配额配置 ─── -->
            <div class="config-section">
              <h4 class="section-title"><span>📊</span> 配额配置</h4>
              <el-form label-width="90px" size="small">
                <el-form-item label="每日上限">
                  <el-input-number v-model="p.quota.dailyLimit" :min="100" :max="100000" controls-position="right" style="width:140px" />
                </el-form-item>
                <el-form-item label="已用/日">
                  <div class="progress-wrap">
                    <div class="progress-info">
                      <span>{{ formatNum(p.quota.dailyUsed) }} / {{ formatNum(p.quota.dailyLimit) }}</span>
                      <span :style="{ color: quotaPercent(p.quota.dailyUsed, p.quota.dailyLimit) >= 80 ? '#FF3B30' : quotaPercent(p.quota.dailyUsed, p.quota.dailyLimit) >= 60 ? '#FF9500' : '#34C759' }">
                        {{ quotaPercent(p.quota.dailyUsed, p.quota.dailyLimit) }}%
                      </span>
                    </div>
                    <el-progress :percentage="quotaPercent(p.quota.dailyUsed, p.quota.dailyLimit)" :stroke-width="8"
                      :status="quotaPercent(p.quota.dailyUsed, p.quota.dailyLimit) >= 80 ? 'exception' : quotaPercent(p.quota.dailyUsed, p.quota.dailyLimit) >= 60 ? 'warning' : 'success'"
                      striped striped-flow />
                  </div>
                </el-form-item>
                <el-form-item label="月度上限">
                  <el-input-number v-model="p.quota.monthlyLimit" :min="1000" :max="1000000" controls-position="right" style="width:140px" />
                </el-form-item>
                <el-form-item label="已用/月">
                  <div class="progress-wrap">
                    <div class="progress-info">
                      <span>{{ formatNum(p.quota.monthlyUsed) }} / {{ formatNum(p.quota.monthlyLimit) }}</span>
                      <span :style="{ color: quotaPercent(p.quota.monthlyUsed, p.quota.monthlyLimit) >= 80 ? '#FF3B30' : quotaPercent(p.quota.monthlyUsed, p.quota.monthlyLimit) >= 60 ? '#FF9500' : '#34C759' }">
                        {{ quotaPercent(p.quota.monthlyUsed, p.quota.monthlyLimit) }}%
                      </span>
                    </div>
                    <el-progress :percentage="quotaPercent(p.quota.monthlyUsed, p.quota.monthlyLimit)" :stroke-width="8"
                      :status="quotaPercent(p.quota.monthlyUsed, p.quota.monthlyLimit) >= 80 ? 'exception' : quotaPercent(p.quota.monthlyUsed, p.quota.monthlyLimit) >= 60 ? 'warning' : 'success'"
                      striped striped-flow />
                  </div>
                </el-form-item>
                <el-form-item label="调用频率">
                  <el-input-number v-model="p.quota.apiRate" :min="1" :max="100" controls-position="right" style="width:140px" />
                  <span class="field-hint">次/秒</span>
                </el-form-item>
                <el-form-item label="剩余次数">
                  <el-input :model-value="formatNum(p.quota.remaining) + ' 次'" disabled />
                </el-form-item>
                <el-form-item label="预警阈值">
                  <el-input-number v-model="p.quota.warningThreshold" :min="50" :max="100" controls-position="right" style="width:140px" />
                  <span class="field-hint">%</span>
                </el-form-item>
                <el-form-item label="超出策略">
                  <el-select v-model="p.quota.exceedPolicy" style="width:100%">
                    <el-option label="拒绝请求" value="deny" />
                    <el-option label="排队处理" value="queue" />
                    <el-option label="降级处理" value="degrade" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>

            <!-- ─── Block 4: 高级配置 ─── -->
            <div class="config-section">
              <h4 class="section-title"><span>⚙️</span> 高级配置</h4>
              <el-form label-width="90px" size="small">
                <el-form-item label="超时时间">
                  <el-input-number v-model="p.advanced.timeout" :min="3" :max="120" controls-position="right" style="width:140px" />
                  <span class="field-hint">秒</span>
                </el-form-item>
                <el-form-item label="重试次数">
                  <el-input-number v-model="p.advanced.retryCount" :min="0" :max="10" controls-position="right" style="width:140px" />
                </el-form-item>
                <el-form-item label="重试间隔">
                  <el-input-number v-model="p.advanced.retryInterval" :min="1" :max="60" controls-position="right" style="width:140px" />
                  <span class="field-hint">秒</span>
                </el-form-item>
                <el-form-item label="缓存策略">
                  <el-radio-group v-model="p.advanced.cacheStrategy">
                    <el-radio value="memory">内存</el-radio>
                    <el-radio value="redis">Redis</el-radio>
                    <el-radio value="none">不缓存</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="缓存时间">
                  <el-input-number v-model="p.advanced.cacheTTL" :min="1" :max="1440" controls-position="right" style="width:140px" />
                  <span class="field-hint">分钟</span>
                </el-form-item>
                <el-form-item label="数据加密">
                  <el-switch v-model="p.advanced.encryption" />
                </el-form-item>
                <el-form-item label="签名算法">
                  <el-select v-model="p.advanced.signatureAlgo" style="width:100%">
                    <el-option label="HMAC-SHA256" value="HMAC-SHA256" />
                    <el-option label="RSA-SHA256" value="RSA-SHA256" />
                    <el-option label="SM3" value="SM3" />
                  </el-select>
                </el-form-item>
                <el-form-item label="代理地址">
                  <el-input v-model="p.advanced.proxy" placeholder="可选，如 http://proxy:8080" />
                </el-form-item>
                <el-form-item label="日志级别">
                  <el-select v-model="p.advanced.logLevel" style="width:100%">
                    <el-option label="DEBUG" value="DEBUG" />
                    <el-option label="INFO" value="INFO" />
                    <el-option label="WARN" value="WARN" />
                    <el-option label="ERROR" value="ERROR" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <!-- Action Bar -->
          <div class="action-bar">
            <el-button type="danger" plain @click="resetConfig(p)">重置</el-button>
            <el-button type="primary" @click="saveConfig(p)">💾 保存配置</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ═══ 7-Day Trend Chart (per provider) ═══ -->
    <el-card shadow="never" class="trend-card">
      <template #header>
        <div class="trend-header">
          <strong>📈 近 7 日调用趋势 · {{ currentProvider?.name || '-' }}</strong>
          <el-tag size="small" type="info" effect="plain">单位：次</el-tag>
        </div>
      </template>
      <div class="chart-wrap">
        <div class="chart-row" v-for="(item, idx) in currentTrend" :key="idx">
          <div class="chart-label">{{ item.date }}</div>
          <div class="chart-track">
            <div class="chart-bar" :style="{ width: item.pct + '%', background: currentProvider?.color || '#409EFF' }"></div>
          </div>
          <div class="chart-value">{{ formatNum(item.count) }}</div>
        </div>
      </div>
    </el-card>

    <!-- ═══ Sync Logs ═══ -->
    <el-card shadow="never" class="log-card">
      <template #header>
        <div class="log-header">
          <strong>📋 同步日志</strong>
          <div class="log-filters">
            <el-select v-model="logProviderFilter" placeholder="全部服务商" size="small" style="width:130px" @change="refreshLogs">
              <el-option label="全部服务商" value="" />
              <el-option v-for="p in providers" :key="p.key" :label="p.name" :value="p.name" />
            </el-select>
            <el-select v-model="logStatusFilter" placeholder="全部状态" size="small" style="width:110px" @change="refreshLogs">
              <el-option label="全部状态" value="" />
              <el-option label="成功" value="成功" />
              <el-option label="失败" value="失败" />
              <el-option label="进行中" value="进行中" />
            </el-select>
            <el-button size="small" @click="refreshLogs">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredLogs" v-loading="logLoading" size="small" stripe max-height="360">
        <el-table-column prop="time" label="时间" width="160" />
        <el-table-column prop="provider" label="数据源" width="90" />
        <el-table-column prop="type" label="同步类型" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === '成功' ? 'success' : scope.row.status === '失败' ? 'danger' : 'primary'" size="small" effect="plain" round>
              {{ scope.row.status === '成功' ? '✅ 成功' : scope.row.status === '失败' ? '❌ 失败' : '🔄 进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="records" label="记录数" width="80" align="right" />
        <el-table-column prop="duration" label="耗时" width="70" align="right" />
        <el-table-column label="详情" width="60">
          <template #default="scope">
            <el-link type="primary" :underline="false" size="small" @click="viewLogDetail(scope.row)">查看</el-link>
          </template>
        </el-table-column>
      </el-table>
      <div class="log-footer">
        <span>共 {{ filteredLogs.length }} 条记录</span>
        <!-- Pagination mock -->
      </div>
    </el-card>

    <!-- ═══ Test Connection Dialog ═══ -->
    <el-dialog v-model="testDialog.visible" :title="testDialog.title" width="480px" destroy-on-close>
      <div v-if="testDialog.loading" class="test-loading">
        <el-progress :percentage="100" :stroke-width="6" status="success" :indeterminate="true" :duration="2" />
        <p>正在测试连接...</p>
      </div>
      <div v-else class="test-result">
        <div class="test-status" :class="testDialog.success ? 'success' : 'fail'">
          <span class="test-icon">{{ testDialog.success ? '✓' : '✗' }}</span>
          <span>{{ testDialog.success ? '连接测试：成功' : '连接测试：失败' }}</span>
        </div>
        <el-descriptions :column="2" border size="small" class="test-details">
          <el-descriptions-item label="响应时间">{{ testDialog.data.responseTime }}</el-descriptions-item>
          <el-descriptions-item label="API 版本">{{ testDialog.data.apiVersion }}</el-descriptions-item>
          <el-descriptions-item label="HTTP 状态码">{{ testDialog.data.httpStatus }}</el-descriptions-item>
          <el-descriptions-item label="服务状态">{{ testDialog.data.serviceStatus }}</el-descriptions-item>
          <el-descriptions-item label="剩余配额">{{ testDialog.data.remainingQuota }}</el-descriptions-item>
          <el-descriptions-item label="响应时间戳">{{ testDialog.data.timestamp }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="testDialog.data.raw" class="test-raw">
          <div class="raw-label">原始响应：</div>
          <pre>{{ testDialog.data.raw }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="testDialog.visible = false">关闭</el-button>
        <el-button v-if="testDialog.success" type="primary" @click="testDialog.visible = false">确认</el-button>
      </template>
    </el-dialog>

    <!-- ═══ Log Detail Dialog ═══ -->
    <el-dialog v-model="logDetail.visible" title="同步详情" width="500px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="批次号">{{ logDetail.data.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="数据源">{{ logDetail.data.provider }}</el-descriptions-item>
        <el-descriptions-item label="同步类型">{{ logDetail.data.type }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="logDetail.data.status === '成功' ? 'success' : logDetail.data.status === '失败' ? 'danger' : 'primary'" size="small">
            {{ logDetail.data.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ logDetail.data.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ logDetail.data.endTime }}</el-descriptions-item>
        <el-descriptions-item label="同步范围">{{ logDetail.data.scope }}</el-descriptions-item>
        <el-descriptions-item label="记录数">{{ logDetail.data.records }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ logDetail.data.duration }}</el-descriptions-item>
        <el-descriptions-item label="异常数">{{ logDetail.data.errors }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="logDetail.data.errorMsg" class="log-error">
        <el-alert :title="logDetail.data.errorMsg" type="error" show-icon :closable="false" />
      </div>
      <div v-if="logDetail.data.suggestion" class="log-suggestion">
        <el-alert :title="logDetail.data.suggestion" type="warning" show-icon :closable="false" />
      </div>
      <template #footer>
        <el-button @click="logDetail.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ═══ Reset Confirm Dialog ═══ -->
    <el-dialog v-model="resetDialog.visible" title="确认重置" width="360px">
      <p>确定要将 <strong>{{ resetDialog.providerName }}</strong> 的配置重置为默认值吗？</p>
      <p style="font-size:12px;color:#909399;margin-top:8px">此操作将恢复所有配置项到初始状态。</p>
      <template #footer>
        <el-button @click="resetDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="confirmReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// ─── Provider Interface ───
interface ProviderConfig {
  apiUrl: string
  appKey: string
  secretKey: string
  companyName: string
  callbackUrl: string
  enabled: boolean
  online: boolean
}

interface SyncConfig {
  frequency: string
  scopes: string[]
  dataMode: string
  autoSync: boolean
  lastSyncTime: string
  lastSyncStatus: string
}

interface QuotaConfig {
  dailyLimit: number
  dailyUsed: number
  monthlyLimit: number
  monthlyUsed: number
  apiRate: number
  remaining: number
  warningThreshold: number
  exceedPolicy: string
}

interface AdvancedConfig {
  timeout: number
  retryCount: number
  retryInterval: number
  cacheStrategy: string
  cacheTTL: number
  encryption: boolean
  signatureAlgo: string
  proxy: string
  logLevel: string
}

interface TrendItem {
  date: string
  count: number
  pct: number
}

interface Provider {
  key: string
  name: string
  logo: string
  color: string
  desc: string
  defaults: ProviderConfig & SyncConfig & QuotaConfig & AdvancedConfig
  config: ProviderConfig
  sync: SyncConfig
  quota: QuotaConfig
  advanced: AdvancedConfig
  trend: TrendItem[]
  stats: { label: string; value: string; color: string; sub: string; trendClass?: string }[]
}

// ─── Active Provider ───
const activeProvider = ref('tianyancha')

// ─── Provider Data ───
const providers = reactive<Provider[]>([
  {
    key: 'tianyancha', name: '天眼查', logo: '天', color: '#1890FF',
    desc: '企业工商信息、司法风险、经营状况等多维度数据',
    defaults: {
      apiUrl: 'https://api.tianyancha.com/services/v3', appKey: 'tyc_app_key_2024_demo', secretKey: 'tyc_secret_key_2024_demo_protected', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/tianyancha/callback', enabled: true, online: true,
      frequency: '15min', scopes: ['entInfo', 'bizInfo', 'risk', 'ip'], dataMode: 'incremental', autoSync: true,
      lastSyncTime: '2026-07-02 10:23:45', lastSyncStatus: '✅ 成功 (同步 1,234 条)',
      dailyLimit: 10000, dailyUsed: 1238, monthlyLimit: 300000, monthlyUsed: 45678, apiRate: 10, remaining: 254322,
      warningThreshold: 80, exceedPolicy: 'degrade',
      timeout: 30, retryCount: 3, retryInterval: 5, cacheStrategy: 'memory', cacheTTL: 30, encryption: true,
      signatureAlgo: 'HMAC-SHA256', proxy: '', logLevel: 'INFO'
    },
    config: {
      apiUrl: 'https://api.tianyancha.com/services/v3', appKey: 'tyc_app_key_2024_demo', secretKey: 'tyc_secret_key_2024_demo_protected', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/tianyancha/callback', enabled: true, online: true
    },
    sync: {
      frequency: '15min', scopes: ['entInfo', 'bizInfo', 'risk', 'ip'], dataMode: 'incremental', autoSync: true,
      lastSyncTime: '2026-07-02 10:23:45', lastSyncStatus: '✅ 成功 (同步 1,234 条)'
    },
    quota: { dailyLimit: 10000, dailyUsed: 1238, monthlyLimit: 300000, monthlyUsed: 45678, apiRate: 10, remaining: 254322, warningThreshold: 80, exceedPolicy: 'degrade' },
    advanced: { timeout: 30, retryCount: 3, retryInterval: 5, cacheStrategy: 'memory', cacheTTL: 30, encryption: true, signatureAlgo: 'HMAC-SHA256', proxy: '', logLevel: 'INFO' },
    trend: [
      { date: '06-26', count: 3245, pct: 65 }, { date: '06-27', count: 2876, pct: 58 },
      { date: '06-28', count: 3612, pct: 72 }, { date: '06-29', count: 2234, pct: 45 },
      { date: '06-30', count: 4098, pct: 82 }, { date: '07-01', count: 4523, pct: 90 },
      { date: '07-02', count: 1234, pct: 40 }
    ],
    stats: [
      { label: '今日调用', value: '1,234', color: '#409EFF', sub: '↑ 12.3%', trendClass: 'trend-up' },
      { label: '本月调用', value: '45,678', color: '#34C759', sub: '↑ 8.7%', trendClass: 'trend-up' },
      { label: '成功次数', value: '44,890', color: '#34C759', sub: '98.3%' },
      { label: '失败次数', value: '788', color: '#FF3B30', sub: '1.7%', trendClass: 'trend-down' },
      { label: '平均响应', value: '236ms', color: '#FF9500', sub: '↑ 5ms', trendClass: 'trend-up' },
      { label: '可用率', value: '99.2%', color: '#34C759', sub: '正常' },
    ]
  },
  {
    key: 'qixinbao', name: '启信宝', logo: '启', color: '#00B96B',
    desc: '企业信用信息、司法协助、招投标数据',
    defaults: {
      apiUrl: 'https://api.qixinbao.com/open/v2', appKey: 'qxb_app_key_2024_demo', secretKey: 'qxb_secret_key_2024_demo_protected', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/qixinbao/callback', enabled: true, online: true,
      frequency: '5min', scopes: ['entInfo', 'bizInfo', 'ip'], dataMode: 'incremental', autoSync: true,
      lastSyncTime: '2026-07-02 09:52:18', lastSyncStatus: '✅ 成功 (同步 876 条)',
      dailyLimit: 5000, dailyUsed: 3450, monthlyLimit: 150000, monthlyUsed: 98234, apiRate: 5, remaining: 51766,
      warningThreshold: 80, exceedPolicy: 'degrade',
      timeout: 15, retryCount: 2, retryInterval: 3, cacheStrategy: 'memory', cacheTTL: 15, encryption: false,
      signatureAlgo: 'HMAC-SHA256', proxy: '', logLevel: 'INFO'
    },
    config: {
      apiUrl: 'https://api.qixinbao.com/open/v2', appKey: 'qxb_app_key_2024_demo', secretKey: 'qxb_secret_key_2024_demo_protected', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/qixinbao/callback', enabled: true, online: true
    },
    sync: {
      frequency: '5min', scopes: ['entInfo', 'bizInfo', 'ip'], dataMode: 'incremental', autoSync: true,
      lastSyncTime: '2026-07-02 09:52:18', lastSyncStatus: '✅ 成功 (同步 876 条)'
    },
    quota: { dailyLimit: 5000, dailyUsed: 3450, monthlyLimit: 150000, monthlyUsed: 98234, apiRate: 5, remaining: 51766, warningThreshold: 80, exceedPolicy: 'degrade' },
    advanced: { timeout: 15, retryCount: 2, retryInterval: 3, cacheStrategy: 'memory', cacheTTL: 15, encryption: false, signatureAlgo: 'HMAC-SHA256', proxy: '', logLevel: 'INFO' },
    trend: [
      { date: '06-26', count: 1880, pct: 55 }, { date: '06-27', count: 2120, pct: 62 },
      { date: '06-28', count: 1650, pct: 48 }, { date: '06-29', count: 2480, pct: 73 },
      { date: '06-30', count: 3120, pct: 82 }, { date: '07-01', count: 2870, pct: 76 },
      { date: '07-02', count: 1340, pct: 39 }
    ],
    stats: [
      { label: '今日调用', value: '1,340', color: '#409EFF', sub: '↑ 5.2%', trendClass: 'trend-up' },
      { label: '本月调用', value: '38,560', color: '#34C759', sub: '↑ 3.1%', trendClass: 'trend-up' },
      { label: '成功次数', value: '37,871', color: '#34C759', sub: '98.2%' },
      { label: '失败次数', value: '689', color: '#FF3B30', sub: '1.8%', trendClass: 'trend-down' },
      { label: '平均响应', value: '312ms', color: '#FF9500', sub: '↑ 12ms', trendClass: 'trend-up' },
      { label: '可用率', value: '98.5%', color: '#34C759', sub: '正常' },
    ]
  },
  {
    key: 'qichacha', name: '企查查', logo: '企', color: '#FA541C',
    desc: '企业风险监控、舆情分析、关联关系挖掘',
    defaults: {
      apiUrl: 'https://openapi.qichacha.com/data/v4', appKey: 'qcc_app_key_2024_demo', secretKey: 'qcc_secret_key_2024_demo_protected', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/qichacha/callback', enabled: true, online: true,
      frequency: 'hourly', scopes: ['entInfo', 'bizInfo', 'risk', 'news'], dataMode: 'incremental', autoSync: true,
      lastSyncTime: '2026-07-01 23:00:00', lastSyncStatus: '✅ 成功 (同步 2,156 条)',
      dailyLimit: 3000, dailyUsed: 2890, monthlyLimit: 90000, monthlyUsed: 78432, apiRate: 3, remaining: 11568,
      warningThreshold: 75, exceedPolicy: 'deny',
      timeout: 20, retryCount: 3, retryInterval: 5, cacheStrategy: 'redis', cacheTTL: 60, encryption: true,
      signatureAlgo: 'RSA-SHA256', proxy: '', logLevel: 'WARN'
    },
    config: {
      apiUrl: 'https://openapi.qichacha.com/data/v4', appKey: 'qcc_app_key_2024_demo', secretKey: 'qcc_secret_key_2024_demo_protected', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/qichacha/callback', enabled: true, online: true
    },
    sync: {
      frequency: 'hourly', scopes: ['entInfo', 'bizInfo', 'risk', 'news'], dataMode: 'incremental', autoSync: true,
      lastSyncTime: '2026-07-01 23:00:00', lastSyncStatus: '✅ 成功 (同步 2,156 条)'
    },
    quota: { dailyLimit: 3000, dailyUsed: 2890, monthlyLimit: 90000, monthlyUsed: 78432, apiRate: 3, remaining: 11568, warningThreshold: 75, exceedPolicy: 'deny' },
    advanced: { timeout: 20, retryCount: 3, retryInterval: 5, cacheStrategy: 'redis', cacheTTL: 60, encryption: true, signatureAlgo: 'RSA-SHA256', proxy: '', logLevel: 'WARN' },
    trend: [
      { date: '06-26', count: 2100, pct: 70 }, { date: '06-27', count: 1850, pct: 62 },
      { date: '06-28', count: 2420, pct: 81 }, { date: '06-29', count: 1980, pct: 66 },
      { date: '06-30', count: 2760, pct: 88 }, { date: '07-01', count: 2156, pct: 72 },
      { date: '07-02', count: 980, pct: 33 }
    ],
    stats: [
      { label: '今日调用', value: '980', color: '#409EFF', sub: '↓ 3.5%', trendClass: 'trend-down' },
      { label: '本月调用', value: '29,460', color: '#34C759', sub: '↑ 1.2%', trendClass: 'trend-up' },
      { label: '成功次数', value: '28,571', color: '#34C759', sub: '97.0%' },
      { label: '失败次数', value: '889', color: '#FF3B30', sub: '3.0%', trendClass: 'trend-down' },
      { label: '平均响应', value: '198ms', color: '#FF9500', sub: '↓ 3ms', trendClass: 'trend-down' },
      { label: '可用率', value: '97.8%', color: '#E6A23C', sub: '偏低' },
    ]
  },
  {
    key: 'juhuishuju', name: '聚合数据', logo: '聚', color: '#722ED1',
    desc: '企业基础信息核验、财务数据、API 聚合服务',
    defaults: {
      apiUrl: 'https://v.juhe.cn/enterprise', appKey: '', secretKey: '', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/juhe/callback', enabled: false, online: false,
      frequency: 'daily', scopes: ['entInfo'], dataMode: 'incremental', autoSync: false,
      lastSyncTime: '-', lastSyncStatus: '⏸ 未配置',
      dailyLimit: 1000, dailyUsed: 0, monthlyLimit: 30000, monthlyUsed: 0, apiRate: 2, remaining: 30000,
      warningThreshold: 80, exceedPolicy: 'deny',
      timeout: 10, retryCount: 1, retryInterval: 2, cacheStrategy: 'memory', cacheTTL: 60, encryption: false,
      signatureAlgo: 'HMAC-SHA256', proxy: '', logLevel: 'INFO'
    },
    config: {
      apiUrl: 'https://v.juhe.cn/enterprise', appKey: '', secretKey: '', companyName: '云枢科技',
      callbackUrl: 'https://platform.example.com/api/integration/juhe/callback', enabled: false, online: false
    },
    sync: {
      frequency: 'daily', scopes: ['entInfo'], dataMode: 'incremental', autoSync: false,
      lastSyncTime: '-', lastSyncStatus: '⏸ 未配置'
    },
    quota: { dailyLimit: 1000, dailyUsed: 0, monthlyLimit: 30000, monthlyUsed: 0, apiRate: 2, remaining: 30000, warningThreshold: 80, exceedPolicy: 'deny' },
    advanced: { timeout: 10, retryCount: 1, retryInterval: 2, cacheStrategy: 'memory', cacheTTL: 60, encryption: false, signatureAlgo: 'HMAC-SHA256', proxy: '', logLevel: 'INFO' },
    trend: [
      { date: '06-26', count: 0, pct: 0 }, { date: '06-27', count: 0, pct: 0 },
      { date: '06-28', count: 0, pct: 0 }, { date: '06-29', count: 0, pct: 0 },
      { date: '06-30', count: 0, pct: 0 }, { date: '07-01', count: 0, pct: 0 },
      { date: '07-02', count: 0, pct: 0 }
    ],
    stats: [
      { label: '今日调用', value: '0', color: '#909399', sub: '-' },
      { label: '本月调用', value: '0', color: '#909399', sub: '-' },
      { label: '成功次数', value: '0', color: '#909399', sub: '-' },
      { label: '失败次数', value: '0', color: '#909399', sub: '-' },
      { label: '平均响应', value: '-', color: '#909399', sub: '-' },
      { label: '可用率', value: '-', color: '#909399', sub: '未连接' },
    ]
  }
])

// ─── Computed: current provider & stats ───
const currentProvider = computed(() => providers.find(p => p.key === activeProvider.value))
const currentStats = computed(() => currentProvider.value?.stats || [])
const currentTrend = computed<TrendItem[]>(() => currentProvider.value?.trend || [])

// ─── Global Stats (not per-provider) ───
// Using per-provider stats now

// ─── Sync Logs ───
const logLoading = ref(false)
const logProviderFilter = ref('')
const logStatusFilter = ref('')

const allSyncLogs = [
  { time: '2026-07-02 10:23:45', provider: '天眼查', type: '增量同步', status: '成功', records: '1,234', duration: '12.3s' },
  { time: '2026-07-02 09:52:18', provider: '启信宝', type: '全量同步', status: '成功', records: '876', duration: '8.1s' },
  { time: '2026-07-02 08:30:00', provider: '天眼查', type: '增量同步', status: '成功', records: '567', duration: '6.5s' },
  { time: '2026-07-02 07:15:22', provider: '企查查', type: '增量同步', status: '失败', records: '0', duration: '30.0s' },
  { time: '2026-07-01 23:00:00', provider: '企查查', type: '全量同步', status: '成功', records: '2,156', duration: '45.2s' },
  { time: '2026-07-01 22:00:00', provider: '天眼查', type: '增量同步', status: '成功', records: '432', duration: '5.8s' },
  { time: '2026-07-01 21:00:00', provider: '启信宝', type: '增量同步', status: '进行中', records: '-', duration: '-' },
  { time: '2026-07-01 18:30:00', provider: '聚合数据', type: '增量同步', status: '失败', records: '0', duration: '5.0s' },
  { time: '2026-07-01 15:00:00', provider: '天眼查', type: '增量同步', status: '成功', records: '890', duration: '9.2s' },
  { time: '2026-07-01 12:00:00', provider: '启信宝', type: '增量同步', status: '成功', records: '345', duration: '4.1s' },
]

const filteredLogs = computed(() => {
  return allSyncLogs.filter(l => {
    if (logProviderFilter.value && l.provider !== logProviderFilter.value) return false
    if (logStatusFilter.value && l.status !== logStatusFilter.value) return false
    return true
  })
})

// ─── Log Detail Dialog ───
const logDetail = reactive({
  visible: false,
  data: {
    batchNo: '', provider: '', type: '', status: '', startTime: '', endTime: '',
    scope: '', records: '', duration: '', errors: '', errorMsg: '', suggestion: ''
  }
})

const logDetailsMap: Record<string, any> = {
  '天眼查|增量同步|2026-07-02 10:23:45': {
    batchNo: 'tyc_20260702_0023', scope: '企业信息 + 工商信息', startTime: '10:23:33', endTime: '10:23:45', errors: '0'
  },
  '启信宝|全量同步|2026-07-02 09:52:18': {
    batchNo: 'qxb_20260702_0052', scope: '企业信息 + 工商信息 + 知识产权', startTime: '09:52:10', endTime: '09:52:18', errors: '0'
  },
  '天眼查|增量同步|2026-07-02 08:30:00': {
    batchNo: 'tyc_20260702_0030', scope: '企业信息', startTime: '08:29:53', endTime: '08:30:00', errors: '0'
  },
  '企查查|增量同步|2026-07-02 07:15:22': {
    batchNo: 'qcc_20260702_0015', scope: '企业信息', startTime: '07:15:00', endTime: '07:15:22', errors: '1',
    errorMsg: 'API 超时 (30s)', suggestion: '检查企查查服务状态或增加超时时间'
  },
  '企查查|全量同步|2026-07-01 23:00:00': {
    batchNo: 'qcc_20260701_2300', scope: '企业信息 + 工商信息 + 司法 + 舆情', startTime: '22:59:15', endTime: '23:00:00', errors: '3'
  },
  '天眼查|增量同步|2026-07-01 22:00:00': {
    batchNo: 'tyc_20260701_2200', scope: '企业信息 + 工商信息', startTime: '21:59:54', endTime: '22:00:00', errors: '0'
  },
  '启信宝|增量同步|2026-07-01 21:00:00': {
    batchNo: 'qxb_20260701_2100', scope: '企业信息', startTime: '21:00:00', endTime: '-', errors: '-',
    errorMsg: '', suggestion: '当前进度: 67%，预计剩余 3 分钟'
  },
  '聚合数据|增量同步|2026-07-01 18:30:00': {
    batchNo: '-', scope: '-', startTime: '18:25:00', endTime: '18:30:00', errors: '0',
    errorMsg: '未配置 API 密钥', suggestion: '请先配置聚合数据的 AppKey 和 SecretKey'
  },
  '天眼查|增量同步|2026-07-01 15:00:00': {
    batchNo: 'tyc_20260701_1500', scope: '企业信息 + 工商信息 + 司法风险', startTime: '14:59:48', endTime: '15:00:00', errors: '0'
  },
  '启信宝|增量同步|2026-07-01 12:00:00': {
    batchNo: 'qxb_20260701_1200', scope: '企业信息 + 知识产权', startTime: '11:59:52', endTime: '12:00:00', errors: '0'
  }
}

// ─── Test Connection Dialog ───
const testDialog = reactive({
  visible: false,
  title: '',
  loading: false,
  success: false,
  data: {
    responseTime: '',
    apiVersion: '',
    httpStatus: '',
    serviceStatus: '',
    remainingQuota: '',
    timestamp: '',
    raw: ''
  }
})

// ─── Reset Dialog ───
const resetDialog = reactive({
  visible: false,
  providerName: '',
  providerKey: ''
})

// ═══ Functions ═══

function testConnection(p: Provider) {
  testDialog.title = `${p.name} 连接测试`
  testDialog.loading = true
  testDialog.visible = true

  setTimeout(() => {
    testDialog.loading = false
    const hasKey = p.config.appKey && p.config.secretKey

    // Test result data
    const testResults: Record<string, any> = {
      tianyancha: {
        success: true, responseTime: '236ms', apiVersion: 'v3.2.1', httpStatus: '200 OK',
        serviceStatus: '正常', remainingQuota: '254,322 次', timestamp: new Date().toISOString(),
        raw: '{\n  "code": 0,\n  "message": "success",\n  "data": {\n    "company": "云枢科技",\n    "creditCode": "91440101MA5******"\n  }\n}'
      },
      qixinbao: {
        success: true, responseTime: '312ms', apiVersion: 'v2.5.0', httpStatus: '200 OK',
        serviceStatus: '正常', remainingQuota: '51,766 次', timestamp: new Date().toISOString(),
        raw: '{\n  "code": 200,\n  "msg": "操作成功",\n  "result": {\n    "entName": "云枢科技",\n    "regStatus": "存续"\n  }\n}'
      },
      qichacha: {
        success: true, responseTime: '198ms', apiVersion: 'v4.1.2', httpStatus: '200 OK',
        serviceStatus: '正常', remainingQuota: '11,568 次', timestamp: new Date().toISOString(),
        raw: '{\n  "status": 200,\n  "message": "成功",\n  "data": {\n    "CompanyName": "云枢科技",\n    "Base": "深圳"\n  }\n}'
      },
      juhuishuju: {
        success: false, responseTime: '-', apiVersion: 'v1.0', httpStatus: '401 Unauthorized',
        serviceStatus: '未授权', remainingQuota: '-', timestamp: new Date().toISOString(),
        raw: '{\n  "error_code": 10001,\n  "reason": "无效的 AppKey"\n}'
      }
    }

    const result = testResults[p.key]
    testDialog.success = hasKey && result.success
    testDialog.data = {
      responseTime: hasKey ? result.responseTime : '-',
      apiVersion: result.apiVersion,
      httpStatus: hasKey ? result.httpStatus : '401 Unauthorized',
      serviceStatus: hasKey ? result.serviceStatus : '未配置密钥',
      remainingQuota: hasKey ? result.remainingQuota : '0',
      timestamp: result.timestamp,
      raw: result.raw
    }

    if (!hasKey) {
      testDialog.success = false
      testDialog.data.raw = '请先配置有效的 AppKey 和 SecretKey'
    }
  }, 1800)
}

function saveConfig(p: Provider) {
  // Mock: update online status based on enabled + has key
  p.config.online = p.config.enabled && !!p.config.appKey
  ElMessage.success(`${p.name} 配置已保存 ✓`)
}

function resetConfig(p: Provider) {
  resetDialog.providerName = p.name
  resetDialog.providerKey = p.key
  resetDialog.visible = true
}

function confirmReset() {
  const p = providers.find(pr => pr.key === resetDialog.providerKey)
  if (!p) return
  const d = p.defaults
  p.config.apiUrl = d.apiUrl
  p.config.appKey = d.appKey
  p.config.secretKey = d.secretKey
  p.config.companyName = d.companyName
  p.config.callbackUrl = d.callbackUrl
  p.config.enabled = d.enabled
  p.config.online = d.online
  p.sync.frequency = d.frequency
  p.sync.scopes = [...d.scopes]
  p.sync.dataMode = d.dataMode
  p.sync.autoSync = d.autoSync
  p.sync.lastSyncTime = d.lastSyncTime
  p.sync.lastSyncStatus = d.lastSyncStatus
  p.quota.dailyLimit = d.dailyLimit
  p.quota.dailyUsed = d.dailyUsed
  p.quota.monthlyLimit = d.monthlyLimit
  p.quota.monthlyUsed = d.monthlyUsed
  p.quota.apiRate = d.apiRate
  p.quota.remaining = d.remaining
  p.quota.warningThreshold = d.warningThreshold
  p.quota.exceedPolicy = d.exceedPolicy
  p.advanced.timeout = d.timeout
  p.advanced.retryCount = d.retryCount
  p.advanced.retryInterval = d.retryInterval
  p.advanced.cacheStrategy = d.cacheStrategy
  p.advanced.cacheTTL = d.cacheTTL
  p.advanced.encryption = d.encryption
  p.advanced.signatureAlgo = d.signatureAlgo
  p.advanced.proxy = d.proxy
  p.advanced.logLevel = d.logLevel
  resetDialog.visible = false
  ElMessage.info(`${p.name} 配置已重置为默认值`)
}

function viewLogDetail(row: any) {
  const detailKey = `${row.provider}|${row.type}|${row.time}`
  const detail = logDetailsMap[detailKey]
  if (detail) {
    logDetail.data = {
      batchNo: detail.batchNo,
      provider: row.provider,
      type: row.type,
      status: row.status,
      startTime: detail.startTime,
      endTime: detail.endTime,
      scope: detail.scope,
      records: row.records,
      duration: row.duration,
      errors: detail.errors,
      errorMsg: detail.errorMsg || '',
      suggestion: detail.suggestion || ''
    }
  } else {
    logDetail.data = {
      batchNo: '-', provider: row.provider, type: row.type, status: row.status,
      startTime: '-', endTime: '-', scope: '-', records: row.records,
      duration: row.duration, errors: '0', errorMsg: '', suggestion: ''
    }
  }
  logDetail.visible = true
}

function refreshLogs() {
  logLoading.value = true
  setTimeout(() => { logLoading.value = false }, 500)
}

function onTabChange() {
  // Stats and trend are computed from currentProvider
}

function triggerFullSync() {
  ElMessage.success('全量同步任务已触发，将在后台执行')
}

function exportLogs() {
  ElMessage.success('日志导出中，请稍后查看下载')
}

function goBack() {
  router.push({ name: 'EnterpriseIndex' })
}

// ─── Helpers ───
function formatNum(n: number): string {
  return n.toLocaleString('zh-CN')
}

function quotaPercent(used: number, limit: number): number {
  if (limit <= 0) return 0
  return Math.round((used / limit) * 100)
}
</script>

<style scoped>
.page-container { padding: 16px; }

/* ─── Breadcrumb ─── */
.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

/* ─── Page Header ─── */
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.page-title { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.page-desc { font-size: 13px; color: #909399; margin-bottom: 12px; }
.header-actions { display: flex; gap: 8px; flex-shrink: 0; }

/* ─── Stats Cards ─── */
.stats-card { margin-bottom: 16px; padding: 8px; }
.stats-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.stat-item { text-align: center; padding: 12px 4px; }
.stat-value { font-size: 22px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }
.stat-sub { font-size: 11px; margin-top: 2px; color: #909399; }
.stat-sub.trend-up { color: #34C759; }
.stat-sub.trend-down { color: #FF3B30; }

/* ─── Provider Card ─── */
.provider-card { margin-bottom: 16px; }
:deep(.el-tabs__item) { font-weight: 500; }

/* ─── Provider Header ─── */
.provider-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
.provider-info { display: flex; align-items: center; gap: 12px; }
.provider-logo { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; color: #fff; flex-shrink: 0; }
.provider-name { font-size: 16px; font-weight: 600; }
.provider-desc { font-size: 12px; color: #909399; margin-top: 2px; }

/* ─── Config Grid ─── */
.config-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

/* ─── Config Section ─── */
.config-section { background: #f5f7fa; border-radius: 10px; padding: 20px; border: 1px solid #f0f0f0; }
.section-title { font-size: 14px; font-weight: 600; margin: 0 0 16px; display: flex; align-items: center; gap: 8px; }
.section-title span { font-size: 16px; }
.config-section :deep(.el-form-item) { margin-bottom: 14px; }
.config-section :deep(.el-form-item__label) { font-size: 12px; font-weight: 500; color: #606266; }
.config-section :deep(.el-checkbox) { margin-right: 8px; }
.config-section :deep(.el-radio) { margin-right: 12px; }

.test-btn { margin-top: 4px; }

/* ─── Progress ─── */
.progress-wrap { width: 100%; }
.progress-info { display: flex; justify-content: space-between; font-size: 12px; color: #606266; margin-bottom: 4px; }
.field-hint { font-size: 11px; color: #909399; margin-left: 4px; }

/* ─── Action Bar ─── */
.action-bar { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 0 0; border-top: 1px solid #ebeef5; margin-top: 16px; }

/* ─── Trend Chart ─── */
.trend-card { margin-bottom: 16px; }
.trend-header { display: flex; justify-content: space-between; align-items: center; }
.chart-wrap { padding: 4px 0; }
.chart-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.chart-label { width: 50px; font-size: 12px; color: #909399; text-align: right; flex-shrink: 0; }
.chart-track { flex: 1; height: 22px; background: #f0f2f5; border-radius: 6px; overflow: hidden; position: relative; }
.chart-bar { height: 100%; border-radius: 6px; transition: width 0.8s ease; min-width: 2%; opacity: 0.8; }
.chart-bar:hover { opacity: 1; }
.chart-value { width: 50px; font-size: 12px; color: #606266; flex-shrink: 0; text-align: right; }

/* ─── Sync Logs ─── */
.log-card { margin-bottom: 16px; }
.log-header { display: flex; justify-content: space-between; align-items: center; }
.log-filters { display: flex; gap: 8px; }
.log-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; font-size: 12px; color: #909399; }

/* ─── Test Dialog ─── */
.test-loading { text-align: center; padding: 24px; }
.test-loading p { font-size: 14px; color: #909399; margin-top: 16px; }
.test-status { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; font-size: 16px; font-weight: 600; }
.test-status.success { background: #E8F5E9; color: #2E7D32; }
.test-status.fail { background: #FFEBEE; color: #C62828; }
.test-icon { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; }
.test-status.success .test-icon { background: #34C759; color: #fff; }
.test-status.fail .test-icon { background: #FF3B30; color: #fff; }
.test-details { margin-bottom: 12px; }
.test-raw { margin-top: 12px; }
.raw-label { font-size: 12px; font-weight: 500; color: #606266; margin-bottom: 4px; }
.test-raw pre { background: #f5f7fa; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px; font-size: 12px; line-height: 1.5; max-height: 180px; overflow: auto; color: #333; }

/* ─── Log Detail ─── */
.log-error { margin-top: 12px; }
.log-suggestion { margin-top: 8px; }
</style>