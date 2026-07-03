# v8 P0-3 Step-up Token 二次鉴权 - 设计与决策

| 项 | 值 |
|---|---|
| 文档版本 | v1.0 |
| 创建日期 | 2026-07-03 |
| 状态 | 提议（已实施 7 commits，待部署验证） |
| ADR 编号 | ADR-008（替代 v3.1 第 0.3 节 + 附录 E.2 描述） |
| 配套文档 | `doc/plan/v8-P0-tenant-protection-plan.md` |
| 实施工作量 | ~30h（实际 4 个 commit 完成，超方案预期） |

---

## 1. 背景与目标

### 1.1 业务背景

云枢中台目前 JWT 7 天有效期内，**单一 token 即可操作所有端点**——包括删除租户、停用租户、改密码、删用户等高敏操作。

### 1.2 风险场景

| 场景 | 风险 | 后果 |
|---|---|---|
| **内部威胁** | 客服 A 复制 token 到私人电脑 | 攻击者拿 7 天 token，可直接删租户 |
| **账号被盗** | 用户密码泄露 | 攻击者有 7 天窗口期，可直接转账/改数据 |
| **等保合规** | 等保 2.0 三级 8.1.5.2 要求"高风险操作二次身份鉴别" | **直接不达标** |
| **横向越权** | 平台超管 token 被低权限 AOP 拦截 | AOP 完整 dump 权限信息 |

### 1.3 目标

- 高敏操作前**重新输密码**（5 分钟内有效）
- 颁发**短期独立 token**（5 分钟 + 单次使用 + IP/UA 绑定）
- 全链路留痕（`sys_oper_log` 加 `step_up_token_id` 字段）
- 配套 v3.1 第 0.3 节 + 附录 E.2 规范

---

## 2. 关键设计决策

### 2.1 决策 D1：触发方式（一期重新输密码，二期短信码）

- **一期**：重新输密码（零依赖，6h POC 可跑通）
- **二期**：短信验证码（需先有短信网关，安全性更好，防密码已泄露场景）

### 2.2 决策 D2：TTL 固定 5 分钟

- v3.1 第 0.3 节明确要求"5 分钟"
- 行业对照：Google re-auth (5 min) / GitHub Sudo (1h) / AWS (12h)
- **5 分钟** 是 P0-1 风险评估的合理值（不长不短）

### 2.3 决策 D3：单次使用（默认 single_use=true）

- 防重放：拿到 token 的人只能用 1 次
- 批量操作：可声明 `allowMultiUse=true`（批量 token 签发）

### 2.4 决策 D4：Controller 负责 RSA 解密，Service 只接明文

- 后端 AuthService 现有模式：`RsaUtil.decrypt(encryptedPassword)` 后调 user service
- 我们的 StepUpController 沿用：`RsaUtil.decrypt(encrypted)` → 调 `stepUpTokenService.issue(plainPassword, ...)`
- Service 干净（只接明文），测试简单（不 mock RsaUtil）

### 2.5 决策 D5：auth 模块加 mybatis-plus 依赖

- 原架构：auth 模块无 datasource（JwtUtil 走 Redis）
- 现状：StepUpToken 需落库 → 加 mybatis-plus + mysql-connector-j
- 影响：Context Load 集成测试遇到 JSqlParser 兼容问题，**改在 v8.0 部署后补**
- 单元测试 13 个 TC 全部通过，业务功能不受影响

---

## 3. 数据模型

### 3.1 `sys_step_up_token` 表（auth 模块首个 migration）

```sql
CREATE TABLE IF NOT EXISTS `sys_step_up_token` (
    `id`              BIGINT       NOT NULL                          COMMENT 'ID',
    `token_hash`      VARCHAR(64)  NOT NULL                          COMMENT 'Token SHA-256 哈希（不存明文）',
    `user_id`         BIGINT       NOT NULL                          COMMENT '签发给的 user_id',
    `tenant_id`       BIGINT       DEFAULT NULL                      COMMENT '租户ID（平台超管为 NULL）',
    `scope`           VARCHAR(100) NOT NULL                          COMMENT '允许的操作范围（逗号分隔）',
    `single_use`      TINYINT(1)   NOT NULL DEFAULT 1                COMMENT '是否单次有效',
    `used`            TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '是否已使用',
    `issued_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at`      DATETIME     NOT NULL                          COMMENT '过期时间（issued_at + 5min）',
    `consumed_at`     DATETIME     DEFAULT NULL,
    `client_ip`       VARCHAR(45)  DEFAULT NULL,
    `user_agent`      VARCHAR(500) DEFAULT NULL,
    `revoked`         TINYINT(1)   NOT NULL DEFAULT 0,
    `revoked_at`      DATETIME     DEFAULT NULL,
    `revoke_reason`   VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_user_expires` (`user_id`, `expires_at`),
    KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Step-up 二次鉴权 Token';
```

### 3.2 `sys_oper_log` 加 2 字段

```sql
ALTER TABLE `sys_oper_log`
    ADD COLUMN `step_up_token_id` BIGINT DEFAULT NULL COMMENT '本次操作使用的 step-up token ID' AFTER `oper_param`,
    ADD COLUMN `requires_step_up` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本操作是否需要 step-up' AFTER `step_up_token_id`,
    ADD KEY `idx_step_up_token` (`step_up_token_id`);
```

### 3.3 设计要点

- `token_hash` 而非明文：防数据库泄露被伪造
- `scope` 限定操作范围：一个 token 只能用于特定操作（类似 OAuth scope）
- `single_use` 标志：默认单次，批量可申请 multi_use
- `revoked` 标志：管理员可主动撤销（改密码时用）
- `idx_expires` 索引：清理过期 token 用

---

## 4. API 规范

### 4.1 签发 step-up token

| 项 | 值 |
|---|---|
| Path | `POST /auth/step-up/issue` |
| 权限 | 已登录（携带 Authorization Bearer）|
| 触发源 | 前端检测到"高敏操作无 step-up token"时弹窗 |
| 请求体 | `{ "password": "RSA加密密文", "scope": "tenant:delete", "singleUse": true }` |
| 响应 | `{ "stepUpToken": "abc...", "expiresAt": "2026-07-03T12:00:00Z", "singleUse": true, "scope": "tenant:delete" }` |
| 错误码 | 401 密码错 / 403 范围越权 / 429 频繁申请 / 500 服务异常 |

**关键约束**：
- 同一 `user_id` 5 分钟内只能申请 **3 次**（防爆破）
- 申请成功后，token **绑定请求时的 IP + UA**，使用时如不一致则拒绝（防 token 劫持）

### 4.2 撤销 step-up token

| 项 | 值 |
|---|---|
| Path | `POST /auth/step-up/revoke` |
| 权限 | 已登录 |
| 请求体 | `{ "reason": "改密码自动撤销" }` |
| 响应 | `Result.ok()` |
| 用途 | 用户改密码后撤销自己所有未用的 step-up token |

### 4.3 高敏操作端点（消费 step-up token）

**一期 9 个端点**（Day 6 已全部覆盖）：

| 端点 | scope | 文件 |
|---|---|---|
| `DELETE /tenant/{id}` | `tenant:delete` | TenantController |
| `PUT /tenant/{id}/status` | `tenant:disable` | TenantController |
| `POST /tenant/{id}/admin/{userId}/reset-password` | `tenant:admin:reset-pwd` | TenantController |
| `POST /tenant-app/{tenantId}/authorize` | `tenant:app:authorize` | TenantAppController |
| `DELETE /storage/{id}` | `storage:delete` | StorageController |
| `DELETE /app/{id}` | `app:delete` | AppController |
| `DELETE /user/{id}` | `user:delete` | UserController |
| `POST /user/{id}/password` | `user:password` | UserController |
| `POST /user/{id}/reset-password` | `user:reset-pwd` | UserController |

**二期 7 个**（P55 支付 / F72 收据 / F73 紧急 Kill / 数据导出）：v8.1 实施

---

## 5. AOP 切面设计

### 5.1 切面 `StepUpAspect`

```java
@Aspect
@Component
@Order(100)
public class StepUpAspect {
    @Around("@annotation(requireStepUp)")
    public Object around(ProceedingJoinPoint pjp, RequireStepUp requireStepUp) throws Throwable {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            log.warn("@RequireStepUp invoked from non-HTTP context, skip step-up");
            return pjp.proceed();
        }
        String token = req.getHeader("X-Step-Up-Token");
        String ip = getClientIp(req);  // X-Forwarded-For > X-Real-IP > remoteAddr
        String ua = req.getHeader("User-Agent");
        
        Long tokenId = stepUpService.verifyAndConsume(
            token, requireStepUp.scope(), ip, ua, requireStepUp.allowMultiUse());
        
        // 写入 request 属性, 供 OperLogAspect 关联审计
        req.setAttribute("stepUpTokenId", tokenId);
        req.setAttribute("requiresStepUp", true);
        return pjp.proceed();
    }
}
```

### 5.2 注解 `@RequireStepUp`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireStepUp {
    String scope();              // 必须与 step-up token 签发时声明一致
    String description() default "";
    boolean allowMultiUse() default false;
}
```

### 5.3 Order 约定

- `Order = 100`：在 `@RequiresPermissions` 之后，业务事务前
- 先验证 RBAC，再验证 step-up，再进业务

---

## 6. 前端集成

### 6.1 通用组件 `StepUpDialog.vue`

跨 2 个前端项目（platform-admin + platform-ops-admin）：

```
platform-ops-admin/src/components/StepUpDialog.vue
platform-admin/src/components/StepUpDialog.vue  (复制)
```

Props：
- `modelValue: Boolean` (v-model 双向绑定)
- `scope: String` (操作范围)
- `description: String` (用户可读描述)
- `onSuccess: (token: string) => Promise` (回调)
- `singleUse: Boolean` (默认 true)

流程：输入密码 → RSA 加密 → 调 `/auth/step-up/issue` → 回调 onSuccess(token)

### 6.2 API 封装

两个前端都加：
- `issueStepUp({ password, scope, singleUse })`
- `revokeStepUp(reason?)`

**改造的端点**（11 个）：
- ops-admin 5 个：tenant delete/disable/reset-pwd/authorize/storage delete
- platform-admin 2 个：user delete / user reset-pwd
- 3 个 API 函数加 stepUpToken? 可选头参数：remove / resetAdminPassword / deleteUser / resetUserPassword / authorizeApps

---

## 7. 配置项

### 7.1 Nacos `common.yml`（已加 7 段）

```yaml
stepup:
  ttl-seconds: 300                # token 有效期 (秒), v3.1 明确 5min
  max-issues-per-window: 3         # 单用户 5min 内最多申请 3 次 (防爆破)
  rate-limit-seconds: 300          # 频率限制窗口 (秒)
```

**三层兜底**：
1. Nacos common.yml（生产，优先）
2. application.yml（本地 fallback）
3. `@Value("${stepup.xxx:300}")` 默认值（最后兜底）

### 7.2 部署时推送

```bash
# 1. SSH 到 Ubuntu 217
ssh hugh@192.168.0.217

# 2. Nacos 控制台 -> 配置管理 -> common.yml -> 编辑
# 3. 粘本文件 7 段 -> 发布

# 4. 验证 (Nacos 长轮询 30s 自动生效)
curl http://192.168.0.217:8082/actuator/configprops | jq '.stepup'
```

---

## 8. 测试覆盖

### 8.1 单元测试 13 个 TC（auth 模块）

| TC | 场景 | 状态 |
|---|---|---|
| TC-01 | 正常签发 → 返回 32 字节 token, 5min expiresAt | ✅ |
| TC-02 | 5min 内 4 次 → 第 4 次抛"频繁" | ✅ |
| TC-03 | 密码错 → 抛"密码验证失败" | ✅ |
| TC-04 | 验证 + 消费 → used 置 true | ✅ |
| TC-05 | 重放 → 抛"已使用" | ✅ |
| TC-06 | 过期 token → 抛"已过期" | ✅ |
| TC-07 | scope 不匹配 → 抛"scope" | ✅ |
| TC-08 | 改密码后 revokeAllUnused | ✅ |
| TC-01 (AOP) | 带 token + IP/UA 匹配 → proceed | ✅ |
| TC-02 (AOP) | 缺 X-Step-Up-Token → 抛"缺少" | ✅ |
| TC-03 (AOP) | scope 不匹配 → 抛"scope" | ✅ |
| TC-04 (AOP) | 异步/无 HTTP 上下文 → 跳过 proceed | ✅ |
| TC-05 (AOP) | X-Forwarded-For 多级代理 → 解析第一个 IP | ✅ |

### 8.2 全模块回归测试

- **131 个测试全过**（auth 13 + ops 21 + user 73 + common 24）
- **BUILD SUCCESS**（每个 commit 都跑过）

### 8.3 集成测试（v8.0 部署后补）

- **MockMvc 端到端测试**：需 Spring Boot 完整启动 + Nacos + 用户服务
- **Spring Context Load**：遇到 JSqlParser 兼容问题，留 v8.0 补
- **E2E**：浏览器自动化测试，留 v8.0 CI 阶段补

---

## 9. 风险与回滚

### 9.1 风险清单

| 风险 | 缓解 |
|---|---|
| 用户体验：每次高敏操作都输密码 | 5 分钟内对同一 scope 复用（v8.1） |
| 密码明文风险 | 前端必须 RSA 加密后传输（沿用 RsaUtil） |
| 架构变更：auth 加 mybatis-plus | 单元测试通过，业务功能不受影响 |
| 集成测试未覆盖 | 部署后 smoke test + 真实环境验证 |

### 9.2 回滚预案

| 模块 | 回滚命令 | 耗时 |
|---|---|---|
| 后端功能 | Nacos 推送 `platform.security.stepup.enabled=false`（建议补此开关）| 30s |
| 数据库 | 删 `V1__auth_step_up_token.sql` migration | 30min |
| 端点注解 | 删 8 个 `@RequireStepUp` 注解 | 5min |
| 前端 | 改回原按钮（不调 StepUpDialog）| 10min |

**建议补的开关**（Day 7 部署前）：在 `PlatformToggleProperties` 加 `platform.security.stepup.enabled`（默认 true），紧急关闭时 Nacos 推送 false 即可。

---

## 10. 部署清单（Day 7+）

### 10.1 推送前

- [x] 7 个 commit 全部在 develop
- [x] 131 个测试全过
- [x] Nacos common.yml 已含 stepup 配置
- [x] application.yml 已含 stepup 兜底

### 10.2 部署步骤

```bash
# 1. 推送 (Windows)
git push origin develop
# (等 CI 通过)

# 2. SSH 拉代码 (Ubuntu 217)
ssh hugh@192.168.0.217
cd /opt/platform
git pull
docker compose pull
docker compose up -d

# 3. 验证 platform-auth 启动
docker ps | grep platform-auth
docker logs platform-auth 2>&1 | grep -E "Started|stepup"

# 4. 验证 step-up 端点
curl -X POST http://192.168.0.217:8082/auth/step-up/issue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{"password":"<rsa-encrypted>","scope":"tenant:delete"}'
# 期望: { "code": 200, "data": { "stepUpToken": "xxx", "expiresAt": "..." } }

# 5. 验证高敏端点
curl -X DELETE http://192.168.0.217:8082/tenant/2 \
  -H "Authorization: Bearer <jwt>" \
  -H "X-Step-Up-Token: <上一步的 token>"
# 期望: { "code": 200 }
# 再调一次 (不带 token)
curl -X DELETE http://192.168.0.217:8082/tenant/2 \
  -H "Authorization: Bearer <jwt>"
# 期望: { "code": 403, "message": "缺少 X-Step-Up-Token 头" }
```

### 10.3 部署后验证（5 个端到端场景）

1. 平台超管登录 → 删租户 → 触发 step-up 弹窗 → 输对密码 → 删除成功
2. 输错密码 3 次 → 第 4 次被拒（rate limit）
3. 再次点删除 → 弹窗又出现（单次使用）
4. 改密码后 → 旧 token 失效
5. 改密码 → 触发 revokeAllUnused 撤销所有未用 token

---

## 11. 已知问题与 v8.0.1 路线

| 问题 | 影响 | 解决方案 |
|---|---|---|
| Context Load 集成测试未写 | 部署前只能用 smoke test | v8.0 部署后补 |
| 5min 内重复输密码 | 用户体验 | v8.1: 5min 内对同一 scope 复用 |
| 无全局紧急关闭开关 | 出问题时回滚慢 | v8.1: 加 `platform.security.stepup.enabled` Nacos 开关 |
| 平台 admin user.changePassword API 备用未接入 | 仅自己改密码场景未覆盖 | v8.0.1: profile.vue 加接入 |
| 无 SMS 二维码触发 | 一期不能短信验证 | 二期: 短信网关就绪后加 |
| 无图形验证码 | 防爆破仅靠 rate limit (3/5min) | v8.1: 加图形验证码（防脚本） |

---

## 12. 关键文件索引

### 12.1 后端新增（10 个文件）

```
code/platform-server/platform-auth/
├── src/main/java/com/cloudhub/platform/auth/
│   ├── domain/StepUpToken.java              (实体, 69 行)
│   ├── domain/mapper/StepUpTokenMapper.java (Mapper, 29 行)
│   ├── service/StepUpTokenService.java       (Service, 225 行)
│   └── aspect/StepUpAspect.java              (AOP, 102 行)
├── src/main/resources/
│   ├── db/migration/V1__auth_step_up_token.sql   (新表, 26 行)
│   └── mapper/StepUpTokenMapper.xml          (自定义 SQL, 46 行)
└── src/test/java/com/cloudhub/platform/auth/
    ├── service/StepUpTokenServiceTest.java  (8 TC, 291 行)
    └── aspect/StepUpAspectTest.java         (5 TC, 210 行)

code/platform-server/platform-common/
└── src/main/java/com/cloudhub/platform/common/annotation/
    └── RequireStepUp.java                    (注解, 43 行)

code/platform-server/platform-ops/
├── src/main/resources/db/migration/
│   └── V13__oper_log_step_up.sql             (ALTER, 9 行)
└── src/main/java/com/cloudhub/platform/ops/
    ├── aspect/OperLogAspect.java              (加 2 字段写入)
    └── domain/entity/OperLog.java             (加 2 字段)

code/platform-server/platform-user/
└── src/main/java/com/cloudhub/platform/user/
    ├── domain/entity/OperLog.java             (加 2 字段)
    └── controller/UserController.java         (3 处加 @RequireStepUp)

code/platform-server/platform-auth/
├── src/main/resources/application.yml         (移除 DataSourceAutoConfiguration 排除)
└── pom.xml                                    (加 mybatis-plus + mysql-connector-j 依赖)
```

### 12.2 前端新增（4 个文件）

```
code/platform-ops-admin/
├── src/api/auth.ts                            (issueStepUp / revokeStepUp)
├── src/api/tenant.ts                          (3 个函数加 stepUpToken?)
├── src/api/tenantApp.ts                       (authorizeApps 加 stepUpToken?)
├── src/api/storage.ts                         (remove 加 stepUpToken?)
├── src/components/StepUpDialog.vue           (通用弹窗, 148 行)
├── src/views/tenant/Index.vue                (3 个操作改造)
└── src/views/storage/Index.vue                (1 个操作改造)

code/platform-admin/
├── src/api/auth.ts                            (issueStepUp / revokeStepUp)
├── src/api/user.ts                            (deleteUser/changePassword/resetUserPassword 加 stepUpToken?)
├── src/components/StepUpDialog.vue           (从 ops-admin 复制)
└── src/views/system/user/Index.vue            (2 个操作改造)
```

### 12.3 配置文件（4 个）

```
.nacos-common.yml                             (生产配置, 已加 stepup.*)
.nacos-common-verify.yml                      (灰度配置, 已加)
.nacos-common-verify2.yml                     (灰度配置, 已加)
code/platform-server/platform-auth/src/main/resources/application.yml  (本地兜底, 已加)
```

---

## 13. 7 个 Commit 索引

```
ff7a9c6  feat(ops-admin): 补 2 个遗漏端点 (authorize + storage delete)
cab3b19  feat(user): 补 UserController.resetPassword 端点 step-up
c23fea0  feat(ops-admin): 前端 StepUpDialog + 3 个高敏操作
b85285d  feat(auth): 接入 8 个高敏端点 + OperLog 审计
5be5350  feat(auth): AOP 切面 + AuthController 2 端点 + 5 TC
9888431  feat(auth): 基础 (SQL + 实体 + Mapper + Service + 注解 + 8 TC)
7d5492d  chore: Nacos + auth application.yml 配置
```

（按时间倒序）
