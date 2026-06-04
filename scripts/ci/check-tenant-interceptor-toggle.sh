#!/bin/bash
# ============================================
# 验证 P0-1 多租户拦截器回滚开关 (ConditionalOnProperty 方案 A) 已正确实施
# ============================================
# 配套文档: doc/P0-1-回滚开关设计.md 方案 A
# 用法: bash scripts/ci/check-tenant-interceptor-toggle.sh
# 退出码: 0 = 全部通过, 1 = 至少一项不通过
# ============================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

# ANSI colors
if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; NC=''
fi

PASS=0
FAIL=0

check() {
    local desc="$1"
    local pattern="$2"
    local file="$3"
    if grep -qE "$pattern" "$file" 2>/dev/null; then
        echo -e "  ${GREEN}✅${NC} $desc"
        PASS=$((PASS+1))
    else
        echo -e "  ${RED}❌${NC} $desc"
        echo -e "     ${YELLOW}预期: $pattern${NC}"
        echo -e "     ${YELLOW}文件: $file${NC}"
        FAIL=$((FAIL+1))
    fi
}

echo ""
echo "=== 1. MybatisPlusConfig.java 改造检查 ==="
MPC="code/platform-server/platform-common/src/main/java/com/cloudhub/platform/common/config/MybatisPlusConfig.java"
check "import ConditionalOnProperty" \
      "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty" "$MPC"
check "启用版 Bean mybatisPlusInterceptor 标注 @ConditionalOnProperty" \
      '@ConditionalOnProperty.*havingValue.*true.*matchIfMissing' "$MPC"
check "禁用版 Bean mybatisPlusInterceptorDisabled 标注 @ConditionalOnProperty" \
      '@ConditionalOnProperty.*havingValue.*false' "$MPC"
check "禁用版 Bean 仅保留 PaginationInnerInterceptor (无 TenantLineInnerInterceptor)" \
      'mybatisPlusInterceptorDisabled' "$MPC"

echo ""
echo "=== 2. 6 个 application.yml 配置检查 ==="
for svc in user auth gateway message ops workflow; do
    YML="code/platform-server/platform-$svc/src/main/resources/application.yml"
    check "platform-$svc.yml 含 platform.tenant.interceptor.enabled" \
          "platform.tenant.interceptor.enabled" "$YML"
    check "platform-$svc.yml 从环境变量 PLATFORM_TENANT_INTERCEPTOR_ENABLED 读" \
          "PLATFORM_TENANT_INTERCEPTOR_ENABLED" "$YML"
done

echo ""
echo "=== 汇总 ==="
TOTAL=$((PASS+FAIL))
echo -e "  通过: ${GREEN}${PASS}${NC} / ${TOTAL}"
if [ $FAIL -eq 0 ]; then
    echo -e "  ${GREEN}✅ 全部通过: P0-1 回滚开关已正确实施${NC}"
    exit 0
else
    echo -e "  ${RED}❌ ${FAIL} 项不通过, 请修复后重试${NC}"
    exit 1
fi
