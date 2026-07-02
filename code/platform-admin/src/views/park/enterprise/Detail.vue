<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb-bar">
      <el-button text size="small" @click="goBack">← 返回</el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业列表</el-breadcrumb-item>
        <el-breadcrumb-item>{{ enterprise.name || '加载中' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 顶部基本信息区（≤120px） -->
    <div class="top-info" v-if="enterprise.id">
      <div class="top-info-body">
        <div class="top-name-row">
          <h2 class="top-name">{{ enterprise.name }}</h2>
          <el-tag :type="enterprise.regStatus === '在营' ? 'success' : 'danger'" size="small" class="top-tag">
            {{ enterprise.regStatus || '-' }}
          </el-tag>
          <el-tag v-if="enterprise.industry" type="info" size="small" class="top-tag">
            {{ enterprise.industry }}
          </el-tag>
          <el-button type="primary" size="small" style="margin-left:auto" @click="openEditDialog('base')">✏️ 编辑基本信息</el-button>
        </div>
        <div class="top-meta">
          <span>简称：{{ enterprise.alias || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>信用代码：{{ enterprise.creditCode || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>法人：{{ enterprise.legalPersonName || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>注册资本：{{ enterprise.regCapital || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>评分：<span :style="{ color: scoreColor, fontWeight: 600 }">{{ enterprise.percentileScore ?? '-' }}</span></span>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="loading-wrap">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 7 Tab 内容 -->
    <div v-if="enterprise.id" class="detail-body">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
        <!-- Tab 1: 基本信息 -->
        <el-tab-pane label="基本信息" name="base">
          <div class="tab-toolbar">
            <h3 class="tab-title">工商登记信息</h3>
            <el-button type="primary" size="small" @click="openEditDialog('base')">✏️ 编辑基本信息</el-button>
          </div>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="企业名称">{{ enterprise.name }}</el-descriptions-item>
            <el-descriptions-item label="简称">{{ enterprise.alias || '-' }}</el-descriptions-item>
            <el-descriptions-item label="曾用名">{{ enterprise.historyNames || '-' }}</el-descriptions-item>
            <el-descriptions-item label="英文名">{{ enterprise.engName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码" content-class-name="mono-font">{{ enterprise.creditCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册号" content-class-name="mono-font">{{ enterprise.regNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="纳税人识别号" content-class-name="mono-font">{{ enterprise.taxNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="组织机构代码" content-class-name="mono-font">{{ enterprise.orgNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登记机关">{{ enterprise.regInstitute || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">法人/资本</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="法定代表人">{{ enterprise.legalPersonName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="企业类型">{{ enterprise.companyOrgType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本">{{ enterprise.regCapital || '-' }} {{ enterprise.regCapitalCurrency || '' }}</el-descriptions-item>
            <el-descriptions-item label="实收资本">{{ enterprise.actualCapital || '-' }}</el-descriptions-item>
            <el-descriptions-item label="人员规模">{{ enterprise.staffNumRange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参保人数">{{ enterprise.socialStaffNum ?? '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">期限/状态</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="成立日期">{{ enterprise.estiblishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经营状态">
              <el-tag :type="enterprise.regStatus === '在营' ? 'success' : 'danger'" size="small">{{ enterprise.regStatus || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否小微企业">{{ enterprise.isMicroEnt === 1 ? '是' : '否' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">行业分类</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="行业门类">{{ enterprise.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业大类">{{ enterprise.categoryBig || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业中类">{{ enterprise.categoryMiddle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业小类">{{ enterprise.categorySmall || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业">{{ enterprise.industry || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">联系/经营</h4>
          <el-descriptions :column="2" border size="small" class="detail-descriptions">
            <el-descriptions-item label="注册地址" :span="2">{{ [enterprise.base, enterprise.city, enterprise.district, enterprise.regLocation].filter(Boolean).join(' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ enterprise.phoneNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ enterprise.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经营范围" :span="2">{{ enterprise.businessScope || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 2: 企业概览 -->
        <el-tab-pane label="企业概览" name="overview">
          <div class="tab-toolbar">
            <h3 class="tab-title">企业运营概览</h3>
            <el-button type="primary" size="small" @click="openEditDialog('overview')">✏️ 编辑概览</el-button>
          </div>
          <el-row :gutter="16" class="overview-stats">
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">总资产</div><div class="stat-value">{{ overview?.totalAssets || '-' }}</div></div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">年营收</div><div class="stat-value">{{ overview?.annualRevenue || '-' }}</div></div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">注册资本</div><div class="stat-value">{{ enterprise.regCapital || '-' }}</div></div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">员工数</div><div class="stat-value">{{ overview?.employeeCount ?? '-' }}</div></div></el-card></el-col>
          </el-row>
          <el-row :gutter="16" class="overview-stats" style="margin-top:12px">
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">专利数</div><div class="stat-value">{{ overview?.patentCount ?? '-' }}</div></div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">商标数</div><div class="stat-value">{{ overview?.trademarkCount ?? '-' }}</div></div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">著作权</div><div class="stat-value">{{ overview?.copyrightCount ?? '-' }}</div></div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-box"><div class="stat-label">风险数</div><div class="stat-value" style="color:#F56C6C">{{ overview?.riskCount ?? '-' }}</div></div></el-card></el-col>
          </el-row>
          <el-card shadow="never" style="margin-top:16px">
            <template #header>资本结构</template>
            <div>{{ overview?.equityStructureJson || '暂无数据' }}</div>
          </el-card>
        </el-tab-pane>

        <!-- Tab 3: 企业标签 -->
        <el-tab-pane label="企业标签" name="tag">
          <div class="tab-toolbar">
            <h3 class="tab-title">企业标签</h3>
            <el-button type="primary" size="small" @click="openTagDialog">✏️ 管理标签</el-button>
          </div>
          <div v-if="tags.length > 0" class="tag-list">
            <el-tag v-for="t in tags" :key="t.id" :type="t.status === 1 ? 'success' : 'info'" style="margin:0 8px 8px 0">
              {{ t.tagName }}
            </el-tag>
          </div>
          <el-empty v-else description="暂无标签" />
        </el-tab-pane>

        <!-- Tab 4: 客户信息 -->
        <el-tab-pane label="客户信息" name="customer">
          <div class="tab-toolbar">
            <h3 class="tab-title">客户信息</h3>
            <div>
              <el-button size="small" type="primary" @click="goCustomerProfile">👤 客户画像</el-button>
              <el-button size="small" type="success" @click="openCustomerDialog" style="margin-left:8px">新增客户</el-button>
            </div>
          </div>
          <el-table :data="customerList" v-loading="customerLoading" border stripe>
            <el-table-column prop="code" label="项目编号" width="130" />
            <el-table-column prop="name" label="负责人" width="100" />
            <el-table-column prop="phone" label="电话" width="120" />
            <el-table-column label="客户类型" width="100">
              <template #default="scope">
                <el-tag :type="getCustType(scope.row.customerType)">{{ getCustLabel(scope.row.customerType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="industrialField" label="产业领域" width="120">
              <template #default="scope">{{ getFieldLabel(scope.row.industrialField) }}</template>
            </el-table-column>
            <el-table-column prop="registTime" label="注册时间" width="150" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="editCustomer(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 5: 云企库数据 -->
        <el-tab-pane label="云企库数据" name="cloud">
          <div class="tab-toolbar">
            <h3 class="tab-title">云企库数据</h3>
            <el-button type="primary" size="small">✏️ 管理数据</el-button>
          </div>
          <el-table :data="cloudDataList" v-loading="cloudLoading" border stripe>
            <el-table-column prop="category" label="数据分类" width="120">
              <template #default="scope">{{ cloudCategoryLabel(scope.row.category) }}</template>
            </el-table-column>
            <el-table-column prop="dataYear" label="年度" width="80" />
            <el-table-column prop="dataContent" label="数据内容" min-width="300" :show-overflow-tooltip="true" />
            <el-table-column prop="createTime" label="创建时间" width="150" />
          </el-table>
        </el-tab-pane>

        <!-- Tab 6: 关注标签 -->
        <el-tab-pane label="关注标签" name="focus">
          <div class="tab-toolbar">
            <h3 class="tab-title">关注标签</h3>
            <el-button type="primary" size="small" @click="openFocusDialog">✏️ 编辑关注</el-button>
          </div>
          <div v-if="focusList.length > 0">
            <el-card v-for="f in focusList" :key="f.id" shadow="never" class="focus-card">
              <template #header>{{ f.name }}</template>
              <div>
                <el-tag v-for="item in (f.items || [])" :key="item.id" style="margin:0 8px 8px 0" size="small">
                  {{ item.name }}
                </el-tag>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无关注标签" />
        </el-tab-pane>

        <!-- Tab 7: 工商信息 -->
        <el-tab-pane label="工商信息" name="register">
          <div class="tab-toolbar">
            <h3 class="tab-title">工商登记信息</h3>
            <el-button type="primary" size="small" @click="openEditDialog('register')">✏️ 编辑</el-button>
          </div>
          <el-descriptions :column="2" border size="small" class="detail-descriptions">
            <el-descriptions-item label="注册号">{{ enterprise.regNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码">{{ enterprise.creditCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="组织机构代码">{{ enterprise.orgNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="纳税人识别号">{{ enterprise.taxNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本">{{ enterprise.regCapital || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实收资本">{{ enterprise.actualCapital || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成立日期">{{ enterprise.estiblishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登记机关">{{ enterprise.regInstitute || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册地址" :span="2">{{ [enterprise.base, enterprise.city, enterprise.district, enterprise.regLocation].filter(Boolean).join(' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经营状态">
              <el-tag :type="enterprise.regStatus === '在营' ? 'success' : 'danger'" size="small">{{ enterprise.regStatus || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="经营范围" :span="2">{{ enterprise.businessScope || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">股票信息</h4>
          <el-descriptions :column="2" border size="small" class="detail-descriptions">
            <el-descriptions-item label="股票代码">{{ enterprise.bondNum || '-' }}</el-descriptions-item>
            <el-descriptions-item label="股票名称">{{ enterprise.bondName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="股票类型">{{ enterprise.bondType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="曾用名">{{ enterprise.usedBondName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEnterpriseById, type Enterprise } from '@/api/enterprise'
import { overviewPage, type EnterpriseOverview } from '@/api/enterprise-cloud'
import { getCustomerPage, getCustomerById, type CustomerInformation } from '@/api/enterprise'
import { getFocusPage, getFocusItemByFocusId } from '@/api/enterprise'
import { listTagByEnterprise } from '@/api/enterprise-tag'

const route = useRoute()
const router = useRouter()
const enterpriseId = computed(() => route.params.id as string)

// ─── State ───
const loading = ref(true)
const activeTab = ref('base')
const enterprise = ref<Enterprise>({})

// 企业概览
const overview = ref<EnterpriseOverview>()

// 标签
const tags = ref<any[]>([])

// 客户信息
const customerLoading = ref(false)
const customerList = ref<CustomerInformation[]>([])

// 云企库数据
const cloudLoading = ref(false)
const cloudDataList = ref<any[]>([])

// 关注标签
const focusList = ref<any[]>([])

// ─── Computed ───
const scoreColor = computed(() => {
  const s = enterprise.value.percentileScore
  if (s == null) return '#999'
  if (s >= 80) return '#34C759'
  if (s >= 60) return '#FF9500'
  return '#FF3B30'
})

// ─── Lifecycle ───
onMounted(async () => {
  if (!enterpriseId.value) return
  await loadEnterprise()
  await loadOverview()
  await loadTags()
  await loadCustomers()
  await loadFocus()
})

async function loadEnterprise() {
  try {
    const res: any = await getEnterpriseById(enterpriseId.value)
    if (res.code === 200) {
      enterprise.value = res.data || {}
    } else {
      ElMessage.error(res.message || '加载企业信息失败')
    }
  } catch {
    ElMessage.error('加载企业信息失败')
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  try {
    const res: any = await overviewPage({ enterpriseId: enterpriseId.value, pageNum: 1, pageSize: 1 })
    if (res.code === 200) {
      overview.value = res.data?.records?.[0]
    }
  } catch { /* best-effort */ }
}

async function loadTags() {
  try {
    const res: any = await listTagByEnterprise(Number(enterpriseId.value))
    if (res.code === 200) {
      tags.value = res.data || []
    }
  } catch { /* best-effort */ }
}

async function loadCustomers() {
  customerLoading.value = true
  try {
    const res: any = await getCustomerPage({ current: 1, size: 50 })
    if (res.code === 200) {
      customerList.value = (res.data?.records || []).filter((c: any) => String(c.enterpriseId) === enterpriseId.value)
    }
  } catch { /* best-effort */ }
  finally { customerLoading.value = false }
}

async function loadFocus() {
  try {
    const res: any = await getFocusPage({ current: 1, size: 50 })
    if (res.code === 200) {
      const items = res.data?.records || []
      const enriched = await Promise.all(items.map(async (f: any) => {
        try {
          const r: any = await getFocusItemByFocusId(f.id!)
          return { ...f, items: r.data || [] }
        } catch { return { ...f, items: [] } }
      }))
      focusList.value = enriched
    }
  } catch { /* best-effort */ }
}

// ─── Navigation ───
function goBack() {
  router.push({ name: 'EnterpriseIndex' })
}

function goCustomerProfile() {
  // 跳转到客户画像页 (后续实现, 现在跳转到客户管理页)
  router.push({ name: 'EnterpriseCustomer' })
}

// ─── Dialogs (placeholder) ───
function openEditDialog(section: string) {
  ElMessage.info(`编辑 ${section} 功能待对接后端`)
}

function openTagDialog() {
  ElMessage.info('标签管理功能待对接后端')
}

function openCustomerDialog() {
  ElMessage.info('新增客户功能待对接后端')
}

function openFocusDialog() {
  ElMessage.info('关注标签编辑功能待对接后端')
}

function editCustomer(row: CustomerInformation) {
  ElMessage.info(`编辑客户: ${row.code}`)
}

function onTabChange(name: string) {
  // 可按需懒加载数据
}

// ─── Helpers ───
function getCustType(t?: number) {
  return ({ 1: 'info', 2: 'warning', 3: 'success' } as Record<number, string>)[t || 0] || 'info'
}
function getCustLabel(t?: number) {
  return ({ 1: '潜在', 2: '意向', 3: '已签约' } as Record<number, string>)[t || 0] || '-'
}
function getFieldLabel(f?: number) {
  return ({ 1: '集成电路', 2: '生物医药', 3: '新材料', 4: '新能源', 5: '智能制造', 6: '信创' } as Record<number, string>)[f || 0] || '-'
}
function cloudCategoryLabel(c: string) {
  return ({ business_risk: '经营风险', business_situation: '经营状况', ent_detail: '企业详情', judicial_risk: '司法风险', knowledge: '企业知识' } as Record<string, string>)[c] || c
}
</script>

<style scoped>
.page-container { padding: 16px; }

.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

.top-info { background: #fff; border-radius: 8px; border: 1px solid #e4e7ed; padding: 16px 24px; margin-bottom: 16px; min-height: 80px; }
.top-info-body { display: flex; flex-direction: column; gap: 8px; }
.top-name-row { display: flex; align-items: center; gap: 10px; }
.top-name { font-size: 18px; font-weight: 700; margin: 0; }
.top-tag { flex-shrink: 0; }
.top-meta { font-size: 12px; color: #909399; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.meta-sep { color: #dcdfe6; margin: 0 4px; }

.loading-wrap { padding: 40px; }

.detail-body { min-height: 400px; }
.detail-body :deep(.el-tabs--border-card) { box-shadow: none; border: 1px solid #e4e7ed; }

.tab-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-title { font-size: 16px; font-weight: 600; margin: 0; }

.section-title { font-size: 14px; font-weight: 600; color: #303133; margin: 20px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }

.detail-descriptions { margin-bottom: 8px; }
.detail-descriptions :deep(.mono-font) { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 12px; }

.overview-stats { margin-bottom: 0; }
.overview-stats .stat-box { text-align: center; padding: 8px 0; }
.overview-stats .stat-label { font-size: 12px; color: #909399; margin-bottom: 8px; }
.overview-stats .stat-value { font-size: 22px; font-weight: 700; color: #303133; }

.tag-list { display: flex; flex-wrap: wrap; padding: 8px 0; }

.focus-card { margin-bottom: 12px; }
.focus-card :deep(.el-card__header) { font-weight: 600; font-size: 14px; padding: 12px 16px; }

/* override el-descriptions table for better word-break */
.detail-descriptions :deep(.el-descriptions__cell) { word-break: break-word; }
</style>