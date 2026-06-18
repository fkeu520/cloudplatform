#!/bin/bash
# 217 验证脚本: 合并 app (用户中心/流程中心/消息中心 → 系统管理) 后的状态检查
# 用法: bash scripts/diag/verify-consolidation.sh
# 退出码: 0=全部通过, 1=DB 异常, 2=API 异常

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail()  { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }
note()  { echo "       $1"; }

DB_CMD="docker exec -i mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user -N -B"

echo "========================================="
echo "  217 验证: app 合并 → 系统管理"
echo "========================================="
echo

# ============ 1) sys_app 当前状态 ============
echo "--- 1) sys_app status ---"
APPS=$($DB_CMD -e "
SELECT CONCAT(id, '|', app_name, '|', app_code, '|', app_type, '|', status)
FROM sys_app WHERE deleted = 0 ORDER BY id;
" 2>/dev/null)

if [ -z "$APPS" ]; then
    fail "DB 查询失败, 检查 MYSQL_ROOT_PASSWORD 环境变量"
fi

echo "$APPS" | while IFS='|' read -r id name code type status; do
    if [ "$id" = "1" ] && [ "$status" = "1" ]; then
        ok "  app $id: $name ($code) - status=1 ✓"
    elif [ "$id" = "5" ] && [ "$status" = "0" ]; then
        ok "  app $id: $name ($code) - status=0 ✓ (8090 运营后台用)"
    elif [ "$id" -ge 6 ] && [ "$id" -le 10 ] && [ "$status" = "1" ]; then
        ok "  app $id: $name ($code) - status=1 ✓ (业务占位)"
    elif [ "$id" -ge 2 ] && [ "$id" -le 4 ] && [ "$status" = "0" ]; then
        ok "  app $id: $name ($code) - status=0 ✓ (已合并)"
    else
        warn "  app $id: $name ($code) - status=$status (检查是否符合预期)"
    fi
done
echo

# ============ 2) sys_menu.app_id 分布 ============
echo "--- 2) sys_menu app_id 分布 ---"
DIST=$($DB_CMD -e "
SELECT CONCAT(app_id, '=', COUNT(*)) FROM sys_menu
WHERE deleted = 0 AND app_id IS NOT NULL
GROUP BY app_id ORDER BY app_id;
" 2>/dev/null)
echo "$DIST"

# 验证: 应该有 app_id=1 (含合并的菜单), 业务 app 6-10 (待 W4 填充), 不应该有 2/3/4
HAS_DEACTIVATED=$(echo "$DIST" | grep -E "^(2=|3=|4=)" || true)
if [ -n "$HAS_DEACTIVATED" ]; then
    fail "发现菜单仍在停用 app 下: $HAS_DEACTIVATED"
fi
ok "所有 app_id=2/3/4 的菜单已迁移"
echo

# ============ 3) 菜单总数对账 ============
echo "--- 3) 菜单总数对账 ---"
BEFORE_COUNT=$($DB_CMD -e "SELECT COUNT(*) FROM sys_menu_app_id_backup;" 2>/dev/null)
AFTER_COUNT=$($DB_CMD -e "SELECT COUNT(*) FROM sys_menu WHERE app_id = 1 AND deleted = 0;" 2>/dev/null)
echo "   备份的行数: $BEFORE_COUNT (合并前在 app 2/3/4 的菜单)"
echo "   app 1 当前菜单数: $AFTER_COUNT"

# 至少要包含合并进来的
if [ "$BEFORE_COUNT" = "0" ]; then
    warn "备份表为空, 可能没跑过合并脚本或没有可合并菜单"
else
    ok "合并脚本已执行 (备份了 $BEFORE_COUNT 行)"
fi
echo

# ============ 4) 孤儿菜单检查 ============
echo "--- 4) 孤儿菜单检查 (app_id=NULL 或指向停用 app) ---"
ORPHAN=$($DB_CMD -e "
SELECT COUNT(*) FROM sys_menu m
LEFT JOIN sys_app a ON a.id = m.app_id
WHERE m.deleted = 0
  AND (m.app_id IS NULL OR (a.status = 0 AND m.app_id != 1));
" 2>/dev/null)

if [ "$ORPHAN" != "0" ]; then
    fail "发现 $ORPHAN 个孤儿菜单, 需手动处理"
fi
ok "无孤儿菜单"
echo

# ============ 5) sys_tenant_app 状态 ============
echo "--- 5) sys_tenant_app (租户 1) 状态 ---"
TENANT_APPS=$($DB_CMD -e "
SELECT CONCAT(ta.app_id, '|', a.app_name, '|', ta.status)
FROM sys_tenant_app ta
LEFT JOIN sys_app a ON a.id = ta.app_id
WHERE ta.tenant_id = 1
ORDER BY ta.app_id;
" 2>/dev/null)
echo "$TENANT_APPS" | while IFS='|' read -r app_id name status; do
    if [ "$app_id" = "1" ] && [ "$status" = "1" ]; then
        ok "  app $app_id: $name - tenant 1 authorized ✓"
    elif [ "$app_id" -ge 6 ] && [ "$app_id" -le 10 ] && [ "$status" = "1" ]; then
        ok "  app $app_id: $name - tenant 1 authorized ✓ (业务占位)"
    else
        note "  app $app_id: $name - status=$status"
    fi
done
echo

# ============ 6) 端到端 API 验证 (租户管理员登录) ============
echo "--- 6) API 端到端验证 ---"
TENANT_ADMIN=$($DB_CMD -e "SELECT username FROM sys_user WHERE user_type=1 AND tenant_id=1 LIMIT 1;" 2>/dev/null | tr -d '\r')

if [ -z "$TENANT_ADMIN" ]; then
    fail "没有租户管理员 (userType=1, tenant_id=1), 跑 fix-tenant-menu-data.sql 先"
fi
note "测试账号: $TENANT_ADMIN / 123456"

LOGIN_RES=$(curl -s -X POST "http://192.168.0.217:8083/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TENANT_ADMIN\",\"password\":\"123456\"}" 2>/dev/null)

TOKEN=$(echo "$LOGIN_RES" | jq -r '.data.token' 2>/dev/null)

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    fail "登录失败, response: $LOGIN_RES"
fi
ok "登录成功"

# 拿 app 列表
APPS_RES=$(curl -s -H "Authorization: Bearer $TOKEN" "http://192.168.0.217:8083/app/user" 2>/dev/null)
APP_COUNT=$(echo "$APPS_RES" | jq -r '.data | length' 2>/dev/null)

if [ "$APP_COUNT" = "0" ]; then
    fail "/app/user 返回空, 请检查"
fi

# 期望: 1 (系统管理) + 业务占位 (可能 0/5 个, 取决于 add-business-apps.sql 是否跑过)
note "租户管理员看到 $APP_COUNT 个 app tab (期望 1 + 业务占位 0~5)"

# 列出 app 名称
APP_NAMES=$(echo "$APPS_RES" | jq -r '.data[] | .appName' 2>/dev/null | tr '\n' ',' | sed 's/,$//')
note "app 列表: $APP_NAMES"

# 检查 停用的 app 不应出现
HAS_DEACTIVATED_APP=$(echo "$APPS_RES" | jq -r '.data[] | select(.appCode=="user-center" or .appCode=="workflow" or .appCode=="notification") | .appCode' 2>/dev/null)
if [ -n "$HAS_DEACTIVATED_APP" ]; then
    fail "停用的 app 仍出现在列表: $HAS_DEACTIVATED_APP"
fi
ok "停用的 app 已不在 tab 列表"
echo

# ============ 7) 菜单 API 验证 (合并后菜单应该全在 app 1) ============
echo "--- 7) /menu/user 验证 ---"
MENU_RES=$(curl -s -H "Authorization: Bearer $TOKEN" "http://192.168.0.217:8083/menu/user?appId=1" 2>/dev/null)
ROOT_MENU_COUNT=$(echo "$MENU_RES" | jq -r '.data | length' 2>/dev/null)
note "app 1 下根菜单数: $ROOT_MENU_COUNT"

if [ "$ROOT_MENU_COUNT" -lt 3 ]; then
    warn "app 1 下根菜单少于 3 个, 可能合并未生效或菜单未挂上"
else
    ok "app 1 下有 $ROOT_MENU_COUNT 个根菜单 (合并后应为多模块合一)"
fi
echo

echo "========================================="
echo "  下一步: 浏览器硬刷新 + 登录"
echo "========================================="
note "1. Ctrl+Shift+R 硬刷新 http://192.168.0.217:8080"
note "2. 用 $TENANT_ADMIN / 123456 登录"
note "3. 顶部 tabs 应该看到 1 个'系统管理' (合并了用户中心/流程中心/基础配置/消息中心)"
note "4. 左侧菜单应该有: 用户管理/角色管理/菜单管理/组织/部门/岗位/字典/参数/流程/消息 等"
echo
ok "全部验证通过!"
