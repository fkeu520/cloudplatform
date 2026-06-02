<template>
  <div class="leave-page">
    <div class="page-header">
      <h3>请假申请</h3>
      <el-button type="primary" @click="showNewDialog">
        <el-icon><Plus /></el-icon> 新增申请
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="instances" v-loading="loading" border stripe style="width:100%">
        <el-table-column prop="leaveTypeLabel" label="请假类型" width="100" align="center" />
        <el-table-column label="天数" width="70" align="center">
          <template #default="{ row }">{{ row.variables?.leaveDays }}天</template>
        </el-table-column>
        <el-table-column label="原因" min-width="200">
          <template #default="{ row }">
            <span class="reason-text">{{ row.variables?.reason }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'primary'" size="small">
              {{ row.status === 1 ? '已完成' : '审批中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showApprovalPath(row)">查看流程</el-button>
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="page-footer" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @change="loadMyInstances"
        />
      </div>
    </el-card>

    <el-dialog v-model="newDialogVisible" title="请假申请" width="520px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="请假类型" required>
          <el-select v-model="form.leaveType" placeholder="请选择" style="width:100%">
            <el-option label="年假" value="annual" />
            <el-option label="事假" value="personal" />
            <el-option label="病假" value="sick" />
            <el-option label="婚假" value="marriage" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" @change="calcDays" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" @change="calcDays" />
        </el-form-item>
        <el-form-item label="请假天数">
          <el-input v-model="form.days" disabled>
            <template #append>天</template>
          </el-input>
        </el-form-item>
        <el-form-item label="请假原因" required>
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入请假原因" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="newDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="submitApplication">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="申请详情" width="580px">
      <div v-if="detailData" class="detail-container">
        <div class="detail-section-title">请假信息</div>
        <div class="detail-card">
          <div class="detail-row">
            <label>流程名称</label>
            <span>{{ detailData.processDefinitionName }}</span>
          </div>
          <div class="detail-row">
            <label>请假类型</label>
            <span>{{ getLeaveTypeLabel(detailData.variables?.leaveType) }}</span>
          </div>
          <div class="detail-row">
            <label>请假天数</label>
            <span>{{ detailData.variables?.leaveDays }}天</span>
          </div>
          <div class="detail-row">
            <label>请假原因</label>
            <span class="detail-reason">{{ detailData.variables?.reason }}</span>
          </div>
          <div class="detail-row">
            <label>申请时间</label>
            <span>{{ formatDate(detailData.startTime) }}</span>
          </div>
          <div class="detail-row">
            <label>当前状态</label>
            <el-tag :type="detailData.status === 1 ? 'success' : 'primary'" size="small">
              {{ detailData.status === 1 ? '已完成' : '审批中' }}
            </el-tag>
          </div>
        </div>

        <div class="detail-section-title" style="margin-top:20px">审批过程</div>
        <el-timeline v-if="detailTimeline.length > 0">
          <el-timeline-item
            v-for="(t, i) in detailTimeline"
            :key="i"
            :timestamp="formatDate(t.startTime)"
            :type="t.endTime ? 'primary' : 'warning'"
            placement="top"
          >
            <div class="tl-item">
              <span class="tl-name">{{ t.activityName || t.activityId }}</span>
              <el-tag v-if="!t.endTime" size="small" type="warning">进行中</el-tag>
              <el-tag v-else size="small" type="success">已完成</el-tag>
            </div>
            <div v-if="t.assignee" class="tl-meta">办理人: {{ t.assignee }}</div>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="empty-state">暂无审批记录</div>
      </div>
      <div v-else class="empty-state">暂无数据</div>
    </el-dialog>

    <el-dialog v-model="pathVisible" title="审批路径" width="550px">
      <div v-if="pathNodes.length > 0" class="path-container">
        <div
          v-for="(node, i) in pathNodes"
          :key="node.id"
          class="path-node"
          :class="getPathNodeClass(node)"
        >
          <div class="path-connector" v-if="i > 0">
            <div class="path-line" :class="{ active: i <= currentNodeIndex }"></div>
            <div class="path-arrow" :class="{ active: i <= currentNodeIndex }">▼</div>
          </div>
          <div class="path-content">
            <div class="path-icon">
              <el-icon v-if="node.status === 'completed'"><CircleCheck /></el-icon>
              <el-icon v-else-if="node.status === 'active'" color="#e6a23c"><Loading /></el-icon>
              <el-icon v-else color="#c0c4cc"><CircleClose /></el-icon>
            </div>
            <div class="path-info">
              <span class="path-name">{{ node.name }}</span>
              <el-tag v-if="node.status === 'completed'" size="small" type="success">已通过</el-tag>
              <el-tag v-else-if="node.status === 'active'" size="small" type="warning">审批中</el-tag>
              <el-tag v-else size="small" type="info">待审批</el-tag>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">暂无数据</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, CircleCheck, CircleClose, Loading } from '@element-plus/icons-vue'
import { deployDefinition, startInstance, getInstancePage, getInstanceById, getInstanceTimeline } from '@/api/workflow'

const LEAVE_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="leave-approval" name="请假审批流程" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="开始" />
    <bpmn:userTask id="deptApproval" name="部门审批" flowable:assignee="admin">
      <bpmn:extensionElements>
        <flowable:candidateUsers>1</flowable:candidateUsers>
      </bpmn:extensionElements>
    </bpmn:userTask>
    <bpmn:userTask id="hrApproval" name="人事审批" flowable:assignee="admin">
      <bpmn:extensionElements>
        <flowable:candidateUsers>1</flowable:candidateUsers>
      </bpmn:extensionElements>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="结束" />
    <bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="deptApproval" />
    <bpmn:sequenceFlow id="flow2" sourceRef="deptApproval" targetRef="hrApproval" />
    <bpmn:sequenceFlow id="flow3" sourceRef="hrApproval" targetRef="endEvent" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave-approval">
      <bpmndi:BPMNShape id="shape_startEvent" bpmnElement="startEvent">
        <dc:Bounds x="80" y="140" width="80" height="60" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="shape_deptApproval" bpmnElement="deptApproval">
        <dc:Bounds x="260" y="130" width="80" height="60" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="shape_hrApproval" bpmnElement="hrApproval">
        <dc:Bounds x="460" y="130" width="80" height="60" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="shape_endEvent" bpmnElement="endEvent">
        <dc:Bounds x="640" y="140" width="80" height="60" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="edge_flow1" bpmnElement="flow1">
        <di:waypoint x="160" y="170" />
        <di:waypoint x="260" y="160" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="edge_flow2" bpmnElement="flow2">
        <di:waypoint x="340" y="160" />
        <di:waypoint x="460" y="160" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="edge_flow3" bpmnElement="flow3">
        <di:waypoint x="540" y="160" />
        <di:waypoint x="640" y="170" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const form = ref({
  leaveType: 'annual',
  startDate: '',
  endDate: '',
  days: 0,
  reason: ''
})

const loading = ref(false)
const submitting = ref(false)
const instances = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const newDialogVisible = ref(false)
const detailVisible = ref(false)
const detailData = ref<any>(null)
const detailTimeline = ref<any[]>([])
const pathVisible = ref(false)
const pathNodes = ref<any[]>([])
const currentNodeIndex = ref(0)

const DEPLOY_KEY = 'leave-approval'

function formatDate(dt: any): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function calcDays() {
  if (form.value.startDate && form.value.endDate) {
    const s = new Date(form.value.startDate)
    const e = new Date(form.value.endDate)
    const diff = Math.max(1, Math.ceil((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24)) + 1)
    form.value.days = diff
  }
}

function getLeaveTypeLabel(type: string) {
  const map: Record<string, string> = { annual: '年假', personal: '事假', sick: '病假', marriage: '婚假', other: '其他' }
  return map[type] || type
}

async function ensureDeployed() {
  try {
    await deployDefinition({ processName: '请假审批流程', bpmnXml: LEAVE_BPMN })
  } catch (e: any) {
    if (e?.response?.status === 500 || e?.message?.includes('已存在')) {
      console.log('Leave BPMN already deployed')
    } else {
      console.warn('Deploy warning:', e)
    }
  }
}

async function loadMyInstances() {
  loading.value = true
  try {
    const res = await getInstancePage({ processDefinitionKey: DEPLOY_KEY, pageNum: query.pageNum, pageSize: query.pageSize }) as any
    const data = res?.data || res
    instances.value = (data?.records || []).map((r: any) => ({
      ...r,
      variables: r.variables || {},
      leaveTypeLabel: getLeaveTypeLabel(r.variables?.leaveType)
    }))
    total.value = data?.total || 0
  } catch {
    instances.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function showNewDialog() {
  resetForm()
  newDialogVisible.value = true
}

async function submitApplication() {
  if (!form.value.leaveType) { ElMessage.warning('请选择请假类型'); return }
  if (!form.value.startDate) { ElMessage.warning('请选择开始日期'); return }
  if (!form.value.endDate) { ElMessage.warning('请选择结束日期'); return }
  if (!form.value.reason.trim()) { ElMessage.warning('请输入请假原因'); return }
  submitting.value = true
  try {
    const userId = localStorage.getItem('userId') || '1'
    await startInstance({
      processDefinitionKey: DEPLOY_KEY,
      variables: {
        leaveType: form.value.leaveType,
        leaveDays: form.value.days,
        reason: form.value.reason,
        applicant: 'admin'
      },
      userId
    })
    ElMessage.success('请假申请已提交')
    newDialogVisible.value = false
    loadMyInstances()
  } catch (e: any) {
    ElMessage.error('提交失败: ' + (e?.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.value = { leaveType: 'annual', startDate: '', endDate: '', days: 0, reason: '' }
}

async function showDetail(item: any) {
  try {
    const id = item.processInstanceId || item.id
    const [instRes, tlRes] = await Promise.all([
      getInstanceById(id),
      getInstanceTimeline(id)
    ])
    const data = instRes?.data || instRes
    const vars: any = {}
    if (data?.variables) {
      if (Array.isArray(data.variables)) {
        for (const v of data.variables) {
          vars[v.name] = v.value
        }
      } else {
        Object.assign(vars, data.variables)
      }
    }
    detailData.value = { ...data, variables: vars }
    const rawActs = tlRes?.data || tlRes || []
    detailTimeline.value = (Array.isArray(rawActs) ? rawActs : []).filter(
      (a: any) => ['userTask', 'startEvent'].includes(a.activityType))
    detailVisible.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

async function showApprovalPath(item: any) {
  try {
    const res = await getInstanceTimeline(item.processInstanceId || item.id) as any
    const acts = (res?.data || res || []) as any[]

    const nodeDefs: { id: string; name: string; order: number }[] = []
    const parser = new DOMParser()
    const doc = parser.parseFromString(LEAVE_BPMN, 'text/xml')
    const ns = 'http://www.omg.org/spec/BPMN/20100524/MODEL'
    const processEl = doc.getElementsByTagNameNS(ns, 'process')[0] || doc.querySelector('process') as Element
    if (processEl) {
      for (let i = 0; i < processEl.children.length; i++) {
        const el = processEl.children[i] as Element
        const tag = el.localName || ''
        if (tag === 'userTask' || tag === 'startEvent' || tag === 'endEvent') {
          nodeDefs.push({
            id: el.getAttribute('id') || '',
            name: el.getAttribute('name') || tag,
            order: nodeDefs.length
          })
        }
      }
    }

    const userActs = acts.filter(a => ['userTask', 'startEvent'].includes(a.activityType))
    const completedIds = new Set(userActs.filter(a => a.endTime).map(a => a.activityId))
    const activeIds = new Set(userActs.filter(a => !a.endTime).map(a => a.activityId))

    pathNodes.value = nodeDefs.map(n => {
      let status = 'pending'
      if (completedIds.has(n.id)) status = 'completed'
      else if (activeIds.has(n.id)) status = 'active'
      return { ...n, status }
    })

    const lastCompleted = nodeDefs.reduce((last, n, idx) => completedIds.has(n.id) ? idx : last, -1)
    currentNodeIndex.value = Math.max(0, lastCompleted + 1)

    pathVisible.value = true
  } catch {
    ElMessage.error('获取审批路径失败')
  }
}

function getPathNodeClass(node: any) {
  return { 'is-completed': node.status === 'completed', 'is-active': node.status === 'active' }
}

onMounted(async () => {
  await ensureDeployed()
  await loadMyInstances()
})
</script>

<style scoped>
.leave-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #606266;
  font-size: 13px;
}

.page-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 13px;
}

.detail-container {
  padding: 0 8px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
  margin-bottom: 12px;
}

.detail-card {
  background: #fafafa;
  border-radius: 6px;
  padding: 4px 16px;
  border: 1px solid #ebeef5;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-row label {
  width: 100px;
  color: #909399;
  font-size: 13px;
  flex-shrink: 0;
}

.detail-row span {
  flex: 1;
  color: #303133;
  font-size: 13px;
}

.detail-reason {
  word-break: break-all;
}

.tl-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.tl-name {
  font-weight: 600;
  font-size: 13px;
}

.tl-meta {
  font-size: 12px;
  color: #909399;
}

.path-container {
  padding: 16px 8px;
}

.path-node {
  position: relative;
}

.path-node.is-active .path-name {
  color: #e6a23c;
}

.path-node.is-completed .path-name {
  color: #67c23a;
}

.path-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 0 0 8px 18px;
}

.path-line {
  width: 2px;
  height: 24px;
  background: #e4e7ed;
}

.path-line.active {
  background: #409eff;
}

.path-arrow {
  font-size: 10px;
  color: #e4e7ed;
  line-height: 1;
  margin-top: -2px;
}

.path-arrow.active {
  color: #409eff;
}

.path-content {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  transition: all 0.2s;
}

.path-node.is-active .path-content {
  background: #fdf6ec;
  border-color: #e6a23c;
}

.path-node.is-completed .path-content {
  background: #f0f9eb;
  border-color: #67c23a;
}

.path-icon {
  font-size: 18px;
  display: flex;
  align-items: center;
}

.path-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.path-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
</style>
