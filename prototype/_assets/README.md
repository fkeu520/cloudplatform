# Prototype 组件库（_assets）

**版本**: v1.0
**日期**: 2026-07-01
**样式规范**: Apple DESIGN.md（适配中台场景）
**来源文件**: `D:\work\AI\openclaw\skills\design-md\apple\DESIGN.md`

---

## 一、文件清单

| 文件 | 角色 |
|------|------|
| `common.css` | 公共样式（设计 tokens + 组件样式），所有 HTML 引用 |
| `README.md` | 本文件，组件库索引 |

---

## 二、使用方式

每个 HTML 原型顶部引用：

```html
<head>
  <link rel="stylesheet" href="../_assets/common.css">
</head>
```

或使用相对路径（视目录深度）：

```html
<!-- prototype/park-enterprise/list.html -->
<link rel="stylesheet" href="../_assets/common.css">

<!-- prototype/park-enterprise/detail.html -->
<link rel="stylesheet" href="../_assets/common.css">
```

---

## 三、设计 Tokens（与 Apple DESIGN.md 对应）

### 3.1 颜色（CSS Variables）

| Token | 值 | 用途 |
|-------|----|----|
| `--color-primary` | `#007AFF` | Apple Blue，主色（按钮、链接）|
| `--color-primary-active` | `#0056B3` | 主色 hover/pressed |
| `--color-primary-pale` | `#E5F1FF` | 主色淡背景（badge、选中）|
| `--color-ink` | `#1d1d1f` | 主文字（暖近黑）|
| `--color-ink-secondary` | `#6e6e73` | 次要文字 |
| `--color-ink-tertiary` | `#86868b` | 三级文字（提示、label）|
| `--color-canvas` | `#ffffff` | 卡片背景 |
| `--color-canvas-elevated` | `#f5f5f7` | 页面背景 |
| `--color-canvas-grouped` | `#f2f2f7` | 分组背景（表头、hover）|
| `--color-divider` | `#d2d2d7` | 主分割线 |
| `--color-divider-secondary` | `#c6c6c8` | 次分割线 |
| `--color-positive` | `#34c759` | 成功绿 |
| `--color-warning` | `#ff9500` | 警告橙 |
| `--color-negative` | `#ff3b30` | 错误红 |
| `--color-gray-1..6` | `#8e8e93` → `#f2f2f7` | 灰阶 |

### 3.2 字体

```css
--font-family-base: SF Pro Text, -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Inter, sans-serif;
--font-family-display: SF Pro Display, -apple-system, BlinkMacSystemFont, Inter, sans-serif;
```

**注意**：Windows 环境无 SF Pro 时降级到 PingFang SC（Mac）或 Microsoft YaHei（Windows）。

### 3.3 圆角

| Token | 值 | 用途 |
|-------|----|----|
| `--rounded-xs` | `6px` | 标签、小按钮 |
| `--rounded-sm` | `8px` | 按钮、输入框 |
| `--rounded-md` | `12px` | 卡片 |
| `--rounded-lg` | `16px` | 大卡片 |
| `--rounded-xl` | `20px` | 营销大卡 |
| `--rounded-2xl` | `24px` | 弹窗 |
| `--rounded-full` | `9999px` | 头像、徽章、关闭按钮 |

### 3.4 阴影

| Token | 值 | 用途 |
|-------|----|----|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.04)` | 微小阴影 |
| `--shadow-md` | `0 2px 8px rgba(0,0,0,0.08)` | 卡片默认 |
| `--shadow-card` | `0 2px 12px rgba(0,0,0,0.08), 0 0 0 1px rgba(0,0,0,0.02)` | 卡片标准（iOS 浮起）|
| `--shadow-elevated` | `0 12px 40px rgba(0,0,0,0.15)` | 弹窗、下拉 |

### 3.5 间距

8 进制：`2/4/8/12/16/20/24/32/40/48/64px`（xxs → 6xl）

---

## 四、组件索引

### 4.1 布局类

| 组件 | Class | 用途 |
|------|-------|------|
| 顶部导航 | `.topbar` | 56px 高，面包屑 + 返回 |
| 页面标题 | `.page-title` | 18px 加粗 |
| 卡片 | `.card` | 圆角 12px + 浮起阴影 |
| 卡片头 | `.card-header` | 标题 + 操作 |
| 卡片体 | `.card-body` | 内容区 |
| 工具栏 | `.toolbar` | 搜索 + 按钮 |
| 左右分栏 | `.layout` `.left-panel` `.right-panel` | 树+详情场景 |
| 顶部信息条 | `.top-info` | 详情页顶部 100px |
| Tab 栏 | `.tabs-bar` `.tab` | 详情页 Tab 切换 |
| Tab 内容 | `.tabs-content` `.tab-pane` | Tab 内容区 |

### 4.2 数据展示类

| 组件 | Class | 用途 |
|------|-------|------|
| 表格 | `table` `th` `td` | 标准数据表格 |
| 描述列表 | `.descriptions` `.item` | 详情页字段展示（2/3/4 列）|
| 分组标题 | `.section-title` | "工商信息"等分组 |
| 徽章 | `.badge` `.badge-active` 等 | 状态标签 |
| HTTP 方法 | `.method-get/post/put/delete` | 操作日志 |
| 标签 | `.tag` `.tag-add` | 企业标签 |
| 树节点 | `.tree-node` `.tree-children` | 树形结构 |

### 4.3 操作类

| 组件 | Class | 用途 |
|------|-------|------|
| 主按钮 | `.btn-primary` | 主操作 |
| 次按钮 | `.btn-secondary` | 次操作（描边）|
| 默认按钮 | `.btn-default` | 普通操作 |
| 危险按钮 | `.btn-danger` | 删除等危险操作 |
| 成功按钮 | `.btn-success` | 启用/确认 |
| 警告按钮 | `.btn-warning` | 警示 |
| 小按钮 | `.btn-sm` | 表格内 |
| 图标按钮 | `.btn-icon` | 圆形图标 |
| 搜索框 | `.search-box` | 工具栏内搜索 |
| 表格分页 | `.pagination` | 列表底部 |
| 弹窗 | `.modal-overlay` `.modal` | 增删改查 |
| 单选/复选 | `.radio-group` `.checkbox-group` | 表单 |
| 表单行 | `.form-row` `.col` | 2 列布局 |

### 4.4 状态类

| 组件 | Class | 用途 |
|------|-------|------|
| 加载中 | `.loading` | 异步等待 |
| 空状态 | `.empty-state` | 无数据 |
| 权限标记 | `.perm-marker` | Dev 模式调试 |

---

## 五、典型用法示例

### 5.1 顶部信息条（详情页）

```html
<div class="top-info">
  <div class="name">
    华为投资控股有限公司
    <span class="badge badge-active">在营</span>
  </div>
  <div class="alias">简称：华为投资</div>
  <div class="meta-row">
    <div class="meta-item">
      <span class="meta-label">统一社会信用代码：</span>
      <span class="meta-value">91440300MA5DA7Q37H</span>
    </div>
    <div class="meta-item">
      <span class="meta-label">注册时间：</span>
      <span class="meta-value">1987-09-15</span>
    </div>
    <div class="meta-item">
      <span class="meta-label">法定代表人：</span>
      <span class="meta-value">任正非</span>
    </div>
  </div>
</div>
```

### 5.2 Tab + 编辑按钮

```html
<div class="tabs-bar">
  <div class="tab active">基本信息</div>
  <div class="tab">企业概览</div>
  <div class="tab">企业标签 <span class="badge-count">5</span></div>
  ...
</div>
<div class="tabs-content">
  <div class="tab-pane active">
    <div class="card-header" style="border:none;padding:0;margin-bottom:20px">
      <h3>工商登记信息</h3>
      <!-- {{perm:enterprise:detail:edit:base}} -->
      <button class="btn btn-primary">✏️ 编辑基本信息</button>
    </div>
    <div class="descriptions">
      <div class="item">
        <div class="label">企业名称 <span class="field-source">sys_enterprise.name</span></div>
        <div class="value">华为投资控股有限公司</div>
      </div>
      ...
    </div>
  </div>
</div>
```

### 5.3 列表页 + Excel 按钮

```html
<div class="toolbar">
  <div class="search-box">
    <input type="text" placeholder="企业名称">
    <input type="text" placeholder="统一社会信用代码">
    <select><option>全部状态</option></select>
    <button class="btn btn-primary">搜索</button>
    <button class="btn btn-default">重置</button>
  </div>
  <div class="search-actions">
    <!-- {{perm:enterprise:excel:export}} -->
    <button class="btn btn-success">📥 导入</button>
    <!-- {{perm:enterprise:excel:template}} -->
    <button class="btn btn-default">📋 下载模板</button>
    <!-- {{perm:enterprise:excel:export}} -->
    <button class="btn btn-warning">📤 导出</button>
  </div>
</div>
```

### 5.4 编辑弹窗

```html
<div class="modal-overlay show">
  <div class="modal modal-lg">
    <div class="modal-header">
      <h3>编辑基本信息</h3>
      <button class="close">×</button>
    </div>
    <div class="modal-body">
      <div class="form-row">
        <div class="col">
          <label>企业名称 <span class="required">*</span></label>
          <input type="text" value="华为投资控股有限公司">
          <div class="field-source">sys_enterprise.name</div>
        </div>
        <div class="col">
          <label>简称</label>
          <input type="text" value="华为投资">
        </div>
      </div>
      ...
    </div>
    <div class="modal-footer">
      <button class="btn btn-default">取消</button>
      <button class="btn btn-primary">保存</button>
    </div>
  </div>
</div>
```

---

## 六、引用规范

每个 HTML 原型**必须**在显眼位置（顶部或注释）包含：

### 6.1 数据源标注

```html
<!--
模块: park-enterprise
页面: detail
版本: v1.0

数据源:
- 顶部信息: sys_enterprise (id, name, alias, credit_code, estiblish_time, reg_status, legal_person_name)
- Tab 1 基本信息: sys_enterprise (51 字段)
- Tab 2 企业概览: sys_enterprise_overview_data
- Tab 3 企业标签: sys_enterprise_tag + sys_tag
- Tab 4 客户信息: sys_customer_information
- Tab 5 云企库数据: sys_enterprise_cloud_data (5 类)
- Tab 6 关注标签: sys_enterprise_focus + sys_focus + sys_focus_item
- Tab 7 工商信息: sys_enterprise_register + sys_enterprise_unregister + sys_enterprise_stock

API:
- GET /enterprise/{id}/detail
- PUT /enterprise/{id} (Tab 1)
- POST /enterprise/{id}/tag (Tab 3)
- DELETE /enterprise/{id}/tag/{tagId} (Tab 3)
- ... (其他编辑 API)
-->
```

### 6.2 权限点标注

使用 HTML 注释标注按钮需要的权限点（与生产代码 v-has-perm 对应）：

```html
<!-- {{perm:enterprise:detail:edit:base}} -->
<button class="btn btn-primary">✏️ 编辑基本信息</button>
```

或显式 `perm-marker`：

```html
<button class="btn btn-primary">
  ✏️ 编辑基本信息
  <span class="perm-marker">perm:enterprise:detail:edit:base</span>
</button>
```

### 6.3 字段来源标注

关键字段标注 `表.字段名`：

```html
<div class="label">企业名称 <span class="field-source">sys_enterprise.name</span></div>
```

---

## 七、已应用此规范的原型

| 模块 | 文件 | 状态 |
|------|------|------|
| park-enterprise | `list.html` | 🔄 进行中 |
| park-enterprise | `detail.html` | 🔄 进行中 |

---

**版本历史**：

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-01 | 初版：基于 Apple DESIGN.md，适配中台场景 |

---

_本组件库是所有 prototype 的视觉基础。新模块原型必须引用 common.css 并遵循本规范。_
