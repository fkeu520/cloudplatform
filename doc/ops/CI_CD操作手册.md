# 云枢中台 CI/CD 操作手册

**版本**: v1.1  
**日期**: 2026-06-02  
**仓库**: https://github.com/fkeu520/cloudplatform

---

## ⚠️ 强约束: 禁止本地构建镜像 (2026-06-02 新增)

> **用户原话**: "以后不要在 docker desktop 上构建镜像, 都通过 github 构建。此点记录到记忆文件及项目相关的文档里, 切忌勿要再犯"

**禁止操作** (Docker Desktop 镜像构建 10+ 分钟必超时):
```powershell
docker compose build              # ❌ 禁止
docker build -t xxx .              # ❌ 禁止
cd code/platform-server
mvn clean package -DskipTests      # ❌ 禁止 (本地打 jar)
cd ../..
docker compose build               # ❌ 禁止
```

**正确路径** (代码 → ghcr.io → 本地 pull):
```powershell
# 1. 改代码
# 2. 提交 (本仓库启用 pre-commit 密钥扫描 hook)
git add .
git commit -m "feat/fix: ..."
git push github develop    # 触发 GitHub Actions CI
git push origin develop    # 同步 Gitee

# 3. 等 CI 完成 (5-10 分钟), 镜像自动 push ghcr.io
#    验证: https://github.com/fkeu520/cloudplatform/actions

# 4. 本地拉取 + 重启
docker compose pull
docker compose up -d

# 5. 单独重启某个服务 (如 gateway 修复后)
docker compose up -d platform-gateway
```

**为什么禁止本地构建**:
- Docker Desktop WSL2 后端构建 Java/Spring Boot 镜像 10+ 分钟必超时
- 资源抢占影响其他开发体验
- ghcr.io 镜像已与 master/develop 分支绑定, 无需本地重建

**记录位置**:
- `C:\Users\PC\.claude\user-constraints.md` (跨会话用户约束)
- `PROGRESS.md` v6.7
- `doc/项目进度.md` v6.7

---

## 一、CI 流程概览

每次推送代码到 GitHub，会自动触发 CI 流水线：

```
代码 Push → GitHub Actions 触发
                 │
         ┌────────┼────────┐
         ▼        ▼        ▼
     backend  frontend  frontend
     (Maven)  -admin    -ops
               (npm)    (npm)
         │        │        │
         └────────┼────────┘
                  ▼
             构建产物
           (Artifacts)
```

### 流水线文件位置

```
.github/workflows/ci.yml    ← CI 定义文件
```

---

## 二、触发条件

| 事件 | 触发分支 | 说明 |
|------|---------|------|
| `git push` | `develop`、`master`、`feature/*` | 日常开发推送自动触发 |
| Pull Request | `develop` → `master` | 合并请求时自动验证 |

---

## 三、CI 任务说明

### 1. backend — 后端编译

```yaml
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - Setup JDK 21 (Temurin)
      - Maven 编译（跳过测试）
      - 上传 6 个 JAR 包作为构建产物
```

**产物**（保存 3 天）：

| 文件 | 说明 |
|------|------|
| `platform-user/target/*.jar` | 用户服务 |
| `platform-auth/target/*.jar` | 认证服务 |
| `platform-gateway/target/*.jar` | API 网关 |
| `platform-workflow/target/*.jar` | 工作流引擎 |
| `platform-message/target/*.jar` | 消息中心 |
| `platform-ops/target/*.jar` | 运营管理 |

### 2. frontend-admin — 管理后台编译

```yaml
jobs:
  frontend-admin:
    runs-on: ubuntu-latest
    steps:
      - Setup Node 20
      - npm install → npm run build
      - 上传 dist 目录
```

### 3. frontend-ops — 运营后台编译

```yaml
jobs:
  frontend-ops:
    runs-on: ubuntu-latest
    steps:
      - Setup Node 20
      - npm install → npm run build
      - 上传 dist 目录
```

三个任务**并行执行**，互不依赖。

---

## 四、查看 CI 状态

### 方式一：GitHub 网页

```
1. 打开 https://github.com/fkeu520/cloudplatform
2. 点击顶部 Actions 标签
3. 左侧选择 CI workflow
4. 点击某个运行记录查看详情
```

### 方式二：Pull Request 页面

提交 PR 时，CI 状态会自动显示在 PR 底部，绿色 ✅ 表示通过，红色 ❌ 表示失败。

---

## 五、CI 失败排查

### 常见失败原因

| 现象 | 原因 | 解决 |
|------|------|------|
| Maven 编译失败 | Java 语法错误或依赖下载失败 | 本地 `mvn clean package` 先验证 |
| npm install 失败 | 依赖包下载超时 | 检查 package-lock.json，重试 |
| 测试失败 | 单元测试断言不通过 | 本地运行测试定位问题 |
| **启动时 Bean 缺失 (L2 阶段)** | AutoConfiguration 误排除 | 查看启动日志 `Negative matches`, 移除对应 exclude |

### 本地复现

```bash
# 后端
cd code/platform-server
mvn clean package -DskipTests

# 前端
cd code/platform-admin
npm install --legacy-peer-deps && npm run build
cd ../platform-ops-admin
npm install && npm run build
```

### L2 优化相关的 CI 注意事项

实施 [`中台建设中长期规划.md`](../中台建设中长期规划.md) 第三章时：

- **Phase A（公共调优）**: CI 编译通过即代表成功，无需改 CI 配置
- **Phase B（行为开关化）**: CI 编译通过即代表成功；新增的 `@ConditionalOnProperty` 类需加单元测试
- **性能基准**: 部署后用 [`性能基准.md`](../性能基准.md) 模板记录 docker stats 采样数据

### P0 紧急项的 CI 注意事项

[`中台建设中长期规划.md` 第五章](../中台建设中长期规划.md#五3-个紧急-p0-项详细方案) 包含 P0-1（多租户拦截器）、P0-2（数据权限）、P1-1（链路追踪）。CI 必须包含：

- **多租户拦截器**: PR 必须包含"两租户互不可见"集成测试
- **数据权限 data_scope**: 必须有"销售员只能看自己"的测试用例
- **链路追踪**: smoke test 验证 traceId 在所有服务传递

---

## 六、扩展：添加 Docker 镜像构建

> 当前 CI 只做编译和产物保存，如需自动构建 Docker 镜像，按以下步骤配置：

### 6.1 准备 Docker Hub / 阿里云 ACR 账号

注册容器镜像仓库账号，创建命名空间和仓库。

### 6.2 添加仓库 Secrets

在 GitHub 仓库设置中添加：

```
Settings → Secrets and variables → Actions → New repository secret
```

| Secret 名称 | 值 |
|-------------|-----|
| `DOCKER_REGISTRY` | `registry.cn-hangzhou.aliyuncs.com`（以阿里云为例） |
| `DOCKER_USERNAME` | 镜像仓库用户名 |
| `DOCKER_PASSWORD` | 镜像仓库密码/Token |

### 6.3 在 ci.yml 末尾添加 Docker job

```yaml
  docker:
    needs: [backend, frontend-admin, frontend-ops]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master'
    steps:
      - uses: actions/checkout@v4
      
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          path: build-output
          
      - name: Login to Docker Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ secrets.DOCKER_REGISTRY }}
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
          
      - name: Build & Push user image
        run: |
          docker build -f Dockerfile \
            -t ${{ secrets.DOCKER_REGISTRY }}/platform/user:${{ github.sha }} \
            -t ${{ secrets.DOCKER_REGISTRY }}/platform/user:latest .
          docker push --all-tags ${{ secrets.DOCKER_REGISTRY }}/platform/user
        working-directory: code/platform-server
        
      # ... 其他服务同理
```

---

## 七、扩展：添加自动部署

### 方案一：SSH 部署到自建服务器

```yaml
  deploy:
    needs: docker
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master'
    steps:
      - name: SSH Deploy
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_KEY }}
          script: |
            cd /opt/platform
            docker compose pull
            docker compose up -d
```

### 方案二：部署到 K8s

```yaml
  deploy-k8s:
    needs: docker
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master'
    steps:
      - name: Deploy to K8s
        run: |
          kubectl set image deployment/user user=${{ secrets.DOCKER_REGISTRY }}/platform/user:${{ github.sha }}
```

---

## 八、分支策略

```
master        → 生产环境，只从 develop 合并
develop       → 日常开发，推送自动触发 CI
feature/*     → 功能分支，推送自动触发 CI
```

> 本地 git 协作（双平台推送 / PR 创建 / 凭据管理 / 安全教训）见 [`doc/git-workflow.md`](git-workflow.md)

---

## 九、快速参考

### 手动触发 CI

在 GitHub Actions 页面，选择 CI workflow，点击 **Run workflow** 手动触发。

### 下载构建产物

1. 打开对应 CI 运行记录
2. 底部 **Artifacts** 区域
3. 点击下载 `backend-jars`、`frontend-admin-dist`、`frontend-ops-dist`

---

## 十、相关文档

| 文档 | 说明 |
|------|------|
| [`中台建设中长期规划.md`](../中台建设中长期规划.md) | **主规划文档**：性能优化 + 中台 5 层蓝图 + 3 紧急 P0 项 |
| [`部署指南.md`](../部署指南.md) | 单机/集群部署流程 |
| [`服务器配置清单.md`](../服务器配置清单.md) | 硬件配置建议 |
| [`性能基准.md`](../性能基准.md) | 优化前后对比数据模板 |
| [`环境搭建指引.md`](../环境搭建指引.md) | 开发环境搭建 |
