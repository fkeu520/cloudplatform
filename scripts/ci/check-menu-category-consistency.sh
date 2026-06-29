#!/bin/bash
# ============================================
# 验证 sys_menu.menu_category 一致性 (防 KNOWN_ISSUES #36.2/#36.4 复发)
# ============================================
# 配套文档: doc/log/项目进度.md v7.8 · doc/log/KNOWN_ISSUES.md #36.2/#36.4
# 用法: bash scripts/ci/check-menu-category-consistency.sh
# 退出码: 0 = 全部通过, 1 = 至少一项不通过
# ============================================
# 背景:
#   2026-06-29 发现 platform-admin 5 个菜单端点 (/tree/nav/user/permissions/role)
#   无 menu_category 过滤, 导致 ops-admin (8090) 串到 admin-platform 全量菜单.
#   修复: MenuService 全部加 filterAdminMenu() 过滤 + Menu 实体加 menuCategory.
#   本脚本验证: (1) MenuService 关键端点都加了过滤 (2) Menu 实体有 menuCategory
#   字段 (3) H2 测试 schema 同步 (4) V40 迁移脚本存在
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

MENU_SERVICE="code/platform-server/platform-user/src/main/java/com/cloudhub/platform/user/service/MenuService.java"
MENU_ENTITY="code/platform-server/platform-user/src/main/java/com/cloudhub/platform/user/domain/entity/Menu.java"
H2_SCHEMA="code/platform-server/platform-user/src/test/resources/sql/tenant-test-schema.sql"
V40_SQL="code/platform-server/platform-user/src/main/resources/db/migration/V40__add_menu_category.sql"

# ============================================
# 1. Menu 实体有 menuCategory 字段
# ============================================
step "1. Menu 实体有 menuCategory 字段"

if [ ! -f "$MENU_ENTITY" ]; then
    fail "Menu.java 不存在: $MENU_ENTITY"
else
    if grep -qE "private[[:space:]]+String[[:space:]]+menuCategory" "$MENU_ENTITY"; then
        ok "Menu.java 有 menuCategory 字段 (String)"
    else
        fail "Menu.java 缺 menuCategory 字段 (会导致 MyBatis 映射失败)"
    fi
fi

# ============================================
# 2. MenuService 关键端点都有 filterAdminMenu 过滤
# ============================================
step "2. MenuService 关键端点都有 filterAdminMenu 过滤"

if [ ! -f "$MENU_SERVICE" ]; then
    fail "MenuService.java 不存在: $MENU_SERVICE"
else
    # filterAdminMenu 方法存在
    if grep -qE "private[[:space:]]+.*[[:space:]]+filterAdminMenu" "$MENU_SERVICE"; then
        ok "filterAdminMenu() 工具方法存在"
    else
        fail "缺 filterAdminMenu() 工具方法"
    fi

    # 5 个端点都必须做 menu_category 隔离 (任意形式: filterAdminMenu() 或 MyBatis getMenuCategory 过滤)
    METHODS=("tree" "navTree" "getUserMenusInternal" "getUserPermissions" "getRoleMenus")
    for m in "${METHODS[@]}"; do
        # 用 grep -A 50 找方法体 (粗略 50 行足够覆盖所有 5 个方法)
        method_body=$(grep -A 50 "${m}(" "$MENU_SERVICE" | head -80)
        if echo "$method_body" | grep -qE "filterAdminMenu|getMenuCategory.*admin|MenuCategory.*admin"; then
            ok "$m() 已做 menu_category 隔离"
        else
            fail "$m() 未做 menu_category 隔离 (admin 可能再次串到 ops 菜单)"
        fi
    done
fi

# ============================================
# 3. H2 测试 schema 同步 menu_category 列
# ============================================
step "3. H2 测试 schema 同步 menu_category 列"

if [ ! -f "$H2_SCHEMA" ]; then
    fail "H2 测试 schema 不存在: $H2_SCHEMA"
else
    if grep -qE "menu_category.*VARCHAR.*DEFAULT" "$H2_SCHEMA"; then
        ok "tenant-test-schema.sql 已含 menu_category 列 (DEFAULT 'admin')"
    else
        fail "tenant-test-schema.sql 缺 menu_category 列 (TenantIsolationTest 会因 schema 不匹配失败)"
    fi
fi

# ============================================
# 4. V40 迁移脚本存在
# ============================================
step "4. V40 迁移脚本存在"

if [ ! -f "$V40_SQL" ]; then
    fail "V40 迁移脚本不存在: $V40_SQL"
else
    if grep -qE "ALTER TABLE.*sys_menu" "$V40_SQL" && grep -qE "ADD COLUMN.*menu_category" "$V40_SQL"; then
        ok "V40 已加 menu_category 列"
    else
        fail "V40 缺 ALTER TABLE 加列语句"
    fi

    # 检查至少插入 8 个 ops-admin 菜单
    OPS_INSERT=$(grep -c "ops-admin" "$V40_SQL" 2>/dev/null || echo "0")
    if [ "$OPS_INSERT" -ge 8 ]; then
        ok "V40 插入 $OPS_INSERT 个 ops-admin 菜单 (>= 8)"
    else
        warn "V40 仅插入 $OPS_INSERT 个 ops-admin 菜单 (期望 >= 8)"
    fi
fi

# ============================================
# 5. platform-ops-admin 不应再直接拉 /menu/tree (应走 /menu/by-category)
# ============================================
step "5. platform-ops-admin 调用链"

OPS_USER_CTRL="code/platform-server/platform-ops/src/main/java/com/cloudhub/platform/ops/controller/OpsUserController.java"

if [ -f "$OPS_USER_CTRL" ]; then
    if grep -qE "/menu/by-category|menu_category.*ops-admin" "$OPS_USER_CTRL"; then
        ok "OpsUserController 已走 menu_category 隔离路径"
    else
        warn "OpsUserController 未引用 /menu/by-category, 可能仍依赖 filterAdminMenu() 后端过滤"
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
    echo -e "  ${GREEN}menu_category consistency 验证通过, 无 #36.2/#36.4 复发风险${NC}"
    exit 0
else
    echo -e "  ${RED}${FAIL} 项不通过, 需修复后重跑${NC}"
    exit 1
fi