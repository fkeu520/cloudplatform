# 云枢中台

> 通用中台解决方案

![Version](https://img.shields.io/badge/version-7.5-blue.svg)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![Flowable](https://img.shields.io/badge/Flowable-6.8.1-blue)
![Docker](https://img.shields.io/badge/Docker-29.5.3-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## 项目简介

云枢中台是以数据为核心、数字运营为纽带的新一代通用中台解决方案，实现技术统一、数据统一、业务统一。

### 核心能力

- **用户中心** + RBAC 权限 + **多租户** (M4 P0-1 已完成)
- **数据权限** (M5 P0-2 data_scope, 行级隔离)
- **流程引擎** (Flowable 6.8.1) + BPMN 设计器 + 请假审批
- **消息中心** (多渠道/模板/站内信/短信/SSE/Kafka)
- **运营管理** (租户/存储/网关/审计)
- **API 网关** (Spring Cloud Gateway + Resilience4j 熔断/限流/超时)
- **链路追踪** (Zipkin) + **业务指标** (Micrometer + Prometheus + Grafana)
- **容器监控** (实时 CPU/内存排行, Prometheus 采集)
- **ELK 日志聚合** (Logstash + Elasticsearch + Kibana, 1 天 ILM)
- **系统监控** + 日志审计

---

## 路线图状态

| 阶段 | 时间 | 状态 |
|------|------|------|
| **M4 P0-1** MyBatis-Plus 多租户拦截器 | 2026-07 | ✅ 已完成 (8 TC + 灰度开关 + null 模式) |
| **M5 P0-2** 数据权限 data_scope | 2026-08 | 🟡 基础版可用 (PR1-4 完成, 复杂场景入 M5+) |
| **M6 P1-1 链路追踪 + L2 调优 + 网关韧性 + 业务指标** | 2026-09 | 🟡 **代码就位, 待 Ubuntu 验证** |
| **M7-M9** SSO / 可观测 / 开放 / 韧性 / 业务中台 | 2026 Q4 - 2027 Q1 | 🔵 待启动 |

> 完整路线见 [`doc/plan/中台建设中长期规划.md`](doc/plan/中台建设中长期规划.md)
> 进度记录见 [`doc/log/项目进度.md`](doc/log/项目进度.md)
> 已知问题见 [`doc/log/KNOWN_ISSUES.md`](doc/log/KNOWN_ISSUES.md)

---

## 关键决策

| 原则 | 含义 |
|------|------|
| **可用性 > 可扩展性 > 性能** | 任何优化不得引入消息丢失、首请求延迟 |
| **不锁死技术栈** | 依赖通过配置开关控制，不删 pom 依赖 |
| **Windows 开发 / Ubuntu 部署** | 代码在 Windows 编辑提交, GitHub Actions CI 构建, Ubuntu 217 部署 |

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
│   MySQL 8.0 | Redis 7 | Kafka (KRaft) | Nacos | MinIO  │
│   ES 8.12 + Logstash + Kibana                          │
├────────────────────────────────────────────────────────┤
│                    可观测层                              │
│   Zipkin (链路) | Prometheus (指标) | Grafana (看板)    │
│   node-exporter (宿主机) | container-exporter (容器)    │
└────────────────────────────────────────────────────────┘
```

### 技术栈

| 端 | 技术 | 版本 |
|------|------|------|
| **后端** | Java + Spring Boot + Spring Cloud Alibaba | 17+ / 3.2.4 / 2023.0.1 |
| **持久层** | MyBatis-Plus + HikariCP | 3.5.7 / 5.1.x |
| **流程引擎** | Flowable | 6.8.1 |
| **API 网关** | Spring Cloud Gateway + Resilience4j | 4.x / 2.2.0 |
| **链路追踪** | Zipkin | 2.24+ |
| **指标采集** | Micrometer + Prometheus + Grafana | 1.13.x / 2.50+ |
| **PC 后台** | Vue 3 + TypeScript + Element Plus + Vite + Pinia | Vue 3.4 |
| **数据库** | MySQL 8.0.46 + Redis 7.2 | - |
| **消息** | Kafka (KRaft) | 7.5.0 |
| **日志** | ELK Stack (GELF UDP 12201) | 8.12.0 |
| **容器监控** | container-exporter (Python + Docker API) | 自建, ~15 MB |
| **部署** | Docker Compose v5.1.4 (单机) / K8s (规划) | Ubuntu 26.04 |

### 部署环境

| 环境 | 主机 | 规格 | 状态 |
|------|------|------|------|
| **开发/演示** | Ubuntu 26.04 @ 192.168.0.217 | 6核 / 14GB / 872GB SSD | ✅ 22 容器全健康, 6.2 GB available |
| **生产规划** | 3 节点 HA (K8s) | 12核48G × 3 | 📋 见 [`服务器配置清单.md`](doc/服务器配置清单.md) |

---

## 项目结构

```
platform/
├── docker-compose.yml         # 容器编排 (22 服务)
├── docker/                    # Docker 配置
│   ├── elasticsearch/         # ILM 1 天保留
│   ├── logstash/              # GELF + Grok 解析 (2026-06-11 修复)
│   ├── mysql/                 # MySQL 初始化
│   ├── prometheus/            # Prometheus scrape config (2 targets)
│   └── grafana/               # Grafana provisioning (datasource + dashboards)
│
├── code/                      # 源码
│   ├── platform-server/       # 后端 (7 模块)
│   ├── platform-admin/        # 管理后台 (Vue 3)
│   └── platform-ops-admin/    # 运营后台 (Vue 3)
│
├── scripts/                   # CI / 工具
│   ├── ci/                    # CI 验证脚本
│   └── container-exporter/    # 容器指标导出器 (Python, 替代 cAdvisor)
│
├── doc/                       # 文档
│   ├── spec/                  # 编码/接口/视觉规范
│   ├── ops/                   # 部署/运维/CI_CD
│   ├── plan/                  # 中长期规划/技术方案
│   ├── decision/              # 决策记录 (多租户/数据权限)
│   ├── log/                   # 项目进度 / KNOWN_ISSUES / 性能基准
│   ├── handoff/               # 会话交接文档
│   └── sql/                   # SQL 脚本
│
├── .github/                   # GitHub Actions CI
├── .githooks/                 # pre-commit secret scan / pre-push test
└── .gitignore                 # 精确排除 (docker mysql data 等)
```

---

## 快速开始

### Docker Compose 启动 (推荐)

```bash
# 开发环境 (217 Ubuntu)
cd /opt/platform
git pull
docker compose pull              # 从 ghcr.io 拉最新镜像
docker compose up -d             # 启动/更新

# 查看状态
docker compose ps
docker stats --no-stream
```

> ⚠️ **禁止本地构建镜像** — 所有镜像通过 GitHub Actions CI 构建并推送到 `ghcr.io`, 本地只 `docker compose pull`。

### 访问地址 (开发环境 217)

| 服务 | 地址 | 账号 |
|------|------|------|
| 管理后台 | http://192.168.0.217:8080 | zhangs / 123456 |
| 运营后台 | http://192.168.0.217:8090 | admin / 123456 |
| Grafana (监控) | http://192.168.0.217:3000 | - |
| Prometheus | http://192.168.0.217:9090 | - |
| Zipkin (链路) | http://192.168.0.217:9411 | - |
| Kibana (日志) | http://192.168.0.217:5601 | - |
| Nacos | http://192.168.0.217:8848 | nacos / nacos |

---

## 功能模块

### 已实现 (v7.5)

| 模块 | 后端 | 前端 | 状态 |
|------|------|------|------|
| 用户/角色/菜单/组织 RBAC | platform-user | platform-admin | ✅ |
| 数据权限 (data_scope) | platform-user | - | 🟡 M5 基础版 (PR1-4 完成) |
| 字典/参数配置 | platform-user | platform-admin | ✅ |
| 操作日志/登录日志 | platform-user | platform-admin | ✅ |
| 多租户 (拦截器) | platform-common | - | ✅ M4 (8 TC + 灰度开关) |
| 流程定义/设计器/部署 | platform-workflow | platform-admin | ✅ |
| 请假申请/审批/驳回/转办 | platform-workflow | platform-admin | ✅ |
| 候选人配置 (人员/岗位) | platform-workflow | platform-admin | ✅ |
| 任务签收/退回 (claim) | platform-workflow | platform-admin | ✅ |
| 流程监控/实例追踪 | platform-workflow | platform-admin | ✅ |
| 消息渠道/模板/发送 | platform-message | platform-admin | ✅ |
| 站内信/消息记录 | platform-message | platform-admin | ✅ |
| SSE 实时推送 | platform-message | platform-admin | ✅ |
| 租户 CRUD/启停 | platform-ops | platform-ops-admin | ✅ |
| 网关动态路由 | platform-ops | platform-ops-admin | ✅ |
| 日志审计 | platform-ops | platform-ops-admin | ✅ |
| JWT 认证 + Token 管理 | platform-auth | - | ✅ |
| API 网关 CORS 修复 | platform-gateway | - | ✅ `allowed-origins: *` |
| 链路追踪 (Zipkin) | - | - | 🟡 代码就位 |
| Resilience4j 熔断/限流 | platform-gateway | - | 🟡 代码就位 |
| Prometheus + Grafana | - | - | 🟡 代码就位 (含 dashboard) |
| 宿主机监控 (node-exporter) | - | - | ✅ Grafana "系统总览" |
| 容器监控 (container-exporter) | - | - | ✅ Grafana "容器资源排行" |

### 待开发 (2026 Q4 - 2027 Q1)

- **SSO / OAuth2** (P0-3, M7)
- **前端组件库** (P2-6)
- **API 文档聚合门户** (P2-1)
- **数据中台 (ETL + OLAP)** (P4-2, 2027)
- **物联中台 (MQTT + 时序)** (P4-?, 2027)
- **低代码表单设计器** (P4-1, M9)

---

## 监控体系

| 层次 | 工具 | 数据源 | 看板 |
|------|------|--------|------|
| **宿主机** | node-exporter (9 MB) | 主机 CPU/内存/磁盘/网络 | Grafana "系统总览" |
| **容器** | container-exporter (15 MB, Python) | Docker API stats | Grafana "平台监控总览" |
| **JVM (规划)** | Micrometer + /actuator/prometheus | 各微服务 | (需启用 prometheus 端点) |
| **告警 (规划)** | Alertmanager | Prometheus | (需部署) |

Grafana 访问: http://192.168.0.217:3000 (admin / 13040936a)

---

## 开发流程

```
Windows 编辑代码 → git push (双平台: Gitee + GitHub)
                        ↓
                GitHub Actions CI
                 并行构建 (backend/frontend)
                 镜像推送到 ghcr.io
                        ↓
               SSH 到 Ubuntu 217
               git pull + docker compose pull
               docker compose up -d
```

**Git 工作流**: 单一 `develop` 分支 (已清理所有 feat/* 分支)

---

## 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端开发 |
| Node.js | 18+ | 前端开发 |
| Docker | 24.0+ | 容器引擎 (生产 29.5.3) |
| Docker Compose | v2+ | 容器编排 (v5.1.4) |

## 服务器配置

| 场景 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| **当前开发** (217) | 6 核 | 14 GB | 872 GB SSD | 22 容器, 6.2 GB available |
| **小规模生产** | 12 核 × 3 | 48 GB × 3 | 1 TB SSD × 3 | 3 节点 HA, 500 并发 |
| **中等规模** | 16 核 × 3 | 64 GB × 3 | 2 TB × 3 | 1000+ 并发 |

详细配置及硬件选型见 [`doc/服务器配置清单.md`](doc/服务器配置清单.md)

---

## 文档体系

| 文档 | 角色 | 路径 |
|------|------|------|
| ⭐ **中台建设中长期规划** | 主规划 | `doc/plan/中台建设中长期规划.md` |
| **服务器配置清单** | 硬件 + 3 节点 HA + 灾备 | `doc/服务器配置清单.md` |
| **项目进度** | 变更日志 (v7.5) | `doc/log/项目进度.md` |
| **KNOWN_ISSUES** | 已知问题及修复 (#1-#21) | `doc/log/KNOWN_ISSUES.md` |
| **部署指南** | Docker Compose 操作 | `doc/ops/部署指南.md` |
| **CI_CD 操作手册** | 镜像构建流程 | `doc/ops/CI_CD操作手册.md` |
| **编码规范** | Java / 前端 / 数据库 | `doc/spec/编码规范.md` |
| **API 接口规范** | 接口设计 | `doc/spec/API接口规范.md` |
| **运维问题排查记录** | 12 个部署问题复盘 | `doc/ops/运维问题排查记录.md` |

---

## License

MIT
