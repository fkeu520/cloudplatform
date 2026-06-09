#!/bin/bash
# ============================================
# 验证 v7.1 数据权限升级灰度开关已正确实施 (PR1-4 共用)
# ============================================
# 配套文档: doc/项目进度.md v7.1 §7.2 · doc/M5-P0-2-决策记录.md v1.1
# 用法: bash scripts/ci/check-data-scope-upgrade-toggle.sh
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
echo "=== 1. DeptMapper.java 新增 selectChildDeptIdsByCte (PR1) ==="
DM="code/platform-server/platform-user/src/main/java/com/cloudhub/platform/user/domain/mapper/DeptMapper.java"
check "import @Select 注解" \
      "import org.apache.ibatis.annotations.Select" "$DM"
check "import @ResultType 注解 (Long 返回值)" \
      "import org.apache.ibatis.annotations.ResultType" "$DM"
check "新增 selectChildDeptIdsByCte 方法" \
      "selectChildDeptIdsByCte" "$DM"
check "使用 MySQL 8 WITH RECURSIVE CTE" \
      "WITH RECURSIVE" "$DM"
check "显式 tenant_id 过滤 (含 NULL 兜底)" \
      "tenant_id = #.tenantId" "$DM"
check "deleted = 0 软删除过滤" \
      "deleted = 0" "$DM"
check "@ResultType(Long.class) 注解" \
      "@ResultType.Long.class." "$DM"

echo ""
echo "=== 2. UserDataScopeProviderImpl.java 灰度分支 (PR1) ==="
UDPI="code/platform-server/platform-user/src/main/java/com/cloudhub/platform/user/tenant/UserDataScopeProviderImpl.java"
check "import @Value" \
      "import org.springframework.beans.factory.annotation.Value" "$UDPI"
check "import TenantContextHolder" \
      "import com.cloudhub.platform.common.config.TenantContextHolder" "$UDPI"
check "灰度开关字段 upgradeEnabled" \
      "private boolean upgradeEnabled" "$UDPI"
check "@Value 注入 platform.data-scope.upgrade.enabled 默认 false" \
      'platform.data-scope.upgrade.enabled:false' "$UDPI"
check "collectChildDeptIds 灰度分支 (if upgradeEnabled)" \
      "if .upgradeEnabled." "$UDPI"
check "新增 CTE 方法 collectChildDeptIdsByCte" \
      "collectChildDeptIdsByCte" "$UDPI"
check "老 DFS 方法保留为 collectChildDeptIdsByRecursive (fallback)" \
      "collectChildDeptIdsByRecursive" "$UDPI"
check "CTE 方法调 deptMapper.selectChildDeptIdsByCte" \
      "deptMapper.selectChildDeptIdsByCte" "$UDPI"

echo ""
echo "=== 3. 6 个 application.yml 配置检查 (PR1-4 共用) ==="
for svc in user auth gateway message ops workflow; do
    YML="code/platform-server/platform-$svc/src/main/resources/application.yml"
    check "platform-$svc.yml 含 platform.data-scope.upgrade.enabled" \
          "enabled:.*PLATFORM_DATA_SCOPE_UPGRADE_ENABLED" "$YML"
    check "platform-$svc.yml 从环境变量 PLATFORM_DATA_SCOPE_UPGRADE_ENABLED 读" \
          "PLATFORM_DATA_SCOPE_UPGRADE_ENABLED" "$YML"
done

echo ""
echo "=== 4. MybatisPlusConfig.java 现状检查 (PR4 演进, PR1 暂不动) ==="
MPC="code/platform-server/platform-common/src/main/java/com/cloudhub/platform/common/config/MybatisPlusConfig.java"
check "DataScopeInnerInterceptor 仍注册 (PR1 不动拦截器)" \
      "DataScopeInnerInterceptor" "$MPC"
# PR4 演进预期: 拦截器按 upgradeEnabled 分支
if grep -qE "data-scope.upgrade" "$MPC" 2>/dev/null; then
    echo -e "  ${GREEN}✅${NC} PR4 演进: 拦截器已读 platform.data-scope.upgrade"
    PASS=$((PASS+1))
else
    echo -e "  ${YELLOW}⏭️${NC}  PR4 演进未实施 (预期内, PR1 不改拦截器)"
fi

echo ""
echo "=== 5. PR4 D+7 write-strict 灰度 (M5 P0-2 全量上线) ==="
for svc in user auth gateway message ops workflow; do
    YML="code/platform-server/platform-$svc/src/main/resources/application.yml"
    check "platform-$svc.yml 有 write-strict 配置" \
          "write-strict:.*PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT" "$YML"
    check "platform-$svc.yml write-strict 默认 true (M5 PR4 全量上线后)" \
          "write-strict:.*PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT:true" "$YML"
done

echo ""
echo "=== 6. docker-compose.yml 不应再有 write-strict env var 覆盖 (全量上线后) ==="
if grep -qE "PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT" docker-compose.yml 2>/dev/null; then
    echo -e "  ${RED}X${NC} docker-compose.yml 仍有 write-strict env var 覆盖 (应已移除)"
    FAIL=$((FAIL+1))
else
    echo -e "  ${GREEN}PASS${NC} docker-compose.yml 已移除 write-strict env var (全量生效)"
    PASS=$((PASS+1))
fi

echo ""
echo "=== ????==="
TOTAL=$((PASS+FAIL))
echo -e "  ???: ${GREEN}${PASS}${NC} / ${TOTAL}"
if [ $FAIL -eq 0 ]; then
    echo -e "  ${GREEN}????????: v7.1 + M5 PR4 write-strict 全量上线 验证通过${NC}"
    exit 0
else
    echo -e "  ${RED}??${FAIL} ??????, ?????????${NC}"
    exit 1
fi
