<template>
  <div class="oper-log-page">
    <div class="search-form">
      <el-form :model="searchForm" inline>
        <el-form-item label="操作模块">
          <el-input v-model="searchForm.title" placeholder="请输入模块名称" style="width: 200px" />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input v-model="searchForm.operName" placeholder="请输入操作人员" style="width: 150px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.businessType" placeholder="请选择">
            <el-option :label="businessTypeOptions[0]" :value="0" />
            <el-option :label="businessTypeOptions[1]" :value="1" />
            <el-option :label="businessTypeOptions[2]" :value="2" />
            <el-option :label="businessTypeOptions[3]" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作状态">
          <el-select v-model="searchForm.status" placeholder="请选择">
            <el-option label="全部" :value="-1" />
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" border>
      <el-table-column prop="title" label="操作模块" width="120" />
      <el-table-column prop="businessType" label="操作类型" width="100">
        <template #default="scope">
          <span>{{ businessTypeOptions[scope.row.businessType] || '未知' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="operName" label="操作人员" width="120" />
      <el-table-column prop="operUrl" label="请求URL" min-width="200" />
      <el-table-column prop="requestMethod" label="请求方法" width="100" />
      <el-table-column prop="operIp" label="操作IP" width="120" />
      <el-table-column prop="status" label="操作状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
            {{ scope.row.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时(ms)" width="100" />
      <el-table-column prop="operTime" label="操作时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" @click="viewDetail(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="pageNum"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    />

    <!-- 详情弹窗 -->
    <el-dialog title="操作日志详情" :visible="showDetail" width="60%">
      <el-form label-width="100px">
        <el-form-item label="操作模块">
          <span>{{ detailData.title }}</span>
        </el-form-item>
        <el-form-item label="操作类型">
          <span>{{ detailData.businessType != null ? businessTypeOptions[detailData.businessType] || '未知' : '未知' }}</span>
        </el-form-item>
        <el-form-item label="操作人员">
          <span>{{ detailData.operName }}</span>
        </el-form-item>
        <el-form-item label="请求URL">
          <span>{{ detailData.operUrl }}</span>
        </el-form-item>
        <el-form-item label="请求方法">
          <span>{{ detailData.requestMethod }}</span>
        </el-form-item>
        <el-form-item label="操作IP">
          <span>{{ detailData.operIp }}</span>
        </el-form-item>
        <el-form-item label="操作时间">
          <span>{{ detailData.operTime }}</span>
        </el-form-item>
        <el-form-item label="消耗时间">
          <span>{{ detailData.costTime }} ms</span>
        </el-form-item>
        <el-form-item label="请求参数">
          <pre class="code-block">{{ detailData.operParam }}</pre>
        </el-form-item>
        <el-form-item label="响应结果">
          <pre class="code-block">{{ detailData.jsonResult }}</pre>
        </el-form-item>
        <el-form-item label="错误信息" v-if="detailData.status === 1">
          <pre class="code-block error">{{ detailData.errorMsg }}</pre>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { getOperLogPage } from '@/api/log'

const businessTypeOptions = ['其它', '新增', '修改', '删除']

const searchForm = reactive({
  title: '',
  operName: '',
  businessType: -1,
  status: -1,
  dateRange: []
})

const tableData = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showDetail = ref(false)
const detailData = reactive<{
  title?: string
  businessType?: number
  operName?: string
  operUrl?: string
  requestMethod?: string
  operIp?: string
  operTime?: string
  costTime?: number
  operParam?: string
  jsonResult?: string
  status?: number
  errorMsg?: string
}>({})

const handleSearch = async () => {
  pageNum.value = 1
  await fetchData()
}

const handleReset = () => {
  searchForm.title = ''
  searchForm.operName = ''
  searchForm.businessType = -1
  searchForm.status = -1
  searchForm.dateRange = []
  pageNum.value = 1
  fetchData()
}

const fetchData = async () => {
  const params: any = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }
  if (searchForm.title) params.title = searchForm.title
  if (searchForm.operName) params.operName = searchForm.operName
  if (searchForm.businessType >= 0) params.businessType = searchForm.businessType
  if (searchForm.status >= 0) params.status = searchForm.status
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startTime = searchForm.dateRange[0]
    params.endTime = searchForm.dateRange[1]
  }

  const res = await getOperLogPage(params)
  if (res.data) {
    tableData.value = res.data.list
    total.value = res.data.total
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchData()
}

const handleCurrentChange = (page: number) => {
  pageNum.value = page
  fetchData()
}

const viewDetail = (row: any) => {
  Object.assign(detailData, row)
  showDetail.value = true
}

fetchData()
</script>

<style scoped>
.oper-log-page {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.code-block {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

.code-block.error {
  background: #fff2f0;
  color: #d93026;
}
</style>
