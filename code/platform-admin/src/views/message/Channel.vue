<template>
  <div class="channel-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>渠道配置</span>
          <el-button type="primary" size="small" @click="openAdd">新增渠道</el-button>
        </div>
      </template>

      <el-table :data="channelList" v-loading="loading" border stripe>
        <el-table-column prop="channelCode" label="渠道编码" width="120" />
        <el-table-column prop="channelName" label="渠道名称" width="160" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配置参数" min-width="300">
          <template #default="{ row }">
            <el-input v-model="row.configJson" type="textarea" :rows="3" readonly
              style="cursor:pointer" @click="editChannel(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="editChannel(row)">编辑</el-button>
            <el-button type="success" link @click="testChannel(row)">测试</el-button>
            <el-popconfirm title="确定删除此渠道？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑渠道' : '新增渠道'" width="650px">
      <el-form :model="formData" label-width="100px" ref="formRef">
        <el-form-item label="渠道编码" prop="channelCode" required>
          <el-input v-model="formData.channelCode" placeholder="如: sms/email/app_push" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="渠道名称" prop="channelName" required>
          <el-input v-model="formData.channelName" placeholder="如: 阿里云短信" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="配置参数" prop="configJson">
          <el-alert title="JSON 格式配置，短信渠道示例：{&quot;accessKeyId&quot;:&quot;&quot;,&quot;accessKeySecret&quot;:&quot;&quot;,&quot;signName&quot;:&quot;&quot;,&quot;regionId&quot;:&quot;cn-hangzhou&quot;}" type="info" :closable="false" style="margin-bottom:8px" />
          <el-input v-model="formDataTempJson" type="textarea" :rows="8" placeholder="请输入JSON配置" />
          <div v-if="jsonError" style="color:#f56c6c;font-size:12px;margin-top:4px">{{ jsonError }}</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testVisible" title="发送测试消息" width="450px">
      <el-form label-width="100px">
        <el-form-item label="渠道">
          <el-tag>{{ testChannelName }}</el-tag>
        </el-form-item>
        <el-form-item label="接收地址">
          <el-input v-model="testAddress" placeholder="手机号/邮箱/用户ID" />
        </el-form-item>
        <el-form-item label="测试内容">
          <el-input v-model="testContent" type="textarea" :rows="3" placeholder="测试消息内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" :loading="testSending" @click="handleTestSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getChannelList, createChannel, updateChannel, deleteChannel, sendTestMessage } from '@/api/message'

const loading = ref(false)
const saving = ref(false)
const testSending = ref(false)
const channelList = ref<any[]>([])
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const jsonError = ref('')

const formData = ref<any>({ channelCode: '', channelName: '', status: 1, configJson: '{}', remark: '' })
const formDataTempJson = ref('{}')

watch(formVisible, (v) => { if (!v) { jsonError.value = '' } })

const testVisible = ref(false)
const testChannelName = ref('')
const testChannelCode = ref('')
const testAddress = ref('')
const testContent = ref('测试消息')

async function fetchChannels() {
  loading.value = true
  try {
    const res = await getChannelList() as any
    channelList.value = res?.data || res || []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  formData.value = { channelCode: '', channelName: '', status: 1, configJson: '{}', remark: '' }
  formDataTempJson.value = '{}'
  jsonError.value = ''
  formVisible.value = true
}

function editChannel(row: any) {
  isEdit.value = true
  formData.value = { ...row }
  formDataTempJson.value = row.configJson || '{}'
  jsonError.value = ''
  formVisible.value = true
}

function validateJson(str: string): boolean {
  try {
    JSON.parse(str)
    return true
  } catch {
    return false
  }
}

async function handleSave() {
  if (!formData.value.channelCode || !formData.value.channelName) {
    ElMessage.warning('请填写渠道编码和名称')
    return
  }
  if (!validateJson(formDataTempJson.value)) {
    jsonError.value = 'JSON 格式错误，请检查'
    return
  }
  jsonError.value = ''
  formData.value.configJson = formDataTempJson.value
  saving.value = true
  try {
    if (isEdit.value) {
      await updateChannel(formData.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createChannel(formData.value)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    fetchChannels()
  } finally {
    saving.value = false
  }
}

function testChannel(row: any) {
  testChannelCode.value = row.channelCode
  testChannelName.value = row.channelName
  testAddress.value = ''
  testContent.value = '这是一条测试消息，用于验证渠道连通性。'
  testVisible.value = true
}

async function handleTestSend() {
  if (!testAddress.value) { ElMessage.warning('请输入接收地址'); return }
  testSending.value = true
  try {
    await sendTestMessage({ channelCode: testChannelCode.value, receiverAddress: testAddress.value, content: testContent.value })
    ElMessage.success('测试消息已发送，请检查接收方')
    testVisible.value = false
  } finally {
    testSending.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await deleteChannel(row.id)
    ElMessage.success('删除成功')
    fetchChannels()
  } catch { /* ignore */ }
}

onMounted(() => { fetchChannels() })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
