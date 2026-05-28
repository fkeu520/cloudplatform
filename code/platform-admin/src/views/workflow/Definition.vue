<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>流程定义</span>
          <div>
            <el-button type="primary" @click="handleNew">新建流程</el-button>
            <el-button @click="fetchData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="流程名称" min-width="140" />
        <el-table-column prop="key" label="流程Key" width="120" />
        <el-table-column prop="version" label="版本" width="60" align="center" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.suspended ? 'danger' : 'success'">
              {{ row.suspended ? '已挂起' : '激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deployTime" label="部署时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDesign(row)">设计</el-button>
            <el-button type="primary" link @click="handleStart(row)">发起</el-button>
            <el-button v-if="row.suspended" type="success" link @click="handleToggle(row)">激活</el-button>
            <el-button v-else type="warning" link @click="handleToggle(row)">挂起</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total>0" v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize" :total="total" :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next" style="margin-top:16px" @change="fetchData" />
    </el-card>

    <el-dialog v-model="designVisible" :title="designTitle" width="90%" top="3vh"
      :close-on-click-modal="false" destroy-on-close>
      <el-form :inline="true" style="margin-bottom:12px">
        <el-form-item label="流程名称">
          <el-input v-model="designForm.processName" placeholder="必填" />
        </el-form-item>
        <el-form-item label="流程Key">
          <el-input v-model="designForm.processKey" placeholder="必填，唯一标识" :disabled="!!designForm.editingId" />
        </el-form-item>
        <el-form-item label="设计器">
          <el-switch v-model="designForm.useNewDesigner" active-text="新版" inactive-text="旧版" />
        </el-form-item>
      </el-form>
      <ProcessDesigner v-if="!designForm.useNewDesigner" ref="designerRef" v-model="designForm.bpmnXml" @publish="handleDeploySubmit" />
      <BpmnDesigner v-else ref="bpmnDesignerRef" :model-value="designForm.bpmnXml" @save="onBpmnSave" />
      <template #footer>
        <el-button @click="designVisible=false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="startVisible" title="发起流程" width="450px">
      <el-form label-width="100px">
        <el-form-item label="流程">
          <el-input :model-value="startForm.processDefinitionName" disabled />
        </el-form-item>
        <el-form-item label="业务Key">
          <el-input v-model="startForm.businessKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible=false">取消</el-button>
        <el-button type="primary" :loading="startLoading" @click="handleStartSubmit">发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDefinitionPage, deployDefinition, suspendDefinition, activateDefinition, deleteDefinition, getDefinitionXml, startInstance } from '../../api/workflow'
import ProcessDesigner from '@/components/ProcessDesigner.vue'
import BpmnDesigner from '@/components/BpmnDesigner.vue'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })

const designerRef = ref()
const bpmnDesignerRef = ref()
const designVisible = ref(false)
const designTitle = ref('新建流程')
const deployLoading = ref(false)
const designForm = reactive({ processName: '', processKey: '', bpmnXml: '', editingId: '', useNewDesigner: false })

const startVisible = ref(false)
const startLoading = ref(false)
const startForm = reactive({ processDefinitionKey: '', processDefinitionName: '', businessKey: '' })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getDefinitionPage(query)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

function handleNew() {
  designTitle.value = '新建流程'
  designForm.processName = ''
  designForm.processKey = ''
  designForm.bpmnXml = ''
  designForm.editingId = ''
  designVisible.value = true
}

async function handleDesign(row: any) {
  designTitle.value = `编辑流程 - ${row.name}`
  designForm.processName = row.name || ''
  designForm.processKey = row.key || ''
  designForm.editingId = row.id
  const res = await getDefinitionXml(row.id)
  designForm.bpmnXml = res.data?.bpmnXml || ''
  designVisible.value = true
}

async function handleDeploySubmit() {
  if (!designForm.processName) { ElMessage.warning('请输入流程名称'); return }
  if (!designForm.processKey) { ElMessage.warning('请输入流程 Key'); return }
  const bpmnXml = designerRef.value?.generateBpmnXml?.() || designForm.bpmnXml
  if (!bpmnXml || bpmnXml === '<?xml') { ElMessage.warning('请先设计流程'); return }
  deployLoading.value = true
  try {
    await deployDefinition({ processName: designForm.processName, bpmnXml })
    ElMessage.success('部署成功')
    designVisible.value = false
    fetchData()
  } finally { deployLoading.value = false }
}

function onBpmnSave(xml: string) {
  designForm.bpmnXml = xml
  handleDeploySubmit()
}

async function handleToggle(row: any) {
  try {
    if (row.suspended) { await activateDefinition(row.id); ElMessage.success('已激活') }
    else { await suspendDefinition(row.id); ElMessage.success('已挂起') }
    fetchData()
  } catch { /* ignore */ }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除此流程定义？', '提示')
  await deleteDefinition(row.id)
  ElMessage.success('已删除')
  fetchData()
}

function handleStart(row: any) {
  startForm.processDefinitionKey = row.key
  startForm.processDefinitionName = row.name
  startForm.businessKey = ''
  startVisible.value = true
}

async function handleStartSubmit() {
  startLoading.value = true
  try {
    const userId = localStorage.getItem('userId') || '1'
    await startInstance({ processDefinitionKey: startForm.processDefinitionKey, businessKey: startForm.businessKey, userId })
    ElMessage.success('流程已发起')
    startVisible.value = false
  } finally { startLoading.value = false }
}
</script>
