<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="标签名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入标签名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增标签</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="name" label="标签名称" min-width="200" />
        <el-table-column prop="sorting" label="排序" width="80" />
        <el-table-column label="启用状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="info" size="small" @click="handleManageItems(scope.row)">管理内容</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm" top="5vh">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="formData.name" maxlength="64" placeholder="e.g. 高新技术企业" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sorting" :min="0" :max="999" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 管理内容弹窗 -->
    <el-dialog v-model="itemsDialogVisible" :title="`管理内容 - ${currentFocus?.name || ''}`" width="700px" top="3vh">
      <el-button type="success" size="small" @click="handleAddItem" style="margin-bottom: 12px">新增内容</el-button>
      <el-table :data="itemsData" v-loading="itemsLoading" border>
        <el-table-column prop="id" label="ID" width="120" />
        <el-table-column prop="name" label="内容名称" />
        <el-table-column prop="sorting" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEditItem(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDeleteItem(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 内容新增/编辑 -->
    <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="500px" append-to-body>
      <el-form :model="itemFormData" label-width="100px" :rules="itemRules" ref="itemFormRef">
        <el-form-item label="内容名称" prop="name">
          <el-input v-model="itemFormData.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemFormData.sorting" :min="0" :max="999" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFocusPage, createFocus, updateFocus, deleteFocus,
  getFocusItemByFocusId, createFocusItem, updateFocusItem, deleteFocusItem,
  type Focus, type FocusItem
} from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<Focus[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchForm = reactive({ keyword: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('新增标签')
const formData = reactive<Focus>({ id: undefined, name: '', sorting: 0, status: 1 })
const formRef = ref()
const rules = { name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }] }

// 内容管理
const itemsDialogVisible = ref(false)
const itemsLoading = ref(false)
const itemsData = ref<FocusItem[]>([])
const currentFocus = ref<Focus | null>(null)

const itemDialogVisible = ref(false)
const itemDialogTitle = ref('新增内容')
const itemFormData = reactive<FocusItem>({ id: undefined, focusId: undefined, name: '', sorting: 0, status: 1 })
const itemFormRef = ref()
const itemRules = { name: [{ required: true, message: '请输入内容名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getFocusPage({ current: pageNum.value, size: pageSize.value, keyword: searchForm.keyword || undefined, status: searchForm.status })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { pageNum.value = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = undefined; pageNum.value = 1; loadData() }
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => { Object.assign(formData, { id: undefined, name: '', sorting: 0, status: 1 }); dialogTitle.value = '新增标签'; dialogVisible.value = true }
const handleEdit = (row: Focus) => { Object.assign(formData, row); dialogTitle.value = '编辑标签'; dialogVisible.value = true }
const handleDelete = async (row: Focus) => {
  try { await ElMessageBox.confirm(`确认删除标签 [${row.name}]?`, '提示', { type: 'warning' }); await deleteFocus(row.id!); ElMessage.success('删除成功'); loadData() } catch (e) {}
}
const handleSubmit = async () => {
  try { await formRef.value.validate(); if (formData.id) { await updateFocus(formData.id, formData); ElMessage.success('更新成功') } else { await createFocus(formData); ElMessage.success('新增成功') }; dialogVisible.value = false; loadData() } catch (e: any) { if (e?.message) ElMessage.error(e.message) }
}

const handleManageItems = async (row: Focus) => {
  currentFocus.value = row
  itemsDialogVisible.value = true
  itemsLoading.value = true
  try {
    const res: any = await getFocusItemByFocusId(row.id!)
    itemsData.value = res.data || []
  } finally { itemsLoading.value = false }
}

const handleAddItem = () => { Object.assign(itemFormData, { id: undefined, focusId: currentFocus.value?.id, name: '', sorting: 0, status: 1 }); itemDialogTitle.value = '新增内容'; itemDialogVisible.value = true }
const handleEditItem = (row: FocusItem) => { Object.assign(itemFormData, row); itemDialogTitle.value = '编辑内容'; itemDialogVisible.value = true }
const handleDeleteItem = async (row: FocusItem) => {
  try { await ElMessageBox.confirm(`确认删除内容 [${row.name}]?`, '提示', { type: 'warning' }); await deleteFocusItem(row.id!); ElMessage.success('删除成功'); if (currentFocus.value) handleManageItems(currentFocus.value) } catch (e) {}
}
const handleSubmitItem = async () => {
  try {
    await itemFormRef.value.validate()
    if (itemFormData.id) { await updateFocusItem(itemFormData.id, itemFormData); ElMessage.success('更新成功') }
    else { await createFocusItem(itemFormData); ElMessage.success('新增成功') }
    itemDialogVisible.value = false
    if (currentFocus.value) handleManageItems(currentFocus.value)
  } catch (e: any) { if (e?.message) ElMessage.error(e.message) }
}

const resetForm = () => { formRef.value?.resetFields() }
onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
