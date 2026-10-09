#!/usr/bin/env bash
# AGENTS.md ↔ .claude/CLAUDE.md 同步校验 (2026-10-09)
#
# 背景: 根 AGENTS.md 是 opencode 每次会话都读的稳定规则; .claude/CLAUDE.md 是
#       Claude Code 用的完整流程纪律版. 两份文件长期并存, 关键红线/规范必须在
#       两份里都出现 —— 只改一份, 另一个 harness 的读者就会拿到过期约束.
#
# 用法: bash scripts/ci/check-agents-claude-sync.sh
#       CI: harness-sync job; 建议本地 pre-commit 也可调用.
# 退出: 0 = 关键约束同步; 1 = 有漂移(打印缺失项).
#
# 注意: 这里只校验「必须同时存在」的少数不变量, 不追求两文件全等
#       (AGENTS.md 是精简版, CLAUDE.md 是完整版, 天然有差异).

set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
A="$ROOT/AGENTS.md"
C="$ROOT/.claude/CLAUDE.md"

[ -f "$C" ] || { echo "DRIFT: 缺少 $C"; exit 1; }

# AGENTS.md 目前是「本地文件」（项目决策：暂不纳入版本库，见 KNOWN_ISSUES / 决策记录）,
# 因此 CI 的 checkout 里没有它。它存在时执行完整校验；缺失时跳过并提示, 保持 CI 不误红。
# 若将来决定把 AGENTS.md 入库, 本脚本在 CI 会自动开始强制校验, 无需改动。
if [ ! -f "$A" ]; then
  echo "NOTICE: AGENTS.md 不在当前工作树(git 未跟踪) —— 跳过 AGENTS↔CLAUDE 同步校验。"
  echo "        如需在 CI 强制校验, 请将 AGENTS.md 纳入版本库。"
  exit 0
fi

# 每条不变量: 描述|ERE 正则 —— 两份文件都必须命中该正则
INVARIANTS=(
  '红线-禁止未验证就 commit|禁止 commit'
  '红线-禁止未批准就 push|git push'
  '红线-部署需用户 go|"go"'
  'commit message 中文|commit message.*中文'
  '验证命令-maven|mvn'
)

drift=0
for item in "${INVARIANTS[@]}"; do
  desc="${item%%|*}"
  re="${item#*|}"
  for f in "$A" "$C"; do
    if ! grep -qE "$re" "$f"; then
      echo "DRIFT: [$desc] 未在 $(basename "$f") 命中 (模式: $re)"
      drift=1
    fi
  done
done

if [ "$drift" -ne 0 ]; then
  echo ""
  echo "AGENTS.md 与 .claude/CLAUDE.md 的关键约束不同步 —— 请同步两份文件后重试。"
  exit 1
fi

echo "OK: AGENTS.md 与 .claude/CLAUDE.md 关键约束同步"
exit 0
