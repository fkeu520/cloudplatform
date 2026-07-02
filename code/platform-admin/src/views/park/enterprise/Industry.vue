<template>
  <div class="page-container">
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>行业类型</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px">
      <div>
        <h2 style="font-size:20px;font-weight:700;margin:0">行业类型</h2>
        <p style="font-size:13px;color:#909399;margin:4px 0 0">管理行业分类体系，支持多级行业结构</p>
      </div>
      <el-button type="primary" @click="openAddMajorDialog">+ 新增大类</el-button>
    </div>

    <div class="layout">
      <!-- 左侧树 -->
      <div class="left-panel">
        <div class="left-panel-header">
          <strong>📂 行业分类</strong>
          <el-button size="small" circle @click="loadTree" title="刷新">🔄</el-button>
        </div>
        <div class="tree-panel">
          <div class="tree-node" :class="{ active: selectedNodeId === null }" @click="selectNode(null)">
            <span class="tree-toggle expanded" @click.stop="toggleRoot">▶</span>
            <span>🏭 全部分类</span>
          </div>
          <div class="tree-children" v-show="rootExpanded">
            <div v-for="major in treeData" :key="major.id" class="tree-branch">
              <div class="tree-node" :class="{ active: selectedNodeId === major.id }" @click="selectNode(major.id)">
                <span class="tree-toggle" :class="{ expanded: major._expanded, empty: !major.children?.length }"
                  @click.stop="toggleBranch(major)">▶</span>
                <span>{{ major.icon }} {{ major.name }}</span>
              </div>
              <div class="tree-children" v-show="major._expanded" v-if="major.children?.length">
                <div v-for="sub in major.children" :key="sub.id" class="tree-branch">
                  <div class="tree-node sub-node" :class="{ active: selectedNodeId === sub.id }" @click="selectNode(sub.id)">
                    <span class="tree-toggle" :class="{ expanded: sub._expanded, empty: !sub.children?.length }"
                      @click.stop="toggleBranch(sub)">▶</span>
                    <span>{{ sub.icon }} {{ sub.name }}</span>
                  </div>
                  <div class="tree-children" v-show="sub._expanded" v-if="sub.children?.length">
                    <div v-for="leaf in sub.children" :key="leaf.id" class="tree-node leaf-node"
                      :class="{ active: selectedNodeId === leaf.id }" @click="selectNode(leaf.id)">
                      <span class="tree-toggle empty">▶</span>
                      <span>{{ leaf.icon }} {{ leaf.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧详情 -->
      <div class="right-panel">
        <div class="right-panel-header">
          <h3>{{ currentNode?.icon || '🏭' }} {{ currentNode?.name || '全部分类' }}</h3>
          <div style="display:flex;gap:6px">
            <el-button v-if="currentNode && currentNode.level > 1 && currentNode.level < 4" size="small" @click="openEditDialog">✏️ 编辑</el-button>
            <el-button v-if="currentNode && currentNode.level < 4" size="small" type="success" @click="openAddSubDialog">+ 新增子类</el-button>
            <el-button v-if="currentNode && currentNode.id !== 0" size="small" type="danger" @click="handleDelete">🗑️ 删除</el-button>
          </div>
        </div>

        <!-- 全部分类概览 -->
        <template v-if="!currentNode">
          <el-row :gutter="12" style="margin-bottom:16px">
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ treeData.length }}</div><div class="stat-lbl">行业大类</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ allSubCount }}</div><div class="stat-lbl">子分类</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ totalEnterpriseCount }}</div><div class="stat-lbl">关联企业</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num" style="color:#34C759">●</div><div class="stat-lbl">系统运行中</div></el-card></el-col>
          </el-row>
          <div style="font-size:14px;font-weight:600;margin-bottom:12px">行业分类概览</div>
          <div class="overview-grid">
            <div v-for="m in treeData" :key="m.id" class="overview-card" @click="selectNode(m.id)">
              <span style="font-size:18px">{{ m.icon }}</span>
              <div><div style="font-size:13px;font-weight:500">{{ m.name }}</div><div style="font-size:11px;color:#909399">{{ m.children?.length || 0 }} 子类</div></div>
            </div>
          </div>
        </template>

        <!-- 单个行业详情 -->
        <template v-else>
          <el-row :gutter="12" style="margin-bottom:16px">
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ subList.length }}</div><div class="stat-lbl">子分类</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ currentNode?.enterpriseCount || 0 }}</div><div class="stat-lbl">关联企业</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ currentNode?.sort || 0 }}</div><div class="stat-lbl">排序</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num" :style="{ color: currentNode?.status === 1 ? '#34C759' : '#C0C4CC' }">●</div><div class="stat-lbl">{{ currentNode?.status === 1 ? '启用' : '停用' }}</div></el-card></el-col>
          </el-row>

          <div style="font-size:14px;font-weight:600;margin-bottom:12px">基本信息</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="行业编码">{{ currentNode?.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业名称">{{ currentNode?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上级分类">{{ currentNode?.parent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="排序号">{{ currentNode?.sort || 0 }}</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="currentNode?.status === 1 ? 'success' : 'info'" size="small">{{ currentNode?.status === 1 ? '启用' : '停用' }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentNode?.createTime || '2024-01-15 10:00:00' }}</el-descriptions-item>
          </el-descriptions>

          <div style="font-size:14px;font-weight:600;margin:16px 0 8px">子分类列表</div>
          <el-table :data="subList" v-loading="subLoading" border size="small" max-height="350">
            <el-table-column type="index" label="#" width="45" />
            <el-table-column prop="name" label="行业名称" min-width="160" />
            <el-table-column prop="code" label="编码" width="120" />
            <el-table-column prop="enterpriseCount" label="关联企业" width="90" />
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="80">
              <template #default="scope"><el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
                <el-button size="small" @click="toggleSubStatus(scope.row)">{{ scope.row.status === 1 ? '停用' : '启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" top="20vh">
      <el-form :model="dialogForm" label-width="100px">
        <el-form-item label="行业名称" prop="name">
          <el-input v-model="dialogForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="行业编码" prop="code">
          <el-input v-model="dialogForm.code" maxlength="16" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="dialogForm.sort" :min="0" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dialogForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" v-if="dialogType === 'major'">
          <el-input v-model="dialogForm.desc" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDialog">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// ─── Tree Data ───
interface TreeItem {
  id: number; icon: string; name: string; code: string; parent: string; sort: number; status: number
  level: number; enterpriseCount: number; createTime?: string
  children?: TreeItem[]; _expanded?: boolean
}

const INDUSTRY_DATA: TreeItem[] = [
  { id:1, icon:'💻', name:'信息技术', code:'I-65', parent:'全部分类', sort:8, status:1, level:1, enterpriseCount:156, children:[
    { id:2, icon:'📱', name:'软件开发', code:'I-65-01', parent:'信息技术', sort:1, status:1, level:2, enterpriseCount:98, children:[
      { id:3, icon:'🖥️', name:'应用软件开发', code:'I-65-01-01', parent:'软件开发', sort:1, status:1, level:3, enterpriseCount:56 },
      { id:4, icon:'💾', name:'系统软件开发', code:'I-65-01-02', parent:'软件开发', sort:2, status:1, level:3, enterpriseCount:42 },
    ]},
    { id:5, icon:'🌐', name:'互联网服务', code:'I-65-02', parent:'信息技术', sort:2, status:1, level:2, enterpriseCount:45 },
    { id:6, icon:'🤖', name:'人工智能', code:'I-65-03', parent:'信息技术', sort:3, status:0, level:2, enterpriseCount:13 },
  ]},
  { id:7, icon:'🏗️', name:'房地产', code:'I-70', parent:'全部分类', sort:1, status:1, level:1, enterpriseCount:101, children:[
    { id:8, icon:'🏢', name:'房地产开发', code:'I-70-01', parent:'房地产', sort:1, status:1, level:2, enterpriseCount:67 },
    { id:9, icon:'🏘️', name:'物业管理', code:'I-70-02', parent:'房地产', sort:2, status:1, level:2, enterpriseCount:34 },
  ]},
  { id:10, icon:'⚡', name:'能源环保', code:'I-75', parent:'全部分类', sort:2, status:1, level:1, enterpriseCount:70, children:[
    { id:11, icon:'☀️', name:'新能源', code:'I-75-01', parent:'能源环保', sort:1, status:1, level:2, enterpriseCount:42 },
    { id:12, icon:'♻️', name:'环保科技', code:'I-75-02', parent:'能源环保', sort:2, status:1, level:2, enterpriseCount:28 },
  ]},
  { id:13, icon:'🏥', name:'医疗健康', code:'I-80', parent:'全部分类', sort:3, status:1, level:1, enterpriseCount:95, children:[
    { id:14, icon:'💊', name:'生物医药', code:'I-80-01', parent:'医疗健康', sort:1, status:1, level:2, enterpriseCount:56 },
    { id:15, icon:'🏨', name:'医疗服务', code:'I-80-02', parent:'医疗健康', sort:2, status:1, level:2, enterpriseCount:39 },
  ]},
  { id:16, icon:'💰', name:'金融', code:'I-85', parent:'全部分类', sort:4, status:1, level:1, enterpriseCount:23, children:[
    { id:17, icon:'🏦', name:'银行保险', code:'I-85-01', parent:'金融', sort:1, status:1, level:2, enterpriseCount:23 },
    { id:18, icon:'📈', name:'证券投资', code:'I-85-02', parent:'金融', sort:2, status:0, level:2, enterpriseCount:0 },
  ]},
  { id:19, icon:'🚚', name:'物流运输', code:'I-90', parent:'全部分类', sort:5, status:1, level:1, enterpriseCount:31, children:[
    { id:20, icon:'📦', name:'快递物流', code:'I-90-01', parent:'物流运输', sort:1, status:1, level:2, enterpriseCount:31 },
    { id:21, icon:'🚢', name:'货运代理', code:'I-90-02', parent:'物流运输', sort:2, status:0, level:2, enterpriseCount:0 },
  ]},
  { id:22, icon:'🎓', name:'教育培训', code:'I-95', parent:'全部分类', sort:6, status:1, level:1, enterpriseCount:18, children:[
    { id:23, icon:'📚', name:'职业技能培训', code:'I-95-01', parent:'教育培训', sort:1, status:1, level:2, enterpriseCount:18 },
    { id:24, icon:'🎯', name:'素质教育', code:'I-95-02', parent:'教育培训', sort:2, status:0, level:2, enterpriseCount:0 },
  ]},
  { id:25, icon:'🏭', name:'制造业', code:'I-50', parent:'全部分类', sort:7, status:1, level:1, enterpriseCount:67, children:[
    { id:26, icon:'🔧', name:'装备制造', code:'I-50-01', parent:'制造业', sort:1, status:1, level:2, enterpriseCount:45 },
    { id:27, icon:'🧪', name:'化工新材料', code:'I-50-02', parent:'制造业', sort:2, status:1, level:2, enterpriseCount:22 },
  ]},
  { id:28, icon:'🌾', name:'现代农业', code:'I-55', parent:'全部分类', sort:9, status:1, level:1, enterpriseCount:12, children:[
    { id:29, icon:'🌿', name:'智慧农业', code:'I-55-01', parent:'现代农业', sort:1, status:1, level:2, enterpriseCount:12 },
    { id:30, icon:'🐄', name:'畜牧养殖', code:'I-55-02', parent:'现代农业', sort:2, status:0, level:2, enterpriseCount:0 },
  ]},
  { id:31, icon:'🎬', name:'文化传媒', code:'I-60', parent:'全部分类', sort:10, status:1, level:1, enterpriseCount:15, children:[
    { id:32, icon:'📺', name:'影视制作', code:'I-60-01', parent:'文化传媒', sort:1, status:1, level:2, enterpriseCount:15 },
    { id:33, icon:'📰', name:'广告营销', code:'I-60-02', parent:'文化传媒', sort:2, status:0, level:2, enterpriseCount:0 },
  ]},
  { id:34, icon:'🛒', name:'商贸服务', code:'I-45', parent:'全部分类', sort:11, status:1, level:1, enterpriseCount:27, children:[
    { id:35, icon:'🏪', name:'零售批发', code:'I-45-01', parent:'商贸服务', sort:1, status:1, level:2, enterpriseCount:27 },
    { id:36, icon:'🌍', name:'进出口贸易', code:'I-45-02', parent:'商贸服务', sort:2, status:0, level:2, enterpriseCount:0 },
  ]},
]

// ─── State ───
const rootExpanded = ref(true)
const selectedNodeId = ref<number | null>(null)
const treeData = ref<TreeItem[]>([])
const subLoading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref<'major' | 'sub'>('major')
const dialogForm = reactive({ name: '', code: '', sort: 0, status: 1, desc: '' })

// ─── Computed ───
const currentNode = computed(() => {
  if (selectedNodeId.value === null) return null
  return findNode(treeData.value, selectedNodeId.value)
})

const subList = computed(() => {
  const node = currentNode.value
  if (!node) return []
  return (node.children || []).map(c => ({
    ...c,
    enterpriseCount: countEnterprises(c),
  }))
})

const allSubCount = computed(() => {
  let count = 0
  for (const m of treeData.value) {
    count += m.children?.length || 0
  }
  return count
})

const totalEnterpriseCount = computed(() => {
  let total = 0
  for (const m of treeData.value) {
    total += m.enterpriseCount || 0
  }
  return total
})

// ─── Lifecycle ───
onMounted(() => {
  loadTree()
})

// ─── Tree ───
function loadTree() {
  treeData.value = INDUSTRY_DATA.map(m => ({
    ...m,
    _expanded: false,
    children: m.children?.map(s => ({ ...s, _expanded: false, children: s.children?.map(l => ({ ...l, _expanded: false })) })),
  }))
}

function findNode(items: TreeItem[], id: number): TreeItem | null {
  for (const item of items) {
    if (item.id === id) return item
    if (item.children) {
      const found = findNode(item.children, id)
      if (found) return found
    }
  }
  return null
}

function countEnterprises(node: TreeItem): number {
  let count = node.enterpriseCount || 0
  if (node.children) {
    for (const c of node.children) {
      count += countEnterprises(c)
    }
  }
  return count
}

function selectNode(id: number | null) {
  selectedNodeId.value = id
}

function toggleRoot() {
  rootExpanded.value = !rootExpanded.value
}

function toggleBranch(item: TreeItem) {
  item._expanded = !item._expanded
}

// ─── Dialogs ───
function openAddMajorDialog() {
  dialogType.value = 'major'
  dialogTitle.value = '新增大类'
  Object.assign(dialogForm, { name: '', code: '', sort: 0, status: 1, desc: '' })
  dialogVisible.value = true
}

function openAddSubDialog() {
  dialogType.value = 'sub'
  dialogTitle.value = `新增子分类 - ${currentNode.value?.name || ''}`
  Object.assign(dialogForm, { name: '', code: '', sort: 0, status: 1, desc: '' })
  dialogVisible.value = true
}

function openEditDialog(row?: any) {
  dialogType.value = 'sub'
  dialogTitle.value = '编辑分类'
  if (row) {
    Object.assign(dialogForm, { name: row.name, code: row.code, sort: row.sort, status: row.status, desc: '' })
  } else {
    const n = currentNode.value
    Object.assign(dialogForm, { name: n?.name || '', code: n?.code || '', sort: n?.sort || 0, status: n?.status || 1, desc: '' })
  }
  dialogVisible.value = true
}

function confirmDialog() {
  ElMessage.success(dialogType.value === 'major' ? '行业大类已添加 ✓' : '子分类已添加 ✓')
  dialogVisible.value = false
}

function handleDelete() {
  ElMessageBox.confirm(`确认删除「${currentNode.value?.name}」？`, '提示', { type: 'warning' })
    .then(() => { ElMessage.success('已删除'); selectedNodeId.value = null })
    .catch(() => {})
}

function toggleSubStatus(row: any) {
  row.status = row.status === 1 ? 0 : 1
  ElMessage.success(row.status === 1 ? '已启用' : '已停用')
}

// ─── Refresh (placeholder) ───
// function loadData() {}  // API call would go here
</script>

<style scoped>
.page-container { padding: 16px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

.layout { display: flex; gap: 20px; min-height: 500px; }
.left-panel { width: 280px; flex-shrink: 0; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; display: flex; flex-direction: column; }
.left-panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e4e7ed; font-size: 14px; }
.tree-panel { flex: 1; overflow-y: auto; padding: 8px 0; }
.right-panel { flex: 1; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; display: flex; flex-direction: column; }
.right-panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e4e7ed; }
.right-panel-header h3 { margin: 0; font-size: 15px; }
.right-panel :deep(.el-card__body) { padding: 20px 24px; }

.tree-node { display: flex; align-items: center; gap: 4px; padding: 7px 16px; cursor: pointer; font-size: 13px; transition: all .15s; border-left: 3px solid transparent; }
.tree-node:hover { background: #ecf5ff; }
.tree-node.active { background: #ecf5ff; border-left-color: #409EFF; font-weight: 600; }
.tree-node.sub-node { padding-left: 40px; font-size: 12px; }
.tree-node.leaf-node { padding-left: 64px; font-size: 12px; color: #606266; }
.tree-toggle { width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center; font-size: 10px; color: #C0C4CC; cursor: pointer; flex-shrink: 0; transition: transform .2s; }
.tree-toggle.expanded { transform: rotate(90deg); }
.tree-toggle.empty { visibility: hidden; }
.tree-children { }
.tree-branch { }

.stat-mini { text-align: center; padding: 8px; }
.stat-num { font-size: 20px; font-weight: 700; color: #409EFF; }
.stat-lbl { font-size: 11px; color: #909399; margin-top: 2px; }

.overview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.overview-card {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  background: #f5f7fa; border-radius: 8px; border: 1px solid #e4e7ed; cursor: pointer; transition: all .15s;
}
.overview-card:hover { border-color: #409EFF; box-shadow: 0 0 0 2px #ecf5ff; }
</style>