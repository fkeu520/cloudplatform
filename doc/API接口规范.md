# 云枢中台 API 接口规范

**版本：** v1.0  
**日期：** 2026-05-15  
**适用范围：** 所有后端微服务暴露的 RESTful API

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
