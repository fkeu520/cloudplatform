# M5 PR4 D+4 端到端验证报告 (真 MySQL 8)

> 配套脚本: `scripts/ci/m5-pr4-d4-e2e-mysql.sh`
> 执行日期: 2026-06-08
> 配套代码: commit `ce5cac2` (DataScopeInnerInterceptor clear() BUG 修复)

## 1. 目标

按 `doc/M5-P0-2-实施子任务.md` §13.5 D+4 任务, 在真 MySQL 8 环境下验证:

1. **D+4 核心**: scope=2 用户访问受 @DataScope 保护的资源 → 应被 dept_id 过滤
2. **回归控制**: scope=1 (admin) 不受 data_scope 限制 → 看到全量
3. **写方法 gap 记录**: 写方法 (UserService.update/delete 等) 未加 @DataScope 注解, 仍可越权

## 2. 测试场景

| 数据 | 值 | 说明 |
|------|----|------|
| dept_a (技术部) | id=1900000000000000004 | tester_a 所属 |
| dept_b (市场部) | id=1900000000000000005 | 跨部门 |
| tester_a | id=1900000000000002001 | role 2 (data_scope=2) |
| post_a | id=1900000000000003001 | 隶属 dept_a |
| post_b | id=1900000000000003002 | 隶属 dept_b |
| admin | id=1 | role 1 (data_scope=1) |

## 3. 测试结果

### 3.1 TC-LIST-01: scope=2 用户 listByOrgId → 应仅返回 dept_a post

**调用**: `GET /post/org/1` with tester_a token (scope=2)

**期望**: 返回 list 仅含 deptId=1900000000000000004 的 post

**实际响应** (截取):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "1900000000000003001",
      "deptId": 1900000000000000004,
      "name": "技术部-后端",
      "code": "POST_A_PR4D4",
      ...
    }
  ]
}
```

**结果**: ✓ PASS
- `count_a=1` (dept_a 的 post)
- `count_b=0` (dept_b 的 post 被过滤掉)

**底层 SQL 验证** (platform-user 日志):
```sql
DataScope SQL rewrite: 
  SELECT id, ... FROM sys_post WHERE org_id = 1 AND deleted = 0 
  -> 
  SELECT id, ... FROM sys_post WHERE org_id = 1 AND deleted = 0 
     AND (dept_id = 1900000000000000004)
```

✓ @DataScope(deptAlias="dept_id") 注解 + DataScopeInnerInterceptor 协作正常, 读路径过滤生效

### 3.2 TC-LIST-02: scope=1 (admin) → 应看全部 2 个 post

**调用**: `GET /post/org/1` with admin token (scope=1)

**实际响应**: a=1 (post_a), b=1 (post_b) — 2 个 post 都返回

**底层 SQL 验证**:
```sql
DataScope SQL rewrite: 
  SELECT id, ... FROM sys_post WHERE org_id = 1 AND deleted = 0
  -> (空 fragment, 不改 SQL)
```

✓ scope=1 (全部) 走 no-op 路径, 与设计一致

## 4. 已知 Gap (M5.5+ 修复)

### 4.1 写方法未加 @DataScope 注解

| Service.method | @DataScope | 写保护? | 修复计划 |
|----------------|------------|---------|----------|
| `UserService.update` | ❌ | ❌ 越权可改他人 | M5.5 实体扩展时加 |
| `UserService.removeByIds` | ❌ | ❌ 越权可删他人 | M5.5 实体扩展时加 |
| `PostServiceImpl.update` | ❌ | ❌ 越权可改跨部门 | M5.5 实体扩展时加 |
| `PostServiceImpl.delete` | ❌ | ❌ 越权可删跨部门 | M5.5 实体扩展时加 |
| `RoleServiceImpl` | ❌ | ⚠️ sys_role 缺 dept_id 字段 | M5.5 实体扩展 |
| `OperLogService.pageList` | ✓ | ✓ 已加 (PR4 D+1) | - |

**根因**:
- PR2 实施 (2026-06-05) 仅给 2 个 list 方法加注解, 写方法未涉及
- PR4 D+1-D+3 (2026-06-08) 实现了写拦截基础设施 (DataScopeInnerInterceptor 支持 UPDATE/DELETE/INSERT), 但**未在具体 Service 方法上加注解**
- 用户原始约束: "对于必要的修改先做待办记录，后续根据实际规划路径，在对应的任务中同步处理"
- 决策: 写方法注解纳入 M5.5 实体扩展同步处理

### 4.2 受影响范围

- 当前 scope=2/3/4/5 用户**可越权**改/删其他部门用户/角色/岗位/字典/参数
- 读路径 (list/page) 已被 @DataScope 保护的接口正常工作
- 安全等级: **降级运行** (写保护未生效), 但不影响 P0-1 租户隔离 (TenantLine interceptor 仍在生效)

## 5. 验证脚本使用

```bash
# 前置: platform-mysql / platform-auth / platform-user 容器 healthy
bash scripts/ci/m5-pr4-d4-e2e-mysql.sh
```

**预期输出**:
- 10 PASS, 0 FAIL, 3 WARN
- WARN 是已记录的 gap, 不是失败

**退出码**: 0 (脚本设计: gap 不影响脚本整体通过)

## 6. 后续行动

| 任务 | 优先级 | 备注 |
|------|--------|------|
| A2 (D+5) 6 服务部署 + 业务回归 | 高 | 灰度开关维持 false, 0 影响验证 |
| A3 (D+6) 业务通知 + 培训 | 中 | scope=2/3/4/5 用户告知 |
| A4 (D+7) 写严格开关灰度 | 中 | 按业务重要性分批切 write-strict=true |
| **M5.5 实体扩展** | 中 | 同步给 UserService/PostService/RoleService 等写方法加 @DataScope |

## 7. 关键证据

- 验证脚本: `scripts/ci/m5-pr4-d4-e2e-mysql.sh` (10 PASS, 0 FAIL)
- 拦截器修复: commit `ce5cac2` (B5.1 修复 clear() 早删 bug)
- @DataScope 注解: PR2 实施 (2 Service.listByOrgId)
- 写拦截基础设施: PR4 D+1-D+3 (jsqlparser + interceptor)
