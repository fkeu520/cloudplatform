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
        <el-select v-model="selectedEnterprise" size="small" style="width:280px" @change="switchCustomer" clearable placeholder="选择企业查看画像">
          <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </div>
    </div>

    <transition name="profile-fade" mode="out-in">
      <div :key="selectedEnterprise || 0" class="profile-body">
        <!-- 顶部画像卡 -->
        <div class="profile-header" :style="{ background: profile.gradient }">
          <div>
            <div class="profile-name">{{ profile.name }}
              <el-tag size="small" class="level-tag" :style="{
                background: profile.levelColor + '25',
                border: '1px solid ' + profile.levelColor,
                color: profile.levelColor
              }">{{ profile.level }}</el-tag>
            </div>
            <div class="profile-meta">
              <span>🏭 {{ profile.industry }}</span>
              <span>📅 合作 {{ profile.cooperationYears }}</span>
              <span>📊 {{ profile.status }}</span>
              <span>👥 {{ profile.staffCount }}</span>
              <span>🌐 {{ profile.region }}</span>
            </div>
          </div>
          <div class="score-block">
            <div class="score-label">客户价值评分</div>
            <div class="score-value" :style="{ color: profile.scoreColor }">{{ profile.score }}</div>
            <div class="score-sub">/ 100 · {{ profile.scoreLabel }}</div>
          </div>
          <div class="health-block">
            <div class="score-label">健康度</div>
            <div class="health-grade" :style="{ color: profile.healthColor }">{{ profile.health }}</div>
            <div class="score-sub">{{ profile.healthLabel }}</div>
          </div>
        </div>

        <!-- RFM 模型 - 5列 -->
        <el-card shadow="never" class="section-card">
          <template #header><strong>📊 RFM 客户价值模型</strong></template>
          <div class="rfm-grid">
            <div v-for="item in profile.rfm" :key="item.label" class="rfm-item">
              <div class="rfm-label">{{ item.label }}</div>
              <div class="rfm-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="rfm-bar"><div class="rfm-fill" :style="{ width: item.percent + '%', background: item.color }"></div></div>
            </div>
          </div>
        </el-card>

        <!-- 双栏：健康度雷达 + 关键联系人 -->
        <div class="dual-grid">
          <el-card shadow="never" class="section-card">
            <template #header><strong>🎯 客户健康度评估</strong></template>
            <div v-for="h in profile.healthDimensions" :key="h.name" class="radar-row">
              <div class="radar-name">{{ h.name }}</div>
              <div class="radar-track">
                <div class="radar-fill" :style="{ width: h.score + '%', background: profile.gradient }"></div>
              </div>
              <div class="radar-score">{{ h.score }}</div>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header><strong>👥 关键联系人（{{ profile.contacts.length }}）</strong></template>
            <div v-for="c in profile.contacts" :key="c.name" class="contact-block">
              <div class="contact-row">
                <el-avatar :size="36" :style="{ background: c.avatarBg }">{{ c.initial }}</el-avatar>
                <div class="contact-info">
                  <div class="contact-name">{{ c.name }}
                    <el-tag size="small" :type="c.roleTag === 'danger' ? 'danger' : c.roleTag === 'warning' ? 'warning' : c.roleTag === 'info' ? 'info' : ''">{{ c.role }}</el-tag>
                  </div>
                  <div class="contact-detail">{{ c.position }} · {{ c.phone }}</div>
                </div>
              </div>
              <div class="contact-ext">
                📧 {{ c.email }}{{ c.address ? ' · 🏢 ' + c.address : '' }}<br>
                ⭐ 影响力：{{ c.influence }} · 决策权：{{ c.decision }}
              </div>
            </div>
          </el-card>
        </div>

        <!-- 合作历史时间轴 -->
        <el-card shadow="never" class="section-card">
          <template #header><strong>📅 合作历史（最近 {{ profile.timeline.length }} 个里程碑）</strong></template>
          <div class="timeline">
            <div v-for="(t, i) in profile.timeline" :key="i" class="timeline-item">
              <div class="tl-time">{{ t.time }}</div>
              <div class="tl-title">{{ t.title }}</div>
              <div class="tl-desc">{{ t.desc }}</div>
            </div>
          </div>
        </el-card>

        <!-- 双栏：需求摘要 + AI 跟进建议 -->
        <div class="dual-grid">
          <el-card shadow="never" class="section-card">
            <template #header><strong>📋 客户需求摘要</strong></template>
            <div class="descriptions">
              <div v-for="n in profile.needs" :key="n.label" :class="['desc-item', { 'desc-full': n.full }]">
                <div class="desc-label">{{ n.label }}</div>
                <div class="desc-value" :style="{ color: n.color }">{{ n.value }}</div>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header><strong>💡 AI 跟进建议</strong></template>
            <div v-for="(s, i) in profile.aiSuggestions" :key="i" class="ai-card" :style="{ background: s.bg, borderLeftColor: s.color }">
              <div class="ai-tag" :style="{ background: s.color + '18', color: s.color }">{{ s.tag }}</div>
              <div class="ai-text">{{ s.text }}</div>
            </div>
          </el-card>
        </div>

        <div class="footer-note">
          📊 数据更新于 2026-07-01 10:23 · 客户画像由 RFM 模型 + 健康度评估 + AI 推荐引擎综合生成 · 仅供内部参考
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getEnterprisePage, getEnterpriseById, type Enterprise } from '@/api/enterprise'

const router = useRouter()

const selectedEnterprise = ref<number>()
const currentEnterprise = ref<Enterprise | null>(null)
const enterpriseOptions = ref<Enterprise[]>([])

// ==================== Mock Profile 数据接口 ====================
interface RfmItem { label: string; value: string; percent: number; color: string }
interface HealthItem { name: string; score: number }
interface ContactItem { name: string; initial: string; avatarBg: string; position: string; role: string; roleTag: string; phone: string; email: string; address?: string; influence: string; decision: string }
interface TimelineEvent { time: string; title: string; desc: string }
interface NeedItem { label: string; value: string; full?: boolean; color?: string }
interface AiSuggestion { tag: string; text: string; color: string; bg: string }

interface CustomerProfile {
  name: string
  level: string
  levelColor: string
  gradient: string
  industry: string
  cooperationYears: string
  status: string
  staffCount: string
  region: string
  score: number
  scoreColor: string
  scoreLabel: string
  health: string
  healthColor: string
  healthLabel: string
  rfm: RfmItem[]
  healthDimensions: HealthItem[]
  contacts: ContactItem[]
  timeline: TimelineEvent[]
  needs: NeedItem[]
  aiSuggestions: AiSuggestion[]
}

// ==================== 5 套 Mock Profiles ====================
const MOCK_PROFILES: Record<string, CustomerProfile> = {
  huawei: {
    name: '华为云计算技术有限公司',
    level: 'VIP 客户',
    levelColor: '#FFD700',
    gradient: 'linear-gradient(135deg, #409EFF 0%, #5856D6 100%)',
    industry: '信息传输/软件/信息技术',
    cooperationYears: '4 年',
    status: '已签约',
    staffCount: '20,000+ 人',
    region: '深圳/全国',
    score: 98,
    scoreColor: '#FFD700',
    scoreLabel: '卓越',
    health: 'A 级',
    healthColor: '#34C759',
    healthLabel: '非常健康',
    rfm: [
      { label: 'R - 最近合作', value: '3 天前', percent: 98, color: '#34C759' },
      { label: 'F - 合作频次', value: '12 次/年', percent: 85, color: '#409EFF' },
      { label: 'M - 合作金额', value: '¥ 4.2 亿', percent: 92, color: '#5856D6' },
      { label: '满意度', value: '96%', percent: 96, color: '#34C759' },
      { label: '续约概率', value: '92%', percent: 92, color: '#E6A23C' },
    ],
    healthDimensions: [
      { name: '合作时长', score: 80 }, { name: '沟通频率', score: 90 },
      { name: '需求响应', score: 95 }, { name: '付款及时性', score: 98 },
      { name: '业务增长', score: 75 }, { name: '满意度', score: 96 },
    ],
    contacts: [
      { name: '张伟', initial: '张', avatarBg: 'linear-gradient(135deg, #409EFF, #5856D6)', position: '招商经理', role: '主联系人', roleTag: 'info', phone: '139****0001', email: 'zhangwei@huawei.com', address: '坂田基地 A 座 12-15 楼', influence: '高', decision: '中' },
      { name: '李娜', initial: '李', avatarBg: 'linear-gradient(135deg, #34C759, #409EFF)', position: '招商专员', role: '辅助', roleTag: '', phone: '139****0002', email: 'lina@huawei.com', address: '坂田基地 B 座 8 楼', influence: '中', decision: '低' },
      { name: '王强', initial: '王', avatarBg: 'linear-gradient(135deg, #FF9500, #FF3B30)', position: '招商总监', role: '决策人', roleTag: 'danger', phone: '139****0003', email: 'wangqiang@huawei.com', influence: '高', decision: '高' },
    ],
    timeline: [
      { time: '2026-06-15', title: '🏆 续约战略合作协议（5 年）', desc: '签订 5 年战略合作协议，签约金额 ¥ 4.2 亿，涵盖 12,000 m² 办公场地 + 综合服务' },
      { time: '2025-12-08', title: '🏢 扩租：增加 3,500 m² 研发场地', desc: '原 A 座扩租 +3,500 m²（12-15 楼），用于 5G 研发团队扩张' },
      { time: '2024-09-20', title: '⭐ 评级升级为 VIP 客户', desc: '合作金额突破 3 亿，续约率 100%，升级为 VIP 客户，享受优先服务' },
      { time: '2024-03-15', title: '📋 首次签约入驻（HX-2024-001）', desc: '项目编号 HX-2024-001，租用 A 座 12-15 楼 8,500 m²，签约 3 年' },
      { time: '2023-09-01', title: '🤝 初次接触（招商对接）', desc: '招商经理张伟首次接洽华为投资控股有限公司，沟通入驻意向' },
    ],
    needs: [
      { label: '主要需求', value: '办公场地 + 综合服务 + 班车 + 团餐 + 公寓' },
      { label: '扩展需求', value: '研发场地（5G/AI）+ 实验室' },
      { label: '投诉记录', value: '0 起', color: '#34C759' },
      { label: '表扬记录', value: '3 起（服务态度/响应速度/设施维护）' },
      { label: '主要痛点', value: '会议室不足；高峰期停车位紧张；园区周边餐饮选择有限', full: true },
    ],
    aiSuggestions: [
      { tag: '续约预警', text: '根据 RFM 评分，5 年战略协议续签概率 92%，建议提前 6 个月开始续约谈判', color: '#F56C6C', bg: 'linear-gradient(135deg, #fef0ef 0%, #fde2e2 100%)' },
      { tag: '扩展机会', text: '客户近期招聘 5G 研发团队 200 人，建议主动对接，提供 B/C 座预留场地', color: '#409EFF', bg: 'linear-gradient(135deg, #ecf5ff 0%, #e8f4fd 100%)' },
      { tag: '风险预警', text: '会议室预订冲突率上升（近 30 天 +15%），建议扩容 2 间智能会议室', color: '#E6A23C', bg: 'linear-gradient(135deg, #fef4e4 0%, #fdf0e0 100%)' },
      { tag: '关怀建议', text: '高管团队近期变动，建议安排季度战略复盘会，维护关键决策人关系', color: '#34C759', bg: 'linear-gradient(135deg, #edf7ed 0%, #e8f5e9 100%)' },
    ],
  },

  zte: {
    name: '中兴通讯股份有限公司',
    level: '钻石客户',
    levelColor: '#00BFFF',
    gradient: 'linear-gradient(135deg, #00B4D8 0%, #0077B6 100%)',
    industry: '通信设备/电信服务',
    cooperationYears: '3 年',
    status: '已签约',
    staffCount: '10,000+ 人',
    region: '深圳/南京/西安',
    score: 92,
    scoreColor: '#00BFFF',
    scoreLabel: '优秀',
    health: 'A 级',
    healthColor: '#34C759',
    healthLabel: '健康',
    rfm: [
      { label: 'R - 最近合作', value: '7 天前', percent: 85, color: '#34C759' },
      { label: 'F - 合作频次', value: '8 次/年', percent: 72, color: '#409EFF' },
      { label: 'M - 合作金额', value: '¥ 2.8 亿', percent: 78, color: '#5856D6' },
      { label: '满意度', value: '93%', percent: 93, color: '#34C759' },
      { label: '续约概率', value: '88%', percent: 88, color: '#E6A23C' },
    ],
    healthDimensions: [
      { name: '合作时长', score: 60 }, { name: '沟通频率', score: 85 },
      { name: '需求响应', score: 82 }, { name: '付款及时性', score: 90 },
      { name: '业务增长', score: 88 }, { name: '满意度', score: 93 },
    ],
    contacts: [
      { name: '赵明', initial: '赵', avatarBg: 'linear-gradient(135deg, #00B4D8, #0077B6)', position: '采购总监', role: '决策人', roleTag: 'danger', phone: '136****5001', email: 'zhaoming@zte.com.cn', address: '科技园总部 B 栋 20 楼', influence: '高', decision: '高' },
      { name: '钱华', initial: '钱', avatarBg: 'linear-gradient(135deg, #34C759, #00B4D8)', position: '项目经理', role: '主联系人', roleTag: 'info', phone: '136****5002', email: 'qianhua@zte.com.cn', address: '研发中心 3 号楼', influence: '中', decision: '中' },
      { name: '孙磊', initial: '孙', avatarBg: 'linear-gradient(135deg, #FF9500, #E6A23C)', position: '行政主管', role: '辅助', roleTag: '', phone: '136****5003', email: 'sunlei@zte.com.cn', influence: '低', decision: '低' },
    ],
    timeline: [
      { time: '2026-04-20', title: '🏆 续签 3 年框架协议', desc: '续签 3 年框架服务协议，合同金额 ¥ 2.8 亿，增加南京园区 5,000 m²' },
      { time: '2025-11-05', title: '🏢 入驻 5G 创新中心', desc: '中兴 5G 创新中心整体迁入园区 B 座 16-20 楼，规模 800 人' },
      { time: '2025-06-18', title: '⭐ 评级升级为钻石客户', desc: '合作金额突破 2 亿，连续 2 年零投诉，升级为钻石客户' },
      { time: '2024-08-22', title: '📋 首次签约（HX-2024-028）', desc: '中兴通讯签约入驻科技园 A 座 8-12 楼，面积 6,000 m²，签约 3 年' },
    ],
    needs: [
      { label: '主要需求', value: '研发办公 + 实验室 + 国际专线网络' },
      { label: '扩展需求', value: '西安园区分支机构场地' },
      { label: '投诉记录', value: '1 起（2025 年 3 月空调故障）', color: '#E6A23C' },
      { label: '表扬记录', value: '2 起（网络稳定性/安保服务）' },
      { label: '主要痛点', value: '国际专线带宽不足；实验室电力保障需升级；夜间加班通勤不便', full: true },
    ],
    aiSuggestions: [
      { tag: '续约预警', text: '框架协议剩余 18 个月，续约概率 88%，建议提前 1 年启动下一轮谈判', color: '#F56C6C', bg: 'linear-gradient(135deg, #fef0ef 0%, #fde2e2 100%)' },
      { tag: '交叉销售', text: '中兴正在扩张西安研究所，可推荐园区在西安的物业资源', color: '#409EFF', bg: 'linear-gradient(135deg, #ecf5ff 0%, #e8f4fd 100%)' },
      { tag: '关系维护', text: '关键决策人赵明季度需拜访一次，建议安排高管午餐会', color: '#E6A23C', bg: 'linear-gradient(135deg, #fef4e4 0%, #fdf0e0 100%)' },
      { tag: '增值服务', text: '实验室电力保障需求可转化为电力增容增值服务项目', color: '#34C759', bg: 'linear-gradient(135deg, #edf7ed 0%, #e8f5e9 100%)' },
    ],
  },

  byd: {
    name: '比亚迪股份有限公司',
    level: '黄金客户',
    levelColor: '#FFD700',
    gradient: 'linear-gradient(135deg, #34C759 0%, #28A745 100%)',
    industry: '汽车制造/新能源',
    cooperationYears: '2.5 年',
    status: '已签约',
    staffCount: '70,000+ 人',
    region: '深圳/全国',
    score: 88,
    scoreColor: '#85CE61',
    scoreLabel: '良好',
    health: 'B+ 级',
    healthColor: '#85CE61',
    healthLabel: '较健康',
    rfm: [
      { label: 'R - 最近合作', value: '15 天前', percent: 72, color: '#E6A23C' },
      { label: 'F - 合作频次', value: '6 次/年', percent: 60, color: '#409EFF' },
      { label: 'M - 合作金额', value: '¥ 1.6 亿', percent: 65, color: '#5856D6' },
      { label: '满意度', value: '88%', percent: 88, color: '#34C759' },
      { label: '续约概率', value: '78%', percent: 78, color: '#E6A23C' },
    ],
    healthDimensions: [
      { name: '合作时长', score: 50 }, { name: '沟通频率', score: 70 },
      { name: '需求响应', score: 80 }, { name: '付款及时性', score: 85 },
      { name: '业务增长', score: 95 }, { name: '满意度', score: 88 },
    ],
    contacts: [
      { name: '周鹏', initial: '周', avatarBg: 'linear-gradient(135deg, #34C759, #28A745)', position: '行政总经理', role: '决策人', roleTag: 'danger', phone: '135****8001', email: 'zhoupeng@byd.com', address: '坪山总部行政楼', influence: '高', decision: '高' },
      { name: '吴芳', initial: '吴', avatarBg: 'linear-gradient(135deg, #85CE61, #34C759)', position: '园区管理经理', role: '主联系人', roleTag: 'info', phone: '135****8002', email: 'wufang@byd.com', address: '研发中心 6 号楼', influence: '中', decision: '中' },
    ],
    timeline: [
      { time: '2026-02-28', title: '🏢 二期扩租：新增 4,200 m²', desc: '比亚迪新增 4,200 m² 用于新能源测试中心，累计租用面积 8,500 m²' },
      { time: '2025-10-15', title: '📋 首次签约入驻（HX-2025-045）', desc: '比亚迪签约入驻园区 C 座 1-5 楼，面积 4,300 m²，签约 3 年' },
      { time: '2025-07-01', title: '🤝 初次接触（招商对接）', desc: '招商团队与比亚迪行政总部首次接洽，沟通研发场地需求' },
      { time: '2025-03-20', title: '📊 参加园区新能源产业峰会', desc: '比亚迪受邀参加园区新能源产业峰会，达成初步合作意向' },
    ],
    needs: [
      { label: '主要需求', value: '研发测试场地 + 重载电力 + 仓储空间' },
      { label: '扩展需求', value: '新能源测试中心专用场地 + 员工公寓' },
      { label: '投诉记录', value: '0 起' },
      { label: '表扬记录', value: '1 起（入驻服务效率高）' },
      { label: '主要痛点', value: '电力容量不足以满足测试需求；楼层承重需加固；货运电梯不足', full: true },
    ],
    aiSuggestions: [
      { tag: '续约预警', text: '续约概率 78%，客户为快速发展期大客户，建议提前 8 个月启动续约', color: '#F56C6C', bg: 'linear-gradient(135deg, #fef0ef 0%, #fde2e2 100%)' },
      { tag: '交叉销售', text: '比亚迪员工超 70 万人，公寓需求旺盛，可推荐人才公寓批量租赁', color: '#409EFF', bg: 'linear-gradient(135deg, #ecf5ff 0%, #e8f4fd 100%)' },
      { tag: '关系维护', text: '企业决策链较长，建议建立多层级对接机制，覆盖行政/技术/财务', color: '#E6A23C', bg: 'linear-gradient(135deg, #fef4e4 0%, #fdf0e0 100%)' },
      { tag: '风险预警', text: '电力容量和承重问题若未解决，可能影响二期续约决策，需尽快协调工程改造', color: '#F56C6C', bg: 'linear-gradient(135deg, #fef0ef 0%, #fde2e2 100%)' },
    ],
  },

  catl: {
    name: '宁德时代新能源科技股份有限公司',
    level: '白金客户',
    levelColor: '#C0C0C0',
    gradient: 'linear-gradient(135deg, #FF9500 0%, #FF6B00 100%)',
    industry: '新能源/动力电池',
    cooperationYears: '2 年',
    status: '已签约',
    staffCount: '30,000+ 人',
    region: '宁德/上海/全国',
    score: 95,
    scoreColor: '#FF9500',
    scoreLabel: '优秀',
    health: 'A 级',
    healthColor: '#34C759',
    healthLabel: '健康',
    rfm: [
      { label: 'R - 最近合作', value: '5 天前', percent: 92, color: '#34C759' },
      { label: 'F - 合作频次', value: '10 次/年', percent: 80, color: '#409EFF' },
      { label: 'M - 合作金额', value: '¥ 3.5 亿', percent: 88, color: '#5856D6' },
      { label: '满意度', value: '95%', percent: 95, color: '#34C759' },
      { label: '续约概率', value: '90%', percent: 90, color: '#E6A23C' },
    ],
    healthDimensions: [
      { name: '合作时长', score: 40 }, { name: '沟通频率', score: 88 },
      { name: '需求响应', score: 92 }, { name: '付款及时性', score: 95 },
      { name: '业务增长', score: 96 }, { name: '满意度', score: 95 },
    ],
    contacts: [
      { name: '陈伟', initial: '陈', avatarBg: 'linear-gradient(135deg, #FF9500, #FF6B00)', position: '采购副总裁', role: '决策人', roleTag: 'danger', phone: '137****2001', email: 'chenwei@catl.com', address: '宁德总部大厦 25 楼', influence: '高', decision: '高' },
      { name: '林芳', initial: '林', avatarBg: 'linear-gradient(135deg, #34C759, #FF9500)', position: '园区运营总监', role: '主联系人', roleTag: 'info', phone: '137****2002', email: 'linfang@catl.com', address: '研发中心 A 座 12 楼', influence: '高', decision: '中' },
      { name: '黄磊', initial: '黄', avatarBg: 'linear-gradient(135deg, #5856D6, #409EFF)', position: '法务经理', role: '辅助', roleTag: '', phone: '137****2003', email: 'huanglei@catl.com', influence: '中', decision: '低' },
    ],
    timeline: [
      { time: '2026-05-10', title: '🏆 续签战略合作协议（3 年）', desc: '续签 3 年战略合作协议，金额 ¥ 3.5 亿，含上海研发中心场地' },
      { time: '2026-01-15', title: '⭐ 评级升级为白金客户', desc: '合作金额突破 3 亿，升级为白金客户，专享 VIP 通道服务' },
      { time: '2025-09-01', title: '🏢 入驻上海创新中心', desc: '宁德时代上海创新中心入驻，租用 8,000 m² 办公楼 + 实验室' },
      { time: '2025-04-10', title: '📋 首次签约（HX-2025-018）', desc: '宁德时代签约入驻园区，租用宁德总部周边 6,000 m² 研发场地' },
      { time: '2025-01-08', title: '🤝 初次接触（招商对接）', desc: '园区招商团队拜访宁德时代总部，双方达成合作意向' },
    ],
    needs: [
      { label: '主要需求', value: '研发办公 + 电池实验室 + 物流仓储' },
      { label: '扩展需求', value: '上海/成都/德国海外办公室网络' },
      { label: '投诉记录', value: '0 起' },
      { label: '表扬记录', value: '4 起（设施品质/环境/物业服务/响应速度）' },
      { label: '主要痛点', value: '实验室废气处理需改造；危险品暂存区需扩建；海外扩张需一站式办公方案', full: true },
    ],
    aiSuggestions: [
      { tag: '续约预警', text: '续约概率 90%，客户忠诚度高，建议维持现有服务水平确保续约', color: '#34C759', bg: 'linear-gradient(135deg, #edf7ed 0%, #e8f5e9 100%)' },
      { tag: '交叉销售', text: '成都、德国办公室需求可打包为跨区域办公解决方案，提升客单价', color: '#409EFF', bg: 'linear-gradient(135deg, #ecf5ff 0%, #e8f4fd 100%)' },
      { tag: '关系维护', text: '决策人陈伟生日 8 月 15 日，可安排专属生日礼遇 + 高管晚宴', color: '#E6A23C', bg: 'linear-gradient(135deg, #fef4e4 0%, #fdf0e0 100%)' },
      { tag: '增值服务', text: '实验室废气处理可协调园区环保改造，转化为绿色园区增值方案', color: '#5856D6', bg: 'linear-gradient(135deg, #f0efff 0%, #e8e5ff 100%)' },
    ],
  },

  nio: {
    name: '蔚来汽车',
    level: '潜力客户',
    levelColor: '#AF52DE',
    gradient: 'linear-gradient(135deg, #AF52DE 0%, #5856D6 100%)',
    industry: '新能源汽车/智能出行',
    cooperationYears: '1 年',
    status: '试用期',
    staffCount: '1,500+ 人',
    region: '上海/北京/合肥',
    score: 78,
    scoreColor: '#AF52DE',
    scoreLabel: '尚可',
    health: 'B 级',
    healthColor: '#E6A23C',
    healthLabel: '发展中',
    rfm: [
      { label: 'R - 最近合作', value: '20 天前', percent: 60, color: '#E6A23C' },
      { label: 'F - 合作频次', value: '3 次/年', percent: 40, color: '#409EFF' },
      { label: 'M - 合作金额', value: '¥ 3,200 万', percent: 35, color: '#5856D6' },
      { label: '满意度', value: '82%', percent: 82, color: '#34C759' },
      { label: '续约概率', value: '65%', percent: 65, color: '#E6A23C' },
    ],
    healthDimensions: [
      { name: '合作时长', score: 20 }, { name: '沟通频率', score: 65 },
      { name: '需求响应', score: 85 }, { name: '付款及时性', score: 70 },
      { name: '业务增长', score: 80 }, { name: '满意度', score: 82 },
    ],
    contacts: [
      { name: '何欣', initial: '何', avatarBg: 'linear-gradient(135deg, #AF52DE, #5856D6)', position: '运营VP', role: '决策人', roleTag: 'danger', phone: '138****3001', email: 'hexin@nio.com', address: '上海总部 18 楼', influence: '高', decision: '高' },
      { name: '张雅', initial: '张', avatarBg: 'linear-gradient(135deg, #409EFF, #AF52DE)', position: '办公选址经理', role: '主联系人', roleTag: 'info', phone: '138****3002', email: 'zhangya@nio.com', influence: '中', decision: '中' },
    ],
    timeline: [
      { time: '2026-03-10', title: '📋 首次签约入驻（测试期）', desc: '蔚来签约入驻园区 D 座 16-18 楼，面积 1,800 m²，试签约 1 年' },
      { time: '2025-12-20', title: '🤝 初次接触（招商对接）', desc: '蔚来运营团队考察园区，对智能网联办公环境感兴趣' },
      { time: '2025-10-08', title: '📊 参加园区未来出行论坛', desc: '蔚来受邀参加园区未来出行产业论坛，建立合作关系' },
    ],
    needs: [
      { label: '主要需求', value: '智能办公空间 + 展示体验中心 + 充电设施' },
      { label: '扩展需求', value: '换电站示范点 + 用户社区空间' },
      { label: '投诉记录', value: '1 起（2026 年 4 月停车位不足）', color: '#E6A23C' },
      { label: '表扬记录', value: '1 起（装修施工协调到位）' },
      { label: '主要痛点', value: '充电桩数量不足；展示体验中心楼层承重限制；试用期团队规模扩张快', full: true },
    ],
    aiSuggestions: [
      { tag: '续约预警', text: '续约概率仅 65%，试用期客户流失风险较高，建议加强服务体验', color: '#F56C6C', bg: 'linear-gradient(135deg, #fef0ef 0%, #fde2e2 100%)' },
      { tag: '交叉销售', text: '蔚来用户社区空间需求可转化为园区活动场地租赁服务', color: '#409EFF', bg: 'linear-gradient(135deg, #ecf5ff 0%, #e8f4fd 100%)' },
      { tag: '关系维护', text: '客户尚在试用期，建议每月安排一次使用体验回访，快速响应需求', color: '#E6A23C', bg: 'linear-gradient(135deg, #fef4e4 0%, #fdf0e0 100%)' },
      { tag: '增值服务', text: '充电设施可联合新能源企业投建，转化为园区充电网络增值服务', color: '#AF52DE', bg: 'linear-gradient(135deg, #f5eaff 0%, #efe5ff 100%)' },
    ],
  },
}

// 默认 Profile（当所选企业不匹配任何 Mock 时的降级方案）
const FALLBACK_PROFILE: CustomerProfile = {
  name: '企业客户',
  level: '标准客户',
  levelColor: '#909399',
  gradient: 'linear-gradient(135deg, #909399 0%, #606266 100%)',
  industry: '未分类',
  cooperationYears: '—',
  status: '待确认',
  staffCount: '—',
  region: '—',
  score: 0,
  scoreColor: '#909399',
  scoreLabel: '待评估',
  health: '待评估',
  healthColor: '#909399',
  healthLabel: '—',
  rfm: [
    { label: 'R - 最近合作', value: '暂无数据', percent: 0, color: '#909399' },
    { label: 'F - 合作频次', value: '暂无数据', percent: 0, color: '#909399' },
    { label: 'M - 合作金额', value: '暂无数据', percent: 0, color: '#909399' },
    { label: '满意度', value: '暂无数据', percent: 0, color: '#909399' },
    { label: '续约概率', value: '暂无数据', percent: 0, color: '#909399' },
  ],
  healthDimensions: [
    { name: '合作时长', score: 0 }, { name: '沟通频率', score: 0 },
    { name: '需求响应', score: 0 }, { name: '付款及时性', score: 0 },
    { name: '业务增长', score: 0 }, { name: '满意度', score: 0 },
  ],
  contacts: [],
  timeline: [],
  needs: [
    { label: '主要需求', value: '待完善', color: '#909399' },
    { label: '扩展需求', value: '待完善', color: '#909399' },
    { label: '投诉记录', value: '—', color: '#909399' },
    { label: '表扬记录', value: '—', color: '#909399' },
    { label: '主要痛点', value: '暂无足够数据生成画像，建议补充企业档案信息', full: true },
  ],
  aiSuggestions: [
    { tag: '数据完善', text: '企业基础档案信息不完整，建议先补充联系人/合同/服务记录', color: '#909399', bg: 'linear-gradient(135deg, #f5f5f5 0%, #f0f0f0 100%)' },
    { tag: '初次接触', text: '该企业的客户画像尚未建立，建议安排首次对接并采集需求', color: '#909399', bg: 'linear-gradient(135deg, #f5f5f5 0%, #f0f0f0 100%)' },
    { tag: '数据采集', text: '可在企业详情页完善合作协议/联系人/服务记录等数据', color: '#909399', bg: 'linear-gradient(135deg, #f5f5f5 0%, #f0f0f0 100%)' },
    { tag: '系统提示', text: '客户画像将在数据积累后自动生成，当前数据量不足以分析', color: '#909399', bg: 'linear-gradient(135deg, #f5f5f5 0%, #f0f0f0 100%)' },
  ],
}

// ==================== Profile 匹配逻辑 ====================
const profileKeywords = [
  { key: '华为', profile: MOCK_PROFILES.huawei },
  { key: '中兴', profile: MOCK_PROFILES.zte },
  { key: '比亚迪', profile: MOCK_PROFILES.byd },
  { key: '宁德时代', profile: MOCK_PROFILES.catl },
  { key: '蔚来', profile: MOCK_PROFILES.nio },
]

function findProfileForEnterprise(name?: string): CustomerProfile {
  if (!name) return FALLBACK_PROFILE
  for (const entry of profileKeywords) {
    if (name.includes(entry.key)) return entry.profile
  }
  return FALLBACK_PROFILE
}

const profile = computed<CustomerProfile>(() => {
  return findProfileForEnterprise(currentEnterprise.value?.name)
})

// ==================== 生命周期 ====================
onMounted(async () => {
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

/* 过渡动画 */
.profile-fade-enter-active,
.profile-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.profile-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.profile-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.profile-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 顶部画像卡 */
.profile-header {
  color: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
}
.profile-name { font-size: 20px; font-weight: 600; margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.level-tag { font-weight: 600; font-size: 11px; }
.profile-meta { font-size: 13px; opacity: 0.85; display: flex; gap: 16px; flex-wrap: wrap; }
.profile-meta span { background: rgba(255,255,255,0.15); padding: 4px 10px; border-radius: 12px; }
.score-block { text-align: center; padding: 0 24px; border-left: 1px solid rgba(255,255,255,0.3); }
.health-block { text-align: center; padding-left: 24px; border-left: 1px solid rgba(255,255,255,0.3); padding-right: 8px; }
.score-label { font-size: 11px; opacity: 0.8; }
.score-value { font-size: 36px; font-weight: 700; line-height: 1.2; }
.score-sub { font-size: 11px; opacity: 0.8; }
.health-grade { font-size: 16px; font-weight: 600; }

/* RFM 5 列网格 */
.rfm-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.rfm-item { text-align: center; padding: 8px 4px; }
.rfm-label { font-size: 11px; color: #909399; margin-bottom: 6px; white-space: nowrap; }
.rfm-value { font-size: 20px; font-weight: 700; margin-bottom: 6px; }
.rfm-bar { height: 4px; background: #f0f0f0; border-radius: 2px; overflow: hidden; }
.rfm-fill { height: 100%; border-radius: 2px; transition: width 0.5s ease; }

/* 双栏布局 */
.dual-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

/* 健康度 */
.radar-row { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.radar-name { width: 90px; font-size: 12px; color: #606266; flex-shrink: 0; }
.radar-track { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.radar-fill { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.radar-score { width: 36px; font-size: 13px; font-weight: 600; text-align: right; font-family: monospace; }

/* 联系人 */
.contact-block { margin-bottom: 12px; padding: 12px; background: #f5f7fa; border-radius: 8px; }
.contact-block:last-child { margin-bottom: 0; }
.contact-row { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.contact-info { flex: 1; }
.contact-name { font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 6px; }
.contact-detail { font-size: 12px; color: #909399; margin-top: 2px; }
.contact-ext { font-size: 12px; color: #606266; line-height: 1.6; margin-top: 6px; padding-left: 46px; }
.contact-ext .contact-email { color: #409EFF; }

/* 时间轴 */
.timeline { position: relative; padding-left: 20px; }
.timeline::before { content: ''; position: absolute; left: 6px; top: 8px; bottom: 8px; width: 2px; background: #e4e7ed; }
.timeline-item { position: relative; padding-bottom: 18px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-item::before {
  content: ''; position: absolute; left: -16px; top: 6px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #409EFF; border: 2px solid #fff; box-shadow: 0 0 0 2px #ecf5ff;
}
.tl-time { font-size: 11px; color: #909399; margin-bottom: 2px; }
.tl-title { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.tl-desc { font-size: 12px; color: #909399; line-height: 1.5; }

/* 需求摘要 */
.descriptions { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.desc-item { padding: 8px 12px; background: #f5f7fa; border-radius: 8px; }
.desc-full { grid-column: 1 / -1; }
.desc-label { font-size: 11px; color: #909399; margin-bottom: 4px; }
.desc-value { font-size: 13px; color: #303133; line-height: 1.5; }

/* AI 建议 */
.ai-card { padding: 14px; border-radius: 8px; border: 1px solid #e4e7ed; border-left: 4px solid; margin-bottom: 10px; }
.ai-card:last-child { margin-bottom: 0; }
.ai-tag { font-size: 11px; font-weight: 600; margin-bottom: 6px; display: inline-block; padding: 2px 8px; border-radius: 4px; }
.ai-text { font-size: 12px; color: #606266; line-height: 1.6; }

.section-card { border: 1px solid #e4e7ed; border-radius: 10px; }
.section-card :deep(.el-card__header) { padding: 12px 16px; font-size: 14px; border-bottom: 1px solid #f0f0f0; }

/* 页脚 */
.footer-note { text-align: center; color: #c0c4cc; font-size: 11px; padding: 16px 0 8px; }
</style>