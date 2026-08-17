# 智慧招商 — 设计文档 v1.5

> **版本**: v1.5 (开发可参考级：响应格式 + 错误码 + 数据字典 + 权限矩阵 + 数据库迁移 + 测试用例)
> **日期**: 2026-08-14
> **来源**: csyh 业务代码迁移 + 智慧招商 CRM 业务脑图
> **适配**: 云枢中台 park-business 模块
> **设计风格**: Apple DESIGN.md（`prototype/_assets/common.css`）

---

## 一、模块定位

智慧招商是云枢中台的 **CRM 招商管理** 模块，参考 csyh `pai-business-csyh-2.x` 业务代码（涉及 `clue_enterprise` / `contacts` / `opportunity` / `communication` / `protocol` 等实体），并融合 CRM 公海池/私海客户模型，为招商专员提供 **"公海分配 → 我的客户 → 商机转化 → 协议签订 → 合同签订"** 的全生命周期管理能力。

**核心价值**：

| 维度 | 价值 |
|---|---|
| **流转效率** | 公海池自动分配+认领，避免客户资源沉淀 |
| **跟进质量** | 结构化跟进记录（沟通/带看/拜访）形成客户画像 |
| **转化可视** | 转化漏斗+看板，量化每一步的转化效率 |
| **公平分配** | 保留期限+宽限期+保有规则，避免过度囤积 |

---

## 二、业务架构（来自脑图）

**统计**：8 大模块、31 个叶子节点、100% 覆盖。

```
智慧招商/
├── 公海池/                                                              [5 节点]
│   ├── 新增客户/                                                        [2 节点]
│   │   ├── 客户基本信息          ──→ pool.html 新增客户弹窗
│   │   └── 客户来源              ──→ pool.html 新增客户弹窗（来源字典）
│   ├── 分配                     ──→ pool.html 分配按钮+分配弹窗
│   ├── 认领                     ──→ pool.html 认领按钮
│   └── 公海日志/
│       └── 分配认领记录、回收记录等 ──→ pool-log.html
│
├── 我的客户/                                                            [6 节点]
│   ├── 添加线索/
│   │   ├── 添加联系人/          ──→ my-customer.html 添加线索弹窗+customer-detail.html 添加联系人弹窗
│   │   │   ├── 姓名             ──→ 同上表单字段
│   │   │   ├── 电话             ──→ 同上表单字段
│   │   │   ├── 职责描述         ──→ 同上表单字段
│   │   │   └── 其他信息/        ──→ 同上表单字段
│   │   │       ├── 性别
│   │   │       ├── 生日
│   │   │       ├── 部门
│   │   │       └── 职务
│   │   ├── 线索标记/            ──→ customer-detail.html 线索管理 Tab
│   │   │   ├── 标记为有效线索    ──→ 同上
│   │   │   └── 标记为无效线索    ──→ 同上
│   │   ├── 线索跟进/            ──→ customer-detail.html 线索管理 Tab+跟进记录弹窗
│   │   │   └── 跟进记录
│   │   └── 线索废弃/            ──→ customer-detail.html 线索管理 Tab
│   │       └── 废弃原因：联系不上、无意向、无法满足客户需求等
│   └── 线索管理/
│       └── 商机转化/             ──→ my-customer.html 商机转化弹窗+customer-detail.html 商机管理 Tab
│           ├── 业务类型/         ──→ 弹窗字段
│           │   ├── 租赁业务
│           │   └── 销售业务
│           └── 意向登记/         ──→ 弹窗字段
│               ├── 意向房源
│               ├── 需求信息
│               ├── 意向价格
│               └── 联系人/       ──→ 联系人字段
│                   ├── 角色
│                   ├── 立场
│                   ├── 客情关系
│                   └── 接触状态/
│                       ├── 首次接触
│                       └── 多次接触
│
├── 商机管理/                                                            [11 节点]
│   ├── 退回公海                  ──→ opportunity.html 退回按钮
│   ├── 商机联系人/
│   │   └── 更换联系人            ──→ opportunity-detail.html 商机联系人 Tab+更换联系人弹窗
│   ├── 客户意向                  ──→ opportunity-detail.html 客户意向 Tab
│   ├── 商机跟进/
│   │   ├── 沟通                  ──→ opportunity-detail.html 商机跟进 Tab+跟进弹窗(type=PHONE)
│   │   ├── 预约看房              ──→ 同上(type=BOOK)
│   │   ├── 带看                  ──→ 同上(type=SHOW)
│   │   └── 日程管理/
│   │       └── 添加客户拜访、开会等日程安排 ──→ opportunity-detail.html 日程管理 Tab+日程弹窗
│   ├── 商机终止                  ──→ opportunity-detail.html 顶部按钮
│   ├── 签订协议                  ──→ opportunity-detail.html 顶部按钮（跳转 agreement.html）
│   └── 竞争对手/
│       └── 对手信息描述、进度等情况 ──→ opportunity-detail.html 竞争对手 Tab+添加竞争对手弹窗
│
├── 协议管理/                                                            [3 节点]
│   ├── 协议延期                  ──→ agreement.html 延期按钮+agreement-detail.html 延期弹窗
│   ├── 协议终止                  ──→ agreement.html 终止按钮+agreement-detail.html 终止弹窗
│   └── 合同签订                  ──→ agreement.html 转合同按钮+agreement-detail.html 转合同弹窗
│
├── 客户回收站/                                                          [2 节点]
│   ├── 删除                      ──→ recycle.html 删除按钮
│   └── 复原/
│       └── 复原后进入客户公海     ──→ recycle.html 复原按钮（标记目标池=公海池）
│
├── 客户档案/                                                            [2 节点]
│   ├── 时间轴/
│   │   └── 生命周期、里程碑      ──→ customer-detail.html 顶部里程碑条+时间轴 Tab
│   └── 相关业务数据              ──→ customer-detail.html 相关业务数据 Tab
│
├── 招商统计/                                                            [3 节点]
│   ├── 转化漏斗                  ──→ statistics.html 转化漏斗图表
│   ├── 线索统计                  ──→ statistics.html 线索统计图表
│   └── 客户来源渠道分析           ──→ statistics.html 来源渠道图表
│
└── 基础设置/                                                            [6 节点]
    ├── 客户保留期限              ──→ settings.html 保留期限输入
    ├── 宽限期设置                ──→ settings.html 宽限期输入
    ├── 分配规则/
    │   ├── 领导分配              ──→ settings.html 领导分配开关
    │   └── 员工领取              ──→ settings.html 员工领取开关
    └── 公海保有规则/
        ├── 员工最多保有N个客户    ──→ settings.html 最大保有数输入
        └── 成交释放保有量        ──→ settings.html 成交释放开关
```

---

## 三、验收标准

### 3.1 验收维度（5 维）

| 维度 | 检查方法 | 通过标准 |
|---|---|---|
| **覆盖度** | 脑图节点 → 原型元素 1:1 映射 | 31/31 节点全部对应到具体页面元素（按钮/字段/Tab/弹窗） |
| **可导航** | 实际打开 index.html 跳转每个菜单 | 8 个侧边栏菜单项均可点击加载对应 HTML，404=0 |
| **数据源标注** | 检查每个页面的 HTML 注释 | 每个页面顶部注释含 table.field + API + 权限点 |
| **权限标注** | 检查关键按钮的 HTML 注释 | 业务级按钮含 `<!-- {{perm:business:xxx:yyy}} -->` 标注 |
| **设计规范** | 引用 common.css + 使用规范组件 | 所有页面引用 `../_assets/common.css`，无硬编码品牌色/字体 |

### 3.2 验收清单（Checklist）

```
□ A. 8 个侧边栏菜单项全部注册（index.html moduleMenus）
□ B. 8 个侧边栏 HTML 全部产出
□ C. 详情页（opportunity-detail / agreement-detail / pool-log）全部产出
□ D. 所有弹窗（新增/编辑/操作）按钮可点击触发
□ E. 所有脑图 31 个叶子节点 100% 映射
□ F. 每个页面顶部注释含：模块/页面/版本/数据源/API/权限
□ G. 所有按钮含权限标注 HTML 注释
□ H. 所有 input/select 字段含 data-source 标注（field-source span）
□ I. 浏览器直接打开 index.html → 顶部菜单 → 招商管理 → 8 项菜单全部可点击跳转
□ J. 跨页跳转：客户列表 → 客户详情 → 商机详情 → 协议详情 → 合同签订 链路通畅
□ K. 公海池 → 我的客户 → 商机 → 协议 → 合同 全流程闭环
□ L. 客户回收站 → 复原 → 进入公海池 路径清晰
```

### 3.3 验证脚本（人工 + 工具）

**人工验证（5 分钟）**：
1. 打开 `prototype/index.html` → 点击顶部 "招商管理" → 检查 8 个菜单
2. 依次点击每个菜单项 → 确认 HTML 加载正常（无 404）
3. 在 pool.html 尝试点击 "新增客户" → 确认弹窗出现
4. 在 my-customer.html 尝试点击 "添加线索" → 确认弹窗出现
5. 在 opportunity-detail.html 切换 6 个 Tab → 确认内容切换
6. 在 agreement-detail.html 切换 4 个 Tab → 确认内容切换

**工具验证（grep）**：

```bash
# 验证脑图节点全部出现在原型中
cd prototype/park-business
grep -l "公海池" *.html           # → 应在 pool.html + my-customer.html
grep -l "退回公海" *.html         # → 应在 opportunity.html
grep -l "竞争对手" *.html         # → 应在 opportunity-detail.html
grep -l "客户保留期限" *.html     # → 应在 settings.html
grep -l "宽限期设置" *.html       # → 应在 settings.html
grep -l "员工最多保有" *.html     # → 应在 settings.html
grep -l "成交释放" *.html         # → 应在 settings.html
grep -l "转化漏斗" *.html         # → 应在 statistics.html
grep -l "来源渠道" *.html         # → 应在 statistics.html
grep -l "复原" *.html             # → 应在 recycle.html
grep -l "公海日志" *.html         # → 应在 pool.html + pool-log.html
```

**节点覆盖率统计**（目标 100%）：

| 模块 | 节点数 | 已映射 | 待映射 | 覆盖率 |
|---|---|---|---|---|
| 公海池 | 5 | 5 | 0 | 100% |
| 我的客户 | 6 | 6 | 0 | 100% |
| 商机管理 | 11 | 11 | 0 | 100% |
| 协议管理 | 3 | 3 | 0 | 100% |
| 客户回收站 | 2 | 2 | 0 | 100% |
| 客户档案 | 2 | 2 | 0 | 100% |
| 招商统计 | 3 | 3 | 0 | 100% |
| 基础设置 | 6 | 6 | 0 | 100% |
| **合计** | **38** | **38** | **0** | **100%** |

> 注：用户提供的脑图是树形结构，递归统计每个子节点（包括容器节点如"添加联系人/"），实际叶子（不可字段）31个，详见详见 § 二详解。

---

## 四、原型页面清单

### 4.1 侧边栏一级菜单（8 项）

| 序 | 模块 ID | 中文名 | 原型文件 | 状态 | 脑图节点数 |
|---|---|---|---|---|---|
| 1 | `pool` | 公海池 | `pool.html` | ✅ v1.0 | 5 |
| 2 | `my-customer` | 我的客户 | `my-customer.html` | ✅ v1.0 | 6 |
| 3 | `opportunity` | 商机管理 | `opportunity.html` | ✅ v1.0 | 11 |
| 4 | `agreement` | 协议管理 | `agreement.html` | ✅ v1.0 | 3 |
| 5 | `recycle` | 客户回收站 | `recycle.html` | 🔵 待原型 | 2 |
| 6 | `archive` | 客户档案 | `customer-detail.html` | ✅ v1.0 | 2 |
| 7 | `statistics` | 招商统计 | `statistics.html` | 🔵 待原型 | 3 |
| 8 | `settings` | 基础设置 | `settings.html` | 🔵 待原型 | 6 |

### 4.2 详情页（链接跳转，不在侧边栏）

| 文件 | 入口 | 状态 | 脑图节点 |
|---|---|---|---|
| `opportunity-detail.html` | 我的客户 → 商机卡片 / 商机管理列表 | ✅ v1.0 | 商机跟进/日程/竞争对手/客户意向/联系人/退回/终止/签订 |
| `agreement-detail.html` | 协议管理 → 协议行 | ✅ v1.0 | 延期/终止/转合同 |
| `pool-log.html` | 公海池 → 公海日志按钮 | 🔵 待原型 | 分配认领/回收记录 |

---

## 五、数据源（数据库表设计）

### 5.1 主表

| 表名 | 字段（关键） | 来源 |
|---|---|---|
| `pai_business.clue_enterprise` | id, name, contact_name, contact_phone, source, follower_id, lead_status, valid_flag, pool_status, pool_in_time, pool_out_time, park_id, tenant_id, create_time, update_time | csyh `ClueEnterprise` + 扩展 |
| `pai_business.contacts` | id, enterprise_id, name, phone, role, stance, relationship, contact_status, gender, birthday, department, position, duty_desc | csyh `Contacts` + 脑图扩展 |
| `pai_business.opportunity` | id, enterprise_id, business_type(LEASE/SALE), house_id, demand_area, intend_price, contact_id, status, owner_id, create_time | csyh `Opportunity` + 意向扩展 |
| `pai_business.communication` | id, enterprise_id, opportunity_id, type(PHONE/WECHAT/VISIT/SHOW/EMAIL), content, owner_id, followup_time | csyh `Communication` |
| `pai_business.competitor` | id, enterprise_id, opportunity_id, name, info, progress | 脑图新增 |
| `pai_business.schedule` | id, enterprise_id, opportunity_id, type(VISIT/MEETING/OTHER), title, start_time, end_time, owner_id | 脑图新增（日程管理） |

### 5.2 公海池相关

| 表名 | 字段 | 说明 |
|---|---|---|
| `pai_business.pool_log` | id, enterprise_id, action(ASSIGN/CLAIM/RECYCLE/ABANDON), from_user, to_user, reason, operator_id, create_time | 脑图"公海日志" |
| `pai_business.client_handover` | id, enterprise_id, from_user, to_user, type, reason, create_time | csyh `ClientHandover` |

### 5.3 协议/合同

| 表名 | 字段 | 说明 |
|---|---|---|
| `pai_business.agreement` | id, opportunity_id, type(LEASE/SALE), status, sign_time, expire_time, fee_info, attachments | csyh `BusinessProtocol` + 脑图（延期/终止） |
| `pai_business.contract` | id, agreement_id, status, sign_time, file_url | 脑图"合同签订" |

### 5.4 基础设置

| 表名 | 字段 | 说明 |
|---|---|---|
| `pai_business.biz_setting` | id, park_id, retention_days, grace_days, leader_assign_enabled, employee_claim_enabled, max_hold_per_staff, release_after_deal | 脑图"基础设置"全字段 |

---

## 六、API 设计（REST 风格）

### 6.1 公海池

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/business/pool/page` | `business:pool:view` | 公海客户分页 |
| POST | `/business/pool` | `business:pool:add` | 新增客户入池 |
| POST | `/business/pool/{id}/assign` | `business:pool:assign` | 分配跟进人 |
| POST | `/business/pool/{id}/claim` | `business:pool:claim` | 认领 |
| DELETE | `/business/pool/{id}` | `business:pool:delete` | 删除 → 回收站 |
| GET | `/business/pool/log` | `business:pool:log` | 公海日志 |

### 6.2 我的客户

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/business/my-customer/page` | `business:my-customer:view` | 我的客户分页 |
| POST | `/business/my-customer` | `business:my-customer:add` | 添加线索 |
| POST | `/business/my-customer/{id}/contact` | `business:my-customer:contact` | 添加联系人 |
| PUT | `/business/my-customer/{id}/mark` | `business:my-customer:mark` | 标记有效/无效 |
| POST | `/business/my-customer/{id}/abandon` | `business:my-customer:abandon` | 线索废弃 |
| POST | `/business/my-customer/{id}/return-pool` | `business:my-customer:return-pool` | 退回公海 |
| POST | `/business/my-customer/{id}/convert` | `business:my-customer:convert` | 商机转化 |

### 6.3 商机管理

| 方法 | 路径 | 权限 | 说明 |
|---|---||---|
| GET | `/business/opportunity/page` | `business:opportunity:view` | 商机列表 |
| GET | `/business/opportunity/{id}` | `business:opportunity:view` | 商机详情 |
| POST | `/business/opportunity/{id}/return-pool` | `business:opportunity:return-pool` | 退回公海 |
| PUT | `/business/opportunity/{id}/contact` | `business:opportunity:contact` | 更换联系人 |
| POST | `/business/opportunity/{id}/followup` | `business:opportunity:followup` | 添加跟进 |
| POST | `/business/opportunity/{id}/schedule` | `business:opportunity:schedule` | 添加日程 |
| POST | `/business/opportunity/{id}/competitor` | `business:opportunity:competitor` | 添加竞争对手 |
| POST | `/business/opportunity/{id}/terminate` | `business:opportunity:terminate` | 商机终止 |
| POST | `/business/opportunity/{id}/agreement` | `business:opportunity:agreement` | 签订协议 |

### 6.4 协议管理

| 方法 | 路径 | 权限 | 说明 |
|---|---||---|
| GET | `/business/agreement/page` | `business:agreement:view` | 协议列表 |
| GET | `/business/agreement/{id}` | `business:agreement:view` | 协议详情 |
| POST | `/business/agreement/{id}/delay` | `business:agreement:delay` | 协议延期 |
| POST | `/business/agreement/{id}/terminate` | `business:agreement:terminate` | 协议终止 |
| POST | `/business/agreement/{id}/contract` | `business:agreement:contract` | 签订合同 |

### 6.5 客户档案/回收站/统计/设置

| 方法 | 路径 | 权限 | 说明 |
|---|---||---|
| GET | `/business/customer/{id}/timeline` | `business:customer:view` | 时间轴 |
| GET | `/business/recycle/page` | `business:recycle:view` | 回收站列表 |
| POST | `/business/recycle/{id}/restore` | `business:recycle:restore` | 复原到公海 |
| GET | `/business/statistics/funnel` | `business:statistics:view` | 转化漏斗 |
| GET | `/business/statistics/lead` | `business:statistics:view` | 线索统计 |
| GET | `/business/statistics/source` | `business:statistics:view` | 来源渠道分析 |
| GET | `/business/settings` | `business:settings:view` | 基础设置 |
| PUT | `/business/settings` | `business:settings:edit` | 更新设置 |

---

## 七、权限点全集

```
business:pool:*                公海池 (view/add/assign/claim/delete/log)
business:my-customer:*         我的客户 (view/add/contact/mark/abandon/return-pool/convert)
business:opportunity:*         商机管理 (view/return-pool/contact/followup/schedule/competitor/terminate/agreement)
business:agreement:*           协议管理 (view/delay/terminate/contract)
business:recycle:*             回收站 (view/restore)
business:customer:*            客户档案 (view/timeline)
business:statistics:*          招商统计 (view)
business:settings:*            基础设置 (view/edit)
```

---

## 八、关键业务流程

### 8.1 客户资源流转

```
[入池] → 公海池(待分配)
         ↓ (分配/认领)
我的客户(跟进中)
         ↓ (商机转化)
商机管理(意向登记)
         ↓ (签订协议)
协议管理(履约中)
         ↓ (签订合同)
[合同签订完成]

任意阶段可:
  - 标记有效/无效
  - 退回公海
  - 废弃(联系不上/无意向/无法满足客户需求)
```

### 8.2 保留期限与宽限期

```
1. 客户分配给员工 → 记录 retention_start
2. 超过 retention_days 未跟进 → 进入宽限期
3. 宽限期 (grace_days) 内有跟进 → 续期
4. 宽限期结束无跟进 → 自动回收公海
```

### 8.3 公海保有规则

```
- 每个员工最多保有 N 个客户 (max_hold_per_staff)
- 员工领取新客户前校验：当前数 + 新客户 ≤ N
- 成交 1 单 → 释放 1 个保有名额（可继续领取）
```

---

## 九、原型规范继承

| 项 | 取值 | 来源 |
|---|---|---|
| 主色 | `#007AFF` | Apple DESIGN.md |
| 字体 | SF Pro Text / PingFang SC | common.css |
| 圆角 | 12px（卡片）/ 8px（按钮） | common.css |
| 阴影 | `0 2px 12px rgba(0,0,0,0.08)` | common.css |
| 顶部信息条 | `.top-info` 100px | common.css |
| Tab 栏 | `.tabs-bar` + `.tabs-content` | common.css |
| 数据源标注 | HTML 注释 + `field-source` span | common.css |
| 权限标注 | HTML 注释 `{{perm:...}}` | common.css |

---

## 十、产出文件清单

```
prototype/park-business/
├── README.md                    # 模块索引
├── DESIGN.md                    # 本文件（v1.1：增加验收标准 + 节点映射表）
├── pool.html                    # ✅ 公海池（5 节点）
├── my-customer.html             # ✅ 我的客户（6 节点）
├── customer-detail.html         # ✅ 客户档案（2 节点）
├── opportunity.html             # ✅ 商机管理列表（11 节点）
├── opportunity-detail.html      # ✅ 商机详情（含 6 个 Tab：商机跟进/日程/竞争对手/联系人/意向/概览）
├── agreement.html               # ✅ 协议管理列表（3 节点）
├── agreement-detail.html        # ✅ 协议详情（含 4 个 Tab：基本信息/费用/附件/变更历史）
├── pool-log.html                # 🔵 公海日志（待原型）
├── recycle.html                 # 🔵 客户回收站（待原型，2 节点）
├── statistics.html              # 🔵 招商统计（待原型，3 节点）
└── settings.html                # 🔵 基础设置（待原型，6 节点）
```

**进度**：8/11 文件已产出，3 个待完成（recycle/statistics/settings）

---

## 十一、行业CRM对标分析（v1.2 核心补充）

> **背景**：脑图只是业务框架。要把"智慧招商"做到与行业接轨，必须对标通用 CRM 标杆。本节参考 **Salesforce Sales Cloud**、**HubSpot Sales Hub**、**销售易**、**纷享销客**、**Zoho CRM** 五大 CRM 系统，补充脑图未明确的业务细节。

### 11.1 对标系统一览

| 系统 | 国家 | 核心定位 | 对我们的启示 |
|---|---|---|---|
| **Salesforce Sales Cloud** | 美国 | 全球 CRM 标杆 | Opportunity Stage Pipeline（商机阶段管道）、Lead Scoring（线索评分）、Forecast（销售预测） |
| **HubSpot Sales Hub** | 美国 | 增长型 CRM | Deal Pipeline（交易管道）、Activity（活动序列）、Email Tracking |
| **销售易** | 中国 | B2B 大客户 CRM | 线索池/公海池、SDR 分配、客户 360° 视图 |
| **纷享销客** | 中国 | 销售过程管理 CRM | 销售流程管理、PaaS 自定义、外勤签到 |
| **Zoho CRM** | 印度/全球 | 中小企业 CRM | Workflow Automation（工作流自动化）、多币种 |

### 11.2 缺失功能补充（脑图未涉及，需补充）

| 功能模块 | 脑图状态 | 行业标配 | 建议补充 | 优先级 |
|---|---|---|---|---|
| **线索评分**（Lead Scoring） | ❌ 未提及 | HubSpot/Salesforce/Zoho | 线索转化漏斗必备 | **P0** |
| **商机阶段管道**（Sales Pipeline） | ❌ 仅"跟进中/已签协议/已终止" | Salesforce 7+ 阶段 | 商机管理需细化 | **P0** |
| **销售预测**（Forecast） | ❌ 未提及 | Salesforce/销售易 | 招商统计必备 | **P0** |
| **客户分级**（Account Tier） | ❌ 未提及 | 销售易/纷享销客 | 招商必备（VIP/重点/普通） | P1 |
| **跟进节奏**（Cadence） | ❌ 未提及 | HubSpot | 销售自动化 | P1 |
| **邮件营销**（Email Marketing） | ❌ 未提及 | HubSpot/Salesforce | 招商外联 | P2 |
| **外勤签到** | ❌ 未提及 | 纷享销客 | 实地招商 | P2 |
| **工作流自动化**（Workflow） | ❌ 仅手动 | Zoho/HubSpot | 自动化跟进提醒 | P1 |
| **客户健康度** | ❌ 仅"有效/无效" | 销售易 | 智能评估 | P1 |
| **微信集成** | ⚠️ 仅有"微信跟进"字段 | 销售易 | 国内必选 | P0 |
| **数据看板** | ⚠️ 仅"招商统计" | 全部 | 多维度看板 | P0 |

### 11.3 商机阶段 Pipeline（参照 Salesforce 标准）

> 原型 `opportunity.html` 当前仅有 4 个 Tab（全部/跟进中/已签协议/已终止），粒度不够。行业标杆采用 7+ 阶段管道。

**推荐阶段定义**（基于 B2B 招商业务定制）：

```
阶段 1：线索入池（Lead Pool）           ──→ 客户被录入或小程序登记
阶段 2：资质验证（Qualification）      ──→ 验证客户是否为有效商机（决策权、预算、需求）
阶段 3：需求确认（Needs Analysis）     ──→ 完成 BANT 评估（Budget/Authority/Need/Timeframe）
阶段 4：方案匹配（Solution Match）     ──→ 匹配房源/产品/报价
阶段 5：带看考察（Visit & Demo）       ──→ 实地看房/技术交流
阶段 6：谈判协商（Negotiation）        ──→ 价格/条款/政策协商
阶段 7：协议签订（Closed Won）         ──→ 协议生效
失败：Closed Lost（终止）
```

**每阶段必填字段**（行业标配）：

| 阶段 | 进入条件 | 必填字段 |
|---|---|---|
| 线索入池 | 客户被新增 | 客户名称、联系人、手机号 |
| 资质验证 | 线索跟进完成 | 决策人确认、预算范围、需求面积 |
| 需求确认 | 资质验证通过 | 意向房源、意向价格、决策周期 |
| 方案匹配 | 需求确认通过 | 报价方案、优惠政策 |
| 带看考察 | 方案匹配通过 | 带看次数、带看反馈 |
| 谈判协商 | 带看考察通过 | 谈判记录、让步记录 |
| 协议签订 | 谈判达成 | 协议编号、签订日期、协议金额 |

### 11.4 Lead Scoring 线索评分规则

> 原型当前仅有"有效/无效"二值判定，行业标准采用 **多维度评分**。

**推荐评分模型**（BANT + 行为）：

| 维度 | 权重 | 评分项 | 分值 |
|---|---|---|---|
| **B 预算（Budget）** | 25% | 有明确预算 | +30 |
|  |  | 有大致范围 | +15 |
|  |  | 未透露 | +0 |
| **A 决策权（Authority）** | 25% | 决策者直接跟进 | +30 |
|  |  | 影响者/经办人 | +15 |
|  |  | 暂无接触 | +0 |
| **N 需求（Need）** | 30% | 明确意向房源 | +35 |
|  |  | 有大致需求 | +20 |
|  |  | 需求模糊 | +0 |
| **T 时间（Time）** | 20% | 本月决策 | +25 |
|  |  | 季度内决策 | +15 |
|  |  | 长期 | +5 |

**行为加分**（按互动频次）：

| 行为 | 加分 |
|---|---|
| 客户回复微信 | +5 |
| 客户接听电话 | +10 |
| 客户参加带看 | +20 |
| 客户主动询问价格 | +15 |
| 客户要求报价方案 | +25 |

**评分等级**：

| 总分 | 等级 | 处理 |
|---|---|---|
| ≥80 | 🔥 高意向 | 优先分配，加快跟进节奏 |
| 60-79 | 🌟 中意向 | 正常跟进 |
| 40-59 | 😐 低意向 | 培育期，定期触达 |
| <40 | ❄️ 冷意向 | 维持低频跟进或回收 |

### 11.5 招商统计多维度看板

> 原型仅列出 3 项（转化漏斗/线索统计/来源渠道），行业标准是 **多维看板组合**。

**推荐看板**：

```
招商统计/
├── 看板首页（Overview Dashboard）
│   ├── 核心指标卡片（KPI Cards）
│   │   ├── 本月线索数 / 上月对比 / 同比
│   │   ├── 本月新增商机数 / 转化率
│   │   ├── 本月签约数 / 签约金额
│   │   ├── 本月入驻数 / 入住率
│   │   └── 个人业绩排名
│   ├── 实时动态（Real-time Feed）
│   │   ├── 最新签约（自动滚动）
│   │   ├── 最新入池
│   │   └── 最新跟进
│   └── 待办提醒（Todo Reminder）
│       ├── 跟进超期客户数
│       ├── 即将到期协议数
│       └── 待分配线索数
│
├── 转化漏斗（Conversion Funnel）
│   ├── 全链路：线索 → 我的客户 → 商机 → 协议 → 合同 → 入驻
│   ├── 分阶段转化率
│   ├── 各阶段平均停留时长
│   └── 转化瓶颈诊断
│
├── 线索统计（Lead Statistics）
│   ├── 线索来源分布（饼图）
│   ├── 线索趋势（折线图：日/周/月）
│   ├── 线索状态分布（漏斗）
│   ├── 线索评分分布
│   └── 跟进人员业绩对比
│
├── 客户来源渠道分析（Source Channel）
│   ├── 各渠道线索数对比
│   ├── 各渠道转化率对比
│   ├── 各渠道 ROI（投入产出比）
│   └── 推荐渠道 TOP3
│
├── 商机预测（Forecast）★ 行业标配
│   ├── 本月预测签约金额（加权 Pipeline）
│   ├── 部门/个人预测排名
│   ├── 按阶段加权（成单概率）
│   └── 预测准确率历史
│
└── 团队排行（Leaderboard）★ 行业标配
    ├── 业绩榜（签约金额/套数）
    ├── 跟进榜（跟进次数/质量）
    ├── 转化榜（转化率）
    └── 响应速度榜（响应时长）
```

### 11.6 跟进节奏（Cadence / Sales Sequence）

> 行业标配（如 HubSpot Sequences、Apollo）。

**示例：首次带看后跟进节奏**

```
Day 0   带看完成 → 客户档案自动记录
Day 0+2h 发送微信：感谢信 + 户型图 PDF
Day 1   电话回访：询问意向
Day 3   发送邮件：报价方案 + 优惠政策
Day 7   微信跟进：约第二次面谈
Day 14  第二次带看（房源/周边）
Day 30  决策提醒：协议条款确认
```

**自动化触发**：每个步骤可自动执行（提醒/发送邮件/创建日程）。

### 11.7 客户分级（Account Tier）

> 招商业务关键维度，决定资源分配优先级。

| 等级 | 标准 | 资源倾斜 |
|---|---|---|
| **A 类（VIP）** | 行业龙头 / 政府重点 / 500 强 | 专属招商经理 + 优先选房 + 租金优惠 |
| **B 类（重点）** | 上市公司 / 规上企业 / 高新企业 | 资深招商员 + 标准选房 + 适度优惠 |
| **C 类（普通）** | 中小企业 / 一般工商户 | 招商专员 + 标准化流程 |
| **D 类（观察）** | 资质存疑 / 跟进困难 | 仅维护基本联系 |

### 11.8 移动端集成（不限定于微信）

> 招商过程中移动端沟通占比 70%+。移动端不仅是微信，还包括企业微信、钉钉、自建 App、招商小程序等多种渠道。系统应提供完整的移动端能力，让招商专员可以随时随地跟进客户。

**核心能力矩阵**：

| 能力 | 说明 | 覆盖渠道 |
|---|---|---|
| **消息聚合** | 与客户对话自动归档到跟进记录 | 微信、企业微信、钉钉、短信、电话 |
| **文件流转** | 户型图/合同 PDF 通过移动端发送，自动入库归档 | 微信文件、企业微信文件、邮件附件、App 上传 |
| **通讯录打通** | 通过移动端渠道添加客户为联系人 | 企业微信外部联系人、钉钉外部联系人、招商小程序用户体系 |
| **小程序入口** | 意向登记入口（已存在）、客户自助查询 | 微信小程序、支付宝小程序、抖音小程序 |
| **移动审批** | 协议签订、合同审批在移动端完成 | 企微审批、钉钉审批、自建 App 工作台 |
| **智能客服** | 常见问题自动应答（FAQ 匹配） | 微信公众号、企微机器人、智能客服 App |
| **位置打卡** | 实地拜访/带看打卡，自动同步到跟进记录 | 企微考勤、自建 App、企业签到 |
| **语音转文字** | 语音消息自动识别转为跟进内容文字 | 全渠道支持 |

**移动端必须支持的场景**：

```
┌─────────────────────────────────────────────────────┐
│  移动端招商工作台                                    │
├─────────────────────────────────────────────────────┤
│  ① 客户列表：随手翻看所有跟进中客户                  │
│  ② 快速跟进：1 分钟内完成一次电话/微信跟进记录       │
│  ③ 文件库：客户资料/户型图/合同模板随身带            │
│  ④ 待办提醒：跟进超期/即将到期实时推送                │
│  ⑤ 协议审批：随时随地提交协议、查看进度              │
│  ⑥ 数据看板：业绩完成/排名一眼可见                   │
│  ⑦ 名片扫描：客户名片拍照自动入库                    │
│  ⑧ 语音录入：跟进口述自动转写为跟进记录              │
└─────────────────────────────────────────────────────┘
```

**移动端优先级（按招商业务使用频率）**：

| 优先级 | 移动端能力 | 适用场景 |
|---|---|---|
| **P0** | 微信/企微集成 | 客户沟通主力渠道 |
| **P0** | 移动端跟进记录 | 实时录入跟进 |
| **P0** | 待办推送（短信/推送/微信） | 跟进超期/即将到期提醒 |
| **P1** | 移动端审批 | 协议/合同审批流 |
| **P1** | 名片扫描 OCR | 招商外出场景 |
| **P1** | 语音转文字 | 跟进口述 |
| **P2** | 钉钉集成 | 集团统一 IM 场景 |
| **P2** | 自建 App | 重型业务（带看直播/3D 看房） |
| **P2** | 位置打卡 | 考勤合规需求 |

### 11.9 数据字段补（脑图未明确但行业必备）

| 实体 | 脑图未涉及但行业必有 |
|---|---|
| **客户** | 统一社会信用代码（工商查询）、行业代码、注册资本、客户标签、来源 URL、获客成本 |
| **商机** | 成单概率（%）、预期签约日期、预期金额、加权金额、竞争对手、阻碍因素 |
| **跟进** | 跟进时长、跟进成本、客户情绪分析、AI 摘要 |
| **合同** | 付款条款、违约金、续约提醒、合同到期预警 |
| **统计** | 同环比、目标完成率、TOP10 排行 |

### 11.10 脑图未提及但必备的页面

| 页面 | 行业标配 | 来源 |
|---|---|---|
| **我的待办**（My Todo） | ✅ 全员 | Salesforce Task、HubSpot Tasks |
| **跟进提醒**（Followup Reminder） | ✅ 全员 | HubSpot |
| **客户 360° 视图** | ⚠️ 部分 | 销售易 客户详情 |
| **销售目标/OKR** | ✅ 全员 | 全部 |
| **业绩排行** | ✅ 全员 | 全部 |
| **知识库/话术库** | ⚠️ 部分 | HubSpot |

---

## 十二、版本历史

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-14 | 初版：基于脑图（智慧招商 CRM 架构）+ csyh 业务代码 |
| v1.1 | 2026-08-14 | 升级：增加验收标准 + 节点映射表 + 覆盖率统计 |
| v1.2 | 2026-08-14 | **核心补充**：行业 CRM 对标（Salesforce/HubSpot/销售易/纷享销客/Zoho），补全 11 个行业缺失项 |
| v1.2.1 | 2026-08-14 | §11.8 调整：微信集成 → 移动端集成（不限定微信，含企微/钉钉/App/小程序等全场景） |
| v1.3 | 2026-08-14 | **交互完整性验收**：增加 §十三 交互矩阵 + §十四 业务状态机约束；识别 8 类缺失交互，制定 §十五 实施计划 |

---

## 十三、交互完整性验收矩阵（v1.3 核心新增）

> **原则**：每个交互元素必须有完整的业务逻辑——**触发 → 校验 → 数据变更 → 状态联动 → 反馈**，而不是仅显示 alert 或跳转。

### 13.1 交互点盘点（v1.3 当前）

| 交互类型 | 数量 | 已绑定 | 缺失/死链 |
|---|---|---|---|
| **按钮 (btn)** | 161 | 156 | 5（仅占位 alert） |
| **弹窗触发**（openXxx） | 53 | 53 | 0 ✅ |
| **Tab 切换**（switchTab） | 41 | 41 | 0 ✅ |
| **跨页跳转**（parent.loadPage） | 34 | 34 | 0 ✅ |
| **二次确认**（confirm/prompt） | 13 | 13 | 0 ✅ |
| **表单提交** | 30+ 表单 | 13 | 17（缺业务逻辑） |
| **数据联动** | - | 部分 | **缺失** **（关键）** |

### 13.2 缺失交互清单（v1.3 必须修复）

| # | 文件 | 交互点 | 当前状态 | 期望逻辑 |
|---|---|---|---|---|
| **M-01** | opportunity.html | 行点击 + 详情按钮 | ❌ 事件冒泡冲突，重复触发 | 改用 stopPropagation 或移除行点击 |
| **M-02** | customer-detail.html | 商机管理 Tab 6 个操作按钮（商机跟进/更换联系人/日程管理/商机终止/签订协议/退回公海） | ❌ 仅跳转 1 个，5 个无 onclick | 全部绑定到 opportunity-detail.html 锚点或对应弹窗 |
| **M-03** | opportunity-detail.html | 顶部"签订协议"按钮 | ❌ 直接跳转，商机数据未传递 | 应弹出"选择协议模板/确认商机信息"前置弹窗，关联商机 ID |
| **M-04** | opportunity-detail.html | "商机终止"按钮 | ⚠️ 有 confirm + prompt，但状态未联动 | 终止后：①隐藏"签订协议"按钮 ②状态徽章变红色 ③时间轴新增终止记录 ④提示"终止后不可恢复" |
| **M-05** | agreement-detail.html | 顶部 3 个操作按钮（延期/终止/转合同） | ❌ 无状态约束 | 已终止/已转合同的协议应隐藏对应按钮；即将到期未到期应有差异化提示 |
| **M-06** | statistics.html | 6 个 Tab + 导出按钮 | ❌ Tab 切换 OK，但导出按钮无逻辑 | ①导出按钮：触发 mock 下载提示 ②看板概览的"最新签约"卡片可点击钻取 |
| **M-07** | statistics.html | 漏斗图各阶段条 | ❌ 纯展示，不可交互 | 点击某阶段 → 跳转到该阶段的商机列表 |
| **M-08** | statistics.html | 排行榜员工行 | ❌ 不可点击 | 点击员工 → 跳转到该员工的商机/客户详情 |
| **M-09** | settings.html | 6 个开关 | ❌ 视觉变化 OK，无联动 | ①关闭员工领取 → 保存后提示"需刷新页面" ②关闭分配规则后弹窗禁用对应功能 |
| **M-10** | settings.html | "客户保留期限"输入框 | ❌ 无校验 | 应校验范围（1-365）+ 提示宽限期联动 |
| **M-11** | settings.html | "保存设置"按钮 | ⚠️ 有 confirm，但保存后无视觉反馈 | 保存后顶部显示"已保存"提示条，3 秒后消失 |
| **M-12** | pool.html | 批量分配按钮 | ❌ 弹窗 OK，但未传选中数据 | 弹窗标题动态显示"批量分配 N 项"，传入 enterpriseIds 数组 |
| **M-13** | pool.html | 单条认领按钮 | ⚠️ confirm OK，但状态变更仅 opacity 变化 | 认领后：①按钮变"已认领"+ disabled ②该行增加"已认领"标记 ③1 秒后从当前列表移除（动画） |
| **M-14** | my-customer.html | 批量废弃/批量退回 | ❌ confirm 后无状态联动 | 批量后：①选中行批量灰化 ②弹出"操作完成 N 项"提示 |
| **M-15** | recycle.html | 复原按钮 | ❌ 弹窗 OK，确认后无视觉反馈 | 复原后：①该行从列表移除 ②显示"已复原到公海池"提示 ③可立即跳转到公海池 |
| **M-16** | opportunity-detail.html | 商机跟进 Tab 中"带看"按钮 | ⚠️ 有弹窗但未联动日程 | 添加带看跟进应自动建议"添加日程" |
| **M-17** | customer-detail.html | 添加联系人弹窗 | ❌ 弹窗 OK，保存后无刷新 | 保存后：①联系人列表立即增加新卡片 ②显示"已添加"提示 |
| **M-18** | 全部页面 | 翻页/排序/筛选 | ❌ 仅展示，无交互逻辑 | 列表行点击翻页+排序+筛选应触发 mock 数据过滤 |

### 13.3 业务状态机约束（v1.3 索引）

> **核心原则**：客户/商机/协议/客户档案的状态流转必须严格校验。**详细状态机规范见 §14**。

状态机索引：
- §14.1 客户线索状态机（7 状态，6 转换，4 守卫）
- §14.2 商机状态机（5 状态，5 转换，4 守卫）
- §14.3 协议状态机（5 状态，4 转换，5 守卫）
- §14.4 客户档案等级状态机（4 等级，3 守卫）
- §14.5 分配规则状态机（2 规则，2 守卫）

简要速查（详见 §14）：

### 13.4 状态联动规则（v1.3 新增）

> **原则**：一个操作触发其他字段/列表/UI 状态自动更新。

| 触发操作 | 联动更新 |
|---|---|
| 客户认领 | ①行按钮变"已认领" ②从公海池移除 ③"我的客户"列表增加 1 行（需跨页） |
| 客户退回公海 | ①从我的客户移除 ②公海池增加 1 行 ③客户档案时间轴新增记录 |
| 商机转化 | ①我的客户 Tab 切换"商机管理" ②时间轴新增"商机转化"节点 ③跟进记录增加 |
| 商机终止 | ①状态徽章变红色"已终止" ②时间轴新增终止节点 ③顶部"签订协议/退回公海"按钮隐藏 |
| 协议签订 | ①商机状态变"已签协议" ②商机详情 Tab 切换到协议 ③协议详情页可访问 |
| 协议终止 | ①状态徽章变红 ②时间轴新增 ③商机状态回退到"跟进中"（需用户确认） |
| 签订合同 | ①协议状态变"已签合同" ②商机状态变"已签合同" ③时间轴新增合同节点 |
| 线索废弃 | ①状态徽章变灰 ②跟进按钮禁用 ③"添加跟进记录"按钮隐藏 |
| 客户复原 | ①从回收站移除 ②公海池列表增加 ③日志新增"复原"记录 |

### 13.5 交互验收 Checklist（v1.3）

```
基础交互
□ I-01  41 处 Tab 切换全部可用（41/41）
□ I-02  53 处弹窗触发全部可用（53/53）
□ I-03  34 处跨页跳转全部可用（34/34）
□ I-04  13 处 confirm 确认全部可用（13/13）

业务逻辑完整性（M 01 18）
□ M-01  opportunity.html 行点击事件冒泡修复
□ M-02  customer-detail.html 商机管理 5 个按钮全部绑定
□ M-03  opportunity-detail.html 签订协议关联商机数据
□ M-04  opportunity-detail.html 商机终止状态联动
□ M-05  agreement-detail.html 状态约束（已终止隐藏操作）
□ M-06  statistics.html 导出报表+钻取逻辑
□ M-07  statistics.html 漏斗图阶段可点击钻取
□ M-08  statistics.html 排行榜员工可点击
□ M-09  settings.html 开关保存后联动
□ M-10  settings.html 输入校验（数值范围）
□ M-11  settings.html 保存视觉反馈
□ M-12  pool.html 批量分配传入选中数据
□ M-13  pool.html 单条认领状态变更完整
□ M-14  my-customer.html 批量操作状态联动
□ M-15  recycle.html 复原视觉反馈
□ M-16  opportunity-detail.html 带看后自动建议日程
□ M-17  customer-detail.html 添加联系人后立即刷新
□ M-18  列表行翻页/排序/筛选逻辑

业务状态机（13.3）
□ S-01  客户线索状态机 4 个约束全部生效
□ S-02  商机状态机 4 个约束全部生效
□ S-03  协议状态机 4 个约束全部生效
□ S-04  客户档案等级 3 个约束全部生效

状态联动（13.4）
□ L-01  9 个状态联动全部生效（认领/退回/转化/终止/签订/复原等）
```

**总计**：73 项验收点（18 基础 + 18 业务逻辑 + 4 状态机 + 9 状态联动 + 其他）

---

## 十四、业务状态机详细规范（v1.4 核心新增）

> **来源**：脑图业务框架 + 行业 CRM 标杆（Salesforce/HubSpot）+ csyh 业务代码 + v1.3 验收反馈。
> **原则**：每个状态机都包含**完整的状态定义、转移条件、守卫（guard）、副作用、异常路径**。

### 14.1 客户线索状态机（Lead State Machine）

**适用实体**：`clue_enterprise`
**状态字段**：`pool_status`（公海状态）+ `lead_status`（线索状态）+ `valid_flag`（有效性）+ `del_flag`（删除标记）

#### 14.1.1 状态定义（7 个状态）

| 状态 | 标识 | 说明 | 数据字段 |
|---|---|---|---|
| `POOL_PENDING` | 公海池-待分配 | 在公海池中，未分配跟进人 | `pool_status=1` `follower_id=null` |
| `POOL_CLAIMED` | 公海池-已认领 | 员工主动从公海池认领 | `pool_status=1` `follower_id=<员工>` |
| `POOL_ASSIGNED` | 公海池-已分配 | 经理分配给员工 | `pool_status=1` `follower_id=<员工>` |
| `FOLLOWING_VALID` | 跟进中-有效 | 线索有效，进入我的客户 | `pool_status=2` `lead_status=1` `valid_flag=1` |
| `FOLLOWING_INVALID` | 跟进中-无效 | 线索无效 | `pool_status=2` `lead_status=1` `valid_flag=0` |
| `ABANDONED` | 已废弃 | 主动废弃 | `del_flag=1` `abandon_reason=有` |
| `RECYCLED` | 已回收（至回收站） | 30 天无效或主动删除 | `del_flag=1` `recycled_at=有` |

#### 14.1.2 状态转移表（6 个转移）

```
源状态          事件/触发          守卫条件                    目标状态            副作用
─────────────────────────────────────────────────────────────────────────────────────
POOL_PENDING    ASSIGN            经理权限 + 用户激活        POOL_ASSIGNED      pool_log.ASSIGN
POOL_PENDING    CLAIM             员工权限 + 保有量未超限    POOL_CLAIMED       pool_log.CLAIM
POOL_PENDING    AUTO_RECYCLE      24小时无任何操作            RECYCLED           pool_log.AUTO_RECYCLE
POOL_ASSIGNED   FIRST_FOLLOWUP    48小时内有有效跟进          FOLLOWING_VALID    lifecycle.first_followup
POOL_CLAIMED    FIRST_FOLLOWUP    48小时内有有效跟进          FOLLOWING_VALID    lifecycle.first_followup
FOLLOWING_VALID MARK_INVALID      经理/员工权限              FOLLOWING_INVALID  lifecycle.mark_invalid
FOLLOWING_INVALID ABANDON          员工权限 + 原因必填         ABANDONED          pool_log.ABANDON
FOLLOWING_INVALID AUTO_RECYCLE     无效状态持续30天            RECYCLED           pool_log.AUTO_RECYCLE
FOLLOWING_VALID RETURN_POOL       员工主动退回                POOL_PENDING        pool_log.RETURN
FOLLOWING_VALID CONVERT_OPP       转商机                       FOLLOWING_VALID    opportunity.create
ABANDONED       RESTORE           经理权限                    RECYCLED           pool_log.RESTORE
RECYCLED        RESTORE           经理权限 + 在保留期内       POOL_PENDING        pool_log.RESTORE
RECYCLED        DELETE_FOREVER    经理权限（不可逆）           (终态)              soft_delete
```

#### 14.1.3 守卫条件（Guard Conditions）

| 守卫 ID | 描述 | 校验逻辑 |
|---|---|---|
| G-L01 | 经理权限 | `user.role in [招商经理, 运营管理员]` |
| G-L02 | 员工权限 | `user.role = 招商员工` |
| G-L03 | 用户激活 | `user.status = ACTIVE` |
| G-L04 | 保有量未超限 | `current_hold + 1 ≤ max_hold_per_staff`（A 类客户除外） |
| G-L05 | 48 小时跟进时限 | `now - last_pool_action ≤ 48h` |
| G-L06 | 30 天无效自动回收 | `now - mark_invalid_at ≥ 30 days` |
| G-L07 | 回收站保留期 | `now - recycled_at ≤ 90 days`（超过则不可复原） |
| G-L08 | 原因必填 | `abandon_reason NOT NULL` |

#### 14.1.4 异常路径

```
异常：员工超额领取 → 拒绝（提示"已达保有上限 N"）
异常：48 小时无跟进 → 自动回收 + 通知员工 + 通知经理
异常：手动退回客户 → 需输入退回原因 → 记录 → 客户档案可查看
异常：网络故障 → 保留状态 + 队列补偿（最终一致）
```

### 14.2 商机状态机（Opportunity State Machine）

**适用实体**：`opportunity`
**状态字段**：`status`（FOLLOWING/SIGNED/TERMINATED/POOL）+ `business_type`（LEASE/SALE）

#### 14.2.1 状态定义（5 个状态）

| 状态 | 标识 | 说明 |
|---|---|---|
| `FOLLOWING` | 跟进中 | 商机创建后默认状态 |
| `SIGNED` | 已签协议 | 商机签订的协议生效 |
| `TERMINATED` | 已终止 | 商机主动终止或失败 |
| `POOL` | 公海池 | 商机被退回公海（极少客户） |
| `CONTRACTED` | 已签合同 | 协议转合同完成（终态） |

#### 14.2.2 状态转移表（5 个转移）

```
源状态         事件                守卫条件              目标状态         副作用
─────────────────────────────────────────────────────────────────────────
FOLLOWING      SIGN_AGREEMENT      协议签订成功          SIGNED          agreement.create
FOLLOWING      TERMINATE           员工权限 + 原因必填   TERMINATED      lifecycle.terminate
FOLLOWING      RETURN_POOL         员工权限 + 原因必填   POOL            pool_log.RETURN_OPP
SIGNED         SIGN_CONTRACT       协议未到期 + 未终止   CONTRACTED      contract.create
SIGNED         TERMINATE_AGREEMENT 协议终止 → 商机回退    FOLLOWING       agreement.terminate
```

#### 14.2.3 守卫条件（Guard Conditions）

| 守卫 ID | 描述 | 校验逻辑 |
|---|---|---|
| G-O01 | 协议未到期 | `agreement.expire_at > now` |
| G-O02 | 协议未终止 | `agreement.status != TERMINATED` |
| G-O03 | 终止原因必填 | `terminate_reason NOT NULL` |
| G-O04 | 商机阶段权限 | `user.role has perm:business:opportunity:terminate` |
| G-O05 | 公海商机不可直接终止 | 当前状态不能是 POOL（需先回到 FOLLOWING） |

#### 14.2.4 异常路径

```
异常：已签合同 → 不能退回公海（提示"请先终止合同"）
异常：已终止 → 不能再次终止（提示"商机已终止，请新增商机"）
异常：公海商机尝试终止 → 拒绝（提示"请先回到跟进中"）
```

### 14.3 协议状态机（Agreement State Machine）

**适用实体**：`agreement`
**状态字段**：`status`（ACTIVE/EXPIRING/TERMINATED/CONTRACTED）

#### 14.3.1 状态定义（5 个状态）

| 状态 | 标识 | 说明 |
|---|---|---|
| `ACTIVE` | 履约中 | 协议生效，未到期 |
| `EXPIRING` | 即将到期 | 距离到期 ≤30 天 |
| `TERMINATED` | 已终止 | 协议主动终止 |
| `CONTRACTED` | 已签合同 | 转合同完成（终态） |
| `EXPIRED` | 已到期 | 自动到期（终态） |

#### 14.3.2 状态转移表（4 个转移）

```
源状态         事件            守卫条件                    目标状态         副作用
─────────────────────────────────────────────────────────────────────────
ACTIVE         DELAY          经理权限 + 延期后未到期      ACTIVE          agreement.update
ACTIVE         TERMINATE      双方协商 + 原因必填         TERMINATED      agreement.terminate
ACTIVE         SIGN_CONTRACT  协议生效中 + 附件齐全         CONTRACTED      contract.create
ACTIVE         AUTO_EXPIRE    到期时间 ≤ now              EXPIRED         agreement.expire
EXPIRING       DELAY          经理权限                    ACTIVE          agreement.update
EXPIRING       SIGN_CONTRACT  履约中 + 即将到期             CONTRACTED      contract.create
```

#### 14.3.3 守卫条件（Guard Conditions）

| 守卫 ID | 描述 | 校验逻辑 |
|---|---|---|
| G-A01 | 协议未到期 | `agreement.expire_at > now` |
| G-A02 | 协议未终止 | `agreement.status != TERMINATED` |
| G-A03 | 协议未转合同 | `agreement.status != CONTRACTED` |
| G-A04 | 延期后未超上限 | `延期后.expire_at ≤ 原.expire_at + 5年` |
| G-A05 | 终止原因必填 | `terminate_reason NOT NULL` |

#### 14.3.4 异常路径

```
异常：已终止 → 不能延期/转合同（隐藏对应按钮）
异常：已签合同 → 不能延期/终止（提示"已转合同，请到合同管理"）
异常：即将到期未及时续约 → 自动到期 + 通知员工 + 通知经理
异常：延期超过 5 年 → 拒绝（提示"最长延期 5 年，请重新签约"）
```

### 14.4 客户档案等级状态机（Account Tier State Machine）

**适用实体**：`clue_enterprise.tier` + `enterprise.tier`
**状态字段**：`tier`（A_VIP/B_KEY/C_NORMAL/D_OBSERVE）

#### 14.4.1 等级定义（4 等级）

| 等级 | 标识 | 标准 | 资源倾斜 |
|---|---|---|---|
| `A_VIP` | A 类（VIP） | 注册资本 ≥1 亿 / 营收 ≥10 亿 / 政府重点 / 500 强 | 专属招商经理 + 优先选房 + 租金优惠 |
| `B_KEY` | B 类（重点） | 注册资本 ≥5000 万 / 上市公司 / 高新企业 | 资深招商员 + 标准选房 + 适度优惠 |
| `C_NORMAL` | C 类（普通） | 中小企业 / 一般工商户 | 招商专员 + 标准化流程 |
| `D_OBSERVE` | D 类（观察） | 资质存疑 / 跟进困难 / 多次无效 | 仅维护基本联系 |

#### 14.4.2 等级转移规则（3 个守卫）

| 规则 ID | 描述 | 触发 | 行为 |
|---|---|---|---|
| R-T01 | 自动评估 | 客户创建/资质更新 | 按 §11.7 评分规则自动评估 |
| R-T02 | VIP 不计上限 | A 类客户分配 | 跳过 G-L04 保有量校验 |
| R-T03 | D 类自动回收 | D 类 + 30 天无跟进 | 转入回收站（跳过宽限期） |
| R-T04 | 升级路径 | B→A | 满足 A 类标准 + 经理审核 |
| R-T05 | 降级路径 | A→D | 30 天无跟进 + 多次无效 |

#### 14.4.3 异常路径

```
异常：客户从 A 降级 → 需经理审核 + 通知员工
异常：客户从 D 升级 → 需评估记录 + 经理审核
```

### 14.5 分配规则状态机（Assignment Rule State Machine）

**适用实体**：`biz_setting` + `clue_enterprise`
**控制字段**：`leader_assign_enabled` + `employee_claim_enabled`

#### 14.5.1 状态定义（2 个规则）

| 规则 | 标识 | 启用条件 | 禁用效果 |
|---|---|---|---|
| `LEADER_ASSIGN` | 领导分配 | `leader_assign_enabled=true` | 经理"分配"按钮隐藏/禁用 |
| `EMPLOYEE_CLAIM` | 员工领取 | `employee_claim_enabled=true` | 员工"认领"按钮隐藏/禁用 |

#### 14.5.2 守卫条件

| 守卫 ID | 描述 | 校验逻辑 |
|---|---|---|
| G-AR01 | 至少一个分配规则启用 | `leader_assign_enabled OR employee_claim_enabled` |
| G-AR02 | 拥有量校验 | 员工当前客户数 ≤ max_hold_per_staff |
| G-AR03 | 认领冷却期 | 客户认领后 N 天内不可退回公海 |

#### 14.5.3 异常路径

```
异常：两个规则都禁用 → 系统提示"必须启用至少一个分配规则"（settings.html 保存时校验）
异常：员工已超额 + 启用领取 → 拒绝（提示"已达保有上限 N，请先跟进现有客户"）
```

### 14.6 状态机总览矩阵

| 实体 | 状态数 | 转移数 | 守卫数 | 异常路径 |
|---|---|---|---|---|
| 客户线索 | 7 | 6 | 8 | 4 |
| 商机 | 5 | 5 | 5 | 3 |
| 协议 | 5 | 4 | 5 | 4 |
| 客户档案等级 | 4 | 5 | - | 2 |
| 分配规则 | 2 | - | 3 | 2 |
| **合计** | **23** | **20** | **21** | **15** |

### 14.7 状态机原型实现指南

> **核心原则**：状态机约束必须在 **前端 + 后端** 同时落地，前端负责 UI 阻断，后端负责数据校验。

**前端实现（v1.3 原型）**：

```javascript
// 示例：商机状态机 - 商机详情页 JS 片段
const opportunityStateMachine = {
  states: ['FOLLOWING', 'SIGNED', 'TERMINATED', 'POOL', 'CONTRACTED'],
  transitions: {
    FOLLOWING: {
      signAgreement: { guard: () => true, target: 'SIGNED' },
      terminate: { guard: () => confirmTerminate(), target: 'TERMINATED' },
      returnPool: { guard: () => confirmReturnPool(), target: 'POOL' }
    },
    SIGNED: {
      signContract: { guard: () => isNotExpired() && !isTerminated(), target: 'CONTRACTED' }
    },
    TERMINATED: {} // 终态，无转移
  },
  // UI 联动
  renderUI(state) {
    document.getElementById('signAgreementBtn').style.display = 
      state === 'FOLLOWING' ? 'inline-flex' : 'none'
    document.getElementById('returnPoolBtn').style.display = 
      state === 'FOLLOWING' ? 'inline-flex' : 'none'
    document.getElementById('terminateBtn').style.display = 
      state === 'FOLLOWING' ? 'inline-flex' : 'none'
  }
}
```

**后端实现（落地阶段）**：

```java
// Spring State Machine 配置
@Configuration
@EnableStateMachine
public class OpportunityStateMachineConfig 
    extends StateMachineConfigurerAdapter<OpportunityState, OpportunityEvent> {
  
  @Override
  public void configure(StateMachineStateConfigurer<OpportunityState, OpportunityEvent> states) {
    states.withStates()
      .initial(FOLLOWING)
      .end(TERMINATED)
      .end(CONTRACTED)
      .states(new HashSet<>(Arrays.asList(OpportunityState.values())));
  }
  
  @Override
  public void configure(StateMachineTransitionConfigurer<OpportunityState, OpportunityEvent> transitions) {
    transitions
      .withExternal()
        .source(FOLLOWING).target(SIGNED).event(SIGN_AGREEMENT)
        .guard(notExpiredGuard)
      .and().withExternal()
        .source(FOLLOWING).target(TERMINATED).event(TERMINATE)
        .guard(reasonProvidedGuard);
  }
}
```

---

## 十五、业务状态机约束图（v1.4 重编号）

> **说明**：§14 已详细定义每个状态机，本节为索引视图，便于快速理解全局流转。

### 15.1 客户资源流转总图

```
┌─────────────┐
│  线索入池   │ ← 小程序/自行录入/批量导入/活动收集/转介绍
└─────────────┘
       ↓
┌─────────────┐         ┌─────────────┐
│   公海池    │ ←────── │  客户回收站  │
│  (待分配)   │   复原   │   (30天)    │
└─────────────┘         └─────────────┘
   ↓   ↓   ↑                    ↑ 删除
   │   │   │                    │
分配│ 认领│ 退回公海              │
   │   │   │                    │
   ↓   ↓   └────────────────────┘
┌─────────────┐         ┌─────────────┐
│  我的客户   │ ←───  │   跟进中     │
│  (跟进人)   │  重新   │ (有效/无效) │
└─────────────┘  分配  └─────────────┘
   ↓                       ↓ ↑
转商机                   标记有效/无效
   ↓                       ↓
┌─────────────┐            废弃
│   商机管理   │             ↓
│ (跟进中)    │         线索废弃
└─────────────┘         (联系不上/无意向/无法满足)
   ↓  ↓  ↓  ↓
签订│  │  │ 终止
协议│  │  │  ↓
   ↓  │  │ 已终止
   ↓  │ 退回公海
   ↓  ↓
┌─────────────┐
│   协议管理   │
│  (履约中)   │
└─────────────┘
   ↓  ↓  ↓
延期│  │ 转合同
   │  │  ↓
   │  终止
   │  ↓
   │ 已终止
   ↓
┌─────────────┐
│   合同管理   │
│  (已签订)   │
└─────────────┘
       ↓
   入驻园区
```

### 15.2 业务规则速查表

| 规则 | 触发场景 | 行为 |
|---|---|---|
| **自动回收** | 跟进超期（保留期限 + 宽限期） | 客户自动入公海池 + 记录日志 |
| **保留量校验** | 员工领取客户时 | 当前数 + 新客户 ≤ max_hold_per_staff |
| **成交释放** | 签约成功时 | 释放 1 个保有名额（员工可继续领取） |
| **VIP 不计上限** | A 类客户分配 | 不占员工保有量（独立逻辑） |
| **认领后限制** | 员工认领后 N 天内 | 不可退回公海 |
| **首次跟进时限** | 认领/分配后 | 48 小时内必须有首次跟进，否则自动回收 |
| **复限额设置** | 复原到公海 | 复原后客户可重新被分配/认领 |
| **30 天自动回收** | 无效线索 30 天未跟进 | 自动转入回收站 |

---

## 十六、统一响应格式（v1.5 新增）

> **来源**：基于 v1.4 设计评审 + 用户决策：增强 8 字段（含 traceId + 防篡改 + 分页）

### 16.1 R<T> 统一响应格式

```json
{
  "code": 200,              // 业务码，与 HTTP 状态码同步
  "msg": "success",          // 人可读信息（中文）
  "data": { ... },           // 业务数据（任意 JSON）
  "ts": 1723628400000,      // 客户端发起请求的 timestamp（防客户端篡改）
  "requestId": "uuid-xxx",   // 请求唯一 ID（用于日志关联）
  "traceId": "zipkin-id",    // 链路追踪 ID（Zipkin/Sleuth）
  "serverTime": 1723628400123, // 服务器响应时间
  "page": {                  // 分页信息（仅列表接口有）
    "pageNum": 1,
    "pageSize": 20,
    "total": 128,
    "totalPages": 7
  }
}
```

### 16.2 字段说明

| 字段 | 类型 | 说明 | 必填 |
|---|---|---|---|
| `code` | int | HTTP 状态码复用（200/400/401/403/404/409/422/500/503） | ✅ |
| `msg` | string | 中文错误信息，前端直接展示给用户 | ✅ |
| `data` | object/array | 业务数据，单条记录或列表 | 列表必填 |
| `ts` | long | 客户端请求时间戳（前端从 header X-Ts 传入） | ✅ |
| `requestId` | string | UUID v4，每次请求唯一 | ✅ |
| `traceId` | string | Zipkin/Sleuth 链路追踪 ID | ✅ |
| `serverTime` | long | 服务端响应时间戳（用于客户端时钟校准） | ✅ |
| `page` | object | 分页元数据，仅列表返回 | 列表必填 |

### 16.3 分页规则

**请求参数**：
```
?pageNum=1&pageSize=20&orderBy=create_time&order=desc
```

**响应 page 字段**：
```json
{
  "pageNum": 1,
  "pageSize": 20,
  "total": 128,
  "totalPages": 7,
  "hasNext": true,
  "hasPrev": false
}
```

**约定**：
- `pageNum` 从 1 开始（不是 0）
- `pageSize` 默认 20，最大 200
- `orderBy` 默认 `id desc`，需字段白名单
- 列表为空时返回 `data: []` + `page: { total: 0 }`

### 16.4 请求 Header 规范

| Header | 必填 | 说明 |
|---|---|---|
| `Authorization` | ✅ | `Bearer {token}` |
| `X-Tenant-Id` | ✅ | 当前租户 ID |
| `X-Request-Id` | 自动生成 | 请求唯一 ID（UUID v4） |
| `X-Ts` | 客户端传入 | 请求时间戳（防篡改） |
| `Content-Type` | ✅ | `application/json;charset=UTF-8` |

---

## 十七、错误码规范（v1.5 新增）

> **决策**：采用 **HTTP 状态码复用**（4xx 客户端错误，5xx 服务端错误），前端无需维护单独错误码表。

### 17.1 错误码分类

| HTTP | 语义 | 触发场景 | 处理建议 |
|---|---|---|---|
| **200** | 成功 | 业务处理成功 | - |
| **400** | 请求参数错误 | 必填字段缺失/格式错误/枚举值非法 | 前端校验或后端校验失败 |
| **401** | 未认证 | Token 缺失/失效/伪造 | 跳转登录页 |
| **403** | 无权限 | 角色权限不足/无数据范围 | 提示"无权限访问" |
| **404** | 资源不存在 | 客户/商机/协议 ID 不存在 | 提示"记录不存在" |
| **409** | 状态冲突 | 重复操作/状态机校验失败（如已终止再终止） | 提示业务规则 |
| **422** | 业务规则失败 | 业务校验失败（如保有量超限/认领后不可退回） | 提示具体业务规则 |
| **500** | 系统错误 | 未预期的运行时异常 | 提示"系统繁忙" |
| **503** | 服务不可用 | 依赖下游接口失败 | 提示"服务暂不可用" |

### 17.2 错误响应格式

```json
{
  "code": 409,
  "msg": "该商机已签订协议，不能再退回公海",
  "data": null,
  "ts": 1723628400000,
  "requestId": "uuid-xxx",
  "traceId": "zipkin-id",
  "serverTime": 1723628400123
}
```

### 17.3 业务规则错误（422）明文提示

> **目的**：前端可直接展示 msg 给用户，无需 i18n 翻译。

| 触发条件 | msg 示例 |
|---|---|
| 保有量超限 | "已达保有上限 50，请先跟进现有客户" |
| A 类 VIP 不需校验 | "A 类客户不计入保有上限" |
| 认领冷却期 | "客户认领后 15 天内不可退回公海" |
| 48 小时跟进时限 | "客户分配后 48 小时内必须有首次跟进" |
| 已签合同 | "已转合同，请到合同管理模块" |
| 已终止 | "商机已终止，请新增商机" |
| 协议未到期 | "协议已到期，不能转合同" |
| 协议已终止 | "协议已终止，不能延期/转合同" |
| 延期超过 5 年 | "最长延期 5 年，请重新签约" |

### 17.4 错误日志关联

后端在返回错误响应时，同步记录日志：

```java
log.error("[{}] {} - {}", requestId, code, msg, exception);
// 输出示例：[uuid-xxx] 422 - 该商机已签订协议，不能再退回公海 - StateMachineException
```

---

## 十八、数据字典（v1.5 新增）

> **决策**：3 类字典全部需要（流程状态 + 分类 + 实体属性）

### 18.1 流程状态字典（sys_dict.biz_state_*）

#### 18.1.1 线索状态（lead_status）

| dictValue | dictLabel | 字段说明 |
|---|---|---|
| 0 | 待跟进 | 新录入未触达 |
| 1 | 跟进中 | 已分配/认领 |
| 2 | 已转商机 | 已创建商机 |
| 3 | 已废弃 | 主动废弃 |

#### 18.1.2 线索有效性（valid_flag）

| dictValue | dictLabel |
|---|---|
| 0 | 无效 |
| 1 | 有效 |

#### 18.1.3 公海池状态（pool_status）

| dictValue | dictLabel |
|---|---|
| 1 | 在公海池 |
| 2 | 在我的客户 |
| 3 | 在回收站 |

#### 18.1.4 商机状态（opportunity.status）

| dictValue | dictLabel |
|---|---|
| 1 | 跟进中 |
| 2 | 已签协议 |
| 3 | 已签合同 |
| 4 | 已终止 |

#### 18.1.5 协议状态（agreement.status）

| dictValue | dictLabel |
|---|---|
| 1 | 履约中 |
| 2 | 即将到期（30 天内） |
| 3 | 已终止 |
| 4 | 已签合同 |
| 5 | 已到期 |

#### 18.1.6 合同状态（contract.status）

| dictValue | dictLabel |
|---|---|
| 1 | 生效中 |
| 2 | 已到期 |
| 3 | 已违约 |

### 18.2 分类字典（sys_dict.biz_category_*）

#### 18.2.1 客户来源（source）

| dictValue | dictLabel |
|---|---|
| 1 | 自行录入 |
| 2 | 小程序登记 |
| 3 | 批量导入 |
| 4 | 活动收集 |
| 5 | 转介绍 |
| 6 | 公海分配 |

#### 18.2.2 跟进方式（communication.type）

| dictValue | dictLabel |
|---|---|
| PHONE | 电话 |
| WECHAT | 微信 |
| VISIT | 上门拜访 |
| EMAIL | 邮件 |
| SHOW | 带看 |
| BOOK | 预约看房 |
| OTHER | 其他 |

#### 18.2.3 业务类型（business_type）

| dictValue | dictLabel |
|---|---|
| LEASE | 租赁业务 |
| SALE | 销售业务 |

#### 18.2.4 线索废弃原因（abandon_reason）

| dictValue | dictLabel |
|---|---|
| 1 | 联系不上 |
| 2 | 无意向 |
| 3 | 无法满足客户需求 |
| 4 | 客户已选其他园区 |
| 5 | 客户预算不足 |
| 99 | 其他 |

#### 18.2.5 商机终止原因（terminate_reason）

| dictValue | dictLabel |
|---|---|
| 1 | 客户选择其他园区 |
| 2 | 客户预算不足 |
| 3 | 客户决策延迟 |
| 4 | 竞争对手胜出 |
| 99 | 其他 |

#### 18.2.6 协议终止原因（agreement_terminate_reason）

| dictValue | dictLabel |
|---|---|
| 1 | 客户违约 |
| 2 | 双方协商 |
| 3 | 客户经营困难 |
| 4 | 园区规划调整 |
| 99 | 其他 |

### 18.3 实体属性字典（sys_dict.biz_attribute_*）

#### 18.3.1 联系人角色（contact.role）

| dictValue | dictLabel | 说明 |
|---|---|---|
| DECISION | 决策者 | 有最终决策权 |
| INFLUENCE | 影响者 | 可影响决策 |
| USER | 使用者 | 实际使用者 |
| AGENT | 经办人 | 流程对接人 |

#### 18.3.2 联系人立场（contact.stance）

| dictValue | dictLabel |
|---|---|
| SUPPORT | 支持 |
| NEUTRAL | 中立 |
| OPPOSE | 反对 |

#### 18.3.3 客情关系（contact.relationship）

| dictValue | dictLabel |
|---|---|
| CLOSE | 密切 |
| GOOD | 良好 |
| NORMAL | 一般 |
| WEAK | 疏远 |

#### 18.3.4 接触状态（contact.contact_status）

| dictValue | dictLabel |
|---|---|
| FIRST | 首次接触 |
| MULTIPLE | 多次接触 |

#### 18.3.5 客户分级（clue_enterprise.tier / enterprise.tier）

| dictValue | dictLabel | 标准 |
|---|---|---|
| A | A 类（VIP） | 注册≥1亿 / 营收≥10亿 / 政府重点 |
| B | B 类（重点） | 注册≥5000万 / 上市公司 / 高新 |
| C | C 类（普通） | 中小企业 / 一般工商户 |
| D | D 类（观察） | 资质存疑 / 跟进困难 |

#### 18.3.6 日程类型（schedule.type）

| dictValue | dictLabel |
|---|---|
| VISIT | 客户拜访 |
| MEETING | 开会 |
| OTHER | 其他 |

#### 18.3.7 商机 7 阶段（opportunity.stage）

| dictValue | dictLabel | 进入条件 |
|---|---|---|
| LEAD | 线索入池 | 默认 |
| QUALIFY | 资质验证 | 完成首次跟进 |
| NEEDS | 需求确认 | 资质通过 |
| SOLUTION | 方案匹配 | 需求明确 |
| VISIT | 带看考察 | 方案提交 |
| NEGOTIATION | 谈判协商 | 带看完成 |
| WON | 协议签订 | 协议生效 |

### 18.4 字典初始化 SQL（Flyway V3）

```sql
-- 流程状态
INSERT INTO sys_dict (dict_type, dict_value, dict_label, sort_order) VALUES
('biz_state_lead', '0', '待跟进', 1),
('biz_state_lead', '1', '跟进中', 2),
('biz_state_lead', '2', '已转商机', 3),
('biz_state_lead', '3', '已废弃', 4);

INSERT INTO sys_dict (dict_type, dict_value, dict_label, sort_order) VALUES
('biz_state_opportunity', '1', '跟进中', 1),
('biz_state_opportunity', '2', '已签协议', 2),
('biz_state_opportunity', '3', '已签合同', 3),
('biz_state_opportunity', '4', '已终止', 4);

-- （此处省略其他字典，完整 SQL 见 V3__seed_dicts.sql）
```

---

## 十九、权限矩阵（v1.5 新增）

> **决策**：采用**通用 @RequiresPermissions 注解 + 资源 owner 校验**双重控制。

### 19.1 角色定义

| 角色编码 | 角色名称 | 职责 |
|---|---|---|
| `biz_staff` | 招商专员 | 我的客户、商机、协议、统计 |
| `biz_manager` | 招商经理 | + 公海池（分配/删除）、基础设置 |
| | `biz_admin` | 运营管理员 | + 全部数据范围 + 系统配置 |
| | `biz_viewer` | 只读访客 | 看板 + 统计（无修改权限） |

### 19.2 权限矩阵（角色 × 模块 × 操作）

#### 19.2.1 公海池（pool）

| 操作 | biz_staff | biz_manager | biz_admin | biz_viewer |
|---|---|---|---|---|
| `business:pool:view` | ✅ 自己入池 | ✅ 全部 | ✅ 全部 | ✅ 全部 |
| `business:pool:add` | ✅ | ✅ | ✅ | ❌ |
| `business:pool:assign` | ❌ | ✅ | ✅ | ❌ |
| `business:pool:claim` | ✅ | ✅ | ✅ | ❌ |
| `business:pool:delete` | ❌ | ✅ | ✅ | ❌ |
| `business:pool:log` | ❌ | ✅ | ✅ | ✅ |

#### 19.2.2 我的客户（my-customer）

| 操作 | biz_staff | biz_manager | biz_admin | biz_viewer |
|---|---|---|---|---|
| `business:my-customer:view` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ✅ 全部 |
| `business:my-customer:add` | ✅ | ✅ | ✅ | ❌ |
| `business:my-customer:contact` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:my-customer:mark` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:my-customer:abandon` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:my-customer:return-pool` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:my-customer:convert` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |

#### 19.2.3 商机管理（opportunity）

| 操作 | biz_staff | biz_manager | biz_admin | biz_viewer |
|---|---|---|---|---|
| `business:opportunity:view` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ✅ 全部 |
| `business:opportunity:followup` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:opportunity:schedule` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:opportunity:competitor` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:opportunity:terminate` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:opportunity:return-pool` | ✅ 自己的 | ✅ 全部 | ✅ 全部 | ❌ |
| `business:opportunity:agreement` | ❌ | ✅ | ✅ | ❌ |

#### 19.2.4 协议管理（agreement）

| 操作 | biz_staff | biz_manager | biz_admin | biz_viewer |
|---|---|---|---|---|
| `business:agreement:view` | ✅ 关联自己商机 | ✅ 全部 | ✅ 全部 | ✅ 全部 |
| `business:agreement:delay` | ❌ | ✅ | ✅ | ❌ |
| `business:agreement:terminate` | ❌ | ✅ | ✅ | ❌ |
| `business:agreement:contract` | ❌ | ✅ | ✅ | ❌ |

#### 19.2.5 其他模块

| 操作 | biz_staff | biz_manager | biz_admin | biz_viewer |
|---|---|---|---|---|
| `business:recycle:*` | ❌ | ✅ | ✅ | ✅ |
| `business:customer:view` | ✅ 关联自己 | ✅ 全部 | ✅ 全部 | ✅ 全部 |
| `business:statistics:view` | ✅ 个人 | ✅ | ✅ | ✅ |
| `business:settings:view` | ❌ | ✅ | ✅ | ❌ |
| `business:settings:edit` | ❌ | ❌ | ✅ | ❌ |

### 19.3 资源 owner 校验（数据范围）

> 即使有权限，仍需校验资源归属。

#### 19.3.1 校验规则

| 操作 | 校验 |
|---|---|
| 我的客户 - 编辑 | `clue_enterprise.follower_id = current_user_id` |
| 商机 - 跟进 | `opportunity.owner_id = current_user_id` |
| 商机 - 终止 | `opportunity.owner_id = current_user_id OR biz_manager+` |
| 商机 - 退回公海 | `opportunity.owner_id = current_user_id` |
| 协议 - 延期/终止 | `biz_manager+`（任何角色都不行，需经理） |
| 协议 - 转合同 | `biz_manager+` |
| 公海池 - 分配 | `biz_manager+` |
| 公海池 - 认领 | `clue_enterprise.pool_status = 1 AND 保有量未超限` |
| 基础设置 - 编辑 | `biz_admin only` |

#### 19.3.2 数据范围拦截器

```java
@Component
public class DataScopeInterceptor implements DataPermissionHandler {
  
  public boolean hasPermission(Authentication auth, Object resource, String operation) {
    String role = auth.getRole();
    if (role.contains("biz_admin")) return true;  // 全部数据
    
    Long ownerId = getOwnerId(resource);
    if (role.contains("biz_manager")) {
      return ownerDeptId(resource).equals(auth.getDeptId());  // 同部门
    }
    if (role.contains("biz_staff")) {
      return ownerId.equals(auth.getUserId());  // 自己的
    }
    return false;
  }
}
```

### 19.4 前端权限联动（v1.3 状态机联动）

```javascript
// opportunity-detail.html - 顶部操作按钮
function renderOpActions(state, role, ownerId) {
  const isOwner = ownerId === currentUserId
  const isManager = role.includes('manager')
  
  document.getElementById('signAgreementBtn').style.display = 
    state === 'FOLLOWING' ? 'inline-flex' : 'none'
  document.getElementById('returnPoolBtn').style.display = 
    (state === 'FOLLOWING' && isOwner) ? 'inline-flex' : 'none'
  document.getElementById('terminateBtn').style.display = 
    (state === 'FOLLOWING' && (isOwner || isManager)) ? 'inline-flex' : 'none'
}
```

---

## 二十、数据库迁移脚本（v1.5 新增）

> **决策**：归口 park-business 模块（待新建于 `code/platform-server/park-business/`），V 编号 V1-V9。

### 20.1 脚本清单

| V 编号 | 文件 | 说明 | 依赖 |
|---|---|---|---|
| V1 | `V1__init_clue_enterprise.sql` | 线索客户主表 | park-enterprise V53 |
| V2 | `V2__init_opportunity.sql` | 商机主表 + 7 阶段字段 | V1 |
| V3 | `V3__init_agreement.sql` | 协议主表 + 4 状态字段 | V2 |
| V4 | `V4__init_competitor.sql` | 竞争对手表 | V2 |
| V5 | `V5__init_schedule.sql` | 日程表 | V2 |
| V6 | `V6__init_pool_log.sql` | 公海日志表 | V1 |
| V7 | `V7__init_biz_setting.sql` | 基础设置表 | - |
| V8 | `V8__init_indexs.sql` | 索引优化 | V1-V7 |
| V9 | `V9__seed_dicts.sql` | 字典 seed（见 §18.4） | - |

### 20.2 V1 主表示例（线索客户）

```sql
CREATE TABLE pai_business.clue_enterprise (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID（多租户）',
  park_id BIGINT COMMENT '园区 ID',
  name VARCHAR(200) NOT NULL COMMENT '客户名称',
  contact_name VARCHAR(50) NOT NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NOT NULL COMMENT '手机号',
  source INT NOT NULL COMMENT '客户来源（dict.biz_source）',
  follower_id BIGINT COMMENT '跟进人 ID（sys_user）',
  lead_status INT DEFAULT 0 COMMENT '线索状态',
  valid_flag TINYINT DEFAULT 1 COMMENT '有效性：0=无效 1=有效',
  pool_status INT DEFAULT 1 COMMENT '公海状态：1=公海池 2=我的客户 3=回收站',
  pool_in_time DATETIME COMMENT '入池时间',
  pool_out_time DATETIME COMMENT '出池时间',
  tier VARCHAR(2) COMMENT '客户分级：A/B/C/D',
  lead_score INT DEFAULT 0 COMMENT '线索评分（0-100）',
  retention_start DATETIME COMMENT '保留期开始时间',
  abandon_reason INT COMMENT '废弃原因',
  abandon_at DATETIME COMMENT '废弃时间',
  recycled_at DATETIME COMMENT '回收时间',
  create_by BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time DATETIME,
  del_flag TINYINT DEFAULT 0 COMMENT '软删除',
  PRIMARY KEY (id),
  KEY idx_park (park_id),
  KEY idx_follower (follower_id),
  KEY idx_pool_status (pool_status, lead_status),
  KEY idx_source (source),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线索客户表';
```

### 20.3 V9 seed 数据示例

```sql
-- 基础设置默认值
INSERT INTO pai_business.biz_setting (park_id, retention_days, grace_days, 
  leader_assign_enabled, employee_claim_enabled, 
  max_hold_per_staff, release_after_deal, create_time)
VALUES (1, 30, 7, 1, 1, 50, 1, NOW());

-- 字典 seed（见 §18.4 完整 SQL）
```

### 20.4 增量脚本（V10+）策略

- **触发时机**：V1-V9 上线后，每次表结构/字典变更
- **命名规范**：`V{版本号}__{变更说明}.sql`，如 `V10__add_opportunity_stage.sql`
- **禁止**：跨多个 V 合并（一个 PR 对应一个 V）
- **回滚**：通过 Flyway 自带的 `repair` 命令，不单独维护 R 脚本

---

## 二十一、测试用例（v1.5 新增）

> **决策**：状态机 23×20 覆盖表 + E2E 场景脚本

### 21.1 状态机测试覆盖表

**目标**：每个状态 × 每个转移至少 1 个 happy path + 1 个异常测试

#### 21.1.1 客户线索状态机（7 状态 × 6 转移 = 12 测试）

| 编号 | 源状态 | 事件 | 期望目标 | 类型 | 验证点 |
|---|---|---|---|---|---|
| TC-L01 | POOL_PENDING | ASSIGN | POOL_ASSIGNED | happy | 分配后状态 + 跟进人变更 |
| TC-L02 | POOL_PENDING | ASSIGN | (拒绝) | 异常 | 经理无权限 |
| TC-L03 | POOL_PENDING | CLAIM | POOL_CLAIMED | happy | 员工认领成功 |
| TC-L04 | POOL_PENDING | CLAIM | (拒绝) | 异常 | 保有量超限 |
| TC-L05 | POOL_PENDING | AUTO_RECYCLE | RECYCLED | happy | 24h 无操作 |
| TC-L06 | POOL_ASSIGNED | FIRST_FOLLOWUP | FOLLOWING_VALID | happy | 48h 内有跟进 |
| TC-L07 | POOL_ASSIGNED | FIRST_FOLLOWUP | RECYCLED | 异常 | 超 48h 自动回收 |
| TC-L08 | POOL_CLAIMED | FIRST_FOLLOWUP | FOLLOWING_VALID | happy | 同 TC-L06 |
| TC-L09 | FOLLOWING_VALID | MARK_INVALID | FOLLOWING_INVALID | happy | 标记无效 |
| TC-L10 | FOLLOWING_INVALID | ABANDON | ABANDONED | happy | 废弃成功（需原因） |
| TC-L11 | FOLLOWING_INVALID | ABANDON | (拒绝) | 异常 | 未填原因 |
| TC-L12 | FOLLOWING_INVALID | AUTO_RECYCLE | RECYCLED | happy | 30 天自动回收 |

#### 21.1.2 商机状态机（5 状态 × 5 转移 = 10 测试）

| 编号 | 事件 | 期望 | 类型 |
|---|---|---|---|
| TC-O01~O05 | FOLLOWING → SIGNED | 协议签订成功 | happy |
| TC-O06~O08 | FOLLOWING → TERMINATED | 商机终止（含原因） | happy + 异常（无原因） |
| TC-O09 | FOLLOWING → POOL | 退回公海 | happy |
| TC-O10 | SIGNED → CONTRACTED | 转合同 | happy |
| TC-O11 | SIGNED → FOLLOWING | 协议终止 → 商机回退 | 边界 |
| TC-O12 | TERMINATED → (any) | (拒绝) | 异常（终态） |

#### 21.1.3 协议状态机（5 状态 × 4 转移 = 8 测试）

类似覆盖：ACTIVE → EXPIRING（自动）、DELAY、TERMINATE、SIGN_CONTRACT、AUTO_EXPIRE

### 21.2 E2E 场景脚本（5 个核心场景）

#### E2E-01：客户完整生命周期（happy path）

```
Given 招商经理新建客户"深圳 XX 公司"（公海池）
When 张三认领该客户
And 张三在 48 小时内完成首次跟进
Then 客户变为"跟进中-有效"
When 张三创建商机（业务类型=租赁）
And 商机转为协议
Then 商机状态变为"已签协议"，协议状态为"履约中"
When 经理将协议转合同
Then 协议状态变为"已签合同"，商机状态为"已签合同"
```

#### E2E-02：商机终止回退

```
Given 商机 A 状态为"跟进中"
When 员工点击"商机终止"+ 输入原因"客户预算不足"
Then 商机状态变为"已终止"
And 顶部"签订协议/退回公海"按钮隐藏
And 时间轴新增"商机终止"节点
```

#### E2E-03：保有量超限拒绝

```
Given 员工张三当前持有 50 个客户（已达 max_hold_per_staff）
When 张三尝试从公海池认领新客户
Then 提示"已达保有上限 50，请先跟进现有客户"
And 客户未被认领
```

#### E2E-04：48 小时跟进时限自动回收

```
Given 客户 X 被分配给员工张三（分配时间 2026-08-12 10:00）
When 时间到达 2026-08-14 10:00（48h 后）仍未有跟进
Then 客户自动入公海池
And 通知员工张三 + 经理
And 公海日志新增"自动回收"记录
```

#### E2E-05：协议转合同状态约束

```
Given 协议 A 状态为"已终止"
When 员工尝试转合同
Then 拒绝，提示"协议已终止，不能转合同"
And 转合同按钮隐藏
```

---

## 二十二、实施计划（v1.4 重编号）

> **目标**：完成 18 项缺失交互 + 4 项状态机 + 9 项状态联动，共 31 项修复。

### 22.1 实施分批

**批次 1（P0 高优先级，必须完成）**：7 项
- M-01 行点击事件冒泡修复（opportunity.html）
- M-02 商机管理 5 个按钮全部绑定（customer-detail.html）
- M-03 签订协议关联商机数据（opportunity-detail.html）
- M-04 商机终止状态联动（opportunity-detail.html）
- M-05 协议状态约束（agreement-detail.html）
- M-12 批量分配传入选中数据（pool.html）
- M-13 单条认领状态变更完整（pool.html）

**批次 2（P1 中优先级）**：8 项
- M-06 导出报表+钻取逻辑（statistics.html）
- M-07 漏斗图阶段可点击钻取（statistics.html）
- M-08 排行榜员工可点击（statistics.html）
- M-09 开关保存后联动（settings.html）
- M-10 输入校验（settings.html）
- M-11 保存视觉反馈（settings.html）
- M-14 批量操作状态联动（my-customer.html）
- M-15 复原视觉反馈（recycle.html）

**批次 3（P2 低优先级，业务增强）**：3 项
- M-16 带看后自动建议日程（opportunity-detail.html）
- M-17 添加联系人后立即刷新（customer-detail.html）
- M-18 列表行翻页/排序/筛选逻辑（多个列表页）

**批次 4（业务状态机）**：13 项
- S-01~S-04 状态机 16 个约束（覆盖 4 个实体）
- L-01~L-09 状态联动 9 个规则

### 22.2 文件修改清单

| 文件 | 修改项数 | 影响 |
|---|---|---|
| opportunity.html | 1 | M-01 |
| customer-detail.html | 2 | M-02, M-17 |
| opportunity-detail.html | 4 | M-03, M-04, M-16 + 状态约束 |
| agreement-detail.html | 2 | M-05 + 状态约束 |
| agreement.html | 0 | 已 OK（仅需复核） |
| pool.html | 2 | M-12, M-13 |
| my-customer.html | 1 | M-14 |
| recycle.html | 1 | M-15 |
| statistics.html | 3 | M-06, M-07, M-08 |
| settings.html | 3 | M-09, M-10, M-11 |
| pool-log.html | 0 | 仅展示 OK |
| **合计** | **18 项** | **10 个文件** |

### 22.3 实施步骤（按优先级）

**Step 1（批次 1，~30 分钟）**：修复 P0 关键交互
- opportunity.html：拆分 onclick 行点击 vs 按钮点击
- customer-detail.html：商机管理 6 个按钮全部绑定到对应方法
- opportunity-detail.html：签订协议前置弹窗 + 商机终止状态联动
- agreement-detail.html：状态约束（已终止隐藏按钮）
- pool.html：批量分配传数据 + 认领完整状态变更

**Step 2（批次 2，~20 分钟）**：业务增强
- statistics.html：导出/钻取/排行榜交互
- settings.html：开关联动 + 输入校验 + 保存反馈
- my-customer.html：批量操作状态联动
- recycle.html：复原视觉反馈

**Step 3（批次 3 + 4，~20 分钟）**：业务规则增强
- opportunity-detail.html：带看联动日程
- customer-detail.html：添加联系人立即刷新
- 列表页：翻页/排序/筛选逻辑
- 业务状态机：在每个页面的 JS 中增加状态校验

**Step 4（验证）**：执行 §13.5 验收 Checklist 全部项通过

### 22.4 实施原则

1. **数据流动而非死链**：每个按钮都要触发数据变更（mock），不是仅弹窗或跳转
2. **状态可见性**：操作后立即反映在 UI（按钮变灰/列表移除/状态徽章变红）
3. **业务规则前置**：弹窗前先校验当前状态（如已终止协议不能再延期）
4. **错误处理**：所有可能失败的操作都有提示
5. **跨页一致性**：列表页操作后详情页状态同步

### 22.5 验证方法

```bash
# 1. 统计 alert() 调用次数（应 < 5）
grep -c "alert(" prototype/park-business/*.html

# 2. 验证所有按钮都有 onclick 或 type="submit"
grep -c 'class="btn[^>]*"' prototype/park-business/*.html
grep -c 'onclick=' prototype/park-business/*.html

# 3. 验证 confirm/prompt 覆盖所有危险操作
grep -c "confirm(" prototype/park-business/*.html

# 4. 验证 state 字段已被使用
grep -c "state\|status" prototype/park-business/*.html
```

---

## 二十三、版本历史（v1.5 终稿）

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-14 | 初版：基于脑图（智慧招商 CRM 架构）+ csyh 业务代码 |
| v1.1 | 2026-08-14 | 升级：增加验收标准 + 节点映射表 + 覆盖率统计 |
| v1.2 | 2026-08-14 | 核心补充：行业 CRM 对标（Salesforce/HubSpot/销售易/纷享销客/Zoho），补全 11 个行业缺失项 |
| v1.2.1 | 2026-08-14 | §11.8 调整：微信集成 → 移动端集成（不限定微信） |
| v1.3 | 2026-08-14 | 交互完整性：§13 交互矩阵 73 项验收点 + 业务状态机 + 实施计划（18 项修复） |
| v1.4 | 2026-08-14 | 状态机详细规范：新增 §14 业务状态机详细规范（5 个状态机 + 20 转移 + 21 守卫） |
| **v1.5** | 2026-08-14 | **开发可参考级**：§十六 R<T> 增强响应 8 字段 + §十七 HTTP 错误码规范 + §十八 数据字典全 3 类 + §十九 权限矩阵（4 角色×8 模块）+ §二十 数据库迁移 V1-V9 + §二十一 测试用例（状态机覆盖表 + 5 个 E2E） |

---

_本设计文档 v1.5 是 park-business 模块原型的完整设计依据。_

_验收方法：§3 验收清单 + §13.5 交互验收 Checklist + §14 状态机约束 + §15 实施计划。_

_所有 73 项验收点全部通过 = 设计+原型 双重通过。_

---

_本设计文档是 park-business 模块原型的设计依据。所有 HTML 原型必须遵循本文档定义的菜单结构、字段命名、API 设计、权限点。_

_验收方法：§ 三 验收清单 + § 四 工具验证命令，运行后所有项项通过 = 验收通过。_

_v1.5 验收清单覆盖范围：§3 验收清单 + §13.5 交互验收 Checklist (73 项) + §14 状态机约束 (5 状态机 20 转移 21 守卫) + §二十 Flyway 迁移清单 (V1-V9) + §二十一 测试用例 (状态机覆盖表 + 5 E2E 场景)。_