# doc/intent — 意图入口

> AI 原生 SDLC 的 Wave 2 交付物链入口：**先写意图，再写规格，再动手**。

## 什么时候必须写 intent

满足以下任一条件，**动手写代码前**先在本目录补一份 intent：

- 一次改动涉及 **≥3 个模块**（`platform-*` / `park-*` 中任意 3 个及以上）
- 涉及 **DB migration**（Flyway `code/platform-server/**/db/migration/*.sql` 或任意表结构变更）

不满足上述条件的小改动（单模块、无迁移）可直接进 openspec / 开工，无需 intent。

## 怎么用

1. 复制 `TEMPLATE.md` → `YYYY-MM-DD-<简述>.md`（例：`2026-10-09-kefu-企业档案接入.md`）
2. 填 `Problem / Proposed outcome / Affected users and systems / Constraints / Open questions`
3. `Status` 从 `draft` 起步，评审后改 `accepted`（废弃则改 `superseded`）
4. 由它驱动 openspec 的 `proposal → spec → tasks`：意图是入口，openspec 是规格机制

## 与 openspec 的关系

| 层 | 载体 | 回答什么 |
|---|---|---|
| 意图 | 本目录 `doc/intent/` | 为什么做、要什么结果 |
| 规格 | `openspec/changes/**`（`/opsx-*`、`/openflow/*`） | 精确行为、验收标准 |
| 计划 | 计划文档 / tasks | 怎么拆、谁做、顺序 |

## 为什么放在 `doc/` 而不是新开仓库

单个代码仓库内，意图放在产品仓库的 `doc/intent/` 最省事；只有意图跨越多个仓库时，才值得单开一个意图仓库承担额外开销。
