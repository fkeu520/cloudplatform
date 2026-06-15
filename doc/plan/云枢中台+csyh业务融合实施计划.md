# 云枢中台 + csyh 业务融合实施计划

> **目标**: 将 csyh 园区运营管理业务能力融入云枢中台，构建"横向中台 + 垂直业务"双层架构
> **分析基础**: csyh 代码可用性报告 + 云枢技术方案 v3.1 + 中台建设中长期规划
> **制定日期**: 2026-06-15
> **版本**: v2.0 (2026-06-15: 新增 Phase -1 公共层前置 / 命名空间统一 / 同步云枢已就位能力)

---

## 一、核心判断

### 1.1 csyh 与云枢的关系定位

| 维度 | csyh | 云枢 platform | 关系 |
|------|------|---------------|------|
| **定位** | 园区运营管理业务系统 | 通用技术中台 | 垂直业务 + 横向中台 |
| **业务领域** | 招商、租赁、合同、物业、财务 | 用户、流程、消息、运营 | 互补，零重合 |
| **技术栈** | Java 8 + 自研 flyrise 框架 | Java 17 + Spring Boot 3 | 异构，必须翻译 |
| **数据规模** | 200-400 张表 | ~20 张表 | 10 倍差距 |
| **可用性** | 业务逻辑 99% 自包含 | 完整可运行 | 各自独立 |

### 1.2 融合策略

**不是"代码合并"，而是"能力提取 + 中台扩展 + 命名空间统一"**：

```
┌─────────────────────────────────────────────┐
│              应用层 (SaaS)                   │
│   园区运营管理 (翻译自 csyh, com.cloudhub.*) │
│   各行业业务应用（按需构建）                  │
├─────────────────────────────────────────────┤
│              业务中台 ← 云枢现有              │
│   ✅ 用户中心 │ ✅ 流程中心 │ ✅ 消息中心     │
│   ✅ 运营中心 │ ⏳ 门户中心 │ ⏳ 应用中心     │
├─────────────────────────────────────────────┤
│              新增：园区业务中台 (6 服务)      │
│   ⏳ park-space │ ⏳ park-contract │ ⏳ park-property │
│   ⏳ park-business │ ⏳ park-finance │ ⏳ park-service │
├─────────────────────────────────────────────┤
│              技术中台 ← 云枢现有              │
│   ✅ Spring Cloud │ ✅ Gateway │ ✅ Nacos     │
│   ✅ ELK │ ✅ Prometheus │ ✅ Zipkin          │
├─────────────────────────────────────────────┤
│              基础设施层 ← 云枢现有             │
│   ✅ Docker Compose │ ✅ MySQL/Redis/Kafka   │
└─────────────────────────────────────────────┘
```

---

## 二、Phase -1: 公共层适配与命名空间统一 (5 周) ⭐ 前置

> **为什么必须最先做**: csyh 业务模块 99% 引用 `cn.flyrise.*` 集团公共库（7,557 个 import），源码在集团私有 svn 无法获取。如果不先做公共层适配，**任何业务模块都无法编译运行**。

### 2.1 cn.flyrise.* 公共库依赖清单

基于 INTEGRATION_ANALYSIS.md 统计（19,510 个 import 中外部依赖）：

| 包路径 | import 数 | 性质 | 适配复杂度 |
|--------|-----------|------|------------|
| `cn.flyrise.common.*` | 5,486 | 通用基础（异常/分页/响应/工具类） | ⭐⭐ |
| `cn.flyrise.mybatis.*` | 1,114 | MyBatis 增强 (IBaseService, BaseEntity) | ⭐ |
| `cn.flyrise.security.*` | - | Shiro + 自研安全框架 | ⭐⭐⭐ |
| `cn.flyrise.business.*` | 713 | 业务通用模块 | ⭐⭐⭐ |
| `cn.flyrise.system.*` | 244 | 系统工具（用户/权限/字典） | ⭐⭐ |
| `cn.flyrise.fe.common.*` | - | 前端 VO/DTO | ⭐ (重设计) |
| `cn.flyrise.mq.*` / `job.*` / `quartz.*` / `oss.*` / `redis.*` | - | 通用中间件 | ⭐-⭐⭐ |
| **合计外部依赖** | **7,557** | | |

### 2.2 公共层适配任务分解

| 周 | 任务 | 输出 |
|----|------|------|
| W1 | 扫描所有 cn.flyrise.* import，分类整理 | 公共类清单 + 适配映射表 |
| W1 | 命名空间统一：`cn.flyrise.*` → `com.cloudhub.platform.park.common.*` | 批量替换脚本 + IDE 重构基线 |
| W2 | **cn.flyrise.common.* 适配** (Reply, BizException, Page, Result) | `park-common-base` 模块 + 单测 |
| W2 | **cn.flyrise.mybatis.* 适配** (IBaseService, BaseEntity) | MyBatis-Plus 包装层 + 单测 |
| W3 | **cn.flyrise.security.* 适配** (Shiro → Spring Security + JWT) | 鉴权适配层 + 双体系用户映射表 |
| W3 | **cn.flyrise.system.* 适配** (用户/权限/字典抽象) | 抽象接口 + 云枢 platform-user 对接 |
| W4 | **cn.flyrise.business.* 适配** (业务通用逻辑) | 业务通用层模块 |
| W4 | cn.flyrise.mq / job / quartz / oss / redis 适配 | 各自独立模块 + 单测 |
| W5 | 集成验证：抽 1 个 csyh 简单模块编译跑通 | 验证公共层 + 命名空间替换完整 |
| W5 | 命名空间全量检查 + 文档更新 | 验收 |

### 2.3 命名空间统一规则

| 原 csyh | 新云枢 |
|---------|--------|
| `cn.flyrise.common.*` | `com.cloudhub.platform.park.common.*` |
| `cn.flyrise.mybatis.*` | `com.cloudhub.platform.park.common.mybatis.*` |
| `cn.flyrise.security.*` | `com.cloudhub.platform.park.common.security.*` |
| `cn.flyrise.system.*` | `com.cloudhub.platform.park.common.system.*` |
| `cn.flyrise.business.*` | `com.cloudhub.platform.park.common.business.*` |
| `cn.flyrise.mq.*` | `com.cloudhub.platform.park.common.mq.*` |
| `cn.flyrise.pai.{module}.*` | `com.cloudhub.platform.park.{module}.*` |
| `cn.flyrise.fe.common.*` | 重设计 DTO，不强求一致 |

**实施方式**:
- `sed -i 's/cn\.flyrise\./com.cloudhub.platform.park./g'` 全量替换
- IDE 重构（IntelliJ）做包路径调整
- 同步更新 `pom.xml` groupId
- 统一 groupId = `com.cloudhub.platform`

### 2.4 验收标准

- [ ] 所有 cn.flyrise.* import 在 csyh 子模块中**为零**
- [ ] 公共层 11 个适配模块全部编译通过 + 单测覆盖 > 80%
- [ ] 抽 `pai-park-space-csyh` 完整编译运行（虽不含业务逻辑，但证明公共层就绪）
- [ ] groupId 全部统一为 `com.cloudhub.platform`

---

## 三、业务能力提取路线图

### 3.1 提取顺序（基于依赖关系）

```
空间中心 (Phase 0) ──┬──> 合同中心 (Phase 1) ──> 财务中心 (Phase 4)
                     │     │
                     │     └──> 招商中心 (Phase 3) ──┘
                     │
                     └──> 物业中心 (Phase 2) ──> 服务中心 (Phase 4)
```

**空间中心**是**绝对前置**（所有其他业务都依赖房源/楼宇/楼层数据）。

### 3.2 csyh 业务能力清单

| 能力中心 | csyh 来源 | 业务价值 | 优先级 | 依赖 |
|----------|----------|----------|--------|------|
| **空间中心** | pai-park-space-csyh | 房源/楼宇/楼层/工位 — 所有业务基础 | **P0** | Phase -1 |
| **合同中心** | pai-contract-csyh | 租赁/物业/销售合同全生命周期 | P0 | 空间中心 |
| **物业中心** | pai-park-property-csyh | 巡检/报修/能耗/装修/停车 | P0 | 空间中心 |
| **招商中心** | pai-business-csyh | 客户/线索/跟进/招商计划 | P1 | 空间中心 |
| **财务中心** | pai-finance-csyh | 收款/账单/发票/统计 | P2 | 合同中心 |
| **服务中心** | pai-service-center-csyh | 报修/投诉/满意度/工单 | P2 | 物业中心 |

---

## 四、实施阶段

### Phase 0: 空间中心 (3 周) — 基础数据

| 周 | 任务 | 输出 |
|----|------|------|
| W1 | 建立 `platform-park/park-space` 模块 + 数据库设计 | 模块骨架 + ER 图 + DDL |
| W2 | 核心 CRUD：园区 / 楼宇 / 楼层 / 工位 | 4 张表 + Service + Controller |
| W3 | 前端：房源管理 + 平面图展示 + 楼宇-楼层树 API | Vue 3 页面 + Element Plus |

**云枢能力复用**:
- 多租户（已完成）：自动加 `tenant_id`
- 流程中心：空间变更审批（如调租流程）

### Phase 1: 合同中心 (3 周)

| 周 | 任务 | 输出 |
|----|------|------|
| W1 | 数据库设计（外键关联房源） | 租赁/物业/销售合同表 |
| W2 | Controller + Service 核心逻辑 | 合同 CRUD + 房源关联查询 |
| W3 | 业务流程对接 + 前端 | Flowable 审批流 + Vue 3 页面 |

**云枢能力复用**: 流程中心（合同审批）、消息中心（到期提醒）、多租户（按租户隔离）。

### Phase 2: 物业中心 (3 周)

| 周 | 任务 | 输出 |
|----|------|------|
| W1 | 数据库设计（关联楼宇/楼层） | 巡检/报修/能耗/装修表 |
| W2 | 巡检/报修核心逻辑 | 工单流转 + 消息通知 |
| W3 | 能耗统计 + 装修审批 + 前端 | 统计报表 + 审批流 + 页面 |

**云枢能力复用**: 流程中心（装修审批）、消息中心（工单通知）、数据权限（物业管辖范围）。

### Phase 3: 招商中心 (2 周)

| 周 | 任务 | 输出 |
|----|------|------|
| W1 | 数据库设计 + 客户/线索/跟进 | 数据库 + Service |
| W2 | 房源状态实时查询 + 前端看板 | 看板展示 + 客户跟进 |

**云枢能力复用**: 空间中心（房源状态 API）、数据权限（按招商员隔离）。

### Phase 4: 财务中心 + 服务中心 (3 周)

| 周 | 任务 | 输出 |
|----|------|------|
| W1-2 | 财务中心 | 收款/账单/发票（按合同/房源计费） |
| W3 | 服务中心 | 报修/投诉/满意度（关联物业工单） |

**云枢能力复用**: 合同中心（财务计费依据）、物业中心（服务工单流转）。

### Phase 5: 整合优化 (2 周)

| 周 | 任务 | 输出 |
|----|------|------|
| W1 | 全量数据权限适配 + 性能优化 | 索引 + 缓存 + 批量查询 |
| W2 | 集成测试 + 端到端压测 | 测试报告 + 性能基线 |

**云枢能力复用**: 数据权限（已完成）、链路追踪、业务指标。

---

## 五、云枢中台能力复用清单

> 云枢 M4/M5/M6 已就位，park-* 模块无需重复实现，直接复用。

| 能力 | 模块 | 状态 | 复用方式 |
|------|------|------|----------|
| 多租户拦截器 | platform-common | ✅ M4 已完成 | 所有 park-* 表自动加 tenant_id |
| 数据权限 data_scope | platform-user | 🟡 M5 基础版 | 园区业务表加 @DataScope 注解 |
| 链路追踪 | 各微服务 | 🟡 M6 P1-1 代码就位 | park-* 启用 Zipkin 客户端 |
| 业务指标 | 各微服务 | 🟡 M6 P1-1 代码就位 | park-* 业务方法加 Micrometer |
| API 网关韧性 | platform-gateway | 🟡 M6 P1-1 代码就位 | park-* 注册到 Resilience4j 熔断 |
| 流程中心 | platform-workflow | ✅ 已完成 | park-* 业务用 Flowable 审批 |
| 消息中心 | platform-message | ✅ 已完成 | park-* 通知走站内信/邮件/短信 |
| 用户中心 | platform-user | ✅ 已完成 | park-* 用户/角色复用 |

---

## 六、关键决策

### 6.1 微服务拆分 (6 个 park-* 不合并)

| 服务 | 端口 | 业务 | 依赖 |
|------|------|------|------|
| `park-space` | 8091 | 空间中心 | 无 |
| `park-contract` | 8092 | 合同中心 | park-space |
| `park-property` | 8093 | 物业中心 | park-space |
| `park-business` | 8094 | 招商中心 | park-space |
| `park-finance` | 8095 | 财务中心 | park-contract |
| `park-service` | 8096 | 服务中心 | park-property |

**理由**: 业务边界清晰、独立扩缩容、与云枢现有 6 服务粒度一致。

### 6.2 数据库策略

- **决策**: 重新设计数据库，参考 csyh 模型但按云枢规范（`t_park_模块_业务` 命名 + `tenant_id` 字段）
- **数据迁移**: 旧 csyh 数据 ETL 后续单独规划（不影响新业务上线）

### 6.3 前端策略

- **决策**: 重新开发前端（Vue 3 + TypeScript + Element Plus）
- **复用**: 复用 csyh 页面结构、表单校验、API 调用逻辑（参考 Vue 文件）
- **统一**: 接入云枢 platform-admin 微前端架构

### 6.4 命名空间统一

- **原 csyh**: `cn.flyrise.*` (集团私有包，无法访问)
- **新云枢**: `com.cloudhub.platform.park.*` (项目自有，git 可控)
- **实施时机**: Phase -1 完成（公共层适配同时完成）
- **长期收益**: 避免双命名空间长期维护成本，所有代码在自有仓库可控

---

## 七、风险与缓解

| 风险 | 严重度 | 缓解方案 |
|------|--------|----------|
| **公共库源码缺失** | 🔴 高 | Phase -1 提前做，先实现 5,486 个 cn.flyrise.common.* 适配 |
| **业务理解偏差** | 🟡 中 | 逐模块与业务方确认需求，AI 重构后人工 review |
| **数据库模型冲突** | 🟡 中 | 先建测试库验证，逐步迁移 |
| **多租户适配遗漏** | 🔴 高 | 强制拦截器 + 自动化测试 |
| **性能问题（大数据量）** | 🟡 中 | 分库分表 + 索引优化 + 缓存 |
| **数据权限配置复杂** | 🟡 中 | 提供可视化配置界面 |
| **前端 UI 库替换成本** | 🟡 中 | 复用 Element Plus，减少自研组件 |
| **命名空间替换遗漏** | 🟡 中 | Phase -1 用 grep 全量扫描 + IDE 重构双保险 |

---

## 八、里程碑与验收

| 里程碑 | 周期 | 关键交付 | 验收标准 |
|--------|------|----------|----------|
| **M-1: 公共层就绪** | +5 周 | 11 个公共适配模块 + 命名空间统一 | csyh 简单模块编译跑通 + import 0 个 cn.flyrise |
| **M0: 空间中心上线** | +8 周 | park-space 模块 + 房源/楼宇/楼层/工位 CRUD + 平面图 | 6 个表 + API + 前端 + 集成测试通过 |
| **M1: 合同中心上线** | +11 周 | park-contract + 合同审批流 + 房源关联 | 关联空间中心数据 + Flowable 审批跑通 |
| **M2: 物业中心上线** | +14 周 | park-property + 巡检/报修/能耗/装修 | 关联楼宇 + 报修工单流转 |
| **M3: 招商中心上线** | +16 周 | park-business + 客户/线索/房源状态看板 | 房源状态实时 |
| **M4: 财务+服务上线** | +19 周 | park-finance + park-service | 计费/工单完整 |
| **M5: 整合完成** | +21 周 | 数据权限 + 性能 + 集成测试 | 端到端压测通过 |

**总周期**: 21 周 ≈ 5 个月（含 Phase -1 5 周公共层 + Phase 5 2 周整合优化，缓冲充足）

---

## 九、本周可启动事项

### Week 1 立即可做（无需等待）

1. **扫描并整理 cn.flyrise.* import 清单**
   ```bash
   cd D:\work\AI\output\code\csyh
   grep -r "import cn.flyrise" --include="*.java" | sort -u > cn-flyrise-imports.txt
   # 分类整理到 phase_-1-mapping.md
   ```

2. **建立 park-* 6 模块 Maven 骨架**
   ```bash
   cd D:\work\AI\output\platform\code\platform-server
   mkdir -p platform-park/park-common
   mkdir -p platform-park/park-space
   mkdir -p platform-park/park-contract
   mkdir -p platform-park/park-property
   mkdir -p platform-park/park-business
   mkdir -p platform-park/park-finance
   mkdir -p platform-park/park-service
   ```

3. **统一 groupId 配置**
   - 所有 park-* pom.xml: `com.cloudhub.platform`
   - 准备命名空间替换脚本: `sed -i 's/cn\.flyrise\./com.cloudhub.platform.park./g'`

4. **AI 重构 csyh → 云枢**（最简模块验证可行性）
   - 选 `pai-park-space-csyh` 翻译到 `park-space`
   - 业务逻辑参考，命名空间批量替换
   - 跑通后即可批量推进

---

## 十、附录

### 10.1 参考文档

- `D:\work\AI\output\code\csyh\INTEGRATION_ANALYSIS.md`: csyh 整体可用性 + 公共库缺失分析
- `D:\work\AI\output\platform\doc\plan\云枢中台技术方案.md`: 云枢技术方案 v3.1
- `D:\work\AI\output\platform\doc\plan\中台建设中长期规划.md`: 中台建设中长期规划
- `D:\work\AI\output\platform\doc\plan\云枢中台实施子任务.md`: 云枢 M1-M9 实施子任务

### 10.2 csyh 关键数据

| 指标 | 数值 |
|------|------|
| 业务模块数 | 18 个 |
| Java 文件数 | 4,876 |
| Vue 组件数 | 1,070 |
| API 路由数 | 4,440 |
| 业务方法数 | 65,000+ |
| SQL Migration | 313 个 |
| Mapper XML | 174 个 |
| **cn.flyrise.* 外部 import** | **7,557 个 (28%)** |
| **其中 SOURCE_MISSING** | **100%** |

---

**报告完毕**。

_生成日期: 2026-06-15_
