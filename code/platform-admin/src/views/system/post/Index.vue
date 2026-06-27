<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>岗位管理</span>
          <el-select v-model="selectedOrgId" placeholder="选择组织" style="width: 180px; margin-right: 10px">
            <el-option label="全部组织" :value="0" />
            <el-option v-for="org in orgOptions" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
          <el-select v-model="selectedDeptId" placeholder="选择部门" style="width: 180px; margin-right: 10px" :disabled="!selectedOrgId">
            <el-option label="全部部门" :value="0" />
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
          <el-button type="primary" @click="handleAdd" v-permission="'system:post:add'">新增岗位</el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
      >
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="岗位名称" width="180" />
        <el-table-column prop="level" label="岗位级别" width="100">
          <template #default="scope">
            <el-tag>{{ scope.row.level || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属组织" width="150" />
        <el-table-column prop="deptName" label="所属部门" width="150" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <TableActions :buttons="[
              { label: '编辑', props: { type: 'primary' }, handler: () => handleEdit(scope.row), permission: 'system:post:edit' },
              { label: '删除', props: { type: 'danger' }, handler: () => handleDelete(scope.row), permission: 'system:post:del' }
            ]" />
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="所属组织" prop="orgId">
          <el-select v-model="formData.orgId" placeholder="请选择组织" @change="onOrgChange">
            <el-option v-for="org in orgOptions" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="formData.deptId" placeholder="请选择部门">
            <el-option label="无" :value="undefined" />
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="岗位级别">
          <el-input v-model="formData.level" placeholder="如：P1-P10" />
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
import { getDeptList } from '@/api/dept'
import { getPostPage, getPostById, createPost, updatePost, deletePost } from '@/api/post'

const loading = ref(false)
const tableData = ref<any[]>([])
const orgOptions = ref<any[]>([])
const deptOptions = ref<any[]>([])
const selectedOrgId = ref(0)
const selectedDeptId = ref(0)

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()

const formData = reactive<any>({
  orgId: undefined,
  deptId: undefined,
  code: '',
  name: '',
  level: '',
  sort: 0,
  status: 1
})

const rules = {
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
  code: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}

async function loadOrgs() {
  const res: any = await getOrgTree()
  if (res.code === 200) {
    orgOptions.value = res.data
  }
}

async function loadDepts(orgId?: number) {
  if (!orgId) {
    deptOptions.value = []
    return
  }
  const res: any = await getDeptList(orgId)
  if (res.code === 200) {
    deptOptions.value = res.data
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (selectedOrgId.value) params.orgId = selectedOrgId.value
    if (selectedDeptId.value) params.deptId = selectedDeptId.value
    
    const res: any = await getPostPage(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

watch(selectedOrgId, (val) => {
  selectedDeptId.value = 0
  loadDepts(val || undefined)
  pageNum.value = 1
  loadData()
})

watch(selectedDeptId, () => {
  pageNum.value = 1
  loadData()
})

function onOrgChange(orgId: number) {
  formData.deptId = undefined
  loadDepts(orgId)
}

function resetForm() {
  formData.orgId = selectedOrgId.value || undefined
  formData.deptId = undefined
  formData.code = ''
  formData.name = ''
  formData.level = ''
  formData.sort = 0
  formData.status = 1
  currentId.value = null
  loadDepts(formData.orgId)
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增岗位'
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑岗位'
  currentId.value = row.id
  const res: any = await getPostById(row.id)
  if (res.code === 200) {
    const post = res.data
    formData.orgId = post.orgId
    formData.deptId = post.deptId
    formData.code = post.code
    formData.name = post.name
    formData.level = post.level || ''
    formData.sort = post.sort
    formData.status = post.status
    loadDepts(post.orgId)
  }
  dialogVisible.value = true
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该岗位？', '提示', { type: 'warning' })
    const res: any = await deletePost(row.id)
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
    const res: any = await updatePost(currentId.value, { ...formData })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadData()
    }
  } else {
    const res: any = await createPost({ ...formData })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadData()
    }
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  loadData()
}

function handleCurrentChange(page: number) {
  pageNum.value = page
  loadData()
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