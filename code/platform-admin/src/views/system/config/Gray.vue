<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ============ Tab 1: 灰度开关状态 ============ -->
      <el-tab-pane label="灰度开关状态" name="status">
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
                <el-button :loading="loadingStatus" @click="loadStatus" type="primary" plain>
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
              1. <strong>DB 方式</strong>: 系统管理 → 参数配置 → 改 sys_config 中同名键 (自动写 sys_gray_audit)
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
            v-loading="loadingStatus"
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
                <li>DB 改值: 系统管理 → 参数配置 → 编辑 platform.* 配置 → <strong>自动写 sys_gray_audit</strong></li>
                <li>紧急止血: docker-compose.yml 加 <code>PLATFORM_*_ENABLED=false</code> 环境变量 → 重启服务</li>
              </ul>
            </li>
            <li>
              <strong>PR 进展</strong>:
              <ul>
                <li>✅ PR1 (63c6678): common.yml 集中托管 platform.*</li>
                <li>✅ PR2 (3b48228): @RefreshScope 热生效改造</li>
                <li>✅ PR3 (e66d956): 统一灰度查询 API + UI</li>
                <li>🔵 PR4 (进行中): sys_gray_audit 审计 + POST /gray/switch</li>
              </ul>
            </li>
          </ol>
        </el-card>
      </el-tab-pane>

      <!-- ============ Tab 2: 操作历史 (gray-release PR4) ============ -->
      <el-tab-pane label="操作历史" name="history">
        <el-card>
          <template #header>
            <div class="header-row">
              <div>
                <h3 class="page-title">灰度操作审计</h3>
                <div class="page-subtitle">
                  所有 platform.* 灰度开关变更记录 (DB_UPDATE / NACOS_PUSH / INIT 等)
                </div>
              </div>
              <div class="header-actions">
                <el-input
                  v-model="filterKey"
                  placeholder="按 key 过滤 (可选)"
                  clearable
                  style="width: 280px"
                  @clear="loadHistory"
                />
                <el-button :loading="loadingHistory" @click="loadHistory" type="primary" plain>
                  <i class="fas fa-sync-alt" /> 查询
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="auditList" v-loading="loadingHistory" border stripe>
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column prop="switchKey" label="配置键" min-width="280" show-overflow-tooltip />
            <el-table-column prop="groupName" label="分组" width="200" show-overflow-tooltip />
            <el-table-column label="变更" width="220">
              <template #default="{ row }">
                <el-tag size="small" type="danger" v-if="row.oldValue">{{ row.oldValue }}</el-tag>
                <el-tag size="small" type="info" v-else>(空)</el-tag>
                <i class="fas fa-arrow-right arrow-icon" />
                <el-tag size="small" type="success">{{ row.newValue }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="opType" label="操作类型" width="110">
              <template #default="{ row }">
                <el-tag :type="opTypeColor(row.opType)" size="small">{{ row.opType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="instanceIp" label="实例" width="130" />
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadHistory"
              @current-change="loadHistory"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listGraySwitches, pageGrayAudit, type GraySwitchVO, type GrayAuditVO } from '@/api/gray'

const activeTab = ref('status')

// ============ Tab 1: 状态 ============
const loadingStatus = ref(false)
const switches = ref<GraySwitchVO[]>([])
const lastQueryTime = ref('')

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

async function loadStatus() {
  loadingStatus.value = true
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
    loadingStatus.value = false
  }
}

// ============ Tab 2: 历史 ============
const loadingHistory = ref(false)
const auditList = ref<GrayAuditVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const filterKey = ref('')

function opTypeColor(opType: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (opType) {
    case 'DB_UPDATE':
      return 'success'
    case 'NACOS_PUSH':
      return 'warning'
    case 'INIT':
      return 'info'
    case 'BEAN_REFRESH':
      return 'warning'
    default:
      return 'info'
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res: any = await pageGrayAudit({
      switchKey: filterKey.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      auditList.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '加载历史失败')
    }
  } catch (e: any) {
    ElMessage.error('请求失败: ' + (e.message || e))
  } finally {
    loadingHistory.value = false
  }
}

onMounted(loadStatus)
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

.arrow-icon {
  margin: 0 6px;
  color: #909399;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>