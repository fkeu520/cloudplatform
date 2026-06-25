<script setup lang="ts">
/**
 * 装修配套 (park-space) - Phase 7 重构
 *
 * 来源 csyh: pai-park-space-ui-csyh-2.x/std/pages/kitType.vue
 *
 * 设计: 顶部筛选 + 列表 (配套名 + 设备数 + 状态) + 弹窗 (内嵌 equipmentList el-table inline edit)
 * 配套名称: 园区内唯一 (V36 check-name 校验, blur 触发)
 * 设备清单: csyh 风格 (设备名称/型号/数量三列, + / - 按钮), 提交时调用 /equipment/batchSave/{kitId}
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKitPage,
  getKitById,
  createKit,
  updateKit,
  deleteKit,
  checkKitName,
  type Kit,
} from '@/api/kit'
import { listByKit, batchSaveByKit } from '@/api/equipment'

// ============== 状态 ==============
const loading = ref(false)
const tableData = ref<Kit[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

// 弹窗
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()

const defaultForm = () => ({
  kitName: '',
  amount: 1,
  status: 1,
})
const form = reactive(defaultForm())

const formRules = {
  kitName: [
    { required: true, message: '请输入配套名称', trigger: 'blur' },
    { max: 64, message: '配套名称不超过 64 字符', trigger: 'blur' },
    {
      validator: async (_rule: any, value: string, callback: any) => {
        if (!value) return callback()
        try {
          const res: any = await checkKitName(value, editingId.value || undefined)
          if (res.code === 200 && res.data === false) {
            return callback(new Error(`已存在配套 "${value}"`))
          }
          return callback()
        } catch {
          return callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// 设备清单 (el-table inline edit)
interface EquipmentRow {
  id?: string
  equipmentName?: string
  model?: string
  amount?: number
  status?: number
}
const equipmentList = ref<EquipmentRow[]>([])

const tableRules = {
  equipmentName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  model: [{ max: 64, message: '型号不超过 64 字符', trigger: 'blur' }],
  amount: [{ required: true, type: 'number', min: 1, message: '数量至少为 1', trigger: 'blur' }],
}

// ============== 数据加载 ==============
async function loadData() {
  loading.value = true
  try {
    const res: any = await getKitPage({
      ...searchForm,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}

function handleSizeChange(v: number) {
  pageSize.value = v
  loadData()
}

function handlePageChange(v: number) {
  pageNum.value = v
  loadData()
}

// ============== 弹窗操作 ==============
function resetForm() {
  Object.assign(form, defaultForm())
  editingId.value = null
  equipmentList.value = []
}

async function handleAdd() {
  resetForm()
  dialogMode.value = 'add'
  dialogVisible.value = true
}

async function handleEdit(k: Kit) {
  resetForm()
  dialogMode.value = 'edit'
  editingId.value = k.id != null ? String(k.id) : null
  try {
    const res: any = await getKitById(k.id!)
    if (res.code === 200) Object.assign(form, defaultForm(), res.data)
    // 加载设备清单
    const eqRes: any = await listByKit(k.id!)
    if (eqRes.code === 200) {
      equipmentList.value = (eqRes.data || []).map((e: any) => ({
        id: e.id,
        equipmentName: e.equipmentName,
        model: e.model || '',
        amount: e.amount ?? 1,
        status: e.status ?? 1,
      }))
    }
  } catch (e) {
    ElMessage.error('获取配套详情失败')
  }
  dialogVisible.value = true
}

function addEquipmentRow() {
  equipmentList.value.push({
    equipmentName: '',
    model: '',
    amount: 1,
    status: 1,
  })
}

function removeEquipmentRow(index: number) {
  equipmentList.value.splice(index, 1)
}

async function handleSubmit() {
  // 校验主表单
  await formRef.value?.validate()
  // 校验设备清单
  for (let i = 0; i < equipmentList.value.length; i++) {
    const row = equipmentList.value[i]
    if (!row.equipmentName) {
      ElMessage.warning(`第 ${i + 1} 行设备名称不能为空`)
      return
    }
    if (!row.amount || row.amount < 1) {
      ElMessage.warning(`第 ${i + 1} 行数量必须 ≥ 1`)
      return
    }
  }
  submitting.value = true
  try {
    // 自动计算 amount = 设备清单条目数 (csyh 行为)
    const payload = { ...form, amount: equipmentList.value.length }
    let kitId: string | null = null
    if (dialogMode.value === 'edit' && editingId.value) {
      const res: any = await updateKit(editingId.value, payload)
      if (res.code !== 200) {
        ElMessage.error(res.msg || '更新失败')
        return
      }
      kitId = editingId.value
    } else {
      const res: any = await createKit(payload)
      if (res.code !== 200) {
        ElMessage.error(res.msg || '新增失败')
        return
      }
      // 后端可能返回 string (BaseEntity.@JsonFormat(STRING)) 或 number
      kitId = res.data != null ? String(res.data) : null
    }
    // 保存设备清单 - 关键: 必须等设备保存成功后再关 dialog
    if (kitId && equipmentList.value.length > 0) {
      // 移除 id 为 undefined 的字段, 后端只需要 id 有值的字段作为更新依据
      const eqPayload = equipmentList.value.map((e) => ({
        id: e.id,
        equipmentName: e.equipmentName,
        model: e.model || '',
        amount: e.amount ?? 1,
        status: e.status ?? 1,
      }))
      const eqRes: any = await batchSaveByKit(kitId, eqPayload)
      if (eqRes.code !== 200) {
        // 设备保存失败 -> 阻止关闭 dialog, 让用户看到错误
        ElMessage.error('设备清单保存失败: ' + (eqRes.msg || '未知错误') + ' (配套已保存)')
        submitting.value = false
        return
      }
    }
    // 全部成功后才提示成功 + 关闭 dialog + 刷新
    ElMessage.success(dialogMode.value === 'edit' ? '更新成功' : '新增成功')
    dialogVisible.value = false
    await loadData()
  } catch (e: any) {
    console.error('[kit handleSubmit] error', e)
    if (e?.msg) ElMessage.error(e.msg)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(k: Kit) {
  try {
    await ElMessageBox.confirm(
      `确认要删除配套 "${k.kitName}" 吗？\n（如配套已被房间引用将无法删除）`,
      '删除确认',
      { type: 'warning' }
    )
    const res: any = await deleteKit(k.id!)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

const dialogTitle = ref('新增配套')

onMounted(async () => {
  await loadData()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部筛选 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input
            v-model="searchForm.keyword"
            placeholder="配套名称"
            clearable
            style="width: 200px"
            @change="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleSearch"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="handleReset">重置</el-button>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增配套</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="kitName" label="配套名称" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="equipmentCount" label="设备总数" width="100" align="right">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.equipmentCount ?? scope.row.amount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" icon="el-icon-edit-outline" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" icon="el-icon-delete" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增配套' : '编辑配套'"
      width="900px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form
        :model="form"
        label-width="100px"
        :rules="formRules"
        ref="formRef"
        :validate-on-rule-change="false"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="配套名称" prop="kitName">
              <el-input v-model="form.kitName" placeholder="如 标准办公A" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量">
              <el-input-number
                v-model="form.amount"
                :min="0"
                style="width: 100%"
                placeholder="留空保存时自动计算为设备清单条目数"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 设备清单 (内嵌子表) -->
        <el-divider content-position="left">
          <span style="font-weight: 600">
            <i class="el-icon-goods"></i> 设备清单
          </span>
        </el-divider>
        <div class="equipment-actions">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="addEquipmentRow">
            添加设备
          </el-button>
          <span class="equipment-hint">
            共 {{ equipmentList.length }} 项设备, 数量合计
            {{ equipmentList.reduce((sum, r) => sum + (r.amount || 0), 0) }}
          </span>
        </div>

        <el-table
          v-if="equipmentList.length > 0"
          :data="equipmentList"
          border
          size="small"
          style="width: 100%; margin-top: 12px"
        >
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="设备名称" min-width="180">
            <template #default="{ row }">
              <el-form-item
                :prop="''"
                :rules="tableRules.equipmentName"
                style="margin: 0"
                :show-message="false"
              >
                <el-input v-model="row.equipmentName" placeholder="设备名称" size="small" maxlength="32" />
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="型号" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.model" placeholder="型号" size="small" maxlength="32" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="1" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ $index }">
              <el-button
                type="danger"
                size="small"
                icon="el-icon-delete"
                @click="removeEquipmentRow($index)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-equipment">
          <i class="el-icon-box"></i>
          <span>暂无设备, 请点击上方"添加设备"按钮</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.equipment-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.equipment-hint {
  color: rgba(0, 0, 0, 0.55);
  font-size: 13px;
}

.empty-equipment {
  margin: 16px 0;
  padding: 32px 16px;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
}

.empty-equipment i {
  margin-right: 8px;
  font-size: 18px;
}

:deep(.el-divider__text) {
  background: #fff;
}
</style>
