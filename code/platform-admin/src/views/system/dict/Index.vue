<template>
  <div class="dict-container">
    <el-card class="dict-type-card">
      <template #header>
        <div class="card-header">
          <span>字典类型</span>
          <el-button type="primary" size="small" @click="handleAddType" v-permission="'system:dict:add'">新增</el-button>
        </div>
      </template>
      <el-input v-model="typeKeyword" placeholder="搜索字典名称/编码" size="small" clearable @input="loadTypes" />
      <el-table :data="typeList" highlight-current-row @current-change="handleTypeChange" size="small" style="margin-top:10px">
        <el-table-column prop="dictName" label="字典名称" />
        <el-table-column prop="dictType" label="编码" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button text size="small" @click.stop="handleEditType(scope.row)" v-permission="'system:dict:edit'">编辑</el-button>
            <el-button text size="small" type="danger" @click.stop="handleDeleteType(scope.row)" v-permission="'system:dict:del'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="dict-data-card">
      <template #header>
        <div class="card-header">
          <span>字典数据{{ currentType ? ' - ' + currentType.dictName : '' }}</span>
          <el-button type="primary" size="small" :disabled="!currentType" @click="handleAddData" v-permission="'system:dict:add'">新增</el-button>
        </div>
      </template>
      <el-table :data="dataList" size="small" v-loading="dataLoading">
        <el-table-column prop="dictLabel" label="标签" />
        <el-table-column prop="dictValue" label="键值" />
        <el-table-column prop="dictSort" label="排序" width="60" />
        <el-table-column prop="cssClass" label="样式" width="80" />
        <el-table-column label="状态" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button text size="small" @click="handleEditData(scope.row)" v-permission="'system:dict:edit'">编辑</el-button>
            <el-button text size="small" type="danger" @click="handleDeleteData(scope.row)" v-permission="'system:dict:del'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="typeDialog" :title="typeFormTitle" width="450px">
      <el-form :model="typeForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="typeForm.dictName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="typeForm.dictType" :disabled="!!editTypeId" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :label="1">启用</el-radio><el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="typeForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveType">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dataDialog" :title="dataFormTitle" width="450px">
      <el-form :model="dataForm" label-width="80px">
        <el-form-item label="标签"><el-input v-model="dataForm.dictLabel" /></el-form-item>
        <el-form-item label="键值"><el-input v-model="dataForm.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dataForm.dictSort" :min="0" /></el-form-item>
        <el-form-item label="样式"><el-input v-model="dataForm.cssClass" placeholder="如: success / danger" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :label="1">启用</el-radio><el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="dataForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDictTypePage, createDictType, updateDictType, deleteDictType,
  getDictDataPage, createDictData, updateDictData, deleteDictData
} from '@/api/dict'


const typeKeyword = ref('')
const typeList = ref<any[]>([])
const currentType = ref<any>(null)
const dataList = ref<any[]>([])
const dataLoading = ref(false)

const typeDialog = ref(false)
const typeFormTitle = ref('')
const editTypeId = ref('')
const typeForm = ref<any>({ dictName: '', dictType: '', status: 1, remark: '' })

const dataDialog = ref(false)
const dataFormTitle = ref('')
const editDataId = ref('')
const dataForm = ref<any>({ dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', status: 1, remark: '' })

async function loadTypes() {
  const res: any = await getDictTypePage({ keyword: typeKeyword.value || undefined })
  if (res.code === 200) typeList.value = res.data.records
}

async function loadData() {
  if (!currentType.value) return
  dataLoading.value = true
  try {
    const res: any = await getDictDataPage({ dictType: currentType.value.dictType })
    if (res.code === 200) dataList.value = res.data.records
  } finally { dataLoading.value = false }
}

function handleTypeChange(row: any) {
  currentType.value = row
  loadData()
}

function handleAddType() {
  editTypeId.value = ''
  typeFormTitle.value = '新增字典类型'
  typeForm.value = { dictName: '', dictType: '', status: 1, remark: '' }
  typeDialog.value = true
}

function handleEditType(row: any) {
  editTypeId.value = row.id
  typeFormTitle.value = '编辑字典类型'
  typeForm.value = { dictName: row.dictName, dictType: row.dictType, status: row.status, remark: row.remark }
  typeDialog.value = true
}

async function handleSaveType() {
  if (editTypeId.value) {
    const res: any = await updateDictType(editTypeId.value, typeForm.value)
    if (res.code === 200) ElMessage.success('更新成功')
  } else {
    const res: any = await createDictType(typeForm.value)
    if (res.code === 200) ElMessage.success('新增成功')
  }
  typeDialog.value = false
  loadTypes()
}

async function handleDeleteType(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该字典类型？关联的数据将一并删除', '提示', { type: 'warning' })
    const res: any = await deleteDictType(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (currentType.value?.id === row.id) { currentType.value = null; dataList.value = [] }
      loadTypes()
    }
  } catch { /* cancelled */ }
}

function handleAddData() {
  editDataId.value = ''
  dataFormTitle.value = '新增字典数据'
  dataForm.value = { dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', status: 1, remark: '' }
  dataDialog.value = true
}

function handleEditData(row: any) {
  editDataId.value = row.id
  dataFormTitle.value = '编辑字典数据'
  dataForm.value = { dictLabel: row.dictLabel, dictValue: row.dictValue, dictSort: row.dictSort, cssClass: row.cssClass, status: row.status, remark: row.remark }
  dataDialog.value = true
}

async function handleSaveData() {
  dataForm.value.dictType = currentType.value.dictType
  if (editDataId.value) {
    const res: any = await updateDictData(editDataId.value, dataForm.value)
    if (res.code === 200) ElMessage.success('更新成功')
  } else {
    const res: any = await createDictData(dataForm.value)
    if (res.code === 200) ElMessage.success('新增成功')
  }
  dataDialog.value = false
  loadData()
}

async function handleDeleteData(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该字典数据？', '提示', { type: 'warning' })
    const res: any = await deleteDictData(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { /* cancelled */ }
}

onMounted(() => loadTypes())
</script>

<style scoped>
.dict-container { display: flex; gap: 16px; padding: 20px; height: calc(100vh - 120px); }
.dict-type-card { width: 320px; flex-shrink: 0; }
.dict-data-card { flex: 1; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
