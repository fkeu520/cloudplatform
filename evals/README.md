# evals/ — 云枢中台 AI 原生 SDLC 评估套件（Wave-1）

## 什么是这里的"评估"

本目录里的**每一个 eval 都是一条可复现的回归测试，守护一次真实发生过的失败**。

它不是新的功能验收、不是抽象的"评分维度"，而是：

- 某次事故 / 某次 bug 修复后写下来的回归锁（P0 测试）
- 现在被 AI 代理在每次改动后作为**可执行的验收标准**重新跑一遍
- 全绿才允许该改动进入下一环节（merge / 部署）

设计意图来自 `sdlc-implementation-playbook.md §8.5`：
> 评估套件（`evals/`）—— 需要一批真实任务；可复用现成的 P0 回归测试作为起点

所以我们不重写测试，而是**索引**已经在 `code/platform-kefu/tests/` 里的 12 个 `test_*_p0.py` 套件，
把它们组织成一个带 manifest（`cases.yaml`）+ 可执行脚本（`run.sh`）的回归网。

---

## 目录结构

```
evals/
├── README.md       ← 本文件
├── cases.yaml      ← manifest：12 个 P0 套件（id / description / suite / origin / severity）
└── run.sh          ← POSIX bash 入口：跑全部 eval，打印摘要，任何失败则 exit 非零
```

实际的测试代码不放在这里，而是直接引用 `code/platform-kefu/tests/` 下已有的文件。
`cases.yaml` 的 `suite` 字段就是 pytest 能直接定位到的目标文件（相对 `code/platform-kefu/` 的路径）。

---

## 本地跑

```bash
# 在项目根目录
bash evals/run.sh
```

- 在 Windows 上用 Git Bash 可直接跑；在 Linux/macOS 上原生 bash 可跑。
- 依赖：Python 3 + pytest + 客服后端的运行依赖（`aiomysql`、`faiss`、`fastapi` 等），
  与 `code/platform-kefu` 的正常开发依赖一致，无需额外安装。
- 任何一个 suite 失败，`run.sh` 以非零码退出；全部通过时退出 0。

单独跑某个 P0 文件（不进 evals 框架）：

```bash
cd code/platform-kefu
python -m pytest tests/test_db_pool_release_p0.py -v
```

---

## 怎么加一条 case

1. 先有真实事故或 bug 修复（不允许"为了加 case 而加 case"）。
2. 在 `code/platform-kefu/tests/` 里（或合适的测试位置）写好回归测试，保证能本地跑通。
3. 在 `evals/cases.yaml` 的 `cases` 列表里追加一条：

```yaml
- id: <短横线小写英文, 例: kefu-tenant-leak-guard>
  description: <一句话说清这个测试守的是哪个真实事故/缺陷>
  suite: tests/test_xxx.py          # 相对 code/platform-kefu/ 的路径
  origin: <git commit 短 SHA 或 KNOWN_ISSUES 编号>   # 例: "8f1d29d0" / "KNOWN_ISSUES #40"
  severity: P0                      # P0 表示"回归后服务直接不可用"级别
```

4. 重新跑 `bash evals/run.sh` 确认新 case 能被发现并通过。

`severity` 目前全部标 `P0`——这一批是从已有 P0 回归测试里索引出来的，尚未做分级。
后续如果引入 P1/P2 的守护项，直接改字段即可，`run.sh` 不需要跟着改。

---

## CI 触发逻辑（重要，读完再决定要不要手动 dispatch）

`.opencode/` 和根目录 `AGENTS.md` 这两个目录/文件**都不进 git 版本库**
（`AGENTS.md` 被 `.gitignore` 排除，`.opencode/` 整个目录也是）。

所以 CI（`.github/workflows/ci.yml`）**不会**因为改了 `AGENTS.md` 或 `.opencode/` 里任何文件而触发。
真正会触发 eval job（如果 CI 侧接进来的话）的 `paths` 是：

```yaml
- 'evals/**'                     # 本目录任何文件改动
- '.githooks/**'                 # pre-commit / pre-push 钩子改动
- 'scripts/ci/**'                # CI 脚本（含 check-*.sh）改动
- 'code/platform-kefu/tests/**'  # 客服后端测试改动（含 P0 回归测试本身）
```

另外支持 **manual dispatch**（`workflow_dispatch`）：即使没改上面任何路径，
也可以手动在 GitHub Actions 页面点 "Run workflow" 强制跑一次 eval。

> 注意：CI 侧的接线（把 `bash evals/run.sh` 挂进 `ci.yml` 的一个 job）由另一个 agent 负责，
> 本目录**只交付** manifest + 可执行脚本 + 文档，不碰 `.github/workflows/ci.yml`。

---

## 已知限制

- 目前只索引了 `platform-kefu` 的 P0 套件，Java 后端（`platform-server`）的回归测试还没接进 `cases.yaml`。
  后续如果要加 Java 侧的 eval，需要在 `run.sh` 里加一段 `mvn -o test -pl <module> -am` 分支，
  并把 manifest 里对应 case 的 `suite` 字段写成 maven 模块坐标（约定好前缀，例如 `mvn:park-space`）。
- `platform-kefu-frontend` 与 `platform-ops-admin` 已接入 vitest 单元测试（见各自 `src/__tests__/`，由 `frontend-test` CI job 跑），但它们的测试尚未纳入 `evals/cases.yaml` —— 本 manifest 目前只收 P0 回归（真实事故锁）。
