<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb-bar">
      <el-button text size="small" @click="goBack">← 返回</el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>客户画像</el-breadcrumb-item>
      </el-breadcrumb>
      <div style="margin-left:auto">
        <el-select v-model="selectedEnterprise" size="small" style="width:260px" @change="switchCustomer">
          <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </div>
    </div>

    <!-- 顶部画像卡 -->
    <div class="profile-header">
      <div>
        <div class="profile-name">{{ currentEnterprise?.name || '企业名称' }}
          <el-tag size="small" style="background:rgba(255,255,255,0.2);border:none;color:#fff;margin-left:8px">VIP</el-tag>
        </div>
        <div class="profile-meta">
          <span>🏭 {{ currentEnterprise?.industry || '-' }}</span>
          <span>📅 合作 4 年</span>
          <span>📊 已签约</span>
          <span>👥 {{ currentEnterprise?.staffNumRange || '-' }}</span>
        </div>
      </div>
      <div class="score-block">
        <div class="score-label">客户价值评分</div>
        <div class="score-value">98</div>
        <div class="score-sub">/ 100 · 优</div>
      </div>
      <div class="health-block">
        <div class="score-label">健康度</div>
        <div class="health-grade">A 级</div>
        <div class="score-sub">健康</div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
      <!-- 左栏 -->
      <div>
        <!-- RFM 模型 -->
        <el-card shadow="never" class="section-card">
          <template #header><strong>📊 RFM 客户价值模型</strong></template>
          <div class="rfm-grid">
            <div v-for="item in rfmData" :key="item.label" class="rfm-item">
              <div class="rfm-label">{{ item.label }}</div>
              <div class="rfm-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="rfm-bar"><div class="rfm-fill" :style="{ width: item.percent + '%', background: item.color }"></div></div>
            </div>
          </div>
        </el-card>

        <!-- 关键联系人 -->
        <el-card shadow="never" class="section-card" style="margin-top:16px">
          <template #header><strong>👥 关键联系人</strong></template>
          <div v-for="c in contacts" :key="c.name" class="contact-row">
            <el-avatar :size="40">{{ c.name[0] }}</el-avatar>
            <div class="contact-info">
              <div class="contact-name">{{ c.name }} <el-tag size="small" :type="c.role === '决策人' ? 'danger' : 'info'">{{ c.role }}</el-tag></div>
              <div class="contact-detail">{{ c.position }} · {{ c.phone }} · {{ c.email }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右栏 -->
      <div>
        <!-- 健康度雷达 -->
        <el-card shadow="never" class="section-card">
          <template #header><strong>📈 企业健康度</strong></template>
          <div v-for="h in healthData" :key="h.name" class="radar-row">
            <div class="radar-name">{{ h.name }}</div>
            <div class="radar-track">
              <div class="radar-fill" :style="{ width: h.score + '%', background: h.color }"></div>
            </div>
            <div class="radar-score">{{ h.score }}</div>
          </div>
        </el-card>

        <!-- 合作历史时间轴 -->
        <el-card shadow="never" class="section-card" style="margin-top:16px">
          <template #header><strong>📜 合作历史</strong></template>
          <div class="timeline">
            <div v-for="(t, i) in timeline" :key="i" class="timeline-item">
              <div class="tl-time">{{ t.time }}</div>
              <div class="tl-title">{{ t.title }}</div>
              <div class="tl-desc">{{ t.desc }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- AI 跟进建议 -->
    <el-card shadow="never" class="section-card" style="margin-top:16px">
      <template #header><strong>🤖 AI 跟进建议</strong></template>
      <el-row :gutter="16">
        <el-col v-for="(s, i) in aiSuggestions" :key="i" :span="6">
          <div class="ai-card" :style="{ borderLeftColor: s.color }">
            <div class="ai-tag" :style="{ background: s.color + '18', color: s.color }">{{ s.tag }}</div>
            <div class="ai-text">{{ s.text }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEnterpriseById, getEnterprisePage, type Enterprise } from '@/api/enterprise'

const router = useRouter()

const selectedEnterprise = ref<number>()
const currentEnterprise = ref<Enterprise | null>(null)
const enterpriseOptions = ref<Enterprise[]>([])

const rfmData = [
  { label: 'R - 最近合作', value: '3 天前', percent: 98, color: '#34C759' },
  { label: 'F - 合作频次', value: '12 次/年', percent: 85, color: '#409EFF' },
  { label: 'M - 合作金额', value: '¥ 4.2 亿', percent: 92, color: '#5856D6' },
  { label: '满意度', value: '96%', percent: 96, color: '#34C759' },
  { label: '续约率', value: '100%', percent: 100, color: '#E6A23C' },
]

const healthData = [
  { name: '经营稳定性', score: 95, color: '#34C759' },
  { name: '成长性', score: 88, color: '#409EFF' },
  { name: '信用状况', score: 92, color: '#5856D6' },
  { name: '创新能力', score: 97, color: '#34C759' },
  { name: '合规性', score: 85, color: '#E6A23C' },
  { name: '社会责任', score: 90, color: '#409EFF' },
]

const contacts = [
  { name: '任正非', role: '决策人', position: 'CEO', phone: '138****0001', email: 'rzh@huawei.com' },
  { name: '孟晚舟', role: '财务负责人', position: 'CFO', phone: '138****0002', email: 'mwz@huawei.com' },
  { name: '张平安', role: '项目对接', position: '云业务总裁', phone: '138****0003', email: 'zpa@huawei.com' },
]

const timeline = [
  { time: '2026-05-15', title: '续签年度战略合作协议', desc: '达成 3 年期战略合作，约定年度采购额不低于 5 亿元' },
  { time: '2026-03-20', title: '完成智能园区升级项目', desc: '华为云承接园区智能化改造，合同金额 2,800 万' },
  { time: '2025-12-01', title: '新增 5 栋楼宇入驻', desc: '华为旗下 3 家子公司新入驻园区，总计面积 12,000 m²' },
  { time: '2025-08-10', title: '启动联合创新实验室', desc: '与园区共建 AI + 物联网联合创新实验室' },
  { time: '2025-03-01', title: '首次入驻园区', desc: '华为云事业部首批入驻，租用面积 5,000 m²' },
]

const aiSuggestions = [
  { tag: '续约预警', text: '距合同到期还有 45 天，建议提前启动续约谈判', color: '#F56C6C' },
  { tag: '交叉销售', text: '华为云正拓展 AI 训练平台，可推荐园区 GPU 算力服务', color: '#409EFF' },
  { tag: '关系维护', text: '近 1 个月无高层互动，建议安排季度战略复盘会', color: '#E6A23C' },
  { tag: '增值服务', text: '企业员工数增长 30%，可推荐人才公寓扩租方案', color: '#34C759' },
]

onMounted(async () => {
  // Load enterprise options
  try {
    const res: any = await getEnterprisePage({ pageNum: 1, pageSize: 50 })
    if (res.code === 200) {
      enterpriseOptions.value = res.data?.records || []
      if (enterpriseOptions.value.length > 0) {
        selectedEnterprise.value = Number(enterpriseOptions.value[0].id)
        await loadEnterprise(selectedEnterprise.value)
      }
    }
  } catch { /* best-effort */ }
})

async function loadEnterprise(id: number) {
  try {
    const res: any = await getEnterpriseById(String(id))
    if (res.code === 200) {
      currentEnterprise.value = res.data
    }
  } catch { /* ignore */ }
}

function switchCustomer(id: number) {
  loadEnterprise(id)
}

function goBack() {
  router.push({ name: 'EnterpriseIndex' })
}
</script>

<style scoped>
.page-container { padding: 16px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

/* 顶部画像卡 */
.profile-header {
  background: linear-gradient(135deg, #409EFF 0%, #5856D6 100%);
  color: #fff; border-radius: 12px; padding: 24px; margin-bottom: 16px;
  display: flex; justify-content: space-between; align-items: center;
}
.profile-name { font-size: 20px; font-weight: 600; margin-bottom: 6px; }
.profile-meta { font-size: 13px; opacity: 0.85; display: flex; gap: 16px; flex-wrap: wrap; }
.profile-meta span { background: rgba(255,255,255,0.15); padding: 4px 10px; border-radius: 12px; }
.score-block { text-align: center; padding: 0 24px; border-left: 1px solid rgba(255,255,255,0.3); }
.health-block { text-align: center; padding-left: 24px; }
.score-label { font-size: 11px; opacity: 0.8; }
.score-value { font-size: 36px; font-weight: 700; line-height: 1.2; }
.score-sub { font-size: 11px; opacity: 0.8; }
.health-grade { font-size: 16px; font-weight: 600; }

/* RFM */
.rfm-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.rfm-item { text-align: center; padding: 12px; }
.rfm-label { font-size: 11px; color: #909399; margin-bottom: 4px; }
.rfm-value { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
.rfm-bar { height: 4px; background: #f0f0f0; border-radius: 2px; overflow: hidden; }
.rfm-fill { height: 100%; border-radius: 2px; }

/* 联系人 */
.contact-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.contact-row:last-child { border-bottom: none; }
.contact-info { flex: 1; }
.contact-name { font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 6px; }
.contact-detail { font-size: 12px; color: #909399; margin-top: 2px; }

/* 健康度 */
.radar-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.radar-name { width: 90px; font-size: 12px; color: #606266; }
.radar-track { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.radar-fill { height: 100%; border-radius: 4px; }
.radar-score { width: 40px; font-size: 13px; font-weight: 600; text-align: right; }

/* 时间轴 */
.timeline { position: relative; padding-left: 20px; }
.timeline::before { content: ''; position: absolute; left: 6px; top: 8px; bottom: 8px; width: 2px; background: #e4e7ed; }
.timeline-item { position: relative; padding-bottom: 16px; }
.timeline-item::before {
  content: ''; position: absolute; left: -16px; top: 6px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #409EFF; border: 2px solid #fff; box-shadow: 0 0 0 2px #ecf5ff;
}
.tl-time { font-size: 11px; color: #909399; margin-bottom: 2px; }
.tl-title { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.tl-desc { font-size: 12px; color: #909399; }

/* AI 建议 */
.ai-card { padding: 16px; border-radius: 8px; border: 1px solid #e4e7ed; border-left: 4px solid; }
.ai-tag { font-size: 11px; font-weight: 600; margin-bottom: 8px; display: inline-block; padding: 2px 8px; border-radius: 4px; }
.ai-text { font-size: 12px; color: #606266; line-height: 1.5; }

.section-card { border: 1px solid #e4e7ed; }
.section-card :deep(.el-card__header) { padding: 12px 16px; font-size: 14px; }
</style>