# 进度保存 - 2026-05-29 (18:30)

## 总体状态
Docker Desktop 重置后全量初始化完成。运营后台超时问题已修复，雪花算法改造代码修改完成（待编译部署）。

---

## 已完成修改（已部署）

### 1. Docker 全量初始化
- 基础设施 10 个容器 + 业务服务 7 个容器全部重建并健康运行
- 修复 Nacos Derby 数据库损坏（清除 `nacos-data` 卷重建）
- 修复 Flyway V16 迁移失败（`sys_oper_log` 表缺失，手动建表并标记成功）
- 修复 `platform_message` 数据库权限（`platform` 用户加授权）
- 修复 MySQL 编码：`/etc/mysql/conf.d/my.cnf` 权限 777→644，字符集恢复 `utf8mb4`

### 2. 运营后台 500 超时修复
- 根因：`flyway_schema_history` 表被 user 和 ops 模块共用，版本号重叠导致迁移跳过
- 手动补列：`sys_tenant.tenant_type`、`sys_user.dept_id`/`post_id`、`sys_organization.short_name`/`full_name` 等
- 手动建表：`sys_storage_config`、`sys_app`、`sys_tenant_app`
- `TenantService.createAdmin()`：添加 `userType=1`
- `TenantService.listAdmins()`：URL 添加 `&userType=1` 过滤

### 3. 数据库初始化数据重建
- `sys_dept` + `sys_post` 新建（无 AUTO_INCREMENT），插入中文种子数据 16 条

---

## 代码修改（待编译部署）

### 雪花算法改造
- `V4__init_org_tables.sql`：`AUTO_INCREMENT` → `NOT NULL`，种子数据去掉硬编码 `id`
- `Menu.java`：`@TableId` → `@TableId(type = IdType.ASSIGN_ID)`
- `Role.java`：`@TableId` → `@TableId(type = IdType.ASSIGN_ID)`
- `OperLog.java(ops)`：新增 `@TableId(type = IdType.ASSIGN_ID)`

---

## 待解决的问题

### Flyway 共享表版本冲突
- `flyway_schema_history` 被 user 和 ops 共用，V7-V10 版本号重叠
- 短期：手动补 DDL；长期：拆分 `flyway.table` 或统一版本号管理

### Docker Desktop 间歇性 500
- WSL2 引擎问题，频繁出现 `request returned 500 Internal Server Error`
- 需手动重启 Docker Desktop 解决

### 运营后台 monitor 路由不匹配
- 前端调 `/api/monitor/health`，后端是 `/ops/monitor/health`
- 需修复 nginx 路由或前端 URL

---

## 部署命令

雪花算法代码部署：
```powershell
$env:PATH = "C:\Program Files\Docker\Docker\resources\bin;$env:PATH"
cd D:\work\AI\output\platform\code\platform-server
mvn clean package -DskipTests
cd D:\work\AI\output\platform
docker compose build platform-user platform-ops platform-auth platform-workflow platform-message
docker compose up -d platform-user platform-ops platform-auth platform-workflow platform-message
```
