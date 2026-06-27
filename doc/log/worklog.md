# 云枢中台 - 工作日志

**版本**: v0.1 (2026-06-26 起)
**粒度**: 每日（工时粒度 0.5h）
**记录人**: AI agent + 用户复核
**用途**: 工时审计 + 工作内容追溯

### 类型枚举

| 类型 | 含义 |
|------|------|
| `评审` | 评审方案/代码/PR |
| `方案` | 编写/修改规划/设计文档 |
| `编码` | 实际写代码 |
| `测试` | 单测/集成测试/验证 |
| `调试` | 排查 bug / 性能 |
| `运维` | 部署/CI/配置 |
| `协调` | 同步文档/会议/沟通 |
| `文档` | 写使用手册/README/KNOWN_ISSUES |

---

## 项目工时总览

| 阶段 | 起止时间 | 累计工时 |
|------|---------|---------|
| 项目启动 → W1 (2026-05-28 ~ W3 6-08) | ~2 周 | ~30 天 (估算，含多人协作) |
| W3 后 ~ 今日 (2026-06-08 ~ 2026-06-25) | ~3 周 | ~25 天 (估算) |
| 修补阶段 v1 (opencode 实施计划 P0/P1/P2) | 2026-06-08 ~ 2026-06-25 | 已完成 |
| 修补阶段 v2 — 配置动态化（规划）| 2026-06-26 起 | 预计 4 天 |
| **累计** | **~2026-05-28 至今约 4 周** | **估算 ~55 天** |

> ⚠️ 项目启动至 2026-06-25 的工时为基于 commit 频度、文档变更次数、handoff 数量的**反向估算**，非精确工时记录。仅作量级参考。

---

## 2026-06-26 (Thu)

### 目的

两件事：
1. 评审并修正 `doc/plan/配置动态改造方案.md` (v1.0 → v1.1)，确认 Spring Boot 版本对 Nacos 接入方式的影响，并同步更新相关规划文档。
2. 修复房间编辑 bug：房间配套、用途等字段修改后未生效。

### 结果

- 方案 v1.1 已落地：4 处错误修正（P1-B-4 RSA 删除、P2-C 描述、ADR 命名空间折中、限流分层表），方向修正（bootstrap.yml → spring.config.import），同步更新 2 份规划文档。
- Bug 修复：`RoomService.update()` 补齐 10 个缺失字段（kitId/purposeId/image/introduce/sorting/houseStructure/unitPrice/totalPrice/buildArea/billableArea），新增 1 个回归测试 `update_v37Fields_shouldPersist`，37 个单测全部通过。

### 改动文件

| 文件 | 改动 |
|------|------|
| `doc/plan/配置动态改造方案.md` | v1.0 → v1.1（13 处改动） |
| `doc/plan/中台建设中长期规划.md` | v1.0 → v1.1（第六章 Q3/Q4 路线图追加配置动态化 sprint） |
| `doc/log/opencode实施计划.md` | v7.0 → v7.1（新增"修补阶段 v2 — 配置动态化"小节） |
| `doc/log/worklog.md` | 新建（本文档） |
| `code/platform-server/park-space/src/main/java/.../RoomService.java` | update() 补齐 10 个字段 |
| `code/platform-server/park-space/src/test/java/.../RoomServiceTest.java` | 新增 update_v37Fields_shouldPersist 回归测试 |
| `code/platform-server/park-space/src/main/resources/db/migration/V50__fix_room_submenu_icons.sql` | 新建：UPDATE 113/114/115/116 icon 为 FA class 名 |
| `doc/log/KNOWN_ISSUES.md` | 追加 #32 (Service.update 白名单遗漏教训) |
| `code/platform-server/platform-message/.../ChannelSenderRegistry.java` | HashMap → ConcurrentHashMap (P0-A 1 行修复) |
| `code/platform-server/platform-gateway/src/main/resources/scripts/rate_limit.lua` | 新建：Redis 滑动窗口限流 Lua 脚本 |
| `code/platform-server/platform-gateway/.../filter/RateLimitFilter.java` | V2 Redis 化 + 灰度开关 + 降级路径 (P0-B) |
| `code/platform-server/platform-gateway/src/test/.../RateLimitFilterTest.java` | 新建：3 个 Lua 脚本单测 |

### 工时

| 工作内容 | 类型 | 工时 |
|---------|------|------|
| 配置动态改造方案 v1.0 评审（4 个议题分析） | 评审 | 1.5h |
| 方案修正：RSA 删除、限流分层表、ADR 更新、P1-A 方向翻转 | 方案 | 1h |
| 同步更新 中台规划 v1.1 + opencode 实施计划 v7.1 | 文档 | 0.5h |
| 全文档同步扫描（确认其他文档无需修改） | 评审 | 0.5h |
| 创建工作日志文档 + 类型列设计 | 文档 | 0.5h |
| 房间编辑 bug 修复（RoomService.update 补 10 字段 + 回归测试） | 编码 | 1h |
| mvn compile + 37 单测验证 | 测试 | 0.5h |
| 追加 KNOWN_ISSUES #32 (Service.update 白名单遗漏经验教训) | 文档 | 0.5h |
| 房源用途/拆分合并菜单图标调查（Layout.vue + MenuService） | 调试 | 0.5h |
| V50 Flyway 迁移脚本：UPDATE 113/114/115/116 icon 为 FA class 名 | 运维 | 0.5h |
| P0-A: ChannelSenderRegistry HashMap → ConcurrentHashMap (1 行) | 编码 | 0.5h |
| P0-B: RateLimitFilter Redis 化（Lua 脚本 + ReactiveRedisTemplate + 灰度开关 + 降级路径） | 编码 | 1.5h |
| P0-B: 3 个 Lua 脚本单测 | 测试 | 0.5h |
| P1-A: 调查 6 模块 bootstrap/import 现状，识别差异 | 评审 | 0.5h |
| P1-A: 5 模块删 bootstrap.yml（auth/gateway/ops/user/workflow） + 6 个 commits | 编码 | 0.5h |
| P1-A: workflow 补 nacos.config.enabled=false，gateway/workflow/message 补 config.import | 编码 | 0.5h |
| P1-A: 9 模块 application.yml 一致性校验 + mvn compile 5 模块 | 测试 | 0.5h |
| **当日合计** | | **11.5h** |

---

## 2026-06-27 (Sat)

| 工作内容 | 类型 | 工时 |
|---------|------|------|
| PR1-7 灰度发布基础设施代码/配置合并到 develop | 编码 | 4h |
| PR7 修复 AuthService (Number→String ClassCastException) | 编码 | 1h |
| Nacos common.yml 补灰度维度字段 (dimension/tenants/users/percent) | 运维 | 0.5h |
| Nacos POST API type=yaml 第一次忘写→重推 | 运维 | 0.5h |
| 分析 217 登录故障链 (JWT_SECRET→RSA→Jackson) | 调试 | 1.5h |
| 分析镜像依赖，只拉 4 个必要镜像节省时间 | 运维 | 0.5h |
| 记录经验教训到 KNOWN_ISSUES #35 | 文档 | 0.5h |
| **当日合计** | | **8.5h** |

---

## 2026-06-28 (Sat) [待填]

| 工作内容 | 类型 | 工时 |
|---------|------|------|
| ... | ... | ... |
| **当日合计** | | **0h** |