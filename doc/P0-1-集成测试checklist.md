# P0-1 MyBatis-Plus 多租户拦截器 · 集成测试 Checklist

**版本**: v1.0  
**日期**: 2026-06-04  
**配套文档**: [`P0-1-M4-完成度审计.md`](P0-1-M4-完成度审计.md)  
**对标**: 主规划 [`中台建设中长期规划.md`](中台建设中长期规划.md) §5.1.3 验证清单

---

## 一、目的

主规划 §5.1.3 验证清单 + §5.1.4 风险与回滚要求的所有"验证项"集中到此文档。**任何一项不通过 = P0-1 不能宣告完成**。

---

## 二、范围

| 维度 | 覆盖 |
|------|------|
| 业务表隔离 | sys_user / sys_role / sys_organization / sys_dict_type / sys_dict_data / sys_config |
| 系统表不过滤 | sys_menu / sys_role_menu / sys_user_role / sys_dept / sys_post / sys_gateway_route / sys_app |
| 审计日志不过滤 | sys_oper_log / sys_login_log |
| 启动期自检 | 9999 租户无数据 + 拦截器注入检查 |
| SQL 审计 | 静态扫描 + 运行时审计 |
| 回滚演练 | 关闭拦截器后业务可恢复 |

---

## 三、前置条件

### 3.1 测试环境

| 项 | 要求 |
|----|------|
| 数据库 | Testcontainers MySQL 8.0（与生产一致） |
| 启动模式 | 真实 `application.yml` 启动（不 mock 拦截器） |
| Flyway | 启用 `baseline-on-migrate: true` + V1-V20 全量迁移 |
| 拦截器 | 真实 `MybatisPlusConfig` Bean（不替换） |
| JWT | 测试 Token 工具：`JwtUtil.createToken(userId, tenantId, role)` |

### 3.2 测试数据准备

```sql
-- 租户 1 数据
INSERT INTO sys_user (id, username, tenant_id) VALUES (101, 'user_t1', 1);
INSERT INTO sys_user (id, username, tenant_id) VALUES (102, 'user_t1_b', 1);

-- 租户 2 数据
INSERT INTO sys_user (id, username, tenant_id) VALUES (201, 'user_t2', 2);
INSERT INTO sys_user (id, username, tenant_id) VALUES (202, 'user_t2_b', 2);

-- 跨租户字典（验证 sys_dict_type）
INSERT INTO sys_dict_type (id, dict_type, tenant_id) VALUES (101, 'tenant1_dict', 1);
INSERT INTO sys_dict_type (id, dict_type, tenant_id) VALUES (201, 'tenant2_dict', 2);
```

**重要**: 测试数据用 `@Transactional + @Rollback` 自动回滚，避免污染。

---

## 四、测试用例（8 个）

### TC-01: 同租户用户可见

**目的**: 验证租户 1 登录后，**只能看到**租户 1 的用户。

**测试步骤**:
1. 注入 `UserMapper userMapper`
2. 模拟租户 1 上下文: `TenantContextHolder.setTenantId(1L)`
3. 调用 `userMapper.selectList(null)`
4. 断言: `result.size() == 2`（仅 user_t1, user_t1_b）
5. 断言: `result.stream().allMatch(u -> u.getTenantId().equals(1L))`

**通过条件**: ✅ 看到 2 条，全是 tenant_id=1

**代码骨架**:

```java
@Test
@DisplayName("TC-01: 租户 1 登录后只看到本租户用户")
void testSameTenantUserVisible() {
    TenantContextHolder.setTenantId(1L);
    List<User> users = userMapper.selectList(null);
    assertEquals(2, users.size());
    assertTrue(users.stream().allMatch(u -> u.getTenantId().equals(1L)));
    TenantContextHolder.clear();
}
```

---

### TC-02: 跨租户用户不可见

**目的**: 验证租户 1 登录后，**看不到**租户 2 的用户（**核心安全测试**）。

**测试步骤**:
1. 模拟租户 1 上下文: `TenantContextHolder.setTenantId(1L)`
2. 调用 `userMapper.selectList(null)`
3. 断言: `result.stream().noneMatch(u -> u.getTenantId().equals(2L))`
4. 断言: `result.stream().noneMatch(u -> Arrays.asList(201L, 202L).contains(u.getId()))`

**通过条件**: ✅ 看不到任何 tenant_id=2 或 id ∈ {201, 202} 的数据

**重要**: 此测试若失败 = **越权事故**，**必须立即修复后才能合并 PR**。

---

### TC-03: 跨租户角色不可见

**目的**: 验证 `sys_role` 同样按 tenant_id 隔离。

**测试步骤**:
1. 准备: 租户 1 角色 (id=101, code='T1_ADMIN', tenant_id=1)，租户 2 角色 (id=201, code='T2_ADMIN', tenant_id=2)
2. 模拟租户 1 上下文，调用 `roleMapper.selectList(null)`
3. 断言: 看不到 tenant_id=2 的角色

**通过条件**: ✅ `result.stream().noneMatch(r -> r.getTenantId().equals(2L))`

---

### TC-04: 跨租户组织不可见

**目的**: 验证 `sys_organization` 按 tenant_id 隔离（V1 唯一键 `uk_code(code, tenant_id, deleted)` 已支持）。

**测试步骤**:
1. 准备: 租户 1 组织 (id=101, code='T1_ORG', tenant_id=1)，租户 2 组织 (id=201, code='T2_ORG', tenant_id=2)
2. 模拟租户 1 上下文，调用 `orgMapper.selectList(null)`
3. 断言: 看不到 tenant_id=2 的组织

**通过条件**: ✅ 同 TC-03

---

### TC-05: 跨租户字典不可见

**目的**: 验证 `sys_dict_type` / `sys_dict_data` 按 tenant_id 隔离。

**测试步骤**:
1. 准备: 租户 1 字典 (id=101, dict_type='tenant1_dict', tenant_id=1)，租户 2 字典 (id=201, dict_type='tenant2_dict', tenant_id=2)
2. 模拟租户 1 上下文，调用 `dictTypeMapper.selectList(null)`
3. 断言: 看不到 tenant_id=2 的字典

**通过条件**: ✅ 同 TC-03

---

### TC-06: 跨租户配置不可见

**目的**: 验证 `sys_config` 按 tenant_id 隔离。

**测试步骤**:
1. 准备: 租户 1 配置 (id=101, config_key='t1.key', config_value='v1', tenant_id=1)，租户 2 配置 (id=201, config_key='t2.key', config_value='v2', tenant_id=2)
2. 模拟租户 1 上下文，调用 `configMapper.selectList(null)`
3. 断言: 看不到 tenant_id=2 的配置

**通过条件**: ✅ 同 TC-03

---

### TC-07: 系统表不受租户过滤

**目的**: 验证 IGNORE_TABLES 清单（19 个）正确生效。

**测试步骤**:
1. 准备: 多租户共享数据
   - `sys_menu` (id=1, name='系统管理') — 平台级，无 tenant_id
   - `sys_dept` (id=1, name='研发部') — 平台级，无 tenant_id
2. 模拟租户 1 上下文，调用 `menuMapper.selectList(null)`
3. 断言: 看到所有菜单（不因租户 1 而被过滤）
4. 模拟租户 2 上下文，调用 `menuMapper.selectList(null)`
5. 断言: 看到**同样的**所有菜单

**通过条件**: ✅ 租户 1 和租户 2 看到的菜单列表**完全一致**

**反向验证**:
- `sys_oper_log` 跨租户查询
- `sys_login_log` 跨租户查询
- `sys_gateway_route` 跨租户查询

---

### TC-08: 启动期越权自检

**目的**: 验证拦截器**真正生效**（主规划 §5.1.4 启动期自检）。

**测试步骤**:
1. 应用启动完成后立即执行
2. 调用 `userMapper.selectList(null)` **不设置** `TenantContextHolder`（即 `tenantId == null`）
3. 期望行为: 拦截器 `ignoreTable` 返回 `true`（无上下文时**不过滤**），但**审计日志应记录告警**
4. 或者: 显式断言 `result.size() == 0`（若设计为"无租户上下文时拒绝访问"）

**与代码现状差异**:
- 当前实现 (MybatisPlusConfig:46-50): `tenantId == null → return true`（**不过滤**）
- 主规划 §5.1.4 期望: `SELECT 1 FROM sys_user WHERE tenant_id = 9999`（**应当返回 0 行**）
- **冲突点**: 当前实现"无上下文 = 不过滤"；主规划期望"无上下文 = 拒绝"或"9999 租户无数据"

**决策建议**（待用户决定）:
- 选项 A: 维持现状（`tenantId == null → return true`），加 audit log
- 选项 B: 改为 `tenantId == null → throw BizException("缺少租户上下文")`
- 选项 C: 改为 `tenantId == null → 强制 9999 租户`（主规划思路）

**本测试用例暂标记** ⏸️ **待用户拍板后**完成。

---

## 五、SQL 审计脚本（CI 集成）

主规划 §5.1.3 要求: "CI 加 check: `grep -rn "selectList\|selectByMap" code/ --include="*.java"` 人工 review 确认无遗漏"

### 5.1 静态扫描脚本 `scripts/ci/sql-tenant-audit.sh`

```bash
#!/bin/bash
# ============================================
# SQL 越权风险扫描: 检测可能被多租户拦截器漏处理的方法调用
# ============================================
set -e

CODE_DIR="${1:-code/platform-server}"
FOUND=0

# 1. 检测所有 selectList / selectByMap / selectBatchIds 调用
echo "[1/3] 扫描 selectList / selectByMap / selectBatchIds ..."
HITS=$(grep -rn "selectList\|selectByMap\|selectBatchIds\|selectCount" \
    "$CODE_DIR" --include="*.java" --include="*.xml" 2>/dev/null || true)
if [ -n "$HITS" ]; then
    echo "$HITS"
    FOUND=1
fi

# 2. 检测原始 SQL 拼接（绕过 MyBatis-Plus 拦截器）
echo "[2/3] 扫描 @Select 注解 / 原始 SQL ..."
SQL_HITS=$(grep -rn "@Select\|<select\|<insert\|<update\|<delete" \
    "$CODE_DIR" --include="*.java" --include="*.xml" 2>/dev/null || true)
if [ -n "$SQL_HITS" ]; then
    echo "⚠️  发现 ${SQL_HITS} 处自定义 SQL，需人工确认含 tenant_id 条件"
    echo "$SQL_HITS"
    FOUND=1
fi

# 3. 检测 EntityManager.createNativeQuery / EntityManager.createQuery
echo "[3/3] 扫描 JPA 风格原生查询 ..."
JPA_HITS=$(grep -rn "createNativeQuery\|createQuery(" \
    "$CODE_DIR" --include="*.java" 2>/dev/null || true)
if [ -n "$JPA_HITS" ]; then
    echo "⚠️  发现 JPA 风格查询，需人工确认租户隔离"
    echo "$JPA_HITS"
    FOUND=1
fi

if [ $FOUND -eq 1 ]; then
    echo ""
    echo "❌ 扫描完成: 发现需人工 review 的点（见上）"
    echo "✅ 人工 review 通过后: 标记本检查为已审"
    exit 0  # 不强制失败，由 reviewer 在 PR 中确认
fi

echo "✅ 扫描通过: 未发现明显越权风险点"
exit 0
```

**集成位置**: `.github/workflows/ci.yml` 的 `backend` job 之后追加 `tenant-sql-audit` job（不阻塞合并，由 reviewer 确认）。

### 5.2 运行时审计（推荐追加）

通过 `DataSource-Proxy` 或 `p6spy` 拦截所有 SQL，断言包含 `tenant_id = ?`，**未包含则打 ERROR 日志**（不阻塞，但每次出现都触发审查）。

实现位置（待办）: `platform-common/.../audit/SqlTenantAuditor.java`

---

## 六、回滚验证

**目的**: 验证 P0-1 拦截器关闭后，业务可恢复 v3.1 之前行为（主规划原则: **可回滚**）。

**测试步骤**:

1. 启动应用时设置环境变量 `PLATFORM_TENANT_INTERCEPTOR_ENABLED=false`（参考 [`P0-1-回滚开关设计.md`](P0-1-回滚开关设计.md) 方案 A）
2. 重新运行 TC-01 ~ TC-06
3. 期望行为: 跨租户数据**全部可见**（即关闭拦截器后回到"代码无强制"状态）
4. **不应报错**：关闭拦截器不应让系统崩溃

**通过条件**: ✅ 关闭后业务功能完整，仅失去多租户隔离保护

---

## 七、自动化建议

### 7.1 现状

- `code/platform-server/platform-user/src/test/java/.../UserServiceTest.java` 存在
- `code/platform-server/platform-user/src/test/java/.../OrganizationServiceTest.java` 存在
- `code/platform-server/platform-user/src/test/java/.../MenuServiceTest.java` 存在
- **均未涉及多租户**

### 7.2 待办

| 任务 | 优先级 | 实施时间 |
|------|--------|----------|
| 新建 `platform-user/src/test/java/.../tenant/TenantIsolationTest.java` 含 TC-01~TC-08 | 🔴 P0 | M4 内 |
| 接入 `scripts/ci/sql-tenant-audit.sh` 到 CI | 🟠 P1 | M4 内 |
| 引入 `p6spy` 运行时审计 | 🟡 P2 | M4-M5 之间 |

### 7.3 实施成本估算

- TC-01~TC-08: **半天**（1 个 Testcontainers + 8 个 `@Test`）
- SQL 审计脚本: **1 小时**（grep + bash）
- p6spy 集成: **1 天**（依赖引入 + 配置 + 日志格式调优）

---

## 八、决策日志

### 8.1 为什么用 `TenantContextHolder.setTenantId()` 模拟登录而不是用 MockMvc + 真实 JWT？

- 优点: 直接 mock ThreadLocal 状态，**测试聚焦拦截器本身**
- 缺点: 跳过了 TenantFilter 链路（但 TenantFilter 是简单 JWT 解析，独立测）
- **决策**: 业务测试用 ThreadLocal mock；TenantFilter 单测单独写

### 8.2 TC-08 的"启动期自检"为什么标记 ⏸️ 待用户决定？

- 当前实现 (MybatisPlusConfig) 与主规划 §5.1.4 期望**不一致**
- 涉及安全策略：是否允许"无租户上下文访问业务表"
- 涉及定时任务 / 消息消费者等"非 HTTP 入口"的设计
- **建议**: 启动 P0-1 集成测试前，先在主规划 §5.1.4 增补"无租户上下文处理策略"小节

### 8.3 为什么 SQL 审计脚本不强制失败？

- 静态扫描**无法**自动判断"该 selectList 是否需要租户条件"（业务上下文）
- 强制失败会导致 CI 频繁阻塞，影响交付
- 折中: 扫描**不阻塞**，但**每次扫描结果都进 PR review checklist**，由 reviewer 人工确认

---

## 九、变更历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-06-04 | 初版：8 个 TC + SQL 审计脚本 + 自动化建议 |

---

## 十、相关 commit

无（本文档为测试规划，未触发代码变更）
