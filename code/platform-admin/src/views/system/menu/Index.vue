<template>
  <div class="page-container">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table
          :data="tableData"
          v-loading="loading"
          row-key="id"
          border
          default-expand-all
          :tree-props="{ children: 'children' }"
        >
          <el-table-column prop="name" label="菜单名称" width="180" />
          <el-table-column prop="path" label="路由路径" width="160" />
          <el-table-column prop="component" label="组件路径" width="200" />
          <el-table-column prop="perms" label="权限标识" width="160" />
          <el-table-column prop="icon" label="图标" width="80" />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column prop="type" label="类型" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.type === 1 ? 'primary' : 'success'">
                {{ scope.row.type === 1 ? '菜单' : '按钮' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑菜单名称" width="400px">
      <el-form :model="formData" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" />
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
import { ElMessage } from 'element-plus'
import { getMenuTree, getMenuById, updateMenu } from '@/api/menu'
import type { Menu } from '@/api/menu'

const loading = ref(false)
const tableData = ref<any[]>([])

const dialogVisible = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()

const formData = reactive<Partial<Menu>>({
  name: ''
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMenuTree()
    if (res.code === 200) {
      tableData.value = res.data
    }
  } finally {
    loading.value = false
  }
}

async function handleEdit(row: any) {
  currentId.value = row.id
  const res: any = await getMenuById(row.id)
  if (res.code === 200) {
    formData.name = res.data.name
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  if (currentId.value) {
    const res: any = await updateMenu(currentId.value, { name: formData.name } as Menu)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadData()
    }
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.table-card :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-wrapper {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
</style>
