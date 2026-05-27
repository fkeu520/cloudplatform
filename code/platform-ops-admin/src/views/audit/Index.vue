<template>
  <div>
    <el-card>
      <template #header>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="操作日志" name="oper" />
          <el-tab-pane label="登录日志" name="login" />
          <el-tab-pane label="ELK日志检索" name="elk" />
        </el-tabs>
      </template>

      <div v-if="activeTab==='oper'">
        <el-form :inline="true" style="margin-bottom:16px">
          <el-form-item label="模块标题"><el-input v-model="operQuery.title" clearable /></el-form-item>
          <el-form-item label="操作人员"><el-input v-model="operQuery.operName" clearable /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchOperLogs">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="operLogs" v-loading="operLoading" border stripe>
          <el-table-column prop="title" label="模块" width="100" />
          <el-table-column prop="operName" label="操作人员" width="90" />
          <el-table-column prop="requestMethod" label="请求方式" width="80" />
          <el-table-column prop="operIp" label="IP" width="130" />
          <el-table-column prop="method" label="方法" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status===0?'success':'danger'">{{ row.status===0?'正常':'异常' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costTime" label="耗时(ms)" width="80" />
          <el-table-column prop="operTime" label="操作时间" width="170" />
        </el-table>
      </div>

      <div v-if="activeTab==='login'">
        <el-table :data="loginLogs" v-loading="loginLoading" border stripe>
          <el-table-column prop="username" label="用户名" width="100" />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="loginType" label="类型" width="80">
            <template #default="{ row }">{{ ['密码','短信','第三方'][row.loginType] || '其他' }}</template>
          </el-table-column>
          <el-table-column prop="device" label="设备" min-width="120" />
          <el-table-column prop="browser" label="浏览器" width="100" />
          <el-table-column prop="status" label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':'danger'">{{ row.status===1?'成功':'失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="loginTime" label="登录时间" width="170" />
        </el-table>
      </div>

      <div v-if="activeTab==='elk'">
        <el-form :inline="true" style="margin-bottom:16px">
          <el-form-item label="关键词"><el-input v-model="elkQuery.keyword" placeholder="搜索日志内容" clearable /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchElk" :loading="elkLoading">搜索</el-button>
          </el-form-item>
        </el-form>
        <pre v-if="elkResult" style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;max-height:500px;overflow:auto;font-size:13px">{{ elkResult }}</pre>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { loginLogPage, operLogPage, searchElk } from '../../api/audit'

const activeTab = ref('login')
const loginLogs = ref<any[]>([])
const loginLoading = ref(false)
const elkQuery = ref({ keyword: '' })
const elkResult = ref('')
const elkLoading = ref(false)
const operLogs = ref<any[]>([])
const operLoading = ref(false)
const operQuery = ref({ title: '', operName: '' })

onMounted(() => fetchLoginLogs())

async function fetchLoginLogs() {
  loginLoading.value = true
  try { const res = await loginLogPage({ pageNum: 1, pageSize: 100 }); loginLogs.value = res.data.records || [] } finally { loginLoading.value = false }
}

async function fetchOperLogs() {
  operLoading.value = true
  try { const res = await operLogPage({ ...operQuery.value, pageNum: 1, pageSize: 100 }); operLogs.value = res.data.records || [] } finally { operLoading.value = false }
}

async function searchElkFn() {
  elkLoading.value = true
  try { const res = await searchElk({ keyword: elkQuery.value.keyword, from: 0, size: 20 }); elkResult.value = JSON.stringify(res.data, null, 2) } finally { elkLoading.value = false }
}
</script>
