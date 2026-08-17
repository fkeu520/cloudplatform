# 智能问答 (kefu) — 原型索引

> **版本**: v1.0
> **日期**: 2026-08-17
> **设计规范**: [Apple DESIGN.md](D:\work\AI\openclaw\skills\design-md\apple\DESIGN.md)
> **详细设计**: [DESIGN.md](./DESIGN.md)
> **核心特性**: **数据源插件架构** —— 后续可接入 park-business / park-enterprise / park-space / park-contract 等模块

---

## 一、模块定位

智能问答 (kefu) 是云枢中台的 **AI 客服** 模块，覆盖：

- **客服会话**：人工客服 + AI 助手的双轨对话（左中右三栏布局）
- **知识库管理**：FAQ + 文档 + 业务实体引用
- **数据看板**：问答量/满意度/命中率/AI 占比

**核心场景**：

| 角色 | 主用页面 |
|---|---|
| **客服专员** | 问答对话、知识库（查看）、FAQ（查看）、会话日志 |
| **客服经理** | 全模块 + 数据看板 + 评估报告 + 数据源配置 |
| **运营** | 数据看板、会话日志、数据源查看 |

---

## 二、文件清单

| 文件 | 类型 | 状态 | 说明 |
|---|---|---|---|
| `README.md` | 文档 | ✅ | 本索引 |
| `DESIGN.md` | 文档 | ✅ | 详细设计（含扩展架构） |
| `chat.html` | 页面 | ✅ | **核心**：客服会话三栏（左中右） |
| `knowledge.html` | 页面 | ✅ | 知识库管理（CRUD + 数据源标签） |
| `dashboard.html` | 页面 | ✅ | 数据看板（问答量/满意度/AI 占比） |
| `faq.html` | 页面 | ✅ | FAQ 管理（快速问答） |
| `logs.html` | 页面 | ✅ | 对话日志（审计/导出） |
| `import.html` | 页面 | ✅ | 知识导入（批量上传） |
| `settings.html` | 页面 | ✅ | 多轮配置 + 数据源管理（扩展点） |
| `evaluation.html` | 页面 | ✅ | 评估报告（AI 质量评分） |

---

## 三、侧边栏菜单（在 `prototype/index.html` 注册）

```js
'kefu': {
  title: '智能问答',
  items: [
    { id: 'chat',       name: '问答对话', icon: '💬', url: 'kefu/chat.html' },
    { id: 'knowledge',  name: '知识库',   icon: '📚', url: 'kefu/knowledge.html' },
    { id: 'dashboard',  name: '数据看板', icon: '📊', url: 'kefu/dashboard.html' },
    { id: 'faq',        name: 'FAQ 管理', icon: '❓', url: 'kefu/faq.html' },
    { id: 'logs',       name: '对话日志', icon: '📜', url: 'kefu/logs.html' },
    { id: 'import',     name: '知识导入', icon: '📥', url: 'kefu/import.html' },
    { id: 'settings',   name: '场景配置', icon: '⚙️', url: 'kefu/settings.html' },
    { id: 'evaluation', name: '评估报告', icon: '📈', url: 'kefu/evaluation.html' },
  ]
},
```

> **数据源管理** 在 settings.html 内（不是顶层菜单项），含 6 个数据源的启用/禁用/同步策略配置。

---

## 四、扩展点（开发者指南）

### 4.1 当前内置数据源（v1.0）

| ID | 类型 | 触发关键词 |
|---|---|---|
| `faq` | internal | 直接问 FAQ |
| `knowledge` | vector_search | 通用知识检索 |

### 4.2 待接入数据源（已 mock 占位）

| ID | 触发关键词 | 接入步骤 |
|---|---|---|
| `park-enterprise` | "XX 企业"、"入驻"、"档案"、"联系人" | 见 DESIGN §七.5 |
| `park-business` | "商机"、"跟进"、"客户分配" | 同上 |
| `park-space` | "房源"、"空房"、"租金"、"面积" | 同上 |
| `park-contract` | "合同"、"到期"、"续签" | 同上 |

### 4.3 接入新数据源 5 步流程

1. 后端实现 `DataSourceAdapter` 接口（路径 `code/platform-server/kefu/adapter/`）
2. 注册到 `kefu.registry`（启动时扫描 + 手动注册）
3. `kefu_data_source` 表插入配置（id/name/module_ref/enabled/sync_strategy）
4. settings.html "数据源管理"区域启用该数据源
5. chat.html 右侧栏自动出现引用卡片（无需改前端代码）

---

## 五、关键字段参考（详见 DESIGN.md §三）

| 实体 | 表名 | 核心字段 |
|---|---|---|
| 会话 | `kefu_session` | id, customer_id, channel, status, agent_id, satisfaction |
| 消息 | `kefu_message` | id, session_id, role, content, **data_source_refs**, knowledge_refs |
| 知识 | `kefu_knowledge` | id, title, category_id, source_type, **source_ref**, enabled |
| FAQ | `kefu_faq` | id, question, answer, category_id, enabled |
| **数据源** | **`kefu_data_source`** | **id, type, module_ref, enabled, sync_strategy, config** |

---

## 六、引用规范

每个 HTML 顶部必须包含：

```html
<!--
模块: kefu
页面: <page-name>
版本: v1.0

数据源:
- 主表: <table>
- 数据源引用: park-enterprise / park-business / faq / knowledge

API:
- GET <path>
- POST <path>

权限:
- <perm:xxx>
-->
```

数据源引用属性：

```html
<span data-source-ref="park-enterprise:123" data-source-type="enterprise">深圳前海科技</span>
```

---

## 七、版本历史

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-17 | 首次入库：8 页面原型 + 数据源插件架构（支持扩展） |

---

_所有 prototype 文件遵循 `prototype/_assets/common.css` 设计规范，可直接用浏览器打开（无后端依赖）。_
_扩展新数据源时，遵循 DESIGN §七 + README §四。_