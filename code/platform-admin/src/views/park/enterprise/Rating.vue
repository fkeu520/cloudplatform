<template>
  <div class="page-container">
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>评分规则</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px">
      <div>
        <h2 style="font-size:20px;font-weight:700;margin:0">企业评分规则</h2>
        <p style="font-size:13px;color:#909399;margin:4px 0 0">配置企业评分维度和规则，系统根据规则自动计算企业评分</p>
      </div>
      <div style="display:flex;gap:8px">
        <el-button @click="handleReset">重置默认</el-button>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </div>
    </div>

    <!-- 概览卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6" v-for="s in overviewStats" :key="s.label">
        <el-card shadow="never" class="overview-card">
          <div class="overview-icon" :style="{ background: s.bg }">{{ s.icon }}</div>
          <div><div class="overview-num">{{ s.value }}</div><div class="overview-lbl">{{ s.label }}</div></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 维度卡片 -->
    <div class="dimension-tabs">
      <el-tabs v-model="activeDim" type="border-card">
        <el-tab-pane v-for="dim in dimensions" :key="dim.key" :name="dim.key">
          <template #label>
            <span>{{ dim.icon }} {{ dim.name }} <el-tag size="small" type="info" style="margin-left:4px">权重 {{ dim.weight }}%</el-tag></span>
          </template>

          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <span style="font-size:13px;color:#909399">{{ dim.desc }}</span>
            <el-button size="small" @click="addRule(dim)">+ 新增规则</el-button>
          </div>

          <el-table :data="dim.rules" border size="small" max-height="400">
            <el-table-column type="index" label="#" width="45" />
            <el-table-column prop="name" label="规则名称" min-width="160" />
            <el-table-column prop="condition" label="条件" width="200" />
            <el-table-column prop="score" label="分值" width="80" align="center">
              <template #default="scope">
                <span :style="{ color: scope.row.score >= 0 ? '#34C759' : '#F56C6C', fontWeight: 600 }">
                  {{ scope.row.score >= 0 ? '+' : '' }}{{ scope.row.score }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="70" align="center">
              <template #default="scope">
                <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="editRule(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteRule(dim, scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 评分分布 -->
    <el-card shadow="never" style="margin-top:16px">
      <template #header><strong>📊 评分分布</strong></template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="d in scoreDistribution" :key="d.label">
          <div style="margin-bottom:8px">
            <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px">
              <span>{{ d.label }}</span><span>{{ d.count }} 家 ({{ d.percent }}%)</span>
            </div>
            <el-progress :percentage="d.percent" :color="d.color" :show-text="false" />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 编辑规则弹窗 -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleDialogTitle" width="500px" top="20vh">
      <el-form :model="ruleForm" label-width="100px">
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="条件">
          <el-input v-model="ruleForm.condition" maxlength="128" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="ruleForm.score" :min="-100" :max="100" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// ─── State ───
const activeDim = ref('operation')

interface Rule { name: string; condition: string; score: number; status: number }
interface Dimension { key: string; icon: string; name: string; weight: number; desc: string; rules: Rule[] }

const dimensions = reactive<Dimension[]>([
  { key: 'operation', icon: '📊', name: '经营状况', weight: 30, desc: '根据企业经营数据评分，包括营收、利润、资产等指标',
    rules: [
      { name: '年营收增长率 ≥ 20%', condition: 'growth_rate >= 20', score: 15, status: 1 },
      { name: '年营收增长率 10-20%', condition: '10 <= growth_rate < 20', score: 10, status: 1 },
      { name: '年营收增长率 0-10%', condition: '0 <= growth_rate < 10', score: 5, status: 1 },
      { name: '净利润为正', condition: 'net_profit > 0', score: 10, status: 1 },
      { name: '总资产 ≥ 1亿', condition: 'total_assets >= 100000000', score: 8, status: 1 },
      { name: '资产负债率 < 50%', condition: 'debt_ratio < 50', score: 7, status: 1 },
    ]},
  { key: 'innovation', icon: '💡', name: '创新能力', weight: 20, desc: '根据企业创新指标评分，包括专利、商标、著作权等',
    rules: [
      { name: '有效发明专利 ≥ 10 项', condition: 'invention_patents >= 10', score: 12, status: 1 },
      { name: '有效发明专利 5-9 项', condition: '5 <= invention_patents < 10', score: 8, status: 1 },
      { name: '有效发明专利 1-4 项', condition: '1 <= invention_patents < 5', score: 4, status: 1 },
      { name: '拥有商标', condition: 'trademark_count > 0', score: 5, status: 1 },
      { name: '拥有软件著作权', condition: 'copyright_count > 0', score: 5, status: 1 },
      { name: '高新技术企业认定', condition: 'is_high_tech == 1', score: 10, status: 1 },
    ]},
  { key: 'credit', icon: '🛡️', name: '信用状况', weight: 25, desc: '根据企业信用记录评分，包括行政处罚、司法风险等',
    rules: [
      { name: '近 3 年无行政处罚', condition: 'penalty_count_3y == 0', score: 15, status: 1 },
      { name: '近 3 年无司法诉讼', condition: 'lawsuit_count_3y == 0', score: 10, status: 1 },
      { name: '无失信被执行记录', condition: 'dishonest_count == 0', score: 20, status: 1 },
      { name: '无经营异常记录', condition: 'abnormal_count == 0', score: 10, status: 1 },
      { name: '连续 3 年工商年报正常', condition: 'annual_report_3y_normal', score: 8, status: 1 },
    ]},
  { key: 'talent', icon: '👥', name: '人才团队', weight: 15, desc: '根据企业人才结构评分，包括规模、学历、社保等',
    rules: [
      { name: '社保参保人数 ≥ 100', condition: 'social_insurance_count >= 100', score: 10, status: 1 },
      { name: '社保参保人数 50-99', condition: '50 <= social_insurance_count < 100', score: 6, status: 1 },
      { name: '本科以上学历占比 ≥ 50%', condition: 'bachelor_ratio >= 50', score: 8, status: 1 },
      { name: '研发人员占比 ≥ 20%', condition: 'rd_ratio >= 20', score: 8, status: 1 },
      { name: '核心团队稳定（近 3 年无重大变更）', condition: 'team_stable_3y', score: 5, status: 1 },
    ]},
  { key: 'social', icon: '🌍', name: '社会责任', weight: 10, desc: '根据企业社会责任表现评分，包括税收、就业、环保等',
    rules: [
      { name: '年纳税额 ≥ 500 万', condition: 'annual_tax >= 5000000', score: 10, status: 1 },
      { name: '年纳税额 100-500 万', condition: '1000000 <= annual_tax < 5000000', score: 6, status: 1 },
      { name: '近 3 年无环保处罚', condition: 'env_penalty_3y == 0', score: 8, status: 1 },
      { name: '带动就业 ≥ 500 人', condition: 'employment_count >= 500', score: 5, status: 1 },
    ]},
])

const overviewStats = [
  { icon: '📐', value: '5', label: '评分维度', bg: '#ecf5ff' },
  { icon: '📋', value: '26', label: '评分规则', bg: '#f0f9eb' },
  { icon: '🏢', value: '128', label: '已评分企业', bg: '#fdf6ec' },
  { icon: '⭐', value: '76.5', label: '平均分', bg: '#f5f7fa' },
]

const scoreDistribution = [
  { label: '优秀 (90-100)', count: 18, percent: 14, color: '#34C759' },
  { label: '良好 (80-89)', count: 35, percent: 27, color: '#409EFF' },
  { label: '中等 (60-79)', count: 48, percent: 38, color: '#E6A23C' },
  { label: '较差 (<60)', count: 27, percent: 21, color: '#F56C6C' },
]

// ─── Rule Dialog ───
const ruleDialogVisible = ref(false)
const ruleDialogTitle = ref('新增规则')
const editingDimKey = ref('')
const editingRuleIndex = ref(-1)
const ruleForm = reactive({ name: '', condition: '', score: 0, status: 1 })

function addRule(dim: Dimension) {
  editingDimKey.value = dim.key
  editingRuleIndex.value = -1
  ruleDialogTitle.value = '新增规则'
  Object.assign(ruleForm, { name: '', condition: '', score: 0, status: 1 })
  ruleDialogVisible.value = true
}

function editRule(rule: Rule) {
  editingRuleIndex.value = -1
  ruleDialogTitle.value = '编辑规则'
  Object.assign(ruleForm, { name: rule.name, condition: rule.condition, score: rule.score, status: rule.status })
  ruleDialogVisible.value = true
}

function confirmRule() {
  if (editingRuleIndex.value >= 0) {
    const dim = dimensions.find(d => d.key === editingDimKey.value)
    if (dim && dim.rules[editingRuleIndex.value]) {
      Object.assign(dim.rules[editingRuleIndex.value], ruleForm)
    }
  } else {
    const dim = dimensions.find(d => d.key === editingDimKey.value)
    if (dim) dim.rules.push({ ...ruleForm })
  }
  ElMessage.success(editingRuleIndex.value >= 0 ? '规则已更新' : '规则已添加')
  ruleDialogVisible.value = false
}

function deleteRule(dim: Dimension, index: number) {
  ElMessageBox.confirm('确认删除该规则？', '提示', { type: 'warning' })
    .then(() => { dim.rules.splice(index, 1); ElMessage.success('已删除') })
    .catch(() => {})
}

// ─── Actions ───
function handleSave() {
  ElMessageBox.confirm('保存将替换当前评分规则配置，继续？', '提示', { type: 'warning' })
    .then(() => ElMessage.success('评分规则已保存 ✓'))
    .catch(() => {})
}

function handleReset() {
  ElMessageBox.confirm('重置将恢复默认评分规则，继续？', '提示', { type: 'warning' })
    .then(() => {
      // Reset to default rules (simplified)
      ElMessage.success('已重置为默认规则')
    })
    .catch(() => {})
}
</script>

<style scoped>
.page-container { padding: 16px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

.overview-card { display: flex; align-items: center; gap: 16px; padding: 8px; }
.overview-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; }
.overview-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.overview-num { font-size: 20px; font-weight: 700; }
.overview-lbl { font-size: 12px; color: #909399; margin-top: 2px; }

.dimension-tabs { }
.dimension-tabs :deep(.el-tabs--border-card) { box-shadow: none; border: 1px solid #e4e7ed; }
</style>