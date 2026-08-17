# 云枢中台 - Prototype 原型库

**版本**: v1.0
**日期**: 2026-07-01
**样式规范**: [Apple DESIGN.md](D:\work\AI\openclaw\skills\design-md\apple\DESIGN.md)（适配中台场景）
**完整工作流规范**: [doc/spec/prototype-工作流规范.md](../doc/spec/prototype-工作流规范.md)

---

## 一、目录结构

```
prototype/
├── README.md                        # 本文件（索引）
├── index.html                       # 统一框架页（顶部菜单 + 左侧菜单 + 主体区）
├── login.html                       # 登录页（独立于框架）
├── mobile.html                      # 移动端适配示例
│
├── _assets/                         # 设计资产
│   ├── common.css                   # 公共样式（设计 tokens + 组件样式）
│   ├── framework.css                # 框架样式（topnav + sidemenu + breadcrumb）
│   └── README.md                    # 组件库索引
│
├── _archive/                        # 旧 prototype（待归档）
│   ├── pc-phase1/                   # 8 HTML（dashboard/user/role/menu/dict/org/oper-log/config）
│   ├── pc-phase2/                   # 8 HTML（workflow-* + theme + preview + portal-manage）
│   └── mobile/                      # 14 HTML + 2 root
│
├── park-enterprise/                 # 企业档案
│   ├── list.html                    # 列表页（含 Excel 导入导出按钮）
│   ├── detail.html                  # 详情页（7 Tab + 编辑权限标注）
│   ├── integration.html             # 集成设置（4 服务商 × 4 区块）
│   ├── profile.html                 # 客户画像（RFM + 健康度 + 时间轴）
│   ├── industry.html                # 行业类型（树形分类 + 子分类管理）
│   ├── focus.html                   # 关注标签（6 分类 × 42 标签）
│   └── rating.html                  # 评分规则（5 维度 × 24 规则）
│
├── park-space/                      # 空间管理（待原型）
├── park-contract/                   # 合同（待原型）
├── park-property/                   # 物业（待原型）
├── park-business/                   # 招商（待原型）
├── park-finance/                    # 财务（待原型）
├── park-service/                    # 服务中心（待原型）
├── workflow/                        # 流程（待原型）
├── message/                         # 消息（待原型）
├── user/                            # 用户/权限（待原型）
└── kefu/                            # 智能问答（待原型）
```

---

## 二、文件分类

### 2.1 第 1 层：登录层（独立）

| 文件 | 角色 | 引用 |
|------|------|------|
| `login.html` | 登录页 | `_assets/common.css` |

### 2.2 第 2 层：框架层（全局唯一）

| 文件 | 角色 | 引用 |
|------|------|------|
| `index.html` | 统一框架（顶部 + 左侧 + 主体 iframe） | `_assets/common.css` + `_assets/framework.css` |

**所有模块的菜单入口必须放在 `index.html`**（数据驱动，定义在 `moduleMenus` 对象）。

### 2.3 第 3 层：内容层（按模块）

| 模块 | 路径 | 引用 |
|------|------|------|
| 企业档案 | `park-enterprise/*.html` | `../_assets/common.css` |
| 空间管理 | `park-space/*.html` | `../_assets/common.css` |
| ... | ... | ... |

**内容页不包含 topnav/sidemenu**（避免重复），假设 `index.html` 框架已存在。

---

## 三、访问入口

### 3.1 标准路径

```
1. 打开 prototype/login.html     → 登录页
2. 登录成功 → 跳转 prototype/index.html  → 统一框架
3. 点击顶部模块（企业档案）→ 切换左侧菜单
4. 点击左侧菜单（企业列表）→ iframe 加载 park-enterprise/list.html
5. 点击企业名称 → iframe 加载 park-enterprise/detail.html?id=N
```

### 3.2 移动端预览

```
打开 prototype/mobile.html → 切换设备（iPhone/iPad/Android）→ 查看移动端布局
```

### 3.3 直接访问（绕过框架）

```
打开 prototype/park-enterprise/list.html   → 直接查看列表页（无框架）
打开 prototype/park-enterprise/detail.html → 直接查看详情页（无框架）
```

---

## 四、模块原型状态

| 模块 | 状态 | 链接 |
|------|------|------|
| **park-enterprise** 企业档案 | ✅ v1.0 | [list.html](park-enterprise/list.html) · [detail.html](park-enterprise/detail.html) · [integration.html](park-enterprise/integration.html) · [profile.html](park-enterprise/profile.html) · [industry.html](park-enterprise/industry.html) · [focus.html](park-enterprise/focus.html) · [rating.html](park-enterprise/rating.html) |
| park-space 空间管理 | 🔵 待原型 | - |
| park-contract 合同 | 🔵 待原型 | - |
| park-property 物业 | 🔵 待原型 | - |
| **park-business** 招商 | ✅ v1.5 (设计阶段) | [pool.html](park-business/pool.html) · [my-customer.html](park-business/my-customer.html) · [opportunity.html](park-business/opportunity.html) · [opportunity-detail.html](park-business/opportunity-detail.html) · [agreement.html](park-business/agreement.html) · [agreement-detail.html](park-business/agreement-detail.html) · [recycle.html](park-business/recycle.html) · [customer-detail.html](park-business/customer-detail.html) · [statistics.html](park-business/statistics.html) · [settings.html](park-business/settings.html) · [pool-log.html](park-business/pool-log.html) · [DESIGN.md](park-business/DESIGN.md) |

> **park-business v1.5 设计文档**：含 23 章（业务架构/状态机/响应格式/错误码/数据字典/权限矩阵/数据库迁移/测试用例）。**当前为设计阶段** — 已识别 18 项缺失交互（M-01~M-18）及 §二十 Flyway V1-V9 脚本/后端 park-business 模块未实施,详见 [DESIGN.md](park-business/DESIGN.md) |
| park-finance 财务 | 🔵 待原型 | - |
| park-service 服务中心 | 🔵 待原型 | - |
| workflow 流程 | 🔵 待原型 | - |
| message 消息 | 🔵 待原型 | - |
| user 用户/权限 | 🔵 待原型 | - |
| **kefu** 智能问答 | ✅ v1.0 (设计阶段) | [chat.html](kefu/chat.html) · [knowledge.html](kefu/knowledge.html) · [dashboard.html](kefu/dashboard.html) · [faq.html](kefu/faq.html) · [logs.html](kefu/logs.html) · [import.html](kefu/import.html) · [settings.html](kefu/settings.html) · [evaluation.html](kefu/evaluation.html) · [DESIGN.md](kefu/DESIGN.md) |

> **kefu v1.0 智能问答**：含 10 章节（业务架构/5 张数据表/状态机/权限/API/扩展架构/扩展点）。**核心特性：数据源插件架构** —— 后续可接入 park-business/park-enterprise/park-space/park-contract 等模块作为业务实体数据源。详见 [DESIGN.md](kefu/DESIGN.md) |
| mobile 移动端示例 | ✅ v1.0 | [mobile.html](mobile.html) |

---

## 五、设计 Tokens 速查

完整 tokens 见 `_assets/common.css`，关键值：

| Token | 值 | 用途 |
|-------|----|----|
| `--color-primary` | `#007AFF` | Apple Blue 主色 |
| `--color-canvas` | `#ffffff` | 卡片背景 |
| `--color-canvas-elevated` | `#f5f5f7` | 页面背景 |
| `--color-ink` | `#1d1d1f` | 主文字 |
| `--font-family-base` | SF Pro Text | 字体 |
| `--rounded-md` | `12px` | 卡片圆角 |
| `--rounded-sm` | `8px` | 按钮圆角 |
| `--shadow-card` | `0 2px 12px rgba(0,0,0,0.08)` | 卡片阴影 |

---

## 六、关键约定

### 6.1 数据源/权限点标注

每个内容页顶部注释必须包含：
- 模块 + 页面 + 版本
- 数据源（表.字段）
- API 列表
- 权限点

```html
<!--
模块: park-enterprise
页面: detail
版本: v1.0

数据源:
- 顶部: sys_enterprise
- Tab 1: sys_enterprise (51 字段)
...

API:
- GET /enterprise/{id}/detail
- PUT /enterprise/{id}

权限:
- enterprise:detail:view
- enterprise:detail:edit:base
-->
```

### 6.2 按钮权限标注

```html
<!-- {{perm:enterprise:detail:edit:base}} -->
<button class="btn btn-primary">✏️ 编辑基本信息</button>
```

### 6.3 字段来源标注

```html
<div class="label">企业名称 <span class="field-source">sys_enterprise.name</span></div>
```

---

## 七、旧 Prototype 归档（待执行）

```bash
# 创建归档目录
mkdir -p prototype/_archive

# 移动旧文件
git mv prototype/pc/phase1 prototype/_archive/pc-phase1
git mv prototype/pc/phase2 prototype/_archive/pc-phase2
git mv prototype/mobile prototype/_archive/mobile

# 删除空目录
rmdir prototype/pc

# 移动 root 入口
git mv prototype/index_pc.html prototype/_archive/
git mv prototype/login_pc.html prototype/_archive/
git mv prototype/index_mo.html prototype/_archive/
git mv prototype/login_mo.html prototype/_archive/

# 删除已完成的修复脚本
git rm prototype/_archive/pc-phase2/fix.ps1 \
         prototype/_archive/pc-phase2/fix2.ps1 \
         prototype/_archive/pc-phase2/fix3.ps1

# 提交
git commit -m "chore(prototype): 归档旧 phase1/phase2 分类的 34 个 HTML，启用按模块分类"
```

**归档原因**：
- 旧文件是 v0 时代的「边开发边原型」，与生产代码不同步
- 旧文件作为历史参考保留，但不作为新模块设计依据

---

## 八、快速开始

```bash
# 1. 打开登录页
start prototype/login.html

# 2. 输入任意账号 + 密码 → 登录
# 3. 自动跳转到 index.html（统一框架）
# 4. 顶部点击"企业档案"模块
# 5. 左侧点击"企业列表"→ 加载 park-enterprise/list.html
# 6. 点击企业名称"华为投资控股有限公司"→ 加载 park-enterprise/detail.html
# 7. 切换 7 个 Tab 查看效果
# 8. 移动端预览：打开 mobile.html → 切换 iPhone/iPad/Android
```

---

## 九、贡献指南

新模块原型产出流程：

1. 在 `prototype/{module}/` 创建目录
2. 在 `prototype/_assets/common.css` 添加模块专属样式（如有）
3. 在 `prototype/index.html` 的 `moduleMenus` 对象中添加模块的菜单入口
4. 产出内容页：list.html / detail.html / add.html / edit.html 等
5. 在 `prototype/{module}/README.md` 写模块说明（字段、权限、API 依赖）
6. 更新本 README §四 模块原型状态

---

**版本历史**：

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-01 | 初版：基于 Apple DESIGN.md，引入统一框架页 + 内容层分离 |

---

_所有 prototype 文件可直接用浏览器打开（无后端依赖），演示模式用 alert 模拟。_
