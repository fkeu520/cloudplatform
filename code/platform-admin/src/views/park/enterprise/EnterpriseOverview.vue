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
            style="width: 320px"
          >
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业名关键词">
          <el-input v-model="searchForm.keyword" placeholder="企业名称关键词" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增概览</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column prop="regCapital" label="注册资本" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="totalAssets" label="总资产" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="annualRevenue" label="年营收" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="employeeCount" label="员工数" width="90" align="right" />
        <el-table-column prop="patentCount" label="专利数" width="80" align="right" />
        <el-table-column prop="trademarkCount" label="商标数" width="80" align="right" />
        <el-table-column prop="riskCount" label="风险数" width="80" align="right" />
        <el-table-column prop="bidCount" label="中标数" width="80" align="right" />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm" top="3vh">
      <el-form :model="formData" label-width="120px" :rules="rules" ref="formRef">
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

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="注册资本">
              <el-input v-model="formData.regCapital" placeholder="元" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总资产">
              <el-input v-model="formData.totalAssets" placeholder="元" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年营收">
              <el-input v-model="formData.annualRevenue" placeholder="元" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="员工数">
              <el-input-number v-model="formData.employeeCount" :min="0" :max="9999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="专利数">
              <el-input-number v-model="formData.patentCount" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商标数">
              <el-input-number v-model="formData.trademarkCount" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="著作权数">
              <el-input-number v-model="formData.copyrightCount" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险数">
              <el-input-number v-model="formData.riskCount" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="中标数">
              <el-input-number v-model="formData.bidCount" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="股权结构(JSON)">
          <el-input v-model="formData.equityStructureJson" type="textarea" :rows="2" placeholder="JSON 格式" maxlength="2000" />
        </el-form-item>
        <el-form-item label="概览(JSON)">
          <el-input v-model="formData.overviewJson" type="textarea" :rows="2" placeholder="JSON 格式" maxlength="2000" />
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
  overviewPage, overviewCreate, overviewUpdate, overviewDelete,
  type EnterpriseOverview
} from '@/api/enterprise-cloud'
import { getEnterprisePage, type Enterprise } from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<EnterpriseOverview[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const enterpriseLoading = ref(false)
const enterpriseOptions = ref<Enterprise[]>([])

const searchForm = reactive({
  enterpriseId: undefined as string | undefined,
  keyword: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增概览')
const formData = reactive<Partial<EnterpriseOverview>>({
  id: undefined,
  enterpriseId: '',
  regCapital: '',
  totalAssets: '',
  annualRevenue: '',
  employeeCount: 0,
  patentCount: 0,
  trademarkCount: 0,
  copyrightCount: 0,
  riskCount: 0,
  bidCount: 0,
  equityStructureJson: '',
  overviewJson: '',
  sortOrder: 0,
  status: 1,
  remark: '',
})
const formRef = ref()

const rules = {
  enterpriseId: [{ required: true, message: '请选择企业', trigger: 'change' }],
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
    const res: any = await overviewPage({
      enterpriseId: searchForm.enterpriseId,
      keyword: searchForm.keyword || undefined,
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
  pageNum.value = 1
  loadData()
}
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => {
  dialogTitle.value = '新增概览'
  Object.assign(formData, {
    id: undefined, enterpriseId: '',
    regCapital: '', totalAssets: '', annualRevenue: '',
    employeeCount: 0, patentCount: 0, trademarkCount: 0,
    copyrightCount: 0, riskCount: 0, bidCount: 0,
    equityStructureJson: '', overviewJson: '',
    sortOrder: 0, status: 1, remark: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row: EnterpriseOverview) => {
  dialogTitle.value = '编辑概览'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: EnterpriseOverview) => {
  try {
    await ElMessageBox.confirm(`确认删除企业「${row.enterpriseName}」的概览数据?`, '提示', { type: 'warning' })
    await overviewDelete(row.id!)
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
      await overviewUpdate(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await overviewCreate(formData)
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
