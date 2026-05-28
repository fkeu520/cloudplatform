<template>
  <div class="workflow-designer">
    <!-- Top Toolbar -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <el-button size="small" @click="undo" :disabled="!canUndo">
          <el-icon><RefreshLeft /></el-icon> 撤销
        </el-button>
        <el-button size="small" @click="redo" :disabled="!canRedo">
          <el-icon><RefreshRight /></el-icon> 重做
        </el-button>
        <el-divider direction="vertical" />
        <el-button size="small" @click="zoomIn">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
        <span class="zoom-level">{{ Math.round(zoom * 100) }}%</span>
        <el-button size="small" @click="zoomOut">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
        <el-button size="small" @click="zoomReset">
          <el-icon><FullScreen /></el-icon> 适配
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="previewVisible = true">
          <el-icon><View /></el-icon> 预览
        </el-button>
        <el-button type="primary" size="small" @click="saveAndPublish">
          <el-icon><Upload /></el-icon> 保存并发布
        </el-button>
        <el-button size="small" @click="saveDraft">
          <el-icon><Document /></el-icon> 保存
        </el-button>
      </div>
    </div>

    <div class="designer-body">
      <!-- Left Palette -->
      <div class="designer-palette">
        <div
          v-for="item in nodeItems"
          :key="item.type"
          class="palette-item node-palette-item"
          draggable="true"
          @dragstart="onPaletteDragStart($event, item)"
          :title="item.label"
        >
          <div class="palette-icon" :class="item.iconClass">
            <component :is="item.icon" />
          </div>
          <span class="palette-label">{{ item.label }}</span>
        </div>
      </div>

      <!-- Center Canvas -->
      <div
        class="designer-canvas"
        ref="canvasRef"
        @drop="onCanvasDrop"
        @dragover.prevent
        @click="onCanvasClick"
      >
        <div class="canvas-content" :style="{ transform: `scale(${zoom})`, transformOrigin: '0 0' }">
          <svg class="canvas-svg" width="2000" height="1000">
            <defs>
              <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
                <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#e8e8e8" stroke-width="0.5" />
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)" />
            <!-- Connections -->
            <g class="connections">
              <path
                v-for="conn in connections"
                :key="conn.id"
                :d="getConnectionPath(conn)"
                class="connection-line"
                :class="{ selected: conn.id === selectedConnectionId }"
                @click.stop="selectConnection(conn)"
              />
              <!-- Connection Action Menu -->
              <g v-if="selectedConnectionId" :transform="getConnectionActionPos(selectedConnection)">
                <g class="action-item" @click.stop="deleteConnection(selectedConnection)">
                  <rect width="36" height="36" rx="6" class="action-item-bg delete" />
                  <text x="18" y="24" text-anchor="middle" class="action-item-text">🗑</text>
                  <title>删除连线</title>
                </g>
              </g>
            </g>
            <!-- Nodes -->
            <g
              v-for="node in nodes"
              :key="node.id"
              class="node-group"
              :class="{ selected: selectedNodeId === node.id }"
              @mousedown.stop="onNodeMouseDown($event, node)"
              @click.stop="selectNode(node)"
            >
              <rect
                :x="node.x - 40"
                :y="node.y - 30"
                width="80"
                height="60"
                rx="8"
                class="node-rect"
                :class="getNodeClass(node)"
              />
              <text
                :x="node.x"
                :y="node.y - 5"
                text-anchor="middle"
                class="node-icon-text"
              >
                {{ getNodeIcon(node) }}
              </text>
              <text
                :x="node.x"
                :y="node.y + 15"
                text-anchor="middle"
                class="node-label"
              >
                {{ node.label }}
              </text>
              <!-- Connection Handle -->
              <circle
                v-if="node.type !== 'end'"
                :cx="node.x + 42"
                :cy="node.y"
                r="6"
                class="conn-handle"
                @mousedown.stop="onConnHandleMouseDown($event, node)"
              />
              <!-- Temporary Connection Line -->
              <line
                v-if="connectingFrom === node.id"
                :x1="node.x + 42"
                :y1="node.y"
                :x2="connMousePos.x"
                :y2="connMousePos.y"
                class="temp-connection"
              />
              <!-- Floating Action Menu -->
              <g v-if="selectedNodeId === node.id && node.type !== 'start' && node.type !== 'end'" class="node-actions-menu" :transform="'translate(' + (node.x + 55) + ', ' + (node.y - 55) + ')'">
                <!-- Row 1: Approval, Exclusive -->
                <g class="action-item" @click.stop="addNodeAfter(node, 'approval')">
                  <rect width="44" height="44" rx="8" class="action-item-bg approval" />
                  <text x="22" y="29" text-anchor="middle" class="action-item-text">审</text>
                  <title>添加审批任务</title>
                </g>
                <g class="action-item" @click.stop="addNodeAfter(node, 'exclusive')" transform="translate(54, 0)">
                  <rect width="44" height="44" rx="8" class="action-item-bg gateway" />
                  <text x="22" y="29" text-anchor="middle" class="action-item-text">◇</text>
                  <title>添加互斥网关</title>
                </g>
                <!-- Row 2: Parallel, Condition -->
                <g class="action-item" @click.stop="addNodeAfter(node, 'parallel')" transform="translate(0, 54)">
                  <rect width="44" height="44" rx="8" class="action-item-bg gateway" />
                  <text x="22" y="29" text-anchor="middle" class="action-item-text">+</text>
                  <title>添加并行网关</title>
                </g>
                <g class="action-item" @click.stop="addNodeAfter(node, 'condition')" transform="translate(54, 54)">
                  <rect width="44" height="44" rx="8" class="action-item-bg condition" />
                  <text x="22" y="29" text-anchor="middle" class="action-item-text">条</text>
                  <title>添加条件分支</title>
                </g>
                <!-- Row 3: Delete (centered) -->
                <g class="action-item" @click.stop="confirmDeleteNode(node)" transform="translate(27, 108)">
                  <rect width="44" height="44" rx="8" class="action-item-bg delete" />
                  <text x="22" y="29" text-anchor="middle" class="action-item-text">🗑</text>
                  <title>删除节点</title>
                </g>
              </g>
            </g>
          </svg>
        </div>
      </div>

      <!-- Right Properties Panel - Node -->
      <div class="designer-properties" v-if="selectedNode">
        <div class="properties-header">
          <div class="properties-title-row">
            <span class="properties-title">{{ getNodeTypeLabel(selectedNode.type) }}</span>
            <el-tag size="small" :type="getNodeTagType(selectedNode.type)">{{ getNodeTypeName(selectedNode.type) }}</el-tag>
            <div style="flex: 1" />
            <el-popconfirm title="确认删除该节点？" @confirm="deleteSelectedNode">
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" circle />
              </template>
            </el-popconfirm>
          </div>
          <div class="properties-id">ID: {{ selectedNode.id }}</div>
        </div>
        <div class="properties-content">
          <!-- General -->
          <div class="prop-section">
            <div class="prop-section-header">
              <el-icon><DocumentCopy /></el-icon>
              <span class="prop-section-title">常规设置</span>
            </div>
            <div class="prop-field">
              <label class="prop-label"><span class="required">*</span> 节点名称</label>
              <el-input 
                v-model="selectedNode.label" 
                size="small" 
                placeholder="请输入节点名称"
                clearable
              />
            </div>
            <div class="prop-field" v-if="selectedNode.type === 'approval' || selectedNode.type === 'callActivity'">
              <label class="prop-label">节点描述</label>
              <el-input 
                v-model="selectedNode.description" 
                size="small" 
                placeholder="可选"
                type="textarea"
                :rows="2"
              />
            </div>
            <div class="prop-field" v-if="selectedNode.type === 'callActivity'">
              <label class="prop-label"><span class="required">*</span> 子流程Key</label>
              <el-input 
                v-model="selectedNode.calledElement" 
                size="small" 
                placeholder="请输入已部署的流程定义Key"
                clearable
              />
              <div style="font-size:11px;color:#909399;margin-top:4px">输入已部署的流程定义 Key，运行时自动启动子流程</div>
            </div>
          </div>

          <!-- Candidate Config -->
          <div class="prop-section" v-if="selectedNode.type === 'approval'">
            <div class="prop-section-header">
              <el-icon><Setting /></el-icon>
              <span class="prop-section-title">配置候选</span>
            </div>
            <div class="prop-field">
              <label class="prop-label">候选范围</label>
              <el-radio-group v-model="selectedNode.candidateScope" size="small">
                <el-radio value="company">公司</el-radio>
                <el-radio value="dept">本部门</el-radio>
                <el-radio value="group">集团</el-radio>
              </el-radio-group>
            </div>
            <div class="prop-field">
              <label class="prop-label">选择方式</label>
              <el-radio-group v-model="selectedNode.candidateType" size="small">
                <el-radio value="personnel">按人员选择</el-radio>
                <el-radio value="position">按岗位选择</el-radio>
              </el-radio-group>
            </div>
            <div class="prop-field" v-if="selectedNode.candidateType === 'personnel'">
              <label class="prop-label">人员选择</label>
              <div class="selection-row">
                <template v-if="selectedNode.personnel?.length > 0">
                  <el-tag
                    v-for="uid in selectedNode.personnel"
                    :key="uid"
                    closable
                    size="small"
                    @close="removePersonnel(uid)"
                  >
                    {{ getUserName(uid) }}
                  </el-tag>
                </template>
                <el-button size="small" @click="openOrgTreeDialog('personnel')">
                  <el-icon><User /></el-icon> 选择人员
                </el-button>
              </div>
            </div>
            <div class="prop-field" v-if="selectedNode.candidateType === 'position'">
              <label class="prop-label">岗位选择</label>
              <div class="selection-row">
                <el-tag v-if="selectedNode.position" closable size="small" @close="removePosition">
                  {{ getPositionName(selectedNode.position) }}
                </el-tag>
                <el-button size="small" @click="openOrgTreeDialog('position')">
                  <el-icon><User /></el-icon> 选择岗位
                </el-button>
              </div>
            </div>
          </div>

          <!-- Submission Config -->
          <div class="prop-section" v-if="selectedNode.type === 'approval'">
            <div class="prop-section-header">
              <el-icon><Operation /></el-icon>
              <span class="prop-section-title">设置送审配置</span>
            </div>
            <div class="prop-field">
              <label class="prop-label">默认选中审批对象</label>
              <el-radio-group v-model="selectedNode.defaultTarget" size="small">
                <el-radio value="first">默认首位</el-radio>
                <el-radio value="manual">手动选择</el-radio>
              </el-radio-group>
            </div>
          </div>

          <!-- Approval Rules -->
          <div class="prop-section" v-if="selectedNode.type === 'approval'">
            <div class="prop-section-header">
              <el-icon><CircleCheck /></el-icon>
              <span class="prop-section-title">设置审批规则</span>
            </div>
            <div class="prop-field">
              <label class="prop-label">审批模式</label>
              <el-radio-group v-model="selectedNode.approvalMode" size="small">
                <el-radio value="single">单签模式</el-radio>
                <el-radio value="countersign">会签模式</el-radio>
                <el-radio value="orsign">或签模式</el-radio>
              </el-radio-group>
            </div>
            <div class="prop-field">
              <div class="prop-label-row">
                <label class="prop-label">连续审批是否跳过</label>
                <el-tooltip content="审批人相同时自动跳过" placement="top">
                  <el-icon class="help-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-switch v-model="selectedNode.skipContinuous" size="small" />
            </div>
            <div class="prop-field">
              <label class="prop-label">是否开启电子签名</label>
              <el-switch v-model="selectedNode.eSignature" size="small" />
            </div>
          </div>
        </div>
      </div>

      <!-- Right Properties Panel - Connection -->
      <div class="designer-properties" v-if="selectedConnection && !selectedNode">
        <div class="properties-header">
          <div class="properties-title-row">
            <span class="properties-title">连线配置</span>
            <el-tag size="small" type="info">CONNECTION</el-tag>
            <div style="flex: 1" />
            <el-button size="small" type="danger" :icon="Delete" circle @click="deleteConnection(selectedConnection)" />
          </div>
          <div class="properties-id">ID: {{ selectedConnection.id }}</div>
        </div>
        <div class="properties-content">
          <div class="prop-section">
            <div class="prop-section-header">
              <el-icon><Connection /></el-icon>
              <span class="prop-section-title">连线信息</span>
            </div>
            <div class="prop-field">
              <label class="prop-label">来源节点</label>
              <el-select v-model="selectedConnection.from" size="small" placeholder="选择来源节点" style="width: 100%">
                <el-option v-for="n in nodes" :key="n.id" :label="n.label" :value="n.id" />
              </el-select>
            </div>
            <div class="prop-field">
              <label class="prop-label">目标节点</label>
              <el-select v-model="selectedConnection.to" size="small" placeholder="选择目标节点" style="width: 100%">
                <el-option v-for="n in nodes" :key="n.id" :label="n.label" :value="n.id" :disabled="n.id === selectedConnection.from" />
              </el-select>
            </div>
            <div class="prop-field" v-if="isExclusiveSource">
              <label class="prop-label">条件表达式</label>
              <el-input
                v-model="selectedConnection.conditionExpr"
                size="small"
                placeholder='例: ${day > 3}'
                clearable
              />
              <div style="font-size:11px;color:#909399;margin-top:4px">使用 ${变量} 格式，满足条件时走此分支</div>
            </div>
            <div class="prop-field" style="margin-top: 24px;">
              <el-button type="danger" size="small" @click="deleteConnection(selectedConnection)" style="width: 100%">
                <el-icon><Delete /></el-icon> 删除此连线
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Org Tree Selection Dialog -->
    <el-dialog 
      v-model="orgTreeDialogVisible" 
      title="候选人配置" 
      width="640px"
      :close-on-click-modal="false"
    >
      <!-- Search bar -->
      <div class="dialog-search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索姓名/部门"
          size="small"
          clearable
          @input="onSearchInput"
        />
      </div>

      <!-- Tabs -->
      <el-tabs v-model="dialogSelectMode" @tab-change="onTabChange" class="dialog-tabs">
        <el-tab-pane label="人员选择" name="personnel">
          <!-- Tree mode -->
          <div v-if="!searchMode" class="org-tree-dialog">
            <div class="dialog-info-bar">
              <el-tag size="small" type="info">
                候选范围：{{ selectedNode?.candidateScope === 'company' ? '当前公司' : selectedNode?.candidateScope === 'dept' ? '本部门' : '集团' }}
              </el-tag>
              <span class="dialog-hint">可多选人员</span>
            </div>
            <el-tree
              ref="orgTreeRef"
              :data="filteredOrgTree"
              :props="treeProps"
              show-checkbox
              default-expand-all
              :check-on-click-node="false"
              node-key="id"
              @check="onTreeCheck"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-icon v-if="data.type === 'org'" class="tree-icon-org"><OfficeBuilding /></el-icon>
                  <el-icon v-else-if="data.type === 'user'" class="tree-icon-user"><User /></el-icon>
                  <el-icon v-else-if="data.type === 'post'" class="tree-icon-post"><UserFilled /></el-icon>
                  <span class="tree-label">{{ node.label }}</span>
                  <span v-if="data.type === 'user'" class="tree-meta">{{ data.orgName }}</span>
                </span>
              </template>
            </el-tree>
          </div>

          <!-- Search mode -->
          <div v-else class="search-results-container">
            <div class="search-results-header">
              <span class="search-results-count">共 {{ searchResults.total }} 条结果</span>
            </div>
            <div class="search-results-list">
              <div
                v-for="item in searchResults.records"
                :key="item.id"
                class="search-result-item"
                :class="{ checked: isSearchItemChecked(item) }"
                @click="toggleSearchItem(item)"
              >
                <el-checkbox
                  :model-value="isSearchItemChecked(item)"
                  size="small"
                  @click.stop
                  @update:model-value="(val: boolean) => toggleSearchItem(item, val)"
                />
                <div class="result-item-info">
                  <span class="result-item-name">{{ getSearchItemName(item) }}</span>
                  <span class="result-item-meta">{{ getSearchItemMeta(item) }}</span>
                </div>
              </div>
            </div>
            <el-pagination
              v-if="searchResults.total > searchPageSize"
              v-model:current-page="searchPageNum"
              :page-size="searchPageSize"
              :total="searchResults.total"
              small
              layout="prev, pager, next"
              @current-change="onSearchPageChange"
            />
          </div>

          <!-- Selected display -->
          <div v-if="checkedDisplayList.length > 0" class="dialog-selected-bar">
            <label class="dialog-selected-label">已选：</label>
            <div class="dialog-selected-tags">
              <el-tag
                v-for="item in checkedDisplayList"
                :key="item.id"
                closable
                size="small"
                @close="removeChecked(item.id)"
              >
                {{ item.name }}
              </el-tag>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="岗位选择" name="position">
          <!-- Tree mode -->
          <div v-if="!searchMode" class="org-tree-dialog">
            <div class="dialog-info-bar">
              <el-tag size="small" type="info">
                候选范围：{{ selectedNode?.candidateScope === 'company' ? '当前公司' : selectedNode?.candidateScope === 'dept' ? '本部门' : '集团' }}
              </el-tag>
              <span class="dialog-hint">可多选岗位</span>
            </div>
            <el-tree
              ref="orgTreeRef"
              :data="filteredOrgTree"
              :props="treeProps"
              show-checkbox
              default-expand-all
              :check-on-click-node="false"
              node-key="id"
              @check="onTreeCheck"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-icon v-if="data.type === 'org'" class="tree-icon-org"><OfficeBuilding /></el-icon>
                  <el-icon v-else-if="data.type === 'user'" class="tree-icon-user"><User /></el-icon>
                  <el-icon v-else-if="data.type === 'post'" class="tree-icon-post"><UserFilled /></el-icon>
                  <span class="tree-label">{{ node.label }}</span>
                  <span v-if="data.type === 'post'" class="tree-meta">{{ data.orgName }}</span>
                </span>
              </template>
            </el-tree>
          </div>

          <!-- Search mode -->
          <div v-else class="search-results-container">
            <div class="search-results-header">
              <span class="search-results-count">共 {{ searchResults.total }} 条结果</span>
            </div>
            <div class="search-results-list">
              <div
                v-for="item in searchResults.records"
                :key="item.id"
                class="search-result-item"
                :class="{ checked: isSearchItemChecked(item) }"
                @click="toggleSearchItem(item)"
              >
                <el-checkbox
                  :model-value="isSearchItemChecked(item)"
                  size="small"
                  @click.stop
                  @update:model-value="(val: boolean) => toggleSearchItem(item, val)"
                />
                <div class="result-item-info">
                  <span class="result-item-name">{{ getSearchItemName(item) }}</span>
                  <span class="result-item-meta">{{ getSearchItemMeta(item) }}</span>
                </div>
              </div>
            </div>
            <el-pagination
              v-if="searchResults.total > searchPageSize"
              v-model:current-page="searchPageNum"
              :page-size="searchPageSize"
              :total="searchResults.total"
              small
              layout="prev, pager, next"
              @current-change="onSearchPageChange"
            />
          </div>

          <!-- Selected display -->
          <div v-if="checkedPostIds.length > 0" class="dialog-selected-bar">
            <label class="dialog-selected-label">已选：</label>
            <div class="dialog-selected-tags">
              <el-tag
                v-for="item in checkedPositionDisplayList"
                :key="item.id"
                closable
                size="small"
                @close="removeChecked(item.id)"
              >
                {{ item.name }}
              </el-tag>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button size="small" @click="orgTreeDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="confirmTreeSelection">确定</el-button>
      </template>
    </el-dialog>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="流程预览" width="800px" top="5vh">
      <div class="preview-canvas" ref="previewRef">
        <svg width="100%" :viewBox="`0 0 2000 1000`">
          <g class="connections">
            <path v-for="conn in connections" :key="conn.id" :d="getConnectionPath(conn)" class="connection-line" />
          </g>
          <g v-for="node in nodes" :key="node.id" :transform="`translate(${node.x}, ${node.y})`">
            <rect x="-40" y="-30" width="80" height="60" rx="8" class="preview-node" />
            <text x="0" y="4" text-anchor="middle" class="preview-text">{{ node.label || getNodeTypeLabel(node.type) }}</text>
          </g>
        </svg>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'publish'): void
}>()

defineExpose({ generateBpmnXml })
import { useUserStore } from '@/stores/user'
import { getOrgTree } from '@/api/org'
import { getUserPage } from '@/api/user'
import { getPostPage } from '@/api/post'
import {
  RefreshLeft, RefreshRight, ZoomIn, ZoomOut, FullScreen,
  Upload, Document,
  CircleCheck, CircleClose, User, UserFilled,
  CirclePlus, Plus, Setting, Operation, DocumentCopy,
  QuestionFilled, Delete, Connection, OfficeBuilding, Folder, View
} from '@element-plus/icons-vue'

const canvasRef = ref<HTMLElement>()
const zoom = ref(1)
const selectedNodeId = ref<string | null>(null)
const selectedConnectionId = ref<string | null>(null)
const userStore = useUserStore()

let nodeIdCounter = 0
let connIdCounter = 0

const nodes = ref<any[]>([
  { id: 'start', type: 'start', label: '开始', x: 200, y: 300 },
  { id: 'approval1', type: 'approval', label: '部门审批', x: 400, y: 300,
    description: '', candidateScope: 'company', candidateType: 'personnel',
    personnel: [], position: null,
    defaultTarget: 'manual', approvalMode: 'single', skipContinuous: false, eSignature: false },
  { id: 'end', type: 'end', label: '结束', x: 600, y: 300 }
])

const connections = ref<any[]>([
  { id: 'conn1', from: 'start', to: 'approval1' },
  { id: 'conn2', from: 'approval1', to: 'end' }
])

const history = ref<any[]>([])
const historyIndex = ref(-1)

const orgTree = ref<any[]>([])
const allEmployees = ref<any[]>([])
const allPositions = ref<any[]>([])
const loading = ref(false)

// Dialog state
const orgTreeDialogVisible = ref(false)
const previewVisible = ref(false)
const dialogSelectMode = ref<'personnel' | 'position'>('personnel')
const orgTreeRef = ref()
const checkedUserIds = ref<string[]>([])
const checkedPostIds = ref<string[]>([])

// Search state
const searchKeyword = ref('')
const searchMode = ref(false)
const searchPageNum = ref(1)
const searchPageSize = ref(20)

async function loadOrgTree() {
  try {
    const res = await getOrgTree() as any
    const data = res?.data || res || []
    // Add type field to org nodes
    function addType(nodes: any[]): any[] {
      return nodes.map((n: any): any => ({
        ...n,
        type: 'org',
        children: n.children ? addType(n.children) : []
      }))
    }
    orgTree.value = addType(data)
    console.log('OrgTree loaded:', orgTree.value)
  } catch (e) {
    console.error('Failed to load org tree:', e)
    orgTree.value = []
  }
}

async function loadAllData() {
  loading.value = true
  try {
    // Load all employees (status=1 = active)
    const empRes = await getUserPage({ pageNum: 1, pageSize: 500, status: 1 }) as any
    allEmployees.value = empRes?.data?.records || empRes?.records || []
    console.log('Employees loaded:', allEmployees.value.length)

    // Load all positions
    const posRes = await getPostPage({ pageNum: 1, pageSize: 200 }) as any
    allPositions.value = posRes?.data?.records || posRes?.records || []
    console.log('Positions loaded:', allPositions.value.length)

    // Build combined org tree with users and posts
    const tree = JSON.parse(JSON.stringify(orgTree.value))
    
    function buildTreeWithMembers(nodes: any[]): any[] {
      return nodes.map((node: any): any => {
        const children: any[] = node.children ? buildTreeWithMembers(node.children) : []
        
        // Use == for type coercion (string id vs number deptId)
        const usersInOrg = allEmployees.value.filter((u: any) => 
          u.deptId == node.id || u.orgId == node.id
        )
        const userNodes = usersInOrg.map((u: any) => ({
          id: 'user_' + u.id,
          name: u.nickname || u.name || u.username,
          orgName: u.orgName,
          type: 'user'
        }))
        
        const postsInOrg = allPositions.value.filter((p: any) => 
          p.deptId == node.id || p.orgId == node.id
        )
        const postNodes = postsInOrg.map((p: any) => ({
          id: 'post_' + p.id,
          name: p.name,
          type: 'post'
        }))
        
        return {
          ...node,
          children: [...children, ...userNodes, ...postNodes]
        }
      })
    }
    
    orgTree.value = buildTreeWithMembers(tree)
    console.log('Combined tree:', orgTree.value)
  } catch (e) {
    console.error('Failed to load data:', e)
    allEmployees.value = []
    allPositions.value = []
  } finally {
    loading.value = false
  }
}

const deptOrgMap = computed(() => {
  const map = new Map<any, any>()
  for (const u of allEmployees.value) {
    if (u.deptId != null && u.orgId != null && !map.has(u.deptId)) {
      map.set(String(u.deptId), String(u.orgId))
    }
  }
  return map
})

function getCurrentUserId(): string | null {
  const info = userStore.userInfo
  if (info && typeof info === 'object') {
    if (info.id) return String(info.id)
    if (info.userId) return String(info.userId)
  }
  try {
    const token = localStorage.getItem('token')
    if (!token) return null
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.sub || payload.userId || null
  } catch {
    return null
  }
}

function getCurrentUserOrgDept() {
  let orgId = userStore.userInfo?.orgId
  let deptId = userStore.userInfo?.deptId
  if (orgId && deptId) return { orgId, deptId }
  const uid = getCurrentUserId()
  if (uid && allEmployees.value.length > 0) {
    const user = allEmployees.value.find((u: any) => String(u.id) === uid)
    if (user) {
      orgId = orgId || user.orgId
      deptId = deptId || user.deptId
    }
  }
  const deptOrgId = deptId != null ? deptOrgMap.value.get(String(deptId)) || orgId : orgId
  return { orgId, deptId, deptOrgId }
}

const filteredOrgTree = computed(() => {
  const node = selectedNode.value
  if (!node) return orgTree.value
  
  const { orgId: userOrgId, deptOrgId } = getCurrentUserOrgDept()
  const isPersonnelMode = dialogSelectMode.value === 'personnel'
  
  function findBranch(nodes: any[], targetId: any): any[] {
    for (const n of nodes) {
      if (n.type !== 'org') continue
      if (n.id == targetId) return [JSON.parse(JSON.stringify(n))]
      if (n.children) {
        const found = findBranch(n.children, targetId)
        if (found.length > 0) return found
      }
    }
    return []
  }
  
  function keepOnlyOrg(nodes: any[]): any[] {
    return nodes.map((n: any): any => {
      if (n.type === 'org') {
        return {
          ...n,
          children: n.children ? n.children.filter((c: any) => c.type !== 'org') : []
        }
      }
      return n
    })
  }
  
  function filterLeafNodes(nodes: any[]): any[] {
    return nodes.map((n: any): any => {
      if (n.type === 'org') {
        const filteredChildren = n.children ? filterLeafNodes(n.children) : []
        let filtered = { ...n, children: filteredChildren }
        if (filteredChildren) {
          filtered.children = filteredChildren.filter((c: any) => {
            if (c.type === 'user') return isPersonnelMode
            if (c.type === 'post') return !isPersonnelMode
            return true
          })
        }
        return filtered
      }
      return n
    })
  }
  
  let result = JSON.parse(JSON.stringify(orgTree.value))
  
  if (node.candidateScope === 'company' && userOrgId) {
    result = findBranch(result, userOrgId)
  } else if (node.candidateScope === 'dept' && deptOrgId) {
    result = findBranch(result, deptOrgId)
    result = keepOnlyOrg(result)
  }
  
  result = filterLeafNodes(result)
  
  return result
})

const treeProps = {
  children: 'children',
  label: 'name',
  type: 'type'
}

const nodeItems = [
  { type: 'start', label: '开始', icon: CircleCheck, iconClass: 'icon-start' },
  { type: 'end', label: '结束', icon: CircleClose, iconClass: 'icon-end' },
  { type: 'approval', label: '审批任务', icon: User, iconClass: 'icon-approval' },
  { type: 'exclusive', label: '互斥网关', icon: CirclePlus, iconClass: 'icon-gateway' },
  { type: 'parallel', label: '并行网关', icon: Plus, iconClass: 'icon-gateway' },
  { type: 'callActivity', label: '子流程', icon: Folder, iconClass: 'icon-call' }
]

const selectedNode = computed(() => nodes.value.find(n => n.id === selectedNodeId.value) || null)
const selectedConnection = computed(() => connections.value.find(c => c.id === selectedConnectionId.value) || null)

const isExclusiveSource = computed(() => {
  const conn = selectedConnection.value
  if (!conn) return false
  const src = nodes.value.find((n: any) => n.id === conn.from)
  return src && src.type === 'exclusive'
})
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

watch(() => selectedNode.value?.candidateScope, (newVal, oldVal) => {
  if (oldVal && newVal !== oldVal && selectedNode.value) {
    selectedNode.value.personnel = []
    selectedNode.value.position = null
  }
})

function getNodeTypeMap(): Record<string, string> {
  return { 'startEvent': 'start', 'endEvent': 'end', 'userTask': 'approval', 'exclusiveGateway': 'exclusive', 'parallelGateway': 'parallel', 'callActivity': 'callActivity' }
}

function parseBpmnXml(xml: string) {
  if (!xml || xml === '<?xml') return
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(xml, 'text/xml')
    const ns = 'http://www.omg.org/spec/BPMN/20100524/MODEL'
    const diNs = 'http://www.omg.org/spec/BPMN/20100524/DI'
    const dcNs = 'http://www.omg.org/spec/DD/20100524/DC'

    const processEl = doc.getElementsByTagNameNS(ns, 'process')[0] ||
      doc.querySelector('process') as Element | undefined
    if (!processEl) return

    const typeMap = getNodeTypeMap()
    const parsedNodes: any[] = []
    const parsedConns: any[] = []
    const shapePositions = new Map<string, { x: number; y: number }>()

    // Read shapes from BPMNDiagram for positions
    const diagram = doc.getElementsByTagNameNS(diNs, 'BPMNDiagram')[0] ||
      doc.querySelector('BPMNDiagram') as Element | undefined
    if (diagram) {
      const plane = diagram.getElementsByTagNameNS(diNs, 'BPMNPlane')[0] ||
        diagram.querySelector('BPMNPlane') as Element | undefined
      if (plane) {
        const shapes = plane.querySelectorAll('BPMNShape, [xmlns\\:bpmndi] BPMNShape')
        shapes.forEach((s) => {
          const elem = s.getAttribute('bpmnElement') || s.getAttributeNS(ns, 'bpmnElement') || ''
          const bounds = s.querySelector('Bounds') || s.getElementsByTagNameNS(dcNs, 'Bounds')[0] as Element
          if (elem && bounds) {
            const x = parseFloat(bounds.getAttribute('x') || '0')
            const y = parseFloat(bounds.getAttribute('y') || '0')
            shapePositions.set(elem, { x: x + 40, y: y + 30 })
          }
        })
      }
    }

    // Read process elements
    let lastId = 0
    for (let i = 0; i < processEl.children.length; i++) {
      const el = processEl.children[i] as Element
      const tag = el.localName || el.tagName.split(':').pop() || ''
      const bpmnType = typeMap[tag]
      if (!bpmnType) continue

      const id = el.getAttribute('id') || ''
      const name = el.getAttribute('name') || ''
      const pos = shapePositions.get(id)
      const x = pos?.x || 200 + (lastId % 3) * 200
      const y = pos?.y || 200 + Math.floor(lastId / 3) * 120

      const node: any = {
        id, type: bpmnType, label: name || getNodeTypeLabel(bpmnType),
        x, y, description: '',
        candidateScope: 'company', candidateType: 'personnel',
        personnel: [], position: null,
        defaultTarget: 'manual', approvalMode: 'single',
        skipContinuous: false, eSignature: false
      }

      if (bpmnType === 'approval') {
        let extEl: Element | null = null
        for (let ci = 0; ci < el.childNodes.length; ci++) {
          const c = el.childNodes[ci]
          if (c.nodeType === 1 && (c as Element).localName === 'extensionElements') {
            extEl = c as Element
            break
          }
        }
        if (extEl) {
          for (let ci = 0; ci < extEl.childNodes.length; ci++) {
            const child = extEl.childNodes[ci]
            if (child.nodeType !== 1) continue
            const tagName = (child as Element).localName || ''
            if (tagName === 'candidateUsers') {
              node.personnel = (child.textContent || '').split(',').map((s: string) => s.trim()).filter(Boolean)
            }
            if (tagName === 'candidateGroups') {
              const groupVal = (child.textContent || '').split(',').map((s: string) => s.trim()).filter(Boolean)
              const posId = groupVal.find((g: string) => g.startsWith('group_'))
              if (posId) node.position = parseInt(posId.replace('group_', ''))
              else node.position = parseInt(groupVal[0]) || null
            }
          }
        }
      }

      parsedNodes.push(node)
      lastId++
    }

    // Read sequence flows
    for (let i = 0; i < processEl.children.length; i++) {
      const el = processEl.children[i] as Element
      const tag = el.localName || el.tagName.split(':').pop() || ''
      if (tag !== 'sequenceFlow') continue
      parsedConns.push({
        id: el.getAttribute('id') || '',
        from: el.getAttribute('sourceRef') || '',
        to: el.getAttribute('targetRef') || ''
      })
    }

    if (parsedNodes.length > 0) {
      nodes.value = parsedNodes
      connections.value = parsedConns
      nodeIdCounter = parsedNodes.filter((n: any) => n.id.startsWith('node_')).length
      connIdCounter = parsedConns.length
    }
  } catch (e) {
    console.warn('Failed to parse BPMN XML:', e)
  }
}

watch(() => props.modelValue, (val) => {
  if (val && val !== '<?xml') parseBpmnXml(val)
}, { immediate: true })

// Search computed
function getScopeOrgIds(): Set<any> {
  const ids = new Set<any>()
  function collect(nodes: any[]) {
    for (const n of nodes) {
      if (n.type !== 'org') continue
      ids.add(n.id)
      if (n.children) collect(n.children)
    }
  }
  const node = selectedNode.value
  if (!node) return ids
  const { orgId: userOrgId, deptOrgId } = getCurrentUserOrgDept()
  let scopeTree: any[] = []
  const tree = JSON.parse(JSON.stringify(orgTree.value))
  if (node.candidateScope === 'company' && userOrgId) {
    scopeTree = findBranchInTree(tree, userOrgId)
  } else if (node.candidateScope === 'dept' && deptOrgId) {
    scopeTree = findBranchInTree(tree, deptOrgId)
  } else {
    scopeTree = tree
  }
  collect(scopeTree)
  return ids
}

function findBranchInTree(nodes: any[], targetId: any): any[] {
  for (const n of nodes) {
    if (n.type !== 'org') continue
    if (n.id == targetId) return [n]
    if (n.children) {
      const found = findBranchInTree(n.children, targetId)
      if (found.length > 0) return found
    }
  }
  return []
}

const searchResults = computed(() => {
  if (!searchKeyword.value.trim()) return { records: [], total: 0 }
  const kw = searchKeyword.value.trim().toLowerCase()
  const scopeIds = getScopeOrgIds()
  if (dialogSelectMode.value === 'personnel') {
    const filtered = allEmployees.value.filter((u: any) => {
      if (!scopeIds.has(u.orgId) && !scopeIds.has(u.deptId)) return false
      const name = (u.nickname || u.name || u.username || '').toLowerCase()
      const org = (u.orgName || '').toLowerCase()
      return name.includes(kw) || org.includes(kw)
    })
    const start = (searchPageNum.value - 1) * searchPageSize.value
    return {
      records: filtered.slice(start, start + searchPageSize.value),
      total: filtered.length
    }
  } else {
    const filtered = allPositions.value.filter((p: any) => {
      if (!scopeIds.has(p.orgId) && !scopeIds.has(p.deptId)) return false
      const name = (p.name || '').toLowerCase()
      const org = (p.orgName || '').toLowerCase()
      return name.includes(kw) || org.includes(kw)
    })
    const start = (searchPageNum.value - 1) * searchPageSize.value
    return {
      records: filtered.slice(start, start + searchPageSize.value),
      total: filtered.length
    }
  }
})

const checkedDisplayList = computed(() => {
  return checkedUserIds.value.map((id: string) => {
    const realId = id.replace('user_', '')
    const user = allEmployees.value.find((u: any) => String(u.id) === realId)
    return { id, name: user ? (user.nickname || user.name || user.username) : realId }
  })
})

const checkedPositionDisplayList = computed(() => {
  return checkedPostIds.value.map((id: string) => {
    const realId = id.replace('post_', '')
    const pos = allPositions.value.find((p: any) => String(p.id) === realId)
    return { id, name: pos?.name || realId }
  })
})

function isSearchItemChecked(item: any): boolean {
  if (dialogSelectMode.value === 'personnel') {
    return checkedUserIds.value.includes('user_' + item.id)
  }
  return checkedPostIds.value.includes('post_' + item.id)
}

function toggleSearchItem(item: any, val?: boolean) {
  if (dialogSelectMode.value === 'personnel') {
    const id = 'user_' + item.id
    const checked = val !== undefined ? val : !checkedUserIds.value.includes(id)
    if (checked) {
      if (!checkedUserIds.value.includes(id)) {
        checkedUserIds.value.push(id)
      }
    } else {
      checkedUserIds.value = checkedUserIds.value.filter(i => i !== id)
    }
  } else {
    const id = 'post_' + item.id
    const checked = val !== undefined ? val : !checkedPostIds.value.includes(id)
    if (checked) {
      if (!checkedPostIds.value.includes(id)) {
        checkedPostIds.value.push(id)
      }
    } else {
      checkedPostIds.value = checkedPostIds.value.filter(i => i !== id)
    }
  }
}

function getSearchItemName(item: any): string {
  if (dialogSelectMode.value === 'personnel') {
    return item.nickname || item.name || item.username
  }
  return item.name || ''
}

function getSearchItemMeta(item: any): string {
  if (dialogSelectMode.value === 'personnel') {
    return item.orgName || ''
  }
  return item.orgName || ''
}

function onTabChange(tabName: string) {
  searchKeyword.value = ''
  searchMode.value = false
  searchPageNum.value = 1
  nextTick(() => {
    if (orgTreeRef.value) {
      const keys = tabName === 'personnel' ? checkedUserIds.value : checkedPostIds.value
      orgTreeRef.value.setCheckedKeys(keys)
    }
  })
}

function onSearchInput(val: string | number) {
  searchKeyword.value = String(val)
  searchPageNum.value = 1
  searchMode.value = String(val).trim().length > 0
}

function onSearchPageChange(page: number) {
  searchPageNum.value = page
}

function removeChecked(id: string) {
  if (dialogSelectMode.value === 'personnel') {
    checkedUserIds.value = checkedUserIds.value.filter(i => i !== id)
  } else {
    checkedPostIds.value = checkedPostIds.value.filter(i => i !== id)
  }
}

function getUserName(uid: string): string {
  const user = allEmployees.value.find((u: any) => String(u.id) === String(uid))
  return user ? (user.nickname || user.name || user.username) : uid
}

function getPositionName(pid: number | null): string {
  if (pid == null) return ''
  const pos = allPositions.value.find((p: any) => Number(p.id) === Number(pid))
  return pos?.name || `岗位(${pid})`
}

function removePersonnel(uid: string) {
  const node = selectedNode.value
  if (!node) return
  node.personnel = node.personnel.filter((id: string) => String(id) !== String(uid))
  const remaining = allEmployees.value.filter((u: any) => node.personnel.includes(String(u.id)))
  node.personnelNames = remaining.map((u: any) => u.nickname || u.name || u.username).join(', ')
}

function removePosition() {
  const node = selectedNode.value
  if (!node) return
  node.position = null
  node.positionName = ''
}

function openOrgTreeDialog(mode: 'personnel' | 'position') {
  dialogSelectMode.value = mode
  searchKeyword.value = ''
  searchMode.value = false
  searchPageNum.value = 1
  // Init both tabs' checked state
  const node = selectedNode.value
  const personnel = node?.personnel || []
  checkedUserIds.value = personnel.map((id: any) => 'user_' + id)
  const pos = node?.position
  checkedPostIds.value = pos ? ['post_' + pos] : []
  orgTreeDialogVisible.value = true
  // Sync el-tree checkboxes after dialog renders
  nextTick(() => {
    if (orgTreeRef.value) {
      const keys = mode === 'personnel' ? checkedUserIds.value : checkedPostIds.value
      orgTreeRef.value.setCheckedKeys(keys)
    }
  })
}

function onTreeCheck(_data: any, params?: any) {
  const checkedNodes = params?.checkedNodes || []
  if (dialogSelectMode.value === 'personnel') {
    checkedUserIds.value = checkedNodes
      .filter((n: any) => n.type === 'user')
      .map((n: any) => n.id)
  } else {
    checkedPostIds.value = checkedNodes
      .filter((n: any) => n.type === 'post')
      .map((n: any) => n.id)
  }
}

function confirmTreeSelection() {
  const node = selectedNode.value
  if (!node) return
  
  // Save personnel selection (from "人员选择" tab)
  const realPersonnelIds = checkedUserIds.value.map((id: string) => id.replace('user_', ''))
  node.personnel = realPersonnelIds
  const selectedUsers = allEmployees.value.filter((u: any) => realPersonnelIds.includes(String(u.id)))
  node.personnelNames = selectedUsers.map((u: any) => u.nickname || u.name || u.username).join(', ')

  // Save position selection (from "岗位选择" tab)
  const realPostIds = checkedPostIds.value.map((id: string) => Number(id.replace('post_', '')))
  node.position = realPostIds[0] || null
  const selectedPost = allPositions.value.find((p: any) => realPostIds.includes(Number(p.id)))
  node.positionName = selectedPost?.name || ''
  
  orgTreeDialogVisible.value = false
  ElMessage.success('选择成功')
}

function selectNode(node: any) {
  selectedNodeId.value = node.id
  selectedConnectionId.value = null
}

function selectConnection(conn: any) {
  selectedConnectionId.value = conn.id
  selectedNodeId.value = null
}

function onCanvasClick() {
  connectingFrom.value = null
  selectedNodeId.value = null
  selectedConnectionId.value = null
}

function getNodeClass(node: any) {
  const map: any = {
    start: 'node-start',
    end: 'node-end',
    approval: 'node-approval',
    subprocess: 'node-subprocess',
    exclusive: 'node-gateway',
    parallel: 'node-gateway'
  }
  return map[node.type] || 'node-default'
}

function getNodeIcon(node: any) {
  const map: any = {
    start: '▶',
    end: '■',
    approval: '',
    subprocess: '',
    exclusive: '◇',
    parallel: ''
  }
  return map[node.type] || '?'
}

function getNodeTypeLabel(type: string) {
  const map: any = {
    start: '开始事件',
    end: '结束事件',
    approval: '审批任务',
    callActivity: '子流程',
    subprocess: '子过程',
    exclusive: '互斥网关',
    parallel: '并行网关'
  }
  return map[type] || type
}

function getNodeTypeName(type: string) {
  const map: any = {
    start: 'START',
    end: 'END',
    approval: 'APPROVAL',
    callActivity: 'CALL',
    subprocess: 'SUBPROCESS',
    exclusive: 'GATEWAY',
    parallel: 'GATEWAY'
  }
  return map[type] || type.toUpperCase()
}

function getNodeTagType(type: string) {
  const map: any = {
    start: 'success',
    end: 'danger',
    approval: 'primary',
    subprocess: 'info',
    exclusive: 'warning',
    parallel: 'warning'
  }
  return map[type] || 'info'
}

function getConnectionPath(conn: any) {
  const from = nodes.value.find(n => n.id === conn.from)
  const to = nodes.value.find(n => n.id === conn.to)
  if (!from || !to) return ''
  return `M ${from.x + 40} ${from.y} L ${to.x - 40} ${to.y}`
}

function getMidPoint(conn: any) {
  const from = nodes.value.find(n => n.id === conn.from)
  const to = nodes.value.find(n => n.id === conn.to)
  if (!from || !to) return { x: 0, y: 0 }
  return { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 }
}

function getConnectionActionPos(conn: any) {
  const mid = getMidPoint(conn)
  return `translate(${mid.x - 18}, ${mid.y - 18})`
}

// Drag from palette
function onPaletteDragStart(e: DragEvent, item: any) {
  if (e.dataTransfer) {
    e.dataTransfer.setData('nodeType', item.type)
    e.dataTransfer.effectAllowed = 'copy'
  }
}

function onCanvasDrop(e: DragEvent) {
  e.preventDefault()
  const type = e.dataTransfer?.getData('nodeType')
  if (!type || !canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = snapToGrid((e.clientX - rect.left) / zoom.value)
  const y = snapToGrid((e.clientY - rect.top) / zoom.value)

  saveHistory()
  const newNode = {
    id: 'node_' + (++nodeIdCounter),
    type,
    label: getNodeTypeLabel(type),
    description: '',
    x, y,
    candidateScope: 'company',
    candidateType: 'personnel',
    personnel: [],
    position: null,
    defaultTarget: 'manual',
    approvalMode: 'single',
    skipContinuous: false,
    eSignature: false,
    calledElement: ''
  }
  nodes.value.push(newNode)

  // Auto connect to nearest node
  if (nodes.value.length > 1) {
    const nearest = findNearestNode(newNode)
    if (nearest) {
      connections.value.push({
        id: 'conn_' + (++connIdCounter),
        from: nearest.id,
        to: newNode.id
      })
    }
  }
}

function findNearestNode(node: any) {
  let minDist = Infinity
  let nearest = null
  for (const n of nodes.value) {
    if (n.id === node.id) continue
    const dist = Math.sqrt(Math.pow(n.x - node.x, 2) + Math.pow(n.y - node.y, 2))
    if (dist < minDist) {
      minDist = dist
      nearest = n
    }
  }
  return nearest
}

const GRID_SIZE = 20

function snapToGrid(val: number): number {
  return Math.round(val / GRID_SIZE) * GRID_SIZE
}

// Node dragging
let dragNode: any = null
let dragOffset = { x: 0, y: 0 }

// Connection dragging
const connectingFrom = ref<string | null>(null)
const connMousePos = ref({ x: 0, y: 0 })

function onConnHandleMouseDown(_e: MouseEvent, node: any) {
  connectingFrom.value = node.id
  connMousePos.value = { x: node.x + 42, y: node.y }
  document.addEventListener('mousemove', onConnMouseMove)
  document.addEventListener('mouseup', onConnMouseUp)
}

function onConnMouseMove(e: MouseEvent) {
  if (!connectingFrom.value) return
  connMousePos.value = {
    x: (e.clientX - (canvasRef.value?.getBoundingClientRect().left || 0)) / zoom.value,
    y: (e.clientY - (canvasRef.value?.getBoundingClientRect().top || 0)) / zoom.value
  }
}

function onConnMouseUp(e: MouseEvent) {
  if (!connectingFrom.value) return
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    const mx = (e.clientX - rect.left) / zoom.value
    const my = (e.clientY - rect.top) / zoom.value
    const target = nodes.value.find((n: any) =>
      Math.abs(n.x - mx) < 35 && Math.abs(n.y - my) < 25 && n.id !== connectingFrom.value
    )
    if (target && !connections.value.some(c => c.from === connectingFrom.value && c.to === target.id)) {
      saveHistory()
      connections.value.push({
        id: 'conn_' + (++connIdCounter),
        from: connectingFrom.value,
        to: target.id
      })
    }
  }
  connectingFrom.value = null
  document.removeEventListener('mousemove', onConnMouseMove)
  document.removeEventListener('mouseup', onConnMouseUp)
}

function onNodeMouseDown(e: MouseEvent, node: any) {
  dragNode = node
  dragOffset.x = e.clientX - node.x * zoom.value
  dragOffset.y = e.clientY - node.y * zoom.value
  saveHistory()
}

function onMouseMove(e: MouseEvent) {
  if (dragNode && canvasRef.value) {
    dragNode.x = snapToGrid((e.clientX - dragOffset.x) / zoom.value)
    dragNode.y = snapToGrid((e.clientY - dragOffset.y) / zoom.value)
  }
}

function onMouseUp() {
  dragNode = null
}

// Add node after selected node
function addNodeAfter(node: any, type: string) {
  saveHistory()
  const newNode = {
    id: 'node_' + (++nodeIdCounter),
    type,
    label: getNodeTypeLabel(type),
    description: '',
    x: node.x + 150,
    y: node.y,
    candidateScope: 'company',
    candidateType: 'personnel',
    personnel: [],
    position: null,
    defaultTarget: 'manual',
    approvalMode: 'single',
    skipContinuous: false,
    eSignature: false
  }
  nodes.value = [...nodes.value, newNode]

  const outgoingConns = connections.value.filter(c => c.from === node.id)

  if (outgoingConns.length > 0) {
    const firstConn = outgoingConns[0]
    const targetId = firstConn.to
    const newConns = connections.value.filter(c => c.id !== firstConn.id)
    newConns.push(
      { id: 'conn_' + (++connIdCounter), from: node.id, to: newNode.id },
      { id: 'conn_' + (++connIdCounter), from: newNode.id, to: targetId }
    )
    connections.value = newConns
  } else {
    connections.value = [...connections.value, {
      id: 'conn_' + (++connIdCounter),
      from: node.id,
      to: newNode.id
    }]
  }

  selectedNodeId.value = newNode.id
  ElMessage.success(`已添加${getNodeTypeLabel(type)}`)
}

// Confirm delete node
function confirmDeleteNode(node: any) {
  if (node.id === 'start' || node.id === 'end') {
    ElMessage.warning('不能删除开始/结束节点')
    return
  }
  deleteSelectedNode()
}

// Connection actions
function deleteConnection(conn: any) {
  saveHistory()
  connections.value = connections.value.filter(c => c.id !== conn.id)
  selectedConnectionId.value = null
  ElMessage.success('已删除连线')
}

// History
function saveHistory() {
  const state = JSON.stringify({ nodes: nodes.value, connections: connections.value })
  history.value = history.value.slice(0, historyIndex.value + 1)
  history.value.push(state)
  historyIndex.value = history.value.length - 1
}

function undo() {
  if (historyIndex.value > 0) {
    historyIndex.value--
    const state = JSON.parse(history.value[historyIndex.value])
    nodes.value = state.nodes
    connections.value = state.connections
  }
}

function redo() {
  if (historyIndex.value < history.value.length - 1) {
    historyIndex.value++
    const state = JSON.parse(history.value[historyIndex.value])
    nodes.value = state.nodes
    connections.value = state.connections
  }
}

// Zoom
function zoomIn() { zoom.value = Math.min(zoom.value + 0.1, 2) }
function zoomOut() { zoom.value = Math.max(zoom.value - 0.1, 0.5) }
function zoomReset() { zoom.value = 1 }

// Delete
function deleteSelectedNode() {
  const node = selectedNode.value
  if (!node) return
  saveHistory()
  nodes.value = nodes.value.filter(n => n.id !== node.id)
  connections.value = connections.value.filter(c => c.from !== node.id && c.to !== node.id)
  selectedNodeId.value = null
  ElMessage.success('已删除')
}

function generateBpmnXml(): string {
  const lines: string[] = []
  lines.push('<?xml version="1.0" encoding="UTF-8"?>')
  lines.push('<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef">')
  lines.push('  <bpmn:process id="process" name="流程" isExecutable="true">')

  for (const node of nodes.value) {
    if (node.type === 'start') {
      lines.push(`    <bpmn:startEvent id="${node.id}" name="${node.label}" />`)
    } else if (node.type === 'end') {
      lines.push(`    <bpmn:endEvent id="${node.id}" name="${node.label}" />`)
    } else if (node.type === 'approval') {
      lines.push(`    <bpmn:userTask id="${node.id}" name="${node.label}" flowable:assignee="admin">`)
      if (node.personnel?.length > 0 || node.position) {
        lines.push('      <bpmn:extensionElements>')
        if (node.personnel?.length > 0) {
          lines.push(`        <flowable:candidateUsers>${node.personnel.join(',')}</flowable:candidateUsers>`)
        }
        if (node.position) {
          lines.push(`        <flowable:candidateGroups>group_${node.position}</flowable:candidateGroups>`)
        }
        lines.push('      </bpmn:extensionElements>')
      }
      lines.push('    </bpmn:userTask>')
    } else if (node.type === 'exclusive') {
      lines.push(`    <bpmn:exclusiveGateway id="${node.id}" name="${node.label}" />`)
    } else if (node.type === 'parallel') {
      lines.push(`    <bpmn:parallelGateway id="${node.id}" name="${node.label}" />`)
    } else if (node.type === 'callActivity') {
      lines.push(`    <bpmn:callActivity id="${node.id}" name="${node.label}" calledElement="${node.calledElement || 'subprocess'}" />`)
    }
  }

  for (const conn of connections.value) {
    let line = `    <bpmn:sequenceFlow id="${conn.id}" sourceRef="${conn.from}" targetRef="${conn.to}"`
    if (conn.conditionExpr) {
      line += `>
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${conn.conditionExpr}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>`
    } else {
      line += ' />'
    }
    lines.push(line)
  }

  lines.push('  </bpmn:process>')

  lines.push('  <bpmndi:BPMNDiagram id="BPMNDiagram_1">')
  lines.push('    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process">')
  for (const node of nodes.value) {
    const x = node.x - 40
    const y = node.y - 30
    lines.push(`      <bpmndi:BPMNShape id="shape_${node.id}" bpmnElement="${node.id}">`)
    lines.push(`        <dc:Bounds x="${x}" y="${y}" width="80" height="60" />`)
    lines.push('      </bpmndi:BPMNShape>')
  }
  for (const conn of connections.value) {
    const from = nodes.value.find((n: any) => n.id === conn.from)
    const to = nodes.value.find((n: any) => n.id === conn.to)
    if (from && to) {
      lines.push(`      <bpmndi:BPMNEdge id="edge_${conn.id}" bpmnElement="${conn.id}">`)
      lines.push(`        <di:waypoint x="${from.x + 40}" y="${from.y}" />`)
      lines.push(`        <di:waypoint x="${to.x - 40}" y="${to.y}" />`)
      lines.push('      </bpmndi:BPMNEdge>')
    }
  }
  lines.push('    </bpmndi:BPMNPlane>')
  lines.push('  </bpmndi:BPMNDiagram>')
  lines.push('</bpmn:definitions>')

  const xml = lines.join('\n')
  emit('update:modelValue', xml)
  return xml
}

// Save
function saveAndPublish() {
  generateBpmnXml()
  emit('publish')
}

function saveDraft() {
  const xml = generateBpmnXml()
  localStorage.setItem('designer_draft', xml)
  ElMessage.success('保存草稿成功')
}

// Keyboard
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Delete' || e.key === 'Backspace') {
    if (selectedConnection.value) {
      deleteConnection(selectedConnection.value)
    } else if (selectedNode.value && selectedNode.value.id !== 'start') {
      deleteSelectedNode()
    }
  }
}

onMounted(async () => {
  saveHistory()
  await loadOrgTree()
  await loadAllData()
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.workflow-designer {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  z-index: 10;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-level {
  font-size: 12px;
  color: #666;
  min-width: 40px;
  text-align: center;
}

.designer-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Left Palette */
.designer-palette {
  width: 100px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.palette-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  padding: 0 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.palette-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  user-select: none;
  border: 1px solid transparent;
}

.palette-item:hover {
  background: #f5f7fa;
  border-color: #e4e7ed;
}

.palette-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.palette-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  margin-bottom: 6px;
  font-size: 18px;
}

.node-palette-item .palette-icon {
  width: 40px;
  height: 40px;
  font-size: 20px;
}

.icon-start { background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%); color: #67c23a; }
.icon-end { background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%); color: #f56c6c; }
.icon-approval { background: linear-gradient(135deg, #ecf5ff 0%, #e6f0ff 100%); color: #409eff; }
.icon-gateway { background: linear-gradient(135deg, #fdf6ec 0%, #faecd8 100%); color: #e6a23c; }
.icon-subprocess, .icon-group, .icon-call { background: linear-gradient(135deg, #f4f4f5 0%, #e9e9eb 100%); color: #909399; }
.icon-drag, .icon-select, .icon-connect { background: linear-gradient(135deg, #f4f4f5 0%, #e9e9eb 100%); color: #606266; }

.palette-label {
  font-size: 11px;
  color: #606266;
  text-align: center;
  line-height: 1.2;
  font-weight: 500;
}

/* Center Canvas */
.designer-canvas {
  flex: 1;
  position: relative;
  overflow: auto;
  background-image: radial-gradient(circle, #d9d9d9 1px, transparent 1px);
  background-size: 20px 20px;
}

.canvas-content {
  position: relative;
  min-width: 100%;
  min-height: 100%;
}

.canvas-svg {
  position: absolute;
  top: 0;
  left: 0;
}

.connection-line {
  fill: none;
  stroke: #c0c4cc;
  stroke-width: 2;
  cursor: pointer;
  transition: all 0.2s;
}

.connection-line:hover {
  stroke: #909399;
  stroke-width: 3;
}

.connection-line.selected {
  stroke: #409eff;
  stroke-width: 3;
}

.conn-handle {
  fill: #409eff;
  stroke: #fff;
  stroke-width: 2;
  cursor: crosshair;
  opacity: 0;
  transition: opacity 0.2s;
}
.node-group:hover .conn-handle {
  opacity: 1;
}
.conn-handle:hover {
  fill: #66b1ff;
}

.temp-connection {
  stroke: #409eff;
  stroke-width: 2;
  stroke-dasharray: 5, 3;
  pointer-events: none;
}

.preview-canvas {
  background: #fafafa;
  border-radius: 8px;
  min-height: 400px;
  overflow: auto;
}
.preview-node {
  fill: #e8f4fd;
  stroke: #409eff;
  stroke-width: 1.5;
  cursor: default;
}
.preview-text {
  font-size: 13px;
  fill: #333;
  pointer-events: none;
}

.node-group {
  cursor: move;
}

.node-group.selected .node-rect {
  stroke: #409eff !important;
  stroke-width: 2 !important;
}

.node-rect {
  fill: #fff;
  stroke: #dcdfe6;
  stroke-width: 1;
  transition: all 0.2s;
}

.node-start .node-rect { fill: #f0f9eb; stroke: #67c23a; }
.node-end .node-rect { fill: #fef0f0; stroke: #f56c6c; }
.node-approval .node-rect { fill: #ecf5ff; stroke: #409eff; }
.node-gateway .node-rect { fill: #fdf6ec; stroke: #e6a23c; }

.node-icon-text {
  font-size: 18px;
  fill: #606266;
}

.node-label {
  font-size: 12px;
  fill: #303133;
}

.node-actions-menu {
  cursor: pointer;
}

.node-actions-menu .action-item {
  cursor: pointer;
  transition: filter 0.2s ease;
}

.node-actions-menu .action-item:hover .action-item-bg {
  filter: brightness(1.15) drop-shadow(0 4px 10px rgba(0,0,0,0.3));
  stroke-width: 2.5;
}

.node-actions-menu .action-item-bg {
  fill: #fff;
  stroke: #dcdfe6;
  stroke-width: 1;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
}

.node-actions-menu .action-item-bg.approval {
  fill: #409eff;
  stroke: #409eff;
}

.node-actions-menu .action-item-bg.gateway {
  fill: #e6a23c;
  stroke: #e6a23c;
}

.node-actions-menu .action-item-bg.condition {
  fill: #909399;
  stroke: #909399;
}

.node-actions-menu .action-item-bg.delete {
  fill: #f56c6c;
  stroke: #f56c6c;
}

.node-actions-menu .action-item-text {
  fill: #fff;
  font-size: 18px;
  font-weight: 600;
  pointer-events: none;
}

/* Right Properties Panel */
.designer-properties {
  width: 360px;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.properties-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
}

.properties-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.properties-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.properties-id {
  font-size: 11px;
  color: #909399;
  font-family: monospace;
}

.properties-content {
  padding: 16px;
  flex: 1;
}

.prop-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.prop-section:last-child {
  border-bottom: none;
}

.prop-section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.prop-section-header .el-icon {
  color: #409eff;
  font-size: 16px;
}

.prop-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.prop-field {
  margin-bottom: 16px;
}

.prop-field:last-child {
  margin-bottom: 0;
}

.prop-label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.prop-label-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.help-icon {
  color: #909399;
  cursor: help;
}

.required {
  color: #f56c6c;
  margin-right: 2px;
}

.selection-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.org-tree-dialog {
  max-height: 500px;
  overflow-y: auto;
}

.dialog-info-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.dialog-hint {
  font-size: 12px;
  color: #909399;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-icon-org {
  color: #409eff;
}

.tree-icon-user {
  color: #67c23a;
}

.tree-icon-post {
  color: #e6a23c;
}

.tree-label {
  font-size: 13px;
}

.tree-meta {
  font-size: 11px;
  color: #909399;
  margin-left: 4px;
}

/* Dialog Search Bar */
.dialog-search-bar {
  margin-bottom: 12px;
}

.dialog-search-bar :deep(.el-input__wrapper) {
  border-radius: 8px;
}

/* Search Results */
.search-results-container {
  max-height: 420px;
  display: flex;
  flex-direction: column;
}

.search-results-header {
  margin-bottom: 8px;
}

.search-results-count {
  font-size: 12px;
  color: #909399;
}

.search-results-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  min-height: 200px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: #f5f7fa;
}

.search-result-item.checked {
  background: #ecf5ff;
}

.result-item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.result-item-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.result-item-meta {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Dialog Selected Bar */
.dialog-selected-bar {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  min-height: 36px;
}

.dialog-selected-label {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
  line-height: 24px;
}

.dialog-selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* Dialog Tabs */
.dialog-tabs {
  margin-bottom: 8px;
}

.dialog-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.dialog-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.dialog-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  padding: 0 16px;
  height: 36px;
  line-height: 36px;
}
</style>
