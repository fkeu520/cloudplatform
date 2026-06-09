# Phase 1 实施计划：核心业务中台 + 基础设施能力

**版本：** v3.0 (已废弃)
**日期：** 2026-05-15
**废弃日期：** 2026-06-03
**当前主规划文档：** [`中台建设中长期规划.md`](中台建设中长期规划.md)（v1.0, 2026-06-03）

---

## ⚠️ 文档废弃说明（2026-06-03）

本计划基于 2026-05-15 的"Phase 1A/B/C + Sprint 3-5" 模型，**已被 [`中台建设中长期规划.md`](中台建设中长期规划.md) 完全取代**。

**废弃原因**:
- 2026-05-15 ~ 2026-05-28 期间计划已全部完成（见 [`项目进度.md`](项目进度.md) v6.0 ~ v6.8）
- 新规划已演进为"中台 5 层蓝图 + Q3 2026 路线图"，与本文件的 12-周 Sprint 模型不同
- 保留本文件仅作为实施历史档案，不再作为下一阶段规划依据

**当前下一阶段路线**:
- 完整路线图见 [`中台建设中长期规划.md` 第六章](中台建设中长期规划.md#六实施路线图)
- 实施历史见 [`项目进度.md`](项目进度.md)
- 详细方案见 [`opencode实施计划.md`](opencode实施计划.md) v7.0

---

# 以下为历史归档内容（2026-05-15 v3.0，已废弃）

**版本：** v3.0
**日期：** 2026-05-15
**周期：** 12 周（Week 1 - Week 12）
**目标：** 完成通用中台基座核心能力 + 基础设施能力
**定位：** 通用中台基座，非特定行业场景

## 概述

Phase 1 是云枢中台建设的核心阶段，目标是构建一套**通用的业务中台基座**，同时完成基础设施能力的建设。

### 核心原则

- **通用性**：能力抽象到行业无关的层面
- **可复用**：一次开发，多处复用
- **易扩展**：模块化设计，便于二次开发
- **可观测**：完善的监控告警体系
- **健壮性**：基础设施先行，服务稳定可靠

### 能力分层

```
Phase 1 建设范围：

┌─────────────────────────────────────────────────────────────────┐
│                         业务能力层                               │
│   用户中心 │ 权限中心 │ 门户中心 │ 流程中心（通用业务能力）       │
├─────────────────────────────────────────────────────────────────┤
│                         基础设施层                               │
│   Redis(缓存) │ Kafka(消息) │ MinIO(文件) │ XXL-JOB(任务)        │
├─────────────────────────────────────────────────────────────────┤
│                         公共组件层                               │
│   Nacos(注册/配置) │ Gateway(网关) │ ELK(日志) │ 监控告警       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Week 1：环境搭建 + 基础设施准备

### 任务清单

| 任务 | 负责人 | 输出物 | 验收标准 |
|------|--------|--------|---------|
| 1. 开发环境搭建（JDK 17/IDEA/Maven） | 全员 | 环境检查通过 | 编译无报错 |
| 2. Git 仓库创建 + 分支策略配置 | 运维 | Git 仓库 | 主分支保护规则生效 |
| 3. CI/CD 流水线搭建（Jenkins/GitLab CI） | 运维 | 流水线配置 | 提交代码自动触发构建 |
| 4. Docker Compose 环境（MySQL/Redis/Kafka/Nacos/MinIO） | 运维 | 一键启动脚本 | 所有组件正常运行 |
| 5. Nacos 配置中心初始化 | 运维 | 配置分组/命名空间 | 配置文件可发布 |
| 6. 项目骨架生成（Spring Cloud + Vue 3） | 后端/前端 | 代码骨架 | 可编译运行 |

### Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: platform
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: bitnami/kafka:3.6
    ports:
      - "9092:9092"
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 0@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
    volumes:
      - kafka-data:/bitnami/kafka

  nacos:
    image: nacos/nacos-server:v2.2.3
    environment:
      MODE: standalone
    ports:
      - "8848:8848"
      - "9848:9848"

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123

volumes:
  mysql-data:
  kafka-data:
```

### 基础设施能力验证

```bash
# Week 1 结束前必须验证以下功能：

# 1. MySQL 连接正常
mysql -h localhost -u root -proot123 -e "SELECT 1"

# 2. Redis 连接正常
redis-cli ping  # 应返回 PONG

# 3. Kafka 可发送/消费消息
kafka-topics.sh --list

# 4. Nacos 服务正常
curl http://localhost:8848/nacos/v1/console/health/readiness

# 5. MinIO 控制台可访问
curl http://localhost:9000/minio/health/live
```

---

## Week 2-3：用户中心 + 缓存基础设施

### 任务

| 任务 | 负责人 | 输出物 |
|------|--------|--------|
| 1. 用户中心微服务 | 后端 | platform-user |
| 2. Redis 缓存封装 | 后端 | CacheService |
| 3. 前端登录页面 | 前端 | 登录页 + Token 刷新 |
| 4. 用户中心单元测试 | 后端 | 测试用例（覆盖率>80%） |

### 模块划分：用户中心

```
platform-user (用户中心微服务)
├── 组织架构管理
│   ├── 组织管理（树形结构，CRUD）
│   └── 部门管理（CRUD）
├── 用户管理
│   ├── 用户注册（手机号/邮箱）
│   ├── 用户登录（密码/验证码）
│   ├── 用户信息（CRUD）
│   └── 用户状态（启用/禁用/锁定）
├── 认证登录
│   ├── 用户名密码登录
│   ├── 短信验证码登录
│   └── 第三方登录（钉钉/企微/飞书）
└── 会话管理
    ├── JWT Token 签发（存储到 Redis）
    ├── Token 刷新
    └── Token 主动失效（Redis 删除）
```

### 缓存架构实现

```java
// 缓存服务封装 - platform-common
@Component
public class CacheService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // ============ String 操作 ============
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    public <T> T get(String key, Class<T> clazz) {
        return (T) redisTemplate.opsForValue().get(key);
    }
    
    // ============ 分布式锁 ============
    public Boolean tryLock(String key, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, "1", timeout, unit);
    }
    
    public void unLock(String key) {
        redisTemplate.delete(key);
    }
    
    // ============ 接口限流 ============
    public boolean rateLimit(String key, int maxCount, long windowSeconds) {
        String countKey = "rate:" + key;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count == 1) {
            redisTemplate.expire(countKey, windowSeconds, TimeUnit.SECONDS);
        }
        return count <= maxCount;
    }
    
    // ============ Session 管理 ============
    public void setSession(String sessionId, Map<String, Object> session, long timeout, TimeUnit unit) {
        String key = "session:" + sessionId;
        redisTemplate.opsForHash().putAll(key, session);
        redisTemplate.expire(key, timeout, unit);
    }
    
    public Map<Object, Object> getSession(String sessionId) {
        return redisTemplate.opsForHash().entries("session:" + sessionId);
    }
    
    public void removeSession(String sessionId) {
        redisTemplate.delete("session:" + sessionId);
    }
}
```

### 数据库设计

```sql
-- 组织表
CREATE TABLE t_org (
    id              BIGINT PRIMARY KEY,
    parent_id       BIGINT DEFAULT 0,
    org_name        VARCHAR(100) NOT NULL,
    org_code        VARCHAR(50) NOT NULL,
    org_type        TINYINT NOT NULL COMMENT '1-集团 2-公司 3-部门 4-岗位',
    sort_order      INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT DEFAULT 0,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_tenant_code (tenant_id, org_code)
) COMMENT='组织表';

-- 用户表
CREATE TABLE t_user (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    username        VARCHAR(50) NOT NULL,
    password        VARCHAR(100) NOT NULL,
    real_name       VARCHAR(50),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    avatar          VARCHAR(255),
    org_id          BIGINT,
    status          TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用 2-锁定',
    last_login_time DATETIME,
    last_login_ip   VARCHAR(50),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT DEFAULT 0,
    UNIQUE KEY uk_tenant_username (tenant_id, username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email)
) COMMENT='用户表';
```

---

## Week 4：权限中心 + 消息队列集成

### 任务

| 任务 | 负责人 | 输出物 |
|------|--------|--------|
| 1. 权限中心微服务 | 后端 | platform-permission |
| 2. Kafka 消息封装 | 后端 | KafkaProducerService |
| 3. 消息消费示例 | 后端 | UserEventConsumer |
| 4. 权限校验拦截器 | 后端 | 接口级别权限校验 |

### 消息队列架构

```java
// 消息生产者服务 - platform-common
@Component
public class KafkaProducerService {
    
    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * 发送业务事件
     * @param topic  主题
     * @param key    消息Key（用于分区路由）
     * @param event  事件对象
     */
    public void sendEvent(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event);
    }
    
    /**
     * 发送用户事件
     */
    public void sendUserEvent(String eventType, Long userId, Map<String, Object> data) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("userId", userId);
        event.put("timestamp", System.currentTimeMillis());
        event.put("data", data);
        
        kafkaTemplate.send("user-events", userId.toString(), event);
    }
    
    /**
     * 发送系统事件
     */
    public void sendSystemEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("timestamp", System.currentTimeMillis());
        event.put("data", data);
        
        kafkaTemplate.send("system-events", eventType, event);
    }
}

// 消息消费者示例
@Component
public class UserEventConsumer {
    
    /**
     * 处理用户事件
     * Topic: user-events
     * Group: platform-message
     */
    @KafkaListener(topics = "user-events", groupId = "platform-message")
    public void handleUserEvent(ConsumerRecord<String, Object> record) {
        Map<String, Object> event = JSON.parseObject(record.value().toString());
        String eventType = (String) event.get("eventType");
        Long userId = (Long) event.get("userId");
        
        switch (eventType) {
            case "USER_REGISTER":
                // 发送欢迎短信/邮件
                sendWelcomeMessage(userId);
                break;
            case "USER_LOGIN":
                // 更新登录状态
                updateLoginStatus(userId, (Map<String, Object>) event.get("data"));
                break;
            case "USER_LOGOUT":
                // 清理会话
                cleanupSession(userId);
                break;
            default:
                log.warn("未知用户事件类型: {}", eventType);
        }
    }
    
    /**
     * 处理系统事件
     * Topic: system-events
     * Group: platform-message
     */
    @KafkaListener(topics = "system-events", groupId = "platform-message")
    public void handleSystemEvent(ConsumerRecord<String, Object> record) {
        // 系统事件处理
    }
}
```

### Kafka Topic 规划

| Topic | 说明 | 消费者组 |
|-------|------|---------|
| user-events | 用户相关事件 | platform-message |
| system-events | 系统事件 | platform-message |
| notification-events | 通知事件 | platform-message |
| file-events | 文件事件 | platform-message |
| flow-events | 流程事件 | platform-flow |

### 权限中心模块

```
platform-permission (权限中心微服务)
├── 功能权限
│   ├── 菜单权限（目录/菜单/按钮）
│   ├── API 权限（接口级别控制）
│   └── 按钮权限（操作级别控制）
├── 角色管理
│   ├── 角色 CRUD
│   ├── 角色权限分配
│   └── 角色继承
├── 权限配置
│   ├── 权限项定义
│   ├── 权限组管理
│   └── 权限变更审计
└── 权限校验
    ├── 接口级别校验（@PreAuthorize）
    ├── 按钮级别校验（前端隐藏）
    └── 数据范围校验（数据权限）
```

---

## Week 5-6：门户中心 + 对象存储

### 任务

| 任务 | 负责人 | 输出物 |
|------|--------|--------|
| 1. 门户中心微服务 | 后端 | platform-portal |
| 2. MinIO 文件服务封装 | 后端 | FileService |
| 3. 文件上传/预览 API | 后端 | 文件中心 API |
| 4. 主题/菜单配置页面 | 前端 | 门户配置页 |

### 文件服务封装

```java
// 文件服务 - platform-common
@Component
public class FileService {
    
    @Resource
    private MinioClient minioClient;
    
    @Value("${minio.bucket-name:platform}")
    private String bucketName;
    
    /**
     * 上传文件
     * @param file     文件对象
     * @param path      存储路径
     * @param fileName  文件名
     * @return 文件URL
     */
    public String uploadFile(MultipartFile file, String path, String fileName) {
        try {
            // 创建桶（如果不存在）
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            
            // 上传文件
            String objectName = path + "/" + fileName;
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
            
            // 返回访问URL
            return minioClient.getObjectUrl(bucketName, objectName);
        } catch (Exception e) {
            throw new BizException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取签名URL（私有文件访问）
     */
    public String getSignedUrl(String objectName, int expirySeconds) {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .expiry(expirySeconds, TimeUnit.SECONDS)
                .build());
    }
    
    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .build());
    }
    
    /**
     * 分片上传（大于100MB使用）
     */
    public InitiateMultipartUploadResult initiateUpload(String objectName, String contentType) {
        return minioClient.initiateMultipartUpload(
            InitiateMultipartUploadArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .contentType(contentType)
                .build());
    }
}
```

### 门户中心模块

```
platform-portal (门户中心微服务)
├── 主题管理
│   ├── 主题配置（颜色/字体/布局）
│   ├── 主题切换（浅色/深色）
│   └── 自定义主题
├── 菜单管理
│   ├── 菜单配置（增删改查）
│   ├── 菜单权限（角色可见）
│   └── 菜单排序
├── 组件库
│   ├── 基础组件（卡片/表格/表单）
│   ├── 业务组件（待办/通知/快捷入口）
│   └── 自定义组件
└── 页面配置
    ├── 工作台（个人桌面）
    ├── 首页（概览）
    └── 自定义页面
```

---

## Week 7-8：流程中心 + 定时任务

### 任务

| 任务 | 负责人 | 输出物 |
|------|--------|--------|
| 1. 流程中心微服务 | 后端 | platform-flow |
| 2. XXL-JOB 任务调度配置 | 后端 | 定时任务配置 |
| 3. 任务执行器开发 | 后端 | JobHandler 示例 |
| 4. 流程设计器集成 | 前端 | BPMN 设计器 |

### 定时任务架构

```java
// XXL-JOB 任务执行器示例
@Component
@JobHandler("userJobExecutor")
public class UserJobExecutor {
    
    @Resource
    private UserService userService;
    
    /**
     * 用户数据清理任务
     * 每天凌晨2点执行
     */
    @XxlJob("cleanInactiveUsers")
    public ReturnT<String> cleanInactiveUsers() {
        int count = userService.cleanInactiveUsers(90); // 90天未登录
        return new ReturnT<>(count + " 个用户已清理");
    }
    
    /**
     * 用户会话清理任务
     * 每小时执行
     */
    @XxlJob("cleanExpiredSessions")
    public ReturnT<String> cleanExpiredSessions() {
        // 清理 Redis 中过期的会话
        // ...
        return ReturnT.SUCCESS;
    }
    
    /**
     * 用户活动统计任务
     * 每天凌晨3点执行
     */
    @XxlJob("统计日活用户")
    public ReturnT<String> countDailyActiveUsers() {
        // 统计昨日 DAU
        long dau = userService.countDailyActiveUsers(
            LocalDate.now().minusDays(1).toString());
        // 发送 Kafka 事件
        kafkaTemplate.send("system-events", "DAILY_ACTIVE_USERS", 
            Map.of("date", LocalDate.now().minusDays(1), "count", dau));
        return new ReturnT<>("DAU: " + dau);
    }
}

// XXL-JOB 配置
@Configuration
public class XxlJobConfig {
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses("http://xxl-job:8080/xxl-job-admin");
        executor.setAppname("platform-executor");
        executor.setPort(9999);
        executor.setAccessToken("cloudhub-token");
        executor.setLogPath("/data/xxl-job/logs");
        return executor;
    }
}
```

### XXL-JOB 任务规划

| 任务名称 | Cron 表达式 | 说明 |
|---------|------------|------|
| cleanInactiveUsers | 0 0 2 * * ? | 清理90天未登录用户 |
| cleanExpiredSessions | 0 0 * * * ? | 清理过期会话 |
| syncUserToEs | 0 0 3 * * ? | 同步用户数据到ES |
| generateDailyReport | 0 30 0 * * ? | 生成日报 |
| cleanOldLogs | 0 0 4 * * ? | 清理30天前的日志 |
| sendReminderEmails | 0 0 9 * * ? | 发送待办提醒邮件 |

### 流程中心模块

```
platform-flow (流程中心微服务)
├── 流程定义
│   ├── 流程模型（BPMN 可视化设计）
│   ├── 流程版本（版本管理）
│   └── 流程发布（部署/启用/停用）
├── 流程实例
│   ├── 流程发起
│   ├── 流程审批（通过/驳回/转办）
│   ├── 流程撤回
│   └── 流程终止
├── 任务管理
│   ├── 待办任务（我的待办）
│   ├── 已办任务（审批历史）
│   ├── 抄送任务
│   └── 任务提醒（Kafka 消息通知）
└── 流程监控
    ├── 流程实例查询
    ├── 流程进度跟踪
    └── 流程效率分析
```

---

## Week 9-10：API 网关 + ELK 日志 + 前端集成

### 任务

| 任务 | 负责人 | 输出物 |
|------|--------|--------|
| 1. API 网关配置 | 后端 | Spring Cloud Gateway |
| 2. ELK 日志收集配置 | 运维 | Logback + Logstash |
| 3. 日志查询 API | 后端 | 日志中心 API |
| 4. 前端 PC 后台框架 | 前端 | Vue 3 + Element Plus |
| 5. 权限管理页面 | 前端 | 用户/角色/权限管理 |

### 日志架构

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    
    <!-- Console 输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${appName}"}</customFields>
        </encoder>
    </appender>
    
    <!-- 文件输出（同步到 Logstash） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/data/logs/${appName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/data/logs/${appName}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${appName}","env":"${SPRING_PROFILES_ACTIVE}"}</customFields>
        </encoder>
    </appender>
    
    <!-- 异步输出到 Kafka（高并发场景） -->
    <appender name="KAFKA" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/data/logs/${appName}-kafka.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 网关核心配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: platform-user
          uri: lb://platform-user
          predicates:
            - Path=/api/v1/users/**
          filters:
            - StripPrefix=1
            
        - id: platform-permission
          uri: lb://platform-permission
          predicates:
            - Path=/api/v1/permissions/**
          filters:
            - StripPrefix=1
            
        - id: platform-portal
          uri: lb://platform-portal
          predicates:
            - Path=/api/v1/portals/**
          filters:
            - StripPrefix=1
            
        - id: platform-flow
          uri: lb://platform-flow
          predicates:
            - Path=/api/v1/flows/**
          filters:
            - StripPrefix=1
            
        - id: platform-file
          uri: lb://platform-file
          predicates:
            - Path=/api/v1/files/**
          filters:
            - StripPrefix=1
            
      default-filters:
        - DedupeResponseHeader=Vary Access-Control-Allow-Origin Access-Control-Allow-Credentials
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burstCapacity: 200
        - name: CircuitBreaker
          args:
            name: defaultCircuitBreaker
            fallbackUri: forward:/fallback
```

---

## Week 11-12：监控告警 + 链路追踪 + 集成测试

### 任务

| 任务 | 负责人 | 输出物 |
|------|--------|--------|
| 1. Prometheus 监控配置 | 运维 | 监控大盘 |
| 2. Grafana 仪表盘 | 运维 | 可视化面板 |
| 3. SkyWalking 链路追踪 | 运维 | 链路分析 |
| 4. 链路追踪 API | 后端 | 日志关联 |
| 5. 集成测试 | 测试 | 测试报告 |
| 6. 性能测试 | 测试 | 压测报告 |
| 7. 文档整理 | 全员 | Phase 1 文档 |

### 监控指标

| 指标类型 | 指标项 | 告警阈值 |
|---------|--------|---------|
| **基础监控** | CPU > 80% | 警告 |
| **基础监控** | Memory > 85% | 警告 |
| **基础监控** | Disk > 90% | 警告 |
| **应用监控** | Error Rate > 1% | 警告 |
| **应用监控** | Response Time > 500ms | 警告 |
| **Kafka 监控** | 消费延迟 > 1000 | 警告 |
| **Redis 监控** | 内存使用 > 80% | 警告 |
| **业务监控** | Login Failed > 10/min | 警告 |
| **业务监控** | 接口 5xx > 5/min | 警告 |

### 链路追踪配置

```yaml
# SkyWalking Agent 配置
agent:
  collector:
    backend_service: skywalking:11800
  service_name: ${spring.application.name}
  trace_ignore_path: /health,/info,/actuator/**
  
# Logback 关联 TraceId
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <customFields>{"traceId":"${TRACE_ID:-}"}</customFields>
</encoder>
```

---

## 里程碑检查点

### Milestone 1：Week 4 结束

- [ ] 用户中心开发完成
- [ ] Redis 缓存服务正常
- [ ] Kafka 消息服务正常
- [ ] 可创建组织/用户
- [ ] 可登录 PC 后台
- [ ] 权限中心基础功能完成

### Milestone 2：Week 8 结束

- [ ] 门户中心开发完成
- [ ] MinIO 文件服务正常
- [ ] 可配置主题/菜单
- [ ] 流程中心开发完成
- [ ] XXL-JOB 定时任务配置完成
- [ ] 可发起简单审批流程

### Milestone 3：Week 12 结束

- [ ] API 网关配置完成
- [ ] ELK 日志收集正常
- [ ] Prometheus/Grafana 监控正常
- [ ] SkyWalking 链路追踪正常
- [ ] 所有模块集成测试通过
- [ ] 性能测试通过（响应时间 < 200ms）
- [ ] Phase 1 文档完整

---

## Phase 1 基础设施能力验收清单

```
Week 1 验收：基础设施就绪
[ ] MySQL 8.0 正常启动，可执行 SQL
[ ] Redis 7.0 正常，可 ping 通
[ ] Kafka 3.6 正常，可发送/消费消息
[ ] Nacos 2.2.3 控制台可访问
[ ] MinIO 控制台可访问（9000/9001）
[ ] Docker Compose 一键启动成功

Week 4 验收：缓存+消息就绪
[ ] Redis 缓存服务封装完成
[ ] 接口限流功能可用
[ ] 分布式锁功能可用
[ ] Kafka 生产者/消费者封装完成
[ ] 用户事件可发送/消费

Week 6 验收：文件存储就绪
[ ] MinIO 文件上传/下载正常
[ ] 分片上传功能正常
[ ] 签名 URL 生成正常

Week 8 验收：任务调度就绪
[ ] XXL-JOB 控制台可访问
[ ] 定时任务可配置/执行
[ ] 任务执行日志可查询

Week 12 验收：可观测性就绪
[ ] Prometheus 采集所有服务指标
[ ] Grafana 仪表盘显示正常
[ ] SkyWalking 链路追踪正常
[ ] ELK 日志可查询
[ ] 告警规则配置完成
```

---

## Phase 1 服务依赖关系

```
┌────────────────────────────────────────────────────────────────────┐
│                        API 网关 (Gateway)                           │
│              路由 → 认证 → 限流 → 日志 → 链路追踪                      │
└────────────────────────────────┬───────────────────────────────────┘
                                 │
       ┌──────────────────────────┼──────────────────────────┐
       │                          │                          │
       ▼                          ▼                          ▼
┌─────────────┐          ┌─────────────┐          ┌─────────────┐
│ 用户中心    │          │ 权限中心    │          │ 门户中心    │
│ (User)     │          │ (Permission)│          │ (Portal)   │
└──────┬──────┘          └──────┬──────┘          └──────┬──────┘
       │                          │                          │
       │   ┌──────────────────────┼──────────────────────┐   │
       │   │                      │                      │   │
       ▼   ▼                      ▼                      ▼   ▼
┌─────────┐ ┌─────────┐    ┌─────────┐            ┌─────────┐
│  Redis  │ │  Kafka  │    │  MySQL  │            │  MinIO  │
│ (缓存)  │ │ (消息)  │    │ (数据)   │            │ (文件)  │
└─────────┘ └─────────┘    └─────────┘            └─────────┘
       │
       │   ┌──────────────────────────────┐
       │   │                              │
       ▼   ▼                              ▼
┌─────────┐                         ┌─────────┐
│XXL-JOB  │                         │   ES    │
│(任务)   │                         │(日志/搜索)│
└─────────┘                         └─────────┘
```

---

## 资源需求

### 人员配置

| 角色 | 人数 | Week 1-4 | Week 5-8 | Week 9-12 |
|------|------|---------|---------|----------|
| 项目经理 | 1 | 全程 | 全程 | 全程 |
| 后端开发 | 4 | 4人 | 4人 | 3人 |
| 前端开发 | 2 | 1人 | 2人 | 2人 |
| 测试工程师 | 1 | - | - | 1人 |
| 运维工程师 | 1 | 1人 | 1人 | 1人 |
| 产品经理 | 1 | 0.5人 | 0.5人 | 1人 |

### 环境需求

| 环境 | 配置 | 数量 | 用途 |
|------|------|------|------|
| 开发环境 | 8核 16G | 每人1台 | 本地开发 |
| 测试环境 | 8核 32G | 2台 | 集成测试 |
| 演示环境 | 8核 32G | 1台 | 演示/验收 |

---

## 风险应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 基础设施组件故障 | 服务不可用 | 高可用部署，多副本备份 |
| Kafka 消息积压 | 业务延迟 | 增加消费者，监控告警 |
| Redis 内存不足 | 缓存失效 | 内存监控，及时扩容 |
| MinIO 磁盘满 | 文件存储失败 | 磁盘监控，清理策略 |
| XXL-JOB 任务失败 | 定时任务丢失 | 重试机制，告警通知 |

---

## Phase 1 交付物清单

| 类型 | 交付物 | 说明 |
|------|--------|------|
| **源码** | platform-user | 用户中心微服务 |
| **源码** | platform-permission | 权限中心微服务 |
| **源码** | platform-portal | 门户中心微服务 |
| **源码** | platform-flow | 流程中心微服务 |
| **源码** | platform-gateway | API 网关 |
| **源码** | platform-admin | PC 管理后台 |
| **配置** | Docker Compose | 一键启动所有中间件 |
| **配置** | Nacos 配置 | 所有服务的配置中心 |
| **文档** | 技术选型文档 | 技术栈确认 |
| **文档** | API 接口文档 | Swagger 在线 |
| **文档** | 部署手册 | 环境准备、部署步骤 |
| **演示** | 演示环境 | 所有功能可体验 |

---

**文档版本记录：**

| 版本 | 日期 | 变更内容 | 编制人 |
|------|------|---------|--------|
| v1.0 | 2026-05-15 | 初始版本（园区场景） | AI 助手 |
| v2.0 | 2026-05-15 | 调整为通用中台基座 | AI 助手 |
| v3.0 | 2026-05-15 | 增加基础设施能力层（消息队列/缓存/日志/任务） | AI 助手 |

---

**📌 Phase 1 准备就绪，可以开始！**