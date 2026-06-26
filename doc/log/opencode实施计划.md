# 云枢中台 - Opencode 实施计划

**版本：** v7.1
**日期：** 2026-06-26 (追加修补阶段 v2 — 配置动态化)
**代码验证：** ✅ 实际代码全量校验
**定位：** 构建通用技术底座，价值驱动、小步快跑
**主规划文档：** [`中台建设中长期规划.md`](中台建设中长期规划.md)（v1.0, 2026-06-03）

---

## ⚠️ v7.0 重大调整（2026-06-03）

本版本重大变更：
- **新增主规划文档** [`中台建设中长期规划.md`](中台建设中长期规划.md)：整合性能优化 + 中台 5 层蓝图 + 3 紧急 P0 项 + 思考过程
- **v6.0 的 Phase 1A/B/C + Sprint 3-5 模型已过时**：本版本仅作为"已完成的实施历史"保留，不再作为下一阶段路线
- **下一阶段路线** 见 [`中台建设中长期规划.md` 第六章 实施路线图](中台建设中长期规划.md#六实施路线图)：
  - Q3 2026: P0-1 多租户拦截器 / P0-2 data_scope / P1-1 链路追踪 / L2 调优
  - Q4 2026: P0-3 SSO / P0-4 能力地图 / P1 可观测全套 / P2 开放基础
  - Q1 2027: P2 SDK / P3 韧性 / P4 业务中台
- **关键决策**:
  - 优化原则: 可用性 > 可扩展性 > 性能（v1.0 L2 违规方案已废弃）
  - 实施顺序: P0-1 → P0-2 → P1-1 → L2（安全性优先）
  - 3 紧急 P0 项 > L2 性能优化（性能省钱，P0 防事故）

---

## 1. 现状评估

| 模块 | 状态 | 完成度 | 验证方式 |
|------|------|--------|---------|
| **platform-server** | 已有 7 个 Maven 模块 | Phase 1-2 完成 90% | 代码确认 |
| - platform-common | 公共模块（实体基类、异常、统一返回、工具类） | 100% | 代码确认 |
| - platform-gateway | API 网关（JWT 鉴权、限流、路由） | 100% | 代码确认 |
| - platform-auth | 认证服务（密码登录、短信登录、Token 管理） | **70%** ⚠️ | 代码确认 — 仍有硬编码 mock 用户 |
| - platform-user | 用户中心（用户/角色/菜单/组织 CRUD + RBAC + 字典 + 参数配置） | 100% | 代码确认 |
| - platform-workflow | 流程中心（Flowable 6.8.1 + bpmn-js 专业设计器 + SVG 设计器 + 审批 + 监控） | 100% | 代码确认 |
| - platform-ops | 运营管理服务（租户/存储/网关/日志审计） | 80% | 代码确认 — MinIO 配置完成，部分剩余 |
| - platform-message | 消息中心（渠道/模板/发送引擎/站内信/记录） | **100%** ✅ | 代码确认 — P0~P5 全部完成 |
| **platform-admin** | Vue 3 + Element Plus 前端 | 系统管理 100%，流程 100%，消息 100% | 代码确认 |
| **platform-ops-admin** | Vue 3 + Element Plus 运营前端 | 脚手架/登录/布局/租户 100%，存储/网关/审计 待补 | 代码确认 |
| **platform-app** | UniApp 小程序（仅启动脚本，无源码） | 0% | 目录确认 |
| **待创建** | 应用中心/内容中心/任务中心/运营中心/门户中心/IoT/数据中台 | 0% | — |

---

## 2. 总体实施路线

```
Phase 1A (已完成) ──→ Phase 1B (已完成) ──→ Phase 1C (部分完成) ──→ Phase 2 Sprint 1-2 (已完成)
   字典+参数配置          基础设施修复           运营管理服务             流程中心 + 消息中心
                           前后端联调验证         platform-ops +           
                           动态菜单+按钮权限       platform-ops-admin       
                           操作日志+文件管理      
                                                                │
                         ┌──────────────────────────────────────┘
                         ▼
                   待修复项 ──→ platform-auth 改造 + 统一登录密码
                   下一阶段 ──→ Sprint 3 (应用/内容/任务中心)
                                                      │
                          ┌───────────────────────────┼───────────────────────────┐
                          ▼                           ▼                           ▼
                     Phase 3 (物联中台)          Phase 4 (数据中台)          Phase 5 (门户中心)
                     独立项目                     独立项目                    (通用展示层)
                     └── 共享 platform-shared ─────────────────────────────────┘
```

**范围界定：** 本次实施聚焦通用技术底座能力，不包含特定行业业务应用。

### 核心原则（基于调整建议）

| 原则 | 说明 |
|------|------|
| **价值驱动** | 每个 Sprint 必须有可验证的业务产出，不追求大而全 |
| **依赖优先** | 被依赖的模块先建（用户中心 → 流程中心 → 消息中心 → 应用中心 → 运营中心 → 物联中台 → 数据中台 → 门户中心） |
| **小步快跑** | 每个任务 2-4h，附带检查清单，可跨 session 独立交付 |
| **共享先行** | 共享层 (platform-shared) 从 Phase 2 开始抽取，IoT/Data 直接复用 |
| **不并行启动** | Phase 2 按优先级一个个 Sprint 推进，不多个微服务同时开 |

---

## 3. Phase 1A — 技术中台补全 ✅（已完成）

### 目标
完成技术中台剩余功能——字典管理 + 参数配置，支撑后续中台平台能力开发。

### 已完成交付

| 模块 | 文件 | 状态 |
|------|------|------|
| 后端字典管理 | DictType/DictData Entity + Mapper + Service + Controller | ✅ |
| 后端参数配置 | Config Entity + Mapper + Service + Controller | ✅ |
| 数据表 | `sys_dict_type` + `sys_dict_data` + `sys_config`（含初始数据） | ✅ |
| 网关路由 | `/dict/**` 和 `/config/**` 路由配置 | ✅ |
| 前端 API | `dict.ts` + `config.ts` 接口定义 | ✅ |
| 前端字典页面 | 左右分栏：类型列表 + 数据管理 | ✅ |
| 前端参数页面 | 表格 CRUD + 搜索 | ✅ |
| 路由 + 菜单 | 侧边栏菜单 + 按钮权限数据 | ✅ |

**接口清单（已验证）：**
- `GET /dict/type/page`, `POST/PUT/DELETE /dict/type/{id}`
- `GET /dict/data/page`, `GET /dict/data/type/{dictType}`
- `POST/PUT/DELETE /dict/data/{id}`
- `GET /config/page`, `GET /config/{id}`, `GET /config/key/{configKey}`
- `POST/PUT/DELETE /config/{id}`

---

## 4. Phase 1B — 前后端联调验证 + 技术增强（已完成 ✅）

### 目标
验证 Phase 1 成果可端到端工作，补充前端核心能力（动态菜单、权限指令、操作日志），为 Phase 2 打好基础。

**策略：** 优先修复已知问题 + 补充必要的前端能力，延后低优先级任务。

### Sprint 任务清单（按优先级）

> **基础设施修正先行：** 完善建议中指出了 pom.xml 版本不匹配（Spring Cloud Alibaba 2022.x → 2023.x，Sa-Token spring-boot → spring-boot3-starter）以及 JDK 版本要求（编译环境 ≥21）。这些必须优先修复，否则后续开发会因环境问题阻塞。

#### P0：基础设施修正 + 联调验证（6h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | **pom.xml 版本修正**：Spring Boot 3.2.0 → **3.2.4**，Spring Cloud Alibaba 2023.0.3.2 → **2023.0.1.0** | 1h | `mvn clean compile` 通过，无依赖冲突 |
| 2 | **JDK 21 编译环境升级**：本地 IDEA / opencode jdtls 切换 JDK 21，pom.xml 保持 source/target 17 | 0.5h | jdtls 正常启动，跳转/引用功能可用 |
| 3 | **Flyway 集成**：添加 flyway-spring-boot-starter 依赖，验证已有 SQL（user.sql 等）能自动执行 | 1.5h | 启动日志显示 Flyway 迁移成功 |
| 4 | **XXL-JOB Admin 部署**：Docker Compose 添加 xxl-job-admin 服务，初始化调度数据库 | 1h | 调度中心控制台可访问 |
| 5 | **Gateway 路由审计**：确认 /dict/** /config/** /auth/** /user/** 路由正确，无需鉴权的路径放行 | 1h | 所有服务路由可正常访问 |
| 6 | 联调验证：登录 / 用户 CRUD / 字典管理 / 配置管理 | 1h | 端到端流程跑通，无报错 | ✅ |

#### P1：动态菜单导航（3h）

| # | 任务 | 文件 | 预计 | 检查项 |
|---|------|------|------|--------|
| 1 | 后端：MenuController 增加 /menu/nav 接口 | `platform-user/` | 1h | 返回当前用户可见的菜单树 |
| 2 | 前端：更新 Layout.vue 从 /menu/nav 加载菜单 | `platform-admin/src/layout/` | 1.5h | 菜单动态生成，无硬编码 |
| 3 | 前端：路由守卫根据菜单动态生成路由 | `platform-admin/src/router/` | 0.5h | 非法路由跳转 404 |

#### P2：按钮级权限指令（3h）

| # | 任务 | 文件 | 预计 | 检查项 |
|---|------|------|------|--------|
| 1 | 前端：创建 v-permission 指令 | `platform-admin/src/directives/permission.ts` | 1h | 指令绑定 meta.roles 生效 |
| 2 | 前端：Pinia 存储用户权限标识 | `platform-admin/src/store/` | 0.5h | 登录后同步权限列表 |
| 3 | 前端：应用 v-permission 到操作按钮 | 各 Index.vue | 1.5h | 按钮按角色显示/隐藏 |

#### P3：操作日志（4h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | SQL：`sys_oper_log` 表 | 0.5h | 含操作人/IP/方法/参数/耗时 |
| 2 | 后端：AOP 切面 `@Log` 注解 | 1.5h | 标注的 Controller 自动记录日志 |
| 3 | 前端：操作日志查询页面 | 2h | 表格展示 + 详情弹窗 |

#### P3：操作日志完成后，接 Knife4j API 文档验证（1h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | Knife4j 聚合文档验证：访问 Gateway Swagger 地址，确认所有服务接口文档可浏览 | 1h | Swagger 页面正常显示 API 列表 | ✅ |

#### P4（可延后）：ECharts Dashboard 真实图表

| # | 任务 | 预计 | 说明 |
|---|------|------|------|
| 1 | ECharts 集成 Dashboard 真实图表 | 3h | `npm install echarts`，接入真实数据 |

> **延后理由：** Dashboard 需要真实业务数据支撑。

---

## 5. Phase 1C — 运营管理服务（部分完成）

### 目标
构建独立运营管理服务 `platform-ops` + 独立前端 `platform-ops-admin`，为系统运维人员提供可视化管理能力。

### 架构

```
platform-server/
└── platform-ops (新建)     ← 运营管理微服务，端口 8088

platform-ops-admin (新建)   ← 独立 Vue 3 应用，端口 8090
├── 独立 Docker 镜像 + nginx
└── 独立入口地址（如 http://ops.cloudhub.local）
```

### 后端任务

#### 模块一：服务脚手架 + 租户管理（4.5h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | `platform-ops` 模块创建：pom.xml、bootstrap.yml、入口类、Gateway 路由配置 | 1h | 启动成功，注册到 Nacos | ✅ |
| 2 | `sys_tenant` 表 + `Tenant` 实体 + Flyway 初始化 SQL（含默认租户数据） | 1h | 迁移自动执行 | ✅ |
| 3 | 租户 CRUD API + 启停/状态管理 | 1.5h | 启停后关联用户继承状态 | ✅ |
| 4 | TenantId 硬编码改 Nacos 配置 + MyBatis-Plus 多租户插件 | 2h | **补充设计：** 明确采用列隔离方案，配置 `TenantLineInnerInterceptor` 自动追加 `AND tenant_id = ?`，跨租户查询需显式授权注解 |

#### 模块二：对象存储配置（3h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | MinIO Admin API 封装（Bucket CRUD、策略配置、Access Key 管理） | 2h | 通过 MinIO Admin SDK 操作 |
| 2 | MinIO 连接配置 API（支持配置多个存储实例） | 1h | 可添加/测试 MinIO 连接 |

#### 模块三：消息队列（0.5h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | Docker Compose 添加 `kafka-ui` 服务（provectus/kafka-ui） | 0.5h | 独立 Web 页面管理 Topic/Consumer/Message |

#### 模块四：服务网关管理（3.5h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | `GatewayRoute` 实体 + 路由定义表 + Flyway SQL | 1.5h | 含路由ID/URI/谓词/过滤器/顺序/状态 | ✅ |
| 2 | 动态路由 CRUD API + RouteDefinitionWriter 动态刷新（无需重启网关） | 2h | 增删改路由即时生效，现有路由不受影响 |

#### 模块五：日志审计（4.5h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | `sys_login_log` 表 + 登录日志记录 AOP 切面 | 1h | 每次登录记录 IP/时间/设备 UA/成功/失败 | ✅ |
| 2 | 操作日志查询 API（复用现有 `sys_oper_log`） | 1h | 多维度筛选、分页 |
| 3 | 登录日志 + API 调用日志查询 API | 1h | 按时间/用户/IP 筛选 |
| 4 | ELK 日志检索 API（查询 ES `platform-logs-*` 索引） | 1.5h | 关键词搜索 + 时间范围过滤 |

### 前端任务

#### 模块六：platform-ops-admin 独立应用（9h）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 脚手架搭建：Vue 3 + Vite + Element Plus + Router + Axios + Pinia | 1h | `npm run dev` 正常 | ✅ |
| 2 | Dockerfile 构建 + nginx.conf 配置 | 0.5h | Docker 镜像可启动 | ✅ |
| 3 | 登录页 + Layout 布局（侧边栏 → 顶部导航） | 1h | 复用 `platform-auth` 认证接口 | ✅ |
| 4 | 租户管理页面（表格 + 弹窗表单 + 启停开关） | 1h | CRUD 正常 | ✅ |
| 5 | 对象存储配置页面（Bucket 列表/创建/策略、密钥管理） | 1h | 操作 MinIO 生效 |
| 6 | 服务网关管理页面（路由表格 + 谓词/过滤器 JSON 编辑） | 1h | 路由动态刷新 |
| 7 | 日志审计页面（Tab 切换：操作日志 / 登录日志 / API 日志） | 0.5h | 搜索 + 详情查看 |
| 8 | 消息队列入口（点击打开 kafka-ui 新标签页） | 0.5h | 跳转正常 |
| 9 | **系统监控面板**：Actuator `/health` 聚合所有服务 + Redis 命中率/连接池/Kafka Lag 展示 | 3h | 所有服务健康状态可见 |

### 部署清单

```yaml
# docker-compose.yml 新增
platform-ops:
  build: ./code/platform-server
  container_name: platform-ops
  ports:
    - "8088:8088"
  depends_on:
    - mysql
    - nacos

platform-ops-admin:
  build: ./code/platform-ops-admin
  container_name: platform-ops-admin
  ports:
    - "8090:80"

kafka-ui:
  image: provectuslabs/kafka-ui:latest
  container_name: platform-kafka-ui
  ports:
    - "8089:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: platform
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

### 当前 Sprint 已知风险

| 风险 | 影响 | 应对 |
|------|------|------|
| pom.xml 版本修正后依赖冲突 | 编译失败，阻塞 Phase 1B 后续任务 | 先独立分支验证 `mvn clean compile`，确认无冲突再合并 |
| JDK 21 与项目已有依赖不兼容 | jdtls 或 Maven 编译报错 | 编译环境升 21，生产保持 17；pom.xml 的 source/target 保持 17 |
| Flowable 6.8.0 开源版不含 AI Agent Task | Phase 4 如需 AI 审批需购买 Enterprise 许可 | Phase 2 Sprint 1 前确认是否需要此功能 |
| Spring Cloud Alibaba 2023.x 要求 JDK 17+ | 已有 JDK 版本不符 | 无影响，当前已满足 |

---

## 修补阶段 — 基础设施修复（建议优先于 Sprint 3）

代码验证发现以下阻塞性问题，建议在进入新功能开发前优先修复：

| 优先级 | 问题 | 位置 | 预计 |
|--------|------|------|------|
| ~~P0~~ | ~~platform-auth 硬编码 mock → 调用 user 服务~~ | ✅ 已修复 | 3h |
| ~~P0~~ | ~~统一登录密码（管理/运营平台两套接口）~~ | ✅ 已修复 | 1h |
| ~~P1~~ | ~~Kafka acks=all + 重试~~ | ✅ `MessageRetryService` + `acks=all` | 3h |
| ~~P1~~ | ~~多租户隔离规范化~~ | ✅ `TenantContextHolder` + `TenantFilter` + `TenantLineInnerInterceptor` | 2h |
| ~~P1~~ | ~~SSE 实时推送替代 15s 轮询~~ | ✅ `SseService` + `SseController` + 前端 EventSource | 4h |
| ~~P1~~ | ~~SiteMessageSender 真实对接~~ | ✅ 写入 `sys_message` 表 | 1h |
| ~~P2~~ | ~~系统监控面板~~ | ✅ `MonitorController` + `monitor/Index.vue` | 3h |
| ~~P2~~ | ~~数据权限模块~~ | ✅ `@DataScope` + `DataScopeAspect` | 4h |
| ~~P2~~ | ~~画布网格对齐~~ | ✅ `snapToGrid(20px)` + SVG 网格 | 2h |
| ~~P2~~ | ~~流程验证~~ | ✅ `WorkflowValidationService` | 3h |
| ~~P2~~ | ~~任务签收UI~~ | ✅ `TaskTodo.vue` claim/unclaim | 2h |
| P3 | SmsSender 阿里云 SDK 对接 | 待接入 | 3h |
| P3 | EmailSender / AppPushSender 渠道实现 | 新建 | 4h |
| P2 | **ops-admin 系统监控面板**：Actuator 聚合 + 组件状态 | `platform-ops-admin` | 3h |
| P2 | **数据权限模块**：`@DataScope` + SQL 拦截器 + 前端角色配置 | 前后端 | 4h |
| P2 | 画布交互优化（拖拽、网格对齐） | `ProcessDesigner.vue` | 3h |
| P2 | 流程验证（BPMN 语法校验、版本管理） | `platform-workflow` | 4h |
| P2 | 用户任务签收（claim/unclaim） | 前后端 | 3h |

---

## 修补阶段 v2 — 配置动态化（建议优先于 Sprint 3）

代码摸底发现配置维护性问题，详见 [`配置动态改造方案.md`](../../plan/配置动态改造方案.md) v1.1 (2026-06-26)：

| 优先级 | 问题 | 位置 | 预计 |
|--------|------|------|------|
| **P0** | `ChannelSenderRegistry.senderMap` 是 `HashMap` 非线程安全，并发初始化有 bug 风险 | `platform-message/.../channel/ChannelSenderRegistry.java:16` | 0.5h |
| **P0** | `RateLimitFilter.ipCounters` 单实例内存版，多实例 HA 后行为漂移 | `platform-gateway/.../filter/RateLimitFilter.java` | 1.5d (Redis + Lua) |
| **P1** | 5 模块 `bootstrap.yml` + 4 模块 `spring.config.import` 风格不统一；Spring Boot 3.2.4 已支持 `spring.config.import`，统一用 import 简化加载 | 5 模块 | 1d |
| **P1** | 9 项频繁调优配置（限流阈值、JWT 过期、多租户开关等）写死 yml，改完需重新打包 | 9 模块 | 1d |
| P2 | SSE / 数据权限 / 操作日志 缓存 Redis 化（HA 时投入） | 3 模块 | 3-4d |

**预计总工作量**: P0+P1 = 4 天（本周末 + 下周）

## 6. Phase 2 — 中台平台核心（按依赖关系分 Sprint）

聚焦通用技术底座能力。**不再多个微服务并行启动**，按依赖关系和价值大小排序，每个 Sprint 有可交付产出。
**当前状态：** Sprint 1（流程中心）✅ + Sprint 2（消息中心）✅ **已完成**。下一阶段建议先修复基础设施问题，再进入 Sprint 3。

### 项目模块结构（Phase 2 新增）

```
platform-server/
├── platform-shared/           (新建) — 共享层，供 IoT/Data 等独立项目复用
│   ├── platform-common        (已有，提升为共享)
│   ├── platform-user-api      (新建) — 用户中心 Feign 客户端
│   └── platform-auth-sdk      (新建) — 认证 SDK
├── platform-common            (已有)
├── platform-gateway           (已有)
├── platform-auth              (已有)
├── platform-user              (已有)
├── platform-ops               (新建) — 运营管理（Phase 1C）
├── platform-workflow          (新建) — 流程中心（Sprint 1）
├── platform-message      (新建) — 消息中心（Sprint 2）
├── platform-application       (新建) — 应用中心（Sprint 3）
├── platform-content           (新建) — 内容中心（Sprint 3 附属）
├── platform-task              (新建) — 任务中心（Sprint 3 附属）
├── platform-operation         (新建) — 运营中心（Sprint 4）
└── platform-portal            (新建) — 门户中心（Phase 5，通用展示层）
```

---

### Sprint 1：流程中心 ✅（已完成）

**依赖：** 用户中心（已完成）  
**理由：** 流程中心是业务中台核心能力，被多个后续模块依赖（任务中心、运营中心、物联中台规则引擎）。  
**实际状态（2026-05-28 代码验证）：** 全部完成。含 SVG 自定义设计器、请假申请、候选人配置（含人员/岗位 Tab 切换）。

> **⚠️ 决策点：** 启动前确认是否需要 Flowable Enterprise 版（2025.1.x）的 AI Agent Task 功能。开源版 6.8.1 满足 BPMN 审批需求，AI 审批需另行购买许可。

#### 后端任务

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | Maven 模块创建 + Flowable 6.8.1 集成 + 数据源配置 | 1h | `platform-workflow` 启动成功 |
| 2 | 流程定义 API：CRUD + 发布/挂起 | 3h | BPMN 文件可部署，Swagger 验证 |
| 3 | 流程实例 API：发起/撤销/删除 | 2h | 实例状态正确流转 |
| 4 | 任务 API：待办/已办/审批/驳回/转办 | 3h | 任务分配正确，历史记录完整 |
| 5 | 流程监控 API：实例追踪 + 效率统计 | 2h | 流程图高亮当前节点 |
| 6 | 与用户中心对接：候选人/角色匹配 | 2h | 任务分配按用户角色过滤 |
| 7 | Flowable 配置优化：线程池 + 超时 + 事件监听 | 1h | 并发审批性能达标 |

#### 前端任务

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 流程定义管理页：列表 + 设计器入口 | 3h | `npm install bpmn-js ^11.x`，可拖拽建模 |
| 2 | 我的待办页：表格 + 审批弹窗 | 3h | 驳回/通过/转办操作正常 |
| 3 | 流程监控页：实例列表 + 进度追踪 | 2h | 流程图高亮 + 日志时间线 |
| 4 | 路由 + 侧边栏菜单 | 0.5h | `/workflow/definition`, `/workflow/task`, `/workflow/monitor` |

---

### Sprint 2：消息中心（已完成 ✅）

**服务名：** `platform-message`（端口 8085）  
**依赖：** 用户中心（已完成）  
**理由：** 流程审批的待办通知、审批结果推送需要消息中心支撑。消息中心也是后续运营中心、物联中台报警通知的基础。  
**实际状态（2026-05-28 代码验证）：** P0~P5 **全部完成**，含 P4 消息历史记录页。

#### 架构设计

```
platform-message
├── channel/                    # 消息渠道抽象
│   ├── ChannelSender (接口)    # 统一发送接口
│   ├── SmsSender               # 阿里云短信（策略实现）
│   ├── SiteMessageSender       # 站内信（策略实现）
│   ├── AppPushSender           # App推送（预留）
│   └── EmailSender             # 邮件（预留）
├── config/                     # 配置管理
│   ├── ChannelConfig           # 渠道参数配置（JSON存储）
│   └── SmsConfig               # 阿里云短信专用配置
├── template/                   # 消息模板
│   ├── TemplateService         # 模板CRUD + 变量解析
│   └── TemplateVariable        # 变量替换引擎
├── record/                     # 消息记录
│   ├── MessageRecord           # 发送记录追踪
│   └── SendCallback            # 发送状态回调
└── site/                       # 站内信（对内API）
    └── SiteMessageService      # 站内信收发
```

#### 数据库表

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `sys_message` | 站内信（从 platform-user 迁移） | title, content, type, sender_id, receiver_id, read_status |
| `sys_message_channel` | 消息渠道配置 | channel_code, channel_name, config_json(JSON), status |
| `sys_message_template` | 消息模板 | template_code, channel_code, sign_name, template_id, template_content, params_json |
| `sys_message_record` | 消息发送记录 | channel_code, template_id, receiver_address, send_status, send_time, error_msg, business_type, business_id |

#### 后端任务（全部 ✅ 代码验证完成）

| # | 任务 | 代码确认 | 状态 |
|---|------|---------|------|
| 1 | Maven 模块创建 + 独立数据库 `platform_message` + Flyway 4 张表 + platform-user 代码迁移 | `PlatformMessageApplication.java` + `V1__init_message_tables.sql` | ✅ |
| 2 | 渠道配置 CRUD API | `MessageChannelController.java` | ✅ |
| 3 | 阿里云短信 SDK 集成 + SmsSender（占位模拟，只记日志） | `SmsSender.java` — 占位模式 | ✅ |
| 4 | 站内信发送 + 查询 API | `SiteMessageController.java` | ✅ |
| 5 | ChannelSender 接口 + 策略选择器 | `ChannelSender.java` + `ChannelSenderRegistry.java` | ✅ |
| 6 | 消息模板 CRUD API + 变量解析引擎 | `MessageTemplateController.java` | ✅ |
| 7 | 消息发送 API（单条/批量/测试） | `MessageRecordController.java` — send/test-send | ✅ |
| 8 | 消息记录 API（分页/详情/重发/删除） | `MessageRecordController.java` — page/detail/resend/delete | ✅ |
| 9 | Kafka 异步发送 + 消费端回调更新发送状态 | `MessageSendConsumer.java` — 重试 3 次 | ✅ |
| 10 | Gateway 添加 `/message/**` 路由 | `platform-gateway/application.yml:109-113` | ✅ |

#### 前端任务（全部 ✅ 代码验证完成）

| # | 任务 | 代码确认 | 状态 |
|---|------|---------|------|
| 1 | 消息记录列表页（渠道/状态/时间/关键词筛选，分页） | `Record.vue` — 216 行 | ✅ |
| 2 | 消息记录详情弹窗 + 重发按钮 | `Record.vue` — 详情 dialog + resend | ✅ |
| 3 | 渠道配置管理页（JSON配置器） | `Channel.vue` — 212 行 | ✅ |
| 4 | 渠道测试发送弹窗 | `Channel.vue` — test-send dialog | ✅ |
| 5 | 短信模板管理页（变量定义表格） | `Template.vue` — 200 行 | ✅ |
| 6 | 站内信列表页（类型筛选/搜索/分页/已读/删除） | `List.vue` — 251 行 | ✅ |
| 7 | 站内信详情页（自动标记已读） | `Detail.vue` — 114 行 | ✅ |
| 8 | 侧边栏菜单：消息记录/渠道/模板/站内信 4 个子菜单 | `router/index.ts` — 5 条路由 | ✅ |
| 9 | 铃铛弹窗（流程通知+系统消息两栏，角标合并，15s 轮询） | `Layout.vue` — 321 行 | ✅ |

#### 路由对照

| Gateway 路由 | 后端服务 | 端口 |
|-------------|---------|------|
| `/message/channel/**` | platform-message | 8085 |
| `/message/template/**` | platform-message | 8085 |
| `/message/record/**` | platform-message | 8085 |
| `/message/site/**` | platform-message | 8085 |

---

### Sprint 3：应用中心 + 内容中心 + 任务中心（3 周）

**依赖：** 用户中心（已完成）、流程中心（Sprint 1）、消息中心（Sprint 2）  
**理由：** 这三个中心依赖流程引擎和消息通知能力，在核心基础就绪后统一补齐。

#### 应用中心（platform-application）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 应用 API：CRUD + 分类 + 上下架 | 3h | 应用状态流转（草稿→上架→下架） |
| 2 | 应用安装 API：安装到租户 + 授权 | 2h | 安装后租户可见 |

#### 内容中心（platform-content）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 栏目 API：树形结构 CRUD | 1.5h | 栏目层级正确 |
| 2 | 文章 API：CRUD + WangEditor 富文本 | 2h | 文章渲染正常 |
| 3 | 公告 API：CRUD + 置顶 | 1h | 置顶公告置顶显示 |

#### 任务中心（platform-task）

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 任务定义 API：类型 + 模板 CRUD | 1.5h | 模板可关联审批流程 |
| 2 | 任务实例 API：创建/分配/处理/关闭 | 2h | 任务状态正确流转 |
| 3 | 任务统计 API：待办/完成/超时 | 1h | Dashboard 图表数据源 |

#### 前端通用

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 应用管理页 + 应用市场页 | 3h | 安装流程端到端 |
| 2 | 文章/公告管理页 | 3h | WangEditor 集成正常 |
| 3 | 任务中心页 + 统计图表 | 3h | 数据来自任务统计 API |

---

### Sprint 4：运营中心（2 周）

**依赖：** 用户中心（已完成）、流程中心（Sprint 1）、消息中心（Sprint 2）、应用中心（Sprint 3）  
**理由：** 运营中心需要整合用户、流程、消息、应用等能力，提供运营分析、企业管理、计费结算等通用运营能力。

#### 后端任务

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | Maven 模块创建 + 表结构（enterprise, billing, analysis） | 1h | `platform-operation` 启动成功 |
| 2 | 企业管理 API：企业档案 + 组织架构 | 2h | 企业层级关系正确 |
| 3 | 运营分析 API：数据统计 + 报表生成 | 3h | 报表数据准确 |
| 4 | 计费结算 API：费用计算 + 账单生成 | 3h | 计费规则可配置 |
| 5 | 运营配置 API：运营参数 + 规则管理 | 2h | 配置实时生效 |

#### 前端任务

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 企业管理页 | 3h | 企业档案 CRUD |
| 2 | 运营分析页：数据报表 + 图表 | 3h | 图表数据准确 |
| 3 | 计费结算页：账单列表 + 详情 | 2h | 账单计算正确 |

---

## 7. Phase 3 — 物联中台（独立项目）


> **定位：** 独立子项目 `platform-iot`，复用 `platform-shared` 共享层（用户中心 API 客户端 + 认证 SDK）。
> **前置条件：** 依赖 Phase 2 Sprint 1 流程中心（规则引擎触发审批）、Sprint 2 消息中心（报警通知）。
> **团队建议：** 物联团队独立开发，不与 Phase 2 争抢人力。

### 项目结构

```
platform-iot/
├── iot-common              # IoT 公共模块（依赖 platform-shared）
├── iot-device              # 设备管理服务
├── iot-rule-engine         # 规则引擎服务（依赖 platform-workflow API）
├── iot-data-collect        # 数据采集服务
├── iot-alarm               # 报警服务（依赖 platform-message API）
├── iot-gateway             # 物联网关（MQTT/CoAP 接入）
└── iot-visual              # 3D 可视化服务
```

### 共享层依赖

| 共享模块 | 用途 | 来源 |
|----------|------|------|
| platform-user-api | 设备关联用户/组织 | platform-shared |
| platform-auth-sdk | IoT 服务认证鉴权 | platform-shared |
| platform-message-api | 报警消息通知 | Phase 2 Sprint 2 |
| platform-workflow-api | 设备联动触发审批 | Phase 2 Sprint 1 |

### 核心功能

| 功能 | 说明 |
|------|------|
| 设备接入 | MQTT/HTTP/CoAP 协议接入，设备注册、认证 |
| 设备管理 | 设备档案、设备分组、设备映射、固件管理 |
| 规则引擎 | 条件触发 + 动作执行（联动、报警、通知） |
| 数据采集 | 时序数据采集（InfluxDB/TDengine） |
| 报警管理 | 报警规则、报警记录、报警处理 |
| 场景联动 | 场景编排、手动/自动执行、定时任务 |

---

## 8. Phase 4 — 数据中台（独立项目）

> **定位：** 独立子项目 `platform-data`，复用 `platform-shared` 共享层。
> **前置条件：** 建议 Phase 2 核心服务运行稳定后再启动，避免资源争抢。
> **轻量替代（Phase 1-2 过渡）：** 数据采集 → Feign 调用 + 定时 SQL 导出；OLAP → MySQL 汇总表。大数据组件到 Phase 4 按需引入。

### 项目结构

```
platform-data/
├── data-common             # 数据中台公共模块（依赖 platform-shared）
├── data-collect            # 数据采集（DataX/Flink CDC）
├── data-model              # 数据建模（指标/主题/大屏）
├── data-govern             # 数据治理（元数据/质量/主数据）
├── data-service            # 数据服务 API
└── data-visual             # 大屏可视化
```

---

## 9. Phase 5 — 门户中心（通用展示层）

> **定位：** 通用展示层，提供多风格主题、可视化配置、场景化组件能力。
> **前置条件：** 依赖 Phase 2 消息中心（消息组件）、流程中心（待办组件）、Phase 3 物联中台（设备状态组件）。
> **理由：** 门户中心作为通用展示层，需要整合所有底层能力，因此放在最后实施。

### 后端任务

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | Maven 模块创建 + 表结构（portal, theme） | 1h | `platform-portal` 启动成功 |
| 2 | 门户 API：多门户 CRUD + 主题绑定 | 2h | 门户列表返回含主题配置 |
| 3 | 门户配置 API：组件布局 CRUD（JSON 存储） | 2h | 布局 JSON 可保存/回读 |
| 4 | 门户发布 API：发布/下线/版本管理 | 1.5h | 发布后前端可见，下线后不可见 |
| 5 | 主题 API：主题 CRUD + 切换 | 1.5h | 浅色/深色/自定义主题 |

### 前端任务

| # | 任务 | 预计 | 检查项 |
|---|------|------|--------|
| 1 | 门户管理页：门户列表 + 布局配置 | 4h | 拖拽组件到页面区域 |
| 2 | 主题管理页：主题 CRUD + 实时预览 | 2h | 切换主题页面样式同步变化 |
| 3 | 门户前台首页：按配置渲染组件（含消息/待办/设备状态组件） | 3h | 首页布局与后台配置一致 |

---

## 10. 前端管理后台（跟随各 Sprint）

> **调整说明：** 前端页面嵌入每个 Sprint 的任务中。每个中心的后端 API 完成后立即开发对应前端页面，避免后端做完等前端。

### 9.1 前端任务分布

| 后端 Sprint | 前端页面 | 同步开发 |
|------------|----------|---------|
| Phase 1C：运营管理 | 独立应用 `platform-ops-admin` | ✅ 独立部署 |
| Sprint 1：流程中心 | 流程定义/待办/监控页 | ✅ 在本 Sprint 内 |
| Sprint 2：消息中心 | 模板/记录/消息角标 | ✅ 在本 Sprint 内 |
| Sprint 3：应用+内容+任务 | 应用市场/文章/任务中心 | ✅ 在本 Sprint 内 |
| Sprint 4：运营中心 | 企业管理/运营分析/计费结算 | ✅ 在本 Sprint 内 |
| Phase 5：门户中心 | 门户配置/主题/前台首页 | ✅ 在本 Phase 内 |

前后端页面完整清单：

```
platform-admin/src/views/
├── workflow/                # 流程管理 — Sprint 1
│   ├── Definition.vue       # 流程定义
│   ├── TaskTodo.vue         # 我的待办
│   └── Monitor.vue          # 流程监控
├── notification/            # 消息中心 — Sprint 2
│   ├── Template.vue         # 消息模板
│   └── Record.vue           # 消息记录
├── application/             # 应用中心 — Sprint 3
│   ├── AppManage.vue        # 应用管理
│   └── AppMarket.vue        # 应用市场
├── content/                 # 内容中心 — Sprint 3
│   ├── Article.vue          # 文章管理
│   ├── Category.vue         # 栏目管理
│   └── Notice.vue           # 公告管理
├── task/                    # 任务中心 — Sprint 3
│   ├── TaskList.vue         # 任务列表
│   └── TaskStats.vue        # 任务统计
├── operation/               # 运营中心 — Sprint 4
│   ├── Enterprise.vue       # 企业管理
│   ├── Analysis.vue         # 运营分析
│   └── Billing.vue          # 计费结算
├── platform-ops-admin/        # 运营管理 — Phase 1C（独立应用）
│   ├── Login.vue              # 登录页
│   ├── Layout.vue             # 布局
│   ├── Tenant.vue             # 租户管理
│   ├── Storage.vue            # 对象存储配置
│   ├── Gateway.vue            # 服务网关管理
│   └── Audit.vue              # 日志审计
└── portal/                  # 门户中心 — Phase 5
    ├── PortalManage.vue     # 门户管理
    ├── ThemeManage.vue      # 主题管理
    └── PortalHome.vue       # 门户首页
```

---

## 11. CI/CD 与测试策略

### 11.1 CI/CD 流水线

| 阶段 | 工具 | 说明 |
|------|------|------|
| 代码提交 | Git | 提交到 main/develop 分支触发 |
| 代码扫描 | SonarQube | 静态代码分析，覆盖率 ≥ 70% |
| 单元测试 | JUnit 5 + Mockito | 核心业务逻辑单元测试 |
| 集成测试 | Spring Boot Test | 数据库 + Redis 集成测试 |
| 构建打包 | Maven + Docker | 生成 JAR + Docker 镜像 |
| 部署 | Docker Compose / K8s | 自动部署到测试环境 |

### 11.2 CI 增强

| # | 任务 | 说明 | 预计 |
|---|------|------|------|
| 1 | **Maven 依赖分析**：`mvn dependency:analyze` 加入 pre-commit hooks 或 CI 流水线 | 检测未使用/未声明的依赖，防止版本漂移 | 0.5h |
| 2 | **OpenAPI 3.0 导出**：每 Sprint 产出 `docs/api/{service}-api.yaml` | 使用 springdoc 的 `springdoc.api-docs.enabled=true` 导出 | 1h/Sprint |
| 3 | **开发者指南**：M1 前完成《开发者指南》（环境搭建/模块说明/调试方法） | 存放于 `docs/developer-guide.md` | 2h |

### 11.3 测试策略

| 测试类型 | 覆盖率要求 | 说明 |
|----------|-----------|------|
| 单元测试 | ≥ 70% | Service 层核心逻辑 |
| 集成测试 | 关键路径 100% | 数据库操作、外部服务调用 |
| 契约测试 | 服务间接口 | Spring Cloud Contract 验证接口兼容性 |
| E2E 测试 | 核心流程 | Playwright 自动化测试 |
| 混沌测试 | 关键服务 | 注入网络延迟、服务宕机等故障 |

### 11.4 监控告警体系

| 组件 | 用途 | 部署方式 |
|------|------|---------|
| ELK | 日志聚合 | Docker Compose（已有） |
| SkyWalking | 链路追踪 | 独立部署，Agent 注入 |
| Prometheus + Grafana | 指标监控 | Docker Compose |
| Sentinel | 熔断限流 | Spring Cloud Alibaba 内置 |

---

## 12. 技术栈版本对照表（推荐）

| 组件 | 当前版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| Spring Boot | 3.2.0 | **3.2.4** | 官方稳定版本 |
| Spring Cloud | 2023.x | **2023.0.1** | 与 Spring Boot 3.2.4 匹配 |
| Spring Cloud Alibaba | 2023.0.3.2 | **2023.0.1.0** | 官方版本矩阵 |
| Flowable | 6.8.0 | **6.8.1** | Jakarta EE 兼容性更好 |
| Sa-Token | - | **1.43.0** | 如需使用，Spring Boot 3 用 spring-boot3-starter |
| XXL-JOB | 2.4.0 | 2.4.0 ✓ | 当前版本合适 |
| Nacos | 2.2.3 | 2.3.2 | Spring Cloud Alibaba 2023.0.1.0 配套 |
| Sentinel | - | 1.8.6 | Spring Cloud Alibaba 配套 |
| Kafka | 3.5+ | 3.5+ ✓ | 当前版本合适 |
| MySQL | 8.0 | 8.0 ✓ | 当前版本合适 |
| Redis | 7.x | 7.x ✓ | 当前版本合适 |

---

## 13. 里程碑节点

### 13.1 已完成（v6.0 及之前，2026-05-15 ~ 2026-05-28）

| 里程碑 | 时间 | 关键验收点 | 状态 |
|--------|------|-----------|------|
| M1 | 2026-05-15 | 用户中心 + 缓存 + 消息就绪 | ✅ |
| M2 | 2026-05-22 | 流程中心 + 消息中心就绪 | ✅ |
| M3 | 2026-05-28 | 运营中心 + 监控告警 + ELK 日志聚合 | ✅ |

### 13.2 下一阶段（v7.0，2026-06-03 起）

完整路线图见 [`中台建设中长期规划.md` 第六章](中台建设中长期规划.md#六实施路线图)。本节仅列里程碑节点：

| 里程碑 | 预期时间 | 关键验收点 | 优先级 |
|--------|---------|-----------|------|
| **M4 Q3 2026 W1-2** | 2026-07 | P0-1 多租户拦截器（防越权事故）| 🔴 |
| **M5 Q3 2026 W3-4** | 2026-08 | P0-2 数据权限 data_scope（行级权限）| 🔴 |
| **M6 Q3 2026 W5-12** | 2026-09 | P1-1 链路追踪 + L2 性能优化（A/B/C 三阶段）| 🟠 |
| **M7 Q4 2026 W1-8** | 2026-11 | P0-3 SSO / OAuth2 + P0-4 业务能力地图 | 🟠 |
| **M8 Q4 2026 W9-13** | 2026-12 | P1-2/3/4 可观测全套 + P2-1/2/3 开放基础 | 🟡 |
| **M9 Q1 2027 W1-13** | 2027-03 | P2-4 SDK + P3 韧性 + P4 业务中台 | 🟢 |

---

## 14. 参考资料

| 编号 | 来源 | 链接 |
|------|------|------|
| [1] | Spring Cloud Alibaba 版本对照表 | https://sca.aliyun.com/docs/2023/overview/version-explain/ |
| [2] | Flowable Spring Boot 3 集成指南 | https://blog.csdn.net/u01353878/article/details/151817478 |
| [3] | Sa-Token 最新版本 | https://gitee.com/dromara/sa-token/releases |
| [4] | XXL-JOB 最佳实践 | https://blog.csdn.net/RickyIT/article/details/158291204 |
| [5] | 微服务架构实施指南 | https://blog.csdn.net/shaobingj126/article/details/150395012 |
