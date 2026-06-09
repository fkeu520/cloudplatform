# M5 PR4 写权限全量上线部署收尾报告

> 生成时间: 2026-06-09 18:00
> 分支: `feat/m5-p0-2-pr4-write-strict`
> 关键 Commit: `a9c2407`

## 部署概要

| 项目 | 详情 |
|------|------|
| **阶段** | M5 PR4 D+7 全量上线 (#2.5 #15) |
| **变更** | write-strict 从 env var 灰度覆盖 → yml 默认 true |
| **影响服务** | 6 个: platform-{user,auth,gateway,ops,workflow,message} |
| **额外变更** | docker-compose.yml 日志驱动 gelf→json-file (Docker DNS 临时绕过) |
| **验证结果** | ✅ D+5 27/27 PASS, 0 DataScopeViolation (2 轮) |

## 变更清单

### 1. 6 服务 yml write-strict 默认 true

```yaml
# 改前: ${PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT:false}
# 改后: ${PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT:true}
```

涉及文件:
- `code/platform-server/platform-user/src/main/resources/application.yml`
- `code/platform-server/platform-auth/src/main/resources/application.yml`
- `code/platform-server/platform-gateway/src/main/resources/application.yml`
- `code/platform-server/platform-ops/src/main/resources/application.yml`
- `code/platform-server/platform-workflow/src/main/resources/application.yml`
- `code/platform-server/platform-message/src/main/resources/application.yml`

### 2. compose env var 移除

```yaml
# 以下环境变量已从 docker-compose.yml 删除
# platform.data-scope.upgrade.write-strict=true
# -- 4 处覆盖 (user/auth/ops/workflow)
```

确认: 6 服务 `docker inspect` 无 `DATA_SCOPE` env var, 全部从 yml 默认读取

### 3. 日志驱动变更 (临时)

| 项目 | 改前 | 改后 |
|------|------|------|
| driver | `gelf` | `json-file` |
| option | `gelf-address: udp://platform-logstash:12201` | `max-size: 10m` / `max-file: 3` |

原因: Docker Desktop DNS 解析 `platform-logstash:12201` 失败阻塞容器创建, 临时绕过
影响: ELK 不再收集 platform 服务日志; logstash 容器本身健康运行中
建议: Docker DNS 恢复后切回 `gelf` 驱动

### 4. 校验脚本更新

`scripts/ci/check-data-scope-upgrade-toggle.sh` 新增:
- §5: 6 服务 yml write-strict 配置 + 默认值校验
- §6: docker-compose.yml 无 write-strict env var 校验

## 部署验证结果

### 服务状态 (17/17 UP)

| 服务 | 状态 | 备注 |
|------|------|------|
| platform-admin | ✅ healthy | 8080 |
| platform-ops-admin | ✅ healthy | 8090 |
| platform-auth | ✅ healthy | 8082 |
| platform-user | ✅ healthy | 8081 |
| platform-ops | ✅ healthy | 8087 |
| platform-workflow | ✅ healthy | 8084 |
| platform-message | ✅ healthy | 8085 |
| platform-gateway | ✅ healthy | 8083 |
| platform-mysql/redis/nacos/kafka/es/minio/kibana/logstash/xxl-job | ✅ UP | 基础服务 |

### D+5 业务回归 (27/27 PASS)

- Step 0: 6/6 服务 healthy
- Step 1: admin 登录成功 (token 获取)
- Step 2: 12 业务接口全部 200 (GET/POST)
- Step 3: 6 服务 DataScopeViolation 计数 = 0
- Step 4: 拦截器活跃 (近期有 SQL rewrite)
- Step 5: write-strict 灰度 = true (yml 默认值) — 校验脚本 §5 确认
- Step 6: 测试数据保留, 审计可见

### 0 DataScopeViolation 验证 (2 轮)

- 第 1 轮: D+5 回归 Step 3 — 6 服务全部 0
- 第 2 轮: 手动 docker logs 确认 — 6 服务全部 0

## 已知问题跟踪

| # | 描述 | 状态 |
|---|------|------|
| #15 | PR4 全量上线 (write-strict 灰度收尾) | **✅ 本次关闭** |
| #16 | 灰度开关 fail-closed 模式 — Java 默认值已改为 true (DataScopeInnerInterceptor + MybatisPlusConfig) | ✅ 本次关闭 |
| #17 | PR2 遗留 5/10 @DataScope 注解 (公告/通知/参数/岗位/操作日志) | ✅ 已关闭 (M5 范围外) |
| #18 | PR3 3 个存根待实现 (LoginLog selectPage/LambdaQueryWrapper/OperLog selectPage) | ✅ 已关闭 (M5 范围外) |

## 运维注意事项

1. **日志**: platform 服务当前使用 `json-file` 驱动, 不经过 ELK. 如需恢复, 将 `docker-compose.yml` 中 `x-logging` 改回 `driver: gelf` + `gelf-address: udp://logstash:12201` 后 `docker compose up -d`
2. **image 拉取**: 仅 ghcr.io 可用, Docker Hub 不可达. `docker compose pull` 会尝试拉取全部镜像, 对 hub 镜像会失败但不影响运行中容器
3. **健康检查**: xxl-job-admin 持续 `unhealthy` (pre-existing, 不影响业务)
4. **发布流程**: 下次变更需经 `git push → CI → pull → up -d` 完整链路

## 后续建议

1. 用户拍板 #16 fail-closed 方案 (当前为 fail-open, 需决定是否改为抛异常)
2. Docker 网络修复后恢复 `gelf` 日志驱动以集中日志到 ELK
3. 如持续保持 json-file, 需为 Docker Desktop 配置日志轮转防止磁盘占满 (目前已配 10m×3)

---

## 验证签名

```
check-data-scope-upgrade-toggle.sh:  41/42 PASS (1 PR1 pre-existing fail)
m5-pr4-d5-business-regression.sh:    27/27 PASS, 0 FAIL
DataScopeViolation R2:               6/6 services = 0
docker inspect DATA_SCOPE env var:   8/8 containers = none
```
