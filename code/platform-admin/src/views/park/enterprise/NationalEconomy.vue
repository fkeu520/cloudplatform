<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="代码/名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 120px">
            <el-option label="门类" :value="1" />
            <el-option label="大类" :value="2" />
            <el-option label="中类" :value="3" />
            <el-option label="小类" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级代码">
          <el-input v-model="searchForm.parentCode" placeholder="上级代码" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增分类</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="code" label="分类代码" width="130" />
        <el-table-column prop="name" label="分类名称" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column label="级别" width="80">
          <template #default="scope">
            <el-tag :type="getLevelTagType(scope.row.level)" size="small">
              {{ getLevelLabel(scope.row.level) }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-form-item label="分类代码" prop="code">
          <el-input v-model="formData.code" maxlength="16" placeholder="国民经济行业分类代码" />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" maxlength="128" placeholder="国民经济行业分类名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="级别" prop="level">
              <el-select v-model="formData.level" placeholder="选择级别" style="width: 100%">
                <el-option label="门类" :value="1" />
                <el-option label="大类" :value="2" />
                <el-option label="中类" :value="3" />
                <el-option label="小类" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级代码">
              <el-input v-model="formData.parentCode" maxlength="16" placeholder="上级分类代码（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
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
  economyPage, economyCreate, economyUpdate, economyDelete,
  type NationalEconomy
} from '@/api/enterprise-cloud'

const loading = ref(false)
const tableData = ref<NationalEconomy[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  level: undefined as number | undefined,
  parentCode: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formData = reactive<Partial<NationalEconomy>>({
  id: undefined,
  code: '',
  name: '',
  level: 1,
  parentCode: '',
  sortOrder: 0,
  status: 1,
  remark: '',
})
const formRef = ref()

const rules = {
  code: [{ required: true, message: '请输入分类代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
}

const levelLabels: Record<number, string> = { 1: '门类', 2: '大类', 3: '中类', 4: '小类' }

function getLevelLabel(level?: number): string {
  return level ? levelLabels[level] || `级别${level}` : '-'
}

function getLevelTagType(level?: number): string {
  switch (level) {
    case 1: return 'danger'
    case 2: return 'warning'
    case 3: return ''
    case 4: return 'info'
    default: return 'info'
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await economyPage({
      keyword: searchForm.keyword || undefined,
      level: searchForm.level,
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
  searchForm.level = undefined
  searchForm.parentCode = ''
  pageNum.value = 1
  loadData()
}
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  Object.assign(formData, { id: undefined, code: '', name: '', level: 1, parentCode: '', sortOrder: 0, status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: NationalEconomy) => {
  dialogTitle.value = '编辑分类'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: NationalEconomy) => {
  try {
    await ElMessageBox.confirm(`确认删除分类 [${row.code}] ${row.name}?`, '提示', { type: 'warning' })
    await economyDelete(row.id!)
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
      await economyUpdate(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await economyCreate(formData)
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
