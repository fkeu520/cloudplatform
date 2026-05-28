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
      <div class="designer-properties" v-if="selectedElement">
        <div class="properties-header">
          <span class="properties-title">元素配置</span>
          <el-tag size="small">{{ selectedElement.businessObject?.$type?.split(':').pop() }}</el-tag>
        </div>
        <div class="properties-body">
          <el-form label-width="70px" size="small">
            <el-form-item label="ID">
              <el-input :model-value="selectedElement.id" disabled />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="elementName" @change="updateElementName" />
            </el-form-item>
            <el-form-item v-if="isUserTask" label="办理人">
              <el-input v-model="elementAssignee" @change="updateElementAssignee" />
            </el-form-item>
            <el-form-item v-if="isUserTask" label="候选用户">
              <el-input v-model="elementCandidates" @change="updateElementCandidates" placeholder="用逗号分隔" />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import BpmnModeler from 'bpmn-js/lib/Modeler'

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
const selectedElement = ref<any>(null)
const elementName = ref('')
const elementAssignee = ref('')
const elementCandidates = ref('')

const isUserTask = computed(() =>
  selectedElement.value?.businessObject?.$type === 'bpmn:UserTask'
)

function getModelerValue(prop: string) {
  return selectedElement.value?.businessObject?.get(prop) || ''
}

const defaultDiagram = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="process" isExecutable="true">
    <bpmn:startEvent id="start" name="开始" />
    <bpmn:endEvent id="end" name="结束" />
    <bpmn:sequenceFlow id="flow1" sourceRef="start" targetRef="end" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process">
      <bpmndi:BPMNShape id="start_shape" bpmnElement="start">
        <dc:Bounds x="100" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="end_shape" bpmnElement="end">
        <dc:Bounds x="300" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow1_edge" bpmnElement="flow1">
        <di:waypoint x="136" y="178" />
        <di:waypoint x="300" y="178" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

function onSelectionChange(e: any) {
  selectedElement.value = e.newSelection?.[0]
    ? modeler?.get('elementRegistry').get(e.newSelection[0])
    : null
  if (selectedElement.value) {
    elementName.value = getModelerValue('name')
    elementAssignee.value = getModelerValue('assignee')
    const candUsers = getModelerValue('candidateUsers')
    elementCandidates.value = Array.isArray(candUsers) ? candUsers.join(', ') : (candUsers || '')
  }
}

onMounted(async () => {
  await nextTick()
  if (!canvasRef.value) return
  modeler = new BpmnModeler({
    container: canvasRef.value
  })
  modeler.on('selection.changed', onSelectionChange)
  try {
    await modeler.importXML(props.modelValue || defaultDiagram)
    updateUndo()
    modeler.get('canvas').zoom('fit-viewport')
    modeler.on('commandStack.changed', updateUndo)
  } catch (e: any) {
    ElMessage.error('加载BPMN失败: ' + (e.message || e))
  }
})

onBeforeUnmount(() => {
  modeler?.destroy()
})

function updateUndo() {
  if (!modeler) return
  const stack = modeler.get('commandStack')
  canUndo.value = stack.canUndo()
  canRedo.value = stack.canRedo()
}

function updateElementName() {
  updateModelerProp('name', elementName.value)
}
function updateElementAssignee() {
  updateModelerProp('assignee', elementAssignee.value)
}
function updateElementCandidates() {
  const val = elementCandidates.value.split(',').map((s: string) => s.trim()).filter(Boolean)
  updateModelerProp('candidateUsers', val)
}

function updateModelerProp(prop: string, val: any) {
  const bo = selectedElement.value?.businessObject
  if (bo) {
    bo.set(prop, val)
    modeler?.get('eventBus').fire('element.changed', { element: selectedElement.value })
  }
}

function undo() { modeler?.get('commandStack').undo() }
function redo() { modeler?.get('commandStack').redo() }
function zoomIn() { modeler?.get('canvas').zoom(1.2) }
function zoomOut() { modeler?.get('canvas').zoom(0.8) }
function zoomReset() { modeler?.get('canvas').zoom('fit-viewport') }

function validate() {
  if (!modeler) return
  const { warnings } = modeler.get('validation')
  if (warnings.length === 0) {
    ElMessage.success('BPMN 校验通过')
  } else {
    ElMessage.warning(`发现 ${warnings.length} 个警告`)
    warnings.forEach((w: any) => console.warn(w.message || w))
  }
}

async function handleSave() {
  if (!modeler) return
  try {
    const { xml } = await modeler.saveXML({ format: true })
    emit('save', xml)
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || e))
  }
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

<style scoped>
.bpmn-designer {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.designer-toolbar {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  gap: 4px;
}
.designer-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.designer-canvas {
  flex: 1;
  height: 100%;
  min-height: 400px;
}
.designer-properties {
  width: 280px;
  border-left: 1px solid #e0e0e0;
  overflow-y: auto;
  background: #fff;
  padding: 12px;
}
.properties-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.properties-title { font-weight: 600; font-size: 14px; }
.properties-body :deep(.el-form-item) { margin-bottom: 12px; }
</style>
