# 云枢中台 CI/CD 操作手册

**版本**: v1.0  
**日期**: 2026-06-01  
**仓库**: https://github.com/fkeu520/cloudplatform

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
