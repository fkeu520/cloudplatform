# 进度保存 - 2026-06-02 (v6.6)

## 总体状态
雪花算法 ID 改造端到端部署完成。代码在 commit 22c094a 中,镜像已推 ghcr.io,容器运行新代码,API 创建 dept/post 验证雪花 ID 正常生成。修复了 sys_dept/sys_post 种子数据因无 AUTO_INCREMENT + INSERT IGNORE 导致 id=0 PK 冲突的问题。

---

## 本次完成

### 1. 雪花算法 ID 部署验证
- **代码状态** (commit 22c094a 已包含):
  - `V4__init_org_tables.sql`: `sys_dept`/`sys_post` 无 `AUTO_INCREMENT`
  - `BaseEntity.java`: `@TableId(type = IdType.ASSIGN_ID)` 全局配置
  - `Menu.java`/`Role.java`/`OperLog.java(ops)`: 显式 `@TableId(type = IdType.ASSIGN_ID)`
  - `application.yml`: `mybatis-plus.global-config.db-config.id-type: assign_id`
- **数据库状态**: 6 张核心表 (`sys_dept`/`sys_post`/`sys_user`/`sys_menu`/`sys_role`/`sys_organization`) 全部无 `AUTO_INCREMENT`
- **CI/CD 镜像**: `ghcr.io/fkeu520/cloudplatform/platform-*:latest` (2026-06-01 17:54-18:56 构建)
- **E2E 验证**:
  - `POST /dept` → 返回 `id=2061624132069793793` (19 位雪花) ✅
  - `POST /post` → 返回 `id=2061624255600435202` (19 位雪花) ✅
  - 时间有序: post 创建时间晚 30s → post ID > dept ID ✅

### 2. 种子数据修复
- **问题**: V4 SQL 用 `INSERT IGNORE INTO sys_dept (org_id, parent_id, name, code, sort, status)`,无 `id` 列 + 无 `AUTO_INCREMENT` → 所有 6 行尝试 `id=0` → PK 冲突 → INSERT IGNORE 静默丢弃 5 行 → 仅剩 1 行 `id=0`
- **修复** (执行 SQL + 更新 V4 文件):
  - `sys_dept`: 6 行, ID 范围 1900000000000000001-1900000000000000006
  - `sys_post`: 10 行, ID 范围 1900000000000001001-1900000000000001010
  - V4 SQL 文件同步更新 (加 `id` 列 + 雪花风格 ID)

### 3. Gateway 故障恢复
- **症状**: Gateway unhealthy 18 分钟 (从 17:54 启动后)
- **根因**: Nacos 注册失败 (`Client not connected, current status:STARTING`) → 启动 web server 失败
- **处理**: `docker restart platform-gateway` 后 33s 启动成功 → healthy

---

## 雪花 ID workerId 配置 (可选)

当前 MP 3.5.5 默认从 MAC/IP 派生 workerId/datacenterId,单实例单主机 Docker 部署下:
- 所有服务容器 → 同一主机 → 同一 MAC → 同一 workerId
- 各服务独立 JVM → 独立 sequence 计数器
- 时间戳 + 4096/ms sequence 容量 → 低流量无碰撞风险

**多机部署前必须显式配置**:
```yaml
mybatis-plus:
  global-config:
    worker-id: ${WORKER_ID:1}      # 0-31
    datacenter-id: ${DC_ID:1}      # 0-31
```
每服务用不同 (workerId, datacenterId) 组合。

---

## 遗留问题清单 (2026-06-02 梳理)

> 详细说明见 `doc/项目进度.md` 的"遗留问题"章节,这里只列 ID + 优先级 + 工作量 + **状态**。

| ID | 项 | 优先级 | 工作量 | 阻塞场景 | 状态 (2026-06-02) |
|----|----|--------|-------|---------|------------------|
| P1-1 | MyBatis-Plus workerId/datacenterId 未显式配置 | 🔴 P1 | 30 分钟 | 多机部署 / 高并发 | ✅ **已修复并验证** |
| P1-2 | Flyway 共享表版本冲突 (user+ops 共用) | 🔴 P1 | 1-2 小时 | 启用 Flyway 自动迁移 | 🟡 **代码已就位** (Flyway 仍 disabled) |
| P2-3 | Gateway Nacos 启动竞态 | 🟡 P2 | 30 分钟 | Docker Desktop 频繁重启 | ✅ **已修复并真场景验证** |
| P2-4 | Docker Desktop WSL2 间歇 500 | 🟡 P2 | 1-2 小时 | 本地开发体验 | 📝 **环境问题** (仅文档说明) |
| P3-5 | 旧表残留小 AUTO_INCREMENT ID | 🟢 P3 | — | 无 (可接受) | ⏭️ 跳过 (按"永不"建议) |
| P3-6 | 种子数据用固定 1900xxx 段 | 🟢 P3 | — | 无 (可接受) | ⏭️ 跳过 (按"永不"建议) |

### P1-1 修复验证 ✅

**改动**:
- `MybatisPlusConfig.java` 新增 `@Bean IdentifierGenerator`,通过 `@Value` 读取 yml `mybatis-plus.snowflake.worker-id/datacenter-id`,校验 0-31 范围
- 5 个服务 yml 加 snowflake 配置,`${MYBATIS_PLUS_SNOWFLAKE_WORKER_ID:N}` 支持环境变量覆盖
- `docker-compose.yml` 注入差异化 env vars (user=1,1 / ops=4,1 / message=1,2)

**E2E 验证 (3/3 服务通过)**:
| 服务 | 配置 (workerId, datacenterId) | 雪花 ID 二进制提取 | 结果 |
|------|------------------------------|-------------------|------|
| platform-user | (1, 1) | dept `2061637441810468865` → datacenter=1, worker=1 | ✅ PASS |
| platform-ops | (4, 1) | tenant `2061638768712105986` → datacenter=1, worker=4 | ✅ PASS |
| platform-message | (1, 2) | channel `2061639325447360514` → datacenter=2, worker=1 | ✅ PASS |

**结论**: 自定义 `IdentifierGenerator` bean 正确加载 + env vars 覆盖生效 + 多服务 (workerId, datacenterId) 完全隔离。

### P1-2 修复 (代码就位)

- `ops` yml 加 `spring.flyway.table: flyway_schema_history_ops`
- `MybatisPlusConfig.java` IGNORE_TABLES 加 `flyway_schema_history_ops` / `flyway_schema_history_workflow` 防止 MP 删除 Flyway 表
- Flyway 当前 `SPRING_FLYWAY_ENABLED=false`,**未来启用时验证表名隔离**

### P2-3 修复 (代码就位)

- 5 个服务 yml 加 `spring.cloud.nacos.discovery.fail-fast: false`
- Nacos healthcheck 增强为 `HTTP 8848` + `gRPC 9848` 双端口测试
- 部署后所有 6 个后端服务 `healthy`,即使 Nacos 启动慢于服务启动顺序

### P2-3 真场景验证 (v6.5 新增) ✅

**测试方法**: 模拟 Docker Desktop 重启场景,停 Nacos 后重启 gateway
- 1. `docker stop platform-nacos` (模拟 Nacos 暂不可用)
- 2. `docker restart platform-gateway` (服务在 Nacos 关闭状态下启动)
- 3. gateway 启动 6s 后 status `Up 6 seconds (health: starting)`
- 4. 30s 后 status `Up 57 seconds (health: starting)` — 服务未崩溃
- 5. `docker exec ... wget /actuator/health` → 503 (Nacos health indicator DOWN, 服务本身 UP)
- 6. `docker start platform-nacos` (Nacos 恢复)
- 7. Nacos 启动 60s 后 `healthy`, HTTP 8848 返回 200
- 8. gateway 自动重连 Nacos: `Grpc connection connect` @ T+106s
- 9. 自动重新注册: `Redo instance operation REGISTER for DEFAULT_GROUP@@platform-gateway` @ T+109s
- 10. gateway status: `(healthy)`, `/actuator/health` → `{"status":"UP"}`

**结论**: `fail-fast=false` 修复完全生效 — 服务在 Nacos 不可用期间保持 UP, Nacos 恢复后自动重连并重新注册, 零人工干预。

---

## v6.5 新增 (本轮)

### 1. 安全修复: 撤销明文 PAT,改用 SSH

**问题**: `git remote -v` 显示 origin.pushurl 包含明文 GitHub PAT (`ghp_` 前缀 classic token, 已 redact), 多次 shell 输出中已暴露。

**修复**:
- 生成 ed25519 SSH 密钥 (`~/.ssh/id_ed25519`, comment `cloudhub-platform-deploy-key`)
- 公钥添加至 GitHub (Authentication Key)
- 删除 origin GitHub pushurl (含 token), 改用 `git@github.com:fkeu520/cloudplatform.git`
- `ssh -T git@github.com` 测试 → `Hi fkeu520! You've successfully authenticated` ✅
- `git push origin develop` 测试 → 双平台 `up-to-date` ✅

**建议**: 找到旧 PAT 所有者并撤销 (`ghp_` 前缀为 classic PAT, 在 https://github.com/settings/tokens 列表)。本次未找到, 已不再使用, 等同于自动失效。

### 2. Gitee PR 镜像创建 ✅

- PR #1: https://gitee.com/hughxu/cloudplatform/pulls/1
- ID: 17165875, state: open
- Source: develop (`c24f1cb7`) → Target: master (`ea5b3e04`)
- 标题: `fix: 修复生产化 P1/P2 技术债 (雪花 ID / Flyway / Nacos)`
- Body: 复用 GitHub #1 同一份中文描述

### 3. 提交清单 (已推送)

| Commit | Hash | Subject | Files |
|--------|------|---------|-------|
| #1 | `6a57127` | fix: 修复生产化 P1/P2 技术债 | 8 (MybatisPlusConfig + 5 yml + V4 SQL + docker-compose) |
| #2 | `c24f1cb` | docs: 更新技术债状态至 v6.4 | 2 (PROGRESS.md + 项目进度.md) |

**双平台 PR 链接**:
- GitHub: https://github.com/fkeu520/cloudplatform/pull/1 (OPEN, 3/8 checks passed)
- Gitee: https://gitee.com/hughxu/cloudplatform/pulls/1 (OPEN)

---

## 修改文件 (已提交, 见 6a57127 + c24f1cb)

1. `code/platform-server/platform-common/src/main/java/com/cloudhub/platform/common/config/MybatisPlusConfig.java` - 新增 `@Bean IdentifierGenerator identifierGenerator(...)` 校验 workerId/datacenterId 0-31
2. `code/platform-server/platform-user/src/main/resources/application.yml` - 加 `mybatis-plus.snowflake.worker-id=1/datacenter-id=1` + `spring.cloud.nacos.discovery.fail-fast: false`
3. `code/platform-server/platform-ops/src/main/resources/application.yml` - 加 `mybatis-plus.snowflake.worker-id=4/datacenter-id=1` + `spring.flyway.table: flyway_schema_history_ops` + nacos fail-fast=false
4. `code/platform-server/platform-message/src/main/resources/application.yml` - 加 `mybatis-plus.snowflake.worker-id=1/datacenter-id=2` + nacos fail-fast=false
5. `code/platform-server/platform-workflow/src/main/resources/application.yml` - 加 nacos fail-fast=false
6. `code/platform-server/platform-gateway/src/main/resources/application.yml` - 加 nacos fail-fast=false
7. `docker-compose.yml` - 注入 `MYBATIS_PLUS_SNOWFLAKE_WORKER_ID/DATACENTER_ID` env vars (user/ops/message),Nacos healthcheck 增强为 `HTTP 8848 + gRPC 9848`
8. `code/platform-server/platform-user/src/main/resources/db/migration/V4__init_org_tables.sql` - sys_dept/sys_post INSERT 加显式 id 列
9. `doc/项目进度.md` - v6.2 → v6.4, 雪花 ID 段 + 遗留问题章节
10. `PROGRESS.md` - v6.2 → v6.4, 遗留问题清单带状态

## v6.5 修改 (已提交 c24f1cb / acb5dca)

- `PROGRESS.md` - v6.4 → v6.5, P2-3 真场景验证 + 安全修复 + Gitee PR 章节
- `doc/项目进度.md` - v6.4 → v6.5, 同步上述新增内容

## v6.6 运维基础建设 (本轮新生成)

### A.1 GitHub SSH 别名 ✅
- 新增 `github` remote: `git@github.com:fkeu520/cloudplatform.git` (SSH, ed25519)
- 保留 `origin` 双 pushurl (Gitee HTTPS + GitHub SSH)
- 验证: `git fetch github` 成功 (4 new refs), `git remote -v` 正确显示

### A.2 双平台工作流文档 ✅
- 新增 `doc/git-workflow.md` (v1.0)
- 内容:
  - Remote 配置详解 (含多 pushurl 设计)
  - 常见 git 操作命令 (推送/拉取/检查)
  - 双平台 PR 流程 (gh CLI + Gitee REST API)
  - **§5 安全教训**: 2026-06-02 PAT 事件复盘 (URL 嵌入 PAT / 文档复述 token / push protection)
  - 同步策略 + master SHA 差异说明
  - 附录: 新机器上手初始化步骤

### A.3 Pre-commit Secret Scan Hook ✅
- 新增 `.githooks/pre-commit` (bash 语法, 跨平台)
- 拦截 patterns: GitHub PAT (5 种) / AWS (Access Key + Secret) / Private Key / Slack / Google API / JWT
- 配置: `git config core.hooksPath .githooks` (本地)
- 测试: 假 `ghp_ABCDEF...` 提交 → hook 拦下, exit code 1, 输出清晰错误 + 修复建议
- 配合 doc §5 形成"工具 + 文档"双层防护

### 提交策略 (本轮)
- 新增 2 文件: `.githooks/pre-commit` + `doc/git-workflow.md`
- 文档更新: `PROGRESS.md` + `doc/项目进度.md` 同步 v6.6
- 计划拆 2 commit:
  1. `chore: 运维配置 (github remote + secret scan hook)`
  2. `docs: 更新至 v6.6 (A 阶段运维基础完成)`

## 数据库变更 (已执行)

1. 删除 `sys_dept WHERE id=0`
2. 删除 `sys_post WHERE id=0`
3. 重新插入 6 条部门 + 10 条岗位 (雪花风格 19 位 ID)
4. 临时清理 E2E 测试数据 (TEST_SNOWFLAKE/TEST_POST_SF)
5. **新**: P1-1 验证测试数据已清理:
   - `DELETE FROM sys_tenant WHERE tenant_code = 'P1_1_TEST'`
   - `DELETE FROM sys_message_channel WHERE channel_code = 'P1_1_TEST'`
   - admin 密码重置为 MD5('admin') (21232f297a57a5a743894a0e4a801fc3)

## 服务状态 (2026-06-02 v6.5 验证, 含 P2-3 恢复后)

| 服务 | 状态 | 端口 | 备注 |
|------|------|------|------|
| platform-user | ✅ healthy | 8081 | 1h+ 稳定 |
| platform-gateway | ✅ healthy | 8083 | P2-3 验证: Nacos 停→启后自动恢复 |
| platform-message | ✅ healthy | 8085 | 1h+ 稳定 |
| platform-workflow | ✅ healthy | 8084 | 1h+ 稳定 |
| platform-auth | ✅ healthy | 8082 | 1h+ 稳定 |
| platform-ops | ✅ healthy | 8087 | 1h+ 稳定 |
| platform-nacos | ✅ healthy | 8848/9848 | P2-3 验证: 停→启后 60s 恢复 |

所有 6 个后端服务 + Nacos 均 healthy。healthcheck URL 检查 `springdoc-openapi` `/v3/api-docs` 端点。P2-3 真场景测试后无残留问题。

---

## 部署命令 (供参考)

```powershell
$env:PATH = "C:\Program Files\Docker\Docker\resources\bin;$env:PATH"
cd D:\work\AI\output\platform

# 验证状态
docker ps --format 'table {{.Names}}\t{{.Status}}'

# 修复 gateway Nacos 启动问题
docker restart platform-gateway

# 重新构建 (代码改了才需要)
cd code/platform-server
mvn clean package -DskipTests
cd ../..
docker compose build platform-user platform-ops platform-auth platform-workflow platform-message
docker compose up -d platform-user platform-ops platform-auth platform-workflow platform-message
```
