<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="客户类型">
          <el-select v-model="searchForm.customerType" placeholder="全部" clearable style="width:120px">
            <el-option label="潜在" :value="1" />
            <el-option label="意向" :value="2" />
            <el-option label="已签约" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="有效" :value="1" />
            <el-option label="失效" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增客户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" :show-overflow-tooltip="true" />
        <el-table-column prop="code" label="项目编号" width="120" />
        <el-table-column prop="name" label="负责人" width="100" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column label="客户类型" width="100">
          <template #default="scope">
            <el-tag :type="getTypeTagType(scope.row.customerType)">{{ getTypeLabel(scope.row.customerType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="area" label="租用面积(m²)" width="120" />
        <el-table-column prop="industrialField" label="产业领域" width="100">
          <template #default="scope">
            {{ getFieldLabel(scope.row.industrialField) }}
          </template>
        </el-table-column>
        <el-table-column label="营业收入" width="120">
          <template #default="scope">
            {{ getIncomeLabel(scope.row.businessIncome) }}
          </template>
        </el-table-column>
        <el-table-column prop="registTime" label="注册时间" width="160" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '有效' : '失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 6 Tab 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" @close="resetForm" top="3vh">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="formData" label-width="120px" :rules="basicRules" ref="basicForm">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="项目编号" prop="code">
                  <el-input v-model="formData.code" maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="客户类型" prop="customerType">
                  <el-select v-model="formData.customerType" style="width:100%">
                    <el-option label="潜在" :value="1" />
                    <el-option label="意向" :value="2" />
                    <el-option label="已签约" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="负责人" prop="name">
                  <el-input v-model="formData.name" maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="电话" prop="phone">
                  <el-input v-model="formData.phone" maxlength="32" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="客户手机号">
                  <el-input v-model="formData.managerPhone" maxlength="32" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="formData.email" maxlength="128" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租用面积(m²)">
                  <el-input-number v-model="formData.area" :min="0" :precision="2" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="注册资本(元)">
                  <el-input-number v-model="formData.registMoney" :min="0" :precision="2" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="注册时间">
                  <el-date-picker v-model="formData.registTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租金标准(元/m²/月)">
                  <el-input v-model="formData.rentalStandard" maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="入驻地址">
                  <el-input v-model="formData.settleAddress" maxlength="500" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="业务信息" name="biz">
          <el-form :model="formData" label-width="120px">
            <el-form-item label="所属产业领域">
              <el-select v-model="formData.industrialField" style="width:100%">
                <el-option label="集成电路" :value="1" />
                <el-option label="生物医药" :value="2" />
                <el-option label="新材料" :value="3" />
                <el-option label="新能源" :value="4" />
                <el-option label="智能制造" :value="5" />
                <el-option label="信创" :value="6" />
              </el-select>
            </el-form-item>
            <el-form-item label="其他产业领域">
              <el-input v-model="formData.industrialFieldOther" maxlength="64" />
            </el-form-item>
            <el-form-item label="主营业务">
              <el-input v-model="formData.mainBusiness" type="textarea" :rows="2" maxlength="500" />
            </el-form-item>
            <el-form-item label="企业实力 (多选)">
              <el-checkbox-group v-model="strengthsList">
                <el-checkbox :label="1">上市公司</el-checkbox>
                <el-checkbox :label="2">规上企业</el-checkbox>
                <el-checkbox :label="3">独角兽</el-checkbox>
                <el-checkbox :label="4">瞪羚</el-checkbox>
                <el-checkbox :label="5">专精特新</el-checkbox>
                <el-checkbox :label="6">高企培育</el-checkbox>
                <el-checkbox :label="7">科技型中小</el-checkbox>
                <el-checkbox :label="8">外资</el-checkbox>
                <el-checkbox :label="9">市高级人才</el-checkbox>
                <el-checkbox :label="10">其他</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="知识产权" name="ip">
          <el-form :model="formData" label-width="160px">
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="有效知识产权总数"><el-input-number v-model="formData.validIntellectualProperty" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="发明专利"><el-input-number v-model="formData.inventionPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="实用新型"><el-input-number v-model="formData.utilityModelPatent" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="外观设计"><el-input-number v-model="formData.industrialDesignPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="商标"><el-input-number v-model="formData.trademark" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="软件著作权"><el-input-number v-model="formData.softwareCopyright" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="植物新品种"><el-input-number v-model="formData.newPlantVariety" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="集成电路布图"><el-input-number v-model="formData.integratedCircuitLayout" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="购买国外专利"><el-input-number v-model="formData.purchaseForeignPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="其他专利"><el-input-number v-model="formData.otherPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="财务状况" name="finance">
          <el-form :model="formData" label-width="120px">
            <el-form-item label="营业收入">
              <el-select v-model="formData.businessIncome" style="width:100%">
                <el-option label="500万以下" :value="1" />
                <el-option label="500-2000万" :value="2" />
                <el-option label="2000万-1亿" :value="3" />
                <el-option label="1亿以上" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="上年度税收">
              <el-select v-model="formData.lastYearTax" style="width:100%">
                <el-option label="10万以下" :value="1" />
                <el-option label="10-50万" :value="2" />
                <el-option label="50-100万" :value="3" />
                <el-option label="100-300万" :value="4" />
                <el-option label="300-500万" :value="5" />
                <el-option label="500万以上" :value="6" />
              </el-select>
            </el-form-item>
            <el-form-item label="投资总金额(元)">
              <el-input-number v-model="formData.totalInvestmentAmount" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </el-form-item>
            <el-form-item label="融资总金额(元)">
              <el-input-number v-model="formData.totalFinancingAmount" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="企业需求" name="need">
          <el-form :model="formData" label-width="120px">
            <el-form-item label="面临困难 (多选)">
              <el-checkbox-group v-model="difficultiesList">
                <el-checkbox :label="1">市场需求不足</el-checkbox>
                <el-checkbox :label="2">资金紧张</el-checkbox>
                <el-checkbox :label="3">融资渠道狭窄</el-checkbox>
                <el-checkbox :label="4">人才短缺</el-checkbox>
                <el-checkbox :label="5">其他</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="配套服务需求 (多选)">
              <el-checkbox-group v-model="supportingList">
                <el-checkbox :label="1">班车</el-checkbox>
                <el-checkbox :label="2">儿童托管</el-checkbox>
                <el-checkbox :label="3">网络通讯</el-checkbox>
                <el-checkbox :label="4">团餐</el-checkbox>
                <el-checkbox :label="5">公寓</el-checkbox>
                <el-checkbox :label="6">室内环境</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="科技咨询 (多选)">
              <el-checkbox-group v-model="technicalList">
                <el-checkbox :label="1">项目申报</el-checkbox>
                <el-checkbox :label="2">高企认定</el-checkbox>
                <el-checkbox :label="3">成果鉴定</el-checkbox>
                <el-checkbox :label="4">知识产权贯标</el-checkbox>
                <el-checkbox :label="5">创新平台认定</el-checkbox>
                <el-checkbox :label="6">政策推送解读</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="对产业园建议">
              <el-input v-model="formData.suggest" type="textarea" :rows="4" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="物理需求" name="physical">
          <el-form :model="formData" label-width="120px">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="结构荷载">
                  <el-select v-model="formData.structuralLoad" style="width:100%">
                    <el-option label="2kN/m²以下" :value="1" />
                    <el-option label="2-3kN/m²" :value="2" />
                    <el-option label="3-5kN/m²" :value="3" />
                    <el-option label="5kN/m²以上" :value="4" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="楼层高度">
                  <el-select v-model="formData.floorHeight" style="width:100%">
                    <el-option label="3.8m以下" :value="1" />
                    <el-option label="3.8-4.5m" :value="2" />
                    <el-option label="4.5m以上" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="电容量">
                  <el-select v-model="formData.capacitance" style="width:100%">
                    <el-option label="100KW及以下" :value="1" />
                    <el-option label="500KW及以下" :value="2" />
                    <el-option label="1000KW及以下" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="新风排烟">
                  <el-select v-model="formData.freshAirSmokeExhaust" style="width:100%">
                    <el-option label="需要" :value="1" />
                    <el-option label="不需要" :value="2" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8"><el-form-item label="电梯长度(mm)"><el-input-number v-model="formData.elevatorLength" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="电梯宽度(mm)"><el-input-number v-model="formData.elevatorWidth" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="电梯高度(mm)"><el-input-number v-model="formData.elevatorHeight" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCustomerPage, createCustomer, updateCustomer, deleteCustomer,
  type CustomerInformation
} from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<CustomerInformation[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({ customerType: undefined as number | undefined, status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('新增客户')
const activeTab = ref('basic')
const formData = reactive<CustomerInformation>({
  id: undefined, code: '', customerType: 1, area: undefined, settleAddress: '',
  url: '', name: '', phone: '', managerPhone: '', email: '', registTime: '',
  registMoney: undefined, rentalStandard: '',
  industrialField: undefined, industrialFieldOther: '', mainBusiness: '',
  companyStrengths: '', companyStrengthsOther: '',
  validIntellectualProperty: 0, inventionPatents: 0, utilityModelPatent: 0,
  industrialDesignPatents: 0, trademark: 0, softwareCopyright: 0,
  newPlantVariety: 0, integratedCircuitLayout: 0, purchaseForeignPatents: 0,
  otherPatents: 0,
  businessIncome: undefined, lastYearTax: undefined,
  totalInvestmentAmount: undefined, totalFinancingAmount: undefined,
  companyDifficulties: '', companyDifficultiesOther: '',
  supportingServices: '', technicalConsultingServices: '',
  managementServices: '', managementServicesOther: '',
  technologyPlatformServices: '', technologyPlatformServicesOther: '',
  investmentServices: '', investmentServicesOther: '',
  suggest: '',
  structuralLoad: undefined, floorHeight: undefined, capacitance: undefined,
  supplyAndDrainage: '', freshAirSmokeExhaust: undefined,
  elevatorLength: undefined, elevatorWidth: undefined, elevatorHeight: undefined,
  elevatorLoad: undefined,
  status: 1
})

// 多选字段临时数组 (用于 checkbox 双向绑定)
const strengthsList = computed({
  get: () => formData.companyStrengths ? formData.companyStrengths.split(',').map(Number).filter(n => !isNaN(n)) : [],
  set: (v: number[]) => { formData.companyStrengths = v.join(',') }
})
const difficultiesList = computed({
  get: () => formData.companyDifficulties ? formData.companyDifficulties.split(',').map(Number).filter(n => !isNaN(n)) : [],
  set: (v: number[]) => { formData.companyDifficulties = v.join(',') }
})
const supportingList = computed({
  get: () => formData.supportingServices ? formData.supportingServices.split(',').map(Number).filter(n => !isNaN(n)) : [],
  set: (v: number[]) => { formData.supportingServices = v.join(',') }
})
const technicalList = computed({
  get: () => formData.technicalConsultingServices ? formData.technicalConsultingServices.split(',').map(Number).filter(n => !isNaN(n)) : [],
  set: (v: number[]) => { formData.technicalConsultingServices = v.join(',') }
})

const basicRules = {
  code: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }]
}

const getTypeLabel = (t?: number) => ({ 1: '潜在', 2: '意向', 3: '已签约' }[t || 0] || '-')
const getTypeTagType = (t?: number) => ({ 1: 'info', 2: 'warning', 3: 'success' }[t || 0] || '')
const getFieldLabel = (f?: number) => ({ 1: '集成电路', 2: '生物医药', 3: '新材料', 4: '新能源', 5: '智能制造', 6: '信创' }[f || 0] || '-')
const getIncomeLabel = (i?: number) => ({ 1: '500万以下', 2: '500-2000万', 3: '2000万-1亿', 4: '1亿以上' }[i || 0] || '-')

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getCustomerPage({
      current: pageNum.value, size: pageSize.value,
      customerType: searchForm.customerType, status: searchForm.status
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { pageNum.value = 1; loadData() }
const handleReset = () => { searchForm.customerType = undefined; searchForm.status = undefined; pageNum.value = 1; loadData() }
const handleSizeChange = (s: number) => { pageSize.value = s; loadData() }
const handlePageChange = (p: number) => { pageNum.value = p; loadData() }

const handleAdd = () => {
  dialogTitle.value = '新增客户'
  Object.assign(formData, {
    id: undefined, code: '', customerType: 1, area: undefined, settleAddress: '',
    url: '', name: '', phone: '', managerPhone: '', email: '', registTime: '',
    registMoney: undefined, rentalStandard: '',
    industrialField: undefined, industrialFieldOther: '', mainBusiness: '',
    companyStrengths: '', companyStrengthsOther: '',
    validIntellectualProperty: 0, inventionPatents: 0, utilityModelPatent: 0,
    industrialDesignPatents: 0, trademark: 0, softwareCopyright: 0,
    newPlantVariety: 0, integratedCircuitLayout: 0, purchaseForeignPatents: 0,
    otherPatents: 0,
    businessIncome: undefined, lastYearTax: undefined,
    totalInvestmentAmount: undefined, totalFinancingAmount: undefined,
    companyDifficulties: '', companyDifficultiesOther: '',
    supportingServices: '', technicalConsultingServices: '',
    managementServices: '', managementServicesOther: '',
    technologyPlatformServices: '', technologyPlatformServicesOther: '',
    investmentServices: '', investmentServicesOther: '',
    suggest: '',
    structuralLoad: undefined, floorHeight: undefined, capacitance: undefined,
    supplyAndDrainage: '', freshAirSmokeExhaust: undefined,
    elevatorLength: undefined, elevatorWidth: undefined, elevatorHeight: undefined,
    elevatorLoad: undefined,
    status: 1
  })
  activeTab.value = 'basic'
  dialogVisible.value = true
}

const handleEdit = async (row: CustomerInformation) => {
  dialogTitle.value = '编辑客户'
  const full: any = await getCustomerById(row.id!)
  Object.assign(formData, full.data || row)
  activeTab.value = 'basic'
  dialogVisible.value = true
}

const handleDelete = async (row: CustomerInformation) => {
  try {
    await ElMessageBox.confirm(`确认删除客户 [${row.code}]?`, '提示', { type: 'warning' })
    await deleteCustomer(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* user cancelled */ }
}

const handleSubmit = async () => {
  if (!formData.code) { ElMessage.warning('请输入项目编号'); activeTab.value = 'basic'; return }
  if (!formData.name) { ElMessage.warning('请输入负责人'); activeTab.value = 'basic'; return }
  if (!formData.phone) { ElMessage.warning('请输入电话'); activeTab.value = 'basic'; return }
  try {
    if (formData.id) {
      await updateCustomer(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createCustomer(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) { if (e?.message) ElMessage.error(e.message) }
}

const getCustomerById = async (id: string) => {
  return await import('@/api/enterprise').then(m => m.getCustomerById(id))
}

const resetForm = () => {
  // 由 @close 触发, 实际重置在 handleAdd
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
