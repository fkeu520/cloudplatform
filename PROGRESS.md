# 进度保存 - 2026-06-02 (02:00)

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
| P2-3 | Gateway Nacos 启动竞态 | 🟡 P2 | 30 分钟 | Docker Desktop 频繁重启 | 🟡 **代码已就位** (服务全部 healthy) |
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

---

## 修改文件 (待提交)

1. `code/platform-server/platform-common/src/main/java/com/cloudhub/platform/common/config/MybatisPlusConfig.java` - 新增 `@Bean IdentifierGenerator identifierGenerator(...)` 校验 workerId/datacenterId 0-31
2. `code/platform-server/platform-user/src/main/resources/application.yml` - 加 `mybatis-plus.snowflake.worker-id=1/datacenter-id=1` + `spring.cloud.nacos.discovery.fail-fast: false`
3. `code/platform-server/platform-ops/src/main/resources/application.yml` - 加 `mybatis-plus.snowflake.worker-id=4/datacenter-id=1` + `spring.flyway.table: flyway_schema_history_ops` + nacos fail-fast=false
4. `code/platform-server/platform-message/src/main/resources/application.yml` - 加 `mybatis-plus.snowflake.worker-id=1/datacenter-id=2` + nacos fail-fast=false
5. `code/platform-server/platform-workflow/src/main/resources/application.yml` - 加 nacos fail-fast=false
6. `code/platform-server/platform-gateway/src/main/resources/application.yml` - 加 nacos fail-fast=false
7. `docker-compose.yml` - 注入 `MYBATIS_PLUS_SNOWFLAKE_WORKER_ID/DATACENTER_ID` env vars (user/ops/message),Nacos healthcheck 增强为 `HTTP 8848 + gRPC 9848`
8. `code/platform-server/platform-user/src/main/resources/db/migration/V4__init_org_tables.sql` - sys_dept/sys_post INSERT 加显式 id 列
9. `doc/项目进度.md` - 版本 v6.2 → v6.3, 雪花 ID 段标记完成 + 遗留问题章节
10. `PROGRESS.md` - 本文件 (v6.2 → v6.3, 遗留问题清单带状态)

## 数据库变更 (已执行)

1. 删除 `sys_dept WHERE id=0`
2. 删除 `sys_post WHERE id=0`
3. 重新插入 6 条部门 + 10 条岗位 (雪花风格 19 位 ID)
4. 临时清理 E2E 测试数据 (TEST_SNOWFLAKE/TEST_POST_SF)
5. **新**: P1-1 验证测试数据已清理:
   - `DELETE FROM sys_tenant WHERE tenant_code = 'P1_1_TEST'`
   - `DELETE FROM sys_message_channel WHERE channel_code = 'P1_1_TEST'`
   - admin 密码重置为 MD5('admin') (21232f297a57a5a743894a0e4a801fc3)

## 服务状态 (2026-06-02 10:42 验证)

| 服务 | 状态 | 端口 |
|------|------|------|
| platform-user | ✅ healthy | 8081 |
| platform-gateway | ✅ healthy | 8083 |
| platform-message | ✅ healthy | 8085 |
| platform-workflow | ✅ healthy | 8084 |
| platform-auth | ✅ healthy | 8082 |
| platform-ops | ✅ healthy | 8087 |
| platform-nacos | ✅ healthy | 8848/9848 |

所有 6 个后端服务 + Nacos 均 healthy。healthcheck URL 检查 `springdoc-openapi` `/v3/api-docs` 端点。

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
