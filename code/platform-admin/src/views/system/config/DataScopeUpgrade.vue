<template>
  <CrudPage
    :loading="crud.loading"
    :table-data="crud.tableData"
    :total="crud.total"
    :page-num="crud.pageNum"
    :page-size="crud.pageSize"
    :dialog-visible="crud.dialogVisible"
    :dialog-title="crud.dialogTitle"
    :form-data="crud.formData"
    v-model:dialog-visible="crud.dialogVisible"
    @search="crud.handleSearch"
    @reset="crud.handleReset"
    @size-change="crud.handleSizeChange"
    @page-change="crud.handlePageChange"
    @submit="crud.handleSubmit"
  >
    <!-- 搜索区：含防抖自动搜索 -->
    <template #search>
      <el-form-item label="关键字">
        <el-input
          v-model="crud.searchForm.keyword"
          placeholder="配置名称/键名"
          clearable
          @input="crud.debouncedLoadData"
          @clear="crud.handleSearch"
        />
      </el-form-item>
    </template>

    <!-- 工具栏 -->
    <template #toolbar>
      <el-button type="success" @click="crud.handleAdd()" v-permission="'system:config:add'">新增配置</el-button>
      <el-button type="warning" @click="handleInitDefaults" v-permission="'system:config:add'">初始化默认配置</el-button>
    </template>

    <!-- 表格列 -->
    <template #columns>
      <el-table-column prop="configName" label="配置名称" width="180" />
      <el-table-column prop="configKey" label="配置键名" width="260" />
      <el-table-column prop="configValue" label="键值" />
      <el-table-column label="系统内置" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.configType === 1 ? 'info' : 'warning'" size="small">
            {{ scope.row.configType === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
    </template>

    <!-- 操作列 -->
    <template #actions="{ row }">
      <el-button type="primary" size="small" @click="crud.handleEdit(row)" v-permission="'system:config:edit'">编辑</el-button>
      <el-button type="danger" size="small" @click="crud.handleDelete(row)" v-permission="'system:config:del'">删除</el-button>
    </template>

    <!-- 弹窗表单 -->
    <template #form>
      <el-form-item label="配置名称" prop="configName">
        <el-input v-model="crud.formData.configName" placeholder="如：数据权限升级总开关" />
      </el-form-item>
      <el-form-item label="配置键名" prop="configKey">
        <el-input v-model="crud.formData.configKey" placeholder="如：platform.data-scope.upgrade.enabled" :disabled="crud.isEdit" />
      </el-form-item>
      <el-form-item label="键值" prop="configValue">
        <el-input v-model="crud.formData.configValue" type="textarea" :rows="2" placeholder="true / false / 其他值" />
      </el-form-item>
      <el-form-item label="系统内置">
        <el-radio-group v-model="crud.formData.configType">
          <el-radio :label="1">是</el-radio>
          <el-radio :label="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="crud.formData.remark" type="textarea" placeholder="配置用途说明" />
      </el-form-item>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
import { ElMessage } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  getConfigPage,
  createConfig,
  updateConfig,
  deleteConfig,
  getConfigById,
  getConfigByKey,
  type Config
} from '@/api/config'

const crud = useCrud(
  {
    page: getConfigPage,
    create: createConfig,
    update: updateConfig,
    delete: deleteConfig,
    getById: getConfigById
  },
  {
    defaultSearch: { keyword: '' },
    defaultForm: { configName: '', configKey: '', configValue: '', configType: 0, remark: '' }
  }
)

/** 预定义的数据权限相关默认配置 */
const DEFAULT_CONFIGS: Config[] = [
  {
    configName: '数据权限升级总开关',
    configKey: 'platform.data-scope.upgrade.enabled',
    configValue: 'false',
    configType: 1,
    remark: 'M5 P0-2 数据权限升级总开关。false=走 v7.0 行为，true=启用增强版（UPDATE/DELETE 拦截 + fail-closed）'
  },
  {
    configName: '写严格模式开关',
    configKey: 'platform.data-scope.upgrade.write-strict',
    configValue: 'false',
    configType: 1,
    remark: 'M5 P0-2 PR4 写严格模式。false=WARN 放行，true=抛 DataScopeViolationException（fail-closed）'
  },
  {
    configName: '多租户拦截器开关',
    configKey: 'platform.tenant.interceptor.enabled',
    configValue: 'true',
    configType: 1,
    remark: 'M4 P0-1 多租户拦截器总开关。false=禁用 TenantLineInnerInterceptor'
  }
]

async function handleInitDefaults() {
  try {
    for (const cfg of DEFAULT_CONFIGS) {
      const res: any = await getConfigByKey(cfg.configKey)
      if (res.code === 200 && res.data) {
        continue
      }
      await createConfig(cfg)
    }
    ElMessage.success('默认配置初始化完成')
    crud.loadData()
  } catch (e: any) {
    ElMessage.error('初始化失败：' + (e.message || e))
  }
}

// 避免 vue-tsc 误报 unused（实际在 template 中通过 @click 引用）
void handleInitDefaults
</script>
