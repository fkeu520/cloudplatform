# 招商管理 (park-business) — 原型索引

> **版本**: v1.0
> **日期**: 2026-08-14
> **设计规范**: [Apple DESIGN.md](D:\work\AI\openclaw\skills\design-md\apple\DESIGN.md)
> **详细设计**: [DESIGN.md](./DESIGN.md)
> **来源**: csyh 业务代码迁移 + 智慧招商 CRM 业务脑图

---

## 一、模块定位

智慧招商是云枢中台的 **CRM 招商管理** 模块，覆盖 **"公海池 → 我的客户 → 商机转化 → 协议签订 → 合同签订"** 全流程。

**核心场景**：

| 角色 | 主用页面 |
|---|---|
| **招商专员** | 我的客户、商机管理、协议管理、招商统计 |
| **招商经理** | 公海池（分配）、基础设置、招商统计、客户档案 |
| **运营管理员** | 公海池（删/恢复）、基础设置、招商统计 |

---

## 二、文件清单

| 文件 | 类型 | 状态 | 说明 |
|---|---|---|---|
| `README.md` | 文档 | ✅ | 本索引 |
| `DESIGN.md` | 文档 | ✅ | 详细设计（业务架构/字段/API/权限） |
| `pool.html` | 页面 | ✅ | 公海池（侧边栏） |
| `my-customer.html` | 页面 | ✅ | 我的客户（侧边栏） |
| `customer-detail.html` | 页面 | ✅ | 客户档案（侧边栏+详情） |
| `pool-log.html` | 页面 | ✅ | 公海日志（公海池入口跳转） |
| `opportunity.html` | 页面 | ✅ | 商机管理列表（侧边栏） |
| `opportunity-detail.html` | 页面 | ✅ | 商机详情（跟进/日程/竞争对手/协议 Tab） |
| `agreement.html` | 页面 | ✅ | 协议管理列表（侧边栏） |
| `agreement-detail.html` | 页面 | ✅ | 协议详情（延期/终止/转合同） |
| `recycle.html` | 页面 | ✅ | 客户回收站（侧边栏） |
| `statistics.html` | 页面 | ✅ | 招商统计（漏斗+线索+来源） |
| `settings.html` | 页面 | ✅ | 基础设置（保留期限/分配/保有规则） |

---

## 三、侧边栏菜单（在 `prototype/index.html` 注册）

```js
'park-business': {
  title: '招商管理',
  items: [
    { id: 'pool',        name: '公海池',     icon: '🌐', url: 'park-business/pool.html' },
    { id: 'my-customer', name: '我的客户',   icon: '👥', url: 'park-business/my-customer.html' },
    { id: 'opportunity', name: '商机管理',   icon: '💼', url: 'park-business/opportunity.html' },
    { id: 'agreement',   name: '协议管理',   icon: '📄', url: 'park-business/agreement.html' },
    { id: 'recycle',     name: '回收站',     icon: '♻️', url: 'park-business/recycle.html' },
    { id: 'archive',     name: '客户档案',   icon: '📁', url: 'park-business/customer-detail.html' },
    { id: 'statistics',  name: '招商统计',   icon: '📊', url: 'park-business/statistics.html' },
    { id: 'settings',    name: '基础设置',   icon: '⚙️', url: 'park-business/settings.html' },
  ]
},
```

> **注意**: 全部 11 个 HTML 已落盘并完成注册（`index.html` `park-business` 模块已包含 8 项菜单,包括 `opportunity` / `agreement`）。

---

## 四、业务流程图

```
[公海池]  ──分配/认领──>  [我的客户]  ──商机转化──>  [商机管理]
   ↑                        │                          │
   │                        │                          ├──跟进/沟通/带看
   │  退回公海                │  标记有效/无效            ├──日程管理
   │                        │  线索废弃                ├──竞争对手
   │                        ↓                          ├──商机终止
   │                     [回收站]                       └──签订协议
   │                                                    ↓
   └──────────  复原进入公海池 ←──────────────  [协议管理]
                                                       ├──协议延期
                                                       ├──协议终止
                                                       └──签订合同
```

---

## 五、关键字段参考（详见 DESIGN.md §四）

| 实体 | 表名 | 核心字段 |
|---|---|---|
| 线索客户 | `clue_enterprise` | id, name, contact_name, source, follower_id, lead_status, valid_flag, pool_status |
| 联系人 | `contacts` | enterprise_id, name, role, stance, relationship, contact_status |
| 商机 | `opportunity` | enterprise_id, business_type(LEASE/SALE), house_id, demand_area, intend_price |
| 跟进 | `communication` | enterprise_id, type, content, followup_time |
| 竞争对手 | `competitor` | opportunity_id, name, info, progress |
| 日程 | `schedule` | opportunity_id, type, title, start_time |
| 协议 | `agreement` | opportunity_id, type, status, sign_time, expire_time |
| 合同 | `contract` | agreement_id, status, sign_time, file_url |
| 基础设置 | `biz_setting` | retention_days, grace_days, max_hold_per_staff |

---

## 六、引用规范

每个 HTML 顶部必须包含：

```html
<!--
模块: park-business
页面: <page-name>
版本: v1.0

数据源:
- <table>: <fields>

API:
- <METHOD> <path>

权限:
- <perm:xxx>
-->
```

按钮权限标注：

```html
<!-- {{perm:business:pool:assign}}> -->
<button class="btn btn-primary">分配</button>
```

字段来源标注：

```html
<div class="label">企业名称 <span class="field-source">clue_enterprise.name</span></div>
```

---

## 七、版本历史

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-14 | 初版：基于智慧招商 CRM 业务脑图 + csyh 业务代码迁移 |

---

_所有 prototype 文件遵循 `prototype/_assets/common.css` 设计规范，可直接用浏览器打开（无后端依赖）。_