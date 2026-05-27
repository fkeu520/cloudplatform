<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-select v-model="selectedOrgId" placeholder="选择组织" style="width: 180px; margin-right: 10px">
            <el-option label="全部组织" :value="0" />
            <el-option v-for="org in orgOptions" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
          <el-button type="primary" @click="handleAdd" v-permission="'system:dept:add'">新增部门</el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="manager" label="负责人" width="120" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="auto" fixed="right">
          <template #default="scope">
            <TableActions :buttons="[
              { label: '编辑', props: { type: 'primary' }, handler: () => handleEdit(scope.row), permission: 'system:dept:edit' },
              { label: '添加子部门', props: { type: 'success' }, handler: () => handleAddChild(scope.row), permission: 'system:dept:add' },
              { label: '删除', props: { type: 'danger' }, handler: () => handleDelete(scope.row), permission: 'system:dept:del' }
            ]" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="所属组织" prop="orgId">
          <el-select v-model="formData.orgId" placeholder="请选择组织">
            <el-option v-for="org in orgOptions" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="formData.parentId"
            :data="deptTreeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="请选择上级部门（不选则为顶级）"
          />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="formData.manager" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TableActions from '@/components/TableActions.vue'
import { getOrgTree } from '@/api/org'
import { getDeptTree, getDeptById, createDept, updateDept, deleteDept } from '@/api/dept'

const loading = ref(false)
const tableData = ref<any[]>([])
const deptTreeData = ref<any[]>([])
const orgOptions = ref<any[]>([])
const selectedOrgId = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()

const formData = reactive<any>({
  orgId: undefined,
  parentId: undefined,
  code: '',
  name: '',
  manager: '',
  phone: '',
  sort: 0,
  status: 1
})

const rules = {
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

async function loadOrgs() {
  const res: any = await getOrgTree()
  if (res.code === 200) {
    orgOptions.value = res.data
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getDeptTree(selectedOrgId.value || undefined)
    if (res.code === 200) {
      tableData.value = res.data
      deptTreeData.value = [...res.data, { id: 0, name: '顶级部门', children: [] }]
    }
  } finally {
    loading.value = false
  }
}

watch(selectedOrgId, () => {
  loadData()
})

function resetForm(orgId?: number, parentId?: number) {
  formData.orgId = orgId || selectedOrgId.value || undefined
  formData.parentId = parentId
  formData.code = ''
  formData.name = ''
  formData.manager = ''
  formData.phone = ''
  formData.sort = 0
  formData.status = 1
  currentId.value = null
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
}

function handleAddChild(row: any) {
  resetForm(row.orgId, row.id)
  isEdit.value = false
  dialogTitle.value = `添加子部门（${row.name}）`
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑部门'
  currentId.value = row.id
  const res: any = await getDeptById(row.id)
  if (res.code === 200) {
    const dept = res.data
    formData.orgId = dept.orgId
    formData.parentId = dept.parentId
    formData.code = dept.code
    formData.name = dept.name
    formData.manager = dept.manager || ''
    formData.phone = dept.phone || ''
    formData.sort = dept.sort
    formData.status = dept.status
  }
  dialogVisible.value = true
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该部门？', '提示', { type: 'warning' })
    const res: any = await deleteDept(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // cancelled
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  if (isEdit.value && currentId.value) {
    const res: any = await updateDept(currentId.value, { ...formData })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadData()
    }
  } else {
    const res: any = await createDept({ ...formData })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadData()
    }
  }
}

onMounted(() => {
  loadOrgs()
  loadData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>