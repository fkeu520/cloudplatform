<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <el-form :inline="true">
        <slot name="search" />
        <el-form-item>
          <el-button type="primary" @click="onSearch" :loading="loading">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <slot name="toolbar" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border>
        <slot name="columns" />
        <el-table-column label="操作" width="auto" fixed="right">
          <template #default="scope">
            <slot name="actions" :row="scope.row" />
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 表单弹窗 -->
    <el-dialog :model-value="dialogVisible" @update:model-value="onDialogVisibleChange" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="80px">
        <slot name="form" />
      </el-form>
      <template #footer>
        <slot name="form-footer">
          <el-button @click="onDialogVisibleChange(false)">取消</el-button>
          <el-button type="primary" @click="onSubmit" :loading="loading">确定</el-button>
        </slot>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
interface Props {
  loading: boolean
  tableData: any[]
  total: number
  pageNum: number
  pageSize: number
  dialogVisible: boolean
  dialogTitle: string
  formData: Record<string, any>
}

defineProps<Props>()
const emit = defineEmits<{
  search: []
  reset: []
  sizeChange: [val: number]
  pageChange: [val: number]
  submit: []
  'update:dialogVisible': [val: boolean]
}>()

function onSearch() { emit('search') }
function onReset() { emit('reset') }
function onSizeChange(val: number) { emit('sizeChange', val) }
function onPageChange(val: number) { emit('pageChange', val) }
function onSubmit() { emit('submit') }
function onDialogVisibleChange(val: boolean) { emit('update:dialogVisible', val) }
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.search-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
