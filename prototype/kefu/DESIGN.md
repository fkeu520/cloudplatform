# 智能问答 (kefu) — 设计文档 v1.0

> **版本**: v1.0 (首次入库)
> **日期**: 2026-08-17
> **来源**: 云枢中台智能问答模块设计
> **适配**: 中台 prototype 框架 (`prototype/_assets/common.css`)
> **风格**: Apple DESIGN.md（沿用 park-business 模块约定）
> **核心特性**: **数据源插件架构** —— 后续可接入 park-business / park-enterprise / park-space / park-contract 等各模块实体

---

## 一、模块定位

智能问答 (kefu) 是云枢中台的 **AI 客服** 模块，覆盖：

- **客服会话**：人工客服 + AI 助手的双轨对话（左中右三栏布局）
- **知识库管理**：FAQ + 文档知识 + 业务实体引用（数据源插件）
- **数据看板**：问答量/满意度/命中率/AI 占比

**核心价值**：

| 维度 | 价值 |
|---|---|
| **响应效率** | 7×24 AI 兜底，人工专注高价值客户 |
| **知识沉淀** | FAQ + 文档 + **业务实体** 三类知识统一管理 |
| **业务联动** | 通过数据源插件**实时调用 park-business/park-enterprise 等模块** |
| **转化可视** | 满意度/命中率/转人工率量化服务质量 |

---

## 二、业务架构

```
主页面（3）
├── chat.html          问答对话（客服会话三栏）  ← 核心入口
├── knowledge.html     知识库管理（CRUD + 数据源标签）
└── dashboard.html     数据看板（图表）

辅助页面（5）
├── faq.html           FAQ 管理（快速问答）
├── logs.html          对话日志（审计）
├── import.html        知识导入（批量上传）
├── settings.html      多轮配置 + 数据源管理  ← 扩展点配置
└── evaluation.html    评估报告（AI 质量）

外部数据源（可扩展插件）
├── [内置] faq-source         FAQ 表检索
├── [内置] knowledge-source   知识库检索
└── [待接入] park-business    客户/商机/跟进（DESIGN §七扩展点）
```

---

## 三、数据模型

### 3.1 客服会话表 `kefu_session`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 会话 ID |
| customer_id | bigint | **关联 park-enterprise.id** |
| channel | enum | wechat/phone/web/app |
| status | enum | AI/ESCALATED/CLOSED |
| agent_id | bigint | 客服 ID（人工接管后） |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| satisfaction | int | 1-5 满意度 |

### 3.2 消息表 `kefu_message`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 消息 ID |
| session_id | bigint | 会话 ID |
| role | enum | customer/agent/ai/system |
| content | text | 消息内容 |
| data_source_refs | json | **引用的数据源 + 实体 ID**（如 `[{source:="park-enterprise",entity:="enterprise",id:="123"}]`）|
| knowledge_refs | json | 引用的知识片段 ID 列表 |
| created_at | datetime | 创建时间 |

### 3.3 知识库表 `kefu_knowledge`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 知识 ID |
| title | varchar | 知识标题 |
| category_id | bigint | 分类 ID |
| content | text | 知识内容 |
| source_type | enum | faq/manual/import/scraped/business_entity |
| source_ref | varchar | **业务实体引用**（如 `park-business:opportunity:OPP-001`）|
| vector_status | enum | pending/indexed/failed |
| enabled | boolean | **是否启用**（可临时禁用某条知识）|
| hit_count | int | 命中次数 |
| updated_at | datetime | 更新时间 |

### 3.4 FAQ 表 `kefu_faq`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | FAQ ID |
| question | text | 问题 |
| answer | text | 答案 |
| category_id | bigint | 分类 |
| hit_count | int | 命中次数 |
| enabled | boolean | **是否启用** |

### 3.5 数据源配置表 `kefu_data_source`（核心扩展表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar | 数据源 ID（如 `park-business`, `park-enterprise`） |
| name | varchar | 显示名称 |
| type | enum | `internal` / `external_api` / `database_query` / `vector_search` |
| module_ref | varchar | **关联模块路径**（如 `code/platform-server/platform-enterprise`） |
| enabled | boolean | 是否启用 |
| sync_strategy | enum | `realtime` / `scheduled` / `manual` |
| sync_interval | int | 同步间隔（秒） |
| config | json | 数据源特定配置（API endpoint、认证、字段映射） |
| last_sync_at | datetime | 上次同步时间 |
| created_at | datetime | 创建时间 |

**初始数据**：

| id | name | type | module_ref | enabled | sync_strategy |
|---|---|---|---|---|---|
| faq | FAQ 库 | internal | - | ✓ | realtime |
| knowledge | 知识库 | vector_search | - | ✓ | realtime |
| park-enterprise | 企业档案 | database_query | code/platform-server/platform-enterprise | ✓ | realtime |
| park-business | 招商管理 | database_query | code/platform-server/platform-business | ⏳ 待开发 | realtime |
| park-space | 空间房源 | database_query | code/platform-server/platform-space | ⏳ 待开发 | realtime |
| park-contract | 合同 | database_query | code/platform-server/platform-contract | ⏳ 待开发 | realtime |

---

## 四、核心权限矩阵

| 角色 | chat | knowledge | dashboard | faq | logs | import | settings | eval | **数据源管理** |
|---|---|---|---|---|---|---|---|---|---|
| **客服专员** | ✓ | view | view | view | view | | - | view | - |
| **客服经理** | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **运营** | | - | view | ✓ | | - | view | view | view |
| **系统管理员** | | - | | - | | - | ✓ | | - | ✓ |

数据源管理权限细粒度：每个角色可绑定可访问的数据源子集。

---

## 五、状态机

### 5.1 会话状态机

```
[NEW] ──首条消息──> [AI_RESPONDING]
                          │
            ┌─────────────┼──────────────┐
            ▼             ▼              ▼
        [AI_CLOSED]   [ESCALATED]    [DATA_FETCHING]   ← 新增：调用外部数据源
            │             │              │
            │             ▼              ▼
            │       [AGENT_RESPONDING] [AI_RESPONDING]
            │             │              │
            └─────────────┴──────────────┘
                          ▼
                     [CLOSED]
```

转移条件：
- AI 置信度 < 0.7 → ESCALATED
- 客户触发关键词（"人工"/"投诉"）→ ESCALATED
- **需要业务实体数据 → DATA_FETCHING → AI_RESPONDING**（新增）
- 客服接受 → AGENT_RESPONDING
- 会话无活动 30 分钟 → CLOSED

### 5.2 数据源调用子流程

```
AI_RESPONDING
   │
   ▼ (检测到需要 park-business 数据)
DATA_FETCHING
   │
   ├─→ 调用 DataSourceAdapter.query(intent, params)
   │
   ├─→ 返回结果（实体引用 + 上下文摘要）
   │
   ▼
AI_RESPONDING (生成最终回答)
   │
   ▼
消息中追加 data_source_refs（前端展示右侧"引用来源"）
```

---

## 六、关键交互规范

每个 HTML 必须：
- 顶部 6 行注释块（模块/页面/版本/数据源/API/权限）
- 沿用 `../_assets/common.css` 样式
- 内联 JS 实现交互（无后端 mock）
- `showToast(msg, type)` 系统反馈
- `parent.loadPage()` 跨页跳转
- **数据源引用时使用 `data-source-ref` 属性**（如 `<span data-source-ref="park-enterprise:123">深圳前海科技</span>`）

---

## 七、扩展架构（核心设计）

### 7.1 数据源插件接口

```javascript
// 抽象接口（前端 mock 实现）
interface DataSourceAdapter {
  id: string                    // 数据源 ID
  name: string                  // 显示名
  enabled: boolean              // 是否启用
  intentPatterns: RegExp[]      // 触发该数据源的问题意图正则

  query(intent, params): Promise<DataSourceResult>
  // 返回：{ refs: [...], summary: string, raw: object }

  // UI 配置（右侧栏如何展示）
  renderRefCard(ref): HTMLElement
}
```

### 7.2 路由匹配流程

```
用户提问: "深圳前海科技当前的入驻情况"
    ↓
AI Router (intent classifier)
    ↓
匹配到: park-enterprise 模式 (匹配企业名称 + "入驻" 关键词)
    ↓
调用 park-enterprise 适配器.query()
    ↓
返回: { enterprise: {...}, contracts: [...], opportunities: [...] }
    ↓
LLM 摘要: "深圳前海科技入驻 A 栋 3 层 301 室，月租 3 万..."
    ↓
消息 data_source_refs: [{source: "park-enterprise", id: "123", type: "enterprise"}]
```

### 7.3 内置数据源（v1.0）

| ID | 类型 | 触发关键词 | 返回数据 |
|---|---|---|---|
| faq | internal | 直接问 FAQ | 命中 FAQ 列表 |
| knowledge | vector_search | 通用知识检索 | 知识片段 Top5 |

### 7.4 待接入数据源（后续）

| ID | 触发关键词 | 接入步骤 |
|---|---|---|
| park-enterprise | "XX 企业"、"入驻"、"档案"、"联系人" | 1. 创建 adapter 2. 注册到 Registry 3. 配置 module_ref |
| park-business | "商机"、"跟进"、"客户分配" | 同上 |
| park-space | "房源"、"空房"、"租金"、"面积" | 同上 |
| park-contract | "合同"、"到期"、"续签" | 同上 |
| park-property | "物业"、"维修"、"缴费" | 同上 |

### 7.5 扩展点（开发者添加新数据源）

1. 在 `code/platform-server/kefu/adapter/` 创建 `XXXAdapter implements DataSourceAdapter`
2. 在 `kefu.registry` 注册：`registry.register(new XXXAdapter())`
3. 在 `kefu_data_source` 表插入配置
4. 在 settings.html "数据源管理"区域启用
5. 在 chat.html 右侧栏自动出现引用卡片

---

## 八、API 设计（参考）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/kefu/session/page` | 会话分页 |
| POST | `/kefu/session/{id}/message` | 发送消息 |
| POST | `/kefu/session/{id}/escalate` | 转人工 |
| POST | `/kefu/session/{id}/close` | 关闭会话 |
| POST | `/kefu/session/{id}/rate` | 满意度评分 |
| GET | `/kefu/knowledge/page?source=park-enterprise` | 知识分页（支持数据源过滤） |
| POST | `/kefu/knowledge` | 新增知识 |
| PUT | `/kefu/knowledge/{id}` | 更新知识 |
| DELETE | `/kefu/knowledge/{id}` | 删除知识 |
| POST | `/kefu/knowledge/import` | 批量导入 |
| **GET** | **`/kefu/data-source`** | **列出所有数据源（前端 settings 页面）** |
| **POST** | **`/kefu/data-source/{id}/sync`** | **触发数据源同步** |
| **POST** | **`/kefu/data-source/{id}/enable`** | **启用数据源** |
| **POST** | **`/kefu/query?intent=...&q=...`** | **统一查询入口（AI Router 用）** |

---

## 九、HTML 设计要点（强调扩展性）

### 9.1 chat.html 三栏布局（核心）

| 区域 | 作用 |
|---|---|
| 左栏 (240px) | 会话列表（按客户分组） |
| 中栏 (flex) | 消息气泡流 + 输入框 + 快捷回复 |
| **右栏 (320px)** | **客户信息卡 + 数据源引用卡 + 推荐回答** |

**右栏是扩展关键**：
- 顶部固定显示当前会话客户的 park-enterprise 信息（mock）
- 中部：每条消息的 `data_source_refs` 渲染成卡片
- 底部：AI 推荐回复候选

### 9.2 knowledge.html 表格列

| 列 | 说明 |
|---|---|
| 标题 | 知识标题 |
| **数据源** | **source_type + 业务实体引用**（如 "park-enterprise · 深圳前海科技"） |
| 分类 | faq/manual/business_entity |
| 命中次数 | hit_count |
| 启用 | 开关 |
| 操作 | 编辑/删除 |

### 9.3 settings.html 数据源管理区

- 列出 `kefu_data_source` 表所有数据源
- 每个数据源卡片：图标 + 名称 + 类型 + 同步策略 + 启用开关 + 配置入口
- 新增数据源按钮（开发向导）

### 9.4 evaluation.html 评估维度

新增 **"数据源命中率"** 指标：本次回答中有多少比例引用了业务实体 vs FAQ vs 知识库。

---

## 十、版本历史

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-17 | 首次入库：8 页面原型 + 数据源插件架构（支持扩展） |

---

_本设计文档是 kefu 模块原型的设计依据。_
_所有 HTML 必须遵循本文档的菜单结构、字段命名、权限点、扩展点。_
_后续接入新数据源时，遵循 §七扩展架构 + §九.5 扩展点流程。_