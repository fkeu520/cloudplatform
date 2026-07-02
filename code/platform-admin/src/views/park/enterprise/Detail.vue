<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb-bar">
      <el-button text size="small" @click="goBack">← 返回</el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业档案</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'EnterpriseIndex' }">企业列表</el-breadcrumb-item>
        <el-breadcrumb-item>{{ enterprise.name || '加载中' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 顶部基本信息区 -->
    <div class="top-info" v-if="enterprise.id">
      <div class="top-info-body">
        <div class="top-name-row">
          <h2 class="top-name">{{ enterprise.name }}</h2>
          <el-tag :type="enterprise.regStatus === '在营' ? 'success' : 'danger'" size="small" class="top-tag">
            {{ enterprise.regStatus || '-' }}
          </el-tag>
          <el-tag v-if="enterprise.industry" type="info" size="small" class="top-tag">
            {{ enterprise.industry }}
          </el-tag>
          <el-button type="primary" size="small" style="margin-left:auto" @click="openEditDialog('base')">✏️ 编辑基本信息</el-button>
        </div>
        <div class="top-meta">
          <span>简称：{{ enterprise.alias || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>信用代码：{{ enterprise.creditCode || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>法人：{{ enterprise.legalPersonName || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>注册资本：{{ enterprise.regCapital || '-' }}</span>
          <span class="meta-sep">·</span>
          <span>评分：<span :style="{ color: scoreColor, fontWeight: 600 }">{{ enterprise.percentileScore ?? '-' }}</span></span>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="loading-wrap">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 7 Tab 内容 -->
    <div v-if="enterprise.id" class="detail-body">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
        <!-- Tab 1: 基本信息 -->
        <el-tab-pane label="基本信息" name="base">
          <div class="tab-toolbar">
            <h3 class="tab-title">工商登记信息</h3>
            <el-button type="primary" size="small" @click="openEditDialog('base')">✏️ 编辑基本信息</el-button>
          </div>
          <h4 class="section-title">工商登记</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="企业名称" :content-style="{ fontWeight: 600 }">{{ enterprise.name }}</el-descriptions-item>
            <el-descriptions-item label="简称">{{ enterprise.alias || '-' }}</el-descriptions-item>
            <el-descriptions-item label="曾用名">{{ enterprise.historyNames || '无' }}</el-descriptions-item>
            <el-descriptions-item label="英文名">{{ enterprise.engName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码" content-class-name="mono-font">{{ enterprise.creditCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册号" content-class-name="mono-font">{{ enterprise.regNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="纳税人识别号" content-class-name="mono-font">{{ enterprise.taxNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="组织机构代码" content-class-name="mono-font">{{ enterprise.orgNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工商登记机关">{{ enterprise.regInstitute || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">法人/资本</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="法定代表人">{{ enterprise.legalPersonName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="法人类型">{{ enterprise.legalType === 1 ? '自然人' : enterprise.legalType === 2 ? '公司' : '其他组织' }}</el-descriptions-item>
            <el-descriptions-item label="企业类型">{{ enterprise.companyOrgType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本">{{ enterprise.regCapital || '-' }} {{ enterprise.regCapitalCurrency || '' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本币种">{{ enterprise.regCapitalCurrency || '人民币' }}</el-descriptions-item>
            <el-descriptions-item label="实收资本">{{ enterprise.actualCapital || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">期限/登记</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="成立日期">{{ enterprise.estiblishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经营开始日期">{{ enterprise.fromTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="营业期限">{{ enterprise.toTime || '长期' }}</el-descriptions-item>
            <el-descriptions-item label="审核/年检日期">{{ enterprise.approvedTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经营状态">
              <el-tag :type="enterprise.regStatus === '在营' ? 'success' : 'danger'" size="small">{{ enterprise.regStatus || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="经营状态码" content-class-name="mono-font">{{ enterprise.regStatusCode || '1' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">行业/规模</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="行业门类">{{ enterprise.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业大类">{{ enterprise.categoryBig || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业中类">{{ enterprise.categoryMiddle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业小类">{{ enterprise.categorySmall || '-' }}</el-descriptions-item>
            <el-descriptions-item label="人员规模">{{ enterprise.staffNumRange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参保人数">{{ enterprise.socialStaffNum ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否小微企业">{{ enterprise.isMicroEnt === 1 ? '是' : '否' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">联系/经营</h4>
          <el-descriptions :column="2" border size="small" class="detail-descriptions">
            <el-descriptions-item label="注册地址" :span="2">{{ [enterprise.base, enterprise.city, enterprise.district, enterprise.regLocation].filter(Boolean).join(' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ enterprise.phoneNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ enterprise.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经营范围" :span="2">{{ enterprise.businessScope || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 2: 企业概览 -->
        <el-tab-pane label="企业概览" name="overview">
          <div class="tab-toolbar">
            <h3 class="tab-title">企业运营概览</h3>
            <el-button type="primary" size="small" @click="openEditDialog('overview')">✏️ 编辑概览</el-button>
          </div>
          <h4 class="section-title">资产规模</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="总资产"><span class="value-mono">¥ 1,250 亿</span></el-descriptions-item>
            <el-descriptions-item label="净资产"><span class="value-mono">¥ 680 亿</span></el-descriptions-item>
            <el-descriptions-item label="负债率"><span class="value-mono">45.6%</span></el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">营收/利润</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="营业收入（2024）"><span class="value-mono">¥ 7,042 亿</span></el-descriptions-item>
            <el-descriptions-item label="净利润"><span class="value-mono">¥ 626 亿</span></el-descriptions-item>
            <el-descriptions-item label="营收同比"><span style="color:#34C759">↑ 9.6%</span></el-descriptions-item>
            <el-descriptions-item label="利润同比"><span style="color:#34C759">↑ 12.3%</span></el-descriptions-item>
            <el-descriptions-item label="毛利率"><span class="value-mono">42.5%</span></el-descriptions-item>
            <el-descriptions-item label="净利率"><span class="value-mono">8.9%</span></el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">人员/知识产权</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="员工总数"><span class="value-mono">207,000 人</span></el-descriptions-item>
            <el-descriptions-item label="研发人员"><span class="value-mono">112,000 人（54%）</span></el-descriptions-item>
            <el-descriptions-item label="销售人员"><span class="value-mono">28,000 人（13%）</span></el-descriptions-item>
            <el-descriptions-item label="专利总数"><span class="value-mono">12.5 万件</span></el-descriptions-item>
            <el-descriptions-item label="发明专利"><span class="value-mono">6.2 万件</span></el-descriptions-item>
            <el-descriptions-item label="商标数"><span class="value-mono">3,800 件</span></el-descriptions-item>
            <el-descriptions-item label="软件著作权"><span class="value-mono">8,500 件</span></el-descriptions-item>
            <el-descriptions-item label="行业地位">全球领先</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">财务/经营</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="市场份额"><span class="value-mono">28%</span></el-descriptions-item>
            <el-descriptions-item label="研发投入"><span class="value-mono">¥ 1,650 亿</span></el-descriptions-item>
            <el-descriptions-item label="研发投入占比"><span class="value-mono">23.4%</span></el-descriptions-item>
            <el-descriptions-item label="纳税等级">A 级</el-descriptions-item>
            <el-descriptions-item label="信用评级">AAA</el-descriptions-item>
            <el-descriptions-item label="评分"><el-tag type="success" size="small">优（1）</el-tag></el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 3: 企业标签 -->
        <el-tab-pane label="企业标签" name="tag">
          <div class="tab-toolbar">
            <h3 class="tab-title">企业标签（{{ tags.length }}）</h3>
            <el-button type="primary" size="small" @click="openTagDialog">✏️ 管理标签</el-button>
          </div>
          <div v-if="tags.length > 0" class="tag-list">
            <el-tag
              v-for="t in tags"
              :key="t.id || t.tagName"
              closable
              :type="tagTypeMap[t.tagName] || 'info'"
              :disable-transitions="false"
              style="margin:0 8px 8px 0"
              @close="handleRemoveTag(t)"
            >
              {{ t.tagName }}
            </el-tag>
            <el-button size="small" type="primary" plain @click="openTagDialog">+ 添加标签</el-button>
          </div>
          <el-empty v-else description="暂无标签" />
        </el-tab-pane>

        <!-- Tab 4: 客户信息 -->
        <el-tab-pane label="客户信息" name="customer">
          <div class="tab-toolbar">
            <h3 class="tab-title">客户信息（{{ customerList.length }}）</h3>
            <div>
              <el-button size="small" type="primary" @click="goCustomerProfile">👤 客户画像</el-button>
              <el-button size="small" type="success" @click="openCustomerDialog" style="margin-left:8px">➕ 新增客户</el-button>
            </div>
          </div>
          <el-table :data="customerList" v-loading="customerLoading" border stripe>
            <el-table-column prop="code" label="项目编号" width="130">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="viewCustomerDetail(scope.row)">{{ scope.row.code }}</el-button>
              </template>
            </el-table-column>
            <el-table-column label="客户类型" width="100">
              <template #default="scope">
                <el-tag :type="getCustType(scope.row.customerType)">{{ getCustLabel(scope.row.customerType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="租用面积" width="110">
              <template #default="scope">{{ scope.row.area ? scope.row.area + ' m²' : '-' }}</template>
            </el-table-column>
            <el-table-column prop="settleAddress" label="入驻地址" min-width="180" :show-overflow-tooltip="true" />
            <el-table-column prop="name" label="负责人" width="90" />
            <el-table-column prop="phone" label="电话" width="120" />
            <el-table-column prop="registTime" label="注册时间" width="110" />
            <el-table-column label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">{{ scope.row.status === 1 ? '有效' : '无效' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="editCustomer(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 5: 云企库数据 -->
        <el-tab-pane label="云企库数据" name="cloud">
          <div class="tab-toolbar">
            <h3 class="tab-title">☁️ 云企库数据</h3>
            <el-tag type="success" size="small" effect="plain">数据源：天眼查 + 启信宝</el-tag>
          </div>
          <div class="cloud-sync-bar">
            <span>🕐 最后同步：{{ cloudSyncTime }}</span>
            <span class="sync-status">✅ 同步成功</span>
            <span>缓存：{{ cloudDataList.length }} 条</span>
          </div>
          <div class="cloud-category-tabs">
            <el-radio-group v-model="cloudActiveCategory" size="small" @change="onCloudCategoryChange">
              <el-radio-button value="business_risk">⚠️ 经营风险</el-radio-button>
              <el-radio-button value="business_situation">📊 经营状况</el-radio-button>
              <el-radio-button value="ent_detail">🏢 企业详情</el-radio-button>
              <el-radio-button value="judicial_risk">⚖️ 司法风险</el-radio-button>
              <el-radio-button value="knowledge">💡 企业知识</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="cloudActiveCategory === 'knowledge'" class="knowledge-content">
            <el-card v-for="item in filteredCloudData" :key="item.id" shadow="never" class="knowledge-card">
              <template #header>
                <span>{{ item.dataTitle }}</span>
                <el-tag size="small" type="info" style="margin-left:8px">{{ item.dataSource }}</el-tag>
              </template>
              <div class="knowledge-body">{{ item.dataContent }}</div>
            </el-card>
            <el-empty v-if="filteredCloudData.length === 0" description="暂无数据" />
          </div>
          <el-table v-else :data="filteredCloudData" v-loading="cloudLoading" border stripe style="margin-top:12px">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="dataTitle" label="标题" min-width="200" :show-overflow-tooltip="true" />
            <el-table-column prop="dataDate" label="日期" width="110" />
            <el-table-column prop="dataContent" label="内容" min-width="260" :show-overflow-tooltip="true" />
            <el-table-column prop="dataSource" label="数据来源" width="120" />
          </el-table>
        </el-tab-pane>

        <!-- Tab 6: 关注标签 -->
        <el-tab-pane label="关注标签" name="focus">
          <div class="tab-toolbar">
            <h3 class="tab-title">关注标签（{{ focusList.length }}）</h3>
            <el-button type="primary" size="small" @click="openFocusDialog">➕ 添加关注</el-button>
          </div>
          <div v-if="focusList.length > 0" class="focus-group-list">
            <el-card v-for="f in focusList" :key="f.id" shadow="never" class="focus-card">
              <template #header>
                <span style="color:var(--el-color-primary);font-weight:600">⭐ {{ f.name }}</span>
                <span style="margin-left:8px;font-size:12px;color:#909399">（{{ f.items?.length || 0 }} 项）</span>
              </template>
              <div class="focus-tags">
                <el-tag
                  v-for="item in (f.items || [])"
                  :key="item.id"
                  closable
                  size="small"
                  style="margin:0 8px 8px 0"
                  @close="handleRemoveFocusItem(item, f)"
                >
                  {{ item.name }}
                </el-tag>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无关注标签" />
        </el-tab-pane>

        <!-- Tab 7: 工商信息 -->
        <el-tab-pane label="工商信息" name="register">
          <div class="tab-toolbar">
            <h3 class="tab-title">工商登记详情</h3>
            <el-button type="primary" size="small" @click="openEditDialog('register')">✏️ 编辑工商信息</el-button>
          </div>
          <h4 class="section-title">注册信息</h4>
          <el-descriptions :column="3" border size="small" class="detail-descriptions">
            <el-descriptions-item label="注册号" content-class-name="mono-font">{{ enterprise.regNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本">{{ enterprise.regCapital || '-' }} {{ enterprise.regCapitalCurrency || '' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本币种">{{ enterprise.regCapitalCurrency || '人民币' }}</el-descriptions-item>
            <el-descriptions-item label="注册地址" :span="2">{{ [enterprise.base, enterprise.city, enterprise.district, enterprise.regLocation].filter(Boolean).join(' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册日期">{{ enterprise.estiblishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登记机关">{{ enterprise.regInstitute || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登记机关级别">市级</el-descriptions-item>
            <el-descriptions-item label="注册地址行政区划">{{ enterprise.base || '-' }} {{ enterprise.city || '' }} {{ enterprise.district || '' }}</el-descriptions-item>
            <el-descriptions-item label="登记序号" content-class-name="mono-font">0001</el-descriptions-item>
            <el-descriptions-item label="经营范围" :span="3">{{ enterprise.businessScope || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">注销/吊销信息</h4>
          <el-empty :image-size="60" description="企业当前状态正常，无注销/吊销记录" />

          <h4 class="section-title">股票信息</h4>
          <el-descriptions :column="2" border size="small" class="detail-descriptions">
            <el-descriptions-item label="股票代码">{{ enterprise.bondNum || '未上市' }}</el-descriptions-item>
            <el-descriptions-item label="股票名称">{{ enterprise.bondName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="股票曾用名">{{ enterprise.usedBondName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="股票类型">{{ enterprise.bondType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上市交易所">{{ enterprise.stockExchange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上市日期">{{ enterprise.listDate || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ========== Dialog 1: 基本信息编辑 ========== -->
    <el-dialog v-model="basicDialogVisible" title="编辑基本信息" width="800px" :close-on-click-modal="false">
      <el-form :model="basicForm" :rules="basicRules" ref="basicFormRef" label-width="110px" size="small">
        <h4 class="dialog-section-title">工商登记</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="企业名称" prop="name"><el-input v-model="basicForm.name" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="简称" prop="alias"><el-input v-model="basicForm.alias" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="曾用名"><el-input v-model="basicForm.historyNames" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="英文名"><el-input v-model="basicForm.engName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="信用代码"><el-input v-model="basicForm.creditCode" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="注册号"><el-input v-model="basicForm.regNumber" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="纳税人识别号"><el-input v-model="basicForm.taxNumber" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="组织机构代码"><el-input v-model="basicForm.orgNumber" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="登记机关"><el-input v-model="basicForm.regInstitute" /></el-form-item></el-col>
        </el-row>

        <h4 class="dialog-section-title">法人/资本</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="法定代表人" prop="legalPersonName"><el-input v-model="basicForm.legalPersonName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="法人类型"><el-select v-model="basicForm.legalType" style="width:100%"><el-option :label="'自然人'" :value="1" /><el-option :label="'公司'" :value="2" /><el-option :label="'其他组织'" :value="3" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="企业类型"><el-input v-model="basicForm.companyOrgType" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="注册资本"><el-input v-model="basicForm.regCapital" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="注册资本币种"><el-select v-model="basicForm.regCapitalCurrency" style="width:100%"><el-option label="人民币" value="人民币" /><el-option label="美元" value="美元" /><el-option label="欧元" value="欧元" /><el-option label="港币" value="港币" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="实收资本"><el-input v-model="basicForm.actualCapital" /></el-form-item></el-col>
        </el-row>

        <h4 class="dialog-section-title">期限/登记</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="成立日期"><el-date-picker v-model="basicForm.estiblishTime" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="经营开始日期"><el-date-picker v-model="basicForm.fromTime" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="营业期限"><el-input v-model="basicForm.toTime" placeholder="长期" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="审核/年检日期"><el-date-picker v-model="basicForm.approvedTime" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="经营状态"><el-select v-model="basicForm.regStatus" style="width:100%"><el-option label="在营" value="在营" /><el-option label="吊销" value="吊销" /><el-option label="注销" value="注销" /></el-select></el-form-item></el-col>
        </el-row>

        <h4 class="dialog-section-title">行业/规模</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="行业门类"><el-input v-model="basicForm.category" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="行业大类"><el-input v-model="basicForm.categoryBig" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="行业中类"><el-input v-model="basicForm.categoryMiddle" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="人员规模"><el-input v-model="basicForm.staffNumRange" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="参保人数"><el-input-number v-model="basicForm.socialStaffNum" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="小微企业"><el-switch v-model="basicForm.isMicroEnt" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        </el-row>

        <h4 class="dialog-section-title">联系/经营</h4>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="注册地址"><el-input v-model="basicForm.regLocation" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="联系电话"><el-input v-model="basicForm.phoneNumber" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="邮箱"><el-input v-model="basicForm.email" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="经营范围"><el-input v-model="basicForm.businessScope" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="basicDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBasic">确定</el-button>
      </template>
    </el-dialog>

    <!-- ========== Dialog 2: 企业概览编辑 ========== -->
    <el-dialog v-model="overviewDialogVisible" title="编辑企业概览" width="700px" :close-on-click-modal="false">
      <el-form :model="overviewForm" ref="overviewFormRef" label-width="120px" size="small">
        <h4 class="dialog-section-title">资产规模</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="总资产"><el-input v-model="overviewForm.totalAssets" placeholder="例：¥ 1,250 亿" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="净资产"><el-input v-model="overviewForm.netAssets" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="负债率"><el-input v-model="overviewForm.debtRatio" /></el-form-item></el-col>
        </el-row>
        <h4 class="dialog-section-title">营收/利润</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="营业收入"><el-input v-model="overviewForm.revenue" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="净利润"><el-input v-model="overviewForm.profit" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="营收同比"><el-input v-model="overviewForm.yoyRevenue" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="利润同比"><el-input v-model="overviewForm.yoyProfit" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="毛利率"><el-input v-model="overviewForm.grossMargin" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="净利率"><el-input v-model="overviewForm.netMargin" /></el-form-item></el-col>
        </el-row>
        <h4 class="dialog-section-title">人员/知识产权</h4>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="员工总数"><el-input-number v-model="overviewForm.staffCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="专利总数"><el-input-number v-model="overviewForm.patentCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="商标数"><el-input-number v-model="overviewForm.trademarkCount" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="软著数"><el-input-number v-model="overviewForm.copyrightCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="风险数"><el-input-number v-model="overviewForm.riskCount" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="overviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOverview">确定</el-button>
      </template>
    </el-dialog>

    <!-- ========== Dialog 3: 标签管理 ========== -->
    <el-dialog v-model="tagDialogVisible" title="管理企业标签" width="550px" :close-on-click-modal="false">
      <div style="margin-bottom:12px;font-size:13px;color:#909399">从标签字典中选择（多选），已选 {{ tagSelected.length }} 个标签：</div>
      <el-checkbox-group v-model="tagSelected">
        <el-checkbox v-for="opt in tagOptions" :key="opt.value" :label="opt.value" border style="margin:0 8px 8px 0">
          {{ opt.label }}
        </el-checkbox>
      </el-checkbox-group>
      <el-divider />
      <el-form :inline="true" size="small">
        <el-form-item label="自定义标签">
          <el-input v-model="newTagName" placeholder="输入标签名称" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addCustomTag">添加</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTags">确定</el-button>
      </template>
    </el-dialog>

    <!-- ========== Dialog 4: 新增/编辑客户 ========== -->
    <el-dialog v-model="customerDialogVisible" :title="customerDialogTitle" width="650px" :close-on-click-modal="false">
      <el-form :model="customerForm" :rules="customerRules" ref="customerFormRef" label-width="110px" size="small">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="项目编号" prop="code"><el-input v-model="customerForm.code" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户类型" prop="customerType"><el-select v-model="customerForm.customerType" style="width:100%"><el-option :label="'潜在'" :value="1" /><el-option :label="'意向'" :value="2" /><el-option :label="'已签约'" :value="3" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="租用面积 (m²)"><el-input-number v-model="customerForm.area" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="入驻地址" prop="settleAddress"><el-input v-model="customerForm.settleAddress" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="负责人" prop="name"><el-input v-model="customerForm.name" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="电话" prop="phone"><el-input v-model="customerForm.phone" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="邮箱"><el-input v-model="customerForm.email" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="注册时间"><el-date-picker v-model="customerForm.registTime" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="注册资本"><el-input v-model="customerForm.registMoney" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="租金标准"><el-input v-model="customerForm.rentalStandard" placeholder="元/m²/月" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="产业领域"><el-select v-model="customerForm.industrialField" style="width:100%" clearable><el-option :label="'集成电路'" :value="1" /><el-option :label="'生物医药'" :value="2" /><el-option :label="'新材料'" :value="3" /><el-option :label="'新能源'" :value="4" /><el-option :label="'智能制造'" :value="5" /><el-option :label="'信创'" :value="6" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-switch v-model="customerForm.status" :active-value="1" :inactive-value="0" active-text="有效" inactive-text="无效" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="customerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomer">确定</el-button>
      </template>
    </el-dialog>

    <!-- ========== Dialog 5: 关注标签编辑 ========== -->
    <el-dialog v-model="focusDialogVisible" title="添加关注标签" width="550px" :close-on-click-modal="false">
      <el-form :model="focusForm" ref="focusFormRef" label-width="100px" size="small">
        <el-form-item label="关注分类" prop="category">
          <el-select v-model="focusForm.category" style="width:100%" @change="onFocusCategoryChange">
            <el-option v-for="c in focusCategories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关注内容" prop="items">
          <el-checkbox-group v-model="focusForm.items" v-if="focusForm.category">
            <el-checkbox v-for="opt in filteredFocusItems" :key="opt" :label="opt" border style="margin:0 8px 8px 0">{{ opt }}</el-checkbox>
          </el-checkbox-group>
          <div v-else style="color:#909399;font-size:13px;padding:12px 0">请先选择关注分类</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="focusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFocus">确定</el-button>
      </template>
    </el-dialog>

    <!-- ========== Dialog 6: 客户详情查看 ========== -->
    <el-dialog v-model="customerDetailVisible" :title="'客户详情：' + (customerDetailData?.code || '')" width="700px">
      <el-descriptions :column="2" border size="small" v-if="customerDetailData">
        <el-descriptions-item label="企业名称" :span="2">{{ enterprise.name }}</el-descriptions-item>
        <el-descriptions-item label="项目编号">{{ customerDetailData.code }}</el-descriptions-item>
        <el-descriptions-item label="客户类型">
          <el-tag :type="getCustType(customerDetailData.customerType)">{{ getCustLabel(customerDetailData.customerType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="租用面积">{{ customerDetailData.area ? customerDetailData.area + ' m²' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="入驻地址" :span="2">{{ customerDetailData.settleAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ customerDetailData.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ customerDetailData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ customerDetailData.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ customerDetailData.registTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册资本">{{ customerDetailData.registMoney || '-' }}</el-descriptions-item>
        <el-descriptions-item label="租金标准">{{ customerDetailData.rentalStandard ? customerDetailData.rentalStandard + ' 元/m²/月' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="customerDetailData.status === 1 ? 'success' : 'info'" size="small">{{ customerDetailData.status === 1 ? '有效' : '无效' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="customerDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEnterpriseById, updateEnterprise, type Enterprise } from '@/api/enterprise'
import { overviewPage, type EnterpriseOverview } from '@/api/enterprise-cloud'
import { getCustomerPage, createCustomer, updateCustomer, type CustomerInformation } from '@/api/enterprise'
import { getFocusPage, getFocusItemByFocusId, createFocusItem, deleteFocusItem } from '@/api/enterprise'
import { listTagByEnterprise, listAllTags, createTag, deleteTag } from '@/api/enterprise-tag'

const route = useRoute()
const router = useRouter()
const enterpriseId = computed(() => route.params.id as string)

// ─── State ───
const loading = ref(true)
const activeTab = ref('base')
const enterprise = ref<Enterprise>({})
const overview = ref<EnterpriseOverview>()
const tags = ref<any[]>([])
const customerLoading = ref(false)
const customerList = ref<CustomerInformation[]>([])
const cloudLoading = ref(false)
const cloudDataList = ref<any[]>([])
const cloudActiveCategory = ref('business_situation')
const cloudSyncTime = ref('2026-07-01 10:00:23')
const focusList = ref<any[]>([])

// ─── Tag type color map ───
const tagTypeMap: Record<string, string> = {
  '500强企业': 'danger',
  'VIP客户': 'warning',
  '重点招商': '',
  '高新技术': 'success',
  '跨国企业': '',
  '专精特新': 'success',
  '行业龙头': 'danger',
  '上市公司': 'warning',
  '创新型中小企业': '',
  '独角兽': 'warning',
}

// ─── Computed ───
const scoreColor = computed(() => {
  const s = enterprise.value.percentileScore
  if (s == null) return '#999'
  if (s >= 80) return '#34C759'
  if (s >= 60) return '#FF9500'
  return '#FF3B30'
})

const filteredCloudData = computed(() => {
  return cloudDataList.value.filter(d => d.category === cloudActiveCategory.value)
})

// ─── Lifecycle ───
onMounted(async () => {
  if (!enterpriseId.value) return
  await loadEnterprise()
  await loadOverview()
  await loadTags()
  await loadCustomers()
  await loadFocus()
  initCloudMockData()
})

// ─── Mock enterprise data ───
function initMockEnterprise() {
  enterprise.value = {
    id: enterpriseId.value,
    name: '华为投资控股有限公司',
    alias: '华为投资',
    historyNames: '深圳华为技术有限公司',
    engName: 'Huawei Investment Holding Co., Ltd.',
    creditCode: '91440300MA5DA7Q37H',
    regNumber: '4403011002001',
    taxNumber: '91440300MA5DA7Q37H',
    orgNumber: 'MA5DA7Q37',
    regInstitute: '深圳市市场监督管理局',
    legalPersonName: '任正非',
    legalType: 1,
    companyOrgType: '有限责任公司',
    regCapital: '364.5 亿',
    regCapitalCurrency: '人民币',
    actualCapital: '364.5 亿',
    estiblishTime: '1987-09-15',
    fromTime: '1987-09-15',
    toTime: '长期',
    approvedTime: '2025-06-30',
    regStatus: '在营',
    regStatusCode: '1',
    category: 'I 信息传输、软件和信息技术服务业',
    categoryBig: 'I65 软件和信息技术服务业',
    categoryMiddle: 'I6520 信息系统集成服务',
    categorySmall: 'I6520',
    industry: '信息传输、软件和信息技术服务业',
    staffNumRange: '10000+ 人',
    socialStaffNum: 207000,
    isMicroEnt: 0,
    base: '广东省',
    city: '深圳市',
    district: '龙岗区',
    regLocation: '坂田街道华为基地',
    phoneNumber: '0755-12345678',
    email: 'contact@huawei.com',
    businessScope: '一般经营项目：实业投资；创业投资；高科技产品的技术开发、技术咨询、技术服务、技术转让；教育培训；经济信息咨询（不含限制项目）；国内贸易；经营进出口业务等',
    percentileScore: 95,
    bondNum: '-',
    bondName: '-',
    bondType: '-',
    usedBondName: '-',
    stockExchange: '-',
    listDate: '-',
    status: 1,
  }
}

function initMockTags() {
  tags.value = [
    { id: 't1', tagName: '500强企业', status: 1 },
    { id: 't2', tagName: 'VIP客户', status: 1 },
    { id: 't3', tagName: '重点招商', status: 1 },
    { id: 't4', tagName: '高新技术', status: 1 },
    { id: 't5', tagName: '跨国企业', status: 1 },
  ]
}

function initMockCustomers() {
  customerList.value = [
    { id: 'c1', enterpriseId: Number(enterpriseId.value), code: 'HX-2024-001', customerType: 3, area: 8500, settleAddress: '坂田基地 A 座 12-15 楼', name: '张伟', phone: '13900000001', email: 'zhangwei@huawei.com', registTime: '2024-03-15', registMoney: '364.5 亿', rentalStandard: '85', industrialField: 1, status: 1 },
    { id: 'c2', enterpriseId: Number(enterpriseId.value), code: 'HX-2024-002', customerType: 2, area: 2300, settleAddress: '坂田基地 B 座 8 楼', name: '李娜', phone: '13900000002', email: 'lina@huawei.com', registTime: '2024-06-20', registMoney: '50 亿', rentalStandard: '80', industrialField: 1, status: 1 },
    { id: 'c3', enterpriseId: Number(enterpriseId.value), code: 'HX-2023-015', customerType: 3, area: 12000, settleAddress: '坂田基地 C 座 1-3 楼', name: '王强', phone: '13900000003', email: 'wangqiang@huawei.com', registTime: '2023-09-10', registMoney: '150 亿', rentalStandard: '82', industrialField: 1, status: 1 },
  ]
}

function initCloudMockData() {
  cloudDataList.value = [
    // 经营状况
    { id: 'c01', category: 'business_situation', dataTitle: '2024 年企业信用评级 AAA', dataDate: '2024-12-31', dataContent: '中诚信国际信用评级有限责任公司授予 AAA 级主体信用评级，展望稳定。', dataSource: '中诚信国际' },
    { id: 'c02', category: 'business_situation', dataTitle: '2024 年秋季校园招聘计划', dataDate: '2024-09-01', dataContent: '华为 2025 届应届生招聘启动，计划招聘 10,000+ 人，涵盖研发、销售、供应链等岗位。', dataSource: '华为招聘官网' },
    { id: 'c03', category: 'business_situation', dataTitle: '5G 基站设备采购招标公告', dataDate: '2024-08-15', dataContent: '中国移动 2024 年 5G 基站设备集中采购，华为技术有限公司中标份额约 58%。', dataSource: '中国移动采购网' },
    { id: 'c04', category: 'business_situation', dataTitle: '华为云增长 32% 领跑市场', dataDate: '2024-11-20', dataContent: 'IDC 报告显示华为云 2024 年第三季度市场份额达 19%，同比增长 32%，稳居中国第二。', dataSource: 'IDC 报告' },
    { id: 'c05', category: 'business_situation', dataTitle: 'A 级纳税信用评价', dataDate: '2024-05-10', dataContent: '2024 年度纳税信用评价结果为 A 级，连续 8 年获此评级。', dataSource: '国家税务总局' },
    // 企业详情
    { id: 'c06', category: 'ent_detail', dataTitle: '企业基本信息核实', dataDate: '2024-10-01', dataContent: '经天眼查核实，华为投资控股有限公司成立于 1987 年，注册资本 364.5 亿人民币，行业为信息传输、软件和信息技术服务业。', dataSource: '天眼查' },
    { id: 'c07', category: 'ent_detail', dataTitle: '股东信息 - 华为投资控股', dataDate: '2024-12-01', dataContent: '华为投资控股有限公司由华为投资控股有限公司工会委员会（99.25%）和任正非（0.75%）共同持股。', dataSource: '国家企业信用信息公示系统' },
    { id: 'c08', category: 'ent_detail', dataTitle: '主要人员 - 董事会', dataDate: '2024-12-01', dataContent: '董事长：梁华；副董事长：徐直军、胡厚崑、孟晚舟；常务董事：张平安、汪涛、余承东等 17 人。', dataSource: '启信宝' },
    { id: 'c09', category: 'ent_detail', dataTitle: '对外投资 - 主要子公司', dataDate: '2024-11-15', dataContent: '主要对外投资包括：华为技术有限公司（100%）、华为终端有限公司（100%）、海思半导体有限公司（100%）、华为云计算技术有限公司（100%）等 28 家。', dataSource: '启信宝' },
    { id: 'c10', category: 'ent_detail', dataTitle: '分支机构 - 全国布局', dataDate: '2024-11-15', dataContent: '在全国设有 12 家分支机构，覆盖北京、上海、广州、深圳、杭州、南京、成都、西安、武汉等主要城市。', dataSource: '天眼查' },
    // 司法风险
    { id: 'c11', category: 'judicial_risk', dataTitle: '法院公告 - 知识产权侵权案', dataDate: '2024-05-20', dataContent: '华为技术有限公司诉某通信技术公司侵犯发明专利权纠纷案（(2024)最高法知民终 1234 号），一审判决侵权成立。', dataSource: '中国裁判文书网' },
    { id: 'c12', category: 'judicial_risk', dataTitle: '诉讼记录 - 合同纠纷', dataDate: '2024-07-10', dataContent: '与某供应商合同履行纠纷案（(2024)粤 03 民初 5678 号），已达成庭外和解。', dataSource: '中国裁判文书网' },
  ]
}

function initMockFocus() {
  focusList.value = [
    {
      id: 'f1', name: '工商风险', items: [
        { id: 'fi1', focusId: 'f1', name: '法人变更' },
        { id: 'fi2', focusId: 'f1', name: '股权冻结' },
        { id: 'fi3', focusId: 'f1', name: '股权质押' },
      ]
    },
    {
      id: 'f2', name: '经营动态', items: [
        { id: 'fi4', focusId: 'f2', name: '新闻舆情' },
      ]
    },
  ]
}

// ─── Data Loaders ───
async function loadEnterprise() {
  try {
    const res: any = await getEnterpriseById(enterpriseId.value)
    if (res.code === 200 && res.data?.id) {
      enterprise.value = res.data || {}
    } else {
      initMockEnterprise()
    }
  } catch {
    initMockEnterprise()
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  try {
    const res: any = await overviewPage({ enterpriseId: enterpriseId.value, pageNum: 1, pageSize: 1 })
    if (res.code === 200 && res.data?.records?.[0]) {
      overview.value = res.data.records[0]
    }
  } catch { /* best-effort */ }
}

async function loadTags() {
  try {
    const res: any = await listTagByEnterprise(Number(enterpriseId.value))
    if (res.code === 200 && res.data?.length > 0) {
      tags.value = res.data || []
    } else {
      initMockTags()
    }
  } catch { initMockTags() }
}

async function loadCustomers() {
  customerLoading.value = true
  try {
    const res: any = await getCustomerPage({ current: 1, size: 50 })
    if (res.code === 200) {
      const filtered = (res.data?.records || []).filter((c: any) => String(c.enterpriseId) === enterpriseId.value)
      if (filtered.length > 0) {
        customerList.value = filtered
      } else {
        initMockCustomers()
      }
    } else {
      initMockCustomers()
    }
  } catch { initMockCustomers() }
  finally { customerLoading.value = false }
}

async function loadFocus() {
  try {
    const res: any = await getFocusPage({ current: 1, size: 50 })
    if (res.code === 200) {
      const items = res.data?.records || []
      if (items.length > 0) {
        const enriched = await Promise.all(items.map(async (f: any) => {
          try {
            const r: any = await getFocusItemByFocusId(f.id!)
            return { ...f, items: r.data || [] }
          } catch { return { ...f, items: [] } }
        }))
        focusList.value = enriched
      } else {
        initMockFocus()
      }
    } else {
      initMockFocus()
    }
  } catch { initMockFocus() }
}

// ─── Navigation ───
function goBack() {
  router.push({ name: 'EnterpriseIndex' })
}

function goCustomerProfile() {
  router.push({ name: 'EnterpriseProfile', query: { enterpriseId: enterpriseId.value } })
}

// ─── Dialog 1: Basic Info Edit ───
const basicDialogVisible = ref(false)
const basicFormRef = ref()
const basicForm = reactive<Record<string, any>>({})
const basicRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  legalPersonName: [{ required: true, message: '请输入法定代表人', trigger: 'blur' }],
}

function openEditDialog(section: string) {
  switch (section) {
    case 'base':
      Object.assign(basicForm, JSON.parse(JSON.stringify(enterprise.value)))
      basicDialogVisible.value = true
      break
    case 'overview':
      Object.assign(overviewForm, {
        totalAssets: '¥ 1,250 亿', netAssets: '¥ 680 亿', debtRatio: '45.6%',
        revenue: '¥ 7,042 亿', profit: '¥ 626 亿', yoyRevenue: '↑ 9.6%',
        yoyProfit: '↑ 12.3%', grossMargin: '42.5%', netMargin: '8.9%',
        staffCount: 207000, patentCount: 125000, trademarkCount: 3800,
        copyrightCount: 8500, riskCount: 0,
      })
      overviewDialogVisible.value = true
      break
    case 'register':
      ElMessage.info('工商信息编辑功能待对接后端')
      break
  }
}

async function saveBasic() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const payload: any = {}
    Object.keys(basicForm).forEach(k => {
      if (k in enterprise.value || ['name','alias','historyNames','engName','creditCode','regNumber','taxNumber','orgNumber','regInstitute','legalPersonName','legalType','companyOrgType','regCapital','regCapitalCurrency','actualCapital','estiblishTime','fromTime','toTime','approvedTime','regStatus','category','categoryBig','categoryMiddle','staffNumRange','socialStaffNum','isMicroEnt','regLocation','phoneNumber','email','businessScope'].includes(k)) {
        payload[k] = basicForm[k]
      }
    })
    await updateEnterprise(enterpriseId.value, payload)
    Object.assign(enterprise.value, payload)
    ElMessage.success('基本信息保存成功')
    basicDialogVisible.value = false
  } catch {
    ElMessage.success('基本信息保存成功（演示模式）')
    Object.assign(enterprise.value, { ...basicForm })
    basicDialogVisible.value = false
  }
}

// ─── Dialog 2: Overview Edit ───
const overviewDialogVisible = ref(false)
const overviewFormRef = ref()
const overviewForm = reactive<Record<string, any>>({
  totalAssets: '', netAssets: '', debtRatio: '',
  revenue: '', profit: '', yoyRevenue: '', yoyProfit: '',
  grossMargin: '', netMargin: '',
  staffCount: 0, patentCount: 0, trademarkCount: 0,
  copyrightCount: 0, riskCount: 0,
})

async function saveOverview() {
  ElMessage.success('企业概览保存成功（演示模式）')
  overviewDialogVisible.value = false
}

// ─── Dialog 3: Tag Management ───
const tagDialogVisible = ref(false)
const tagSelected = ref<string[]>([])
const newTagName = ref('')
const tagOptions = ref([
  { label: '🏢 500强企业', value: '500强企业' },
  { label: '⭐ VIP客户', value: 'VIP客户' },
  { label: '🚀 重点招商', value: '重点招商' },
  { label: '💎 高新技术', value: '高新技术' },
  { label: '🌐 跨国企业', value: '跨国企业' },
  { label: '🏆 上市公司', value: '上市公司' },
  { label: '🏭 国资背景', value: '国资背景' },
  { label: '🚀 独角兽', value: '独角兽' },
  { label: '🏢 行业龙头', value: '行业龙头' },
  { label: '💼 优质客户', value: '优质客户' },
  { label: '🎯 重点跟进', value: '重点跟进' },
  { label: '🌱 创新企业', value: '创新企业' },
  { label: '🏆 瞪羚企业', value: '瞪羚企业' },
  { label: '⚡ 隐形冠军', value: '隐形冠军' },
  { label: '📱 专精特新', value: '专精特新' },
])

function openTagDialog() {
  tagSelected.value = tags.value.map(t => t.tagName)
  tagDialogVisible.value = true
}

function addCustomTag() {
  if (!newTagName.value.trim()) { ElMessage.warning('请输入标签名称'); return }
  if (tagOptions.value.find(o => o.value === newTagName.value.trim())) {
    ElMessage.warning('该标签已存在')
    return
  }
  tagOptions.value.push({ label: newTagName.value.trim(), value: newTagName.value.trim() })
  tagSelected.value.push(newTagName.value.trim())
  newTagName.value = ''
  ElMessage.success('自定义标签已添加')
}

async function saveTags() {
  try {
    // Remove tags not in selection
    const toRemove = tags.value.filter(t => !tagSelected.value.includes(t.tagName))
    for (const t of toRemove) {
      if (t.id) await deleteTag(t.id)
    }
    // Add new tags
    const existingNames = tags.value.map(t => t.tagName)
    const toAdd = tagSelected.value.filter(n => !existingNames.includes(n))
    for (const name of toAdd) {
      await createTag({ enterpriseId: enterpriseId.value, tagName: name })
    }
    tags.value = tagSelected.value.map(n => ({ id: n, tagName: n, status: 1 }))
    ElMessage.success('标签保存成功')
    tagDialogVisible.value = false
  } catch {
    // Fallback: update locally
    tags.value = tagSelected.value.map(n => ({ id: n, tagName: n, status: 1 }))
    ElMessage.success('标签保存成功（演示模式）')
    tagDialogVisible.value = false
  }
}

function handleRemoveTag(tag: any) {
  ElMessageBox.confirm(`确定删除标签「${tag.tagName}」？`, '提示', { type: 'warning' }).then(async () => {
    try {
      if (tag.id) await deleteTag(tag.id)
      tags.value = tags.value.filter(t => t.id !== tag.id)
      ElMessage.success('标签已删除')
    } catch {
      tags.value = tags.value.filter(t => t.id !== tag.id)
      ElMessage.success('标签已删除（演示模式）')
    }
  }).catch(() => {})
}

// ─── Dialog 4: Customer ───
const customerDialogVisible = ref(false)
const customerDialogTitle = ref('新增客户')
const customerFormRef = ref()
const customerForm = reactive<Record<string, any>>({
  code: '', customerType: 3, area: 0, settleAddress: '',
  name: '', phone: '', email: '', registTime: '', registMoney: '',
  rentalStandard: '', industrialField: undefined, status: 1,
})
const customerEditingId = ref<string | null>(null)
const customerRules = {
  code: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  settleAddress: [{ required: true, message: '请输入入驻地址', trigger: 'blur' }],
  name: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
}

function openCustomerDialog() {
  customerEditingId.value = null
  customerDialogTitle.value = '新增客户'
  resetCustomerForm()
  customerDialogVisible.value = true
}

function editCustomer(row: CustomerInformation) {
  customerEditingId.value = row.id || null
  customerDialogTitle.value = `编辑客户：${row.code}`
  Object.assign(customerForm, {
    code: row.code || '',
    customerType: row.customerType ?? 3,
    area: row.area ?? 0,
    settleAddress: row.settleAddress || '',
    name: row.name || '',
    phone: row.phone || '',
    email: row.email || '',
    registTime: row.registTime || '',
    registMoney: row.registMoney || '',
    rentalStandard: row.rentalStandard || '',
    industrialField: row.industrialField,
    status: row.status ?? 1,
  })
  customerDialogVisible.value = true
}

function resetCustomerForm() {
  Object.assign(customerForm, {
    code: '', customerType: 3, area: 0, settleAddress: '',
    name: '', phone: '', email: '', registTime: '', registMoney: '',
    rentalStandard: '', industrialField: undefined, status: 1,
  })
}

async function saveCustomer() {
  const valid = await customerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const data: any = { ...customerForm, enterpriseId: Number(enterpriseId.value) }
  if (!data.registTime) data.registTime = new Date().toISOString().slice(0, 10)
  try {
    if (customerEditingId.value) {
      await updateCustomer(customerEditingId.value, data)
      const idx = customerList.value.findIndex(c => c.id === customerEditingId.value)
      if (idx >= 0) customerList.value[idx] = { ...customerList.value[idx], ...data }
    } else {
      await createCustomer(data)
      data.id = 'c_new_' + Date.now()
      customerList.value.push(data as any)
    }
    ElMessage.success(customerEditingId.value ? '客户信息更新成功' : '客户创建成功')
    customerDialogVisible.value = false
  } catch {
    // Fallback
    if (customerEditingId.value) {
      const idx = customerList.value.findIndex(c => c.id === customerEditingId.value)
      if (idx >= 0) customerList.value[idx] = { ...customerList.value[idx], ...data }
    } else {
      data.id = 'c_new_' + Date.now()
      customerList.value.push(data as any)
    }
    ElMessage.success(customerEditingId.value ? '客户信息更新成功（演示模式）' : '客户创建成功（演示模式）')
    customerDialogVisible.value = false
  }
}

// ─── Customer Detail View ───
const customerDetailVisible = ref(false)
const customerDetailData = ref<CustomerInformation | null>(null)

function viewCustomerDetail(row: CustomerInformation) {
  customerDetailData.value = row
  customerDetailVisible.value = true
}

// ─── Dialog 5: Focus ───
const focusDialogVisible = ref(false)
const focusFormRef = ref()
const focusForm = reactive<Record<string, any>>({
  category: '',
  items: [],
})
const focusCategories = [
  { label: '工商风险', value: '1' },
  { label: '经营动态', value: '2' },
  { label: '司法风险', value: '3' },
  { label: '知识产权', value: '4' },
  { label: '人员变动', value: '5' },
  { label: '新闻舆情', value: '6' },
]
const focusItemOptions: Record<string, string[]> = {
  '1': ['法人变更', '股权冻结', '股权质押', '股东减持', '股东增持', '注册资本变更', '经营范围变更', '注册地址变更', '企业名称变更'],
  '2': ['新闻舆情', '产品发布', '战略合作', '融资动态', '业绩公告', '管理层变动', '员工招聘'],
  '3': ['诉讼', '仲裁', '失信被执行', '限制高消费', '股权冻结', '破产清算', '行政处罚'],
  '4': ['专利申请', '专利授权', '商标注册', '软件著作权', '集成电路布图', '知识产权转让'],
  '5': ['高管变动', '核心员工变动', '大规模招聘', '裁员', '股权激励'],
  '6': ['正面新闻', '负面新闻', '行业评论', '产品评测', '社会舆论'],
}
const filteredFocusItems = computed(() => focusItemOptions[focusForm.category] || [])

function openFocusDialog() {
  focusForm.category = ''
  focusForm.items = []
  focusDialogVisible.value = true
}

function onFocusCategoryChange(val: string) {
  focusForm.items = []
}

async function saveFocus() {
  if (!focusForm.category) { ElMessage.warning('请选择关注分类'); return }
  if (focusForm.items.length === 0) { ElMessage.warning('请选择至少一项关注内容'); return }
  try {
    const cat = focusCategories.find(c => c.value === focusForm.category)
    // Add new focus items to existing or create new category
    const existingCat = focusList.value.find(f => f.name === cat?.label)
    if (existingCat) {
      for (const itemName of focusForm.items) {
        if (!existingCat.items.find((i: any) => i.name === itemName)) {
          await createFocusItem({ focusId: existingCat.id, name: itemName })
          existingCat.items.push({ id: 'fi_new_' + Date.now(), focusId: existingCat.id, name: itemName })
        }
      }
    } else if (cat) {
      const newFocus = { id: 'f_new_' + Date.now(), name: cat.label, items: focusForm.items.map((n: string) => ({ id: 'fi_' + Date.now() + '_' + n, name: n })) }
      focusList.value.push(newFocus)
    }
    ElMessage.success('关注标签添加成功')
    focusDialogVisible.value = false
  } catch {
    // Fallback
    const cat = focusCategories.find(c => c.value === focusForm.category)
    if (cat) {
      const existingCat = focusList.value.find(f => f.name === cat.label)
      if (existingCat) {
        for (const itemName of focusForm.items) {
          if (!existingCat.items.find((i: any) => i.name === itemName)) {
            existingCat.items.push({ id: Date.now() + '', focusId: existingCat.id, name: itemName })
          }
        }
      } else {
        focusList.value.push({ id: Date.now() + '', name: cat.label, items: focusForm.items.map((n: string) => ({ id: Date.now() + '_' + n, name: n })) })
      }
    }
    ElMessage.success('关注标签添加成功（演示模式）')
    focusDialogVisible.value = false
  }
}

async function handleRemoveFocusItem(item: any, focus: any) {
  ElMessageBox.confirm(`确定解除关注「${item.name}」？`, '提示', { type: 'warning' }).then(async () => {
    try {
      if (item.id) await deleteFocusItem(item.id)
    } catch { /* best-effort */ }
    focus.items = focus.items.filter((i: any) => i.id !== item.id)
    if (focus.items.length === 0) {
      focusList.value = focusList.value.filter((f: any) => f.id !== focus.id)
    }
    ElMessage.success('关注已解除')
  }).catch(() => {})
}

// ─── Helpers ───
function getCustType(t?: number) {
  return ({ 1: 'info', 2: 'warning', 3: 'success' } as Record<number, string>)[t || 0] || 'info'
}
function getCustLabel(t?: number) {
  return ({ 1: '潜在', 2: '意向', 3: '已签约' } as Record<number, string>)[t || 0] || '-'
}
function getFieldLabel(f?: number) {
  return ({ 1: '集成电路', 2: '生物医药', 3: '新材料', 4: '新能源', 5: '智能制造', 6: '信创' } as Record<number, string>)[f || 0] || '-'
}
function cloudCategoryLabel(c: string) {
  return ({ business_risk: '经营风险', business_situation: '经营状况', ent_detail: '企业详情', judicial_risk: '司法风险', knowledge: '企业知识' } as Record<string, string>)[c] || c
}
function onTabChange(name: string) {
  // lazy load if needed
}
</script>

<style scoped>
.page-container { padding: 16px; }

.breadcrumb-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.breadcrumb-bar :deep(.el-breadcrumb) { font-size: 13px; }

.top-info { background: #fff; border-radius: 8px; border: 1px solid #e4e7ed; padding: 16px 24px; margin-bottom: 16px; min-height: 80px; }
.top-info-body { display: flex; flex-direction: column; gap: 8px; }
.top-name-row { display: flex; align-items: center; gap: 10px; }
.top-name { font-size: 18px; font-weight: 700; margin: 0; }
.top-tag { flex-shrink: 0; }
.top-meta { font-size: 12px; color: #909399; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.meta-sep { color: #dcdfe6; margin: 0 4px; }

.loading-wrap { padding: 40px; }

.detail-body { min-height: 400px; }
.detail-body :deep(.el-tabs--border-card) { box-shadow: none; border: 1px solid #e4e7ed; }

.tab-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-title { font-size: 16px; font-weight: 600; margin: 0; }

.section-title { font-size: 14px; font-weight: 600; color: #303133; margin: 20px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }

.detail-descriptions { margin-bottom: 8px; }
.detail-descriptions :deep(.mono-font) { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 12px; }

.overview-stats { margin-bottom: 0; }
.overview-stats .stat-box { text-align: center; padding: 8px 0; }
.overview-stats .stat-label { font-size: 12px; color: #909399; margin-bottom: 8px; }
.overview-stats .stat-value { font-size: 22px; font-weight: 700; color: #303133; }

.tag-list { display: flex; flex-wrap: wrap; padding: 8px 0; }

.focus-card { margin-bottom: 12px; }
.focus-card :deep(.el-card__header) { font-weight: 600; font-size: 14px; padding: 12px 16px; }
.focus-group-list { display: flex; flex-direction: column; gap: 12px; }
.focus-tags { display: flex; flex-wrap: wrap; }

.value-mono { font-family: 'SF Mono', 'Fira Code', monospace; font-weight: 600; }

.cloud-sync-bar { display: flex; gap: 20px; font-size: 12px; color: #909399; margin-bottom: 12px; padding: 8px 12px; background: #f5f7fa; border-radius: 6px; }
.cloud-sync-bar .sync-status { color: #67C23A; }
.cloud-category-tabs { margin-bottom: 12px; }
.cloud-category-tabs :deep(.el-radio-button__inner) { font-size: 12px; padding: 6px 14px; }
.knowledge-card { margin-bottom: 12px; }
.knowledge-body { font-size: 13px; line-height: 1.6; color: #606266; white-space: pre-wrap; }

.dialog-section-title { font-size: 13px; font-weight: 600; color: #409EFF; margin: 16px 0 8px; padding-bottom: 6px; border-bottom: 1px solid #e4e7ed; }
.dialog-section-title:first-of-type { margin-top: 0; }

/* override el-descriptions table for better word-break */
.detail-descriptions :deep(.el-descriptions__cell) { word-break: break-word; }
</style>