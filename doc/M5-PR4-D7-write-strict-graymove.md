# M5 PR4 D+7 写严格开关灰度发布 & 监控计划

> 发布日期: 2026-06-08  
> 配套: `doc/M5-P0-2-实施子任务.md` §13.7 灰度方案

## 1. 灰度目标

按业务重要性**分批**将 `platform.data-scope.upgrade.write-strict` 从 `false` (降级放行) 切换为 `true` (fail-closed), 使 DataScope 写拦截器在 SQL 解析失败时抛 `DataScopeViolationException` 而非静默放行。

## 2. 灰度顺序

| 顺序 | 服务 | 风险等级 | 切前验证 | 影响 | 回退 |
|------|------|---------|---------|------|------|
| **1** | platform-message | 低 | D+5 回归 27/27 通过 | 消息发送失败时返回 500 而非静默跳过 | `docker compose exec platform-message set env` |
| **2** | platform-user | 中 | D+5 回归 27/27 通过 + D+4 e2e 10/10 通过 | 用户/角色/菜单/字典等 CRUD 写失败 500 | 同上 |
| **3** | platform-workflow | 高 | 工作流冒烟测试 | 流程实例操作/审批/转办等失败 500 | 紧急回退 |
| **4** | platform-ops | 最高 | 待业务确认 | 运营后台全局配置失效 | 需业务签字 |

## 3. 操作步骤 (每个服务)

### 3.1 临时切 write-strict=true

```bash
# Step 1: 设置环境变量覆盖 yml 默认值
# 在 docker-compose.yml 对应 service 的环境变量中加一行:
#   - PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT=true

# Step 2: 重启服务
docker compose up -d platform-<service>

# Step 3: 验证服务 healthy
docker inspect --format '{{.State.Health.Status}}' platform-<service>

# Step 4: 验证接口正常 (冒烟)
curl -s -X POST http://localhost:8083/<module>/... -H "Authorization: Bearer $TOKEN" 2>&1
```

### 3.2 监控指标 (切后 5 分钟)

```bash
# 检查 DataScopeViolation 计数  
docker logs --tail 200 platform-<service> | grep -c "DataScopeViolation"

# 预期: 0 (全部 SQL 正确被改写)
# 异常: 计数 > 0 → 立即回滚
```

### 3.3 回退步骤

```bash
# 5 分钟内可回退:
# 1. 还原 docker-compose.yml 环境变量
# 2. 重启服务
docker compose up -d platform-<service>
# 3. 验证恢复正常
```

## 4. 灰度检查清单

### 4.1 前置条件

- [ ] D+5 业务回归通过 (27+ PASS, 0 FAIL)
- [ ] D+4 端到端验证通过 (10+ PASS, documented gaps)
- [ ] B 任务所有 BUG 已修复并在线 (B3, B5.1)
- [ ] 所有已有 @DataScope 注解的服务方法已验证读路径正常
- [ ] 写方法 gap (UserService.update/delete 等) 已记录, M5.5 修复

### 4.2 切换前

- [ ] 通知业务方: "写严格模式即将灰度, 5 分钟内恢复异常"
- [ ] 截图: 当前服务日志中 DataScopeViolation 计数 = 0
- [ ] 备份: docker-compose.yml 当前版本

### 4.3 切换中

- [ ] 修改 docker-compose.yml → 加 PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT=true
- [ ] git commit + push (CI 构建镜像)
- [ ] docker compose pull && docker compose up -d
- [ ] 验证 service healthy

### 4.4 切换后 (监控 5 分钟)

- [ ] 主要 API 冒烟 100% 200
- [ ] DataScopeViolation 计数 = 0
- [ ] 业务方确认无异常
- [ ] 截图: 服务日志 + API 验证

### 4.5 切换后 (监控 24 小时)

- [ ] 0 业务反馈异常
- [ ] 0 DataScopeViolation (来自日志)
- [ ] 0 API 错误率上升 (Kibana)
- [ ] 准备下一个服务的切换

## 5. 灰度启动时间表 (建议)

| 时间 | 操作 | 负责人 |
|------|------|--------|
| 2026-06-15 09:00 | Docker Desktop | 架构组 |
| 2026-06-15 09:10 | platform-message 切 write-strict=true | 架构组 |
| 2026-06-15 09:15 | 监控 5 分钟 (无异常) | 架构组 |
| 2026-06-15 09:30 | platform-user 切 write-strict=true | 架构组 |
| 2026-06-15 10:00 | 全量业务回归 (D+5 脚本) | 架构组 |
| 2026-06-16 | platform-workflow 切 write-strict=true | 架构组 + 业务 |
| 2026-06-17 | platform-ops (待确认) | 业务决定 |

## 6. 灰度完成标准

- [ ] 4 个服务全部 write-strict=true
- [ ] 连续 24 小时 0 DataScopeViolation
- [ ] 0 业务投诉 data_scope 写拦截
- [ ] 全量回归脚本 27/27 PASS
- [ ] e2e 验证脚本 10/10 PASS

## 7. 灰度回滚标准 (立即回滚)

只要以下任一条件满足:
- DataScopeViolation 计数 > 0 且非白名单
- 业务方反馈特定接口不能操作 (scope=1 admin 受影响)
- 故障等级 P0 (核心服务不可用)

**回滚命令:**
```bash
# 删除环境变量
# 重启服务
docker compose up -d platform-<service>
# 验证
docker logs --tail 100 platform-<service> | grep DataScopeViolation
curl -s http://localhost:8083/<module>/health
```

## 8. 灰度结束 (全量上线)

24 小时无异常后:
1. 将 write-strict=true 写入 application.yml 默认值
2. 移除 docker-compose.yml 中的环境变量覆盖
3. 更新 KNOWN_ISSUES #15/#16 为已解决
4. 通知全员: 数据权限写保护全量生效
5. 关闭灰度开关 (platform.data-scope.upgrade.enabled = false)
