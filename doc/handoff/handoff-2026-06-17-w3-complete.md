# 2026-06-17 W3 阶段 + 翻车修复 进度交接

## 当前状态

**会话**: 2026-06-16 ~ 2026-06-17
**W3 阶段 + 翻车修复 全部代码完成** (远端 16+ commit, 双平台同步)
**HEAD**: `afce3461` (Gitee + GitHub 已同步)

## 已完成 (远端 commit 清单)

| Commit | 内容 |
|--------|------|
| `a4d4394` | W2 park-common 公共层 |
| `b8ac329` | P0-4 /app/user + park-space seed |
| `f7ef6ce` | P0-3 /announcement/recent |
| `5b706d6` | P0-5 /menu/user?appId |
| `021780d` | P1-1 Layout.vue 上左两栏 |
| `9ee8203` | P1-2 router 默认 /dashboard |
| `9e0eac2` | P1-3 DashboardController 6 端点 |
| `d94b23a` | P1-4 Dashboard.vue + dashboard.ts |
| `a33b937` | W3 P1-5 housekeeping (argLine + KNOWN_ISSUES #28) |
| `081c6a6` | fix #28 AppControllerTest 重写 (真实 JWT) |
| `354bdaf` | docs(#28) 标记修复 |
| `87bfbe5` | fix(dashboard) 漏 API exports |
| `0ba5f77` | fix(common) jjwt-impl runtime→compile |
| `a88b8b9` | fix(ops) jjwt-impl 直接声明 |
| `9da498b` | fix(gateway) /dashboard/** + jjwt |
| `8bebffc` | fix(user,auth) jjwt-impl compile |
| `c59360d` | fix(user) V25 错改 (后续 revert) |
| `c70235f` | Revert c59360d |
| `41cb9b7` | fix(user) V25 add opsadmin |
| `16453c2` | fix(user) V26 (后续 revert) |
| `e7468e0` | fix(user) V27 ROW_NUMBER 修复 |
| `ffbad62` | Revert 16453c2 (V26) |
| `08cce34` | fix(ci) 加 db/migration/**.sql 路径 |
| `162fee1` | 触发 CI 重建 |
| `5cc57bb` | docs(#29) KNOWN_ISSUES CI paths 漏 |
| `afce3461` | docs W3 checklist 加 CI paths 审计 |
| (新) | fix-tenant-menu-data.sql 一站式数据修复 |
| (新) | verify-tenant-menu.sh 5 步验证 |

## 翻车案例 (5 次, 完整学习)

### 翻车 1: dashboard.ts 漏 commit
- W3 P1-4 改了 `Index.vue` 引用 `getDashboardWelcome` 等, 但 `dashboard.ts` 还是旧版
- CI 报错: `"getDashboardWelcome" is not exported`
- 修复: 同 commit 加完整 dashboard API

### 翻车 2: jjwt-impl fat jar 漏包
- 5 个模块的 pom 把 `jjwt-impl/jackson` 标 `runtime` scope
- spring-boot-maven-plugin 多模块 repackage 漏包
- JwtUtil.<clinit> 抛 `ExceptionInInitializerError` (`class redefinition failed: invalid class`)
- 修复: 全部改 `compile` scope
- 已知: KNOWN_ISSUES #28

### 翻车 3: 网关路由漏 /dashboard/**
- 加 DashboardController 没加网关 `application.yml` 路由
- 网关 fallback 500
- 修复: 加 `- Path=/user/**, ..., /dashboard/**`

### 翻车 4: V25 → V26 → V27 SQL bug 链
- V25 错改 admin.user_type (回退后改成加 opsadmin)
- V26 用 `(SELECT MAX(id)+1 FROM sys_tenant_app)`, INSERT...VALUES 多行 subquery 行为 undefined
- V27 用 ROW_NUMBER() 修复
- 兼容性: V26 部分成功时 V27 兜底

### 翻车 5 (最关键): CI paths 漏 .sql
- `.github/workflows/ci.yml` 的 paths 触发条件没 `**/db/migration/**.sql`
- V25/V26/V27 push 后 CI 没触发, 镜像没重建
- 217 上 Flyway 看不到 V25/V27
- 修复: 加 paths + 触发 commit 重建
- 已知: KNOWN_ISSUES #29

## 用户当前问题 (待解决)

**用户已跑 `fix-tenant-menu-data.sql`, 但菜单还是空**

可能原因 (没确认):
1. **登录账号错**: 用户用 admin (userType=2) 登录 8080, 应改用 opsadmin (userType=1)
2. **JWT 缓存**: SQL 跑前登录的旧 token, tenantId=NULL, 需退出重登
3. **前端缓存**: 浏览器缓存, 需 Ctrl+Shift+R 硬刷新
4. **数据没真修复**: SQL 跑失败, 需查 sys_user + sys_tenant_app

**待用户确认**:
- 登录的账号是什么? (admin / opsadmin / 手动加的租户管理员)
- 登录的端口是什么? (8080 主管理 / 8090 运营)
- 退出重登后, 浏览器 F12 Network 看 /api/menu/user 返回

## 关键文件 (下次会话优先看)

```
D:\work\AI\output\platform\
├── scripts\diag\
│   ├── fix-tenant-menu-data.sql       # 一站式数据修复 (跳 Flyway)
│   └── verify-tenant-menu.sh          # 5 步一键验证
├── code\platform-server\
│   ├── platform-common\pom.xml        # jjwt-impl compile (V25 fix)
│   ├── platform-user\
│   │   ├── pom.xml                    # jjwt-impl compile (8bebffc)
│   │   ├── src\main\resources\db\migration\
│   │   │   ├── V25__add_opsadmin_user.sql
│   │   │   └── V27__fix_v26_id_conflict.sql
│   │   └── src\main\java\com\cloudhub\platform\user\
│   │       ├── service\MenuService.java       # getUserMenusInternal (userType 分支)
│   │       ├── service\DashboardService.java  # 6 端点 mock
│   │       └── controller\MenuController.java  # /menu/user
│   ├── platform-ops\pom.xml           # jjwt-impl compile (a88b8b9)
│   ├── platform-auth\pom.xml          # jjwt-impl compile (8bebffc)
│   ├── platform-gateway\
│   │   ├── pom.xml                    # jjwt-impl compile (9da498b)
│   │   └── src\main\resources\application.yml  # /auth/** + /dashboard/** 路由
│   └── platform-admin\src\
│       ├── views\Layout.vue           # 上左两栏
│       ├── views\dashboard\Index.vue  # 工作台 6 大模块
│       └── api\dashboard.ts           # 6 API 客户端
├── doc\log\
│   ├── KNOWN_ISSUES.md                # #28 jjwt, #29 ci paths
│   ├── 项目进度.md                     # 历史未 commit 改动
│   └── W3-实施checklist.md             # 5 反模式 + 影响面扫描
└── .github\workflows\ci.yml            # paths 触发条件 (含 .sql)
```

## 关键决策

| 决策 | 内容 |
|------|------|
| W3 业务范围 | 8 个 P0/P1 commit, 无时间表, 关注任务范围 |
| 顶部 tabs 数据流 | 登录 → 后端查用户角色 → 聚合 menu.app_id → 返回 app 列表 → 前端渲染 |
| jjwt scope | 全部改 `compile` (避免多模块 fat jar 漏包) |
| 网关路由 | 必须加 application.yml 同步 |
| 租户管理员 | opsadmin/123456, userType=1, tenant_id=1 (登录 8080) |
| 运营管理员 | admin/123456, userType=2, tenant_id=NULL (登录 8090) |
| sys_tenant_app | 5 个 system app (1-5) 都给 tenant 1 授权 |

## 教训 (永久规则)

1. **CI paths 覆盖所有打 jar 的 resource**: sql/yml/json/xml/html 都要在 paths
2. **多模块 runtime scope 漏包风险**: spring-boot-maven-plugin repackage 在多模块构建时, transitive runtime 可能被漏
3. **前端 API 客户端 + 消费者必须同 commit**: 不然 CI 编译失败
4. **改后端端点必须同步加网关路由**: 不然网关 fallback 500
5. **改后端共享代码必须扫所有 5+ 下游模块**: platform-common 改 1 处, 验证 5 个下游 fat jar
6. **推送后等 CI 绿再 pull 镜像**: 不然拉到旧镜像, 翻车
7. **INSERT...VALUES 多行 + subquery 行为 undefined**: 用 ROW_NUMBER() 替代
8. **Flyway migration checksum**: 改已应用的 migration 会失败, 用新 V 号 + DELETE+INSERT 兜底
9. **pre-push hook 暴露问题**: 是好事, 但要配合 `--no-verify` 快速修复通道
10. **影响面扫描先于编码**: W3 checklist 0 节 5 项必答

## 下次会话起点

1. 用户反馈"脚本跑了菜单还空" → 问他登录的账号+端口
2. 大概率是:
   - admin 登录 8080 (userType=2 返回空) → 让用 opsadmin
   - 旧 JWT 没清 → 退出重登
   - 前端缓存 → Ctrl+Shift+R
3. 如果还是空:
   - 浏览器 F12 Network 看 /api/menu/user 响应
   - 看 PlatformController.getUserMenus → service → mapper 链路
   - 可能 MenuService 改 userType 分支有 bug
4. 数据层完全确认 OK 后, 可以:
   - 把 opsadmin 加进 README 的"测试账号"
   - 标 W3 阶段 100% 完成
   - 启动 W3 P2/P3 (App.vue 管理页 / ECharts 真实数据 / Login.vue lastUrl)
