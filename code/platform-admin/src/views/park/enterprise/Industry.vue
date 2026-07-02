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
      <el-button type="primary" @click="openDialog('addTop', null)">+ 新增大类</el-button>
    </div>

    <div class="layout">
      <!-- 左侧树 -->
      <div class="left-panel">
        <div class="left-panel-header">
          <strong>📂 行业分类</strong>
          <el-button size="small" circle @click="reloadTree" title="刷新">🔄</el-button>
        </div>
        <div class="tree-panel">
          <div class="tree-node" :class="{ active: selectedNodeId === null }" @click="selectNode(null)">
            <span class="tree-toggle expanded" @click.stop="toggleRoot">▶</span>
            <span>🏭 全部分类</span>
            <span class="node-badge">{{ treeData.length }} 门类</span>
          </div>
          <div class="tree-children" v-show="rootExpanded">
            <div v-for="major in treeData" :key="major.id" class="tree-branch">
              <div class="tree-node" :class="{ active: selectedNodeId === major.id }" @click="selectNode(major.id)">
                <span class="tree-toggle" :class="{ expanded: major._expanded, empty: !major.children?.length }"
                  @click.stop="toggleBranch(major)">▶</span>
                <span>{{ major.icon }} {{ major.name }}</span>
                <span class="node-badge">{{ countEnterprises(major) }} 家</span>
              </div>
              <div class="tree-children" v-show="major._expanded" v-if="major.children?.length">
                <div v-for="sub in major.children" :key="sub.id" class="tree-branch">
                  <div class="tree-node sub-node" :class="{ active: selectedNodeId === sub.id }" @click="selectNode(sub.id)">
                    <span class="tree-toggle" :class="{ expanded: sub._expanded, empty: !sub.children?.length }"
                      @click.stop="toggleBranch(sub)">▶</span>
                    <span>{{ sub.icon }} {{ sub.name }}</span>
                    <span class="node-badge">{{ countEnterprises(sub) }} 家</span>
                  </div>
                  <div class="tree-children" v-show="sub._expanded" v-if="sub.children?.length">
                    <div v-for="leaf in sub.children" :key="leaf.id" class="tree-node leaf-node"
                      :class="{ active: selectedNodeId === leaf.id }" @click="selectNode(leaf.id)">
                      <span class="tree-toggle empty">▶</span>
                      <span>{{ leaf.icon }} {{ leaf.name }}</span>
                      <span class="node-badge">{{ leaf.enterpriseCount || 0 }} 家</span>
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
            <el-button v-if="currentNode && currentNode.level > 0 && currentNode.level < 4" size="small" @click="openDialog('edit', currentNode)">✏️ 编辑</el-button>
            <el-button v-if="currentNode && currentNode.level < 4" size="small" type="success" @click="openDialog('addSub', currentNode)">+ 新增子类</el-button>
            <el-button v-if="currentNode && currentNode.id !== 0" size="small" type="danger" @click="handleDelete">🗑️ 删除</el-button>
          </div>
        </div>

        <!-- 全部分类概览 -->
        <template v-if="!currentNode">
          <el-row :gutter="12" style="margin-bottom:16px">
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ treeData.length }}</div><div class="stat-lbl">行业大类</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ totalSubCount }}</div><div class="stat-lbl">子分类</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ totalEnterpriseCount }}</div><div class="stat-lbl">关联企业</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num" style="color:#34C759">●</div><div class="stat-lbl">系统运行中</div></el-card></el-col>
          </el-row>
          <div style="font-size:14px;font-weight:600;margin-bottom:12px">行业分类概览</div>
          <div class="overview-grid">
            <div v-for="m in treeData" :key="m.id" class="overview-card" @click="selectNode(m.id)">
              <span style="font-size:18px">{{ m.icon }}</span>
              <div>
                <div style="font-size:13px;font-weight:500">{{ m.name }}</div>
                <div style="font-size:11px;color:#909399">{{ countChildren(m) }} 子类 · {{ countEnterprises(m) }} 企业</div>
              </div>
            </div>
          </div>
        </template>

        <!-- 单个行业详情 -->
        <template v-else>
          <el-row :gutter="12" style="margin-bottom:16px">
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ directChildCount }}</div><div class="stat-lbl">直接子分类</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ countEnterprises(currentNode) }}</div><div class="stat-lbl">关联企业</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num">{{ currentNode.sort }}</div><div class="stat-lbl">排序</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never" class="stat-mini"><div class="stat-num" :style="{ color: currentNode.status === 1 ? '#34C759' : '#C0C4CC' }">●</div><div class="stat-lbl">{{ currentNode.status === 1 ? '启用' : '停用' }}</div></el-card></el-col>
          </el-row>

          <div style="font-size:14px;font-weight:600;margin-bottom:12px">基本信息</div>
          <el-descriptions :column="3" border size="small" style="margin-bottom:16px">
            <el-descriptions-item label="行业编码">{{ currentNode.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业名称">{{ currentNode.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上级分类">{{ currentNode.parent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="层级">{{ levelLabel(currentNode.level) }}</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="currentNode.status === 1 ? 'success' : 'info'" size="small">{{ currentNode.status === 1 ? '启用' : '停用' }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentNode.createTime || '2024-01-15 10:00:00' }}</el-descriptions-item>
            <el-descriptions-item v-if="currentNode.desc" label="描述" :span="3">{{ currentNode.desc }}</el-descriptions-item>
          </el-descriptions>

          <div style="font-size:14px;font-weight:600;margin-bottom:8px">子分类列表</div>
          <el-table :data="subList" v-loading="subLoading" border size="small" max-height="350" v-if="subList.length > 0">
            <el-table-column type="index" label="#" width="45" />
            <el-table-column prop="name" label="行业名称" min-width="140" />
            <el-table-column prop="code" label="编码" width="110" />
            <el-table-column label="层级" width="60">
              <template #default="scope"><span style="color:#909399;font-size:12px">{{ levelLabel(scope.row.level) }}</span></template>
            </el-table-column>
            <el-table-column prop="enterpriseCount" label="关联企业" width="90" />
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="75">
              <template #default="scope"><el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openDialog('edit', scope.row)">编辑</el-button>
                <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" @click="toggleSubStatus(scope.row)">
                  {{ scope.row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDeleteChild(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无子分类" :image-size="80" />
        </template>
      </div>
    </div>

    <!-- ═══ CRUD Dialog ═══ -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px" top="18vh" :close-on-click-modal="false" @close="resetForm">
      <el-form ref="formRef" :model="dialogForm" :rules="dialogRules" label-width="100px" size="default">
        <!-- Parent selector for sub-category dialogs -->
        <el-form-item v-if="dialogMode === 'addSub'" label="所属父类" prop="parentId">
          <el-tree-select
            v-model="dialogForm.parentId"
            :data="parentTreeOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级分类"
            filterable
            style="width:100%"
            node-key="id"
          />
        </el-form-item>
        <el-form-item v-else-if="dialogMode === 'editSub'" label="所属父类">
          <el-input :model-value="dialogForm.parentName" disabled />
        </el-form-item>

        <el-form-item label="行业名称" prop="name">
          <el-input v-model="dialogForm.name" maxlength="64" placeholder="如：信息技术" />
        </el-form-item>
        <el-form-item label="行业编码" prop="code">
          <el-input v-model="dialogForm.code" maxlength="20" placeholder="如：I-65" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="dialogForm.sort" :min="0" :max="999" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="dialogForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="dialogMode === 'addTop' || dialogMode === 'editTop'" label="描述">
          <el-input v-model="dialogForm.desc" type="textarea" :rows="3" placeholder="行业分类描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// ─── Tree Item Type ───
interface TreeItem {
  id: number
  icon: string
  name: string
  code: string
  parent: string
  parentId?: number
  sort: number
  status: number
  level: number
  enterpriseCount: number
  desc?: string
  createTime?: string
  children?: TreeItem[]
  _expanded?: boolean
}

// ─── Mock Industry Data (GB/T 4754-2017 based) ───
function createMockData(): TreeItem[] {
  let id = 1
  const nxt = () => id++
  const data: TreeItem[] = [
    {
      id: nxt(), icon: '🌾', name: '农、林、牧、渔业', code: 'A', parent: '全部分类', sort: 1, status: 1, level: 1,
      enterpriseCount: 38, desc: '农业、林业、畜牧业、渔业及相关服务业', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🌿', name: '农业', code: 'A-01', parent: '农、林、牧、渔业', sort: 1, status: 1, level: 2, enterpriseCount: 18, desc: '谷物、蔬菜、水果等种植业', children: [
          { id: nxt(), icon: '🌾', name: '谷物种植', code: 'A-01-01', parent: '农业', sort: 1, status: 1, level: 3, enterpriseCount: 8 },
          { id: nxt(), icon: '🥬', name: '蔬菜种植', code: 'A-01-02', parent: '农业', sort: 2, status: 1, level: 3, enterpriseCount: 6 },
          { id: nxt(), icon: '🍎', name: '水果种植', code: 'A-01-03', parent: '农业', sort: 3, status: 1, level: 3, enterpriseCount: 4 },
        ]},
        { id: nxt(), icon: '🌲', name: '林业', code: 'A-02', parent: '农、林、牧、渔业', sort: 2, status: 1, level: 2, enterpriseCount: 8, children: [
          { id: nxt(), icon: '🌳', name: '林木育种', code: 'A-02-01', parent: '林业', sort: 1, status: 1, level: 3, enterpriseCount: 3 },
          { id: nxt(), icon: '🪵', name: '木材采运', code: 'A-02-02', parent: '林业', sort: 2, status: 1, level: 3, enterpriseCount: 5 },
        ]},
        { id: nxt(), icon: '🐄', name: '畜牧业', code: 'A-03', parent: '农、林、牧、渔业', sort: 3, status: 1, level: 2, enterpriseCount: 7, children: [
          { id: nxt(), icon: '🐖', name: '牲畜饲养', code: 'A-03-01', parent: '畜牧业', sort: 1, status: 1, level: 3, enterpriseCount: 4 },
          { id: nxt(), icon: '🐔', name: '家禽饲养', code: 'A-03-02', parent: '畜牧业', sort: 2, status: 0, level: 3, enterpriseCount: 3 },
        ]},
        { id: nxt(), icon: '🐟', name: '渔业', code: 'A-04', parent: '农、林、牧、渔业', sort: 4, status: 0, level: 2, enterpriseCount: 5 },
      ],
    },
    {
      id: nxt(), icon: '⛏️', name: '采矿业', code: 'B', parent: '全部分类', sort: 2, status: 1, level: 1,
      enterpriseCount: 25, desc: '固体、液体及气体矿产的开采', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🪨', name: '煤炭开采和洗选', code: 'B-06', parent: '采矿业', sort: 1, status: 1, level: 2, enterpriseCount: 5 },
        { id: nxt(), icon: '🛢️', name: '石油和天然气开采', code: 'B-07', parent: '采矿业', sort: 2, status: 1, level: 2, enterpriseCount: 7, children: [
          { id: nxt(), icon: '⛽', name: '石油开采', code: 'B-07-01', parent: '石油和天然气开采', sort: 1, status: 1, level: 3, enterpriseCount: 4 },
          { id: nxt(), icon: '🔥', name: '天然气开采', code: 'B-07-02', parent: '石油和天然气开采', sort: 2, status: 1, level: 3, enterpriseCount: 3 },
        ]},
        { id: nxt(), icon: '⚒️', name: '黑色金属矿采选', code: 'B-08', parent: '采矿业', sort: 3, status: 1, level: 2, enterpriseCount: 6 },
        { id: nxt(), icon: '🔩', name: '有色金属矿采选', code: 'B-09', parent: '采矿业', sort: 4, status: 0, level: 2, enterpriseCount: 4 },
        { id: nxt(), icon: '🧱', name: '非金属矿采选', code: 'B-10', parent: '采矿业', sort: 5, status: 1, level: 2, enterpriseCount: 3 },
      ],
    },
    {
      id: nxt(), icon: '🏭', name: '制造业', code: 'C', parent: '全部分类', sort: 3, status: 1, level: 1,
      enterpriseCount: 245, desc: '产品制造、加工及相关活动', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🥩', name: '农副食品加工业', code: 'C-13', parent: '制造业', sort: 1, status: 1, level: 2, enterpriseCount: 28, children: [
          { id: nxt(), icon: '🌽', name: '谷物加工', code: 'C-13-01', parent: '农副食品加工业', sort: 1, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🥩', name: '肉类加工', code: 'C-13-02', parent: '农副食品加工业', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
          { id: nxt(), icon: '🥛', name: '乳制品加工', code: 'C-13-03', parent: '农副食品加工业', sort: 3, status: 0, level: 3, enterpriseCount: 6 },
        ]},
        { id: nxt(), icon: '🍺', name: '食品制造业', code: 'C-14', parent: '制造业', sort: 2, status: 1, level: 2, enterpriseCount: 35, children: [
          { id: nxt(), icon: '🍞', name: '焙烤食品', code: 'C-14-01', parent: '食品制造业', sort: 1, status: 1, level: 3, enterpriseCount: 15 },
          { id: nxt(), icon: '🍬', name: '糖果制造', code: 'C-14-02', parent: '食品制造业', sort: 2, status: 1, level: 3, enterpriseCount: 8 },
          { id: nxt(), icon: '🥤', name: '饮料制造', code: 'C-14-03', parent: '食品制造业', sort: 3, status: 1, level: 3, enterpriseCount: 12 },
        ]},
        { id: nxt(), icon: '🧵', name: '纺织业', code: 'C-17', parent: '制造业', sort: 3, status: 1, level: 2, enterpriseCount: 22, children: [
          { id: nxt(), icon: '🧶', name: '棉纺织', code: 'C-17-01', parent: '纺织业', sort: 1, status: 1, level: 3, enterpriseCount: 10 },
          { id: nxt(), icon: '👕', name: '服装加工', code: 'C-17-02', parent: '纺织业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
        ]},
        { id: nxt(), icon: '🧪', name: '化学原料和化学制品制造业', code: 'C-26', parent: '制造业', sort: 4, status: 1, level: 2, enterpriseCount: 45, children: [
          { id: nxt(), icon: '🧴', name: '日用化学品', code: 'C-26-01', parent: '化学原料和化学制品制造业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '💊', name: '涂料油墨', code: 'C-26-02', parent: '化学原料和化学制品制造业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🧫', name: '合成材料', code: 'C-26-03', parent: '化学原料和化学制品制造业', sort: 3, status: 1, level: 3, enterpriseCount: 15 },
        ]},
        { id: nxt(), icon: '🔩', name: '金属制品业', code: 'C-33', parent: '制造业', sort: 5, status: 1, level: 2, enterpriseCount: 38, children: [
          { id: nxt(), icon: '🔧', name: '结构性金属制品', code: 'C-33-01', parent: '金属制品业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '⚙️', name: '金属工具制造', code: 'C-33-02', parent: '金属制品业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🔗', name: '金属表面处理', code: 'C-33-03', parent: '金属制品业', sort: 3, status: 0, level: 3, enterpriseCount: 8 },
        ]},
        { id: nxt(), icon: '🤖', name: '专用设备制造业', code: 'C-35', parent: '制造业', sort: 6, status: 1, level: 2, enterpriseCount: 42, children: [
          { id: nxt(), icon: '🏗️', name: '建筑工程机械', code: 'C-35-01', parent: '专用设备制造业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '🌾', name: '农业机械', code: 'C-35-02', parent: '专用设备制造业', sort: 2, status: 1, level: 3, enterpriseCount: 14 },
          { id: nxt(), icon: '💉', name: '医疗设备制造', code: 'C-35-03', parent: '专用设备制造业', sort: 3, status: 1, level: 3, enterpriseCount: 10 },
        ]},
        { id: nxt(), icon: '🖥️', name: '计算机、通信和其他电子设备制造业', code: 'C-39', parent: '制造业', sort: 7, status: 1, level: 2, enterpriseCount: 35, children: [
          { id: nxt(), icon: '💻', name: '电子器件制造', code: 'C-39-01', parent: '计算机、通信和其他电子设备制造业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '📱', name: '通信设备制造', code: 'C-39-02', parent: '计算机、通信和其他电子设备制造业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '📺', name: '广播电视设备', code: 'C-39-03', parent: '计算机、通信和其他电子设备制造业', sort: 3, status: 0, level: 3, enterpriseCount: 5 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '⚡', name: '电力、热力、燃气及水生产和供应业', code: 'D', parent: '全部分类', sort: 4, status: 1, level: 1,
      enterpriseCount: 56, desc: '电力生产供应、热力、燃气及水的生产和供应', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🔌', name: '电力生产', code: 'D-44', parent: '电力、热力、燃气及水生产和供应业', sort: 1, status: 1, level: 2, enterpriseCount: 28, children: [
          { id: nxt(), icon: '☀️', name: '太阳能发电', code: 'D-44-01', parent: '电力生产', sort: 1, status: 1, level: 3, enterpriseCount: 14 },
          { id: nxt(), icon: '💨', name: '风力发电', code: 'D-44-02', parent: '电力生产', sort: 2, status: 1, level: 3, enterpriseCount: 8 },
          { id: nxt(), icon: '🔥', name: '火力发电', code: 'D-44-03', parent: '电力生产', sort: 3, status: 1, level: 3, enterpriseCount: 6 },
        ]},
        { id: nxt(), icon: '🔥', name: '燃气生产和供应', code: 'D-45', parent: '电力、热力、燃气及水生产和供应业', sort: 2, status: 1, level: 2, enterpriseCount: 14 },
        { id: nxt(), icon: '💧', name: '水的生产和供应', code: 'D-46', parent: '电力、热力、燃气及水生产和供应业', sort: 3, status: 1, level: 2, enterpriseCount: 14 },
      ],
    },
    {
      id: nxt(), icon: '🏗️', name: '建筑业', code: 'E', parent: '全部分类', sort: 5, status: 1, level: 1,
      enterpriseCount: 120, desc: '房屋建筑、土木工程、建筑安装及装饰装修', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏢', name: '房屋建筑业', code: 'E-47', parent: '建筑业', sort: 1, status: 1, level: 2, enterpriseCount: 48, children: [
          { id: nxt(), icon: '🏠', name: '住宅房屋建筑', code: 'E-47-01', parent: '房屋建筑业', sort: 1, status: 1, level: 3, enterpriseCount: 28 },
          { id: nxt(), icon: '🏬', name: '商业房屋建筑', code: 'E-47-02', parent: '房屋建筑业', sort: 2, status: 1, level: 3, enterpriseCount: 20 },
        ]},
        { id: nxt(), icon: '🛣️', name: '土木工程建筑业', code: 'E-48', parent: '建筑业', sort: 2, status: 1, level: 2, enterpriseCount: 36, children: [
          { id: nxt(), icon: '🚇', name: '市政道路工程', code: 'E-48-01', parent: '土木工程建筑业', sort: 1, status: 1, level: 3, enterpriseCount: 16 },
          { id: nxt(), icon: '🌉', name: '桥梁工程', code: 'E-48-02', parent: '土木工程建筑业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🏞️', name: '园林绿化', code: 'E-48-03', parent: '土木工程建筑业', sort: 3, status: 1, level: 3, enterpriseCount: 8 },
        ]},
        { id: nxt(), icon: '🔧', name: '建筑安装业', code: 'E-49', parent: '建筑业', sort: 3, status: 1, level: 2, enterpriseCount: 20 },
        { id: nxt(), icon: '🎨', name: '建筑装饰、装修业', code: 'E-50', parent: '建筑业', sort: 4, status: 1, level: 2, enterpriseCount: 16 },
      ],
    },
    {
      id: nxt(), icon: '🛒', name: '批发和零售业', code: 'F', parent: '全部分类', sort: 6, status: 1, level: 1,
      enterpriseCount: 185, desc: '商品批发、零售及相关服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '📦', name: '批发业', code: 'F-51', parent: '批发和零售业', sort: 1, status: 1, level: 2, enterpriseCount: 95, children: [
          { id: nxt(), icon: '🌾', name: '农畜产品批发', code: 'F-51-01', parent: '批发业', sort: 1, status: 1, level: 3, enterpriseCount: 25 },
          { id: nxt(), icon: '🧪', name: '化工产品批发', code: 'F-51-02', parent: '批发业', sort: 2, status: 1, level: 3, enterpriseCount: 30 },
          { id: nxt(), icon: '💻', name: '电子产品批发', code: 'F-51-03', parent: '批发业', sort: 3, status: 1, level: 3, enterpriseCount: 40 },
        ]},
        { id: nxt(), icon: '🏪', name: '零售业', code: 'F-52', parent: '批发和零售业', sort: 2, status: 1, level: 2, enterpriseCount: 90, children: [
          { id: nxt(), icon: '🏬', name: '综合零售', code: 'F-52-01', parent: '零售业', sort: 1, status: 1, level: 3, enterpriseCount: 35 },
          { id: nxt(), icon: '🛍️', name: '服装零售', code: 'F-52-02', parent: '零售业', sort: 2, status: 1, level: 3, enterpriseCount: 28 },
          { id: nxt(), icon: '📱', name: '电子零售', code: 'F-52-03', parent: '零售业', sort: 3, status: 1, level: 3, enterpriseCount: 27 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '🚚', name: '交通运输、仓储和邮政业', code: 'G', parent: '全部分类', sort: 7, status: 1, level: 1,
      enterpriseCount: 88, desc: '交通运输、仓储和邮政服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🚛', name: '道路运输业', code: 'G-54', parent: '交通运输、仓储和邮政业', sort: 1, status: 1, level: 2, enterpriseCount: 35, children: [
          { id: nxt(), icon: '🚌', name: '城市公交', code: 'G-54-01', parent: '道路运输业', sort: 1, status: 1, level: 3, enterpriseCount: 15 },
          { id: nxt(), icon: '🚛', name: '货物运输', code: 'G-54-02', parent: '道路运输业', sort: 2, status: 1, level: 3, enterpriseCount: 20 },
        ]},
        { id: nxt(), icon: '🚢', name: '水上运输业', code: 'G-55', parent: '交通运输、仓储和邮政业', sort: 2, status: 1, level: 2, enterpriseCount: 12 },
        { id: nxt(), icon: '✈️', name: '航空运输业', code: 'G-56', parent: '交通运输、仓储和邮政业', sort: 3, status: 1, level: 2, enterpriseCount: 8 },
        { id: nxt(), icon: '📦', name: '仓储业', code: 'G-59', parent: '交通运输、仓储和邮政业', sort: 4, status: 1, level: 2, enterpriseCount: 23, children: [
          { id: nxt(), icon: '🏚️', name: '普通仓储', code: 'G-59-01', parent: '仓储业', sort: 1, status: 1, level: 3, enterpriseCount: 14 },
          { id: nxt(), icon: '❄️', name: '冷链仓储', code: 'G-59-02', parent: '仓储业', sort: 2, status: 1, level: 3, enterpriseCount: 9 },
        ]},
        { id: nxt(), icon: '📮', name: '邮政业', code: 'G-60', parent: '交通运输、仓储和邮政业', sort: 5, status: 1, level: 2, enterpriseCount: 10 },
      ],
    },
    {
      id: nxt(), icon: '🏨', name: '住宿和餐饮业', code: 'H', parent: '全部分类', sort: 8, status: 1, level: 1,
      enterpriseCount: 62, desc: '住宿服务及餐饮服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏩', name: '住宿业', code: 'H-61', parent: '住宿和餐饮业', sort: 1, status: 1, level: 2, enterpriseCount: 28, children: [
          { id: nxt(), icon: '🏨', name: '酒店服务', code: 'H-61-01', parent: '住宿业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '🏠', name: '民宿服务', code: 'H-61-02', parent: '住宿业', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
        ]},
        { id: nxt(), icon: '🍽️', name: '餐饮业', code: 'H-62', parent: '住宿和餐饮业', sort: 2, status: 1, level: 2, enterpriseCount: 34, children: [
          { id: nxt(), icon: '🍜', name: '正餐服务', code: 'H-62-01', parent: '餐饮业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '🥡', name: '快餐服务', code: 'H-62-02', parent: '餐饮业', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
          { id: nxt(), icon: '☕', name: '饮料及冷饮', code: 'H-62-03', parent: '餐饮业', sort: 3, status: 0, level: 3, enterpriseCount: 6 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '💻', name: '信息传输、软件和信息技术服务业', code: 'I', parent: '全部分类', sort: 9, status: 1, level: 1,
      enterpriseCount: 156, desc: '信息传输、软件开发和信息技术服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '📡', name: '电信、广播电视和卫星传输', code: 'I-63', parent: '信息传输、软件和信息技术服务业', sort: 1, status: 1, level: 2, enterpriseCount: 18 },
        { id: nxt(), icon: '🌐', name: '互联网和相关服务', code: 'I-64', parent: '信息传输、软件和信息技术服务业', sort: 2, status: 1, level: 2, enterpriseCount: 45, children: [
          { id: nxt(), icon: '🌍', name: '互联网接入', code: 'I-64-01', parent: '互联网和相关服务', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '📱', name: '移动互联网服务', code: 'I-64-02', parent: '互联网和相关服务', sort: 2, status: 1, level: 3, enterpriseCount: 27 },
        ]},
        { id: nxt(), icon: '💻', name: '软件和信息技术服务业', code: 'I-65', parent: '信息传输、软件和信息技术服务业', sort: 3, status: 1, level: 2, enterpriseCount: 93, children: [
          { id: nxt(), icon: '🖥️', name: '软件开发', code: 'I-65-01', parent: '软件和信息技术服务业', sort: 1, status: 1, level: 3, enterpriseCount: 56 },
          { id: nxt(), icon: '💾', name: '系统集成', code: 'I-65-02', parent: '软件和信息技术服务业', sort: 2, status: 1, level: 3, enterpriseCount: 22 },
          { id: nxt(), icon: '🤖', name: '人工智能', code: 'I-65-03', parent: '软件和信息技术服务业', sort: 3, status: 0, level: 3, enterpriseCount: 15 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '💰', name: '金融业', code: 'J', parent: '全部分类', sort: 10, status: 1, level: 1,
      enterpriseCount: 65, desc: '银行、证券、保险及其他金融活动', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏦', name: '货币金融服务', code: 'J-66', parent: '金融业', sort: 1, status: 1, level: 2, enterpriseCount: 28, children: [
          { id: nxt(), icon: '🏛️', name: '商业银行', code: 'J-66-01', parent: '货币金融服务', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '💳', name: '金融科技', code: 'J-66-02', parent: '货币金融服务', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
        ]},
        { id: nxt(), icon: '📈', name: '资本市场服务', code: 'J-67', parent: '金融业', sort: 2, status: 1, level: 2, enterpriseCount: 18 },
        { id: nxt(), icon: '🛡️', name: '保险业', code: 'J-68', parent: '金融业', sort: 3, status: 1, level: 2, enterpriseCount: 19, children: [
          { id: nxt(), icon: '🏥', name: '人寿保险', code: 'J-68-01', parent: '保险业', sort: 1, status: 1, level: 3, enterpriseCount: 10 },
          { id: nxt(), icon: '🚗', name: '财产保险', code: 'J-68-02', parent: '保险业', sort: 2, status: 1, level: 3, enterpriseCount: 9 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '🏢', name: '房地产业', code: 'K', parent: '全部分类', sort: 11, status: 1, level: 1,
      enterpriseCount: 101, desc: '房地产开发经营、物业管理等', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏗️', name: '房地产开发经营', code: 'K-70', parent: '房地产业', sort: 1, status: 1, level: 2, enterpriseCount: 67, children: [
          { id: nxt(), icon: '🏢', name: '住宅开发', code: 'K-70-01', parent: '房地产开发经营', sort: 1, status: 1, level: 3, enterpriseCount: 38 },
          { id: nxt(), icon: '🏬', name: '商业地产开发', code: 'K-70-02', parent: '房地产开发经营', sort: 2, status: 1, level: 3, enterpriseCount: 29 },
        ]},
        { id: nxt(), icon: '🏘️', name: '物业管理', code: 'K-71', parent: '房地产业', sort: 2, status: 1, level: 2, enterpriseCount: 34, children: [
          { id: nxt(), icon: '🏠', name: '住宅物业管理', code: 'K-71-01', parent: '物业管理', sort: 1, status: 1, level: 3, enterpriseCount: 20 },
          { id: nxt(), icon: '🏢', name: '商业物业管理', code: 'K-71-02', parent: '物业管理', sort: 2, status: 1, level: 3, enterpriseCount: 14 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '📋', name: '租赁和商务服务业', code: 'L', parent: '全部分类', sort: 12, status: 1, level: 1,
      enterpriseCount: 78, desc: '机械设备租赁、企业管理服务等', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🚜', name: '租赁业', code: 'L-72', parent: '租赁和商务服务业', sort: 1, status: 1, level: 2, enterpriseCount: 25, children: [
          { id: nxt(), icon: '🏗️', name: '机械设备租赁', code: 'L-72-01', parent: '租赁业', sort: 1, status: 1, level: 3, enterpriseCount: 15 },
          { id: nxt(), icon: '🚗', name: '汽车租赁', code: 'L-72-02', parent: '租赁业', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
        ]},
        { id: nxt(), icon: '💼', name: '商务服务业', code: 'L-73', parent: '租赁和商务服务业', sort: 2, status: 1, level: 2, enterpriseCount: 53, children: [
          { id: nxt(), icon: '👥', name: '企业管理服务', code: 'L-73-01', parent: '商务服务业', sort: 1, status: 1, level: 3, enterpriseCount: 20 },
          { id: nxt(), icon: '⚖️', name: '法律服务', code: 'L-73-02', parent: '商务服务业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '📊', name: '咨询与调查', code: 'L-73-03', parent: '商务服务业', sort: 3, status: 1, level: 3, enterpriseCount: 21 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '🔬', name: '科学研究和技术服务业', code: 'M', parent: '全部分类', sort: 13, status: 1, level: 1,
      enterpriseCount: 92, desc: '科学研究、专业技术服务及技术推广', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🔭', name: '研究和试验发展', code: 'M-73', parent: '科学研究和技术服务业', sort: 1, status: 1, level: 2, enterpriseCount: 35, children: [
          { id: nxt(), icon: '🔬', name: '自然科学', code: 'M-73-01', parent: '研究和试验发展', sort: 1, status: 1, level: 3, enterpriseCount: 15 },
          { id: nxt(), icon: '⚗️', name: '工程和技术研究', code: 'M-73-02', parent: '研究和试验发展', sort: 2, status: 1, level: 3, enterpriseCount: 20 },
        ]},
        { id: nxt(), icon: '📐', name: '专业技术服务业', code: 'M-74', parent: '科学研究和技术服务业', sort: 2, status: 1, level: 2, enterpriseCount: 38, children: [
          { id: nxt(), icon: '🏗️', name: '工程技术服务', code: 'M-74-01', parent: '专业技术服务业', sort: 1, status: 1, level: 3, enterpriseCount: 18 },
          { id: nxt(), icon: '🧪', name: '质检技术服务', code: 'M-74-02', parent: '专业技术服务业', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🌍', name: '测绘服务', code: 'M-74-03', parent: '专业技术服务业', sort: 3, status: 0, level: 3, enterpriseCount: 8 },
        ]},
        { id: nxt(), icon: '📡', name: '科技推广和应用服务业', code: 'M-75', parent: '科学研究和技术服务业', sort: 3, status: 1, level: 2, enterpriseCount: 19 },
      ],
    },
    {
      id: nxt(), icon: '🌊', name: '水利、环境和公共设施管理业', code: 'N', parent: '全部分类', sort: 14, status: 1, level: 1,
      enterpriseCount: 42, desc: '水利管理、生态保护和环境治理、公共设施管理', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🌊', name: '水利管理业', code: 'N-76', parent: '水利、环境和公共设施管理业', sort: 1, status: 1, level: 2, enterpriseCount: 12 },
        { id: nxt(), icon: '🌿', name: '生态保护和环境治理业', code: 'N-77', parent: '水利、环境和公共设施管理业', sort: 2, status: 1, level: 2, enterpriseCount: 18, children: [
          { id: nxt(), icon: '🌲', name: '生态保护', code: 'N-77-01', parent: '生态保护和环境治理业', sort: 1, status: 1, level: 3, enterpriseCount: 8 },
          { id: nxt(), icon: '♻️', name: '环境治理', code: 'N-77-02', parent: '生态保护和环境治理业', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
        ]},
        { id: nxt(), icon: '🏞️', name: '公共设施管理业', code: 'N-78', parent: '水利、环境和公共设施管理业', sort: 3, status: 1, level: 2, enterpriseCount: 12 },
      ],
    },
    {
      id: nxt(), icon: '🔧', name: '居民服务、修理和其他服务业', code: 'O', parent: '全部分类', sort: 15, status: 1, level: 1,
      enterpriseCount: 53, desc: '居民日常生活服务及修理服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏠', name: '居民服务业', code: 'O-80', parent: '居民服务、修理和其他服务业', sort: 1, status: 1, level: 2, enterpriseCount: 28, children: [
          { id: nxt(), icon: '💇', name: '理发美容', code: 'O-80-01', parent: '居民服务业', sort: 1, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🛁', name: '洗浴服务', code: 'O-80-02', parent: '居民服务业', sort: 2, status: 1, level: 3, enterpriseCount: 8 },
          { id: nxt(), icon: '🧹', name: '家政服务', code: 'O-80-03', parent: '居民服务业', sort: 3, status: 1, level: 3, enterpriseCount: 8 },
        ]},
        { id: nxt(), icon: '🔧', name: '机动车、电子产品和日用产品修理业', code: 'O-81', parent: '居民服务、修理和其他服务业', sort: 2, status: 1, level: 2, enterpriseCount: 18 },
        { id: nxt(), icon: '🎭', name: '其他服务业', code: 'O-82', parent: '居民服务、修理和其他服务业', sort: 3, status: 0, level: 2, enterpriseCount: 7 },
      ],
    },
    {
      id: nxt(), icon: '🎓', name: '教育', code: 'P', parent: '全部分类', sort: 16, status: 1, level: 1,
      enterpriseCount: 45, desc: '各阶段教育及培训服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🧒', name: '学前教育', code: 'P-82', parent: '教育', sort: 1, status: 1, level: 2, enterpriseCount: 15, children: [
          { id: nxt(), icon: '🏫', name: '幼儿园教育', code: 'P-82-01', parent: '学前教育', sort: 1, status: 1, level: 3, enterpriseCount: 10 },
          { id: nxt(), icon: '🎨', name: '早期教育', code: 'P-82-02', parent: '学前教育', sort: 2, status: 1, level: 3, enterpriseCount: 5 },
        ]},
        { id: nxt(), icon: '📚', name: '高等教育', code: 'P-83', parent: '教育', sort: 2, status: 1, level: 2, enterpriseCount: 8 },
        { id: nxt(), icon: '📖', name: '技能培训、教育辅助', code: 'P-84', parent: '教育', sort: 3, status: 1, level: 2, enterpriseCount: 22, children: [
          { id: nxt(), icon: '💻', name: '职业技能培训', code: 'P-84-01', parent: '技能培训、教育辅助', sort: 1, status: 1, level: 3, enterpriseCount: 14 },
          { id: nxt(), icon: '🎯', name: '素质教育', code: 'P-84-02', parent: '技能培训、教育辅助', sort: 2, status: 0, level: 3, enterpriseCount: 8 },
        ]},
      ],
    },
    {
      id: nxt(), icon: '🏥', name: '卫生和社会工作', code: 'Q', parent: '全部分类', sort: 17, status: 1, level: 1,
      enterpriseCount: 58, desc: '医疗卫生服务及社会工作服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏥', name: '医院', code: 'Q-84', parent: '卫生和社会工作', sort: 1, status: 1, level: 2, enterpriseCount: 28, children: [
          { id: nxt(), icon: '🏨', name: '综合医院', code: 'Q-84-01', parent: '医院', sort: 1, status: 1, level: 3, enterpriseCount: 14 },
          { id: nxt(), icon: '🦷', name: '专科医院', code: 'Q-84-02', parent: '医院', sort: 2, status: 1, level: 3, enterpriseCount: 10 },
          { id: nxt(), icon: '💊', name: '中医医院', code: 'Q-84-03', parent: '医院', sort: 3, status: 1, level: 3, enterpriseCount: 4 },
        ]},
        { id: nxt(), icon: '💉', name: '基层医疗卫生服务', code: 'Q-85', parent: '卫生和社会工作', sort: 2, status: 1, level: 2, enterpriseCount: 18, children: [
          { id: nxt(), icon: '🏠', name: '社区卫生服务中心', code: 'Q-85-01', parent: '基层医疗卫生服务', sort: 1, status: 1, level: 3, enterpriseCount: 12 },
          { id: nxt(), icon: '🩺', name: '诊所服务', code: 'Q-85-02', parent: '基层医疗卫生服务', sort: 2, status: 1, level: 3, enterpriseCount: 6 },
        ]},
        { id: nxt(), icon: '🤝', name: '社会工作', code: 'Q-86', parent: '卫生和社会工作', sort: 3, status: 1, level: 2, enterpriseCount: 12 },
      ],
    },
    {
      id: nxt(), icon: '🎬', name: '文化、体育和娱乐业', code: 'R', parent: '全部分类', sort: 18, status: 1, level: 1,
      enterpriseCount: 48, desc: '文化传媒、体育活动和娱乐服务', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '📰', name: '新闻和出版业', code: 'R-86', parent: '文化、体育和娱乐业', sort: 1, status: 1, level: 2, enterpriseCount: 10 },
        { id: nxt(), icon: '📺', name: '广播、电视、电影和录音制作业', code: 'R-87', parent: '文化、体育和娱乐业', sort: 2, status: 1, level: 2, enterpriseCount: 15, children: [
          { id: nxt(), icon: '🎥', name: '影视制作', code: 'R-87-01', parent: '广播、电视、电影和录音制作业', sort: 1, status: 1, level: 3, enterpriseCount: 8 },
          { id: nxt(), icon: '🎬', name: '广告制作', code: 'R-87-02', parent: '广播、电视、电影和录音制作业', sort: 2, status: 1, level: 3, enterpriseCount: 7 },
        ]},
        { id: nxt(), icon: '🎨', name: '文化艺术业', code: 'R-88', parent: '文化、体育和娱乐业', sort: 3, status: 1, level: 2, enterpriseCount: 12, children: [
          { id: nxt(), icon: '🎭', name: '表演艺术', code: 'R-88-01', parent: '文化艺术业', sort: 1, status: 1, level: 3, enterpriseCount: 6 },
          { id: nxt(), icon: '🏛️', name: '博物馆', code: 'R-88-02', parent: '文化艺术业', sort: 2, status: 1, level: 3, enterpriseCount: 6 },
        ]},
        { id: nxt(), icon: '🏀', name: '体育', code: 'R-89', parent: '文化、体育和娱乐业', sort: 4, status: 1, level: 2, enterpriseCount: 6 },
        { id: nxt(), icon: '🎮', name: '娱乐业', code: 'R-90', parent: '文化、体育和娱乐业', sort: 5, status: 1, level: 2, enterpriseCount: 5 },
      ],
    },
    {
      id: nxt(), icon: '🏛️', name: '公共管理、社会保障和社会组织', code: 'S', parent: '全部分类', sort: 19, status: 1, level: 1,
      enterpriseCount: 35, desc: '国家机构、社会保障及社会组织', createTime: '2024-01-15 10:00:00',
      children: [
        { id: nxt(), icon: '🏛️', name: '国家机构', code: 'S-91', parent: '公共管理、社会保障和社会组织', sort: 1, status: 1, level: 2, enterpriseCount: 18, children: [
          { id: nxt(), icon: '⚖️', name: '司法机构', code: 'S-91-01', parent: '国家机构', sort: 1, status: 1, level: 3, enterpriseCount: 6 },
          { id: nxt(), icon: '📋', name: '行政机构', code: 'S-91-02', parent: '国家机构', sort: 2, status: 1, level: 3, enterpriseCount: 12 },
        ]},
        { id: nxt(), icon: '🤝', name: '社会保障', code: 'S-92', parent: '公共管理、社会保障和社会组织', sort: 2, status: 1, level: 2, enterpriseCount: 10 },
        { id: nxt(), icon: '👥', name: '社会组织', code: 'S-93', parent: '公共管理、社会保障和社会组织', sort: 3, status: 1, level: 2, enterpriseCount: 7 },
      ],
    },
  ]
  return data
}

// ─── State ───
const rootExpanded = ref(true)
const selectedNodeId = ref<number | null>(null)
const treeData = ref<TreeItem[]>([])
const subLoading = ref(false)
const saving = ref(false)

// ─── Dialog State ───
type DialogMode = 'addTop' | 'editTop' | 'addSub' | 'editSub'
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('addTop')
const dialogTitle = ref('')
const dialogForm = reactive({
  id: 0,
  parentId: null as number | null,
  parentName: '',
  name: '',
  code: '',
  sort: 0,
  status: 1,
  desc: '',
})
const formRef = ref<FormInstance | null>(null)

const dialogRules: FormRules = {
  name: [
    { required: true, message: '请输入行业名称', trigger: 'blur' },
    { min: 1, max: 64, message: '名称长度在 1-64 个字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入行业编码', trigger: 'blur' },
    { min: 1, max: 20, message: '编码长度在 1-20 个字符', trigger: 'blur' },
  ],
  parentId: [
    { required: true, message: '请选择所属父类', trigger: 'change' },
  ],
}

// ─── Computed ───
const currentNode = computed(() => {
  if (selectedNodeId.value === null) return null
  return findNode(treeData.value, selectedNodeId.value)
})

const directChildCount = computed(() => {
  const node = currentNode.value
  if (!node) return 0
  return node.children?.length || 0
})

const subList = computed(() => {
  const node = currentNode.value
  if (!node) return []
  return (node.children || []).map(c => ({
    ...c,
    enterpriseCount: countEnterprises(c),
  }))
})

const totalSubCount = computed(() => {
  let count = 0
  for (const m of treeData.value) {
    count += m.children?.length || 0
    if (m.children) {
      for (const s of m.children) {
        count += s.children?.length || 0
      }
    }
  }
  return count
})

const totalEnterpriseCount = computed(() => {
  let total = 0
  for (const m of treeData.value) {
    total += countEnterprises(m)
  }
  return total
})

/** Options for the parent tree-select in addSub dialog: only items that can have children */
const parentTreeOptions = computed(() => {
  return treeData.value.map(m => ({
    id: m.id,
    name: `${m.icon} ${m.name}`,
    children: (m.children || []).filter(c => c.level < 3).map(c => ({
      id: c.id,
      name: `${c.icon} ${c.name}`,
      children: [],
    })),
  }))
})

// ─── Lifecycle ───
onMounted(() => {
  treeData.value = createMockData()
})

// ─── Tree Helpers ───
function reloadTree() {
  const snapshot = deepClone(treeData.value)
  treeData.value = snapshot
  ElMessage.success('已刷新')
}

function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj))
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

function findNodeAndParent(items: TreeItem[], id: number): { node: TreeItem | null; parent: TreeItem | null } {
  for (const item of items) {
    if (item.id === id) return { node: item, parent: null }
    if (item.children) {
      for (const child of item.children) {
        if (child.id === id) return { node: child, parent: item }
        if (child.children) {
          for (const grand of child.children) {
            if (grand.id === id) return { node: grand, parent: child }
          }
        }
      }
    }
  }
  return { node: null, parent: null }
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

function countChildren(node: TreeItem): number {
  let count = node.children?.length || 0
  if (node.children) {
    for (const c of node.children) {
      count += c.children?.length || 0
    }
  }
  return count
}

function levelLabel(level: number): string {
  switch (level) {
    case 1: return '门类'
    case 2: return '大类'
    case 3: return '中类'
    default: return `L${level}`
  }
}

// ─── Selection & Tree Interaction ───
function selectNode(id: number | null) {
  selectedNodeId.value = id
}

function toggleRoot() {
  rootExpanded.value = !rootExpanded.value
}

function toggleBranch(item: TreeItem) {
  item._expanded = !item._expanded
}

// ─── Dialog Management ───
let nextId = 1000

function openDialog(mode: 'addTop' | 'addSub' | 'edit', node: TreeItem | null) {
  formRef.value?.clearValidate()

  if (mode === 'addTop') {
    dialogMode.value = 'addTop'
    dialogTitle.value = '新增大类'
    Object.assign(dialogForm, { id: 0, parentId: null, parentName: '', name: '', code: '', sort: 0, status: 1, desc: '' })
  } else if (mode === 'addSub') {
    dialogMode.value = 'addSub'
    const parent = node || currentNode.value
    dialogTitle.value = `新增子分类`
    Object.assign(dialogForm, {
      id: 0,
      parentId: parent?.id || null,
      parentName: parent?.name || '',
      name: '', code: '', sort: 0, status: 1, desc: '',
    })
  } else if (mode === 'edit') {
    const target = node || currentNode.value
    if (!target) return
    if (target.level === 1) {
      dialogMode.value = 'editTop'
      dialogTitle.value = `编辑门类 - ${target.name}`
    } else {
      dialogMode.value = 'editSub'
      dialogTitle.value = `编辑子分类 - ${target.name}`
    }
    Object.assign(dialogForm, {
      id: target.id,
      parentId: null,
      parentName: target.parent || '',
      name: target.name,
      code: target.code,
      sort: target.sort,
      status: target.status,
      desc: target.desc || '',
    })
  }
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
}

async function confirmSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  // Simulate async API call
  await new Promise(resolve => setTimeout(resolve, 400))

  if (dialogMode.value === 'addTop') {
    // Add new top-level category
    const newItem: TreeItem = {
      id: nextId++,
      icon: '📁',
      name: dialogForm.name,
      code: dialogForm.code,
      parent: '全部分类',
      sort: dialogForm.sort,
      status: dialogForm.status,
      level: 1,
      enterpriseCount: 0,
      desc: dialogForm.desc,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
      children: [],
    }
    treeData.value.push(newItem)
    ElMessage.success(`门类「${dialogForm.name}」已添加 ✓`)
  } else if (dialogMode.value === 'addSub') {
    // Add sub-category under selected parent
    const parentId = dialogForm.parentId
    if (parentId === null) {
      ElMessage.error('请选择所属父类')
      saving.value = false
      return
    }
    const parent = findNode(treeData.value, parentId)
    if (!parent) {
      ElMessage.error('父类不存在')
      saving.value = false
      return
    }
    const childLevel = parent.level + 1
    const newItem: TreeItem = {
      id: nextId++,
      icon: childLevel === 2 ? '📂' : '📄',
      name: dialogForm.name,
      code: dialogForm.code,
      parent: parent.name,
      sort: dialogForm.sort,
      status: dialogForm.status,
      level: childLevel,
      enterpriseCount: 0,
      desc: dialogForm.desc,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
      children: childLevel < 3 ? [] : undefined,
    }
    if (!parent.children) parent.children = []
    parent.children.push(newItem)
    parent._expanded = true
    ElMessage.success(`子分类「${dialogForm.name}」已添加 ✓`)
  } else if (dialogMode.value === 'editTop' || dialogMode.value === 'editSub') {
    // Edit existing item
    const editId = dialogForm.id
    const { node } = findNodeAndParent(treeData.value, editId)
    if (node) {
      node.name = dialogForm.name
      node.code = dialogForm.code
      node.sort = dialogForm.sort
      node.status = dialogForm.status
      if ('desc' in dialogForm && dialogForm.desc !== undefined) {
        node.desc = dialogForm.desc
      }
      ElMessage.success(`「${dialogForm.name}」已更新 ✓`)
    } else {
      ElMessage.error('未找到要编辑的分类')
    }
  }

  saving.value = false
  dialogVisible.value = false
}

function handleDelete() {
  const node = currentNode.value
  if (!node) return
  const count = countEnterprises(node)

  let msg = `确认删除「${node.name}」？`
  if (count > 0) {
    msg += `\n该分类下共有 ${count} 家关联企业，删除后企业将失去此分类。`
  }
  if (node.children?.length) {
    msg += `\n该分类包含 ${node.children.length} 个子分类，将一并删除。`
  }

  ElMessageBox.confirm(msg, '确认删除', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  }).then(() => {
    const { node: found, parent } = findNodeAndParent(treeData.value, node.id)
    if (!found) {
      ElMessage.error('未找到要删除的分类')
      return
    }
    if (parent && parent.children) {
      const idx = parent.children.findIndex(c => c.id === node.id)
      if (idx !== -1) parent.children.splice(idx, 1)
    } else {
      // Top-level item
      const idx = treeData.value.findIndex(c => c.id === node.id)
      if (idx !== -1) treeData.value.splice(idx, 1)
    }
    selectedNodeId.value = null
    ElMessage.success(`「${node.name}」已删除`)
  }).catch(() => {})
}

function handleDeleteChild(row: TreeItem) {
  const node = currentNode.value
  if (!node || !node.children) return

  ElMessageBox.confirm(`确认删除子分类「${row.name}」？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  }).then(() => {
    const idx = node.children!.findIndex(c => c.id === row.id)
    if (idx !== -1) {
      node.children!.splice(idx, 1)
      ElMessage.success(`「${row.name}」已删除`)
    }
  }).catch(() => {})
}

function toggleSubStatus(row: TreeItem) {
  row.status = row.status === 1 ? 0 : 1
  ElMessage.success(row.status === 1 ? '已启用' : '已停用')
}
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
.node-badge { margin-left: auto; font-size: 11px; color: #909399; background: #f0f2f5; padding: 1px 6px; border-radius: 4px; white-space: nowrap; }
.tree-toggle { width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center; font-size: 10px; color: #C0C4CC; cursor: pointer; flex-shrink: 0; transition: transform .2s; }
.tree-toggle.expanded { transform: rotate(90deg); }
.tree-toggle.empty { visibility: hidden; }

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