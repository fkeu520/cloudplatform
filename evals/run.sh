#!/usr/bin/env bash
# evals/run.sh — 云枢中台 evals 回归网入口（Wave-1）
#
# 读 evals/cases.yaml，按 suite 字段逐个跑 pytest，打印摘要，
# 任何一个 suite 失败则以非零码退出。
#
# 依赖：bash、python3、pytest、grep/sed/awk（POSIX 工具链，无 jq）
# 可在 Git Bash (Windows)、Linux CI (ubuntu) 直接跑。

set -euo pipefail

# ── 定位项目根目录（不依赖 CWD，从脚本自身位置推断）─────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KETFU_DIR="$REPO_ROOT/code/platform-kefu"
CASES_FILE="$SCRIPT_DIR/cases.yaml"

# ── 解析 cases.yaml，提取 suite 字段（无 jq，纯 grep/sed）─────
# 每个 case 块里 suite 行形如 "    suite: tests/test_xxx.py"（4 空格缩进）
# 精确匹配 suite: 前缀，避免误抓 description 里的其他内容
suites="$(
  grep -E '^[[:space:]]+suite:' "$CASES_FILE" \
    | sed 's/^[[:space:]]*suite:[[:space:]]*//' \
    | tr -d '"'
)"

# ── 检查 cases.yaml 是否被找到 ─────────────────────────────────
if [ -z "$suites" ]; then
  echo "ERROR: 未能从 $CASES_FILE 解析到任何 suite 字段" >&2
  echo "       请确认 cases.yaml 里每个 case 块都有 'suite:' 字段" >&2
  exit 1
fi

# ── 设置占位环境变量，使 app.config.Settings 可以初始化 ─────────
# 这些值不是真实密钥，与 P0 测试套件内部使用的占位值一致，
# 防止 Settings() 在 import 期因缺少必填字段而抛 ValidationError。
export DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-test-only-placeholder}"
export SILICONFLOW_API_KEY="${SILICONFLOW_API_KEY:-test-only-placeholder}"
export JWT_SECRET="${JWT_SECRET:=test-only-jwt-secret-not-a-real-key-0000}"
# 故意不设置 MYSQL_HOST —— 避免污染其他模块的 skip 逻辑。

total_cases=$(echo "$suites" | grep -c '.')
echo "═══════════════════════════════════════════════════════════"
echo " evals/run.sh — 云枢中台回归评估套件"
echo " 项目根:   $REPO_ROOT"
echo " 客服后端: $KETFU_DIR"
echo " 计划执行: $total_cases 个 case"
echo "═══════════════════════════════════════════════════════════"

# ── 逐个执行 ─────────────────────────────────────────────────
pass=0
fail=0
failed_suites=""

while IFS= read -r suite; do
  [ -z "$suite" ] && continue

  echo ""
  echo "── 执行: $suite"

  set +e
  cd "$KETFU_DIR"
  python -m pytest "$suite" -q 2>&1
  rc=$?
  set -e

  if [ "$rc" -eq 0 ]; then
    echo "   PASS  $suite"
    pass=$((pass + 1))
  else
    echo "   FAIL  $suite  (exit=$rc)"
    fail=$((fail + 1))
    failed_suites="$failed_suites\n    - $suite"
  fi
done <<EOF
$suites
EOF

# ── 摘要 ─────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════════"
echo " 摘要"
echo "  通过: $pass / $total_cases"
echo "  失败: $fail"
if [ -n "$failed_suites" ]; then
  echo "  失败清单:"
  echo -e "$failed_suites"
fi
echo "═══════════════════════════════════════════════════════════"

if [ "$fail" -gt 0 ]; then
  echo ""
  echo "结果: FAIL — 有 $fail 个 case 未通过，退出码 1"
  exit 1
fi

echo "结果: PASS — 全部 $total_cases 个 case 通过，退出码 0"
exit 0
