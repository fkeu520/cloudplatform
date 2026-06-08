import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export interface CrudApi {
  page: (params: any) => Promise<any>
  create?: (data: any) => Promise<any>
  update?: (id: string, data: any) => Promise<any>
  delete?: (id: string) => Promise<any>
  getById?: (id: string) => Promise<any>
}

export interface UseCrudOptions {
  defaultSearch?: Record<string, any>
  defaultPageSize?: number
  defaultForm?: Record<string, any>
  primaryKey?: string
  autoLoad?: boolean
}

function simpleDebounce<T extends (...args: any[]) => any>(fn: T, delay: number) {
  let timer: ReturnType<typeof setTimeout>
  return (...args: Parameters<T>) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

export function useCrud(api: CrudApi, options: UseCrudOptions = {}) {
  const {
    defaultSearch = {},
    defaultPageSize = 10,
    defaultForm = {},
    primaryKey = 'id',
    autoLoad = true
  } = options

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const searchForm = reactive({ ...defaultSearch })
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const isEdit = ref(false)
  const currentId = ref<string | null>(null)
  const formRef = ref()
  const formData = reactive({ ...defaultForm })

  async function loadData() {
    loading.value = true
    try {
      const params = {
        ...searchForm,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
      const res: any = await api.page(params)
      if (res.code === 200) {
        tableData.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } finally {
      loading.value = false
    }
  }

  const debouncedLoadData = simpleDebounce(loadData, 300)

  function handleSearch() {
    pageNum.value = 1
    loadData()
  }

  function handleReset() {
    Object.keys(searchForm).forEach(key => {
      searchForm[key] = defaultSearch[key]
    })
    handleSearch()
  }

  function handleSizeChange(val: number) {
    pageSize.value = val
    loadData()
  }

  function handlePageChange(val: number) {
    pageNum.value = val
    loadData()
  }

  function resetForm() {
    Object.keys(formData).forEach(key => {
      formData[key] = defaultForm[key]
    })
    currentId.value = null
  }

  function handleAdd() {
    resetForm()
    isEdit.value = false
    dialogTitle.value = '新增'
    dialogVisible.value = true
  }

  async function handleEdit(row: any) {
    resetForm()
    isEdit.value = true
    dialogTitle.value = '编辑'
    currentId.value = row[primaryKey]
    if (api.getById) {
      const res: any = await api.getById(row[primaryKey])
      if (res.code === 200) {
        Object.assign(formData, res.data)
      }
    } else {
      Object.assign(formData, row)
    }
    dialogVisible.value = true
  }

  async function handleDelete(row: any) {
    if (!api.delete) return
    try {
      await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
      const res: any = await api.delete(row[primaryKey])
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadData()
      }
    } catch {
      // cancelled
    }
  }

  async function handleSubmit() {
    if (formRef.value) {
      await formRef.value.validate()
    }
    if (isEdit.value && currentId.value && api.update) {
      const res: any = await api.update(currentId.value, formData)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else if (api.create) {
      const res: any = await api.create(formData)
      if (res.code === 200) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        loadData()
      }
    }
  }

  if (autoLoad) {
    onMounted(() => {
      loadData()
    })
  }

  return {
    loading,
    tableData,
    total,
    pageNum,
    pageSize,
    searchForm,
    dialogVisible,
    dialogTitle,
    isEdit,
    currentId,
    formRef,
    formData,
    loadData,
    debouncedLoadData,
    handleSearch,
    handleReset,
    handleSizeChange,
    handlePageChange,
    handleAdd,
    handleEdit,
    handleDelete,
    handleSubmit
  }
}
