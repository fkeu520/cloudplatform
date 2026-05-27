<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>服务网关管理</span>
          <el-button type="primary" @click="handleAdd">新增路由</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="routeName" label="路由名称" width="140" />
        <el-table-column prop="routeId" label="路由ID" width="120" />
        <el-table-column prop="uri" label="目标URI" width="200" />
        <el-table-column prop="orderNo" label="排序" width="60" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.status===1" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑路由':'新增路由'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="路由名称" prop="routeName"><el-input v-model="form.routeName" /></el-form-item>
        <el-form-item label="路由ID" prop="routeId"><el-input v-model="form.routeId" :disabled="isEdit" /></el-form-item>
        <el-form-item label="目标URI" prop="uri"><el-input v-model="form.uri" placeholder="http://localhost:8081" /></el-form-item>
        <el-form-item label="谓词"><el-input v-model="form.predicates" type="textarea" :rows="3" placeholder='[{"name":"Path","args":{"pattern":"/user/**"}}]' /></el-form-item>
        <el-form-item label="过滤器"><el-input v-model="form.filters" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.orderNo" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
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
import { page, create, update, remove, toggleStatus } from '../../api/gatewayRoute'

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const form = reactive({ routeName: '', routeId: '', uri: '', predicates: '', filters: '', orderNo: 0, remark: '' })
const rules = { routeName: [{ required: true, message: '请输入路由名称' }], routeId: [{ required: true, message: '请输入路由ID' }], uri: [{ required: true, message: '请输入目标URI' }] }

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try { const res = await page({ pageNum: 1, pageSize: 100 }); list.value = res.data.records || [] } finally { loading.value = false }
}
function handleAdd() { isEdit.value = false; editId.value = null; Object.assign(form, { routeName: '', routeId: '', uri: '', predicates: '', filters: '', orderNo: 0, remark: '' }); dialogVisible.value = true }
function handleEdit(row: any) { isEdit.value = true; editId.value = row.id; Object.assign(form, row); dialogVisible.value = true }
async function handleDelete(row: any) { await ElMessageBox.confirm('确定删除？', '提示'); await remove(row.id); ElMessage.success('已删除'); fetchData() }
async function handleToggle(row: any) { await toggleStatus(row.id); ElMessage.success('已切换'); fetchData() }
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
