# 云枢中台

> 通用中台解决方案

![Version](https://img.shields.io/badge/version-4.2-blue.svg)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![Flowable](https://img.shields.io/badge/Flowable-6.8.1-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## 项目简介

云枢中台是以数据为核心、数字运营为纽带的新一代通用中台解决方案，实现技术统一、数据统一、业务统一。

### 核心能力

- **用户中心** + RBAC 权限 + **多租户（DB 层已支持，拦截器 P0-1 待实施）**
- **流程引擎** (Flowable 6.8.1) + BPMN 设计器（候选人配置已修复）
- **消息中心** (多渠道/模板/站内信/短信/SSE)
- **运营管理** (租户/存储/网关/审计)
- **SSE 实时推送** + Kafka 异步消息
- **ELK 日志聚合** (GELF UDP + 1 天 ILM 自动清理)
- **系统监控** + 日志审计

---

## 路线图状态

| 阶段 | 时间 | 状态 |
|------|------|------|
| **Phase 1-2** (Phase 1A/B/C + Sprint 1-2) | 2026-05-15 ~ 2026-05-28 | ✅ 已完成 |
| **M4 P0-1** MyBatis-Plus 多租户拦截器 | 2026-07 | ✅ 已完成 (8 TC + 灰度开关 + null 模式) |
| **M5 P0-2** 数据权限 data_scope | 2026-08 | 🟡 基础版可用 (3+5 TC, 复杂场景入 M5+) |
| **M6 P1-1 + L2 调优 + 网关韧性 + 业务指标** | 2026-09 | 🔵 待启动 |
| **M7-M9** SSO / 可观测 / 开放 / 韧性 / 业务中台 | 2026 Q4 - 2027 Q1 | 🔵 待启动 |

> 完整路线见 [`doc/中台建设中长期规划.md`](doc/中台建设中长期规划.md)（主规划，v1.0）
> 详细里程碑见 [`doc/opencode实施计划.md`](doc/opencode实施计划.md)（v7.0）

---

## 关键决策（2026-06-03 规划更新）

| 原则 | 含义 |
|------|------|
| **可用性 > 可扩展性 > 性能** | 任何优化不得引入消息丢失、首请求延迟 |
| **不锁死技术栈** | 依赖通过配置开关控制，不删 pom 依赖 |
| **3 紧急 P0 > L2 性能优化** | 多租户/数据权限/链路追踪优先于省内存 |

完整 5 层蓝图: P0 核心 / P1 可观测 / P2 开放 / P3 韧性 / P4 业务 (见主规划第四章)

---

## 已实现架构

```
┌────────────────────────────────────────────────────────┐
│                    平台接入层                            │
│   platform-admin (8080)    platform-ops-admin (8090)   │
├────────────────────────────────────────────────────────┤
│                    微服务层                              │
│   platform-auth   platform-user   platform-workflow    │
│   platform-message   platform-ops   platform-gateway    │
├────────────────────────────────────────────────────────┤
│                    中间件层                              │
│   MySQL 8.0 | Redis 7 | Kafka | Nacos | MinIO          │
│   ES 8.12 + Logstash (GELF 1天ILM) + Kibana            │
└────────────────────────────────────────────────────────┘
```

### 技术栈

| 端 | 技术 | 版本 |
|------|------|------|
| **后端** | Java + Spring Boot + Spring Cloud Alibaba | 17+ / 3.2.4 / 2023.0.1 |
| **持久层** | MyBatis-Plus + HikariCP | 3.5.7 / 5.1.x |
| **流程引擎** | Flowable | 6.8.1 |
| **API 网关** | Spring Cloud Gateway + Resilience4j (M6 引入) | 4.x / 2.2.0 |
| **链路追踪** | Zipkin (M6 引入) | 2.24+ |
| **指标采集** | Micrometer + Prometheus (M6 引入) | 1.13.x / 2.50+ |
| **PC 后台** | Vue 3 + TypeScript + Element Plus + Vite + Pinia | Vue 3.4 |
| **数据库** | MySQL + Redis + Elasticsearch | 8.0.46 / 7.2 / 8.12.0 |
| **消息** | Kafka (KRaft) | 7.5.0 |
| **日志** | ELK Stack (GELF UDP 12201) | 8.12.0 |
| **部署** | Docker Compose + Nginx | - |

---

## 项目结构

```
platform/
├── doc/                           # 文档（4 活跃 + 2 废弃）
│   ├── 中台建设中长期规划.md      # ⭐ 主规划（v1.0, 671 行）
│   ├── 云枢中台技术方案.md        # 技术实施细节（v3.1）
│   ├── opencode实施计划.md        # 实施历史 + 快照（v7.0）
│   ├── 项目进度.md                # 变更日志（v7.0）
│   ├── 部署指南.md                # 部署操作
│   ├── 服务器配置清单.md          # 硬件配置
│   ├── 性能基准.md                # L2 优化前后对比模板
│   ├── CI_CD操作手册.md           # CI/CD 流程
│   ├── Phase1-实施计划.md         # ⚠️ v1 历史档案（已废弃）
│   ├── 云枢中台蓝图框架.md        # ⚠️ v2.0 历史档案（已废弃）
│   └── HANDOFF_2026-06-04.md      # 明日交接文档
│
├── code/
│   ├── platform-server/           # 后端 (Spring Cloud)
│   │   ├── platform-common/       # 公共模块（BaseEntity/Result/JwtUtil）
│   │   ├── platform-gateway/      # API 网关 (8083) — 路由/CORS
│   │   ├── platform-auth/         # 认证服务 (8082) — JWT
│   │   ├── platform-user/         # 用户服务 (8081) — 用户/角色/菜单/组织
│   │   ├── platform-workflow/     # 流程服务 (8084) — Flowable BPMN
│   │   ├── platform-message/      # 消息服务 (8085) — 渠道/模板/SSE
│   │   └── platform-ops/          # 运营服务 (8087) — 租户/存储/审计
│   ├── platform-admin/            # 管理后台 (Vue 3, 8080)
│   ├── platform-ops-admin/        # 运营后台 (Vue 3, 8090)
│   └── platform-app/              # 移动端 (UniApp, 待开发)
│
├── docker/                       # Docker 配置
│   ├── elasticsearch/            # ILM 1 天保留策略
│   ├── logstash/                 # GELF 输入 + Grok 解析
│   └── mysql/                    # MySQL 初始化
│
└── docker-compose.yml             # 容器编排（含 ELK 1 天 ILM）
```

---

## 快速开始

### Docker 一键启动（推荐）

```bash
docker compose up -d
```

> 镜像通过 GitHub Container Registry 分发：自动构建推送，本地 `docker compose pull` 拉取。
> ⚠️ **不要在 Docker Desktop 上构建镜像**（详见 `doc/CI_CD操作手册.md`）

### 分步启动（仅开发调试）

```bash
# 后端 (本地编译)
cd code/platform-server
mvn clean package -DskipTests
java -jar platform-user/target/platform-user-1.0.0-SNAPSHOT.jar

# 前端
cd code/platform-admin
npm install
npm run dev
```

### 访问地址

| 服务 | 地址 | 账号 |
|------|------|------|
| 管理后台 | http://localhost:8080 | admin / 123456 |
| 运营后台 | http://localhost:8090 | admin / 123456 |
| 后端 API | http://localhost:8083 | - |
| Swagger 文档 | http://localhost:8083/swagger-ui.html | - |
| Nacos | http://localhost:8848 | nacos / nacos |
| Kibana | http://localhost:5601 | - |
| MinIO 控制台 | http://localhost:9001 | minioadmin / minioadmin123 |

---

## 功能模块

### 已实现 (v4.2)

| 模块 | 后端 | 前端 | 状态 |
|------|------|------|------|
| 用户/角色/菜单/组织 RBAC | platform-user | platform-admin | ✅ |
| 数据权限 (data_scope) | platform-user | - | 🟡 **M5 基础版** (3+5 TC, 跨服务集成入 M5+) |
| 字典/参数配置 | platform-user | platform-admin | ✅ |
| 操作日志/登录日志 | platform-user | platform-admin | ✅ |
| 多租户 (拦截器) | platform-common | - | ✅ **M4 已完成** (8 TC + 灰度开关 + null 模式) |
| 流程定义/设计器/部署 | platform-workflow | platform-admin | ✅ |
| 请假申请/审批/驳回/转办 | platform-workflow | platform-admin | ✅ |
| 候选人配置 (人员/岗位) | platform-workflow | platform-admin | ✅ |
| 任务签收/退回 (claim) | platform-workflow | platform-admin | ✅ |
| 流程监控/实例追踪 | platform-workflow | platform-admin | ✅ |
| 消息渠道/模板/发送 | platform-message | platform-admin | ✅ |
| 站内信 (收/发/已读/删除) | platform-message | platform-admin | ✅ |
| 消息记录/重发 | platform-message | platform-admin | ✅ |
| SSE 实时推送 | platform-message | platform-admin | ✅ |
| 租户 CRUD/启停 | platform-ops | platform-ops-admin | ✅ |
| 网关动态路由 | platform-ops | platform-ops-admin | ✅ |
| 日志审计 | platform-ops | platform-ops-admin | ✅ |
| 系统监控 (Health 聚合) | platform-ops | platform-ops-admin | ✅ |
| JWT 认证 + Token 管理 | platform-auth | - | ✅ |
| ELK 日志聚合 (GELF + 1 天 ILM) | platform-common | - | ✅ |

### M6 启动时一并实施 (2026-09)

| 能力 | 目标 | 主规划项 |
|------|------|---------|
| 链路追踪 (Zipkin) | 跨服务调用可视化 | P1-1 |
| API 网关韧性 (熔断/限流/重试/超时) | 唯一入口高可用 | P3-6 |
| 业务指标 (Micrometer + Prometheus) | SLO 可衡量 | P1-5 |
| L2 性能调优 (JVM/HikariCP/Tomcat) | 内存优化 -220MB | L2 v2.1 |

### 待开发 (2026 Q4 - 2027 Q1)

- **SSO / OAuth2** (P0-3, M7) — 业务系统接入中台必备
- **前端组件库 / 工具库** (P2-6, M6 末) — 解决 2 admin 重复封装
- **API 文档聚合门户** (P2-1, M8) — 统一 Swagger 门户
- **API 配额 / SDK** (P2-3/4, M9) — 对外 API 经济
- **数据中台 (ETL + OLAP)** (P4-2, 2027) — DataX/Flink/Doris
- **物联中台 (MQTT + 时序)** (P4-?, 2027) — EMQX/TDengine
- **低代码表单设计器** (P4-1, M9) — 业务方自助

---

## 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行时 |
| Node.js | 18+ | 前端开发 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.0+ | 缓存 |
| Docker | 24.0+ | 容器化部署 |
| Maven | 3.9+ | 后端构建 |
| Git | 2.40+ | 版本控制 |

### 服务器配置建议

| 场景 | CPU | 内存 | 磁盘 |
|------|------|------|------|
| 开发/演示 | 4 核 | 8GB | 100GB |
| 小规模生产 | 8 核 | 16GB | 200GB |
| 中规模生产 | 16 核 | 32GB | 500GB |

详细配置见 [`doc/服务器配置清单.md`](doc/服务器配置清单.md)

---

## 文档体系

| 文档 | 角色 | 链接 |
|------|------|------|
| ⭐ **中台建设中长期规划** | 主规划（5 层蓝图 + 路线图 + P0 详细）| [doc/中台建设中长期规划.md](doc/中台建设中长期规划.md) |
| 云枢中台技术方案 | 技术实施细节（v3.1）| [doc/云枢中台技术方案.md](doc/云枢中台技术方案.md) |
| opencode 实施计划 | 实施历史 + 快照（v7.0）| [doc/opencode实施计划.md](doc/opencode实施计划.md) |
| 项目进度 | 变更日志（v7.0）| [doc/项目进度.md](doc/项目进度.md) |
| 部署指南 | 部署操作 | [doc/部署指南.md](doc/部署指南.md) |
| 服务器配置清单 | 硬件配置 | [doc/服务器配置清单.md](doc/服务器配置清单.md) |
| 性能基准 | L2 优化前后对比模板 | [doc/性能基准.md](doc/性能基准.md) |
| CI_CD 操作手册 | 镜像构建流程 | [doc/CI_CD操作手册.md](doc/CI_CD操作手册.md) |
| 工作交接 | 2026-06-04 进度 | [doc/HANDOFF_2026-06-04.md](doc/HANDOFF_2026-06-04.md) |

> ⚠️ 历史档案（已废弃但保留）：`Phase1-实施计划.md` (v1), `云枢中台蓝图框架.md` (v2.0)

---

## 用户强约束

> 来自 `doc/CI_CD操作手册.md` §警告：禁止本地构建镜像

```powershell
# ❌ 禁止
docker compose build
docker build -t xxx .
mvn clean package && docker build  # 本地打 jar + 构建镜像

# ✅ 正确
git add . && git commit -m "..." && git push
# 等 CI 完成 (5-10 min), 镜像自动 push ghcr.io
docker compose pull && docker compose up -d
```

**理由**:
- Docker Desktop WSL2 后端构建 Spring Boot 镜像 10+ 分钟必超时
- 资源抢占影响其他开发
- ghcr.io 镜像与 master/develop 分支绑定

---

## 开发流程

```
代码改动 → git push → GitHub Actions CI
                ↓
        并行构建 (backend/frontend)
                ↓
        镜像自动 push ghcr.io
                ↓
        本地 docker compose pull
                ↓
        docker compose up -d
                ↓
        服务验证
```

详见 [`doc/CI_CD操作手册.md`](doc/CI_CD操作手册.md)

---

## 路线图快照

```
Q3 2026 (M4-M6):
  P0-1 多租户拦截器 → P0-2 data_scope → P1-1 链路追踪 + L2 调优 + 网关韧性 + 业务指标

Q4 2026 (M7-M8):
  P0-3 SSO / OAuth2 → P0-4 业务能力地图 → P1-2/3/4 可观测全套 → P2-1/2/3 开放基础
  P3-7 分布式事务 Outbox

Q1 2027 (M9):
  P2-4 SDK + P2-5 Webhook + P2-7 微前端
  P3-1 备份演练 + P3-2 灰度 + P3-3 降级
  P4-1 低代码表单 + P4-3 主数据管理 (MDM)
```

详细路线见 [`doc/中台建设中长期规划.md` 第六章](doc/中台建设中长期规划.md#六实施路线图)

---

## License

MIT
