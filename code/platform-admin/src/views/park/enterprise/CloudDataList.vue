<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="企业名称">
          <el-select
            v-model="searchForm.enterpriseId"
            placeholder="搜索企业"
            clearable
            filterable
            remote
            :remote-method="searchEnterprises"
            :loading="enterpriseLoading"
            style="width: 280px"
          >
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业名关键词">
          <el-input v-model="searchForm.keyword" placeholder="企业名称关键词" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="数据年份">
          <el-input v-model="searchForm.dataYear" placeholder="e.g. 2024" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column prop="dataYear" label="数据年份" width="100" />
        <el-table-column prop="dataDate" label="数据日期" width="110" />
        <el-table-column label="数据内容" min-width="200" :show-overflow-tooltip="true">
          <template #default="scope">
            <span>{{ truncateText(scope.row.dataContent, 60) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" :show-overflow-tooltip="true" />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" @close="resetForm" top="5vh">
      <el-form :model="formData" label-width="110px" :rules="rules" ref="formRef">
        <el-form-item label="企业" prop="enterpriseId">
          <el-select
            v-model="formData.enterpriseId"
            placeholder="搜索选择企业"
            filterable
            remote
            :remote-method="searchEnterprises"
            :loading="enterpriseLoading"
            style="width: 100%"
          >
            <el-option
              v-for="e in enterpriseOptions"
              :key="e.id"
              :label="e.name"
              :value="e.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据年份" prop="dataYear">
          <el-input v-model="formData.dataYear" maxlength="10" placeholder="e.g. 2024" />
        </el-form-item>
        <el-form-item label="数据日期" prop="dataDate">
          <el-date-picker
            v-model="formData.dataDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="数据内容" prop="dataContent">
          <el-input v-model="formData.dataContent" type="textarea" :rows="4" maxlength="2000" placeholder="请输入数据内容" />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cloudDataPage, cloudDataCreate, cloudDataUpdate, cloudDataDelete,
  CLOUD_CATEGORY_LABELS, CLOUD_CATEGORY_LIST,
  type CloudData, type CloudDataCategory
} from '@/api/enterprise-cloud'
import { getEnterprisePage, type Enterprise } from '@/api/enterprise'

const route = useRoute()
const category = computed<CloudDataCategory>(() => (route.query.category as CloudDataCategory) || 'business_risk')
const pageTitle = computed(() => CLOUD_CATEGORY_LABELS[category.value] || '云企库数据')

const loading = ref(false)
const tableData = ref<CloudData[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const enterpriseLoading = ref(false)
const enterpriseOptions = ref<Enterprise[]>([])

const searchForm = reactive({
  enterpriseId: undefined as string | undefined,
  keyword: '',
  dataYear: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增数据')
const formData = reactive<Partial<CloudData>>({
  id: undefined,
  enterpriseId: '',
  dataYear: '',
  dataDate: '',
  dataContent: '',
  sortOrder: 0,
  status: 1,
  remark: '',
})
const formRef = ref()

const rules = {
  enterpriseId: [{ required: true, message: '请选择企业', trigger: 'change' }],
  dataYear: [{ required: true, message: '请输入数据年份', trigger: 'blur' }],
  dataContent: [{ required: true, message: '请输入数据内容', trigger: 'blur' }],
}

function truncateText(text: string | undefined, len: number): string {
  if (!text) return '-'
  return text.length > len ? text.substring(0, len) + '...' : text
}

async function searchEnterprises(keyword: string) {
  enterpriseLoading.value = true
  try {
    const res: any = await getEnterprisePage({ keyword, pageNum: 1, pageSize: 50 })
    if (res.code === 200) {
      enterpriseOptions.value = res.data?.records || []
    }
  } catch { /* ignore */ }
  finally { enterpriseLoading.value = false }
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await cloudDataPage({
      category: category.value,
      enterpriseId: searchForm.enterpriseId,
      keyword: searchForm.keyword || undefined,
      dataYear: searchForm.dataYear || undefined,
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
  searchForm.enterpriseId = undefined
  searchForm.keyword = ''
  searchForm.dataYear = ''
  pageNum.value = 1
  loadData()
}
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => {
  dialogTitle.value = `新增${pageTitle.value}数据`
  Object.assign(formData, {
    id: undefined,
    enterpriseId: '',
    dataYear: '',
    dataDate: '',
    dataContent: '',
    sortOrder: 0,
    status: 1,
    remark: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row: CloudData) => {
  dialogTitle.value = `编辑${pageTitle.value}数据`
  Object.assign(formData, {
    id: row.id,
    enterpriseId: row.enterpriseId,
    dataYear: row.dataYear,
    dataDate: row.dataDate,
    dataContent: row.dataContent,
    sortOrder: row.sortOrder,
    status: row.status,
    remark: row.remark,
  })
  dialogVisible.value = true
}

const handleDelete = async (row: CloudData) => {
  try {
    await ElMessageBox.confirm(`确认删除企业「${row.enterpriseName}」的${pageTitle.value}数据?`, '提示', { type: 'warning' })
    await cloudDataDelete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // user cancelled or error
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const payload = {
      ...formData,
      category: category.value,
    }
    if (formData.id) {
      await cloudDataUpdate(formData.id, payload)
      ElMessage.success('更新成功')
    } else {
      await cloudDataCreate(payload)
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

onMounted(() => {
  searchEnterprises('')
  loadData()
})
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
