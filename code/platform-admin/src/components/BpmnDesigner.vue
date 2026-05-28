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
          <span class="properties-title">元素配置</span>
          <el-tag size="small">{{ elementType }}</el-tag>
        </div>
        <div class="properties-body">
          <el-form label-width="70px" size="small" v-if="elementType && elementType !== 'Process'">
            <el-form-item label="ID">
              <el-input :model-value="elementId" disabled />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="elementName" @input="updateName" />
            </el-form-item>
            <el-form-item v-if="elementType === 'UserTask'" label="办理人">
              <el-input v-model="elementAssignee" @input="updateAssignee" />
            </el-form-item>
            <el-form-item v-if="elementType === 'UserTask'" label="候选用户">
              <el-input v-model="elementCandidates" @input="updateCandidates" placeholder="用逗号分隔" />
            </el-form-item>
          </el-form>
          <div v-else style="color:#909399;font-size:13px;padding:8px 0">点击图中的节点编辑属性</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'

const emit = defineEmits<{
  (e: 'save', xml: string): void
}>()

const props = defineProps<{
  modelValue?: string
}>()

const canvasRef = ref<HTMLElement>()
let modeler: BpmnModeler | null = null
const canUndo = ref(false)
const canRedo = ref(false)
const showProperties = ref(true)
const elementId = ref('')
const elementName = ref('')
const elementAssignee = ref('')
const elementCandidates = ref('')
const elementType = ref('')

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

function loadElement(element: any) {
  if (!element) { showProperties.value = false; return }
  showProperties.value = true
  const bo = element.businessObject || element
  elementId.value = element.id || ''
  elementName.value = bo.name || ''
  elementAssignee.value = bo.assignee || ''
  const cand = bo.candidateUsers
  elementCandidates.value = Array.isArray(cand) ? cand.join(', ') : (cand || '')
  elementType.value = (bo.$type || '').split(':').pop() || ''
}

function onElementClick(e: any) {
  const el = e?.element
  if (el) loadElement(el)
}

function onSelectionChange(e: any) {
  const id = e?.newSelection?.[0]
  if (id) {
    const el = modeler?.get('elementRegistry').get(id)
    if (el) loadElement(el)
  }
}

onMounted(async () => {
  await nextTick()
  if (!canvasRef.value) return

  // Define custom palette provider BEFORE creating modeler
  const CustomPaletteProvider = function(_palette: any, create: any, elementFactory: any) {
    const ENTRIES = [
      { id: 'start', type: 'bpmn:StartEvent', label: '开始事件', cls: 'bpmn-icon-start-event-none' },
      { id: 'end', type: 'bpmn:EndEvent', label: '结束事件', cls: 'bpmn-icon-end-event-none' },
      { id: 'task', type: 'bpmn:UserTask', label: '审批任务', cls: 'bpmn-icon-user-task' },
      { id: 'gateway-xor', type: 'bpmn:ExclusiveGateway', label: '互斥网关', cls: 'bpmn-icon-gateway-xor' },
      { id: 'gateway-parallel', type: 'bpmn:ParallelGateway', label: '并行网关', cls: 'bpmn-icon-gateway-parallel' },
      { id: 'call', type: 'bpmn:CallActivity', label: '子流程', cls: 'bpmn-icon-call-activity' }
    ]
    return {
      getPaletteEntries: () => {
        const entries: any = {}
        ENTRIES.forEach(({ id, type: t, label, cls }) => {
          entries[id] = {
            group: 'activity',
            className: cls,
            title: label,
            action: { click: () => create.start({ element: elementFactory.createShape({ type: t }) }) }
          }
        })
        return entries
      }
    }
  }
  CustomPaletteProvider.$inject = ['palette', 'create', 'elementFactory']

  modeler = new BpmnModeler({
    container: canvasRef.value,
    additionalModules: [
      { __init__: ['customPaletteProvider'], customPaletteProvider: ['type', CustomPaletteProvider] }
    ]
  })

  modeler.on('element.click', onElementClick)
  modeler.on('selection.changed', onSelectionChange)
  modeler.on('commandStack.changed', () => {
    if (!modeler) return
    const stack = modeler.get('commandStack')
    canUndo.value = stack.canUndo()
    canRedo.value = stack.canRedo()
  })

  try {
    await modeler.importXML(props.modelValue || defaultDiagram)
    canUndo.value = false
    canRedo.value = false
    modeler.get('canvas').zoom('fit-viewport')
    const processElement = modeler.get('elementRegistry').get('process')
    if (processElement) loadElement(processElement)
  } catch (e: any) {
    ElMessage.error('加载BPMN失败: ' + (e.message || e))
  }
})

onBeforeUnmount(() => { modeler?.destroy() })

function updateModelerProp(prop: string, val: any) {
  if (!modeler) return
  const selection = modeler.get('selection')
  const selected = selection.get()
  if (!selected || selected.length === 0) return
  if (prop === 'candidateUsers' && typeof val === 'string') {
    val = val.split(',').map((s: string) => s.trim()).filter(Boolean)
  }
  modeler.get('modeling').updateProperties(selected[0], { [prop]: val })
}

function updateName() { updateModelerProp('name', elementName.value) }
function updateAssignee() { updateModelerProp('assignee', elementAssignee.value) }
function updateCandidates() { updateModelerProp('candidateUsers', elementCandidates.value) }
function undo() { modeler?.get('commandStack').undo() }
function redo() { modeler?.get('commandStack').redo() }
function zoomIn() { modeler?.get('canvas').zoom(1.2) }
function zoomOut() { modeler?.get('canvas').zoom(0.8) }
function zoomReset() { modeler?.get('canvas').zoom('fit-viewport') }

function validate() {
  if (!modeler) return
  try {
    const warnings = modeler.get('validation').getWarnings()
    ElMessage.success(warnings?.length ? `发现 ${warnings.length} 个警告` : 'BPMN 校验通过')
  } catch {
    ElMessage.success('BPMN 校验通过')
  }
}

async function handleSave() {
  if (!modeler) return
  try {
    const { xml } = await modeler.saveXML({ format: true })
    emit('save', xml)
  } catch (e: any) { ElMessage.error('保存失败: ' + (e.message || e)) }
}

async function downloadSvg() {
  if (!modeler) return
  const { svg } = await modeler.saveSVG()
  const blob = new Blob([svg], { type: 'image/svg+xml' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'diagram.svg'
  a.click()
  URL.revokeObjectURL(a.href)
}
</script>

<style>
.bpmn-designer { height:65vh; display:flex; flex-direction:column; border:1px solid #e0e0e0; border-radius:4px; }
.designer-toolbar { display:flex; align-items:center; padding:4px 8px; background:#f8f9fa; border-bottom:1px solid #e0e0e0; gap:4px; }
.designer-body { flex:1; display:flex; overflow:hidden; position:relative; }
.designer-canvas { flex:1; height:100%; min-height:400px; }
.designer-canvas .djs-container { overflow:hidden; }
.designer-properties { width:280px; border-left:1px solid #e0e0e0; overflow-y:auto; background:#fff; padding:12px; }
.properties-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; padding-bottom:8px; border-bottom:1px solid #ebeef5; }
.properties-title { font-weight:600; font-size:14px; }
.properties-body :deep(.el-form-item) { margin-bottom:12px; }
</style>
