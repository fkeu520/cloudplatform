<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="操作日志" name="oper" />
          <el-tab-pane label="登录日志" name="login" />
        </el-tabs>
      </template>

      <div v-if="activeTab==='oper'">
        <div class="search-form">
          <el-form :model="operForm" inline>
            <el-form-item label="操作模块">
              <el-input v-model="operForm.title" placeholder="请输入" style="width:200px" clearable />
            </el-form-item>
            <el-form-item label="操作人员">
              <el-input v-model="operForm.operName" placeholder="请输入" style="width:150px" clearable />
            </el-form-item>
            <el-form-item label="操作类型">
              <el-select v-model="operForm.businessType" placeholder="请选择" clearable style="width:120px">
                <el-option label="其它" :value="0" />
                <el-option label="新增" :value="1" />
                <el-option label="修改" :value="2" />
                <el-option label="删除" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="operForm.status" placeholder="请选择" clearable style="width:100px">
                <el-option label="成功" :value="0" />
                <el-option label="失败" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="operForm.dateRange" type="daterange" range-separator="至"
                start-placeholder="开始" end-placeholder="结束" style="width:260px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchOper(1)">查询</el-button>
              <el-button @click="resetOper">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-table :data="operLogs" v-loading="operLoading" border stripe>
          <el-table-column prop="title" label="模块" width="100" />
          <el-table-column prop="businessType" label="类型" width="70">
            <template #default="{ row }">{{ ['其它','新增','修改','删除'][row.businessType] || '未知' }}</template>
          </el-table-column>
          <el-table-column prop="operName" label="操作人员" width="90" />
          <el-table-column prop="operUrl" label="请求URL" min-width="200" show-overflow-tooltip />
          <el-table-column prop="requestMethod" label="请求方式" width="80" />
          <el-table-column prop="operIp" label="IP" width="130" />
          <el-table-column prop="status" label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status===0?'success':'danger'">{{ row.status===0?'正常':'异常' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costTime" label="耗时(ms)" width="80" />
          <el-table-column prop="operTime" label="操作时间" width="170" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link size="small" @click="viewOperDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="operTotal>0" @size-change="(s:number)=>{operSize=s;searchOper(1)}"
          @current-change="(p:number)=>searchOper(p)" :current-page="operPage" :page-sizes="[10,20,50]"
          :page-size="operSize" layout="total,sizes,prev,pager,next" :total="operTotal" />
      </div>

      <div v-if="activeTab==='login'">
        <div class="search-form">
          <el-form :model="loginForm" inline>
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入" style="width:150px" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="loginForm.status" placeholder="请选择" clearable style="width:100px">
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="loginForm.dateRange" type="daterange" range-separator="至"
                start-placeholder="开始" end-placeholder="结束" style="width:260px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchLogin(1)">查询</el-button>
              <el-button @click="resetLogin">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-table :data="loginLogs" v-loading="loginLoading" border stripe>
          <el-table-column prop="username" label="用户名" width="100" />
          <el-table-column prop="loginType" label="类型" width="70">
            <template #default="{ row }">{{ ['密码','短信','第三方'][row.loginType] || '其他' }}</template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="userType" label="用户类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.userType===2?'danger':row.userType===1?'warning':'info'" size="small">
                {{ row.userType===0?'普通用户':row.userType===1?'租户管理员':'运营管理员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="消息" min-width="160" />
          <el-table-column prop="status" label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':'danger'">{{ row.status===1?'成功':'失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="loginTime" label="登录时间" width="170" />
        </el-table>
        <el-pagination v-if="loginTotal>0" @size-change="(s:number)=>{loginSize=s;searchLogin(1)}"
          @current-change="(p:number)=>searchLogin(p)" :current-page="loginPage" :page-sizes="[10,20,50]"
          :page-size="loginSize" layout="total,sizes,prev,pager,next" :total="loginTotal" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="60%">
      <el-form label-width="100px">
        <el-form-item label="操作模块"><span>{{ operDetail.title }}</span></el-form-item>
        <el-form-item label="操作类型"><span>{{ ['其它','新增','修改','删除'][operDetail.businessType] || '未知' }}</span></el-form-item>
        <el-form-item label="操作人员"><span>{{ operDetail.operName }}</span></el-form-item>
        <el-form-item label="请求URL"><span>{{ operDetail.operUrl }}</span></el-form-item>
        <el-form-item label="请求方法"><span>{{ operDetail.requestMethod }}</span></el-form-item>
        <el-form-item label="操作IP"><span>{{ operDetail.operIp }}</span></el-form-item>
        <el-form-item label="操作时间"><span>{{ operDetail.operTime }}</span></el-form-item>
        <el-form-item label="消耗时间"><span>{{ operDetail.costTime }} ms</span></el-form-item>
        <el-form-item label="请求参数"><pre class="code-block">{{ operDetail.operParam }}</pre></el-form-item>
        <el-form-item label="响应结果"><pre class="code-block">{{ operDetail.jsonResult }}</pre></el-form-item>
        <el-form-item v-if="operDetail.status===1" label="错误信息"><pre class="code-block error">{{ operDetail.errorMsg }}</pre></el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getOperLogPage, getLoginLogPage } from '@/api/log'

const activeTab = ref('oper')

// 操作日志
const operLoading = ref(false)
const operLogs = ref<any[]>([])
const operPage = ref(1)
const operSize = ref(10)
const operTotal = ref(0)
const operForm = reactive({
  title: '', operName: '', businessType: undefined as number | undefined,
  status: undefined as number | undefined, dateRange: [] as any[]
})

// 登录日志
const loginLoading = ref(false)
const loginLogs = ref<any[]>([])
const loginPage = ref(1)
const loginSize = ref(10)
const loginTotal = ref(0)
const loginForm = reactive({
  username: '', status: undefined as number | undefined, dateRange: [] as any[]
})

const detailVisible = ref(false)
const operDetail = reactive<any>({})

async function searchOper(p?: number) {
  if (p) operPage.value = p
  operLoading.value = true
  try {
    const params: any = { pageNum: operPage.value, pageSize: operSize.value }
    if (operForm.title) params.title = operForm.title
    if (operForm.operName) params.operName = operForm.operName
    if (operForm.businessType !== undefined) params.businessType = operForm.businessType
    if (operForm.status !== undefined && operForm.status >= 0) params.status = operForm.status
    if (operForm.dateRange && operForm.dateRange.length === 2) {
      params.startTime = operForm.dateRange[0]
      params.endTime = operForm.dateRange[1]
    }
    const res: any = await getOperLogPage(params)
    if (res.code === 200) {
      operLogs.value = res.data.records || []
      operTotal.value = res.data.total || 0
    }
  } finally { operLoading.value = false }
}

function resetOper() {
  operForm.title = ''; operForm.operName = ''; operForm.businessType = undefined
  operForm.status = undefined; operForm.dateRange = []
  searchOper(1)
}

async function searchLogin(p?: number) {
  if (p) loginPage.value = p
  loginLoading.value = true
  try {
    const params: any = { pageNum: loginPage.value, pageSize: loginSize.value }
    if (loginForm.username) params.username = loginForm.username
    if (loginForm.status !== undefined && loginForm.status >= 0) params.status = loginForm.status
    if (loginForm.dateRange && loginForm.dateRange.length === 2) {
      params.startTime = loginForm.dateRange[0]
      params.endTime = loginForm.dateRange[1]
    }
    const res: any = await getLoginLogPage(params)
    if (res.code === 200) {
      loginLogs.value = res.data.records || []
      loginTotal.value = res.data.total || 0
    }
  } finally { loginLoading.value = false }
}

function resetLogin() {
  loginForm.username = ''; loginForm.status = undefined; loginForm.dateRange = []
  searchLogin(1)
}

function viewOperDetail(row: any) {
  Object.assign(operDetail, row)
  detailVisible.value = true
}

onMounted(() => { searchOper(); searchLogin() })
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 16px; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
.code-block { background: #f5f5f5; padding: 12px; border-radius: 4px; max-height: 200px; overflow: auto; font-size: 13px; white-space: pre-wrap; word-break: break-all; }
.code-block.error { background: #fff2f0; color: #d93026; }
</style>
