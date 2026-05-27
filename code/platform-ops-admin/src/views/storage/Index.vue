<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>对象存储配置</span>
          <el-button type="primary" @click="handleAdd">新增配置</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="endpoint" label="Endpoint" width="200" />
        <el-table-column prop="defaultBucket" label="默认Bucket" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleTest(row)">测试</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Endpoint" prop="endpoint"><el-input v-model="form.endpoint" placeholder="http://localhost:9000" /></el-form-item>
        <el-form-item label="Access Key" prop="accessKey"><el-input v-model="form.accessKey" /></el-form-item>
        <el-form-item label="Secret Key" prop="secretKey"><el-input v-model="form.secretKey" type="password" show-password /></el-form-item>
        <el-form-item label="默认Bucket"><el-input v-model="form.defaultBucket" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { page, create, update, remove, testConnection } from '../../api/storage'

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const form = reactive({ name: '', endpoint: '', accessKey: '', secretKey: '', defaultBucket: '', remark: '' })
const rules = { name: [{ required: true, message: '请输入名称' }], endpoint: [{ required: true, message: '请输入Endpoint' }] }

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try { const res = await page({ pageNum: 1, pageSize: 100 }); list.value = res.data.records || [] } finally { loading.value = false }
}

function handleAdd() { isEdit.value = false; editId.value = null; Object.assign(form, { name: '', endpoint: '', accessKey: '', secretKey: '', defaultBucket: '', remark: '' }); dialogVisible.value = true }
function handleEdit(row: any) { isEdit.value = true; editId.value = row.id; Object.assign(form, row); dialogVisible.value = true }
async function handleDelete(row: any) { await ElMessageBox.confirm('确定删除？', '提示'); await remove(row.id); ElMessage.success('已删除'); fetchData() }
async function handleTest(row: any) { const res = await testConnection(row.id); ElMessage.success(res.data ? '连接成功' : '连接失败') }

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && editId.value) { await update(editId.value, form); ElMessage.success('已更新') }
    else { await create(form); ElMessage.success('已创建') }
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}
</script>
