<template>
  <div class="page-container">
    <!-- Breadcrumb -->
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>评分规则</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- Header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">评分规则</h2>
        <p class="page-desc">配置企业综合评分规则，支持多维度评分体系，自动计算企业评分</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleReset">恢复默认</el-button>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </div>
    </div>

    <!-- Overview Cards -->
    <el-row :gutter="12" class="overview-row">
      <el-col :span="6" v-for="s in overviewStats" :key="s.label">
        <el-card shadow="never" class="overview-card">
          <div class="overview-icon" :style="{ background: s.bg, color: s.color }">{{ s.icon }}</div>
          <div class="overview-info">
            <div class="overview-num">{{ s.value }}</div>
            <div class="overview-lbl">{{ s.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="main-tabs">
      <!-- ═══ Tab: 评分规则 ═══ -->
      <el-tab-pane label="📋 评分规则" name="rules">
        <div class="rules-toolbar">
          <div class="toolbar-left">
            <el-select v-model="dimFilter" placeholder="全部维度" size="small" style="width:130px">
              <el-option label="全部维度" value="" />
              <el-option v-for="d in dimensions" :key="d.key" :label="d.name" :value="d.key" />
            </el-select>
            <el-input v-model="ruleSearch" placeholder="搜索规则名称..." size="small" style="width:200px" clearable />
          </div>
          <el-button size="small" type="primary" @click="showAddDimensionDialog">+ 新增维度</el-button>
        </div>

        <!-- Dimension Cards -->
        <div v-for="dim in filteredDimensions" :key="dim.key" class="dim-card">
          <div class="dim-header" @click="dim.collapsed = !dim.collapsed">
            <div class="dim-header-left">
              <span class="dim-name">{{ dim.icon }} {{ dim.name }}</span>
              <el-tag size="small" type="info" effect="plain">权重 {{ dim.weight }}%</el-tag>
              <el-tag v-if="dim.isDeduct" size="small" type="danger" effect="plain" style="margin-left:4px">扣分项</el-tag>
            </div>
            <div class="dim-header-right">
              <span class="dim-rule-count">{{ dim.rules.length }} 条规则</span>
              <el-button size="small" text @click.stop="showEditDimensionDialog(dim)">✏️</el-button>
              <el-icon class="toggle-icon" :class="{ open: !dim.collapsed }"><ArrowDown /></el-icon>
            </div>
          </div>
          <div v-show="!dim.collapsed" class="dim-body">
            <el-table :data="dim.rules" border size="small" max-height="360" empty-text="暂无评分规则">
              <el-table-column type="index" label="#" width="45" />
              <el-table-column prop="name" label="规则名称" min-width="140" />
              <el-table-column label="适用条件" min-width="160">
                <template #default="scope">{{ scope.row.condition }}</template>
              </el-table-column>
              <el-table-column prop="score" label="分值" width="80" align="center">
                <template #default="scope">
                  <span :class="['score-val', scope.row.score >= 0 ? 'positive' : 'negative']">
                    {{ scope.row.score >= 0 ? '+' : '' }}{{ scope.row.score }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.score >= 0 ? 'success' : 'danger'" size="small" effect="plain">
                    {{ scope.row.score >= 0 ? '加分' : '扣分' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="65" align="center">
                <template #default="scope">
                  <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" fixed="right">
                <template #default="scope">
                  <el-button size="small" text type="primary" @click="showEditRuleDialog(dim, scope.$index)">编辑</el-button>
                  <el-popconfirm title="确认删除该规则？" @confirm="deleteRule(dim, scope.$index)">
                    <template #reference>
                      <el-button size="small" text type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <div class="add-rule-row">
              <el-button size="small" type="primary" plain @click="showAddRuleDialog(dim)">+ 添加规则</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ═══ Tab: 评分历史 ═══ -->
      <el-tab-pane label="📈 评分历史" name="history">
        <el-card shadow="never">
          <div class="history-header">
            <strong>企业评分分布</strong>
            <div class="history-filters">
              <el-select v-model="historyIndustry" size="small" style="width:130px">
                <el-option label="全部行业" value="" />
                <el-option label="信息技术" value="IT" />
                <el-option label="房地产" value="RE" />
                <el-option label="能源环保" value="ENV" />
                <el-option label="生物医药" value="MED" />
              </el-select>
              <el-select v-model="historyPeriod" size="small" style="width:110px">
                <el-option label="本月" value="month" />
                <el-option label="本季度" value="quarter" />
                <el-option label="本年" value="year" />
              </el-select>
            </div>
          </div>

          <!-- Bar Chart -->
          <div class="bar-chart-wrap">
            <div v-for="b in scoreBuckets" :key="b.label" class="bar-col">
              <span class="bar-val">{{ b.count }}</span>
              <div class="bar" :style="{ height: b.height + '%', background: b.color, opacity: b.opacity }" />
              <span class="bar-label">{{ b.label }}</span>
            </div>
          </div>

          <!-- Stats -->
          <div class="history-stats">
            <span>共 <strong>{{ historyStats.total }}</strong> 家企业已评分</span>
            <span>平均分：<strong>{{ historyStats.avg }}</strong></span>
            <span>最高分：<strong>{{ historyStats.max }}</strong></span>
            <span>最低分：<strong>{{ historyStats.min }}</strong></span>
          </div>

          <!-- Recent Changes -->
          <div class="recent-changes">
            <div class="section-title">最近评分变动</div>
            <el-table :data="recentChanges" border size="small" max-height="280">
              <el-table-column prop="enterprise" label="企业名称" min-width="160" />
              <el-table-column prop="oldScore" label="原评分" width="80" align="center" />
              <el-table-column prop="newScore" label="新评分" width="80" align="center" />
              <el-table-column label="变动" width="80" align="center">
                <template #default="scope">
                  <span :style="{ color: scope.row.delta > 0 ? '#34C759' : scope.row.delta < 0 ? '#F56C6C' : '#909399', fontWeight: 600 }">
                    {{ scope.row.delta > 0 ? '↑' : scope.row.delta < 0 ? '↓' : '—' }} {{ scope.row.delta > 0 ? '+' : '' }}{{ scope.row.delta.toFixed(1) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="原因" min-width="140" />
              <el-table-column prop="time" label="时间" width="100" />
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ═══ Tab: 全局设置 ═══ -->
      <el-tab-pane label="⚙️ 全局设置" name="settings">
        <el-card shadow="never">
          <template #header><strong>评分全局设置</strong></template>
          <el-form :model="settingsForm" label-width="110px" class="settings-form">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="评分满分">
                  <el-input-number v-model="settingsForm.fullScore" :min="10" :max="500" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="及格线">
                  <el-input-number v-model="settingsForm.passScore" :min="0" :max="settingsForm.fullScore" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="评分周期">
                  <el-select v-model="settingsForm.period" style="width:100%">
                    <el-option label="实时" value="real" />
                    <el-option label="每日" value="daily" />
                    <el-option label="每周" value="weekly" />
                    <el-option label="每月" value="monthly" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="计算方式">
                  <el-select v-model="settingsForm.method" style="width:100%">
                    <el-option label="加权平均" value="weighted" />
                    <el-option label="简单平均" value="simple" />
                    <el-option label="最高分优先" value="max" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="等级划分">
              <div class="grade-row">
                <div v-for="g in settingsForm.grades" :key="g.label" class="grade-item">
                  <span class="grade-tag" :style="{ background: g.color + '20', color: g.color, border: '1px solid ' + g.color + '40' }">{{ g.label }}</span>
                  <span class="grade-text">{{ g.text }}</span>
                  <el-input-number v-model="g.threshold" :min="0" :max="settingsForm.fullScore" size="small" controls-position="right" style="width:80px" />
                  <span class="grade-text">分以上</span>
                </div>
              </div>
            </el-form-item>
            <el-divider />
            <el-form-item label="自动评分">
              <div class="toggle-with-label">
                <el-switch v-model="settingsForm.autoScore" />
                <span class="toggle-hint">启用自动评分（每日凌晨 2:00 执行）</span>
              </div>
            </el-form-item>
            <el-form-item label="通知设置">
              <div class="notify-row">
                <el-checkbox v-model="settingsForm.notifyEmail" label="邮件通知" border size="small" />
                <el-checkbox v-model="settingsForm.notifySms" label="短信通知" border size="small" />
                <el-checkbox v-model="settingsForm.notifySite" label="站内信" border size="small" />
                <span class="toggle-hint" style="margin-left:8px">评分变动超过 {{ settingsForm.notifyThreshold }} 分时发送</span>
                <el-input-number v-model="settingsForm.notifyThreshold" :min="1" :max="50" size="small" controls-position="right" style="width:80px;margin-left:4px" />
              </div>
            </el-form-item>
            <div class="settings-footer">
              <el-button @click="handleSettingsReset">取消</el-button>
              <el-button type="primary" @click="handleSettingsSave">保存设置</el-button>
            </div>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- ═══ Dialog: Add/Edit Rule ═══ -->
    <el-dialog v-model="ruleDialog.visible" :title="ruleDialog.isEdit ? '编辑规则' : '新增规则'" width="520px" top="15vh" :close-on-click-modal="false">
      <el-form :model="ruleDialog.form" label-width="90px">
        <el-form-item label="所属维度" v-if="!ruleDialog.isEdit">
          <el-select v-model="ruleDialog.form.dimKey" placeholder="选择维度" style="width:100%">
            <el-option v-for="d in dimensions" :key="d.key" :label="d.name" :value="d.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属维度" v-else>
          <el-input :model-value="ruleDimName" disabled />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则名称">
              <el-input v-model="ruleDialog.form.name" placeholder="如：注册资本" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用条件">
              <el-input v-model="ruleDialog.form.condition" placeholder="如：≥ 1000万" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分值">
              <el-input-number v-model="ruleDialog.form.score" :min="-100" :max="100" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="ruleDialog.form.type" style="width:100%">
                <el-option label="加分" :value="1" />
                <el-option label="扣分" :value="-1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-switch v-model="ruleDialog.form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmRule">确定</el-button>
      </template>
    </el-dialog>

    <!-- ═══ Dialog: Add/Edit Dimension ═══ -->
    <el-dialog v-model="dimDialog.visible" :title="dimDialog.isEdit ? '编辑评分维度' : '新增评分维度'" width="480px" top="18vh" :close-on-click-modal="false">
      <el-form :model="dimDialog.form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="维度名称">
              <el-input v-model="dimDialog.form.name" placeholder="如：企业基础" maxlength="16" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图标">
              <el-input v-model="dimDialog.form.icon" placeholder="如：🏛️" maxlength="4" style="width:80px" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="权重 (%)">
              <el-input-number v-model="dimDialog.form.weight" :min="0" :max="100" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="dimDialog.form.sortOrder" :min="0" :max="99" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="维度描述">
          <el-input v-model="dimDialog.form.desc" type="textarea" :rows="3" placeholder="维度说明" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dimDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmDimension">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

// ─── Types ───
interface Rule {
  name: string
  condition: string
  score: number
  status: number
}
interface Dimension {
  key: string
  icon: string
  name: string
  weight: number
  desc: string
  isDeduct: boolean
  collapsed: boolean
  rules: Rule[]
}

// ─── Tab ───
const activeTab = ref('rules')
const dimFilter = ref('')
const ruleSearch = ref('')

const filteredDimensions = computed(() => {
  let list = dimensions
  if (dimFilter.value) {
    list = list.filter(d => d.key === dimFilter.value)
  }
  if (ruleSearch.value) {
    const q = ruleSearch.value.toLowerCase()
    list = list.map(d => ({
      ...d,
      rules: d.rules.filter(r => r.name.toLowerCase().includes(q))
    })).filter(d => d.rules.length > 0)
  }
  return list
})

// ─── 5 Dimensions × 24 Rules ───
const dimensions = reactive<Dimension[]>([
  {
    key: 'basic', icon: '🏛️', name: '企业基础', weight: 25, desc: '企业基本信息相关评分，包括注册资本、成立年限、企业规模等',
    isDeduct: false, collapsed: false,
    rules: [
      { name: '注册资本', condition: '≥ 1000万', score: 5, status: 1 },
      { name: '注册资本', condition: '≥ 500万 且 < 1000万', score: 3, status: 1 },
      { name: '成立年限', condition: '≥ 3年', score: 5, status: 1 },
      { name: '企业规模', condition: '大型企业', score: 5, status: 1 },
      { name: '行业地位', condition: '行业领先', score: 5, status: 1 },
      { name: '资质等级', condition: '一级资质', score: 2, status: 1 },
    ]
  },
  {
    key: 'operation', icon: '📈', name: '经营状况', weight: 30, desc: '根据企业经营数据评分，包括营收、利润、资产等指标',
    isDeduct: false, collapsed: false,
    rules: [
      { name: '年营收', condition: '≥ 1亿', score: 10, status: 1 },
      { name: '增长率', condition: '≥ 20%', score: 8, status: 1 },
      { name: '净利润', condition: '盈利', score: 6, status: 1 },
      { name: '资产负债率', condition: '< 50%', score: 3, status: 1 },
      { name: '现金流', condition: '正向', score: 3, status: 1 },
    ]
  },
  {
    key: 'innovation', icon: '💡', name: '创新能力', weight: 20, desc: '根据企业创新指标评分，包括专利、高新技术企业等',
    isDeduct: false, collapsed: false,
    rules: [
      { name: '专利数量', condition: '≥ 10 项', score: 6, status: 1 },
      { name: '高新技术企业', condition: '已认定', score: 5, status: 1 },
      { name: '研发投入', condition: '≥ 5%', score: 4, status: 1 },
      { name: '专精特新', condition: '已认定', score: 3, status: 1 },
      { name: '知识产权', condition: '有登记', score: 2, status: 1 },
    ]
  },
  {
    key: 'credit', icon: '🛡️', name: '信用风险', weight: 15, desc: '根据企业信用风险记录扣分，包括经营异常、行政处罚等',
    isDeduct: true, collapsed: false,
    rules: [
      { name: '经营异常', condition: '有记录', score: -10, status: 1 },
      { name: '行政处罚', condition: '有记录', score: -5, status: 1 },
      { name: '司法诉讼', condition: '作为被告 ≥ 3 起', score: -8, status: 1 },
      { name: '失信被执行', condition: '有记录', score: -15, status: 1 },
    ]
  },
  {
    key: 'social', icon: '🤝', name: '社会贡献', weight: 10, desc: '根据企业社会责任表现评分，包括就业、纳税、ESG等',
    isDeduct: false, collapsed: false,
    rules: [
      { name: '就业人数', condition: '≥ 100 人', score: 3, status: 1 },
      { name: '纳税信用', condition: 'A 级', score: 3, status: 1 },
      { name: '公益捐赠', condition: '有记录', score: 2, status: 1 },
      { name: 'ESG 评级', condition: 'A 级以上', score: 2, status: 1 },
    ]
  },
])

// ─── Overview Stats ───
const totalRules = computed(() => dimensions.reduce((s, d) => s + d.rules.length, 0))

const overviewStats = computed(() => [
  { icon: '📐', value: dimensions.length.toString(), label: '评分维度', bg: '#ecf5ff', color: '#409EFF' },
  { icon: '📋', value: totalRules.value.toString(), label: '评分规则', bg: '#f0f9eb', color: '#34C759' },
  { icon: '⚙️', value: '100', label: '满分', bg: '#fdf6ec', color: '#E6A23C' },
  { icon: '🔄', value: '每日', label: '评分周期', bg: '#f5f7fa', color: '#909399' },
])

// ─── Rule Dialog ───
const ruleDimName = ref('')

const ruleDialog = reactive({
  visible: false,
  isEdit: false,
  editingDim: null as Dimension | null,
  editingIndex: -1,
  form: { dimKey: '', name: '', condition: '', score: 0, type: 1, status: 1 },
})

function showAddRuleDialog(dim: Dimension) {
  ruleDialog.isEdit = false
  ruleDialog.editingDim = null
  ruleDialog.editingIndex = -1
  ruleDialog.form = { dimKey: dim.key, name: '', condition: '', score: 0, type: dim.isDeduct ? -1 : 1, status: 1 }
  ruleDialog.visible = true
}

function showEditRuleDialog(dim: Dimension, index: number) {
  const rule = dim.rules[index]
  ruleDialog.isEdit = true
  ruleDialog.editingDim = dim
  ruleDialog.editingIndex = index
  ruleDimName.value = dim.name
  ruleDialog.form = { dimKey: dim.key, name: rule.name, condition: rule.condition, score: rule.score, type: rule.score >= 0 ? 1 : -1, status: rule.status }
  ruleDialog.visible = true
}

function confirmRule() {
  const f = ruleDialog.form
  if (!f.name || !f.condition) {
    ElMessage.warning('请填写规则名称和适用条件')
    return
  }
  const finalScore = f.type === -1 ? -Math.abs(f.score) : Math.abs(f.score)
  const rule: Rule = { name: f.name, condition: f.condition, score: f.type === -1 && f.score >= 0 ? -f.score : f.score, status: f.status }
  if (ruleDialog.isEdit && ruleDialog.editingDim) {
    Object.assign(ruleDialog.editingDim.rules[ruleDialog.editingIndex], rule)
    ElMessage.success('规则已更新')
  } else {
    const targetDim = dimensions.find(d => d.key === f.dimKey)
    if (targetDim) targetDim.rules.push(rule)
    ElMessage.success('规则已添加')
  }
  ruleDialog.visible = false
}

function deleteRule(dim: Dimension, index: number) {
  dim.rules.splice(index, 1)
  ElMessage.success('规则已删除')
}

// ─── Dimension Dialog ───
const dimDialog = reactive({
  visible: false,
  isEdit: false,
  editingKey: '',
  form: { name: '', icon: '', weight: 10, sortOrder: 0, desc: '' },
})

function showAddDimensionDialog() {
  dimDialog.isEdit = false
  dimDialog.editingKey = ''
  dimDialog.form = { name: '', icon: '📦', weight: 10, sortOrder: dimensions.length + 1, desc: '' }
  dimDialog.visible = true
}

function showEditDimensionDialog(dim: Dimension) {
  dimDialog.isEdit = true
  dimDialog.editingKey = dim.key
  dimDialog.form = { name: dim.name, icon: dim.icon, weight: dim.weight, sortOrder: dimensions.indexOf(dim) + 1, desc: dim.desc }
  dimDialog.visible = true
}

function confirmDimension() {
  const f = dimDialog.form
  if (!f.name) {
    ElMessage.warning('请填写维度名称')
    return
  }
  if (dimDialog.isEdit) {
    const dim = dimensions.find(d => d.key === dimDialog.editingKey)
    if (dim) {
      dim.name = f.name
      dim.icon = f.icon || '📦'
      dim.weight = f.weight
      dim.desc = f.desc
    }
    ElMessage.success('维度已更新')
  } else {
    const key = 'dim_' + Date.now().toString(36)
    dimensions.push({
      key, icon: f.icon || '📦', name: f.name, weight: f.weight, desc: f.desc,
      isDeduct: false, collapsed: false, rules: [],
    })
    ElMessage.success('新维度已添加')
  }
  dimDialog.visible = false
}

// ─── Score History Tab ───
const historyIndustry = ref('')
const historyPeriod = ref('year')

const scoreBuckets = [
  { label: '0-59', count: 35, height: 35, color: '#409EFF', opacity: 0.6 },
  { label: '60-69', count: 68, height: 55, color: '#409EFF', opacity: 0.7 },
  { label: '70-79', count: 156, height: 80, color: '#409EFF', opacity: 0.85 },
  { label: '80-89', count: 234, height: 100, color: '#409EFF', opacity: 1 },
  { label: '90-100', count: 89, height: 60, color: '#34C759', opacity: 1 },
]

const historyStats = reactive({ total: 582, avg: '67.3', max: '96.5', min: '8.0' })

const recentChanges = reactive([
  { enterprise: '华为投资控股', oldScore: '78.5', newScore: '82.3', delta: 3.8, reason: '新增专利 12 项', time: '2026-07-02' },
  { enterprise: '腾讯科技', oldScore: '85.0', newScore: '85.0', delta: 0, reason: '无变动', time: '2026-07-02' },
  { enterprise: '某问题企业', oldScore: '45.0', newScore: '32.0', delta: -13.0, reason: '经营异常 + 行政处罚', time: '2026-07-01' },
  { enterprise: '阿里巴巴集团', oldScore: '92.0', newScore: '94.5', delta: 2.5, reason: '研发投入增长', time: '2026-07-01' },
  { enterprise: '比亚迪股份', oldScore: '71.0', newScore: '76.0', delta: 5.0, reason: '营收增长 + 新专利', time: '2026-06-30' },
])

// ─── Global Settings Tab ───
const settingsForm = reactive({
  fullScore: 100,
  passScore: 60,
  period: 'daily',
  method: 'weighted',
  grades: [
    { label: 'A (优秀)', text: '', threshold: 80, color: '#34C759' },
    { label: 'B (良好)', text: '', threshold: 60, color: '#409EFF' },
    { label: 'C (中等)', text: '', threshold: 40, color: '#E6A23C' },
    { label: 'D (较差)', text: '', threshold: 0, color: '#F56C6C' },
  ],
  autoScore: true,
  notifyEmail: true,
  notifySms: false,
  notifySite: true,
  notifyThreshold: 10,
})

// ─── Actions ───
function handleSave() {
  ElMessageBox.confirm('保存将替换当前评分规则配置，继续？', '提示', { type: 'warning' })
    .then(() => ElMessage.success('评分规则已保存 ✓'))
    .catch(() => {})
}

function handleReset() {
  ElMessageBox.confirm('重置将恢复默认评分规则，继续？', '提示', { type: 'warning' })
    .then(() => ElMessage.success('已重置为默认规则'))
    .catch(() => {})
}

function handleSettingsSave() {
  ElMessage.success('全局设置已保存 ✓')
}

function handleSettingsReset() {
  ElMessage.info('已取消修改')
}
</script>

<style scoped>
.page-container { padding: 16px; }

.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0; }
.page-desc { font-size: 13px; color: #909399; margin: 4px 0 0; }
.header-actions { display: flex; gap: 8px; flex-shrink: 0; }

/* ─── Overview Cards ─── */
.overview-row { margin-bottom: 16px; }
.overview-card { display: flex; align-items: center; gap: 16px; padding: 8px; }
.overview-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; }
.overview-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.overview-info { }
.overview-num { font-size: 20px; font-weight: 700; line-height: 1.2; }
.overview-lbl { font-size: 12px; color: #909399; margin-top: 2px; }

/* ─── Main Tabs ─── */
.main-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.main-tabs :deep(.el-tabs__item) { font-size: 14px; padding: 0 16px; }

/* ─── Rules Toolbar ─── */
.rules-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; flex-wrap: wrap; }
.toolbar-left { display: flex; gap: 8px; align-items: center; }

/* ─── Dimension Cards ─── */
.dim-card { border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; margin-bottom: 10px; }
.dim-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #f5f7fa; border-bottom: 1px solid #e4e7ed; cursor: pointer; user-select: none; }
.dim-header:hover { background: #ecf5ff; }
.dim-header-left { display: flex; align-items: center; gap: 8px; }
.dim-name { font-size: 14px; font-weight: 600; }
.dim-header-right { display: flex; align-items: center; gap: 8px; }
.dim-rule-count { font-size: 12px; color: #909399; }
.toggle-icon { font-size: 12px; color: #c0c4cc; transition: transform 0.2s; }
.toggle-icon.open { transform: rotate(180deg); }
.dim-body { padding: 12px 16px; }

.score-val { font-weight: 600; font-size: 14px; }
.score-val.positive { color: #34C759; }
.score-val.negative { color: #F56C6C; }

.add-rule-row { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #e4e7ed; }

/* ─── History Tab ─── */
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.history-filters { display: flex; gap: 8px; }

.bar-chart-wrap { display: flex; gap: 4px; align-items: flex-end; height: 160px; padding: 0 10px 24px; border-bottom: 1px solid #e4e7ed; margin-bottom: 16px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.bar-val { font-size: 11px; color: #909399; }
.bar { width: 100%; border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.3s; }
.bar-label { font-size: 11px; color: #909399; }

.history-stats { display: flex; justify-content: space-between; font-size: 12px; color: #606266; margin-bottom: 20px; }
.history-stats strong { font-weight: 600; }

.section-title { font-size: 13px; font-weight: 600; color: #606266; margin-bottom: 8px; }

/* ─── Settings Tab ─── */
.settings-form { max-width: 700px; }
.settings-form .el-form-item { margin-bottom: 18px; }

.grade-row { display: flex; gap: 12px; flex-wrap: wrap; }
.grade-item { display: flex; align-items: center; gap: 6px; }
.grade-tag { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.grade-text { font-size: 12px; color: #606266; }

.toggle-with-label { display: flex; align-items: center; gap: 10px; }
.toggle-hint { font-size: 12px; color: #909399; }

.notify-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }

.settings-footer { display: flex; justify-content: flex-end; gap: 12px; padding-top: 12px; }
</style>