# W3+ 多服务功能实施 checklist

> **背景**: 2026-06-16 W3 阶段中, 我作为 Sisyphus 模式 3 次未一次性规划完整, 导致 3 个连环问题:
>
> 1. **W3 P1-4 (d94b23a)**: commit `Index.vue` 但 `dashboard.ts` 旧版未更新, CI 构建失败
> 2. **W3 P0-4 (b8ac329) + JWT 类初始化**: Docker fat jar 缺 `jjwt-impl/jackson`, 改 `platform-common` 的 pom scope 没用, 改 `platform-ops` 直接声明才修
> 3. **W3 P1-3 (9e0eac2)**: 加 `DashboardController` 6 端点, 但网关 `application.yml` 路由表没声明 `/dashboard/**`, 网关 fallback 触发 500
>
> 共同反模式: **实施前没做跨服务影响面扫描, 实施后没做 fat jar 内容验证**。本文是事后总结的硬性流程, 今后 W3+ 任何跨多服务 (backend + frontend + gateway) 的功能, 必走此 checklist。

---

## 0. 实施前: 跨服务影响面扫描 (5 项必答)

| # | 问题 | 影响点 | 答 (举例 W3 P1-3 dashboard) |
|---|------|--------|---------------------------|
| 0.1 | **新后端端点属于哪个微服务?** | 该模块 pom + 启动类 | platform-user |
| 0.2 | **新端点要进网关路由表吗?** | `platform-gateway/src/main/resources/application.yml` 第 83-152 行 | 是 — 加 `/dashboard/**` 到 platform-user 路由 |
| 0.3 | **新端点用了哪些共享代码 (platform-common 的类)?** | 该共享类的所有使用者都需在 fat jar | `JwtUtil` → 所有用 `JwtUtil` 的服务 (user, ops, auth, gateway, message, workflow) 都要 `jjwt-impl` |
| 0.4 | **新端点有数据库变更吗?** | Flyway 脚本 (`V{N}__*.sql`) | 是 — V11 + V12 已有, 后续如新增必须写脚本 |
| 0.5 | **前端 import 了哪些新模块?** | API 客户端文件 + 类型定义 | `src/api/dashboard.ts` 必须和 `Index.vue` 一起 commit |

**答不全 → 不开始编码**。每个问题都要在 `task_plan.md` 里写明。

---

## 1. 实施中: 模块依赖传递扫描 (尤其涉及 platform-common)

### 1.1 共享代码 (platform-common) 修改后, 必做:

```bash
# 列出 platform-common 的所有 runtime / compile 依赖
mvn -pl platform-common dependency:list -DincludeScope=runtime

# 列出所有使用 platform-common 的模块
grep -l "platform-common" code/platform-server/*/pom.xml
```

**重点关注**:
- `jjwt-api` 是 compile,  `jjwt-impl/jackson` 是 runtime → 存在多模块 fat jar 漏包风险
- 任何 `runtime` scope 依赖, 在多模块 `mvn package` 时, spring-boot-maven-plugin 的 `repackage` 可能漏包

**对应用 `runtime` 依赖的处理**:
- 选项 A: 升级到 `compile` (接受传递依赖图变大) — 推荐
- 选项 B: 每个下游模块直接声明 (代码重复, 但保险)
- 选项 C: 在父 pom `dependencyManagement` 用 `scope=compile` 覆盖 (推荐 — 集中)

**W3 阶段已采用 C**: 父 pom 改 `jjwt-impl/jackson` 为 `compile`, 同时 `platform-ops` 直接声明 (双保险)。

### 1.2 共享代码修改的 6 步验证:

```bash
# 1. 编译平台 common
mvn -pl platform-common install -DskipTests -o

# 2. 编译所有下游模块 (验证编译过)
mvn -pl platform-user,platform-ops,platform-auth,platform-gateway compile -o

# 3. **关键**: 检查每个下游模块的 fat jar 里有没有 platform-common 的 runtime 依赖
mvn -pl platform-user package -DskipTests
mvn -pl platform-ops package -DskipTests
mvn -pl platform-auth package -DskipTests
mvn -pl platform-gateway package -DskipTests

# 4. **关键**: 验 fat jar 内容
for m in platform-user platform-ops platform-auth platform-gateway; do
  jar tf code/platform-server/$m/target/$m-1.0.0-SNAPSHOT.jar | grep -E "jjwt-impl|jjwt-jackson"
done
```

**预期**: 4 个模块都同时看到 `jjwt-impl` 和 `jjwt-jackson`。

### 1.3 新增共享代码的 5 步流程:

1. `platform-common` 加新类
2. `mvn -pl platform-common install -DskipTests -o` 装到本地
3. 下游模块用 `import` 调用
4. **5 个下游模块的 pom 都要确认**: `grep "platform-common" code/platform-server/*/pom.xml`
5. 5 个下游模块都跑一次 `mvn -pl {module} clean test` (至少编译过)

---

## 2. 实施中: 网关路由一致性 (易漏点)

### 2.1 加新后端端点必做:

```bash
# 1. 看现有路由
grep -A 3 "id: platform-" code/platform-server/platform-gateway/src/main/resources/application.yml | head -60

# 2. 新端点 path 模式 → 确认归属服务
# 例: /dashboard/** → platform-user
#     /tenant-app/** → platform-ops
#     /workflow/** → platform-workflow
```

**新端点 path 必须在某一行 `Path=` 的路径列表里**, 否则网关 fallback 500。

### 2.2 加新网关路由的 3 步流程:

```yaml
# 在 application.yml 的 routes: 块里, 找到对应服务的路径列表
# 例: dashboard 端点加到 platform-user
- id: platform-user
  uri: lb://platform-user
  predicates:
    - Path=/user/**, /role/**, /menu/**, /dict/**, /config/**, /org/**, /dept/**, /post/**, /oper-log/**, /login-log/**, /dashboard/**  # ← 加上
```

3 步:
1. 修改 `application.yml`
2. **关键**: 同一 commit 必须包含 gateway pom (如果有改) + application.yml (路由改)
3. 推到 CI 后, 确认 CI 用最新 gateway 镜像

### 2.3 网关验证:

```bash
# 启动后用 curl 直接打网关 (绕过前端 nginx)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/dashboard/welcome

# 预期: 返回正常 JSON, 不是 500
```

---

## 3. 实施中: 前端 API 客户端与消费者一致 (W3 P1-4 翻车点)

### 3.1 规则: **API 客户端文件 + 消费者 必须在同一 commit**

```typescript
// src/api/dashboard.ts - 必须包含所有 export 函数
export function getDashboardWelcome() { ... }
export function getDashboardTodos(limit: number) { ... }
// ... 其他 6 个
```

```vue
<!-- src/views/dashboard/Index.vue - 同一 commit -->
<script setup>
import { getDashboardWelcome, getDashboardTodos, ... } from '@/api/dashboard'
</script>
```

**反例 (W3 P1-4 翻车)**: `Index.vue` 改了 import, 但 `dashboard.ts` 还是旧版 → CI 编译失败 "getDashboardWelcome is not exported"。

### 3.2 前端 commit 前 3 步验证:

```bash
# 1. TypeScript 编译
cd code/platform-admin
npm run build

# 2. 关键: 检查所有 import 的函数 / 类型是否都有 export
grep -E "^export (function|interface|type)" src/api/dashboard.ts

# 3. 检查消费者 import 的函数名是否都在导出列表里
grep -E "import.*from.*dashboard" src/views/dashboard/Index.vue
```

**预期**: 消费者 import 的函数, 全部都能在 `api/dashboard.ts` 的 export 里找到。

### 3.3 前端类型同步:

```typescript
// api/dashboard.ts 定义类型
export interface DashboardWelcome { ... }

// 消费者 (Index.vue) 用同一份类型
import type { DashboardWelcome } from '@/api/dashboard'
const welcome = reactive<DashboardWelcome>({ ... })
```

**类型必须定义一次, 多处 import** — 不要在 `Index.vue` 里再写一遍。

---

## 4. 实施后: 提交前本地 5 步验证

```bash
# 1. 后端编译
mvn -pl platform-user,platform-ops,platform-auth,platform-gateway clean package -DskipTests -o

# 2. 后端测试 (受影响的模块)
mvn -pl platform-user,platform-ops clean test -o

# 3. 前端编译
cd code/platform-admin
npm run build

# 4. **关键**: 检查每个模块 fat jar 关键依赖
for m in platform-user platform-ops platform-auth platform-gateway; do
  jar tf code/platform-server/$m/target/$m-1.0.0-SNAPSHOT.jar | grep -E "jjwt|platform-common"
done

# 5. lsp_diagnostics (TypeScript/Java 都跑)
```

**全绿 → push**。有红 → 修, 不推 `--no-verify` (除非记录 KNOWN_ISSUES)。

---

## 5. 推送后: CI 监控 + 217 部署前验证

### 5.1 CI 触发条件确认:

`.github/workflows/ci.yml` 触发路径 (line 11-46):

```yaml
paths:
  - 'code/platform-server/**.java'
  - 'code/platform-server/**/pom.xml'
  - 'code/platform-server/**/application.yml'  # 网关路由改这个必触发
  - 'code/platform-admin/src/**'                # 前端
  - ...
```

**🟢 2026-06-17 教训**: 任何打到 jar 里的 resource 都必须在 paths 覆盖, 包括:
- `**/*.sql` (Flyway migration, KNOWN_ISSUES #29)
- `**/*.xml` (MyBatis mapper)
- `**/*.yml` / `**/*.properties` (应用配置)
- `**/*.json` (i18n / 配置)
- `**/*.html` (模板)

**审计命令**:
```bash
# 查 src/main/resources 下所有文件类型
find code/platform-server/*/src/main/resources -type f | sed 's/.*\.//' | sort -u
# 对照 ci.yml paths 列表, 缺的扩展名要补
```

如果改了 ci.yml paths 漏的文件类型 → 必须:
1. 修 ci.yml 加 path
2. push 一个 trigger commit 命中新 path
3. 看 GitHub Actions 变绿, 镜像 rebuild
4. 217 拉新镜像后, Flyway/Mapper/Config 才生效
```

**确认 commit 改的文件至少匹配一个 path**, 否则 CI 不跑, 镜像不更新 (2026-06-12 翻车点)。

### 5.2 等 CI 完成 (5-15 分钟):

```bash
# 在 GitHub Actions 页面看最后一次 workflow run 变绿
# 不要在 CI 没完成前在 217 上 pull
```

### 5.3 217 上 pull 后 3 步验证:

```bash
# 1. 镜像时间 > CI 完成时间
docker inspect ghcr.io/fkeu520/cloudplatform/platform-user:latest --format '{{.Created}}'

# 2. 关键: 检查 fat jar 关键依赖
docker run --rm --entrypoint sh ghcr.io/fkeu520/cloudplatform/platform-user:latest -c "jar tf /app.jar | grep jjwt"

# 3. 关键: 测端点
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/dashboard/welcome
```

**预期**: 3 个 jjwt jar 都在 + dashboard 端点返回正常 JSON。

### 5.4 端到端验证:

```bash
# 浏览器打开 http://192.168.0.142:8080
# - 登录
# - 工作台 6 大模块加载
# - 顶部 tabs 切换 app
# - 左侧菜单切换
```

---

## 6. 反模式 (DO NOT)

| 反模式 | 后果 | 替代 |
|--------|------|------|
| ❌ Commit `Index.vue` 改 import, 但不 commit `api/dashboard.ts` | CI 失败, "X is not exported" | 同 commit |
| ❌ 改 `platform-common` 加新类, 不验证下游 5 个模块都还能编 | 5 个模块其中某 1 个会编不过 | mvn install + 5 个下游 mvn compile |
| ❌ 改 jjwt scope 为 compile, 假设所有下游自动继承 | spring-boot repackage 漏包 (W3 翻车) | **验 fat jar 内容** |
| ❌ 加新后端端点, 不加网关路由 | 网关 fallback 500 (W3 翻车) | 同步改 `application.yml` |
| ❌ 不等 CI 跑完就 pull | 拉到的还是旧镜像 (2026-06-12 翻车) | 等 CI 绿 |
| ❌ 改完直接 `git push --no-verify` 绕 hook | 掩盖测试失败 | 测试先修, 实在修不了再 KNOWN_ISSUES |
| ❌ 一个 commit 跨 3 个不相关功能 | 难回滚, 难 code review | 一个逻辑单元一个 commit |

---

## 7. W3 阶段已完成的工作 (本 checklist 的应用对象)

### 7.1 已完成且通过 CI 验证:

- W1: csyh import 扫描 (1 commit)
- W2: park-common 公共层 (1 commit)
- W3 P0-3: sys_announcement + /announcement/recent (1 commit)
- W3 P0-4: /app/user + park-space seed (1 commit)
- W3 P0-5: /menu/user?appId (1 commit)
- W3 P1-1: Layout.vue 上左两栏 (1 commit)
- W3 P1-2: router 默认 /dashboard (1 commit)
- W3 P1-3: DashboardController (1 commit) — **漏加网关路由 (P0-3 翻车)**
- W3 P1-4: Dashboard.vue + dashboard.ts (1 commit) — **dashboard.ts 漏 commit (P1-4 翻车)**
- W3 P1-5: KNOWN_ISSUES #28 修复 (1 commit) — **jjwt-impl scope 漏改全下游**

### 7.2 仍待 P0/P1 后续:

- P2: App.vue / Menu.vue / Announcement.vue 管理后台 UI
- P3: Login.vue lastUrl + ECharts 真实数据
- M5: 数据权限 (M5 P0-2 已完成 PR1-4)
- park-* 业务接入: park-space, park-contract (等 W3+ 启动)

---

## 8. 工具命令速查 (用到时直接 copy)

```bash
# 看 platform-common 的所有依赖
mvn -pl platform-common dependency:list -DincludeScope=runtime -o

# 看平台 server 依赖树
mvn -pl platform-server dependency:tree -o

# 验 fat jar 内容
jar tf code/platform-server/{module}/target/{module}-1.0.0-SNAPSHOT.jar | grep {lib}

# 前端 type check
cd code/platform-admin && npm run type-check

# 看 CI 触发条件
cat .github/workflows/ci.yml | grep -A 2 'paths:'

# 验 217 镜像
docker inspect ghcr.io/fkeu520/cloudplatform/{module}:latest --format '{{.Created}}'
docker run --rm --entrypoint sh ghcr.io/fkeu520/cloudplatform/{module}:latest -c "jar tf /app.jar | grep {lib}"
```

---

> **最后更新**: 2026-06-16, 由 W3 阶段 3 次连环翻车总结
> **维护人**: Sisyphus (Sonnet 4.5+)
> **未来 PR**: 任何跨多服务 (backend × N + frontend + gateway) 的功能, 必读此 checklist
