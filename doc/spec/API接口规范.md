# 云枢中台 API 接口规范

**版本：** v3.1
**日期：** 2026-06-04
**适用范围：** 所有后端微服务暴露的 RESTful API
**变更：** v1.0 (2026-05-15) → v3.1 (2026-06-04) - 重大版本不兼容, 新增 §6 中台特性规范 (多租户/数据权限/统一错误码/分页), §7 接口示例同步更新

---

## 一、设计原则

### 1.1 RESTful 设计

| 资源操作 | HTTP 方法 | URL 示例 | 说明 |
|---------|----------|---------|------|
| 查询列表 | GET | `/api/v1/users` | 支持分页、筛选、排序 |
| 查询详情 | GET | `/api/v1/users/{id}` | 根据 ID 查询 |
| 创建资源 | POST | `/api/v1/users` | 请求体传递数据 |
| 更新资源 | PUT | `/api/v1/users/{id}` | 全量更新 |
| 部分更新 | PATCH | `/api/v1/users/{id}` | 增量更新 |
| 删除资源 | DELETE | `/api/v1/users/{id}` | 软删除/硬删除 |
| 批量操作 | POST | `/api/v1/users/batch` | 批量创建/更新/删除 |
| 资源统计 | GET | `/api/v1/users/statistics` | 聚合统计 |
| 资源导出 | GET | `/api/v1/users/export` | 导出 Excel/CSV |
| 资源导入 | POST | `/api/v1/users/import` | 导入数据 |

### 1.2 URL 设计规范

```
https://api.cloudhub.com/api/{version}/{module}/{resource}/{action}

示例：
/api/v1/users                    # 用户列表
/api/v1/users/123                # 用户详情
/api/v1/users/123/orders         # 用户的订单（子资源）
/api/v1/users/123/orders/456     # 用户的指定订单
/api/v1/users/search             # 用户搜索
/api/v1/users/123/enable         # 用户启用（动作）
```

**URL 命名规范：**
- ✅ 全部小写，短横线连接：`user-management`
- ✅ 名词复数：`users` `orders` `departments`
- ❌ 禁止使用动词：`/getUsers` `/createOrder`
- ❌ 禁止使用驼峰：`/userManagement`
- ❌ 禁止使用下划线：`/user_management`

---

## 二、请求规范

### 2.1 请求头

| 请求头 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| `Authorization` | 是 | Bearer Token | `Bearer eyJhbGciOiJIUzI1NiIs...` |
| `Content-Type` | 是 | 请求体格式 | `application/json` |
| `Accept` | 否 | 响应格式 | `application/json` |
| `X-Request-Id` | 否 | 请求追踪 ID | `req-20240515-001` |
| `X-Client-Version` | 否 | 客户端版本 | `web-1.2.3` |
| `Accept-Language` | 否 | 语言偏好 | `zh-CN` |

### 2.2 请求体格式

```json
{
  "idempotentKey": "uuid-generated-key",
  "data": {
    "name": "张三",
    "email": "zhangsan@example.com"
  }
}
```

### 2.3 查询参数

```
GET /api/v1/users?pageNum=1&pageSize=20&status=ACTIVE&sort=-createTime

参数说明：
- pageNum: 当前页码（从1开始）
- pageSize: 每页条数（默认20，最大1000）
- status: 筛选条件
- sort: 排序字段（-表示降序，+或不填表示升序）
```

---

## 三、响应规范

### 3.1 统一响应格式

```json
{
  "code": "200",
  "message": "success",
  "data": { },
  "timestamp": 1715731200000,
  "requestId": "req-20240515-001"
}
```

### 3.2 分页响应格式

```json
{
  "code": "200",
  "message": "success",
  "data": {
    "list": [
      { "id": "1", "name": "张三" },
      { "id": "2", "name": "李四" }
    ],
    "pagination": {
      "pageNum": 1,
      "pageSize": 20,
      "total": 100,
      "pages": 5,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "timestamp": 1715731200000
}
```

### 3.3 错误响应格式

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null,
  "timestamp": 1715731200000,
  "requestId": "req-20240515-001",
  "details": [
    {
      "field": "userId",
      "message": "用户ID 999 不存在"
    }
  ]
}
```

### 3.4 HTTP 状态码使用

| 状态码 | 使用场景 | 说明 |
|--------|---------|------|
| **200** | 成功 | GET/PUT/PATCH/DELETE 成功 |
| **201** | 创建成功 | POST 创建资源成功 |
| **204** | 无内容 | DELETE 成功，不返回数据 |
| **400** | 请求参数错误 | 参数校验失败、格式错误 |
| **401** | 未认证 | Token 缺失或过期 |
| **403** | 无权限 | 没有访问权限 |
| **404** | 资源不存在 | URL 错误或资源已删除 |
| **409** | 资源冲突 | 重复提交、乐观锁冲突 |
| **429** | 请求过多 | 触发限流 |
| **500** | 服务器内部错误 | 系统异常 |
| **502** | 网关错误 | 下游服务不可用 |
| **503** | 服务不可用 | 系统维护或过载 |

---

## 四、错误码规范

### 4.1 错误码格式

```
{模块}_{错误类型}_{序号}

示例：
USER_NOT_FOUND          # 用户不存在
USER_ALREADY_EXISTS     # 用户已存在
USER_PASSWORD_ERROR     # 密码错误
ORDER_STATUS_INVALID    # 订单状态不合法
SYSTEM_ERROR            # 系统内部错误
```

### 4.2 常见错误码

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| `SUCCESS` | 200 | 成功 |
| `PARAM_ERROR` | 400 | 参数错误 |
| `UNAUTHORIZED` | 401 | 未认证 |
| `FORBIDDEN` | 403 | 无权限 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `CONFLICT` | 409 | 资源冲突 |
| `RATE_LIMIT` | 429 | 请求过于频繁 |
| `SYSTEM_ERROR` | 500 | 系统内部错误 |
| `SERVICE_UNAVAILABLE` | 503 | 服务不可用 |

### 4.3 业务错误码（示例）

```java
// 用户模块
USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "用户已存在"),
USER_PASSWORD_ERROR("USER_PASSWORD_ERROR", "密码错误"),
USER_ACCOUNT_LOCKED("USER_ACCOUNT_LOCKED", "账户已锁定"),
USER_OLD_PASSWORD_ERROR("USER_OLD_PASSWORD_ERROR", "原密码错误"),

// 订单模块
ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
ORDER_STATUS_INVALID("ORDER_STATUS_INVALID", "订单状态不合法"),
ORDER_ALREADY_PAID("ORDER_ALREADY_PAID", "订单已支付"),
ORDER_INSUFFICIENT_STOCK("ORDER_INSUFFICIENT_STOCK", "库存不足"),

// 通用
PARAM_VALIDATION_ERROR("PARAM_VALIDATION_ERROR", "参数校验失败"),
IDEMPOTENT_VIOLATION("IDEMPOTENT_VIOLATION", "重复请求"),
FILE_UPLOAD_ERROR("FILE_UPLOAD_ERROR", "文件上传失败"),
```

---

## 五、版本控制

### 5.1 URL 版本

```
/api/v1/users      # 版本 1
/api/v2/users      # 版本 2（不兼容变更）
```

### 5.2 版本升级原则

- **v1 → v2**：不兼容的 API 变更（如删除字段、修改 URL）
- **v1 内部变更**：兼容的扩展（如新增可选字段、新增接口）

---

## 六、接口文档

### 6.1 Swagger/OpenAPI 注解

```java
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Operation(summary = "获取用户列表", description = "支持分页、筛选、排序")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping
    public Result<PageResult<UserVO>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "用户状态") @RequestParam(required = false) String status) {
        // ...
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public Result<UserVO> createUser(
            @RequestBody @Valid UserCreateRequest request) {
        // ...
    }
}
```

### 6.2 文档访问

- 开发环境：`http://localhost:8080/swagger-ui.html`
- 测试环境：`https://api-test.cloudhub.com/swagger-ui.html`

---

## 七、接口示例

### 7.1 用户管理接口

```yaml
# 获取用户列表
GET /api/v1/users?pageNum=1&pageSize=20&status=ACTIVE
Request:
  Headers:
    Authorization: Bearer {token}
    Content-Type: application/json

Response 200:
  {
    "code": "SUCCESS",
    "message": "success",
    "data": {
      "list": [
        {
          "id": "10001",
          "name": "张三",
          "email": "zhangsan@example.com",
          "status": "ACTIVE",
          "createTime": "2024-01-15T10:30:00+08:00"
        }
      ],
      "pagination": {
        "pageNum": 1,
        "pageSize": 20,
        "total": 150,
        "pages": 8,
        "hasNext": true,
        "hasPrevious": false
      }
    }
  }

# 创建用户
POST /api/v1/users
Request:
  Headers:
    Authorization: Bearer {token}
    Content-Type: application/json
    X-Idempotent-Key: 550e8400-e29b-41d4-a716-446655440000
  Body:
    {
      "name": "张三",
      "email": "zhangsan@example.com",
      "phone": "13800138000",
      "role": "ADMIN"
    }

Response 201:
  {
    "code": "SUCCESS",
    "message": "创建成功",
    "data": {
      "id": "10002",
      "name": "张三",
      "email": "zhangsan@example.com",
      "status": "ACTIVE",
      "createTime": "2024-05-15T14:30:00+08:00"
    }
  }

# 更新用户
PUT /api/v1/users/10002
Request:
  Body:
    {
      "name": "张三（修改）",
      "email": "zhangsan_new@example.com"
    }

Response 200:
  {
    "code": "SUCCESS",
    "message": "更新成功",
    "data": {
      "id": "10002",
      "name": "张三（修改）",
      "email": "zhangsan_new@example.com"
    }
  }

# 删除用户
DELETE /api/v1/users/10002
Response 200:
  {
    "code": "SUCCESS",
    "message": "删除成功",
    "data": null
  }
```

---

**文档版本记录：**

| 版本 | 日期 | 变更内容 | 编制人 |
|------|------|---------|--------|
| v1.0 | 2026-05-15 | 初始版本 | AI 助手 |
| v2.0 | (未发布) | 计划中：业务字段扩展 | — |
| **v3.1** | 2026-06-04 | **重大版本**：新增 §6 中台特性规范（多租户请求头 / 统一错误码 / 分页响应 / 链路追踪 ID）| Sisyphus |

---

## 八、中台特性规范（v3.1 新增）

> **与 [中台建设中长期规划 v1.0](中台建设中长期规划.md) §四 5 层蓝图对齐**。本节定义 v3.1 引入的中台能力所需的接口规范，所有 v3.1+ 服务必须遵循。

### 8.1 多租户请求规范

#### 8.1.1 租户识别（三种方式，按优先级）

```
1. JWT Token: 优先 (JwtUtil.getTenantId 解析, 写入 ThreadLocal)
2. Header: X-Tenant-Id: 1 (备选, 仅用于无登录场景如回调/webhook)
3. 默认值: 1 (兜底, 仅 sys_oper_log / sys_login_log 审计表使用)
```

#### 8.1.2 请求头规范

| 请求头 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| `Authorization` | 是 (登录接口除外) | Bearer Token | `Bearer eyJhbGciOiJIUzI1NiIs...` |
| `X-Tenant-Id` | 否 | 租户 ID (优先级 2) | `1` |
| `X-Request-Id` | 强烈建议 | 请求追踪 ID (链路追踪) | `req-20240604-001` |
| `X-Idempotent-Key` | 写操作必填 | 幂等键 (UUID, 24h 有效) | `550e8400-e29b-41d4-a716-446655440000` |

#### 8.1.3 响应规范（多租户场景）

所有业务响应**不**额外返回 `tenantId` 字段（业务方已通过 Token 隐式持有）；如需跨租户操作，应使用**运营管理后台**专用接口（`/ops/tenant-cross/**`）。

#### 8.1.4 数据库约束

- 业务表（用户/角色/字典/配置）**必须**含 `tenant_id BIGINT NOT NULL DEFAULT 1` 字段
- 唯一键**必须**含 `tenant_id` 维度：`UNIQUE KEY (code, tenant_id, deleted)`
- 索引**建议**含 `tenant_id`：`KEY idx_tenant_id (tenant_id)`

> 详见 [P0-1-M4-完成度审计.md](P0-1-M4-完成度审计.md) §四 IGNORE_TABLES 清单。

#### 8.1.5 拦截器行为（MyBatis-Plus TenantLineInnerInterceptor）

| 行为 | 触发条件 | 期望结果 |
|------|---------|---------|
| 自动追加 `tenant_id = ?` | 业务表 + 上下文有租户 | ✅ 强制隔离 |
| 不过滤 | 19 个 IGNORE_TABLES 之一 | ✅ 平台表/审计表 |
| 紧急关闭 | `PLATFORM_TENANT_INTERCEPTOR_ENABLED=false` | ⚠️ 越权风险, 仅止血 |

> 详见 [P0-1-回滚开关设计.md](P0-1-回滚开关设计.md) 方案 A。

---

### 8.2 统一错误码规范

#### 8.2.1 错误码格式（v3.1 重构）

```
{MODULE}_{ERROR_CATEGORY}_{SPECIFIC}

示例:
USER_NOT_FOUND              # 用户不存在 (200 业务错误)
USER_ALREADY_EXISTS         # 用户已存在
USER_PASSWORD_ERROR         # 密码错误
ORDER_STATUS_INVALID        # 订单状态不合法
TENANT_CROSS_ACCESS_DENIED  # 跨租户访问被拒 (新)
TENANT_CONTEXT_MISSING      # 租户上下文缺失 (新)
```

#### 8.2.2 模块前缀

| 前缀 | 模块 |
|------|------|
| `AUTH_` | 认证授权 |
| `USER_` | 用户中心 |
| `ROLE_` | 角色管理 |
| `MENU_` | 菜单权限 |
| `ORG_` | 组织架构 |
| `DICT_` | 字典管理 |
| `CONFIG_` | 参数配置 |
| `WORKFLOW_` | 流程引擎 |
| `MESSAGE_` | 消息中心 |
| `TENANT_` | 多租户 (新) |
| `STORAGE_` | 存储管理 |
| `GATEWAY_` | 网关路由 |
| `OPS_` | 运营管理 |
| `SYSTEM_` | 系统通用 |

#### 8.2.3 错误类别（中间段）

| 类别 | 含义 | HTTP 状态 |
|------|------|----------|
| `NOT_FOUND` | 资源不存在 | 404 |
| `ALREADY_EXISTS` | 资源已存在 | 409 |
| `INVALID` | 状态/参数不合法 | 400 |
| `ERROR` | 操作失败 | 400 |
| `FORBIDDEN` | 无权限 | 403 |
| `EXPIRED` | 已过期 | 410 |
| `CROSS_` | 跨域/跨租户违规 | 403 |

#### 8.2.4 错误响应体

```json
{
  "code": "TENANT_CROSS_ACCESS_DENIED",
  "message": "跨租户访问被拒: 当前租户 1 无权访问租户 2 数据",
  "data": null,
  "timestamp": 1717500000000,
  "requestId": "req-20240604-001",
  "trace": {
    "tenantId": 1,
    "userId": 100,
    "path": "/api/v1/users/200"
  }
}
```

---

### 8.3 分页响应规范

#### 8.3.1 请求参数

```
GET /api/v1/users?pageNum=1&pageSize=20&sort=-createTime&keyword=zhang

参数:
- pageNum: 当前页码 (从 1 开始, 默认 1)
- pageSize: 每页条数 (默认 20, 最大 200)
- sort: 排序字段, "-" 前缀表示降序, 多个用逗号分隔 (例: "-createTime,+id")
- keyword: 模糊搜索 (具体字段由各接口定义)
```

#### 8.3.2 响应体

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "list": [...],
    "pagination": {
      "pageNum": 1,
      "pageSize": 20,
      "total": 150,
      "pages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "timestamp": 1717500000000
}
```

#### 8.3.3 MyBatis-Plus 集成

- 使用 `PaginationInnerInterceptor` (已配置, 见 [P0-1-M4-完成度审计.md](P0-1-M4-完成度审计.md))
- 多租户 + 分页: 拦截器**同时**生效, SQL 自动 `WHERE tenant_id = ? LIMIT ?, ?`

---

### 8.4 链路追踪 ID 规范（M6 引入，先定义规范）

> **P1-1** 链路追踪 (Zipkin) 计划 M6 启动, 本节为**预定义**规范。

| 字段 | 位置 | 来源 | 传递 |
|------|------|------|------|
| `X-Request-Id` | HTTP Header | 网关生成 (UUID) | 全链路透传 |
| `X-User-Id` | HTTP Header | 网关从 JWT 解析 | 业务服务日志 |
| `X-Tenant-Id` | HTTP Header | 网关从 JWT 解析 | 业务服务日志 |
| `traceId` | MDC (日志) | 网关/Sleuth 生成 | Logback 集成 |
| `spanId` | MDC (日志) | Sleuth 生成 | Logback 集成 |

> ELK 已支持 `traceId` 字段 (Logstash Grok 解析), 见 [环境搭建指引 §4.3](环境搭建指引.md)。

---

### 8.5 决策日志

#### 8.5.1 为什么 v1.0 → v3.1 跳号?

- v1.0 (2026-05-15) 初版
- v2.0 计划"业务字段扩展"但**未实施**, 跳过
- v3.1 (2026-06-04) 引入中台特性 (多租户/数据权限/错误码), 是不兼容变更, 跳到 v3.x

#### 8.5.2 多租户识别为什么 JWT 优先?

- JWT 一次性解析, 性能最优
- Header 备选用于 webhook / 跨服务回调 (无 Token)
- 默认值 1 仅用于 sys_oper_log 等审计表 (无业务上下文)

#### 8.5.3 错误码为什么不用 HTTP 状态码直接表达?

- HTTP 状态码粒度粗 (如 400 不能区分参数错误 vs 业务错误)
- 业务错误码 (e.g. `USER_NOT_FOUND`) 在前端 i18n 友好
- HTTP 状态码保留给网关层 (502/504 网关错误)
