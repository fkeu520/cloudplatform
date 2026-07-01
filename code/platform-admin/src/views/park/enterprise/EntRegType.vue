<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="代码/名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="上级代码">
          <el-input v-model="searchForm.parentCode" placeholder="上级代码" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增类型</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="code" label="类型代码" width="130" />
        <el-table-column prop="name" label="类型名称" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="parentCode" label="上级代码" width="120" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" @close="resetForm" top="5vh">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="类型代码" prop="code">
          <el-input v-model="formData.code" maxlength="16" placeholder="企业注册类型代码" />
        </el-form-item>
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="formData.name" maxlength="64" placeholder="企业注册类型名称" />
        </el-form-item>
        <el-form-item label="上级代码">
          <el-input v-model="formData.parentCode" maxlength="16" placeholder="上级类型代码（可选）" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="formData.sortOrder" :min="0" :max="99999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="formData.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  regTypePage, regTypeCreate, regTypeUpdate, regTypeDelete,
  type EntRegType
} from '@/api/enterprise-cloud'

const loading = ref(false)
const tableData = ref<EntRegType[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  parentCode: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增注册类型')
const formData = reactive<Partial<EntRegType>>({
  id: undefined,
  code: '',
  name: '',
  parentCode: '',
  sortOrder: 0,
  status: 1,
  remark: '',
})
const formRef = ref()

const rules = {
  code: [{ required: true, message: '请输入类型代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await regTypePage({
      keyword: searchForm.keyword || undefined,
      parentCode: searchForm.parentCode || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pageNum.value = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.parentCode = ''
  pageNum.value = 1
  loadData()
}
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => {
  dialogTitle.value = '新增注册类型'
  Object.assign(formData, { id: undefined, code: '', name: '', parentCode: '', sortOrder: 0, status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: EntRegType) => {
  dialogTitle.value = '编辑注册类型'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: EntRegType) => {
  try {
    await ElMessageBox.confirm(`确认删除注册类型 [${row.code}] ${row.name}?`, '提示', { type: 'warning' })
    await regTypeDelete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // user cancelled or error
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (formData.id) {
      await regTypeUpdate(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await regTypeCreate(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
