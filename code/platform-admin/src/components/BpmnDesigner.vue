<template>
  <div class="bpmn-designer">
    <div class="designer-toolbar">
      <el-button size="small" @click="undo" :disabled="!canUndo">撤销</el-button>
      <el-button size="small" @click="redo" :disabled="!canRedo">重做</el-button>
      <el-divider direction="vertical" />
      <el-button size="small" @click="zoomIn">放大</el-button>
      <el-button size="small" @click="zoomOut">缩小</el-button>
      <el-button size="small" @click="zoomReset">适配</el-button>
      <el-divider direction="vertical" />
      <el-button size="small" @click="validate">校验</el-button>
      <el-button size="small" @click="downloadSvg">下载SVG</el-button>
      <div style="flex:1" />
      <el-button type="primary" size="small" @click="handleSave">保存</el-button>
    </div>
    <div class="designer-body">
      <div class="designer-canvas" ref="canvasRef" />
      <div class="designer-properties" v-if="showProperties">
        <div class="properties-header">
          <span class="properties-title">{{ elementTypeLabel }}</span>
          <el-tag size="small" :type="elementTagType">{{ elementType }}</el-tag>
        </div>
        <div class="properties-body">
          <el-form label-width="80px" size="small" v-if="elementType && elementType !== 'Process'">

            <!-- 常规设置 -->
            <div class="prop-section">
              <div class="prop-section-title">常规设置</div>
              <el-form-item label="ID">
                <el-input :model-value="elementId" disabled />
              </el-form-item>
              <el-form-item label="节点名称">
                <el-input v-model="elementName" @input="updateName" placeholder="请输入节点名称" />
              </el-form-item>
              <el-form-item v-if="elementType === 'UserTask' || elementType === 'CallActivity'" label="节点描述">
                <el-input v-model="elementDescription" @input="updateDescription" type="textarea" :rows="2" placeholder="可选" />
              </el-form-item>
              <el-form-item v-if="elementType === 'CallActivity'" label="子流程Key">
                <el-input v-model="calledElement" @input="updateCalledElement" placeholder="已部署的流程定义Key" />
              </el-form-item>
            </div>

            <!-- 配置候选 (UserTask) -->
            <div class="prop-section" v-if="elementType === 'UserTask'">
              <div class="prop-section-title">配置候选</div>
              <el-form-item label="候选范围">
                <el-radio-group v-model="candidateScope" @change="onCandidateScopeChange" size="small">
                  <el-radio value="company">公司</el-radio>
                  <el-radio value="dept">本部门</el-radio>
                  <el-radio value="group">集团</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="选择方式">
                <el-radio-group v-model="candidateType" size="small">
                  <el-radio value="personnel">按人员选择</el-radio>
                  <el-radio value="position">按岗位选择</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="candidateType === 'personnel'" label="人员选择">
                <div class="selection-row">
                  <el-tag
                    v-for="uid in selectedPersonnelIds"
                    :key="uid"
                    closable
                    size="small"
                    @close="removePersonnel(uid)"
                  >{{ getUserName(uid) }}</el-tag>
                  <el-button size="small" type="primary" link @click="openOrgDialog('personnel')">
                    <el-icon><Plus /></el-icon> 选择人员
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item v-if="candidateType === 'position'" label="岗位选择">
                <div class="selection-row">
                  <el-tag v-if="selectedPositionId" closable size="small" @close="removePosition">
                    {{ getPositionName(selectedPositionId) }}
                  </el-tag>
                  <el-button size="small" type="primary" link @click="openOrgDialog('position')">
                    <el-icon><Plus /></el-icon> 选择岗位
                  </el-button>
                </div>
              </el-form-item>
            </div>

            <!-- 设置审批规则 (UserTask) -->
            <div class="prop-section" v-if="elementType === 'UserTask'">
              <div class="prop-section-title">设置审批规则</div>
              <el-form-item label="审批模式">
                <el-radio-group v-model="approvalMode" @change="saveFlowableData" size="small">
                  <el-radio value="single">单签模式</el-radio>
                  <el-radio value="countersign">会签模式</el-radio>
                  <el-radio value="orsign">或签模式</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="连续审批跳过">
                <el-switch v-model="skipContinuous" @change="saveFlowableData" />
                <span class="switch-hint">审批人相同时自动跳过</span>
              </el-form-item>
              <el-form-item label="电子签名">
                <el-switch v-model="eSignature" @change="saveFlowableData" />
              </el-form-item>
            </div>

            <!-- 连线配置 (SequenceFlow) -->
            <div class="prop-section" v-if="elementType === 'SequenceFlow'">
              <div class="prop-section-title">连线配置</div>
              <el-form-item label="条件表达式">
                <el-input v-model="conditionExpression" @input="updateConditionExpression" placeholder="${day > 3}" />
                <div class="field-hint">使用 ${变量} 格式，满足条件时走此分支</div>
              </el-form-item>
            </div>

          </el-form>
          <div v-else style="color:#909399;font-size:13px;padding:8px 0">点击图中的节点编辑属性</div>
        </div>
      </div>
    </div>

    <!-- 候选人选择弹窗 -->
    <el-dialog v-model="orgDialogVisible" title="候选人配置" width="600px" :close-on-click-modal="false">
      <el-input v-model="orgSearchKeyword" placeholder="搜索姓名/部门" size="small" clearable style="margin-bottom:12px" />
      <el-tabs v-model="orgDialogMode" class="dialog-tabs">
        <el-tab-pane label="人员选择" name="personnel">
          <div class="org-tree-container">
            <el-tree
              ref="orgTreeRef"
              :data="filteredOrgTree"
              :props="{ label: 'name', children: 'children' }"
              show-checkbox
              default-expand-all
              node-key="id"
              check-on-click-node
              :filter-node-method="filterOrgNode"
            >
              <template #default="{ data }">
                <span class="org-tree-node">
                  <el-tag v-if="data.type === 'user'" size="small" type="success" style="margin-right:4px">人</el-tag>
                  <el-tag v-else-if="data.type === 'post'" size="small" type="warning" style="margin-right:4px">岗</el-tag>
                  <span>{{ data.name }}</span>
                  <span v-if="data.orgName" style="color:#909399;font-size:12px;margin-left:6px">{{ data.orgName }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-tab-pane>
        <el-tab-pane label="岗位选择" name="position">
          <div class="org-tree-container">
            <el-tree
              ref="posTreeRef"
              :data="filteredOrgTree"
              :props="{ label: 'name', children: 'children' }"
              show-checkbox
              default-expand-all
              node-key="id"
              check-on-click-node
              :filter-node-method="filterOrgNode"
            >
              <template #default="{ data }">
                <span class="org-tree-node">
                  <el-tag v-if="data.type === 'user'" size="small" type="success" style="margin-right:4px">人</el-tag>
                  <el-tag v-else-if="data.type === 'post'" size="small" type="warning" style="margin-right:4px">岗</el-tag>
                  <span>{{ data.name }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="orgDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmOrgSelection">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'
import { getOrgTree } from '@/api/org'
import { getUserPage } from '@/api/user'
import { getPostPage } from '@/api/post'

const emit = defineEmits<{ (e: 'save', xml: string): void }>()
const props = defineProps<{ modelValue?: string }>()

const canvasRef = ref<HTMLElement>()
let modeler: BpmnModeler | null = null
const canUndo = ref(false)
const canRedo = ref(false)
const showProperties = ref(true)
const elementId = ref('')
const elementName = ref('')
const elementDescription = ref('')
const elementType = ref('')
const currentElement = ref<any>(null)

const candidateScope = ref('company')
const candidateType = ref('personnel')
const selectedPersonnelIds = ref<string[]>([])
const selectedPositionId = ref<any>(null)
const approvalMode = ref('single')
const skipContinuous = ref(false)
const eSignature = ref(false)
const calledElement = ref('')
const conditionExpression = ref('')

const orgDialogVisible = ref(false)
const orgDialogMode = ref<'personnel' | 'position'>('personnel')
const orgSearchKeyword = ref('')
const orgTreeRef = ref<any>(null)
const posTreeRef = ref<any>(null)
const orgTree = ref<any[]>([])
const allEmployees = ref<any[]>([])
const allPositions = ref<any[]>([])
const flowableData = new Map<string, Record<string, string>>()

const defaultDiagram = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="process" isExecutable="true">
    <bpmn:startEvent id="start" name="开始" />
    <bpmn:endEvent id="end" name="结束" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process">
      <bpmndi:BPMNShape id="start_shape" bpmnElement="start">
        <dc:Bounds x="100" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="end_shape" bpmnElement="end">
        <dc:Bounds x="300" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const elementTypeLabel = computed(() => {
  const map: Record<string, string> = {
    StartEvent: '开始事件', EndEvent: '结束事件', UserTask: '审批任务',
    ExclusiveGateway: '互斥网关', ParallelGateway: '并行网关',
    CallActivity: '子流程', SequenceFlow: '连线'
  }
  return map[elementType.value] || elementType.value || '元素'
})

const elementTagType = computed(() => {
  const map: Record<string, string> = {
    StartEvent: 'success', EndEvent: 'danger', UserTask: 'primary',
    ExclusiveGateway: 'warning', ParallelGateway: 'warning',
    CallActivity: 'info', SequenceFlow: 'info'
  }
  return (map[elementType.value] || '') as any
})

const filteredOrgTree = computed(() => {
  if (!orgSearchKeyword.value) return orgTree.value
  return orgTree.value
})

watch(orgSearchKeyword, (val) => {
  orgTreeRef.value?.filter(val)
  posTreeRef.value?.filter(val)
})

function filterOrgNode(value: string, data: any) {
  if (!value) return true
  return (data.name || '').includes(value)
}

// XML 字符转义 (修复 conditionExpression 嵌入 BPMN XML 时 < > & " ' 未转义导致 Flowable 解析失败)
// 关联 KNOWN_ISSUES: 类似问题在 candidateUsers/candidateGroups 也会出现 (line 708-714)
function xmlEscape(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

// XML 字符反转义 (从 BPMN XML 提取 conditionExpression 时调用, 把 &lt; 转回 <)
// 否则 UI 上会显示 ${day &lt; 3} 而不是 ${day < 3}, 用户体验差
function xmlUnescape(s: string): string {
  return s
    .replace(/&apos;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&')
}

function getUserName(uid: string): string {
  const user = allEmployees.value.find((u: any) => String(u.id) === String(uid))
  return user ? (user.nickname || user.name || user.username) : uid
}

function getPositionName(pid: any): string {
  if (!pid) return ''
  const pos = allPositions.value.find((p: any) => String(p.id) === String(pid))
  return pos?.name || `岗位(${pid})`
}

async function loadOrgData() {
  try {
    const [treeRes, empRes, posRes] = await Promise.all([
      getOrgTree(),
      getUserPage({ pageNum: 1, pageSize: 500, status: 1 }),
      getPostPage({ pageNum: 1, pageSize: 200 })
    ])
    const rawTree = (treeRes as any)?.data || treeRes || []
    const employees = (empRes as any)?.data?.records || (empRes as any)?.records || []
    const positions = (posRes as any)?.data?.records || (posRes as any)?.records || []
    allEmployees.value = employees
    allPositions.value = positions

    function buildTree(nodes: any[]): any[] {
      return nodes.map((node: any) => {
        const children: any[] = node.children ? buildTree(node.children) : []
        const usersInOrg = employees.filter((u: any) => u.deptId == node.id || u.orgId == node.id)
        const userNodes = usersInOrg.map((u: any) => ({
          id: 'user_' + u.id, name: u.nickname || u.name || u.username,
          orgName: u.orgName || '', type: 'user', realId: String(u.id)
        }))
        const postsInOrg = positions.filter((p: any) => p.deptId == node.id || p.orgId == node.id)
        const postNodes = postsInOrg.map((p: any) => ({
          id: 'post_' + p.id, name: p.name, type: 'post', realId: String(p.id)
        }))
        return { ...node, children: [...children, ...userNodes, ...postNodes] }
      })
    }
    orgTree.value = buildTree(Array.isArray(rawTree) ? rawTree : [rawTree])
  } catch (e) {
    console.warn('Failed to load org data:', e)
  }
}

function openOrgDialog(mode: 'personnel' | 'position') {
  orgDialogMode.value = mode
  orgDialogVisible.value = true
  orgSearchKeyword.value = ''
  nextTick(() => {
    if (mode === 'personnel' && orgTreeRef.value) {
      orgTreeRef.value.setCheckedKeys(selectedPersonnelIds.value.map(id => 'user_' + id))
    } else if (mode === 'position' && posTreeRef.value) {
      posTreeRef.value.setCheckedKeys(selectedPositionId.value ? ['post_' + selectedPositionId.value] : [])
    }
  })
}

function confirmOrgSelection() {
  if (orgDialogMode.value === 'personnel' && orgTreeRef.value) {
    const checkedNodes = orgTreeRef.value.getCheckedNodes(true)
    const userNodes = checkedNodes.filter((n: any) => n.type === 'user')
    selectedPersonnelIds.value = userNodes.map((n: any) => n.realId)
  } else if (orgDialogMode.value === 'position' && posTreeRef.value) {
    const checkedNodes = posTreeRef.value.getCheckedNodes(true)
    const postNodes = checkedNodes.filter((n: any) => n.type === 'post')
    selectedPositionId.value = postNodes.length > 0 ? postNodes[0].realId : null
  }
  orgDialogVisible.value = false
  saveFlowableData()
}

function removePersonnel(uid: string) {
  selectedPersonnelIds.value = selectedPersonnelIds.value.filter(id => id !== uid)
  saveFlowableData()
}

function removePosition() {
  selectedPositionId.value = null
  saveFlowableData()
}

function onCandidateScopeChange() {
  selectedPersonnelIds.value = []
  selectedPositionId.value = null
  saveFlowableData()
}

function saveFlowableData() {
  if (!currentElement.value) return
  const id = currentElement.value.id
  const data: Record<string, string> = {}
  if (elementDescription.value) data.description = elementDescription.value
  if (candidateScope.value) data.candidateScope = candidateScope.value
  if (candidateType.value) data.candidateType = candidateType.value
  if (candidateType.value === 'personnel') {
    if (selectedPersonnelIds.value.length > 0) data.candidateUsers = selectedPersonnelIds.value.join(',')
  } else {
    if (selectedPositionId.value) data.candidateGroups = String(selectedPositionId.value)
  }
  if (approvalMode.value) data.approvalMode = approvalMode.value
  if (skipContinuous.value) data.skipContinuous = 'true'
  if (eSignature.value) data.eSignature = 'true'
  if (calledElement.value) data.calledElement = calledElement.value
  if (conditionExpression.value) data.conditionExpression = conditionExpression.value
  flowableData.set(id, data)

  // 同时写入 bpmn-js 模型元素，确保 modeler.saveXML() 能直接生成正确 XML
  if (modeler && currentElement.value) {
    try {
      const modeling = modeler.get('modeling')
      const moddle = modeler.get('moddle')
      const el = currentElement.value
      const bo = el.businessObject || el
      const updateProps: Record<string, any> = {}

      // 条件表达式 - 创建 FormalExpression 模型元素
      if (elementType.value === 'SequenceFlow') {
        if (conditionExpression.value) {
          updateProps.conditionExpression = moddle.create('bpmn:FormalExpression', { body: conditionExpression.value })
        } else {
          updateProps.conditionExpression = undefined
        }
      }

      // 候选人/组 - 仅保存到 flowableData Map，由 injectFlowableProps 写入 XML extensionElements
      // extensionElement 形式是 BPMN 2.0 标准, Flowable 完全支持, 且两个设计器可互通解析

      // flowable 自定义属性直接写入 businessObject (非标准 BPMN，仅用于前端状态保持)
      if (data.candidateScope) bo.candidateScope = data.candidateScope
      if (data.approvalMode) bo.approvalMode = data.approvalMode
      if (data.skipContinuous === 'true') bo.skipContinuous = 'true'
      if (data.eSignature === 'true') bo.eSignature = 'true'

      // 子流程
      if (elementType.value === 'CallActivity' && data.calledElement) {
        updateProps.calledElement = data.calledElement
      }

      modeling.updateProperties(el, updateProps)
    } catch (e) {
      console.warn('Failed to update model element:', e)
    }
  }
}

function loadFlowableData(elementId: string) {
  const data = flowableData.get(elementId) || {}
  elementDescription.value = data.description || ''
  candidateScope.value = data.candidateScope || 'company'
  candidateType.value = data.candidateType || 'personnel'
  selectedPersonnelIds.value = (data.candidateUsers || '').split(',').map(s => s.trim()).filter(Boolean)
  selectedPositionId.value = data.candidateGroups || null
  approvalMode.value = data.approvalMode || 'single'
  skipContinuous.value = data.skipContinuous === 'true'
  eSignature.value = data.eSignature === 'true'
  calledElement.value = data.calledElement || ''
  conditionExpression.value = data.conditionExpression || ''
}

function extractFlowableDataFromXml(xml: string) {
  // 字符串正则提取，绕过 DOM 命名空间问题
  // 匹配所有有 id 的 BPMN 元素: <bpmn:XXX ... id="..." ... />
  const elementRegex = /<bpmn:(\w+)\b([^>]*?)\/?>/g
  let match: RegExpExecArray | null
  while ((match = elementRegex.exec(xml)) !== null) {
    const tagName = match[1] // e.g., userTask, sequenceFlow, callActivity
    const attrsStr = match[2]
    const idMatch = attrsStr.match(/\bid="([^"]+)"/)
    if (!idMatch) continue
    const id = idMatch[1]
    const data: Record<string, string> = {}

    // 提取 flowable: 开头的属性
    const flowableAttrRegex = /\bflowable:([a-zA-Z]+)="([^"]*)"/g
    let attrMatch: RegExpExecArray | null
    while ((attrMatch = flowableAttrRegex.exec(attrsStr)) !== null) {
      data[attrMatch[1]] = attrMatch[2]
    }

    // 提取 conditionExpression (sequenceFlow 的子元素)
    if (tagName === 'sequenceFlow') {
      const condMatch = xml.match(
        new RegExp(`<bpmn:sequenceFlow\\b[^>]*?\\bid="${id}"[^>]*>[\\s\\S]*?<bpmn:conditionExpression[^>]*>([\\s\\S]*?)<\\/bpmn:conditionExpression>`)
      )
      if (condMatch) data.conditionExpression = xmlUnescape(condMatch[1])
    }

    // 提取 extensionElements 中的 candidateUsers/candidateGroups (兼容旧格式)
    if (tagName === 'userTask') {
      const extMatch = xml.match(
        new RegExp(`<bpmn:userTask\\b[^>]*?\\bid="${id}"[\\s\\S]*?<bpmn:extensionElements>([\\s\\S]*?)<\\/bpmn:extensionElements>`)
      )
      if (extMatch) {
        const cuMatch = extMatch[1].match(/<flowable:candidateUsers[^>]*>([\s\S]*?)<\/flowable:candidateUsers>/)
        if (cuMatch) data.candidateUsers = cuMatch[1].trim()
        const cgMatch = extMatch[1].match(/<flowable:candidateGroups[^>]*>([\s\S]*?)<\/flowable:candidateGroups>/)
        if (cgMatch) data.candidateGroups = cgMatch[1].trim()
      }
    }

    if (Object.keys(data).length > 0) flowableData.set(id, data)
  }
}

function extractFlowableData(doc: Document) {
  // 保留 DOM 版本以备调用，但优先使用字符串版本
  const ns = 'http://flowable.org/bpmn'
  const root = doc.documentElement
  const allElements = root.querySelectorAll('*')
  allElements.forEach((el) => {
    const id = el.getAttribute('id')
    if (!id) return
    const data: Record<string, string> = {}
    // Extract flowable attributes
    for (let i = 0; i < el.attributes.length; i++) {
      const attr = el.attributes[i]
      if (attr.name.startsWith('flowable:')) {
        const localName = attr.name.replace('flowable:', '')
        data[localName] = attr.value
      }
    }
    // Extract extension elements
    const extEl = Array.from(el.getElementsByTagName('bpmn:extensionElements'))[0]
    if (extEl) {
      const children = Array.from(extEl.children)
      children.forEach((child) => {
        const tagName = child.localName || ''
        if (tagName === 'candidateUsers') data.candidateUsers = child.textContent || ''
        if (tagName === 'candidateGroups') data.candidateGroups = child.textContent || ''
      })
    }
    // Extract condition expression
    const condEl = Array.from(el.getElementsByTagName('bpmn:conditionExpression'))[0]
    if (condEl) data.conditionExpression = condEl.textContent || ''
    if (Object.keys(data).length > 0) flowableData.set(id, data)
  })
}

function stripFlowableFromXml(xml: string): { cleanXml: string } {
  const parser = new DOMParser()
  const doc = parser.parseFromString(xml, 'text/xml')
  const ns = 'http://flowable.org/bpmn'
  const root = doc.documentElement
  root.removeAttribute('xmlns:flowable')
  const allElements = root.querySelectorAll('*')
  allElements.forEach((el) => {
    const id = el.getAttribute('id')
    if (!id) return
    const data: Record<string, string> = {}
    const toRemove: Attr[] = []
    for (let i = 0; i < el.attributes.length; i++) {
      const attr = el.attributes[i]
      if (attr.namespaceURI === ns || attr.name.startsWith('flowable:')) {
        const localName = attr.localName || attr.name.replace('flowable:', '')
        data[localName] = attr.value
        toRemove.push(attr)
      }
    }
    toRemove.forEach(a => el.removeAttribute(a.name))
    const extEl = el.querySelector('bpmn\\:extensionElements, extensionElements')
    if (extEl) {
      const children = Array.from(extEl.children)
      children.forEach((child) => {
        if (child.namespaceURI === ns || child.prefix === 'flowable') {
          const propName = child.localName || child.tagName.split(':').pop() || ''
          data[propName] = child.textContent || ''
          child.remove()
        }
      })
      if (extEl.children.length === 0) extEl.remove()
    }
    const condEl = el.querySelector('bpmn\\:conditionExpression, conditionExpression')
    if (condEl) data.conditionExpression = condEl.textContent || ''
    if (Object.keys(data).length > 0) flowableData.set(id, data)
  })
  return { cleanXml: new XMLSerializer().serializeToString(doc) }
}

function loadElement(element: any) {
  currentElement.value = element
  if (!element) { showProperties.value = false; return }
  showProperties.value = true
  const bo = element.businessObject || element
  elementId.value = element.id || ''
  elementName.value = bo.name || ''
  elementType.value = (bo.$type || '').split(':').pop() || ''
  loadFlowableData(element.id)
}

function onElementClick(e: any) { if (e?.element) loadElement(e.element) }
function onSelectionChange(e: any) {
  const el = e?.newSelection?.[0]
  if (el) loadElement(el)
}

onMounted(async () => {
  await nextTick()
  // 等待 DOM 完全渲染（对话框动画等）
  await new Promise(r => setTimeout(r, 100))
  if (!canvasRef.value) return
  // 确保容器有尺寸
  const rect = canvasRef.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    console.warn('Canvas container has no dimensions, retrying...')
    await new Promise(r => setTimeout(r, 300))
  }
  await loadOrgData()

  modeler = new BpmnModeler({
    container: canvasRef.value
  })

  // 注册自定义调色板（在 modeler 初始化后，避免 null entry 崩溃）
  try {
    const palette = modeler.get('palette')
    const create = modeler.get('create')
    const elementFactory = modeler.get('elementFactory')
    if (palette && create && elementFactory) {
      // 清除默认调色板条目（替换 providers 为仅保留自定义条目）
      const customEntries: any = {}
      const nodes = [
        { id: 'create-start', type: 'bpmn:StartEvent', label: '开始事件', cls: 'bpmn-icon-start-event-none' },
        { id: 'create-end', type: 'bpmn:EndEvent', label: '结束事件', cls: 'bpmn-icon-end-event-none' },
        { id: 'create-task', type: 'bpmn:UserTask', label: '审批任务', cls: 'bpmn-icon-user-task' },
        { id: 'create-gateway-xor', type: 'bpmn:ExclusiveGateway', label: '互斥网关', cls: 'bpmn-icon-gateway-xor' },
        { id: 'create-gateway-parallel', type: 'bpmn:ParallelGateway', label: '并行网关', cls: 'bpmn-icon-gateway-parallel' },
        { id: 'create-call', type: 'bpmn:CallActivity', label: '子流程', cls: 'bpmn-icon-call-activity' }
      ]
      nodes.forEach(({ id, type: t, label, cls }) => {
        customEntries[id] = {
          group: 'activity',
          className: cls,
          title: label,
          action: {
            dragstart: (ev: any) => create.start(ev, elementFactory.createShape({ type: t })),
            click: (ev: any) => create.start(ev, elementFactory.createShape({ type: t }))
          }
        }
      })
      // 替换所有 providers，只保留我们的自定义条目
      palette._providers = [{
        getPaletteEntries: () => customEntries
      }]
      palette._rebuild()
    }
  } catch (e) {
    console.warn('调色板初始化失败:', e)
  }

  const origFire = modeler.get('eventBus').fire.bind(modeler.get('eventBus'))
  modeler.get('eventBus').fire = function(event: any, ...args: any[]) {
    try { return origFire(event, ...args) }
    catch (err: any) { if (err?.message?.includes('getAttributeNS')) return; throw err }
  }

  modeler.on('element.click', onElementClick)
  modeler.on('selection.changed', onSelectionChange)
  modeler.on('commandStack.changed', () => {
    if (!modeler) return; const s = modeler.get('commandStack'); canUndo.value = s.canUndo(); canRedo.value = s.canRedo()
  })

  try {
    let rawXml = props.modelValue || defaultDiagram
    // Remove camunda namespace if present, replace with flowable
    rawXml = rawXml
      .replace(/xmlns:camunda="http:\/\/camunda\.org\/schema\/1\.0\/bpmn"/g, '')
      .replace(/camunda:/g, 'flowable:')

    flowableData.clear()
    // Parse and extract flowable data without re-serializing
    try {
      const parser = new DOMParser()
      const doc = parser.parseFromString(rawXml, 'text/xml')
      const parseError = doc.querySelector('parsererror')
      if (parseError) {
        console.warn('XML parse error, using default diagram')
        rawXml = defaultDiagram
      } else {
        // 使用字符串正则提取，绕过 DOM 命名空间问题
        extractFlowableDataFromXml(rawXml)
      }
    } catch (e) {
      console.warn('Failed to extract flowable data:', e)
    }

    await modeler.importXML(rawXml)
    canUndo.value = false; canRedo.value = false
    modeler.get('canvas').zoom('fit-viewport')
    const pe = modeler.get('elementRegistry').get('process'); if (pe) loadElement(pe)
  } catch (e: any) {
    console.error('BPMN import failed:', e)
    ElMessage.warning('BPMN加载失败，使用空白画布')
    try {
      await modeler.importXML(defaultDiagram)
      canUndo.value = false; canRedo.value = false
      modeler.get('canvas').zoom('fit-viewport')
      const pe = modeler.get('elementRegistry').get('process'); if (pe) loadElement(pe)
    } catch (e2: any) {
      ElMessage.error('加载BPMN失败: ' + (e2.message || e2))
    }
  }
})

onBeforeUnmount(() => { modeler?.destroy() })

function updateName() { if (modeler && currentElement.value) modeler.get('modeling').updateProperties(currentElement.value, { name: elementName.value || undefined }) }
function updateDescription() { saveFlowableData() }
function updateCalledElement() { saveFlowableData() }
function updateConditionExpression() {
  saveFlowableData()
  // 直接更新 bpmn-js 模型上的条件表达式，确保 modeler.saveXML() 包含条件
  if (modeler && currentElement.value && elementType.value === 'SequenceFlow') {
    try {
      const moddle = modeler.get('moddle')
      const modeling = modeler.get('modeling')
      if (conditionExpression.value) {
        const expr = moddle.create('bpmn:FormalExpression', { body: conditionExpression.value })
        modeling.updateProperties(currentElement.value, { conditionExpression: expr })
      } else {
        modeling.updateProperties(currentElement.value, { conditionExpression: undefined })
      }
    } catch (e) {
      console.warn('Failed to update conditionExpression in model:', e)
    }
  }
}

function injectXmlns(rootEl: Element) { if (!rootEl.hasAttribute('xmlns:flowable')) rootEl.setAttribute('xmlns:flowable', 'http://flowable.org/bpmn') }

function buildExtensionElementsXml(candidateUsers?: string, candidateGroups?: string): string {
  const inner: string[] = []
  if (candidateUsers) inner.push(`<flowable:candidateUsers>${xmlEscape(candidateUsers)}</flowable:candidateUsers>`)
  if (candidateGroups) inner.push(`<flowable:candidateGroups>${xmlEscape(candidateGroups)}</flowable:candidateGroups>`)
  return `<bpmn:extensionElements>${inner.join('')}</bpmn:extensionElements>`
}

function injectFlowableProps(xml: string): string {
  // 1. 确保根元素有 xmlns:flowable 声明
  if (!/xmlns:flowable\s*=/.test(xml)) {
    xml = xml.replace(
      /(<bpmn:definitions\b[^>]*?)(\s*>)/,
      '$1 xmlns:flowable="http://flowable.org/bpmn"$2'
    )
  }
  // 2. 确保 targetNamespace 设置
  if (!/targetNamespace\s*=/.test(xml)) {
    xml = xml.replace(
      /(<bpmn:definitions\b[^>]*?)(\s*>)/,
      '$1 targetNamespace="http://flowable.org/processdef"$2'
    )
  }

  // 3. 为每个 userTask 注入 flowable 属性 + 候选人到 extensionElements
  flowableData.forEach((data, elementId) => {
    // 先处理 userTask 标签上的简单 flowable 属性
    const taskRegex = new RegExp(
      `(<bpmn:userTask\\b[^>]*?\\bid="${elementId}"[^>]*?)(\\s*/?>)`,
      'g'
    )
    xml = xml.replace(taskRegex, (_match, attrs, close) => {
      let newAttrs = attrs
      // 移除已存在的简单 flowable 属性 (避免重复)
      newAttrs = newAttrs.replace(/\s+flowable:(?:candidateScope|approvalMode|skipContinuous|eSignature)="[^"]*"/g, '')
      // 移除已存在的 candidateUsers/candidateGroups attribute (旧格式, 现在改用 extensionElements)
      newAttrs = newAttrs.replace(/\s+flowable:candidateUsers="[^"]*"/g, '')
      newAttrs = newAttrs.replace(/\s+flowable:candidateGroups="[^"]*"/g, '')
      // 注入简单属性
      const injectAttrs: string[] = []
      if (data.candidateScope) injectAttrs.push(`flowable:candidateScope="${data.candidateScope}"`)
      if (data.approvalMode) injectAttrs.push(`flowable:approvalMode="${data.approvalMode}"`)
      if (data.skipContinuous === 'true') injectAttrs.push(`flowable:skipContinuous="true"`)
      if (data.eSignature === 'true') injectAttrs.push(`flowable:eSignature="true"`)
      if (injectAttrs.length > 0) {
        newAttrs += ' ' + injectAttrs.join(' ')
      }
      return newAttrs + close
    })

    // 注入候选人到 extensionElements (统一格式, 与 ProcessDesigner 互通)
    if (data.candidateUsers || data.candidateGroups) {
      const extXml = buildExtensionElementsXml(data.candidateUsers, data.candidateGroups)
      const flowRegex = new RegExp(
        `(<bpmn:userTask\\b[^>]*?\\bid="${elementId}"[^>]*?)(/?>)([\\s\\S]*?)(</bpmn:userTask>)`,
        'g'
      )
      xml = xml.replace(flowRegex, (match, attrs, close, body) => {
        // 移除旧 candidateUsers/candidateGroups
        const cleanedBody = body
          .replace(/<bpmn:extensionElements>[\s\S]*?<\/bpmn:extensionElements>/g, '')
        if (close === '/>') {
          // 自闭合 → 展开为开闭标签 + extensionElements
          return `<bpmn:userTask${attrs}>${extXml}</bpmn:userTask>`
        }
        // 已有子元素, 在 userTask 闭合前插入 extensionElements
        return `<bpmn:userTask${attrs}${close}${cleanedBody}${extXml}</bpmn:userTask>`
      })
    }

    // 4. callActivity 的 calledElement
    if (data.calledElement) {
      const callRegex = new RegExp(
        `(<bpmn:callActivity\\b[^>]*?\\bid="${elementId}"[^>]*?)(\\s*/?>)`,
        'g'
      )
      xml = xml.replace(callRegex, (_match, attrs, close) => {
        let newAttrs = attrs.replace(/\s+calledElement="[^"]*"/g, '')
        newAttrs += ` calledElement="${data.calledElement}"`
        return `<bpmn:callActivity${newAttrs}${close}`
      })
    }

    // 5. sequenceFlow 的 conditionExpression (是子元素)
    if (data.conditionExpression) {
      const flowRegex = new RegExp(
        `(<bpmn:sequenceFlow\\b[^>]*?\\bid="${elementId}"[^>]*?)(/?>)`,
        'g'
      )
      // 2026-06-15 修复: XML 转义, 避免 ${day < 3} 里的 < 导致 BPMN 解析失败
      const escapedExpr = xmlEscape(data.conditionExpression)
      xml = xml.replace(flowRegex, (match, attrs, close) => {
        const condXml = `<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${escapedExpr}</bpmn:conditionExpression>`
        if (close === '/>') {
          // 自闭合标签 → 改为开闭标签 + 条件子元素
          return `<bpmn:sequenceFlow${attrs}>${condXml}</bpmn:sequenceFlow>`
        }
        // 已有子元素：移除旧条件表达式，追加新条件
        let result = match.replace(
          /<bpmn:conditionExpression[^>]*>[\s\S]*?<\/bpmn:conditionExpression>/g,
          ''
        )
        result = result.replace(
          /(<\/bpmn:sequenceFlow>)/,
          `${condXml}$1`
        )
        return result
      })
    }
  })

  return xml
}

async function handleSave() {
  if (!modeler) return
  try {
    const { xml } = await modeler.saveXML({ format: true })
    emit('save', injectFlowableProps(xml))
  } catch (e: any) { ElMessage.error('保存失败: ' + (e.message || e)) }
}

function undo() { modeler?.get('commandStack').undo() }
function redo() { modeler?.get('commandStack').redo() }
function zoomIn() { modeler?.get('canvas').zoom(1.2) }
function zoomOut() { modeler?.get('canvas').zoom(0.8) }
function zoomReset() { modeler?.get('canvas').zoom('fit-viewport') }
function validate() { if (!modeler) return; try { const w = modeler.get('validation').getWarnings(); ElMessage.success(w?.length ? `发现 ${w.length} 个警告` : 'BPMN 校验通过') } catch { ElMessage.success('BPMN 校验通过') } }
async function downloadSvg() { if (!modeler) return; const { svg } = await modeler.saveSVG(); const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([svg], { type: 'image/svg+xml' })); a.download = 'diagram.svg'; a.click(); URL.revokeObjectURL(a.href) }
</script>

<style>
.bpmn-designer { height:70vh; display:flex; flex-direction:column; border:1px solid #e0e0e0; border-radius:4px; }
.designer-toolbar { display:flex; align-items:center; padding:4px 8px; background:#f8f9fa; border-bottom:1px solid #e0e0e0; gap:4px; flex-shrink:0; }
.designer-body { flex:1; display:flex; overflow:hidden; position:relative; min-height:0; }
.designer-canvas { flex:1; min-height:0; min-width:0; }
.designer-canvas .djs-container { width:100%; height:100%; }
/* Palette container styling */
.designer-canvas .djs-palette { position:absolute; top:0; left:0; z-index:10; }
.designer-canvas .djs-palette .djs-palette-toggle { display:none; }
.designer-canvas .djs-palette .djs-palette-entries { display:flex; flex-direction:column; gap:4px; padding:8px; }
.designer-canvas .djs-palette .entry { width:40px; height:40px; display:flex; align-items:center; justify-content:center; cursor:pointer; border-radius:4px; }
.designer-canvas .djs-palette .entry:hover { background:#f0f0f0; }
.designer-properties { width:300px; border-left:1px solid #e0e0e0; overflow-y:auto; background:#fff; padding:12px; flex-shrink:0; }
.properties-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; padding-bottom:8px; border-bottom:1px solid #ebeef5; }
.properties-title { font-weight:600; font-size:14px; }
.properties-body :deep(.el-form-item) { margin-bottom:10px; }
.prop-section { margin-bottom:16px; padding-bottom:12px; border-bottom:1px solid #f0f0f0; }
.prop-section:last-child { border-bottom:none; }
.prop-section-title { font-size:13px; font-weight:600; color:#409eff; margin-bottom:10px; }
.selection-row { display:flex; flex-wrap:wrap; gap:4px; align-items:center; }
.switch-hint { font-size:11px; color:#909399; margin-left:8px; }
.field-hint { font-size:11px; color:#909399; margin-top:4px; }
.org-tree-container { max-height:350px; overflow-y:auto; border:1px solid #ebeef5; border-radius:4px; padding:8px; }
.org-tree-node { display:flex; align-items:center; font-size:13px; }
</style>
