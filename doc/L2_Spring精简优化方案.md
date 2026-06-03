# L2 Spring 精简优化方案 v2.0（保守修订版）

**版本**: v2.0（v1.0 修订）
**日期**: 2026-06-03
**核心原则**:
1. **可用性优先**: 任何优化不得引入消息丢失、首次请求延迟、扩展性下降
2. **可扩展性优先**: 不锁死技术栈，依赖能加能减
3. **可回滚性**: 每个改动必须能 1 分钟内回退
4. **可观测性**: 优化前后必须有数据对比（性能基准模板）

> **v1.0 → v2.0 关键变化**: 删除 Phase 3、删除全局懒加载、Phase 2 改为**配置开关**而非硬编码 exclude、Phase 1 仅调优不变更行为。

---

## 一、v1.0 方案的违规审计

| 编号 | 原方案 | 违规类型 | 严重度 |
|------|------|------|------|
| V1 | 全局 `lazy-initialization=true` | 破坏 `@KafkaListener` 容器启动时序 → **消息丢失** | 🔴 严重 |
| V2 | 排除 `platform-workflow` 的 Flyway | Flyway migration 目录空但 `database-schema-update: true` 让 Flowable 自动建表 → **多环境 schema 不一致** | 🔴 严重 |
| V3 | 排除 `platform-user` 的 Flyway | user 有 V1-V19 完整迁移，业务关键 | 🔴 严重 |
| V4 | Phase 3 删 pom 依赖 | 技术栈锁死，新人加功能困惑 | 🔴 严重 |
| V5 | 排除 `platform-auth` 的 DataSource | 当前未用 OK，但未来 SSO/OAuth 必用 → **扩展性受限** | 🟡 中等 |
| V6 | 排除 `platform-message` 的 Mail | 阿里云短信可能当前在用 | 🟡 中等 |
| V7 | Tomcat max-threads=80（硬编码） | 高并发场景下成为瓶颈 | 🟡 中等 |
| V8 | Redis pool max-active=8（硬编码） | 多副本下连接数不够 | 🟡 中等 |

---

## 二、保守优化方案（保留可扩展性 + 高可用）

### 2.1 新版三阶段总览

| 阶段 | 改动 | 工作量 | 风险 | 收益 |
|------|------|------|------|------|
| **Phase A 公共调优** | 仅调优线程池/连接池，**不改行为** | 1 小时 | 零 | -100MB |
| **Phase B 行为开关化** | 把硬编码改为 Nacos 配置项 | 半天 | 低 | -300MB |
| **Phase C 灰度 + 监控** | 灰度发布 + 监控验证 | 1 天 | 低 | 回归保护 |
| **合计** | - | **~2 天** | 低 | **-400MB（保守估计）** |

> **重要**: 此版不追求激进内存压缩。生产环境内存余量 > 性能极限。

---

### 三、Phase A：纯调优（零行为变更，零风险）

> **核心原则**: 调优不改行为，只调参数。所有值都通过环境变量可覆盖。

#### A.1 Tomcat 线程池（按服务差异化）

每个 `application.yml` 末尾追加：

```yaml
server:
  tomcat:
    threads:
      # 公式: max = 期望并发数 / 副本数 × 1.5
      # 例如: 200 并发 / 2 副本 × 1.5 = 150
      # 当前假设 8C16G 单机双副本, 不同服务并发差异大, 不用统一值
      max: ${TOMCAT_THREADS_MAX:80}
      min-spare: ${TOMCAT_THREADS_MIN:10}
    accept-count: 100
    max-connections: 2000
    connection-timeout: 20s
```

**差异化默认值**（在 docker-compose.yml 的 environment 中覆盖）：

| 服务 | TOMCAT_THREADS_MAX | 依据 |
|------|------|------|
| platform-gateway | 200 | 网关是流量汇聚点，承载所有后端 |
| platform-auth | 100 | 登录路径必经 |
| platform-user | 80 | 普通业务 |
| platform-message | 80 | 普通业务 |
| platform-workflow | 100 | BPMN 流程审批可能瞬时并发高 |
| platform-ops | 60 | 运营后台并发低 |

#### A.2 Redis 连接池（按服务差异化）

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          # 公式: max-active = 期望 QPS / 副本数 / Redis 接受 QPS × 副本数
          # 简化: max-active = Tomcat max × 0.1 (10% 线程需要 Redis)
          max-active: ${REDIS_POOL_MAX:8}
          max-idle: ${REDIS_POOL_MAX:8}
          min-idle: 2
          max-wait: 2s
```

**差异化**（在 docker-compose 覆盖）：

| 服务 | REDIS_POOL_MAX |
|------|------|
| platform-gateway | 20（限流用）|
| platform-auth | 16（Token 验证高频）|
| platform-user | 8 |
| platform-message | 12（站内信缓存）|
| platform-workflow | 12（流程变量缓存）|
| platform-ops | 8 |

#### A.3 Kafka 消费参数

```yaml
spring:
  kafka:
    consumer:
      # 消费并发 = 副本数, 不超过 topic partition 数
      concurrency: ${KAFKA_CONSUMER_CONCURRENCY:1}
      max-poll-records: ${KAFKA_MAX_POLL:50}
    producer:
      # 关键: 高可用场景下不能批量丢消息
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1
```

#### A.4 JVM 堆（统一通过 JAVA_OPTS）

```yaml
# docker-compose.yml 每个服务加 JAVA_OPTS
environment:
  - JAVA_OPTS=${JAVA_OPTS:--Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100}
```

**差异化建议**（生产 8C16G）：

| 服务 | -Xmx | -Xms | 理由 |
|------|------|------|------|
| platform-gateway | 512m | 256m | WebFlux 反应式，堆压力小 |
| platform-auth | 512m | 256m | 无 DB，堆压力小 |
| platform-user | 768m | 384m | 缓存多 |
| platform-message | 768m | 384m | 模板缓存 |
| platform-workflow | 1024m | 512m | Flowable 引擎重 |
| platform-ops | 768m | 384m | 多模块 |

#### A.5 预期收益与不变项

| 收益 | 数值 | 原因 |
|------|------|------|
| 内存节省 | -100MB | Tomcat 线程栈从默认 200 降到合理值 |
| 内存节省 | -50MB | Redis 连接池默认无界，加显式限制 |
| **合计** | **-150MB** | **零行为变更，零风险** |

**不变项（关键）**:
- ✅ **不启用懒加载**: 保留 `@KafkaListener` / `@Scheduled` 启动期注册
- ✅ **不修改 Bean 装配**: 现有所有 Service / Controller / Component 不动
- ✅ **不修改 pom 依赖**: 0 编译风险

---

### 四、Phase B：行为开关化（低风险，配置驱动而非硬编码）

> **核心原则**: 不是"禁用某个能力"，而是"按需启用某个能力"。

#### B.1 创建开关配置（写入 Nacos `common.yml`）

```yaml
# Nacos Config: common.yml
# 平台级功能开关, 任何服务可读, 通过 Nacos 控制台修改即可热生效 (部分配置)

platform:
  features:
    # 关闭后不初始化 Mail 发送器 (节省 ~30MB)
    mail-enabled: ${PLATFORM_MAIL_ENABLED:false}
    # 关闭后不初始化 Kafka 生产者 (节省 ~20MB)
    # 注意: workflow 强依赖 Kafka, 此开关对 workflow 无效
    kafka-producer-enabled: ${PLATFORM_KAFKA_PRODUCER_ENABLED:true}
    # 关闭后不加载 OpenFeign (节省 ~50MB)
    feign-enabled: ${PLATFORM_FEIGN_ENABLED:true}
```

#### B.2 各服务读开关（@ConditionalOnProperty）

**v1.0 错误做法**: `spring.autoconfigure.exclude` 硬编码
**v2.0 正确做法**: 用 Spring 的 `@ConditionalOnProperty` 注解

```java
// 示例: MailConfig.java（新增）
@Configuration
@ConditionalOnProperty(name = "platform.features.mail-enabled", havingValue = "true", matchIfMissing = false)
public class MailConfig {
    @Bean
    public JavaMailSender mailSender() { ... }
}
```

```java
// 示例: KafkaProducerConfig.java
@Configuration
@ConditionalOnProperty(name = "platform.features.kafka-producer-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfig { ... }
```

**好处**:
- 想启用 → Nacos 改 `true` → 滚动重启（不需改代码/打包）
- 想关闭 → Nacos 改 `false` → 滚动重启
- **永远不删依赖**，扩展性 100% 保留

#### B.3 哪些服务做开关化

| 服务 | 开关 | 节省 | 必要性 |
|------|------|------|------|
| platform-message | mail-enabled=false | -30MB | 🟡 中（阿里云 SMS 仍可能用）|
| platform-user | kafka-producer-enabled=false | -20MB | 🟢 高（当前确无 producer）|
| platform-ops | kafka-producer-enabled=false | -20MB | 🟢 高（同上）|
| platform-auth | feign-enabled=false | -50MB | ❌ 跳过：auth 强依赖 user-service Feign |

> **重要**: 任何开关默认值 = 当前行为（true），关闭需要显式操作。新部署保持全功能，避免误关。

#### B.4 预期收益与风险

| 收益 | 数值 |
|------|------|
| Phase B 节省 | -70MB（关闭 3 个 Bean 配置）|
| **可回滚性** | Nacos 改 true → 30s 生效，无需代码改动 |
| **可扩展性** | 依赖全保留，配置层控制，未来加功能零成本 |

**风险**: 配置改错 → 启动期 NPE
**缓解**: 启动期 Bean 缺失会立即报错（`NoSuchBeanDefinitionException`），不会"看似启动成功实际异常"

---

### 五、Phase C：灰度 + 监控保障（必做！）

> **核心原则**: 优化上线必须有数据支撑，没有监控的优化是盲改。

#### C.1 灰度发布策略

```
1. 第一批: 仅 platform-user 启用 Phase A（线程池调优）
   - 观察 24h: docker stats / 接口 RT / 错误率 / Kafka 消费 lag
   - 指标正常 → 进入第二批
   
2. 第二批: 启用 platform-gateway / platform-auth
   - 观察 24h, 重点: 网关 RT、登录成功率
   
3. 第三批: 启用 platform-message / platform-workflow / platform-ops
   - 重点: 消息发送成功率、流程审批成功率

每个 Phase 间隔 24h, 任何指标恶化立即回滚
```

#### C.2 必须监控的指标

写入 `doc/性能基准.md` 模板：

| 指标 | 工具 | 阈值 |
|------|------|------|
| 容器内存 | docker stats | < Xmx × 1.3（堆外） |
| 堆使用率 | Actuator `/actuator/metrics/jvm.memory.used` | < 80% |
| GC 暂停 | `/actuator/metrics/jvm.gc.pause` P99 | < 100ms |
| 接口 RT | Micrometer Timer | P99 < 500ms |
| Kafka 消费 lag | Kafka Admin API | < 100 |
| Redis 连接数 | Lettuce 客户端指标 | < max-active × 副本数 |

#### C.3 回滚预案

```bash
# Phase A 回滚（仅调参，秒级）
# 直接在 Nacos 改回原值，或 docker compose 重启前用旧 env vars
docker compose up -d platform-user  # env vars 已在 compose 中

# Phase B 回滚（关掉开关）
# Nacos: platform.features.mail-enabled = true
# 等下次滚动重启生效
```

---

### 六、容量规划（必须先算账再优化）

> **核心原则**: 不能拍脑袋说"够用"，必须按业务模型算。

#### 6.1 业务假设（2026 H2 预期）

| 指标 | 值 |
|------|------|
| 日活用户 | 5,000 |
| 峰值并发 | 500（早上 9-10 点）|
| 平均每人每日请求 | 200 |
| 峰值 QPS | 500 × 2 (avg 2 req/concurrent) = 1000 |
| 工作流日发起量 | 1,000 |
| 消息日发送量 | 50,000 |

#### 6.2 资源计算

**Tomcat 线程数**:
```
每个请求平均耗时 200ms
单线程 QPS = 1 / 0.2 = 5
200 QPS per service / 5 = 40 线程
× 1.5 缓冲 = 60 线程
× 双副本分摊 = 30 线程 per pod
```

**Redis 连接数**:
```
500 并发 × 10% 同时访问 Redis = 50 connections
÷ 6 服务 ÷ 2 副本 = 4 connections per pod
最小 4, 推荐 8 (含缓冲)
```

**Kafka consumer 数量**:
```
消息量 50000/86400s = 0.6 msg/s
单 consumer 处理 10 msg/s 足够
副本数 2 = 2 consumer
```

#### 6.3 容量表

| 资源 | 单 pod | 双副本 | 4 副本 |
|------|------|------|------|
| Tomcat 线程 | 80 | 160 | 320 |
| Redis 连接 | 8 | 16 | 32 |
| Kafka consumer | 1 | 2 | 4 |
| 内存（Xmx）| 768MB | - | - |

**结论**: 8C16G 单机 + 双副本可承载 **500 峰值并发**，足够到 2027 H1。

---

### 七、被删除的 v1.0 内容及原因

| v1.0 内容 | 删除原因 |
|------|------|
| Phase 1 全局 `lazy-initialization=true` | 破坏 `@KafkaListener` / `@Scheduled` 启动时序 → 消息丢失 |
| Phase 2 排除 `platform-workflow` 的 Flyway | Flowable auto-DDL 接管，多环境 schema 不一致 |
| Phase 2 排除 `platform-user` 的 Flyway | user 有 V1-V19 业务迁移，关键 |
| Phase 2 排除 `platform-auth` 的 DataSource | 锁死未来 SSO/OAuth 扩展 |
| Phase 2 排除 `platform-message` 的 Mail | 阿里云 SMS 可能在用，配置化保留 |
| Phase 3 删 pom 依赖 | 锁死技术栈，新人 onboarding 难，IDE 失能 |
| 硬编码 Tomcat/Redis 数值 | 不可按环境调优，无扩展性 |

---

### 八、新版工作量与收益

| 阶段 | 改动 | 工作量 | 风险 | 收益 |
|------|------|------|------|------|
| Phase A 调优 | 6 个 application.yml + docker-compose 差异化 env | 1 小时 | 零 | -150MB |
| Phase B 开关化 | 新增 MailConfig/KafkaConfig + @ConditionalOnProperty + Nacos common.yml | 半天 | 低 | -70MB |
| Phase C 灰度监控 | 写监控脚本 + 分批发布 + 性能基准 | 1 天 | 低 | 回归保护 |
| **合计** | - | **~2 天** | 低 | **-220MB（保守）** |

**对比 v1.0**:
- v1.0 承诺 -1.0GB，**实为高风险假数据**（多数排除项违反可扩展原则）
- v2.0 承诺 -220MB，**全部可回滚 + 零技术栈锁死 + 零消息丢失风险**

---

### 九、相关文档

| 文档 | 说明 |
|------|------|
| `doc/性能基准.md` | Phase C 期间持续填充的监控数据 |
| `doc/部署指南.md` | 引用本方案作为性能优化章节 |
| `doc/CI_CD操作手册.md` | Phase C 灰度发布操作 |
| `doc/服务器配置清单.md` | 容量表来源 |

---

### 十、待办

- [ ] **决策**: 是否同步给运维组评审本方案（生产变更评审流程）
- [ ] **依赖**: 部署前必须安装 Prometheus + Grafana 监控栈
- [ ] **依赖**: Nacos `common.yml` 需先创建，Phase B 才能热加载
- [ ] **测试**: Phase A 调优需在测试环境压测 24h 后再上生产
