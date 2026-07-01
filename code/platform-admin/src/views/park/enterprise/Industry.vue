<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="行业代码">
          <el-input v-model="searchForm.keyword" placeholder="代码/门类/大类/小类" clearable />
        </el-form-item>
        <el-form-item label="门类">
          <el-input v-model="searchForm.category" placeholder="e.g. 信息传输" clearable />
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
          <el-button type="success" @click="handleAdd">新增行业</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="code" label="行业代码" width="120" />
        <el-table-column prop="category" label="门类" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="categoryBig" label="大类" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="categoryMiddle" label="中类" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="categorySmall" label="小类" min-width="200" :show-overflow-tooltip="true" />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm" top="5vh">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="行业代码" prop="code">
          <el-input v-model="formData.code" maxlength="16" placeholder="GB/T 4754-2017 字母+数字" />
        </el-form-item>
        <el-form-item label="门类" prop="category">
          <el-input v-model="formData.category" maxlength="64" placeholder="e.g. 信息传输/软件和信息技术服务业" />
        </el-form-item>
        <el-form-item label="大类">
          <el-input v-model="formData.categoryBig" maxlength="64" />
        </el-form-item>
        <el-form-item label="中类">
          <el-input v-model="formData.categoryMiddle" maxlength="64" />
        </el-form-item>
        <el-form-item label="小类">
          <el-input v-model="formData.categorySmall" maxlength="64" />
        </el-form-item>
        <el-form-item label="状态">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getIndustryPage, createIndustry, updateIndustry, deleteIndustry,
  type EnterpriseIndustry
} from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<EnterpriseIndustry[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  category: '',
  status: undefined as number | undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增行业')
const formData = reactive<EnterpriseIndustry>({
  id: undefined,
  code: '',
  category: '',
  categoryBig: '',
  categoryMiddle: '',
  categorySmall: '',
  status: 1
})
const formRef = ref()

const rules = {
  code: [{ required: true, message: '请输入行业代码', trigger: 'blur' }],
  category: [{ required: true, message: '请输入门类', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getIndustryPage({
      current: pageNum.value,
      size: pageSize.value,
      keyword: searchForm.keyword || undefined,
      category: searchForm.category || undefined,
      status: searchForm.status
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
  searchForm.keyword = ''; searchForm.category = ''; searchForm.status = undefined
  pageNum.value = 1; loadData()
}
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => {
  dialogTitle.value = '新增行业'
  Object.assign(formData, { id: undefined, code: '', category: '', categoryBig: '', categoryMiddle: '', categorySmall: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: EnterpriseIndustry) => {
  dialogTitle.value = '编辑行业'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: EnterpriseIndustry) => {
  try {
    await ElMessageBox.confirm(`确认删除行业 [${row.code}] ${row.category}?`, '提示', { type: 'warning' })
    await deleteIndustry(row.id!)
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
      await updateIndustry(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createIndustry(formData)
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
