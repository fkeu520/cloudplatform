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
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="editCustomer(scope.row)">编辑</el-button>
                <el-button type="danger" size="small" @click="handleDeleteCustomer(scope.row)">删除</el-button>
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

    <!-- ========== Dialog 4: 新增/编辑客户（6 Tab） ========== -->
    <el-dialog v-model="customerDialogVisible" :title="customerDialogTitle" width="920px" :close-on-click-modal="false" top="3vh">
      <el-tabs v-model="customerEditTab" type="border-card">
        <el-tab-pane label="基本信息" name="c-base">
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
              <el-col :span="12"><el-form-item label="专属链接"><el-input v-model="customerForm.url" placeholder="https://...专属链接" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="状态"><el-switch v-model="customerForm.status" :active-value="1" :inactive-value="0" active-text="有效" inactive-text="无效" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="业务信息" name="c-business">
          <el-form :model="customerForm" label-width="120px" size="small">
            <el-form-item label="所属产业领域"><el-select v-model="customerForm.industrialField" style="width:100%"><el-option :label="'集成电路'" :value="1" /><el-option :label="'生物医药'" :value="2" /><el-option :label="'新材料'" :value="3" /><el-option :label="'新能源'" :value="4" /><el-option :label="'智能制造'" :value="5" /><el-option :label="'信创'" :value="6" /></el-select></el-form-item>
            <el-form-item label="其他领域"><el-input v-model="customerForm.industrialFieldOther" maxlength="128" /></el-form-item>
            <el-form-item label="主营业务"><el-input v-model="customerForm.mainBusiness" type="textarea" :rows="3" maxlength="500" /></el-form-item>
            <el-form-item label="企业实力（多选）"><el-checkbox-group v-model="strengthsListEdit"><el-checkbox :label="1">上市公司</el-checkbox><el-checkbox :label="2">规上企业</el-checkbox><el-checkbox :label="3">独角兽</el-checkbox><el-checkbox :label="4">瞪羚</el-checkbox><el-checkbox :label="5">专精特新</el-checkbox><el-checkbox :label="6">高企培育</el-checkbox><el-checkbox :label="7">科技型中小</el-checkbox><el-checkbox :label="8">外资</el-checkbox><el-checkbox :label="9">市高级人才</el-checkbox><el-checkbox :label="10">其他</el-checkbox></el-checkbox-group></el-form-item>
            <el-form-item label="其他实力"><el-input v-model="customerForm.companyStrengthsOther" maxlength="128" /></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="知识产权" name="c-ip">
          <el-form :model="customerForm" label-width="160px" size="small">
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="有效知识产权总数"><el-input-number v-model="customerForm.validIntellectualProperty" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="发明专利"><el-input-number v-model="customerForm.inventionPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="实用新型"><el-input-number v-model="customerForm.utilityModelPatent" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="外观设计"><el-input-number v-model="customerForm.industrialDesignPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="商标"><el-input-number v-model="customerForm.trademark" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="软件著作权"><el-input-number v-model="customerForm.softwareCopyright" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="植物新品种"><el-input-number v-model="customerForm.newPlantVariety" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="集成电路布图"><el-input-number v-model="customerForm.integratedCircuitLayout" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="购买国外专利"><el-input-number v-model="customerForm.purchaseForeignPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="其他专利"><el-input-number v-model="customerForm.otherPatents" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="财务状况" name="c-finance">
          <el-form :model="customerForm" label-width="120px" size="small">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="营业收入"><el-select v-model="customerForm.businessIncome" style="width:100%"><el-option :label="'500万以下'" :value="1" /><el-option :label="'500-2000万'" :value="2" /><el-option :label="'2000万-1亿'" :value="3" /><el-option :label="'1亿以上'" :value="4" /></el-select></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="上年度税收"><el-select v-model="customerForm.lastYearTax" style="width:100%"><el-option :label="'10万以下'" :value="1" /><el-option :label="'10-50万'" :value="2" /><el-option :label="'50-100万'" :value="3" /><el-option :label="'100-300万'" :value="4" /><el-option :label="'300-500万'" :value="5" /><el-option :label="'500万以上'" :value="6" /></el-select></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="投资总金额(元)"><el-input-number v-model="customerForm.totalInvestmentAmount" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="融资总金额(元)"><el-input-number v-model="customerForm.totalFinancingAmount" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="企业需求" name="c-needs">
          <el-form :model="customerForm" label-width="120px" size="small">
            <el-form-item label="面临困难（多选）"><el-checkbox-group v-model="difficultiesListEdit"><el-checkbox :label="1">市场需求不足</el-checkbox><el-checkbox :label="2">资金紧张</el-checkbox><el-checkbox :label="3">融资渠道狭窄</el-checkbox><el-checkbox :label="4">人才短缺</el-checkbox><el-checkbox :label="5">其他</el-checkbox></el-checkbox-group></el-form-item>
            <el-form-item label="其他困难"><el-input v-model="customerForm.companyDifficultiesOther" maxlength="128" /></el-form-item>
            <el-form-item label="配套服务需求（多选）"><el-checkbox-group v-model="supportingListEdit"><el-checkbox :label="1">班车</el-checkbox><el-checkbox :label="2">儿童托管</el-checkbox><el-checkbox :label="3">网络通讯</el-checkbox><el-checkbox :label="4">团餐</el-checkbox><el-checkbox :label="5">公寓</el-checkbox><el-checkbox :label="6">室内环境</el-checkbox></el-checkbox-group></el-form-item>
            <el-form-item label="科技咨询服务（多选）"><el-checkbox-group v-model="technicalListEdit"><el-checkbox :label="1">项目申报</el-checkbox><el-checkbox :label="2">高企认定</el-checkbox><el-checkbox :label="3">成果鉴定</el-checkbox><el-checkbox :label="4">知识产权贯标</el-checkbox><el-checkbox :label="5">创新平台认定</el-checkbox><el-checkbox :label="6">政策推送解读</el-checkbox></el-checkbox-group></el-form-item>
            <el-form-item label="管理服务需求"><el-input v-model="customerForm.managementServices" maxlength="200" /></el-form-item>
            <el-form-item label="公共技术平台服务需求"><el-input v-model="customerForm.technologyPlatformServices" maxlength="200" /></el-form-item>
            <el-form-item label="投资服务需求"><el-input v-model="customerForm.investmentServices" maxlength="200" /></el-form-item>
            <el-form-item label="对产业园建议"><el-input v-model="customerForm.suggest" type="textarea" :rows="3" maxlength="500" /></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="物理需求" name="c-physical">
          <el-form :model="customerForm" label-width="120px" size="small">
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="结构荷载 (kN/m²)"><el-input v-model="customerForm.structuralLoadStr" placeholder="如：8.0" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="楼层高度 (m)"><el-input v-model="customerForm.floorHeightStr" placeholder="如：4.5" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="电容量 (kW)"><el-input v-model="customerForm.capacitanceStr" placeholder="如：500" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="给排水"><el-select v-model="customerForm.supplyAndDrainage" style="width:100%"><el-option label="有" value="有" /><el-option label="无" value="无" /></el-select></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="新风排烟"><el-select v-model="customerForm.freshAirSmokeExhaustStr" style="width:100%"><el-option label="需要" value="需要" /><el-option label="不需要" value="不需要" /></el-select></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="电梯长度 (m)"><el-input v-model="customerForm.elevatorLengthStr" placeholder="如：2.0" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="电梯宽度 (m)"><el-input v-model="customerForm.elevatorWidthStr" placeholder="如：1.8" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="电梯高度 (m)"><el-input v-model="customerForm.elevatorHeightStr" placeholder="如：2.8" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="电梯荷载 (kg)"><el-input v-model="customerForm.elevatorLoadStr" placeholder="如：1600" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
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

    <!-- ========== Dialog 6: 客户详情查看（6 Sub-tab） ========== -->
    <el-dialog v-model="customerDetailVisible" :title="'客户详情：' + (customerDetailData?.code || '')" width="920px" top="3vh" @close="resetCustomerDetailTab">
      <el-tabs v-model="customerDetailTab" type="border-card" v-if="customerDetailData">
        <el-tab-pane label="基本信息" name="c-base">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <span style="font-size:14px;font-weight:600">📋 基本信息</span>
            <el-button size="small" type="primary" plain @click="editFromDetail">✏️ 编辑</el-button>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="企业名称" :span="2">{{ enterprise.name }}</el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ customerDetailData.code }}</el-descriptions-item>
            <el-descriptions-item label="客户类型"><el-tag :type="getCustType(customerDetailData.customerType)">{{ getCustLabel(customerDetailData.customerType) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="租用面积">{{ customerDetailData.area ? customerDetailData.area + ' m²' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="入驻地址" :span="2">{{ customerDetailData.settleAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ customerDetailData.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ customerDetailData.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ customerDetailData.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ customerDetailData.registTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册资本">{{ customerDetailData.registMoney || '-' }}</el-descriptions-item>
            <el-descriptions-item label="租金标准">{{ customerDetailData.rentalStandard ? customerDetailData.rentalStandard + ' 元/m²/月' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="专属链接"><span class="value-mono" style="font-size:11px">{{ customerDetailData.url || '-' }}</span></el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="customerDetailData.status === 1 ? 'success' : 'info'" size="small">{{ customerDetailData.status === 1 ? '有效' : '无效' }}</el-tag></el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="业务信息" name="c-business">
          <div style="margin-bottom:12px"><span style="font-size:14px;font-weight:600">💼 业务信息</span></div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="所属产业领域">{{ getFieldLabel(customerDetailData.industrialField) }}</el-descriptions-item>
            <el-descriptions-item label="其他领域">{{ customerDetailData.industrialFieldOther || '无' }}</el-descriptions-item>
            <el-descriptions-item label="主营业务" :span="2">{{ customerDetailData.mainBusiness || '-' }}</el-descriptions-item>
            <el-descriptions-item label="企业实力">{{ strengthsLabel(customerDetailData.companyStrengths) }}</el-descriptions-item>
            <el-descriptions-item label="其他实力">{{ customerDetailData.companyStrengthsOther || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="知识产权" name="c-ip">
          <div style="margin-bottom:12px"><span style="font-size:14px;font-weight:600">📜 知识产权</span></div>
          <div class="detail-stats-row">
            <div class="detail-stat-card"><div class="stat-label">📜 发明专利</div><div class="stat-val">{{ ipStats.invention }}</div><div class="stat-sub">专利总数</div></div>
            <div class="detail-stat-card"><div class="stat-label">🔧 实用新型</div><div class="stat-val">{{ ipStats.utility }}</div><div class="stat-sub">专利总数</div></div>
            <div class="detail-stat-card"><div class="stat-label">🎨 外观设计</div><div class="stat-val">{{ ipStats.design }}</div><div class="stat-sub">专利总数</div></div>
            <div class="detail-stat-card"><div class="stat-label">©️ 商标+著作权</div><div class="stat-val" style="color:#007AFF">{{ ipStats.tmCopyright }}</div><div class="stat-sub">商标 {{ ipStats.trademark }} + 软著 {{ ipStats.copyright }}</div></div>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;flex-wrap:wrap;gap:8px">
            <el-radio-group v-model="ipActiveType" size="small">
              <el-radio-button value="patent_invention">发明专利 ({{ ipData.patent_invention.length }})</el-radio-button>
              <el-radio-button value="patent_utility">实用新型 ({{ ipData.patent_utility.length }})</el-radio-button>
              <el-radio-button value="patent_design">外观设计 ({{ ipData.patent_design.length }})</el-radio-button>
              <el-radio-button value="trademark">商标 ({{ ipData.trademark.length }})</el-radio-button>
              <el-radio-button value="software_copyright">软著 ({{ ipData.software_copyright.length }})</el-radio-button>
              <el-radio-button value="ic_layout">布图 ({{ ipData.ic_layout.length }})</el-radio-button>
            </el-radio-group>
            <el-button size="small" type="primary" @click="openIPAddDialog">➕ 新增IP</el-button>
          </div>
          <el-table :data="filteredIPData" border size="small" max-height="360">
            <el-table-column type="index" label="序号" width="50" />
            <el-table-column prop="name" label="名称/标题" min-width="180" :show-overflow-tooltip="true" />
            <el-table-column prop="applyNo" label="申请号" width="170" class-name="mono-font" />
            <el-table-column prop="applyDate" label="申请日" width="100" />
            <el-table-column prop="grantDate" label="授权日" width="100"><template #default="scope">{{ scope.row.grantDate || '-' }}</template></el-table-column>
            <el-table-column prop="owner" label="所有人" width="150" :show-overflow-tooltip="true" />
            <el-table-column label="状态" width="80"><template #default="scope"><el-tag :type="scope.row.status === 'granted' ? 'success' : 'warning'" size="small">{{ scope.row.status === 'granted' ? '已授权' : '审核中' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope"><el-button type="primary" link size="small" @click="viewIPDetail(scope.row)">查看</el-button><el-button type="primary" link size="small" @click="editIPRecord(scope.row)">编辑</el-button><el-button type="danger" link size="small" @click="deleteIPRecord(scope.row)">删除</el-button></template>
            </el-table-column>
          </el-table>
          <div class="detail-pagination"><span>共 {{ filteredIPAllData.length }} 条</span><el-pagination v-model:current-page="ipPageNum" :page-size="ipPageSize" :total="filteredIPAllData.length" layout="prev, pager, next" small /></div>
        </el-tab-pane>
        <el-tab-pane label="财务状况" name="c-finance">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <span style="font-size:14px;font-weight:600">📊 财务状况</span>
            <el-select v-model="financeYear" size="small" style="width:160px"><el-option v-for="y in financeYears" :key="y" :label="y + ' 年'" :value="y" /></el-select>
          </div>
          <div class="detail-stats-row">
            <div class="detail-stat-card"><div class="stat-label">📈 营业收入</div><div class="stat-val" style="color:#007AFF">{{ currentFinance.revenue }}</div><div class="stat-sub" :style="{ color: currentFinance.yoyColor }">{{ currentFinance.yoy }}</div></div>
            <div class="detail-stat-card"><div class="stat-label">💵 净利润</div><div class="stat-val" style="color:#34C759">{{ currentFinance.profit }}</div><div class="stat-sub" style="color:#34C759">{{ currentFinance.profitTrend }}</div></div>
            <div class="detail-stat-card"><div class="stat-label">🧾 税收</div><div class="stat-val">{{ currentFinance.tax }}</div><div class="stat-sub">纳税等级 {{ currentFinance.taxLevel }}</div></div>
            <div class="detail-stat-card"><div class="stat-label">📊 资产总额</div><div class="stat-val">{{ currentFinance.assets }}</div><div class="stat-sub">净资产 {{ currentFinance.netAssets }}</div></div>
          </div>
          <div style="font-size:13px;font-weight:600;margin:12px 0 8px;padding-bottom:6px;border-bottom:1px solid #ebeef5">{{ financeYear }} 年详细数据</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="营业收入"><span class="value-mono">{{ currentFinance.revenue }}</span></el-descriptions-item>
            <el-descriptions-item label="上年度税收">{{ currentFinance.taxLevel }}</el-descriptions-item>
            <el-descriptions-item label="净利润"><span class="value-mono">{{ currentFinance.profit }}</span></el-descriptions-item>
            <el-descriptions-item label="毛利率"><span class="value-mono">{{ currentFinance.grossMargin }}</span></el-descriptions-item>
            <el-descriptions-item label="净利率"><span class="value-mono">{{ currentFinance.netMargin }}</span></el-descriptions-item>
            <el-descriptions-item label="营收同比"><span :style="{ color: currentFinance.yoyColor }">{{ currentFinance.yoy }}</span></el-descriptions-item>
            <el-descriptions-item label="投资总金额"><span class="value-mono">{{ currentFinance.investTotal }}</span></el-descriptions-item>
            <el-descriptions-item label="融资总金额"><span class="value-mono">{{ currentFinance.financeTotal }}</span></el-descriptions-item>
            <el-descriptions-item label="资产总额"><span class="value-mono">{{ currentFinance.assets }}</span></el-descriptions-item>
            <el-descriptions-item label="净资产"><span class="value-mono">{{ currentFinance.netAssets }}</span></el-descriptions-item>
            <el-descriptions-item label="负债率"><span class="value-mono">{{ currentFinance.debtRatio }}</span></el-descriptions-item>
            <el-descriptions-item label="信用评级">{{ currentFinance.credit }}</el-descriptions-item>
            <el-descriptions-item label="财务备注" :span="2">{{ currentFinance.remark }}</el-descriptions-item>
          </el-descriptions>
          <div style="font-size:13px;font-weight:600;margin:16px 0 8px;padding-bottom:6px;border-bottom:1px solid #ebeef5">📊 历年财务对比（2015-2024）</div>
          <el-table :data="financeHistoryData" border size="small" max-height="300">
            <el-table-column prop="year" label="年份" width="70"><template #default="scope"><el-button type="primary" link size="small" @click="financeYear = scope.row.year">{{ scope.row.year }}</el-button></template></el-table-column>
            <el-table-column prop="revenue" label="营业收入" width="120" class-name="value-mono" />
            <el-table-column prop="profit" label="净利润" width="100" class-name="value-mono" />
            <el-table-column prop="tax" label="税收" width="100" class-name="value-mono" />
            <el-table-column prop="assets" label="资产总额" width="110" class-name="value-mono" />
            <el-table-column prop="grossMargin" label="毛利率" width="80" class-name="value-mono" />
            <el-table-column label="营收同比" width="90"><template #default="scope"><span :style="{ color: scope.row.yoyColor }">{{ scope.row.yoy }}</span></template></el-table-column>
            <el-table-column prop="source" label="数据来源" width="80" />
            <el-table-column label="操作" width="60"><template #default="scope"><el-button type="primary" link size="small" @click="financeYear = scope.row.year">查看</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="企业需求" name="c-needs">
          <div style="margin-bottom:12px"><span style="font-size:14px;font-weight:600">📋 企业需求</span></div>
          <div class="needs-meta-bar"><span>最近更新日期：<strong>{{ needsMeta.updateDate }}</strong></span><span>更新人：<strong>{{ needsMeta.updatePerson }}</strong></span><span>数据来源：<strong>{{ needsMeta.dataSource }}</strong></span></div>
          <div style="font-size:13px;font-weight:600;margin:12px 0 8px;padding-bottom:6px;border-bottom:1px solid #ebeef5">📋 企业需求</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="面临困难">{{ difficultiesLabel(customerDetailData.companyDifficulties) }}</el-descriptions-item>
            <el-descriptions-item label="其他困难">{{ customerDetailData.companyDifficultiesOther || '-' }}</el-descriptions-item>
            <el-descriptions-item label="配套服务需求">{{ supportingLabel(customerDetailData.supportingServices) }}</el-descriptions-item>
            <el-descriptions-item label="科技咨询服务需求">{{ technicalLabel(customerDetailData.technicalConsultingServices) }}</el-descriptions-item>
            <el-descriptions-item label="管理服务需求">{{ customerDetailData.managementServices || '-' }}</el-descriptions-item>
            <el-descriptions-item label="公共技术平台服务需求">{{ customerDetailData.technologyPlatformServices || '-' }}</el-descriptions-item>
            <el-descriptions-item label="投资服务需求">{{ customerDetailData.investmentServices || '-' }}</el-descriptions-item>
            <el-descriptions-item label="对产业园建议" :span="2">{{ customerDetailData.suggest || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div style="font-size:13px;font-weight:600;margin:16px 0 8px;padding-bottom:6px;border-bottom:1px solid #ebeef5">📜 需求变更历史</div>
          <el-table :data="needsHistory" border size="small" max-height="200">
            <el-table-column prop="time" label="变更时间" width="160" />
            <el-table-column prop="user" label="变更人" width="130" />
            <el-table-column label="变更类型" width="90"><template #default="scope"><el-tag :type="scope.row.type === '新增' ? 'success' : scope.row.type === '更新' ? 'warning' : 'info'" size="small">{{ scope.row.type }}</el-tag></template></el-table-column>
            <el-table-column prop="content" label="变更内容" min-width="250" :show-overflow-tooltip="true" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="物理需求" name="c-physical">
          <div style="margin-bottom:12px"><span style="font-size:14px;font-weight:600">🏗️ 物理需求</span></div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="结构荷载 (kN/m²)"><span class="value-mono">{{ physicalData.loadCapacity }}</span></el-descriptions-item>
            <el-descriptions-item label="楼层高度 (m)"><span class="value-mono">{{ physicalData.floorHeight }}</span></el-descriptions-item>
            <el-descriptions-item label="电容量 (kW)"><span class="value-mono">{{ physicalData.powerCapacity }}</span></el-descriptions-item>
            <el-descriptions-item label="给排水">{{ physicalData.waterDrainage }}</el-descriptions-item>
            <el-descriptions-item label="新风排烟">{{ physicalData.freshAir }}</el-descriptions-item>
            <el-descriptions-item label="电梯长度 (m)"><span class="value-mono">{{ physicalData.elevatorLength }}</span></el-descriptions-item>
            <el-descriptions-item label="电梯宽度 (m)"><span class="value-mono">{{ physicalData.elevatorWidth }}</span></el-descriptions-item>
            <el-descriptions-item label="电梯高度 (m)"><span class="value-mono">{{ physicalData.elevatorHeight }}</span></el-descriptions-item>
            <el-descriptions-item label="电梯荷载 (kg)"><span class="value-mono">{{ physicalData.elevatorLoad }}</span></el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="customerDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  <!-- ========== Dialog 7: IP 新增/编辑 ========== -->
    <el-dialog v-model="ipDialogVisible" :title="ipDialogTitle" width="700px" :close-on-click-modal="false">
      <el-form :model="ipForm" ref="ipFormRef" label-width="120px" size="small">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="知识产权类型" prop="ipType"><el-select v-model="ipForm.ipType" style="width:100%"><el-option label="发明专利" value="patent_invention" /><el-option label="实用新型" value="patent_utility" /><el-option label="外观设计" value="patent_design" /><el-option label="商标" value="trademark" /><el-option label="软件著作权" value="software_copyright" /><el-option label="集成电路布图" value="ic_layout" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="名称" prop="name"><el-input v-model="ipForm.name" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="申请号" prop="applyNo"><el-input v-model="ipForm.applyNo" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="申请日"><el-date-picker v-model="ipForm.applyDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="授权日"><el-date-picker v-model="ipForm.grantDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="截止日"><el-date-picker v-model="ipForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="状态"><el-select v-model="ipForm.status" style="width:100%"><el-option label="已授权" value="granted" /><el-option label="审核中" value="pending" /><el-option label="已失效" value="expired" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="所有人"><el-input v-model="ipForm.owner" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="代理机构"><el-input v-model="ipForm.agency" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="代理人"><el-input v-model="ipForm.agent" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="分类号"><el-input v-model="ipForm.classification" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="附件上传"><div class="upload-placeholder" @click="ElMessage.info('演示模式：上传专利证书 PDF')">📎 点击上传证书 / 申请文件（支持 PDF/JPG）</div></el-form-item>
        <el-form-item label="备注"><el-input v-model="ipForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveIPRecord">确定</el-button>
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
import { getCustomerPage, createCustomer, updateCustomer, deleteCustomer as apiDeleteCustomer, type CustomerInformation } from '@/api/enterprise'
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

// ─── Mock IP Data ───
const MOCK_IP_MAP: Record<string, any> = {}
const MOCK_FINANCE_MAP: Record<string, any> = {}
const MOCK_NEEDS_MAP: Record<string, any> = {}
const MOCK_PHYSICAL_MAP: Record<string, any> = {}

function initMockCustomers() {
  // Customer 1: 华为
  const c1: any = {
    id: 'c1', enterpriseId: Number(enterpriseId.value), code: 'HX-2024-001',
    customerType: 3, area: 8500, settleAddress: '坂田基地 A 座 12-15 楼',
    name: '张伟', phone: '13900000001', email: 'zhangwei@huawei.com',
    registTime: '2024-03-15', registMoney: '364.5 亿', rentalStandard: '85',
    url: 'https://platform.local/c/HX2024-001-a3f9',
    industrialField: 1, industrialFieldOther: '', mainBusiness: '通信设备、智能手机、芯片设计与制造、云计算服务、企业级IT解决方案的研发、生产、销售',
    companyStrengths: '1,2,3,5,8', companyStrengthsOther: '',
    businessIncome: 4, lastYearTax: 5,
    totalInvestmentAmount: 28000000000, totalFinancingAmount: 15000000000,
    companyDifficulties: '', companyDifficultiesOther: '',
    supportingServices: '1,2,3,4,5', technicalConsultingServices: '1,2,5',
    managementServices: '需要办公空间扩展咨询', technologyPlatformServices: '需要EDA工具共享平台',
    investmentServices: '寻求上下游产业链投资机会',
    suggest: '希望增加更多的会议室和路演厅，便于企业开展技术交流和对外发布',
    supplyAndDrainage: '有',
    status: 1,
  }
  // Customer 2: 中兴
  const c2: any = {
    id: 'c2', enterpriseId: Number(enterpriseId.value), code: 'HX-2024-002',
    customerType: 2, area: 2300, settleAddress: '坂田基地 B 座 8 楼',
    name: '李娜', phone: '13900000002', email: 'lina@zte.com.cn',
    registTime: '2024-06-20', registMoney: '50 亿', rentalStandard: '80',
    url: 'https://platform.local/c/HX2024-002-b4e7',
    industrialField: 1, industrialFieldOther: '物联网', mainBusiness: '通信系统设备、光传输网络、5G基站、政企网络解决方案的设计与制造',
    companyStrengths: '1,2,5,7', companyStrengthsOther: '国家高新技术企业',
    businessIncome: 3, lastYearTax: 4,
    totalInvestmentAmount: 8500000000, totalFinancingAmount: 4500000000,
    companyDifficulties: '1,3', companyDifficultiesOther: '人才招聘困难',
    supportingServices: '3,4,5', technicalConsultingServices: '1,6',
    managementServices: '需要ISO认证辅导', technologyPlatformServices: '需要5G测试实验室',
    investmentServices: '',
    suggest: '希望园区能够提供更加灵活的付款方式',
    supplyAndDrainage: '有',
    status: 1,
  }
  // Customer 3: 比亚迪
  const c3: any = {
    id: 'c3', enterpriseId: Number(enterpriseId.value), code: 'HX-2023-015',
    customerType: 3, area: 12000, settleAddress: '坂田基地 C 座 1-3 楼',
    name: '王强', phone: '13900000003', email: 'wangqiang@byd.com',
    registTime: '2023-09-10', registMoney: '150 亿', rentalStandard: '82',
    url: 'https://platform.local/c/HX2023-015-c2d8',
    industrialField: 4, industrialFieldOther: '汽车零部件', mainBusiness: '新能源汽车整车制造、动力电池及储能系统、轨道交通装备的研发、生产和销售',
    companyStrengths: '1,2,5,10', companyStrengthsOther: '国内新能源车企龙头',
    businessIncome: 4, lastYearTax: 5,
    totalInvestmentAmount: 19000000000, totalFinancingAmount: 9500000000,
    companyDifficulties: '2,4', companyDifficultiesOther: '',
    supportingServices: '1,4,5', technicalConsultingServices: '3,5',
    managementServices: '需要ERP系统实施咨询', technologyPlatformServices: '需要电池检测认证平台',
    investmentServices: '寻求动力电池上游材料供应商投资',
    suggest: '需要增加大型货运电梯和装卸平台，满足设备进出需求',
    supplyAndDrainage: '有',
    status: 1,
  }
  customerList.value = [c1, c2, c3]

  // Init mock data maps
  // IP data
  MOCK_IP_MAP.c1 = {
    patent_invention: [
      { name: '一种基于5G的低功耗通信方法', applyNo: 'CN202410123456.7', applyDate: '2024-03-15', grantDate: '2024-09-20', owner: '华为技术有限公司', status: 'granted' },
      { name: '面向云端的高效数据加密算法', applyNo: 'CN202410234567.8', applyDate: '2024-05-10', grantDate: '2024-11-15', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '分布式AI模型训练资源调度方法', applyNo: 'CN202410345678.9', applyDate: '2024-07-22', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
      { name: '基于区块链的供应链溯源系统', applyNo: 'CN202410456789.0', applyDate: '2024-08-05', grantDate: '2025-02-10', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '智能终端的多模态交互方法', applyNo: 'CN202410567890.1', applyDate: '2024-10-18', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
      { name: '一种新型半导体材料及其制备方法', applyNo: 'CN202310987654.3', applyDate: '2023-11-08', grantDate: '2024-06-12', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '神经网络模型压缩方法及装置', applyNo: 'CN202311098765.4', applyDate: '2023-12-15', grantDate: '2024-08-22', owner: '华为投资控股有限公司', status: 'granted' },
    ],
    patent_utility: [
      { name: '一种可折叠电子设备支架', applyNo: 'CN202420111222.3', applyDate: '2024-02-08', grantDate: '2024-08-15', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '智能温控散热模组', applyNo: 'CN202420222333.4', applyDate: '2024-04-12', grantDate: '2024-10-20', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '便携式多功能充电设备', applyNo: 'CN202420333444.5', applyDate: '2024-06-20', grantDate: '2024-12-25', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '一种防水防尘摄像头', applyNo: 'CN202420444555.6', applyDate: '2024-08-15', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
      { name: '可调节式办公桌', applyNo: 'CN202420555666.7', applyDate: '2024-10-22', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
    ],
    patent_design: [
      { name: '手机外观设计（Mate系列）', applyNo: 'CN202430001122.3', applyDate: '2024-01-30', grantDate: '2024-07-15', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '智能手表表盘设计', applyNo: 'CN202430002233.4', applyDate: '2024-03-20', grantDate: '2024-09-25', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '平板电脑外观设计', applyNo: 'CN202430003344.5', applyDate: '2024-05-18', grantDate: '2024-11-30', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '耳机造型设计', applyNo: 'CN202430004455.6', applyDate: '2024-07-25', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
      { name: '路由器外观设计', applyNo: 'CN202430005566.7', applyDate: '2024-09-12', grantDate: '2025-03-18', owner: '华为投资控股有限公司', status: 'granted' },
    ],
    trademark: [
      { name: 'HUAWEI（中文+英文）', applyNo: 'TM20240001111', applyDate: '2024-01-10', grantDate: '2024-04-20', owner: '华为投资控股有限公司', status: 'granted' },
      { name: 'HarmonyOS 鸿蒙', applyNo: 'TM20240002222', applyDate: '2024-02-15', grantDate: '2024-06-10', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '麒麟 Kirin', applyNo: 'TM20240003333', applyDate: '2024-03-20', grantDate: '2024-08-05', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '昇腾 Ascend', applyNo: 'TM20240004444', applyDate: '2024-04-25', grantDate: '2024-10-15', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '盘古 Pangu', applyNo: 'TM20240005555', applyDate: '2024-06-12', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
    ],
    software_copyright: [
      { name: '鸿蒙操作系统 V4.0', applyNo: '2024SR0123456', applyDate: '2024-01-15', grantDate: '2024-04-20', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '方舟编译器 V3.0', applyNo: '2024SR0234567', applyDate: '2024-03-10', grantDate: '2024-06-25', owner: '华为投资控股有限公司', status: 'granted' },
      { name: 'GaussDB 数据库 V2.0', applyNo: '2024SR0345678', applyDate: '2024-05-22', grantDate: '2024-08-30', owner: '华为投资控股有限公司', status: 'granted' },
      { name: 'MindSpore AI 框架 V2.0', applyNo: '2024SR0456789', applyDate: '2024-07-18', grantDate: '2024-10-22', owner: '华为投资控股有限公司', status: 'granted' },
      { name: '云桌面 Workspace V5.0', applyNo: '2024SR0567890', applyDate: '2024-09-25', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
      { name: '智慧园区运营平台 V1.0', applyNo: '2024SR0678901', applyDate: '2024-11-12', grantDate: '', owner: '华为投资控股有限公司', status: 'pending' },
    ],
    ic_layout: [
      { name: '海思麒麟9000S芯片布图', applyNo: 'CN202400001', applyDate: '2024-02-10', grantDate: '2024-08-20', owner: '海思半导体', status: 'granted' },
      { name: '鲲鹏920服务器芯片布图', applyNo: 'CN202400002', applyDate: '2024-04-15', grantDate: '2024-10-25', owner: '海思半导体', status: 'granted' },
      { name: '昇腾910 AI芯片布图', applyNo: 'CN202400003', applyDate: '2024-06-20', grantDate: '', owner: '海思半导体', status: 'pending' },
      { name: '巴龙5000基带芯片布图', applyNo: 'CN202400004', applyDate: '2024-08-08', grantDate: '2025-02-15', owner: '海思半导体', status: 'granted' },
    ],
  }
  MOCK_IP_MAP.c2 = {
    patent_invention: [
      { name: '电源管理芯片的低功耗设计方法', applyNo: 'CN202410654321.1', applyDate: '2024-01-20', grantDate: '2024-07-15', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '5G基站射频信号处理方法', applyNo: 'CN202410765432.2', applyDate: '2024-03-10', grantDate: '2024-09-28', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '光传输网络故障自愈算法', applyNo: 'CN202410876543.3', applyDate: '2024-06-05', grantDate: '', owner: '中兴通讯股份有限公司', status: 'pending' },
      { name: '边缘计算节点弹性扩展方法', applyNo: 'CN202310654321.4', applyDate: '2023-10-12', grantDate: '2024-05-20', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '基于AI的网络流量预测方法', applyNo: 'CN202311765432.5', applyDate: '2023-12-01', grantDate: '2024-08-10', owner: '中兴通讯股份有限公司', status: 'granted' },
    ],
    patent_utility: [
      { name: '一种防尘防水基站设备箱体', applyNo: 'CN202420666777.1', applyDate: '2024-03-15', grantDate: '2024-09-20', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '光纤连接器快速安装装置', applyNo: 'CN202420777888.2', applyDate: '2024-05-22', grantDate: '2024-11-30', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '机房温控节能装置', applyNo: 'CN202420888999.3', applyDate: '2024-08-10', grantDate: '', owner: '中兴通讯股份有限公司', status: 'pending' },
    ],
    patent_design: [
      { name: '基站天线外观设计', applyNo: 'CN202430006677.1', applyDate: '2024-02-20', grantDate: '2024-08-25', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '路由器产品外观', applyNo: 'CN202430007788.2', applyDate: '2024-06-15', grantDate: '2024-12-20', owner: '中兴通讯股份有限公司', status: 'granted' },
    ],
    trademark: [
      { name: 'ZTE 中兴', applyNo: 'TM20240006666', applyDate: '2024-01-05', grantDate: '2024-04-15', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '中兴天机 Axon', applyNo: 'TM20240007777', applyDate: '2024-03-18', grantDate: '2024-07-22', owner: '中兴通讯股份有限公司', status: 'granted' },
    ],
    software_copyright: [
      { name: '中兴网管系统 V5.0', applyNo: '2024SR0789012', applyDate: '2024-02-20', grantDate: '2024-06-15', owner: '中兴通讯股份有限公司', status: 'granted' },
      { name: '5G核心网软件 V3.0', applyNo: '2024SR0890123', applyDate: '2024-04-25', grantDate: '2024-08-30', owner: '中兴通讯股份有限公司', status: 'granted' },
    ],
    ic_layout: [],
  }
  MOCK_IP_MAP.c3 = {
    patent_invention: [
      { name: '新能源汽车电池热管理方法', applyNo: 'CN202410987654.0', applyDate: '2024-02-28', grantDate: '2024-10-15', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '智能驾驶环境感知融合算法', applyNo: 'CN202411098765.1', applyDate: '2024-04-20', grantDate: '2024-12-01', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '全固态电池制备工艺', applyNo: 'CN202410098765.2', applyDate: '2024-07-15', grantDate: '', owner: '比亚迪股份有限公司', status: 'pending' },
    ],
    patent_utility: [
      { name: '刀片电池模组固定装置', applyNo: 'CN202420999000.1', applyDate: '2024-01-15', grantDate: '2024-07-20', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '车载充电机散热结构', applyNo: 'CN202421000111.2', applyDate: '2024-04-10', grantDate: '2024-10-25', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '电动汽车底盘保护装置', applyNo: 'CN202421111222.3', applyDate: '2024-07-05', grantDate: '', owner: '比亚迪股份有限公司', status: 'pending' },
      { name: '智能座舱旋转机构', applyNo: 'CN202421222333.4', applyDate: '2024-09-18', grantDate: '2025-03-25', owner: '比亚迪股份有限公司', status: 'granted' },
    ],
    patent_design: [
      { name: '汉EV前脸造型设计', applyNo: 'CN202430008899.1', applyDate: '2024-01-10', grantDate: '2024-07-15', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '海豹车型轮毂设计', applyNo: 'CN202430009900.2', applyDate: '2024-05-20', grantDate: '2024-11-25', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '仰望U8内饰设计', applyNo: 'CN202430010011.3', applyDate: '2024-08-15', grantDate: '', owner: '比亚迪股份有限公司', status: 'pending' },
    ],
    trademark: [
      { name: '比亚迪 BYD', applyNo: 'TM20240008888', applyDate: '2024-02-01', grantDate: '2024-05-20', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '刀片电池 Blade Battery', applyNo: 'TM20240009999', applyDate: '2024-04-12', grantDate: '2024-08-30', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: 'DM-i 超级混动', applyNo: 'TM20240010000', applyDate: '2024-06-20', grantDate: '2024-12-05', owner: '比亚迪股份有限公司', status: 'granted' },
    ],
    software_copyright: [
      { name: 'DiLink 智能座舱系统 V4.0', applyNo: '2024SR0901234', applyDate: '2024-03-15', grantDate: '2024-07-20', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: 'DiPilot 智能驾驶系统 V2.0', applyNo: '2024SR1012345', applyDate: '2024-06-01', grantDate: '2024-10-15', owner: '比亚迪股份有限公司', status: 'granted' },
      { name: '云辇系统控制软件 V1.0', applyNo: '2024SR1123456', applyDate: '2024-08-20', grantDate: '', owner: '比亚迪股份有限公司', status: 'pending' },
    ],
    ic_layout: [
      { name: '车规级IGBT芯片布图', applyNo: 'CN202400005', applyDate: '2024-03-01', grantDate: '2024-09-10', owner: '比亚迪半导体', status: 'granted' },
      { name: 'SiC MOSFET芯片布图', applyNo: 'CN202400006', applyDate: '2024-05-20', grantDate: '', owner: '比亚迪半导体', status: 'pending' },
    ],
  }
  // Finance data
  MOCK_FINANCE_MAP.c1 = {}
  MOCK_FINANCE_MAP.c2 = {}
  MOCK_FINANCE_MAP.c3 = {}
  const years = ['2024','2023','2022','2021','2020','2019','2018','2017','2016','2015']
  const f1data: Record<string, any> = {
    '2024': { revenue: '¥ 7,042 亿', profit: '¥ 626 亿', tax: '¥ 350 亿', assets: '¥ 1,250 亿', grossMargin: '42.5%', netMargin: '8.9%', yoy: '↑ 9.6%', profitTrend: '↑ 12.3%', taxLevel: 'A级（5）', netAssets: '¥ 680 亿', investTotal: '¥ 280 亿', financeTotal: '¥ 150 亿', debtRatio: '45.6%', credit: 'AAA', remark: '2024年营收稳步增长', yoyColor: '#34C759', source: '年报' },
    '2023': { revenue: '¥ 6,422 亿', profit: '¥ 558 亿', tax: '¥ 320 亿', assets: '¥ 1,150 亿', grossMargin: '43.2%', netMargin: '8.7%', yoy: '↑ 8.5%', profitTrend: '↑ 8.3%', taxLevel: 'A级（5）', netAssets: '¥ 620 亿', investTotal: '¥ 240 亿', financeTotal: '¥ 130 亿', debtRatio: '46.1%', credit: 'AAA', remark: '2023年企业全面回归增长轨道', yoyColor: '#34C759', source: '年报' },
    '2022': { revenue: '¥ 5,920 亿', profit: '¥ 515 亿', tax: '¥ 295 亿', assets: '¥ 1,050 亿', grossMargin: '43.5%', netMargin: '8.7%', yoy: '↑ 5.2%', profitTrend: '↑ 5.8%', taxLevel: 'A级（5）', netAssets: '¥ 565 亿', investTotal: '¥ 210 亿', financeTotal: '¥ 100 亿', debtRatio: '46.2%', credit: 'AAA', remark: '2022年顶住外部压力', yoyColor: '#34C759', source: '年报' },
    '2021': { revenue: '¥ 5,628 亿', profit: '¥ 487 亿', tax: '¥ 268 亿', assets: '¥ 980 亿', grossMargin: '44.1%', netMargin: '8.7%', yoy: '↓ 28.6%', profitTrend: '↓ 24.6%', taxLevel: 'A级（5）', netAssets: '¥ 530 亿', investTotal: '¥ 180 亿', financeTotal: '¥ 80 亿', debtRatio: '45.9%', credit: 'AAA', remark: '2021年受外部环境影响营收下降', yoyColor: '#FF3B30', source: '年报' },
    '2020': { revenue: '¥ 7,888 亿', profit: '¥ 646 亿', tax: '¥ 360 亿', assets: '¥ 920 亿', grossMargin: '36.3%', netMargin: '8.2%', yoy: '↑ 3.8%', profitTrend: '↑ 3.0%', taxLevel: 'A级（5）', netAssets: '¥ 500 亿', investTotal: '¥ 165 亿', financeTotal: '¥ 60 亿', debtRatio: '45.7%', credit: 'AAA', remark: '2020年消费者业务受全球疫情影响', yoyColor: '#34C759', source: '年报' },
  }
  const f2data: Record<string, any> = {
    '2024': { revenue: '¥ 1,242 亿', profit: '¥ 93.6 亿', tax: '¥ 62 亿', assets: '¥ 420 亿', grossMargin: '38.5%', netMargin: '7.5%', yoy: '↑ 8.2%', profitTrend: '↑ 10.5%', taxLevel: 'A级（4）', netAssets: '¥ 185 亿', investTotal: '¥ 85 亿', financeTotal: '¥ 45 亿', debtRatio: '56.0%', credit: 'AA+', remark: '政企网和运营商业务拉动增长', yoyColor: '#34C759', source: '年报' },
    '2023': { revenue: '¥ 1,148 亿', profit: '¥ 84.7 亿', tax: '¥ 55 亿', assets: '¥ 390 亿', grossMargin: '39.2%', netMargin: '7.4%', yoy: '↑ 3.5%', profitTrend: '↑ 2.8%', taxLevel: 'A级（4）', netAssets: '¥ 168 亿', investTotal: '¥ 72 亿', financeTotal: '¥ 38 亿', debtRatio: '56.9%', credit: 'AA+', remark: '2023年运营商网络业务稳步增长', yoyColor: '#34C759', source: '年报' },
    '2022': { revenue: '¥ 1,109 亿', profit: '¥ 82.4 亿', tax: '¥ 50 亿', assets: '¥ 365 亿', grossMargin: '38.8%', netMargin: '7.4%', yoy: '↑ 6.8%', profitTrend: null, taxLevel: 'A级（4）', netAssets: '¥ 155 亿', investTotal: '¥ 60 亿', financeTotal: '¥ 30 亿', debtRatio: '57.5%', credit: 'AA+', remark: '2022年5G建设持续推进', yoyColor: '#34C759', source: '年报' },
  }
  const f3data: Record<string, any> = {
    '2024': { revenue: '¥ 7,860 亿', profit: '¥ 402 亿', tax: '¥ 225 亿', assets: '¥ 860 亿', grossMargin: '20.5%', netMargin: '5.1%', yoy: '↑ 42.6%', profitTrend: '↑ 80.5%', taxLevel: 'A级（5）', netAssets: '¥ 320 亿', investTotal: '¥ 190 亿', financeTotal: '¥ 95 亿', debtRatio: '62.8%', credit: 'AAA', remark: '新能源汽车销量突破300万辆', yoyColor: '#34C759', source: '年报' },
    '2023': { revenue: '¥ 5,512 亿', profit: '¥ 223 亿', tax: '¥ 148 亿', assets: '¥ 680 亿', grossMargin: '19.8%', netMargin: '4.0%', yoy: '↑ 28.4%', profitTrend: '↑ 42.7%', taxLevel: 'A级（5）', netAssets: '¥ 268 亿', investTotal: '¥ 155 亿', financeTotal: '¥ 72 亿', debtRatio: '60.6%', credit: 'AAA', remark: '2023年新能源汽车销量爆发式增长', yoyColor: '#34C759', source: '年报' },
  }
  years.forEach(y => { if (f1data[y]) MOCK_FINANCE_MAP.c1[y] = f1data[y] })
  years.forEach(y => { if (f2data[y]) MOCK_FINANCE_MAP.c2[y] = f2data[y] })
  years.forEach(y => { if (f3data[y]) MOCK_FINANCE_MAP.c3[y] = f3data[y] })
  // Needs data
  MOCK_NEEDS_MAP.c1 = { updateDate: '2026-06-15 14:30', updatePerson: '张伟（招商经理）', dataSource: '客户实地访谈 + 问卷',
    history: [{ time: '2026-06-15 14:30', user: '张伟（招商经理）', type: '更新', content: '调整配套服务需求：增加"室内环境"' }, { time: '2026-03-20 10:15', user: '王强（招商总监）', type: '新增', content: '新增对产业园建议' }, { time: '2025-12-08 16:45', user: '张伟（招商经理）', type: '更新', content: '配套服务需求从5项更新为6项' }, { time: '2025-08-12 09:20', user: '李娜（招商专员）', type: '初审', content: '初次采集企业需求信息' }] }
  MOCK_NEEDS_MAP.c2 = { updateDate: '2026-05-28 11:20', updatePerson: '李娜（招商专员）', dataSource: '电话访谈',
    history: [{ time: '2026-05-28 11:20', user: '李娜（招商专员）', type: '更新', content: '更新困难信息：新增"融资渠道狭窄"' }, { time: '2026-02-15 15:30', user: '张伟（招商经理）', type: '新增', content: '新增科技咨询服务需求' }, { time: '2025-10-10 14:00', user: '李娜（招商专员）', type: '初审', content: '初次采集企业需求信息' }] }
  MOCK_NEEDS_MAP.c3 = { updateDate: '2026-06-20 09:45', updatePerson: '王强（招商总监）', dataSource: '客户实地访谈',
    history: [{ time: '2026-06-20 09:45', user: '王强（招商总监）', type: '更新', content: '更新物理需求' }, { time: '2026-04-10 16:20', user: '张伟（招商经理）', type: '新增', content: '新增管理服务需求和投资服务需求' }, { time: '2026-01-22 13:30', user: '王强（招商总监）', type: '更新', content: '更新困难信息' }, { time: '2025-11-05 10:00', user: '李娜（招商专员）', type: '初审', content: '初次采集企业需求信息' }] }
  // Physical data
  MOCK_PHYSICAL_MAP.c1 = { loadCapacity: '8.0', floorHeight: '4.5', powerCapacity: '500', waterDrainage: '有', freshAir: '需要', elevatorLength: '2.0', elevatorWidth: '1.8', elevatorHeight: '2.8', elevatorLoad: '1600' }
  MOCK_PHYSICAL_MAP.c2 = { loadCapacity: '5.0', floorHeight: '3.6', powerCapacity: '200', waterDrainage: '有', freshAir: '不需要', elevatorLength: '1.8', elevatorWidth: '1.6', elevatorHeight: '2.4', elevatorLoad: '1000' }
  MOCK_PHYSICAL_MAP.c3 = { loadCapacity: '10.0', floorHeight: '5.2', powerCapacity: '800', waterDrainage: '有', freshAir: '需要', elevatorLength: '2.4', elevatorWidth: '2.0', elevatorHeight: '3.0', elevatorLoad: '2000' }
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
const customerEditTab = ref('c-base')
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

// Edit form computed checkbox groups
const strengthsListEdit = computed({
  get: () => customerForm.companyStrengths ? String(customerForm.companyStrengths).split(',').map(Number).filter((n: number) => !isNaN(n)) : [],
  set: (v: number[]) => { customerForm.companyStrengths = v.join(',') }
})
const difficultiesListEdit = computed({
  get: () => customerForm.companyDifficulties ? String(customerForm.companyDifficulties).split(',').map(Number).filter((n: number) => !isNaN(n)) : [],
  set: (v: number[]) => { customerForm.companyDifficulties = v.join(',') }
})
const supportingListEdit = computed({
  get: () => customerForm.supportingServices ? String(customerForm.supportingServices).split(',').map(Number).filter((n: number) => !isNaN(n)) : [],
  set: (v: number[]) => { customerForm.supportingServices = v.join(',') }
})
const technicalListEdit = computed({
  get: () => customerForm.technicalConsultingServices ? String(customerForm.technicalConsultingServices).split(',').map(Number).filter((n: number) => !isNaN(n)) : [],
  set: (v: number[]) => { customerForm.technicalConsultingServices = v.join(',') }
})

function openCustomerDialog() {
  customerEditingId.value = null
  customerDialogTitle.value = '新增客户'
  resetCustomerForm()
  customerDialogVisible.value = true
}

function editCustomer(row: CustomerInformation) {
  customerEditingId.value = row.id || null
  customerDialogTitle.value = `编辑客户：${row.code}`
  const src: any = row
  Object.assign(customerForm, {
    code: src.code || '', customerType: src.customerType ?? 3,
    area: src.area ?? 0, settleAddress: src.settleAddress || '',
    name: src.name || '', phone: src.phone || '',
    email: src.email || '', registTime: src.registTime || '',
    registMoney: src.registMoney || '', rentalStandard: src.rentalStandard || '',
    industrialField: src.industrialField, status: src.status ?? 1,
    url: src.url || '', industrialFieldOther: src.industrialFieldOther || '',
    mainBusiness: src.mainBusiness || '', companyStrengths: src.companyStrengths || '',
    companyStrengthsOther: src.companyStrengthsOther || '',
    validIntellectualProperty: src.validIntellectualProperty ?? 0,
    inventionPatents: src.inventionPatents ?? 0, utilityModelPatent: src.utilityModelPatent ?? 0,
    industrialDesignPatents: src.industrialDesignPatents ?? 0,
    trademark: src.trademark ?? 0, softwareCopyright: src.softwareCopyright ?? 0,
    newPlantVariety: src.newPlantVariety ?? 0, integratedCircuitLayout: src.integratedCircuitLayout ?? 0,
    purchaseForeignPatents: src.purchaseForeignPatents ?? 0, otherPatents: src.otherPatents ?? 0,
    businessIncome: src.businessIncome, lastYearTax: src.lastYearTax,
    totalInvestmentAmount: src.totalInvestmentAmount, totalFinancingAmount: src.totalFinancingAmount,
    companyDifficulties: src.companyDifficulties || '', companyDifficultiesOther: src.companyDifficultiesOther || '',
    supportingServices: src.supportingServices || '', technicalConsultingServices: src.technicalConsultingServices || '',
    managementServices: src.managementServices || '', managementServicesOther: src.managementServicesOther || '',
    technologyPlatformServices: src.technologyPlatformServices || '', technologyPlatformServicesOther: src.technologyPlatformServicesOther || '',
    investmentServices: src.investmentServices || '', investmentServicesOther: src.investmentServicesOther || '',
    suggest: src.suggest || '',
    supplyAndDrainage: src.supplyAndDrainage || '',
    structuralLoadStr: src.structuralLoadStr || '', floorHeightStr: src.floorHeightStr || '',
    capacitanceStr: src.capacitanceStr || '', freshAirSmokeExhaustStr: src.freshAirSmokeExhaustStr || '',
    elevatorLengthStr: src.elevatorLengthStr || '', elevatorWidthStr: src.elevatorWidthStr || '',
    elevatorHeightStr: src.elevatorHeightStr || '', elevatorLoadStr: src.elevatorLoadStr || '',
  })
  customerEditTab.value = 'c-base'
  customerDialogVisible.value = true
}

function resetCustomerForm() {
  Object.assign(customerForm, {
    code: '', customerType: 3, area: 0, settleAddress: '',
    name: '', phone: '', email: '', registTime: '', registMoney: '',
    rentalStandard: '', industrialField: undefined, status: 1,
    url: '', industrialFieldOther: '', mainBusiness: '', companyStrengths: '', companyStrengthsOther: '',
    validIntellectualProperty: 0, inventionPatents: 0, utilityModelPatent: 0,
    industrialDesignPatents: 0, trademark: 0, softwareCopyright: 0,
    newPlantVariety: 0, integratedCircuitLayout: 0, purchaseForeignPatents: 0, otherPatents: 0,
    businessIncome: undefined, lastYearTax: undefined,
    totalInvestmentAmount: undefined, totalFinancingAmount: undefined,
    companyDifficulties: '', companyDifficultiesOther: '',
    supportingServices: '', technicalConsultingServices: '',
    managementServices: '', managementServicesOther: '',
    technologyPlatformServices: '', technologyPlatformServicesOther: '',
    investmentServices: '', investmentServicesOther: '', suggest: '',
    supplyAndDrainage: '',
    structuralLoadStr: '', floorHeightStr: '', capacitanceStr: '',
    freshAirSmokeExhaustStr: '', elevatorLengthStr: '', elevatorWidthStr: '',
    elevatorHeightStr: '', elevatorLoadStr: '',
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

// ─── Customer Detail View (6 Tab) ───
const customerDetailVisible = ref(false)
const customerDetailData = ref<CustomerInformation | null>(null)
const customerDetailTab = ref('c-base')

function viewCustomerDetail(row: CustomerInformation) {
  customerDetailData.value = row
  customerDetailVisible.value = true
  customerDetailTab.value = 'c-base'
  // Reset IP state for this customer
  const custId = row.id || 'c1'
  ipData.value = MOCK_IP_MAP[custId] || MOCK_IP_MAP.c1 || emptyIPData()
  ipActiveType.value = 'patent_invention'
  ipPageNum.value = 1
  updateIPStats()
  // Reset finance
  financeYear.value = '2024'
  // Reset needs
  loadNeedsForCustomer(custId)
  // Reset physical
  physicalData.value = MOCK_PHYSICAL_MAP[custId] || MOCK_PHYSICAL_MAP.c1 || emptyPhysicalData()
}

function resetCustomerDetailTab() {
  customerDetailTab.value = 'c-base'
}

function editFromDetail() {
  if (customerDetailData.value) {
    editCustomer(customerDetailData.value)
  }
}

// ─── IP State ───
const ipData = ref<any>({ patent_invention: [], patent_utility: [], patent_design: [], trademark: [], software_copyright: [], ic_layout: [] })
const ipActiveType = ref('patent_invention')
const ipPageNum = ref(1)
const ipPageSize = ref(5)
const ipStats = ref({ invention: 0, utility: 0, design: 0, trademark: 0, copyright: 0, tmCopyright: 0 })

function emptyIPData() {
  return { patent_invention: [], patent_utility: [], patent_design: [], trademark: [], software_copyright: [], ic_layout: [] }
}

function emptyPhysicalData() {
  return { loadCapacity: '-', floorHeight: '-', powerCapacity: '-', waterDrainage: '-', freshAir: '-', elevatorLength: '-', elevatorWidth: '-', elevatorHeight: '-', elevatorLoad: '-' }
}

function updateIPStats() {
  const d = ipData.value
  const inv = d.patent_invention?.length || 0
  const utl = d.patent_utility?.length || 0
  const des = d.patent_design?.length || 0
  const tm = d.trademark?.length || 0
  const cr = d.software_copyright?.length || 0
  ipStats.value = { invention: inv, utility: utl, design: des, trademark: tm, copyright: cr, tmCopyright: tm + cr }
}

const filteredIPAllData = computed(() => {
  return ipData.value[ipActiveType.value] || []
})

const filteredIPData = computed(() => {
  const all = filteredIPAllData.value
  const start = (ipPageNum.value - 1) * ipPageSize.value
  return all.slice(start, start + ipPageSize.value)
})

// ─── IP CRUD ───
const ipDialogVisible = ref(false)
const ipDialogTitle = ref('新增知识产权')
const ipFormRef = ref()
const ipForm = reactive<any>({ ipType: 'patent_invention', name: '', applyNo: '', applyDate: '', grantDate: '', expireDate: '', status: 'granted', owner: '', agency: '', agent: '', classification: '', remark: '' })
const ipEditingIndex = ref<number | null>(null)

function openIPAddDialog() {
  ipEditingIndex.value = null
  ipDialogTitle.value = '➕ 新增知识产权'
  Object.assign(ipForm, { ipType: ipActiveType.value, name: '', applyNo: '', applyDate: '', grantDate: '', expireDate: '', status: 'granted', owner: '', agency: '', agent: '', classification: '', remark: '' })
  ipDialogVisible.value = true
}

function editIPRecord(row: any) {
  const all = ipData.value[ipActiveType.value] || []
  const idx = all.indexOf(row)
  if (idx < 0) return
  ipEditingIndex.value = idx
  ipDialogTitle.value = '✏️ 编辑知识产权'
  Object.assign(ipForm, {
    ipType: ipActiveType.value,
    name: row.name || '',
    applyNo: row.applyNo || '',
    applyDate: row.applyDate || '',
    grantDate: row.grantDate || '',
    expireDate: row.expireDate || '',
    status: row.status || 'granted',
    owner: row.owner || '',
    agency: row.agency || '',
    agent: row.agent || '',
    classification: row.classification || '',
    remark: row.remark || '',
  })
  ipDialogVisible.value = true
}

function saveIPRecord() {
  if (!ipForm.name) { ElMessage.warning('请输入名称'); return }
  if (!ipForm.applyNo) { ElMessage.warning('请输入申请号'); return }
  const entry = {
    name: ipForm.name, applyNo: ipForm.applyNo,
    applyDate: ipForm.applyDate || '', grantDate: ipForm.grantDate || '',
    expireDate: ipForm.expireDate || '', status: ipForm.status || 'pending',
    owner: ipForm.owner || '', agency: ipForm.agency || '',
    agent: ipForm.agent || '', classification: ipForm.classification || '',
    remark: ipForm.remark || '',
  }
  const type = ipForm.ipType || ipActiveType.value
  if (!ipData.value[type]) ipData.value[type] = []
  if (ipEditingIndex.value !== null) {
    ipData.value[type][ipEditingIndex.value] = entry
  } else {
    ipData.value[type].push(entry)
  }
  updateIPStats()
  ipDialogVisible.value = false
  ElMessage.success('知识产权保存成功')
}

function deleteIPRecord(row: any) {
  ElMessageBox.confirm('确定要删除该知识产权记录吗？', '提示', { type: 'warning' }).then(() => {
    const all = ipData.value[ipActiveType.value] || []
    const idx = all.indexOf(row)
    if (idx >= 0) {
      all.splice(idx, 1)
      if (all.length === 0 && ipPageNum.value > 1) ipPageNum.value--
      updateIPStats()
      ElMessage.success('已删除')
    }
  }).catch(() => {})
}

function viewIPDetail(row: any) {
  ElMessageBox.alert(
    `类型：${ipTypeLabel(ipActiveType.value)}\n名称：${row.name}\n申请号：${row.applyNo}\n申请日：${row.applyDate || '-'}\n授权日：${row.grantDate || '-'}\n所有人：${row.owner}\n状态：${row.status === 'granted' ? '已授权' : '审核中'}`,
    '知识产权详情'
  )
}

function ipTypeLabel(type: string) {
  return ({ patent_invention: '发明专利', patent_utility: '实用新型', patent_design: '外观设计', trademark: '商标', software_copyright: '软件著作权', ic_layout: '集成电路布图' } as Record<string, string>)[type] || type
}

// ─── Finance State ───
const financeYear = ref('2024')
const financeYears = ['2024','2023','2022','2021','2020','2019','2018','2017','2016','2015']

const currentFinance = computed(() => {
  const custId = customerDetailData.value?.id || 'c1'
  const yearData = (MOCK_FINANCE_MAP[custId] || MOCK_FINANCE_MAP.c1 || {})[financeYear.value]
  return yearData || { revenue: '-', profit: '-', tax: '-', assets: '-', grossMargin: '-', netMargin: '-', yoy: '-', yoyColor: '#999', profitTrend: '-', taxLevel: '-', netAssets: '-', investTotal: '-', financeTotal: '-', debtRatio: '-', credit: '-', remark: '-', source: '-' }
})

const financeHistoryData = computed(() => {
  const custId = customerDetailData.value?.id || 'c1'
  const yearMap = MOCK_FINANCE_MAP[custId] || MOCK_FINANCE_MAP.c1 || {}
  return financeYears.filter(y => yearMap[y]).map(y => {
    const d = yearMap[y]
    return { year: y, revenue: d.revenue, profit: d.profit, tax: d.tax, assets: d.assets, grossMargin: d.grossMargin, yoy: d.yoy, yoyColor: d.yoyColor || '#34C759', source: d.source || '年报' }
  })
})

// ─── Needs State ───
const needsMeta = ref({ updateDate: '-', updatePerson: '-', dataSource: '-' })
const needsHistory = ref<any[]>([])

function loadNeedsForCustomer(custId: string) {
  const nd = MOCK_NEEDS_MAP[custId] || MOCK_NEEDS_MAP.c1
  needsMeta.value = { updateDate: nd?.updateDate || '-', updatePerson: nd?.updatePerson || '-', dataSource: nd?.dataSource || '-' }
  needsHistory.value = nd?.history || []
}

// ─── Physical State ───
const physicalData = ref<any>(emptyPhysicalData())

// ─── Customer Delete ───
function handleDeleteCustomer(row: CustomerInformation) {
  ElMessageBox.confirm(`确定删除客户「${row.code}」？此操作不可恢复。`, '提示', { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }).then(async () => {
    try {
      if (row.id) await apiDeleteCustomer(row.id)
      customerList.value = customerList.value.filter(c => c.id !== row.id)
      ElMessage.success('客户已删除')
    } catch {
      customerList.value = customerList.value.filter(c => c.id !== row.id)
      ElMessage.success('客户已删除（演示模式）')
    }
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

// ─── Customer Detail Label Helpers ───
const STRENGTH_LABELS: Record<number, string> = { 1: '上市公司', 2: '规上企业', 3: '独角兽', 4: '瞪羚', 5: '专精特新', 6: '高企培育', 7: '科技型中小', 8: '外资', 9: '市高级人才', 10: '其他' }
const DIFFICULTY_LABELS: Record<number, string> = { 1: '市场需求不足', 2: '资金紧张', 3: '融资渠道狭窄', 4: '人才短缺', 5: '其他' }
const SUPPORT_LABELS: Record<number, string> = { 1: '班车', 2: '儿童托管', 3: '网络通讯', 4: '团餐', 5: '公寓', 6: '室内环境' }
const TECH_LABELS: Record<number, string> = { 1: '项目申报', 2: '高企认定', 3: '成果鉴定', 4: '知识产权贯标', 5: '创新平台认定', 6: '政策推送解读' }

function strengthsLabel(s?: string) {
  if (!s) return '-'
  const nums = s.split(',').map(Number).filter(n => !isNaN(n) && n > 0)
  return nums.map(n => STRENGTH_LABELS[n] || n).join('、') || '-'
}
function difficultiesLabel(s?: string) {
  if (!s) return '无'
  const nums = s.split(',').map(Number).filter(n => !isNaN(n) && n > 0)
  return nums.map(n => DIFFICULTY_LABELS[n] || n).join('、') || '无'
}
function supportingLabel(s?: string) {
  if (!s) return '-'
  const nums = s.split(',').map(Number).filter(n => !isNaN(n) && n > 0)
  return nums.map(n => SUPPORT_LABELS[n] || n).join('、') || '-'
}
function technicalLabel(s?: string) {
  if (!s) return '-'
  const nums = s.split(',').map(Number).filter(n => !isNaN(n) && n > 0)
  return nums.map(n => TECH_LABELS[n] || n).join('、') || '-'
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

/* ─── Customer Detail 6-tab styles ─── */
.detail-stats-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 12px; margin-bottom: 16px; }
.detail-stat-card { background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; padding: 16px; }
.detail-stat-card .stat-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.detail-stat-card .stat-val { font-size: 22px; font-weight: 700; color: #303133; }
.detail-stat-card .stat-sub { font-size: 11px; color: #909399; margin-top: 4px; }
.detail-pagination { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; font-size: 13px; color: #909399; }
.needs-meta-bar { display: flex; gap: 24px; font-size: 12px; padding: 10px 16px; background: #f5f7fa; border-radius: 8px; margin-bottom: 12px; }
.needs-meta-bar strong { font-weight: 600; }
.upload-placeholder { border: 1px dashed #dcdfe6; border-radius: 8px; padding: 16px; text-align: center; cursor: pointer; color: #909399; font-size: 13px; }
.upload-placeholder:hover { border-color: #409EFF; color: #409EFF; }
</style>