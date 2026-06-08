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
    <template #search>
      <el-form-item label="关键字">
        <el-input
          v-model="crud.searchForm.keyword"
          placeholder="参数名称/键名"
          clearable
          @input="crud.debouncedLoadData"
          @clear="crud.handleSearch"
        />
      </el-form-item>
    </template>

    <template #toolbar>
      <el-button type="success" @click="crud.handleAdd()" v-permission="'system:config:add'">新增参数</el-button>
    </template>

    <template #columns>
      <el-table-column prop="configName" label="名称" width="160" />
      <el-table-column prop="configKey" label="键名" width="200" />
      <el-table-column prop="configValue" label="键值" />
      <el-table-column label="系统内置" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.configType === 1 ? 'info' : 'warning'" size="small">
            {{ scope.row.configType === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
    </template>

    <template #actions="{ row }">
      <el-button type="primary" size="small" @click="crud.handleEdit(row)" v-permission="'system:config:edit'">编辑</el-button>
      <el-button type="danger" size="small" @click="crud.handleDelete(row)" v-permission="'system:config:del'">删除</el-button>
    </template>

    <template #form>
      <el-form-item label="名称">
        <el-input v-model="crud.formData.configName" />
      </el-form-item>
      <el-form-item label="键名">
        <el-input v-model="crud.formData.configKey" :disabled="crud.isEdit" />
      </el-form-item>
      <el-form-item label="键值">
        <el-input v-model="crud.formData.configValue" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="系统内置">
        <el-radio-group v-model="crud.formData.configType">
          <el-radio :label="1">是</el-radio>
          <el-radio :label="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="crud.formData.remark" type="textarea" />
      </el-form-item>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
import { reactive } from 'vue'
import { useCrud } from '@/composables/useCrud'
import {
  getConfigPage,
  createConfig,
  updateConfig,
  deleteConfig
} from '@/api/config'

const crud = reactive(useCrud(
  {
    page: getConfigPage,
    create: createConfig,
    update: updateConfig,
    delete: deleteConfig
  },
  {
    defaultSearch: { keyword: '' },
    defaultForm: { configName: '', configKey: '', configValue: '', configType: 0, remark: '' }
  }
))
</script>
