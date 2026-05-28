# 云枢中台

> 通用中台解决方案

![Version](https://img.shields.io/badge/version-4.1-blue.svg)

---

## 项目简介

云枢中台是以数据为核心、数字运营为纽带的新一代通用中台解决方案，实现技术统一、数据统一、业务统一。

### 核心能力

- 用户中心 + RBAC 权限 + 多租户
- 流程引擎 (Flowable 6.8.1) + BPMN 设计器
- 消息中心 (多渠道/模板/站内信/短信)
- 运营管理 (租户/存储/网关/审计)
- SSE 实时推送 + Kafka 异步消息
- 系统监控 + 日志审计

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
│                    基础设施层                            │
│   MySQL 8.0 | Redis 7.0 | Kafka | Nacos | MinIO | ES   │
└────────────────────────────────────────────────────────┘
```

### 技术栈

| 端 | 技术 |
|------|------|
| **后端** | Java 17 + Spring Boot 3.2 + Spring Cloud + MyBatis-Plus + Flowable |
| **PC 后台** | Vue 3 + TypeScript + Element Plus + Vite + Pinia |
| **数据库** | MySQL 8.0 + Redis 7.0 + Elasticsearch |
| **消息** | Kafka + SSE |
| **中间件** | Nacos + MinIO + Knife4j |
| **部署** | Docker Compose + Nginx |

---

## 项目结构

```
platform/
├── doc/                           # 文档
├── code/
│   ├── platform-server/           # 后端 (Spring Cloud)
│   │   ├── platform-common/       # 公共模块 (BaseEntity/Result/JwtUtil/MP配置)
│   │   ├── platform-gateway/      # API 网关 (8083)
│   │   ├── platform-auth/         # 认证服务 (8082) — JWT/Token/SMS
│   │   ├── platform-user/         # 用户服务 (8081) — 用户/角色/菜单/组织
│   │   ├── platform-workflow/     # 流程服务 (8084) — Flowable BPMN
│   │   ├── platform-message/      # 消息服务 (8085) — 渠道/模板/站内信/SSE
│   │   └── platform-ops/          # 运营服务 (8087) — 租户/存储/网关/审计
│   ├── platform-admin/            # 管理后台 (Vue 3, 8080)
│   ├── platform-ops-admin/        # 运营后台 (Vue 3, 8090)
│   └── platform-app/              # 移动端 (UniApp, 待开发)
├── prototype/                     # 原型
└── docker-compose.yml             # 容器编排
```

---

## 快速开始

### Docker 一键启动

```bash
docker-compose up -d
```

### 分步启动

```bash
# 后端 (每个服务单独启动)
cd code/platform-server
mvn clean package -DskipTests
java -jar platform-user/target/platform-user-1.0.0-SNAPSHOT.jar
# 同理启动 auth, workflow, message, ops, gateway

# 前端
cd code/platform-admin
npm install
npm run dev
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 管理后台 | http://localhost:8080 |
| 运营后台 | http://localhost:8090 |
| 后端 API | http://localhost:8083 |
| Swagger 文档 | http://localhost:8083/swagger-ui.html |
| Nacos | http://localhost:8848 |
| Kafka UI | http://localhost:8089 |
| MinIO | http://localhost:9001 |

---

## 功能模块

### 已实现 (v4.1)

| 模块 | 后端 | 前端 | 状态 |
|------|------|------|------|
| 用户/角色/菜单/组织 RBAC | platform-user | platform-admin | ✅ |
| 字典/参数配置 | platform-user | platform-admin | ✅ |
| 操作日志/登录日志 | platform-user | platform-admin | ✅ |
| 流程定义/设计器/部署 | platform-workflow | platform-admin | ✅ |
| 请假申请/审批/驳回/转办 | platform-workflow | platform-admin | ✅ |
| 候选人配置 (人员/岗位Tab) | platform-workflow | platform-admin | ✅ |
| 任务签收/退回 (claim) | platform-workflow | platform-admin | ✅ |
| 流程监控/实例追踪 | platform-workflow | platform-admin | ✅ |
| 消息渠道/模板/发送 | platform-message | platform-admin | ✅ |
| 站内信 (收/发/已读/删除) | platform-message | platform-admin | ✅ |
| 消息记录/重发 | platform-message | platform-admin | ✅ |
| SSE 实时推送通知 | platform-message | platform-admin | ✅ |
| 租户 CRUD/启停 | platform-ops | platform-ops-admin | ✅ |
| 网关动态路由 | platform-ops | platform-ops-admin | ✅ |
| 日志审计 | platform-ops | platform-ops-admin | ✅ |
| 系统监控 (Health聚合) | platform-ops | platform-ops-admin | ✅ |
| JWT 认证 + Token管理 | platform-auth | — | ✅ |
| 多租户 SQL 隔离 | platform-common | — | ✅ |

### 待开发

- 应用中心 / 内容中心 / 任务中心
- 数据中台 / 物联中台
- 门户中心
- ECharts Dashboard
- XXL-JOB 调度中心

---

## 环境要求

| 软件 | 版本 |
|------|------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| Docker | 24.0+ |
| Maven | 3.9+ |

---

## 文档

- [项目进度](doc/项目进度.md)
- [实施计划](doc/opencode实施计划.md)
- [技术方案](doc/云枢中台技术方案.md)
- [部署指南](doc/部署指南.md)
