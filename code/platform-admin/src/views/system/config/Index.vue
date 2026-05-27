<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="参数名称/键名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="keyword = ''; handleSearch()">重置</el-button>
          <el-button type="success" @click="handleAdd" v-permission="'system:config:add'">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="configName" label="名称" width="160" />
        <el-table-column prop="configKey" label="键名" width="200" />
        <el-table-column prop="configValue" label="键值" />
        <el-table-column label="系统内置" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.configType === 1 ? 'info' : 'warning'" size="small">
              {{ scope.row.configType === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)" v-permission="'system:config:edit'">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)" v-permission="'system:config:del'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="键名"><el-input v-model="form.configKey" :disabled="!!editId" /></el-form-item>
        <el-form-item label="键值"><el-input v-model="form.configValue" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="系统内置">
          <el-radio-group v-model="form.configType">
            <el-radio :label="1">是</el-radio><el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigPage, createConfig, updateConfig, deleteConfig } from '@/api/config'

const loading = ref(false)
const tableData = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editId = ref('')
const form = ref<any>({ configName: '', configKey: '', configValue: '', configType: 0, remark: '' })

async function loadData() {
  loading.value = true
  try {
    const res: any = await getConfigPage({ keyword: keyword.value || undefined })
    if (res.code === 200) tableData.value = res.data.records
  } finally { loading.value = false }
}

function handleSearch() { loadData() }

function handleAdd() {
  editId.value = ''
  dialogTitle.value = '新增参数'
  form.value = { configName: '', configKey: '', configValue: '', configType: 0, remark: '' }
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editId.value = row.id
  dialogTitle.value = '编辑参数'
  form.value = { configName: row.configName, configKey: row.configKey, configValue: row.configValue, configType: row.configType, remark: row.remark }
  dialogVisible.value = true
}

async function handleSave() {
  if (editId.value) {
    const res: any = await updateConfig(editId.value, form.value)
    if (res.code === 200) ElMessage.success('更新成功')
  } else {
    const res: any = await createConfig(form.value)
    if (res.code === 200) ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该参数？', '提示', { type: 'warning' })
    const res: any = await deleteConfig(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 20px; }
.search-card { margin-bottom: 20px; }
.table-card { margin-bottom: 20px; }
</style>
