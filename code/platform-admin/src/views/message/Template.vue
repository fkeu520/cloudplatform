<template>
  <div class="template-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>短信模板</span>
          <div>
            <el-select v-model="filterChannel" placeholder="渠道筛选" clearable size="small" style="width:140px;margin-right:8px" @change="onFilter">
              <el-option label="站内信" value="site" />
              <el-option label="短信" value="sms" />
            </el-select>
            <el-button type="primary" size="small" @click="openAdd">新增模板</el-button>
          </div>
        </div>
      </template>

      <el-table :data="templateList" v-loading="loading" border stripe>
        <el-table-column prop="templateCode" label="模板编码" width="140" />
        <el-table-column prop="templateName" label="模板名称" width="160" show-overflow-tooltip />
        <el-table-column label="所属渠道" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.channelCode === 'sms'" type="warning" size="small">短信</el-tag>
            <el-tag v-else-if="row.channelCode === 'site'" type="primary" size="small">站内信</el-tag>
            <span v-else>{{ row.channelCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="signName" label="短信签名" width="120" />
        <el-table-column prop="templateId" label="第三方模板ID" width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模板内容" min-width="300">
          <template #default="{ row }">
            <span style="font-size:13px;color:#606266;">{{ row.templateContent }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="editTemplate(row)">编辑</el-button>
            <el-popconfirm title="确定删除此模板？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        style="margin-top:16px" @change="fetchTemplates" />
    </el-card>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="700px">
      <el-form :model="formData" label-width="120px">
        <el-form-item label="模板编码" required>
          <el-input v-model="formData.templateCode" placeholder="如: SMS_VERIFY_CODE" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="formData.templateName" placeholder="如: 短信-验证码模板" />
        </el-form-item>
        <el-form-item label="所属渠道" required>
          <el-select v-model="formData.channelCode" style="width:100%" :disabled="isEdit">
            <el-option label="站内信" value="site" />
            <el-option label="短信" value="sms" />
          </el-select>
        </el-form-item>
        <el-form-item label="短信签名">
          <el-input v-model="formData.signName" placeholder="短信渠道需要填写签名" :disabled="formData.channelCode !== 'sms'" />
        </el-form-item>
        <el-form-item label="第三方模板ID">
          <el-input v-model="formData.templateId" placeholder="阿里云短信模板CODE" :disabled="formData.channelCode !== 'sms'" />
        </el-form-item>
        <el-form-item label="模板内容" required>
          <el-alert title="使用 {name} 格式的变量占位符，如：您的验证码为 {code}，有效期5分钟。" type="info" :closable="false" style="margin-bottom:8px" />
          <el-input v-model="formData.templateContent" type="textarea" :rows="4" placeholder="模板内容" />
        </el-form-item>
        <el-form-item label="变量定义">
          <div style="width:100%">
            <el-alert title="定义模板中用到的变量，格式：[{&quot;name&quot;:&quot;code&quot;,&quot;desc&quot;:&quot;验证码&quot;}]" type="info" :closable="false" style="margin-bottom:8px" />
            <el-input v-model="formDataTempParams" type="textarea" :rows="3" placeholder="变量定义JSON" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTemplatePage, createTemplate, updateTemplate, deleteTemplate } from '@/api/message'

const loading = ref(false)
const saving = ref(false)
const templateList = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterChannel = ref('')
const formVisible = ref(false)
const isEdit = ref(false)
const formDataTempParams = ref('[]')

const formData = ref<any>({
  templateCode: '', templateName: '', channelCode: 'sms', signName: '',
  templateId: '', templateContent: '', paramsJson: '[]', status: 1
})

async function fetchTemplates() {
  loading.value = true
  try {
    const res = await getTemplatePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      channelCode: filterChannel.value || undefined,
      keyword: undefined
    }) as any
    const data = res?.data || res
    templateList.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function onFilter() {
  pageNum.value = 1
  fetchTemplates()
}

function openAdd() {
  isEdit.value = false
  formData.value = { templateCode: '', templateName: '', channelCode: 'sms', signName: '', templateId: '', templateContent: '', paramsJson: '[]', status: 1 }
  formDataTempParams.value = '[]'
  formVisible.value = true
}

function editTemplate(row: any) {
  isEdit.value = true
  formData.value = { ...row }
  formDataTempParams.value = row.paramsJson || '[]'
  formVisible.value = true
}

async function handleSave() {
  if (!formData.value.templateCode || !formData.value.templateName || !formData.value.templateContent) {
    ElMessage.warning('请填写模板编码、名称和内容')
    return
  }
  try {
    JSON.parse(formDataTempParams.value)
  } catch {
    ElMessage.warning('变量定义 JSON 格式错误')
    return
  }
  formData.value.paramsJson = formDataTempParams.value
  saving.value = true
  try {
    if (isEdit.value) {
      await updateTemplate(formData.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(formData.value)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    fetchTemplates()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    fetchTemplates()
  } catch { /* ignore */ }
}

onMounted(() => { fetchTemplates() })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
