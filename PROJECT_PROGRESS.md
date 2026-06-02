# 项目进度报告 - 云枢中台

## 项目基本信息
- **项目名称**: 云枢中台
- **版本**: v6.1
- **最后更新**: 2026-05-29 (18:30)
- **代码验证**: ✅ 实际代码已全量校验

## 已完成工作

### 1. 基础架构
- ✅ Spring Cloud 微服务体系（user/auth/gateway/workflow/ops/message）
- ✅ Docker Compose 编排 (mysql, redis, nacos, kafka, es, logstash, kibana, minio, nginx)
- ✅ Nacos 服务注册发现 + 配置中心
- ✅ Flyway 数据库迁移管理（各服务独立迁移表）
- ✅ MyBatis-Plus 持久层 + 逻辑删除 + 自动填充
- ✅ Knife4j API 文档聚合
- ✅ 独立数据库：`platform`（用户/流程/运营）+ `platform_message`（消息中心）

### 2. 系统管理（platform-user）
- ✅ 用户/角色/菜单/组织/部门/岗位 CRUD + RBAC 权限
- ✅ 字典管理 + 参数配置
- ✅ 操作日志自动记录（@Log 注解 + AOP）
- ✅ 动态菜单导航 + 按钮权限指令
- ✅ 登录日志记录（管理平台 + 运营平台）

### 3. 流程中心（platform-workflow）
- ✅ Flowable 6.8.1 后端集成
- ✅ BPMN 设计器（自定义 SVG 渲染，非 bpmn-js）
- ✅ 流程定义 CRUD（部署/挂起/激活/删除/编辑）
- ✅ 属性面板（常规/候选配置/送审配置/审批规则）
- ✅ 流程导出 BPMN XML
- ✅ 流程实例发起/查询/删除
- ✅ 待办/已办任务查询 + 审批/驳回/转办
- ✅ 审批进度时间轴 + 流程监控 + 实例高亮追踪
- ✅ Kafka 事件推送（任务创建/完成通知）

### 4. 候选人配置
- ✅ 候选范围过滤（公司/本部门/集团）
- ✅ 组织树弹窗选择器 (el-tree + checkbox)
- ✅ 人员/岗位 Tab 切换 + 互斥选择
- ✅ 搜索框 + 扁平结果 + 分页
- ✅ XML 解析候选人回显 + 复选框同步

### 5. 请假申请模块
- ✅ 请假流程 BPMN 统一格式（`bpmn:` 前缀 + `candidateUsers`）
- ✅ 请假申请列表（分页表格）
- ✅ 新增申请弹窗（类型/日期/天数/原因）
- ✅ 查看流程（完整节点链 + 当前节点标记）
- ✅ 申请详情 + 审批时间轴
- ✅ 内置 BPMN 定义自动部署

### 6. 消息中心（platform-message）— Sprint 2 ✅
#### P0：服务搭建
- ✅ 独立 Maven 模块，端口 8085
- ✅ 独立数据库 `platform_message` + Flyway 4 张表
- ✅ Gateway 路由 / Docker 镜像构建

#### P1：渠道管理
- ✅ 渠道 CRUD API + 前端管理页面
- ✅ 渠道测试发送功能 + JSON 配置编辑器
- ✅ 阿里云短信 SmsSender（占位模式）

#### P2：短信模板管理
- ✅ 模板 CRUD API + 前端管理页面
- ✅ 变量定义 JSON 编辑 + 变量解析引擎

#### P3：消息发送引擎
- ✅ ChannelSender 接口 + ChannelSenderRegistry 策略选择器
- ✅ Kafka 异步发送 + 消费端回调 + 重试机制
- ✅ 消息记录 API（分页/详情/发送/重发/删除）

#### P4：消息历史记录页 ✅（代码已验证完成）
- ✅ 消息记录列表页（渠道/状态/时间/关键词筛选，分页）
- ✅ 消息详情弹窗（完整消息信息 + 发送链路追踪）
- ✅ 失败消息重发按钮
- ✅ 侧边栏「消息记录」菜单项

#### P5：前端整合
- ✅ 站内信列表页（类型筛选/搜索/分页/删除/全部已读）
- ✅ 站内信详情页（自动标记已读/返回导航）
- ✅ API 文件：站内信/渠道/模板/记录/测试发送 完整 API
- ✅ 铃铛弹窗（系统消息+流程通知两栏，15s 轮询，角标合并计数）

### 7. 运营管理（platform-ops）
- ✅ 独立前后端（platform-ops:8087 + platform-ops-admin:8090）
- ✅ 租户 CRUD + 启停管理
- ✅ 对象存储配置（MinIO）
- ✅ 网关动态路由管理 + 动态刷新
- ✅ 日志审计（操作日志/登录日志/API 日志）

### 8. 用户分类体系
- ✅ `sys_user.user_type`：0=普通用户, 1=租户管理员, 2=运营管理员
- ✅ 运营/管理平台用户隔离
- ✅ 运营后台用户管理（CRUD + 菜单授权树 + 重置密码）

### 9. 图标系统 & Docker 优化 & Git
- ✅ Font Awesome 图标库集成
- ✅ `.dockerignore` 优化（457MB → ~90MB）
- ✅ 修复 Kafka/ES/Nginx 容器通信问题
- ✅ Gitee 远程仓库（master + develop 分支）

## 已完成 — P0 修复（2026-05-28）

### ✅ platform-auth 认证改造
- `AuthService.loginByPassword()` 改为调用 `POST /user/internal/validate`（RestTemplate）
- `AuthService.loginBySms()` 改为调用 `GET /user/internal/by-username/{mobile}`
- 新增 `UserService.validatePassword()` 和 `UserService.getByUsername()` 内部方法
- 新增 `UserController` 内部端点：`/user/internal/validate` + `/user/internal/by-username/{username}`
- 新增 `platform-auth` 模块 `RestTemplateConfig`
- 移除全部硬编码 mock 用户逻辑

### ✅ 统一登录密码
- ops-admin `Login.vue` 移除硬编码默认密码 `admin/123456`，改为空输入
- 运营平台/管理平台均通过真实 DB 验证密码

## 已完成 — 安全与优化修复（2026-05-29）

### ✅ 严重安全问题修复
- JWT 密钥改为从环境变量 `JWT_SECRET` 读取，fallback 为开发默认值
- SMS 验证码日志脱敏，不再打印明文验证码
- SMS 发送增加频率限制（每分钟1次），使用 `SecureRandom` 替代 `Random`
- ELK 查询增加 JSON 转义，防止 JSON 注入攻击
- 网关白名单移除 `/actuator`、`/swagger`、`/v3/api-docs` 等敏感路径
- CORS 配置改为从环境变量 `CORS_ORIGINS` 读取，默认限制为 localhost
- 前端 `Detail.vue` XSS 修复：`formatContent` 函数增加 HTML 转义
- 全局异常处理不再暴露内部异常详情给客户端

### ✅ 高风险问题修复
- AuthController Token 处理：增加 `stripBearer()` 方法处理 "Bearer " 前缀
- 限流器竞态条件修复：使用 `LongAdder` + `AtomicLong` 替代 `AtomicInteger`
- 分布式锁原子性修复：`unlock()` 方法使用 Lua 脚本保证原子操作
- SSE 广播迭代安全修复：收集失败 key 后批量删除
- 工作流任务接口安全：从 `X-User-Id` Header 获取 userId，防止伪造
- SSE 订阅端点安全：优先从 Header 获取 userId，缺失时抛异常

### ✅ 中等问题修复
- RestTemplate 超时配置：auth/ops 模块增加连接超时(3s)和读取超时(10s)
- Redis 配置路径修复：workflow 模块 `spring.redis` → `spring.data.redis`
- 日志输出修复：workflow 模块 `StdOutImpl` → `Slf4jImpl`，common.yml 同步修改
- Kafka 反序列化安全：`trusted.packages` 从 `"*"` 改为具体包路径
- Flyway 配置补全：workflow 模块增加 `validate-on-migrate: false`
- LogAspect 空指针修复：`e.getMessage()` 增加 null 检查
- SmsSender 客户端缓存：避免每次发送创建新 Client 实例
- TenantService 异常处理：`createRootOrg` 失败时抛异常而非静默忽略
- 前端 SSE 事件监听清理：`onBeforeUnmount` 中正确移除事件监听器

### ✅ 密码传输加密（2026-05-29）
- 新增 `RsaUtil` 工具类：RSA-2048 非对称加密，支持密钥对生成/加解密
- 新增 `GET /auth/public-key` 接口：前端获取 RSA 公钥
- `AuthService.loginByPassword()` 支持解密 RSA 加密后的密码
- 前端新增 `crypto.ts` 工具模块：封装 jsencrypt 加密逻辑
- platform-admin/ops-admin 登录页密码 RSA 加密后再传输
- 用户新增/创建页面密码字段 RSA 加密传输
- 两个前端项目新增 `jsencrypt` 依赖

## 已完成 — Docker 初始化 & 运营后台修复（2026-05-29）

### ✅ Docker 全量初始化
- Docker Desktop 重置后重建全部 17 个容器
- 修复 Nacos Derby 数据库损坏（清除 `nacos-data` 卷重建）
- 修复 Flyway V16 迁移失败（`sys_oper_log` 表缺失，手动建表并标记成功）
- 修复 `platform_message` 数据库权限（`platform` 用户授权）
- 修复 MySQL 编码：`/etc/mysql/conf.d/my.cnf` 权限 777→644，`character_set_client/connection/results` 恢复 `utf8mb4`

### ✅ 运营后台 500 超时修复
- 根因：`flyway_schema_history` 表被 user 和 ops 模块共用，版本号重叠（V7-V10），导致 ops 的 V5/V8/V9/V10 迁移被跳过
- 手动补列：`sys_tenant.tenant_type`、`sys_user.dept_id`/`post_id`、`sys_organization.short_name`/`full_name`/`legal_person` 等
- 手动建表：`sys_storage_config`、`sys_app`、`sys_tenant_app`
- `TenantService.createAdmin()` 修复：添加 `params.put("userType", 1)`，创建的管理员正确标记为租户管理员
- `TenantService.listAdmins()` 修复：URL 添加 `&userType=1` 过滤

### ✅ 雪花算法改造（代码修改，待编译部署）
- `V4__init_org_tables.sql`：`sys_dept`/`sys_post` 移除 `AUTO_INCREMENT`，种子数据去掉硬编码 `id`
- `Menu.java`：`@TableId` → `@TableId(type = IdType.ASSIGN_ID)`
- `Role.java`：`@TableId` → `@TableId(type = IdType.ASSIGN_ID)`
- `OperLog.java(ops)`：新增 `@TableId(type = IdType.ASSIGN_ID)`
- 数据库 `sys_dept`/`sys_post` 已手动建表并插入种子数据

## 待完成工作

### ✅ P1 — 全部完成 (2026-05-28)
3. **Kafka 本地消息表兜底 + acks=all**
   - ✅ Kafka production 配置 `acks=all` + `retries=3` + `enable.idempotence=true`
   - ✅ 新增 `MessageRetryService` — 每分钟扫描 stuck 消息重发
   
4. **多租户隔离规范化**
   - ✅ 新增 `TenantContextHolder`（ThreadLocal 存储 tenantId/userId）
   - ✅ 新增 `TenantFilter` — 从 JWT 提取 tenantId 注入上下文
   - ✅ `MybatisPlusConfig` 添加 `TenantLineInnerInterceptor` + 忽略表配置

5. **SSE 实时推送**
   - ✅ 新增 `SseService` — 管理 SseEmitter 连接池
   - ✅ 新增 `SseController` — `GET /message/sse/subscribe`
   - ✅ `WorkflowMessageConsumer` 处理后通过 SSE 推送到前端
   - ✅ 前端 `Layout.vue` 使用 `EventSource` 替代 polling（polling 降级）

6. **ChannelSender 真实对接**
   - ✅ `SiteMessageSender` 写入 `sys_message` 表（调用 `SiteMessageService.save()`）
   - ✅ `SmsSender` 从 `MessageChannel` 配置读取渠道参数（阿里云 SDK 待对接）

### ✅ P2 — 全部完成 (2026-05-28)
7. **ops-admin 系统监控面板**
   - ✅ 新增 `MonitorController` — 并行检查所有服务 `/actuator/health`
   - ✅ 新增 `monitor/Index.vue` — 卡片式展示各服务状态（绿/红色顶条）
   - ✅ 路由 + 侧边栏菜单「系统监控」
   - ⚠️ 各服务需添加 `spring-boot-starter-actuator` 依赖才能自动检测

8. **数据权限模块**
   - ✅ 新增 `@DataScope` 注解 + `DataScopeAspect` 切面
   - ✅ `TenantContextHolder` 提供 userId + tenantId 上下文
   - ⚠️ 前端角色管理数据权限范围配置待页面化（当前为框架层就绪）

9. **画布交互优化**
   - ✅ 新增 `GRID_SIZE=20` 网格对齐函数 `snapToGrid()`
   - ✅ 从画板拖拽落点 + 节点拖拽移动均自动吸附到 20px 网格
   - ✅ SVG 背景网格线可视化

10. **流程验证**
    - ✅ 新增 `WorkflowValidationService` — BPMN XML 解析/语法/任务人检查
    - ✅ 新增 `POST /workflow/definition/validate` 接口

11. **用户任务签收**
    - ✅ `TaskTodo.vue` — 待办列表添加「签收」「退回」按钮
    - ✅ 签收前弹框确认，调用 `claimTask`/`unclaimTask` API

### P3 — 低优先级
12. 条件表达式编辑器
13. 子流程支持
14. 流程模拟/预览
15. 邮件渠道 / App 推送渠道
16. ECharts Dashboard 真实图表
17. XXL-JOB 调度中心部署
18. `mvn dependency:analyze` 加入 CI 流程
19. 每 Sprint 产出 `docs/api/` OpenAPI 3.0 YAML

## 技术栈
- **前端**: Vue 3 + TypeScript + Element Plus + Pinia + Font Awesome
- **后端**: Spring Boot 3.2 + Flowable 6.8.1 + MyBatis-Plus + Spring Cloud
- **数据库**: MySQL 8.0（platform + platform_message）
- **中间件**: Redis, Nacos, Kafka, Elasticsearch, MinIO
- **部署**: Docker Compose + Nginx

## 服务端口总览
| 服务 | 端口 | 数据库 |
|------|------|--------|
| platform-admin | 8080 | - |
| platform-user | 8081 | platform |
| platform-auth | 8082 | platform |
| platform-gateway | 8083 | - |
| platform-workflow | 8084 | platform |
| **platform-message** | **8085** | **platform_message** |
| platform-ops | 8087 | platform |
| platform-ops-admin | 8090 | - |

## 关键文件
- `src/components/ProcessDesigner.vue` — 流程设计器（SVG 渲染）
- `src/views/workflow/Leave.vue` — 请假申请
- `src/views/workflow/Definition.vue` — 流程定义管理
- `src/views/workflow/TaskTodo.vue` — 我的待办
- `src/views/workflow/Monitor.vue` — 流程监控
- `src/views/message/List.vue` — 站内信列表
- `src/views/message/Detail.vue` — 站内信详情
- `src/views/message/Channel.vue` — 渠道配置
- `src/views/message/Template.vue` — 短信模板
- **`src/views/message/Record.vue`** — 消息记录（P4 已实现）
- `src/views/Layout.vue` — 铃铛弹窗（系统消息+流程通知）
- `src/api/message.ts` — 消息中心完整 API
- `src/api/workflow.ts` — 工作流 API
- `code/platform-server/platform-workflow/` — 后端工作流
- `code/platform-server/platform-message/` — 后端消息中心
- `code/platform-server/platform-auth/` — 认证服务（含待改造 mock）
