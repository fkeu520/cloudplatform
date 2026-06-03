<template>
  <div class="page-container">
    <div class="main-layout">
      <div class="left-panel">
        <div class="panel-header">
          <span>组织架构</span>
        </div>
        <div class="tree-container">
          <el-tree
            ref="orgTreeRef"
            :data="orgTreeData"
            :props="{ label: 'name', children: 'children' }"
            :expand-on-click-node="false"
            :default-expand-all="true"
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node-wrapper">
                <span class="tree-node">
                  <span :class="getNodeIcon(data.type, data.nodeType)"></span>
                  <span>{{ data.name }}</span>
                  <span v-if="data.count !== undefined" class="node-count">({{ data.count }})</span>
                </span>
                <span class="tree-actions" @click.stop="showMenu(data, $event)">
                  <span class="actions-trigger">⋮⋮⋮</span>
                </span>
                <Teleport to="body">
                  <span v-if="menuKey(menuData) === menuKey(data)" class="actions-menu" :style="menuStyle" @click.stop @mouseleave="hideMenu">
                    <span v-if="canEditNode(data)" @click="handleEditName(data); hideMenu()">{{ editNameTextForNode(data) }}</span>
                    <span v-if="canAddHeadquarters(data)" @click="handleAddHeadquarters(data); hideMenu()">添加总部</span>
                    <template v-if="showMultipleAddOptionsForNode(data)">
                      <span @click="handleAddSubOrg(data); hideMenu()">添加分子公司</span>
                      <span @click="handleAddDept(data); hideMenu()">添加部门</span>
                    </template>
                    <span v-else-if="canAddDept(data)" @click="handleAddDept(data); hideMenu()">添加部门</span>
                    <span v-else-if="canAddPost(data)" @click="handleAddPost(data); hideMenu()">添加岗位</span>
                    <span v-if="canDeleteNode(data)" @click="handleDeleteNode(data); hideMenu()" class="delete-action">删除</span>
                  </span>
                </Teleport>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <div class="right-panel">
        <div class="panel-header">
          <span>员工列表</span>
          <span class="current-scope">当前范围：{{ currentScopeName }}</span>
          <el-button type="primary" @click="handleAddEmployee" v-permission="'system:org:addEmp'" :disabled="!selectedNode">添加员工</el-button>
          <el-button type="success" @click="handleCreateEmployee" :disabled="!selectedNode">新增</el-button>
        </div>

        <el-table
          :data="employeeList"
          v-loading="loading"
          border
        >
          <el-table-column prop="nickname" label="姓名" width="120" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="mobile" label="手机" width="120" />
          <el-table-column prop="email" label="邮箱" width="180" />
          <el-table-column prop="orgName" label="公司" width="120" />
          <el-table-column prop="deptName" label="部门" width="120" />
          <el-table-column prop="postName" label="岗位" width="120" />
          <el-table-column prop="statusDesc" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.statusDesc }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="auto">
            <template #default="scope">
              <el-button size="small" @click="handleEditEmployee(scope.row)">编辑</el-button>
              <el-button size="small" @click="handleRemoveEmployee(scope.row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > 0"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          class="pagination"
        />
      </div>
    </div>



    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item v-if="dialogType === 'editOrg' || dialogType === 'addOrg'" label="组织编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'addOrg'" label="组织类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择">
            <el-option label="集团" :value="1" />
            <el-option label="总部" :value="2" />
            <el-option label="子公司" :value="3" />
            <el-option label="分公司" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dialogType === 'addDept'" label="部门编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'addPost'" label="岗位编码" prop="code">
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'addPost'" label="岗位级别">
          <el-input v-model="formData.level" placeholder="如：P1-P10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDialogSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="employeeDialogVisible" title="选择员工" width="700px">
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索姓名或账号" style="width: 250px" @keyup.enter="loadUserList" />
        <el-button @click="loadUserList">搜索</el-button>
      </div>
      <el-table
        ref="userTableRef"
        :data="userList"
        v-loading="userLoading"
        border
        :row-key="(user: any) => user.id"
        :default-sort="{ prop: 'createTime', order: 'desc' }"
        @selection-change="handleUserSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="nickname" label="姓名" width="120" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="mobile" label="手机" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="statusDesc" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        @size-change="handleUserSizeChange"
        @current-change="handleUserCurrentChange"
        :current-page="userPageNum"
        :page-sizes="[10, 20, 50]"
        :page-size="userPageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="userTotal"
        class="pagination"
      />
      <template #footer>
        <el-button @click="employeeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAddEmployees">确定添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createDialogVisible" title="新增员工" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" placeholder="登录密码" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="createForm.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.mobile" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="电子邮箱" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input :value="selectedNodeData?.name" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑员工" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="姓名">
          <el-input :value="editForm.nickname" disabled />
        </el-form-item>
        <el-form-item label="所属组织">
          <el-select v-model="editForm.orgId" placeholder="选择组织" @change="onEditOrgChange" clearable style="width:100%">
            <el-option v-for="org in flatOrgList" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="editForm.deptId" placeholder="选择部门" @change="onEditDeptChange" clearable style="width:100%">
            <el-option v-for="d in editDeptList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="editForm.postId" placeholder="选择岗位" clearable style="width:100%">
            <el-option v-for="p in editPostList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrgTree, createOrg, updateOrg, deleteOrg } from '@/api/org'
import { getDeptTree, getDeptList, createDept, updateDept, deleteDept } from '@/api/dept'
import { getPostByDeptId, createPost, updatePost, deletePost } from '@/api/post'
import { getUserPage, createUser, updateUser } from '@/api/user'
import { rsaEncrypt } from '@/api/crypto'

const loading = ref(false)
const userLoading = ref(false)
const orgTreeData = ref<any[]>([])
const employeeList = ref<any[]>([])
const selectedNode = ref<any>(null)
const selectedNodeData = ref<any>(null)
const orgTreeRef = ref()
const userTableRef = ref()

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const userPageNum = ref(1)
const userPageSize = ref(10)
const userTotal = ref(0)
const userList = ref<any[]>([])
const searchKeyword = ref('')
const employeeDialogVisible = ref(false)
const selectedUserIds = ref<string[]>([])
const createDialogVisible = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  nickname: '',
  mobile: '',
  email: ''
})
const editDialogVisible = ref(false)
const editForm = reactive({
  id: '',
  nickname: '',
  orgId: null as number | null,
  deptId: null as number | null,
  postId: null as number | null
})
const editDeptList = ref<any[]>([])
const editPostList = ref<any[]>([])

const menuData = ref<any>(null)
const menuStyle = ref({})

function showMenu(data: any, event: MouseEvent) {
  menuData.value = data
  const el = event.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  menuStyle.value = {
    position: 'fixed',
    left: rect.right + 8 + 'px',
    top: rect.top + 'px',
    zIndex: 9999
  }
}

function menuKey(data: any) {
  return data ? data.nodeType + '-' + data.id : ''
}

function hideMenu() {
  menuData.value = null
}

function handleUserSelectionChange(rows: any[]) {
  selectedUserIds.value = rows.map(row => row.id)
}



const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref('')
const formRef = ref()
const formData = reactive({
  code: '',
  name: '',
  type: 1,
  level: ''
})

const rules = computed(() => {
  const baseRules: any = {
    name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
  }
  if (dialogType.value.startsWith('add')) {
    baseRules.code = [{ required: true, message: '请输入编码', trigger: 'blur' }]
    if (dialogType.value === 'addOrg') {
      baseRules.type = [{ required: true, message: '请选择类型', trigger: 'change' }]
    }
  }
  return baseRules
})

const currentScopeName = computed(() => {
  if (!selectedNodeData.value) return '未选择'
  return selectedNodeData.value.name || '未选择'
})

function canEditNode(data: any): boolean {
  if (!data) return false
  return ['org', 'dept', 'post'].includes(data.nodeType)
}

function hasHeadquarters(orgTree: any[]): boolean {
  for (const org of orgTree) {
    if (org.type === 2) return true
    if (org.children) {
      for (const child of org.children) {
        if (child.nodeType === 'org' && child.type === 2) return true
      }
    }
  }
  return false
}

function canAddHeadquarters(data: any): boolean {
  if (!data || data.nodeType !== 'org' || data.type !== 1) return false
  return !hasHeadquarters(orgTreeData.value)
}

function canAddDept(data: any): boolean {
  if (!data) return false
  const nodeType = data.nodeType
  if (nodeType === 'org' && (data.type === 2 || data.type === 3 || data.type === 4)) return true
  return false
}

function canAddPost(data: any): boolean {
  if (!data) return false
  return data.nodeType === 'dept'
}

function showMultipleAddOptionsForNode(data: any): boolean {
  if (!data) return false
  const nodeType = data.nodeType
  if (nodeType === 'org' && (data.type === 2 || data.type === 3 || data.type === 4)) return true
  return false
}

function canDeleteNode(data: any): boolean {
  if (!data) return false
  return ['org', 'dept', 'post'].includes(data.nodeType)
}

function editNameTextForNode(data: any): string {
  if (!data) return ''
  const nodeType = data.nodeType
  if (nodeType === 'org') {
    if (data.type === 1) return '修改集团名称'
    if (data.type === 2) return '修改总部名称'
    if (data.type === 3) return '修改子公司名称'
    if (data.type === 4) return '修改分公司名称'
    return '修改组织名称'
  }
  if (nodeType === 'dept') return '修改部门名称'
  if (nodeType === 'post') return '修改岗位名称'
  return '修改名称'
}

function getNodeIcon(type?: number, nodeType?: string): string {
  if (nodeType === 'org') {
    if (type === 1) return 'el-icon-building'
    if (type === 2) return 'el-icon-office-building'
    if (type === 3) return 'el-icon-factory'
    if (type === 4) return 'el-icon-shop'
    return 'el-icon-building'
  }
  if (nodeType === 'dept') return 'el-icon-sitemap'
  if (nodeType === 'post') return 'el-icon-user'
  return 'el-icon-folder'
}

async function loadOrgTree() {
  loading.value = true
  try {
    const orgRes: any = await getOrgTree()
    if (orgRes.code === 200) {
      const orgList = orgRes.data
      for (const org of orgList) {
        await processOrgNode(org)
      }
      orgTreeData.value = orgList
    }
  } finally {
    loading.value = false
  }
}

async function processOrgNode(org: any) {
  org.nodeType = 'org'
  const depts: any[] = []
  const deptRes: any = await getDeptTree(org.id)
  if (deptRes.code === 200) {
    for (const dept of deptRes.data) {
      dept.nodeType = 'dept'
      const postRes: any = await getPostByDeptId(dept.id)
      if (postRes.code === 200 && postRes.data.length > 0) {
        dept.children = postRes.data.map((post: any) => {
          post.nodeType = 'post'
          return post
        })
      }
      depts.push(dept)
    }
  }
  if (org.children && org.children.length > 0) {
    for (const child of org.children) {
      await processOrgNode(child)
    }
    org.children = [...depts, ...org.children]
  } else {
    org.children = depts
  }
}

function handleNodeClick(data: any, node: any) {
  selectedNode.value = node
  selectedNodeData.value = data
  loadEmployees()
}

function handleEditName(data?: any) {
  const nodeData = data || selectedNodeData.value
  if (!nodeData) return
  selectedNodeData.value = nodeData
  dialogType.value = `edit${nodeData.nodeType.charAt(0).toUpperCase() + nodeData.nodeType.slice(1)}`
  dialogTitle.value = editNameTextForNode(nodeData)
  formData.name = nodeData.name
  formData.code = nodeData.code || ''
  formData.type = nodeData.type || 1
  formData.level = nodeData.level || ''
  dialogVisible.value = true
}

function handleAddHeadquarters(data?: any) {
  const nodeData = data || selectedNodeData.value
  if (!nodeData) return
  selectedNodeData.value = nodeData
  dialogType.value = 'addOrg'
  dialogTitle.value = '添加总部'
  formData.name = ''
  formData.code = ''
  formData.type = 2
  formData.level = ''
  dialogVisible.value = true
}

function handleAddSubOrg(data?: any) {
  const nodeData = data || selectedNodeData.value
  if (!nodeData) return
  selectedNodeData.value = nodeData
  dialogType.value = 'addOrg'
  dialogTitle.value = '添加分子公司'
  formData.name = ''
  formData.code = ''
  formData.type = 3
  formData.level = ''
  dialogVisible.value = true
}

function handleAddDept(data?: any) {
  const nodeData = data || selectedNodeData.value
  if (!nodeData) return
  selectedNodeData.value = nodeData
  dialogType.value = 'addDept'
  dialogTitle.value = '添加部门'
  formData.name = ''
  formData.code = ''
  formData.type = 1
  formData.level = ''
  dialogVisible.value = true
}

function handleAddPost(data?: any) {
  const nodeData = data || selectedNodeData.value
  if (!nodeData) return
  selectedNodeData.value = nodeData
  dialogType.value = 'addPost'
  dialogTitle.value = '添加岗位'
  formData.name = ''
  formData.code = ''
  formData.type = 1
  formData.level = ''
  dialogVisible.value = true
}

async function handleDeleteNode(data?: any) {
  const nodeData = data || selectedNodeData.value
  if (!nodeData) return
  selectedNodeData.value = nodeData
  const nodeType = nodeData.nodeType
  const name = nodeData.name
  try {
    await ElMessageBox.confirm(`确定删除「${name}」？`, '提示', { type: 'warning' })
    let res: any
    if (nodeType === 'org') {
      res = await deleteOrg(selectedNodeData.value.id)
    } else if (nodeType === 'dept') {
      res = await deleteDept(selectedNodeData.value.id)
    } else {
      res = await deletePost(selectedNodeData.value.id)
    }
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadOrgTree()
      employeeList.value = []
      total.value = 0
    }
  } catch {
    // cancelled
  }
}

async function handleDialogSubmit() {
  if (!formData.name.trim()) {
    ElMessage.error('请输入名称')
    return
  }
  
  const nodeType = selectedNodeData.value?.nodeType
  let res: any
  
  if (dialogType.value.startsWith('edit')) {
    if (nodeType === 'org') {
      res = await updateOrg(selectedNodeData.value.id, { name: formData.name })
    } else if (nodeType === 'dept') {
      res = await updateDept(selectedNodeData.value.id, { name: formData.name })
    } else {
      res = await updatePost(selectedNodeData.value.id, { name: formData.name })
    }
  } else {
    if (dialogType.value === 'addOrg') {
      res = await createOrg({
        parentId: selectedNodeData.value.id,
        name: formData.name,
        code: formData.code,
        type: formData.type,
        status: 1
      })
    } else if (dialogType.value === 'addDept') {
      res = await createDept({
        orgId: selectedNodeData.value.id,
        name: formData.name,
        code: formData.code,
        status: 1
      })
    } else if (dialogType.value === 'addPost') {
      res = await createPost({
        orgId: selectedNodeData.value.orgId,
        deptId: selectedNodeData.value.id,
        name: formData.name,
        code: formData.code,
        level: formData.level,
        status: 1
      })
    }
  }
  
  if (res.code === 200) {
    ElMessage.success(dialogType.value.startsWith('edit') ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadOrgTree()
  }
}

async function loadEmployees() {
  if (!selectedNodeData.value) return
  
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    const nodeType = selectedNodeData.value.nodeType
    if (nodeType === 'org') {
      if (selectedNodeData.value.type === 1) {
        params.tenantId = Number(selectedNodeData.value.tenantId || 1)
      } else {
        const ids = collectOrgIds(selectedNodeData.value)
        params.orgIds = ids.join(',')
      }
    } else if (nodeType === 'dept') {
      params.deptId = selectedNodeData.value.id
    } else if (nodeType === 'post') {
      params.deptId = selectedNodeData.value.deptId
      params.postId = selectedNodeData.value.id
    }
    
    const res: any = await getUserPage(params)
    if (res.code === 200) {
      employeeList.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function collectOrgIds(org: any): number[] {
  const ids: number[] = [org.id]
  if (org.children && org.children.length > 0) {
    for (const child of org.children) {
      if (child.nodeType === 'org') {
        ids.push(...collectOrgIds(child))
      }
    }
  }
  return ids
}

function handleAddEmployee() {
  employeeDialogVisible.value = true
  selectedUserIds.value = []
  loadUserList()
}

function handleCreateEmployee() {
  createForm.username = ''
  createForm.password = ''
  createForm.nickname = ''
  createForm.mobile = ''
  createForm.email = ''
  createDialogVisible.value = true
}

async function handleCreateSubmit() {
  if (!createForm.username || !createForm.password) {
    ElMessage.error('用户名和密码不能为空')
    return
  }
  // 加密密码
  const encryptedPassword = await rsaEncrypt(createForm.password)
  const data: any = {
    username: createForm.username,
    password: encryptedPassword,
    nickname: createForm.nickname || undefined,
    mobile: createForm.mobile || undefined,
    email: createForm.email || undefined
  }
  if (selectedNodeData.value) {
    data.orgId = Number(selectedNodeData.value.orgId || selectedNodeData.value.id)
    if (selectedNodeData.value.nodeType === 'dept') {
      data.deptId = Number(selectedNodeData.value.id)
    }
  }
  const res: any = await createUser(data)
  if (res.code === 200) {
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadEmployees()
  }
}

const flatOrgList = computed(() => {
  const result: any[] = []
  function walk(list: any[]) {
    for (const item of list) {
      if (item.nodeType === 'org' && item.type !== 1) {
        result.push(item)
      }
      if (item.children) walk(item.children)
    }
  }
  walk(orgTreeData.value)
  return result
})

async function handleEditEmployee(row: any) {
  editDeptList.value = []
  editPostList.value = []
  if (row.orgId) {
    const deptRes: any = await getDeptList(row.orgId)
    if (deptRes.code === 200) editDeptList.value = deptRes.data
  }
  if (row.deptId) {
    const postRes: any = await getPostByDeptId(row.deptId)
    if (postRes.code === 200) editPostList.value = postRes.data
  }
  editForm.id = row.id
  editForm.nickname = row.nickname
  editForm.orgId = row.orgId || null
  editForm.deptId = row.deptId || null
  editForm.postId = row.postId || null
  editDialogVisible.value = true
}

function onEditOrgChange(orgId: number) {
  editForm.deptId = null
  editForm.postId = null
  editPostList.value = []
  if (!orgId) {
    editDeptList.value = []
    return
  }
  getDeptList(orgId).then((res: any) => {
    if (res.code === 200) editDeptList.value = res.data
  })
}

function onEditDeptChange(deptId: number) {
  editForm.postId = null
  if (!deptId) {
    editPostList.value = []
    return
  }
  getPostByDeptId(deptId).then((res: any) => {
    if (res.code === 200) editPostList.value = res.data
  })
}

async function handleEditSubmit() {
  if (!editForm.id) return
  const data: any = {
    orgId: editForm.orgId || null,
    deptId: editForm.deptId || null,
    postId: editForm.postId || null
  }
  const res: any = await updateUser(editForm.id, data)
  if (res.code === 200) {
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadEmployees()
  }
}

async function loadUserList() {
  userLoading.value = true
  try {
    const params: any = {
      pageNum: userPageNum.value,
      pageSize: userPageSize.value,
      keyword: searchKeyword.value || undefined,
      status: 1
    }
    const res: any = await getUserPage(params)
    if (res.code === 200) {
      userList.value = res.data.records
      userTotal.value = res.data.total
    }
  } finally {
    userLoading.value = false
  }
}

function handleUserSizeChange(size: number) {
  userPageSize.value = size
  loadUserList()
}

function handleUserCurrentChange(page: number) {
  userPageNum.value = page
  loadUserList()
}

function handleConfirmAddEmployees() {
  if (selectedUserIds.value.length === 0) {
    ElMessage.warning('请选择要添加的员工')
    return
  }
  
  const nodeType = selectedNodeData.value.nodeType
  const updatePromises: Promise<any>[] = []
  
  selectedUserIds.value.forEach(userId => {
    const updateData: any = {}
    if (nodeType === 'org') {
      updateData.orgId = selectedNodeData.value.id
      updateData.deptId = null
      updateData.postId = null
    } else if (nodeType === 'dept') {
      updateData.deptId = selectedNodeData.value.id
      updateData.orgId = selectedNodeData.value.orgId || findOrgIdByDeptId(selectedNodeData.value.id)
      updateData.postId = null
    }
    updatePromises.push(updateUser(userId, updateData))
  })
  
  Promise.all(updatePromises).then(results => {
    const success = results.every(r => r.code === 200)
    if (success) {
      ElMessage.success(`成功添加 ${selectedUserIds.value.length} 名员工`)
      employeeDialogVisible.value = false
      if (userTableRef.value) {
        userTableRef.value.clearSelection()
      }
      selectedUserIds.value = []
      loadEmployees()
    } else {
      ElMessage.error('添加失败')
    }
  }).catch(() => {
    ElMessage.error('添加失败')
  })
}

function findOrgIdByDeptId(deptId: number): number | null {
  for (const org of orgTreeData.value) {
    const found = findDeptInOrg(org, deptId)
    if (found) return org.id
  }
  return null
}

function findDeptInOrg(org: any, deptId: number): boolean {
  if (org.children) {
    for (const dept of org.children) {
      if (dept.id === deptId) return true
      if (dept.children && findDeptInOrg(dept, deptId)) return true
    }
  }
  return false
}

async function handleRemoveEmployee(row: any) {
  try {
    await ElMessageBox.confirm(`确定将「${row.nickname}」从当前组织移除？`, '提示', { type: 'warning' })
    const res: any = await updateUser(row.id, { orgId: null, deptId: null })
    if (res.code === 200) {
      ElMessage.success('移除成功')
      loadEmployees()
    }
  } catch {
    // cancelled
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  loadEmployees()
}

function handleCurrentChange(page: number) {
  pageNum.value = page
  loadEmployees()
}

onMounted(() => {
  loadOrgTree()
  document.addEventListener('click', (e) => {
    if (menuData.value) {
      const target = e.target as HTMLElement
      if (!target.closest('.actions-menu')) {
        hideMenu()
      }
    }
  })
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: calc(100vh - 100px);
}

.main-layout {
  display: flex;
  height: 100%;
  gap: 20px;
}

.left-panel {
  width: 320px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: visible;
}

.right-panel {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.panel-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 15px;
}

.panel-header span {
  font-weight: 600;
}

.current-scope {
  font-weight: normal;
  color: #666;
  font-size: 13px;
}

.panel-header button {
  margin-left: auto;
}

.tree-container {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
  overflow-x: visible;
}

.tree-container :deep(.el-tree) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree-node) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree-node__content) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree-node__children) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree-node__expand-icon) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree--highlight-current) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree-node__list) {
  overflow: visible !important;
}

.tree-container :deep(.el-tree-node__label) {
  overflow: visible !important;
}

.tree-node-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-count {
  font-size: 12px;
  color: #999;
}

.tree-actions {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
  z-index: 10;
  overflow: visible;
}

.actions-trigger {
  font-size: 16px;
  color: #999;
  opacity: 0;
  transition: opacity 0.2s;
  padding: 4px 8px;
  user-select: none;
}

.tree-node-wrapper:hover .actions-trigger {
  opacity: 1;
}

.actions-menu {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  min-width: 120px;
  padding: 4px 0;
  white-space: nowrap;
}

.actions-menu span {
  display: block;
  padding: 6px 16px;
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.actions-menu span:hover {
  background: #f5f7fa;
  color: #409eff;
}

.actions-menu .delete-action {
  color: #f56c6c;
}

.actions-menu .delete-action:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.pagination {
  margin-top: 15px;
  text-align: right;
}
</style>