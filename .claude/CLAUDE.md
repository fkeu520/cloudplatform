# 云枢中台 - 项目级 AI 规则

> AI 助手（Claude/Codex 等）在此项目工作时必须遵守的规则。
> 跨会话生效。

---

## 1. 严格代码修改流程（2026-06-24 强制）

任何代码调整必须按以下顺序执行，缺一步禁止 commit：

| 步骤 | 动作 | 验证方式 |
|------|------|----------|
| 1 | 确定调整范围（文件清单 + 影响功能） | 与用户对齐 |
| 2 | 写/改代码（含对应测试） | 完整 diff |
| 3 | 跑相关测试 | unit + integration + 已存在测试 |
| 4 | 确认编译通过 | `mvn compile` / `mvn test-compile` / `npm run build` / `vue-tsc` |
| 5 | commit + push | **前 4 步全完成才能 commit** |

### 强禁止
- 改完直接 commit，不跑测试
- commit 但不验证编译
- 跑通一半就说"应该没问题"
- 用 `mvn -DskipTests` / `maven.test.skip=true` 蒙混
- 用 `target/classes` 缓存假装编译成功
- "本地跑不通就靠 CI 验证"（CI 是兜底，不是替代）

### 本地编译失败的标准应对
1. 排查根因（Lombok / Java 版本 / 依赖 / 类路径）
2. 修到本地能跑通
3. 然后才能 commit
4. **不允许**"本地跑不通就 push 给 CI 测" — 这把 CI 当挡箭牌

### 例外
- 紧急 hotfix：可缩短流程，但事后必须补全测试 + commit message 写明原因
- 纯文档（.md/.txt）修改：跳过编译验证
- 纯注释/docstring：跳过编译验证

---

## 2. 严格 3 步 Git + 部署流程（2026-06-22 强制，effective）

```
Step 1: Windows git commit   ← 本地改完，commit
Step 2: git push + CI        ← 等用户 push 批准 + CI 5-10 min 绿
Step 3: SSH 217 部署         ← 等用户 "go" 信号才 SSH 部署
```

**禁止**:
- ❌ 改完不 commit 直接 push（会带 uncommitted 变更）
- ❌ commit 后不等用户批准就 push
- ❌ push 后不等 CI 就 SSH 部署
- ❌ CI 失败仍 SSH 部署（"CI 红就当没看见"）
- ❌ 声明部署完成而没做 smoke test

**"DONE" 标准定义**（所有必须满足）:
- 代码已 commit
- push 已批准
- CI 已通过（5-10 min 跑完 + 绿）
- Ubuntu 217 已 `git pull` + `docker compose pull` + `up -d`
- smoke test 已通过

---

## 3. 工作目录与环境

- 项目根：`D:\work\AI\output\platform`
- 部署环境：Ubuntu 26.04 @ 192.168.0.217（SSH: `hugh@192.168.0.217`）
- 部署目录：`/opt/platform`（容器化）
- 数据库 MySQL：root=root123456，业务账号 platform/platform123
- 镜像：ghcr.io/fkeu520/cloudplatform/*（**禁止本地构建**）

---

## 4. 工具与命令规范

- 优先用 `rtk` 前缀的 token 优化命令（见 `C:\Users\PC\.claude\CLAUDE.md`）
- mvn 用 `-o` offline 模式加速（依赖已下载）
- 命名空间强制 `com.cloudhub.platform.*`（见 `doc/spec/编码规范.md` §1.1 + §1.7）
- 提交前用 `git check-ignore -v <file>` 排查被 .gitignore 误挡

---

## 5. 跨会话记忆

- `doc/handoff/handoff-YYYY-MM-DD.md` — 每次会话结束写交接
- `doc/log/项目进度.md` — 变更日志
- `doc/log/KNOWN_ISSUES.md` — 已知问题登记（修完当日补）
- 状态用图标：🔴 待修复 / 🟡 待跟进 / 🟢 已解决 / ⚫ 不修复

---

## 6. 命名空间与代码规范

- 业务代码包名统一 `com.cloudhub.platform.<module>.*`
- 禁止出现历史私有包（`cn.flyrise.*`, `cn.hutool.*` 等）
- Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5.7
- 前端 Vue 3 + Composition API + Element Plus + Vite + TypeScript

---

## 7. SuperMemory 维护

每次完成重要任务后，调用 `/context-memory` skill 保存关键信息到 supermemory（项目级 + 用户级）。

记录内容：
- 完成事项清单（含 commit hash）
- 关键决策（用户拍板）
- 当前状态（部署在哪 / 部署时间）
- 待办事项（按优先级）
- 引用文档（doc/ 路径）
- 技术坑（CI 失败教训 / 环境配置 / 依赖版本）
- 凭据（数据库/SSH/镜像仓库）

---

**最后更新**: 2026-06-24（严格代码流程约束）
**维护者**: AI 助手
**跨会话生效**: 是（commit 进 git，所有协作者可见）
