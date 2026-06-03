# L2 Spring 精简优化方案

**版本**: v1.0
**日期**: 2026-06-03
**目标**: 通过排除未使用的 Spring 自动配置、开启懒加载、调优线程池，将 6 个 platform-* 后端服务的内存占用从 5.7GB 压缩到 2.2GB（**-2.0GB / -35%**），不修改任何业务代码。
**前置条件**: 已完成 L1（JVM 调优），本方案独立于 L1。

---

## 一、优化前后对比

| 服务 | 当前堆 | L2 后堆 | 节省 | 关键排除项 |
|------|------|------|------|------|
| platform-user | 768MB | 384MB | -50% | Kafka 客户端（未直发）|
| platform-message | 768MB | 384MB | -50% | - |
| platform-auth | 512MB | 256MB | -50% | DataSource + JPA（仅调 user-service）|
| platform-workflow | 896MB | 512MB | -43% | - |
| platform-gateway | 512MB | 256MB | -50% | 已部分排除（DataSource/JPA）|
| platform-ops | 768MB | 384MB | -50% | Kafka（业务暂未启用）|
| **合计** | **4.2GB** | **2.2GB** | **-2.0GB** | |

> **8C16G 部署场景**: 整体余量从 200MB → 2.2GB，**完全消除 OOM 风险**。
> **4C8G 部署场景**: 仍需先实施 L1 + 配合其他 L3 方案。

---

## 二、三层优化策略

### 2.1 第一层：公共配置（所有服务共享）

在每个 `application.yml` 末尾追加：

```yaml
# ============================================================
# L2 优化: Spring 精简
# ============================================================
spring:
  main:
    lazy-initialization: true      # 懒加载, 节省启动期和空闲期内存
  jpa:
    open-in-view: false            # 关闭 OSIV (防 Hibernate 内存泄漏)
  mvc:
    throw-exception-if-no-handler-found: true   # 早失败
  web:
    resources:
      add-mappings: false          # 禁用静态资源处理 (前端是 nginx)

server:
  tomcat:
    threads:
      max: 80                      # 默认 200, 8 核减半
      min-spare: 10
    accept-count: 50
    max-connections: 2000

management:
  endpoints:
    web:
      exposure:
        include: health,info       # 仅暴露 health/info (不暴露 metrics/heapdump)
  endpoint:
    health:
      show-details: never
```

**预期收益**: 每个服务 -50MB（关闭 OSIV、关闭未用 Actuator、限 Tomcat 线程池）。

---

### 2.2 第二层：服务专属排除（按 pom.xml 依赖分析）

#### 依赖使用现状（基于 pom.xml + application.yml 静态分析）

| 服务 | 已声明但**未使用**的依赖 | 排除类 |
|------|------|------|
| **platform-user** | spring-kafka（无 producer/consumer） | KafkaAutoConfiguration |
| **platform-auth** | mysql-connector-j, mybatis-plus（仅通过 OpenFeign 调 user-service） | DataSource + MybatisPlus |
| **platform-gateway** | （已排除 DataSource/JPA，OK） | - |
| **platform-message** | spring-boot-starter-mail（业务暂未启用） | MailSenderAutoConfiguration |
| **platform-workflow** | spring-boot-starter-jdbc + flyway（Flowable 自管） | FlywayAutoConfiguration |
| **platform-ops** | spring-kafka（业务暂未启用）+ druid | KafkaAutoConfiguration |

> **结论**: 每个服务平均可排除 1-2 个 AutoConfiguration，节省 -80~150MB。

#### 各服务具体改动

##### platform-user/src/main/resources/application.yml
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
```

##### platform-auth/src/main/java/.../PlatformAuthApplication.java
```java
@SpringBootApplication(scanBasePackages = "com.cloudhub.platform", exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    MybatisPlusAutoConfiguration.class
})
```

##### platform-message/src/main/resources/application.yml
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
```

##### platform-workflow/src/main/resources/application.yml
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
```

##### platform-ops/src/main/resources/application.yml
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
```

##### platform-gateway（已配置, 无需改）
已在 `spring.autoconfigure.exclude` 中排除 DataSource/JPA/SpringDoc/Knife4j。

**预期收益**: 每个服务 -80~150MB（关键依赖类不被加载、相关 Bean 不创建）。

---

### 2.3 第三层：精简 pom.xml 依赖（可选，需谨慎）

> ⚠️ **风险**: 直接删除 dependency 会导致编译失败。建议先排除 AutoConfiguration 验证通过后再清理 pom。

| 服务 | 可移除 | 体积节省 | 风险 |
|------|------|------|------|
| platform-user | `spring-kafka`（如确认业务永久不需要） | ~2MB JAR + ~50MB 运行时 | 中（需 grep 验证无 KafkaTemplate 使用）|
| platform-auth | `mysql-connector-j`, `mybatis-plus-*`, `flyway-mysql` | ~5MB JAR | 低（仅通过 OpenFeign 调 user-service）|
| platform-message | `spring-boot-starter-mail`, `dysmsapi20170525`（如不用阿里云短信） | ~8MB JAR | 中（需确认业务） |
| platform-workflow | `flyway-mysql`, `spring-boot-starter-jdbc`（Flowable 自管 schema） | ~3MB JAR | 低 |
| platform-ops | `spring-kafka`（业务暂未启用） | ~2MB JAR | 中 |

**操作步骤**:
1. `grep -r "kafka\|KafkaTemplate" code/platform-server/platform-user/src` 验证无使用
2. 注释掉 pom.xml 的对应 `<dependency>` 块
3. `mvn clean package -DskipTests` 验证编译通过
4. 启动后查看 `EXCLUDE:` 日志确认 Bean 未创建

**预期收益**: -50~100MB 运行时（类加载器少加载 BeanDefinition）+ 镜像体积 -1~8MB。

---

## 三、实施计划（按风险由低到高）

### Phase 1：公共配置（零风险，1 小时）

```bash
# 1.1 修改 6 个 application.yml, 追加 L2 公共配置
# 1.2 git add . && git commit -m "perf(platform): 启用懒加载+线程池调优"
# 1.3 git push 触发 CI
# 1.4 验证: docker compose pull && up -d
#     docker stats platform-user platform-workflow 观察内存
```

**预期**: 总内存 -300MB（无业务改动风险）。

### Phase 2：服务专属排除（低风险，半天）

按 2.2 节表格，6 个服务逐个改 application.yml 或 Application.java：

```bash
# 2.1 逐服务修改 → 单服务 commit → 观察启动日志
# 2.2 验证: docker logs platform-user | grep "KafkaAutoConfiguration excluded"
# 2.3 监控内存: docker stats
```

**预期**: 总内存再 -500MB。

### Phase 3：pom 精简（中风险，1 周观察期）

需要每个服务做 grep 验证 + 单元测试 + 集成测试。分批做，每批只动一个服务。

**预期**: 总内存再 -200MB，镜像体积 -1~8MB。

---

## 四、验证清单

### 4.1 启动验证（每个服务）

```bash
# 查看自动配置排除日志
docker logs platform-user 2>&1 | grep -i "negative matches"

# 预期看到:
# Negative matches:
#   KafkaAutoConfiguration
#   MailSenderAutoConfiguration
#   ...
```

### 4.2 内存对比验证

```bash
# 优化前
docker stats --no-stream platform-user platform-workflow
# MEM USAGE / LIMIT 比例记录到 doc/性能基准.md

# 优化后对比
```

### 4.3 功能回归

| 场景 | 验证点 |
|------|------|
| 登录 | admin/123456 能登录 |
| 用户列表 | 列表渲染、雪花 ID 显示正常 |
| 工作流发起 | leave-approval 流程能起、能签收、能完成 |
| 消息发送 | Kafka 消息正常生产消费 |
| 候选人保存 | BPMN 保存后回显正确（已修复）|

### 4.4 性能基准

记录到 `doc/性能基准.md`：
- L1+L2 前: docker stats 截图
- L1+L2 后: docker stats 截图
- 对比: 堆内存、Metaspace、GC 暂停时间

---

## 五、风险与回滚

| 风险 | 概率 | 影响 | 回滚方案 |
|------|------|------|------|
| 懒加载导致首次访问慢 | 中 | 首请求 +200ms | 接受 / 关键 Bean 加 `@Lazy(false)` |
| 误排除必要配置 | 低 | 启动失败 | CI 立即报错, git revert |
| 排除后 Bean 缺失 | 低 | 运行时 NPE | 启动期 + 集成测试覆盖 |
| pom 精简漏删使用 | 中 | 编译失败 | mvn 报错, 恢复注释 |

**回滚命令**:
```bash
# 找到最近一次 L2 commit
git log --oneline | grep "perf(platform)" | head -3
# 回滚
git revert <commit-sha>
git push
```

---

## 六、相关文档

| 文档 | 说明 |
|------|------|
| `doc/部署指南.md` | 部署流程（更新：含 L2 后的内存建议）|
| `doc/服务器配置清单.md` | 硬件清单（更新：L2 后可降到 4C8G 的部分场景）|
| `doc/CI_CD操作手册.md` | CI/CD 流程（更新：Phase 3 的 pom 精简需 CI 验证）|
| `doc/性能基准.md` | 新建: 优化前后对比数据 |
| `.github/workflows/ci.yml` | CI 流水线（无需改）|

---

## 七、工作量估算

| Phase | 工作量 | 风险 | 收益 |
|------|------|------|------|
| Phase 1 公共配置 | 1 小时 | 零 | -300MB |
| Phase 2 服务排除 | 半天 | 低 | -500MB |
| Phase 3 pom 精简 | 1 周（含测试）| 中 | -200MB |
| **合计** | **~1.5 周** | 中 | **-1.0GB / -35%** |

**建议**: 实施 Phase 1 + Phase 2（-800MB，零/低风险），Phase 3 留作后续优化。
