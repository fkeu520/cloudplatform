<template>
  <div class="page-container">
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item>关注标签</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px">
      <div>
        <h2 style="font-size:20px;font-weight:700;margin:0">关注标签</h2>
        <p style="font-size:13px;color:#909399;margin:4px 0 0">管理企业关注标签分类体系，支持按标签分类快速筛选和关注企业动态</p>
      </div>
      <div style="display:flex;gap:8px">
        <el-button @click="openCategoryDialog">+ 新增分类</el-button>
        <el-button type="primary" @click="openTagDialog">+ 新增标签</el-button>
      </div>
    </div>

    <!-- 分类 Tabs -->
    <div class="cat-tabs">
      <div class="cat-tab" :class="{ active: activeCategory === 'all' }" @click="activeCategory = 'all'">
        全部 <span style="opacity:0.7">({{ allTagsCount }})</span>
      </div>
      <div v-for="cat in categories" :key="cat.id" class="cat-tab"
        :class="{ active: activeCategory === String(cat.id) }"
        @click="activeCategory = String(cat.id)">
        {{ cat.icon }} {{ cat.name }} <span style="opacity:0.7">({{ catItemsCount(cat) }})</span>
      </div>
    </div>

    <!-- 标签 Grid -->
    <div v-if="filteredCategories.length > 0">
      <div v-for="cat in filteredCategories" :key="cat.id" class="cat-card">
        <div class="cat-card-header">
          <div>
            <span class="cat-card-name">{{ cat.icon }} {{ cat.name }}</span>
            <span class="cat-card-count">{{ catItemsCount(cat) }} 个标签</span>
          </div>
          <div style="display:flex;gap:6px">
            <el-button size="small" @click="editCategory(cat)">✏️ 编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCategory(cat)">🗑️ 删除</el-button>
            <el-button size="small" @click="openTagDialogFor(cat)">+ 标签</el-button>
          </div>
        </div>
        <div class="cat-card-body">
          <div class="tag-group">
            <div v-for="tag in getCatItems(cat)" :key="tag.id" class="tag-item">
              <span>{{ tag.name }}</span>
              <button class="tag-remove" @click="deleteTag(tag)" title="移除">×</button>
            </div>
            <div v-if="getCatItems(cat).length === 0" class="empty-tags">暂无标签</div>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无关注分类" />

    <!-- 分类弹窗 -->
    <el-dialog v-model="catDialogVisible" :title="catDialogTitle" width="450px" top="25vh">
      <el-form :model="catForm" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="catForm.name" maxlength="32" placeholder="如：科技创新" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="catForm.icon" maxlength="8" placeholder="如：💡" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sort" :min="0" :max="999" controls-position="right" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCategory">{{ isEditCat ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>

    <!-- 标签弹窗 -->
    <el-dialog v-model="tagDialogVisible" :title="tagDialogTitle" width="450px" top="25vh">
      <el-form :model="tagForm" label-width="100px">
        <el-form-item label="所属分类">
          <el-select v-model="tagForm.categoryId" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagForm.name" maxlength="32" placeholder="如：高新技术企业" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="tagForm.sort" :min="0" :max="999" controls-position="right" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTag">{{ isEditTag ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFocusPage, createFocus, updateFocus, deleteFocus,
  getFocusItemByFocusId, createFocusItem, updateFocusItem, deleteFocusItem,
  type Focus, type FocusItem
} from '@/api/enterprise'

interface Category extends Focus {
  icon?: string
  sort?: number
  items: FocusItem[]
}

// ─── State ───
const activeCategory = ref('all')
const categories = ref<Category[]>([])
const loading = ref(false)

// ─── Category Dialog ───
const catDialogVisible = ref(false)
const catDialogTitle = ref('新增分类')
const isEditCat = ref(false)
const editingCatId = ref<number | null>(null)
const catForm = reactive({ name: '', icon: '📌', sort: 0 })

// ─── Tag Dialog ───
const tagDialogVisible = ref(false)
const tagDialogTitle = ref('新增标签')
const isEditTag = ref(false)
const editingTagId = ref<string | null>(null)
const tagForm = reactive({ categoryId: undefined as number | undefined, name: '', sort: 0 })

// ─── Computed ───
const allTagsCount = computed(() => {
  let count = 0
  for (const cat of categories.value) {
    count += cat.items.length
  }
  return count
})

const filteredCategories = computed(() => {
  if (activeCategory.value === 'all') return categories.value
  return categories.value.filter(c => String(c.id) === activeCategory.value)
})

function catItemsCount(cat: Category) {
  return cat.items.length
}

function getCatItems(cat: Category) {
  return cat.items || []
}

// ─── Lifecycle ───
onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res: any = await getFocusPage({ current: 1, size: 50 })
    if (res.code === 200) {
      const records: Focus[] = res.data?.records || []
      const enriched: Category[] = []
      for (const f of records) {
        try {
          const r: any = await getFocusItemByFocusId(f.id!)
          enriched.push({ ...f, items: r.data || [], icon: getIconFor(f.id), sort: f.sorting || 0 })
        } catch {
          enriched.push({ ...f, items: [], icon: getIconFor(f.id), sort: f.sorting || 0 })
        }
      }
      categories.value = enriched
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function getIconFor(id?: number): string {
  const icons: Record<number, string> = {
    1: '💡', 2: '🏭', 3: '🚀', 4: '👥', 5: '🌍', 6: '📋'
  }
  return icons[id || 0] || '📌'
}

// ─── Category CRUD ───
function openCategoryDialog() {
  isEditCat.value = false
  editingCatId.value = null
  catDialogTitle.value = '新增分类'
  Object.assign(catForm, { name: '', icon: '📌', sort: 0 })
  catDialogVisible.value = true
}

function editCategory(cat: Category) {
  isEditCat.value = true
  editingCatId.value = Number(cat.id)
  catDialogTitle.value = '编辑分类'
  Object.assign(catForm, { name: cat.name || '', icon: cat.icon || '📌', sort: cat.sort || 0 })
  catDialogVisible.value = true
}

async function confirmCategory() {
  if (!catForm.name) { ElMessage.warning('请输入分类名称'); return }
  try {
    if (isEditCat.value && editingCatId.value) {
      await updateFocus(String(editingCatId.value), { name: catForm.name, sorting: catForm.sort })
      ElMessage.success('分类已更新')
    } else {
      await createFocus({ name: catForm.name, sorting: catForm.sort, status: 1 })
      ElMessage.success('分类已添加')
    }
    catDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function deleteCategory(cat: Category) {
  ElMessageBox.confirm(`确认删除分类「${cat.name}」及其所有标签？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteFocus(cat.id!)
        ElMessage.success('已删除')
        loadData()
      } catch { /* ignore */ }
    })
    .catch(() => {})
}

// ─── Tag CRUD ───
function openTagDialog() {
  isEditTag.value = false
  editingTagId.value = null
  tagDialogTitle.value = '新增标签'
  Object.assign(tagForm, { categoryId: categories.value[0]?.id, name: '', sort: 0 })
  tagDialogVisible.value = true
}

function openTagDialogFor(cat: Category) {
  isEditTag.value = false
  editingTagId.value = null
  tagDialogTitle.value = `新增标签 - ${cat.name}`
  Object.assign(tagForm, { categoryId: Number(cat.id), name: '', sort: 0 })
  tagDialogVisible.value = true
}

async function confirmTag() {
  if (!tagForm.name) { ElMessage.warning('请输入标签名称'); return }
  if (!tagForm.categoryId) { ElMessage.warning('请选择所属分类'); return }
  try {
    if (isEditTag.value && editingTagId.value) {
      await updateFocusItem(editingTagId.value, { name: tagForm.name, sorting: tagForm.sort })
      ElMessage.success('标签已更新')
    } else {
      await createFocusItem({ focusId: tagForm.categoryId, name: tagForm.name, sorting: tagForm.sort, status: 1 })
      ElMessage.success('标签已添加')
    }
    tagDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function deleteTag(tag: FocusItem) {
  ElMessageBox.confirm(`确认移除标签「${tag.name}」？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteFocusItem(tag.id!)
        ElMessage.success('已移除')
        loadData()
      } catch { /* ignore */ }
    })
    .catch(() => {})
}
</script>

<style scoped>
.page-container { padding: 16px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

.cat-tabs { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }
.cat-tab {
  padding: 6px 16px; border-radius: 20px; font-size: 13px; cursor: pointer;
  border: 1.5px solid #e4e7ed; background: #fff; color: #909399;
  transition: all .2s; font-weight: 500;
}
.cat-tab:hover { background: #ecf5ff; border-color: #409EFF; color: #409EFF; }
.cat-tab.active { background: #409EFF; color: #fff; border-color: #409EFF; }

.cat-card { border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; margin-bottom: 12px; }
.cat-card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: #f5f7fa; border-bottom: 1px solid #e4e7ed;
}
.cat-card-name { font-size: 14px; font-weight: 600; }
.cat-card-count { font-size: 12px; color: #909399; margin-left: 8px; }
.cat-card-body { padding: 12px 16px; }

.tag-group { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-item {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px 4px 14px; border-radius: 16px; font-size: 12px;
  background: #ecf5ff; color: #409EFF; border: 1px solid transparent; transition: all .2s;
}
.tag-item:hover { border-color: #409EFF; }
.tag-remove {
  width: 16px; height: 16px; border-radius: 50%; border: none;
  background: rgba(0,0,0,0.1); cursor: pointer; font-size: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #409EFF; flex-shrink: 0; padding: 0; line-height: 1;
}
.tag-remove:hover { background: #F56C6C; color: #fff; }
.empty-tags { font-size: 12px; color: #C0C4CC; padding: 8px 0; }
</style>