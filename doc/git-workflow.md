# 双平台 Git 工作流 (Gitee + GitHub)

**版本**：v1.0
**更新**：2026-06-02
**适用**：云枢中台 (cloudplatform) 项目

> **范围**：本地 git 操作、双平台同步、PR 流程、凭据管理、安全教训
> **不在本文档**：CI 流水线、构建产物、Docker 镜像、自动部署 → 见 [`doc/CI_CD操作手册.md`](CI_CD操作手册.md)

---

## 概述

本项目同时推送到 **Gitee** (主) 和 **GitHub** (镜像),通过单次 `git push` 实现双平台同步。本文记录:

1. Remote 配置
2. 常见操作命令
3. 双平台 PR 流程
4. **安全教训** (2026-06-02 PAT 事件)

---

## 1. Remote 配置

### `origin` (主 remote, 双 pushurl)

```bash
$ git remote -v
github  git@github.com:fkeu520/cloudplatform.git (fetch)
github  git@github.com:fkeu520/cloudplatform.git (push)
origin  https://gitee.com/hughxu/cloudplatform.git (fetch)
origin  https://gitee.com/hughxu/cloudplatform.git (push)
origin  https://github.com/fkeu520/cloudplatform.git (push)
```

### 配置含义

| Remote | URL | 协议 | 用途 |
|--------|-----|------|------|
| `origin` (fetch) | `https://gitee.com/...` | HTTPS | 拉取用 Gitee (国内快) |
| `origin` (push) #1 | `https://gitee.com/...` | HTTPS | 推 Gitee (公开仓库, 无需 token) |
| `origin` (push) #2 | `git@github.com:fkeu520/...` | **SSH** | 推 GitHub (认证用密钥) |
| `github` (fetch) | `git@github.com:fkeu520/...` | SSH | GitHub 专属操作 (如 gh CLI) |
| `github` (push) | `git@github.com:fkeu520/...` | SSH | (备用) 仅推 GitHub |

### 为什么这样配置

- **Gitee 双协议**: 拉 + 推都走 HTTPS,公开仓库无需认证
- **GitHub SSH 推**: 避免明文 PAT,使用密钥
- **多 pushurl**: 单次 `git push origin develop` 自动同步到两边
- **`github` 别名**: 方便 `git fetch github` 单独拉 GitHub,或 `gh` CLI 操作

---

## 2. 常见操作

### 推送 (双平台同步)

```bash
# 推送 develop (自动同步 Gitee + GitHub)
git push origin develop

# 推 master (同理)
git push origin master
```

### 拉取

```bash
# 默认 fetch 用 Gitee
git fetch origin
git pull origin develop

# 拉 GitHub 单独 (对比)
git fetch github
```

### 检查远程状态

```bash
# 双平台分支对齐
git ls-remote origin develop
git ls-remote github develop

# 查看 pushurl 配置
git config --get-all remote.origin.pushurl
```

---

## 3. 双平台 PR 流程

### GitHub PR (用 gh CLI)

```bash
# 一次性认证 (用 PAT 或 SSH 转发)
export GH_TOKEN=<your_pat>

# 创建 PR
gh pr create \
    --repo fkeu520/cloudplatform \
    --base master \
    --head develop \
    --title "fix: <标题>" \
    --body-file pr-body.md

# 查看 PR
gh pr view 1 --repo fkeu520/cloudplatform

# 合并 (squash, 自动同步)
gh pr merge 1 --squash --repo fkeu520/cloudplatform
```

### Gitee PR (用 REST API)

Gitee 无官方 CLI,使用 PowerShell + REST API:

```powershell
# 1. 准备 PR body 文件
"pr-body.md" | Out-File -Encoding utf8

# 2. 调 API
$env:GITEE_TOKEN = '<your_pat>'  # 用户生成,见下文
$payload = @{
    access_token = $env:GITEE_TOKEN
    title        = 'fix: <标题>'
    body         = Get-Content "pr-body.md" -Raw
    head         = 'develop'
    base         = 'master'
}
$resp = Invoke-RestMethod `
    -Uri 'https://gitee.com/api/v5/repos/<owner>/<repo>/pulls' `
    -Method Post -Body $payload -ContentType 'application/x-www-form-urlencoded'

# 3. 销毁 token
Remove-Item Env:GITEE_TOKEN
```

详细示例见 `.githooks/pre-commit` 旁的 PowerShell 模板 (待补充) 或本次会话脚本 `create-gitee-pr.ps1`。

### 双平台 PR 一致性

- 标题、body、base、head 在两边保持一致
- 合并策略都用 **Squash Merge**,保证单 commit on master
- ⚠️ **双平台 master SHA 会不同** (各自 squash),内容相同,可接受

---

## 4. 认证与凭据

### GitHub 认证 (SSH 密钥)

- 密钥: `~/.ssh/id_ed25519` (ed25519 算法, 优于 RSA)
- 公钥: 已添加至 GitHub
- 启动 ssh-agent + 加载密钥 (开机需运行):
  ```powershell
  Set-Service ssh-agent -StartupType 'Automatic'
  Start-Service ssh-agent
  ssh-add $env:USERPROFILE\.ssh\id_ed25519
  ```
- 测试: `ssh -T git@github.com` → `Hi fkeu520! You've successfully authenticated`

### Gitee 认证 (HTTPS + 公开仓库)

- 公开仓库推送无需 token
- 私有操作 (PR 创建) 需临时 PAT,见上文 PowerShell 流程
- Gitee Token 生成: https://gitee.com/profile/personal_access_tokens

---

## 5. ⚠️ 安全教训 (2026-06-02 PAT 事件)

### 事件经过

1. 初始 remote URL 含明文 GitHub PAT: `https://fkeu520:ghp_VrxrRq...@github.com/...`
2. 该 PAT 在多次 shell 输出中暴露 (`git remote -v`)
3. 修复后,文档中**复述了完整 token 字符串**作为"问题描述"
4. 推送时 GitHub push protection (`GH013`) 拒绝,文档 commit `6808354` 被拦
5. 处理: redact token → amend commit (`acb5dca`) → force push 双平台

### 教训

1. **永远不要在 URL 中嵌入 PAT** — 用 SSH 或 credential manager
2. **不要在文档中复述完整的敏感凭据** — 用 `(ghp_ 前缀, 已 redact)` 替代
3. **GitHub push protection 是好机制** — 不要试图绕过 (`--no-verify` 是反模式)
4. **任何敏感字符串应通过环境变量** — `GH_TOKEN` / `GITEE_TOKEN`, 不入文件

### 应对 checklist (发现疑似泄露时)

- [ ] 立即停用相关凭据 (PAT 撤销、密码改)
- [ ] 检查 shell history / 日志
- [ ] 检查 commit history (`git log -p` / `git log -S`)
- [ ] 提交新版本,redact 任何泄露字符串
- [ ] 用 amend + force push 修复未推送 commit
- [ ] 评估是否需主动通知 (若是生产凭据)

---

## 6. 同步策略

### 何时双平台同步

| 操作 | 是否同步 |
|------|---------|
| 推 develop | ✅ 自动 (origin 多 pushurl) |
| 推 master | ✅ 自动 (同上) |
| 创建 PR | ❌ 手动 (两边分别创建) |
| 合并 PR | ❌ 手动 (web UI 或 API) |
| 标签 | ✅ 自动 (push 时跟随) |

### 单平台特殊操作

如需仅推 GitHub (如 Gitee 暂时不可用):
```bash
git push github develop
```

如需仅推 Gitee:
```bash
git push origin develop  # 但因为多 pushurl, 这会同时推 GitHub
# 临时方案: 临时移除 github pushurl, 推完恢复
git remote set-url --delete --push origin git@github.com:fkeu520/cloudplatform.git
git push origin develop
git remote set-url --add --push origin git@github.com:fkeu520/cloudplatform.git
```

---

## 7. Master SHA 差异 (现状)

2026-06-02 合并后:
- Gitee master: `5dc9792`
- GitHub master: `dfe8c7a9`
- 内容完全相同,只是 squash 各自独立

**无需强制统一** — SHA 差异是预期行为,不影响功能。

---

## 8. 未来改进

- [ ] 标签命名规范 (semver)
- [ ] 保护 master 分支 (Gitee 仓库设置 + GitHub branch protection)
- [ ] 双平台 mirror 自动化 (GitHub push → Gitee push webhook)
- [ ] Gitee 端 CI 镜像 (当前仅 GitHub 有 Actions, 见 `CI_CD操作手册.md`)

---

## 附录: 初始化本工作流 (新机器上手)

```bash
# 1. 生成 SSH 密钥
ssh-keygen -t ed25519 -C "<your_email>"

# 2. 添加到 ssh-agent
Set-Service ssh-agent -StartupType 'Automatic'
Start-Service ssh-agent
ssh-add $env:USERPROFILE\.ssh\id_ed25519

# 3. 公钥添加到 GitHub (https://github.com/settings/keys)

# 4. 测试
ssh -T git@github.com

# 5. 克隆项目
git clone https://gitee.com/hughxu/cloudplatform.git
cd platform

# 6. 添加 github 别名
git remote add github git@github.com:fkeu520/cloudplatform.git
git fetch github

# 7. 验证 push 双平台
git push origin develop  # 应同时推 Gitee + GitHub
```
