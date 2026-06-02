<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>我的待办</span>
          <el-button @click="fetchTodo">刷新</el-button>
        </div>
      </template>

      <el-table :data="todoList" v-loading="todoLoading" border stripe>
        <el-table-column prop="name" label="任务名称" min-width="140" />
        <el-table-column prop="processInstanceId" label="流程实例" width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="办理人" width="160">
          <template #default="{ row }">
            <span v-if="row.assignee">{{ row.assignee }}</span>
            <el-tag v-else-if="row.candidateUsers?.length" size="small" type="warning">
              候选: {{ row.candidateUsers.join(', ') }}
            </el-tag>
            <span v-else style="color:#909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="70" align="center" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleApprove(row)">审批</el-button>
            <el-button type="warning" link @click="handleTransfer(row)">转办</el-button>
            <el-button v-if="!row.assignee" type="success" link @click="handleClaim(row)">签收</el-button>
            <el-button v-else type="info" link @click="handleUnclaim(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="todoTotal>0" v-model:current-page="todoQuery.pageNum"
        v-model:page-size="todoQuery.pageSize" :total="todoTotal" :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next" style="margin-top:16px" @change="fetchTodo" />
    </el-card>

    <el-card style="margin-top:16px">
      <template #header><span>我的已办</span></template>
      <el-table :data="doneList" v-loading="doneLoading" border stripe>
        <el-table-column prop="name" label="任务名称" min-width="140" />
        <el-table-column prop="processDefinitionName" label="所属流程" width="140" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="完成时间" width="180">
          <template #default="{ row }">{{ formatDate(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="deleteReason" label="结果" width="100" />
      </el-table>
      <el-pagination v-if="doneTotal>0" v-model:current-page="doneQuery.pageNum"
        v-model:page-size="doneQuery.pageSize" :total="doneTotal" :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next" style="margin-top:16px" @change="fetchDone" />
    </el-card>

    <el-dialog v-model="approveVisible" title="审批任务" width="550px">
      <el-descriptions :column="1" border style="margin-bottom:16px">
        <el-descriptions-item label="流程名称">{{ currentTask?.processDefinitionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask?.name }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(currentTask?.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="approveComment" type="textarea" :rows="4" placeholder="请输入审批意见" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleReject" type="danger" :loading="approveLoading" :disabled="!approveComment.trim()">驳回</el-button>
        <el-button @click="approveVisible=false">取消</el-button>
        <el-button type="primary" :loading="approveLoading" @click="handleApproveSubmit">通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="转办任务" width="400px">
      <el-form label-width="80px">
        <el-form-item label="转交人">
          <el-input v-model="transferUserId" placeholder="用户ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible=false">取消</el-button>
        <el-button type="primary" @click="handleTransferSubmit">确定转办</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTodoTasks, getDoneTasks, completeTask, rejectTask, transferTask, claimTask, unclaimTask } from '../../api/workflow'
import { ElMessageBox } from 'element-plus'

const username = localStorage.getItem('username') || 'admin'

function formatDate(dt: any): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const todoList = ref<any[]>([])
const todoTotal = ref(0)
const todoLoading = ref(false)
const todoQuery = reactive({ userId: username, pageNum: 1, pageSize: 10 })

const doneList = ref<any[]>([])
const doneTotal = ref(0)
const doneLoading = ref(false)
const doneQuery = reactive({ userId: username, pageNum: 1, pageSize: 10 })

const approveVisible = ref(false)
const approveLoading = ref(false)
const approveComment = ref('')
const currentTask = ref<any>(null)

const transferVisible = ref(false)
const transferUserId = ref('')
const transferTaskId = ref('')

onMounted(() => { fetchTodo(); fetchDone() })

async function fetchTodo() {
  todoLoading.value = true
  try { const r = await getTodoTasks(todoQuery); todoList.value = r.data.records || []; todoTotal.value = r.data.total || 0 } finally { todoLoading.value = false }
}

async function fetchDone() {
  doneLoading.value = true
  try { const r = await getDoneTasks(doneQuery); doneList.value = r.data.records || []; doneTotal.value = r.data.total || 0 } finally { doneLoading.value = false }
}

function handleApprove(row: any) {
  currentTask.value = row
  approveComment.value = ''
  approveVisible.value = true
}

async function handleApproveSubmit() {
  if (!approveComment.value.trim()) { ElMessage.warning('请输入审批意见'); return }
  approveLoading.value = true
  try {
    await completeTask(currentTask.value.id, {}, approveComment.value, username)
    ElMessage.success('审批通过')
    approveVisible.value = false; fetchTodo(); fetchDone()
  } finally { approveLoading.value = false }
}

async function handleReject() {
  if (!approveComment.value.trim()) { ElMessage.warning('请输入驳回意见'); return }
  approveLoading.value = true
  try {
    await rejectTask(currentTask.value.id, approveComment.value, username)
    ElMessage.success('已驳回')
    approveVisible.value = false; fetchTodo(); fetchDone()
  } finally { approveLoading.value = false }
}

function handleTransfer(row: any) {
  transferTaskId.value = row.id
  transferUserId.value = ''
  transferVisible.value = true
}

async function handleClaim(row: any) {
  try {
    await ElMessageBox.confirm('确认签收此任务？', '签收提示')
    await claimTask(row.id, username)
    ElMessage.success('签收成功')
    fetchTodo()
  } catch { /* cancel or error */ }
}

async function handleUnclaim(row: any) {
  try {
    await ElMessageBox.confirm('确认退回任务？退回后可从候选任务中重新签收', '退回提示')
    await unclaimTask(row.id)
    ElMessage.success('已退回')
    fetchTodo()
  } catch { /* cancel or error */ }
}

async function handleTransferSubmit() {
  await transferTask(transferTaskId.value, transferUserId.value, username)
  ElMessage.success('转办成功')
  transferVisible.value = false
  fetchTodo()
}
</script>
