#!/bin/bash
# ============================================
# 验证所有后端服务的 JWT_SECRET 已正确配置 (防 KNOWN_ISSUES #36 复发)
# ============================================
# 配套文档: doc/log/项目进度.md v7.8 · doc/log/KNOWN_ISSUES.md #36
# 用法: bash scripts/ci/check-jwt-secret-alignment.sh
# 退出码: 0 = 全部通过, 1 = 至少一项不通过
# ============================================
# 背景:
#   2026-06-25 仅修 auth 缺 JWT_SECRET, 2026-06-29 复发发现
#   platform-gateway 也缺, 引发 Dashboard 401.
#   根因: JwtUtil 静态块 (line 28-35) 强制校验 JWT_SECRET,
#   缺环境变量时抛 ExceptionInInitializerError (Error 非 Exception)
#   → TenantFilter.catch (Exception) 不捕获 → 500
#   本脚本扫描 docker-compose.yml 中所有需要 JwtUtil 的后端服务,
#   验证它们都声明了 JWT_SECRET 环境变量.
# ============================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

# ANSI colors
if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[1;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; BLUE=''; NC=''
fi

PASS=0
FAIL=0

step() { echo -e "\n${BLUE}== $1 ==${NC}"; }
ok()   { echo -e "  ${GREEN}PASS${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "  ${RED}FAIL${NC} $1"; FAIL=$((FAIL+1)); }
warn() { echo -e "  ${YELLOW}WARN${NC} $1"; }

# ============================================
# 1. 扫描所有引用 JwtUtil 的后端服务
# ============================================
step "1. 扫描所有引用 JwtUtil 的后端服务"

# 用 grep 找引用 com.cloudhub.platform.common.util.JwtUtil 的模块
JWT_SERVICES=$(grep -rl "com.cloudhub.platform.common.util.JwtUtil" \
    code/platform-server --include="*.java" 2>/dev/null \
    | xargs -I {} dirname {} \
    | xargs -I {} dirname {} \
    | xargs -I {} basename {} \
    | sort -u)

if [ -z "$JWT_SERVICES" ]; then
    fail "未找到任何引用 JwtUtil 的服务, 扫描逻辑可能失效"
    exit 1
fi

echo "  引用 JwtUtil 的服务: $(echo $JWT_SERVICES | tr '\n' ' ')"

# ============================================
# 2. 验证 docker-compose.yml 中每个服务都有 JWT_SECRET
# ============================================
step "2. 验证 docker-compose.yml 中每个服务都有 JWT_SECRET"

if [ ! -f "docker-compose.yml" ]; then
    fail "docker-compose.yml 不存在"
    exit 1
fi

for svc in $JWT_SERVICES; do
    # docker-compose.yml 中服务名为 platform-<svc> 形式
    DOCKER_NAME="platform-${svc}"

    # 跳过共享库 (platform-common 是 lib, 不跑容器)
    if [ "$svc" = "common" ]; then
        warn "platform-common 是共享 lib, 不部署为容器, 跳过 JWT_SECRET 检查"
        continue
    fi

    # 检查 DOCKER_NAME 块存在
    if ! grep -qE "^[[:space:]]+${DOCKER_NAME}:" docker-compose.yml 2>/dev/null; then
        warn "${DOCKER_NAME} 不在 docker-compose.yml 中"
        continue
    fi

    # 简单方法: 在 docker-compose.yml 中, 从 ^  DOCKER_NAME: 行开始,
    # 到下一个 0 缩进或 2 空格但非子项的行结束
    # 用 awk: state machine 简单实现
    in_block=$(awk -v svc="${DOCKER_NAME}" '
        $0 ~ "^[[:space:]]+" svc ":" { flag=1; next }
        flag && /^[^[:space:]]/ { flag=0; next }
        flag { print }
    ' docker-compose.yml)

    if echo "$in_block" | grep -qE "JWT_SECRET"; then
        ok "${DOCKER_NAME} 服务块已声明 JWT_SECRET"
    else
        fail "${DOCKER_NAME} 服务块缺 JWT_SECRET (KNOWN_ISSUES #36 复发风险)"
    fi
done

# ============================================
# 3. 验证所有 JWT_SECRET 使用同一 source of truth (环境变量或一致默认值)
# ============================================
step "3. 验证 JWT_SECRET 统一 source of truth"

# 提取所有 JWT_SECRET 赋值模式
SECRETS=$(grep -E "JWT_SECRET" docker-compose.yml 2>/dev/null | \
    grep -oE "JWT_SECRET[=:][^[:space:]\"]+" | sort -u)

echo "  JWT_SECRET 出现模式:"
echo "$SECRETS" | sed 's/^/    /'

# 校验: 应当全部用 ${JWT_SECRET:-default} 形式, 或全部用同一直赋值
# 不允许出现 2 个不同的硬编码默认值
HARDCODED=$(echo "$SECRETS" | grep -vE "\$\{JWT_SECRET" | wc -l)
ENV_VAR=$(echo "$SECRETS" | grep -E "\$\{JWT_SECRET" | wc -l)

if [ "$HARDCODED" -gt 0 ] && [ "$ENV_VAR" -gt 0 ]; then
    fail "JWT_SECRET 混用模式: $HARDCODED 个硬编码, $ENV_VAR 个环境变量 (建议统一为 \${JWT_SECRET:-default})"
elif [ "$HARDCODED" -gt 1 ]; then
    fail "JWT_SECRET 出现 $HARDCODED 个不同的硬编码值 (应统一为单 source of truth)"
else
    ok "JWT_SECRET source 模式一致 (${ENV_VAR} 个环境变量, ${HARDCODED} 个硬编码)"
fi

# ============================================
# 4. 验证 JwtUtil 静态块存在强制校验
# ============================================
step "4. 验证 JwtUtil 静态块强制校验 (防止 缺 JWT_SECRET 时静默)"

JWT_UTIL="code/platform-server/platform-common/src/main/java/com/cloudhub/platform/common/util/JwtUtil.java"

if [ ! -f "$JWT_UTIL" ]; then
    fail "JwtUtil.java 不存在: $JWT_UTIL"
else
    if grep -qE "static[[:space:]]*\{|static[[:space:]]+[A-Z]" "$JWT_UTIL"; then
        ok "JwtUtil 存在 static 初始化块"
    else
        fail "JwtUtil 缺 static 初始化块, 无法启动时强制校验"
    fi
    if grep -qE "throw.*Exception|throw.*Error" "$JWT_UTIL"; then
        ok "JwtUtil 静态块抛错 (启动期暴露 JWT_SECRET 缺失)"
    else
        warn "JwtUtil 静态块未抛错, 缺 JWT_SECRET 时不会 fail-fast"
    fi
fi

# ============================================
# 总结
# ============================================
TOTAL=$((PASS+FAIL))
echo ""
echo "== Summary =="
echo -e "  PASS: ${GREEN}${PASS}${NC} / ${TOTAL}"
if [ $FAIL -eq 0 ]; then
    echo -e "  ${GREEN}JWT_SECRET alignment 验证通过, 无 #36 复发风险${NC}"
    exit 0
else
    echo -e "  ${RED}${FAIL} 项不通过, 需修复后重跑${NC}"
    exit 1
fi
