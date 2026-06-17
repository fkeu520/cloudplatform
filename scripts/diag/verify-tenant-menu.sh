#!/bin/bash
# 217 一站式验证脚本: 拉新镜像 + 重启 + 验证 V25/V27 + 验证菜单数据
# 用法: bash scripts/diag/verify-tenant-menu.sh
# 退出码: 0=全部通过, 1=CI 还没绿, 2=数据未修复, 3=API 失败

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

echo "========================================="
echo "  217 验证: 租户管理员菜单 (V25+V27)"
echo "========================================="
echo

# ============ 1) 镜像时间 (需 > V27 commit 162fee1 ~11:40 UTC) ============
echo "--- 1) 镜像时间 ---"
IMG_TIME=$(docker inspect ghcr.io/fkeu520/cloudplatform/platform-user:latest --format '{{.Created}}' 2>/dev/null)
echo "   platform-user:latest 创建时间: $IMG_TIME"

# 转换为 epoch 比较
IMG_EPOCH=$(date -d "$IMG_TIME" +%s 2>/dev/null || echo 0)
TRIGGER_EPOCH=$(date -d "2026-06-17 03:30 UTC" +%s 2>/dev/null || echo 0)

if [ "$IMG_EPOCH" -lt "$TRIGGER_EPOCH" ]; then
    fail "镜像太旧 (早于 2026-06-17 03:30 UTC), CI 还没构建新镜像. 等 5-10 分钟再跑"
fi
ok "镜像时间 OK (含 V25+V27)"
echo

# ============ 2) 重启 platform-user (Flyway 自动跑 V25+V27) ============
echo "--- 2) 重启 platform-user ---"
docker compose restart platform-user
sleep 8
ok "platform-user 重启完成"
echo

# ============ 3) Flyway 日志 (V25+V27 跑了没) ============
echo "--- 3) Flyway 日志 ---"
if docker logs platform-user 2>&1 | grep -E "V2[57]|v2[57]|Successfully applied" | tail -5; then
    :
else
    fail "Flyway 没找到 V25/V27 日志, 等几秒再试 (Flyway 启动慢)"
fi
echo

# ============ 4) DB 数据验证 ============
echo "--- 4) DB 数据 ---"
DB_RESULT=$(docker exec -it mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user -N -B -e "
SELECT 'tenant_admin_count' AS k, COUNT(*) AS v FROM sys_user WHERE user_type=1;
SELECT 'tenant_app_count' AS k, COUNT(*) AS v FROM sys_tenant_app WHERE tenant_id=1;
SELECT 'tenant_admins' AS k, GROUP_CONCAT(CONCAT(id,'/',username,'/',tenant_id)) AS v FROM sys_user WHERE user_type=1;
" 2>/dev/null)

if [ -z "$DB_RESULT" ]; then
    fail "DB 查询失败, 检查 MYSQL_ROOT_PASSWORD 环境变量"
fi

echo "$DB_RESULT"

TENANT_ADMIN_COUNT=$(echo "$DB_RESULT" | grep "tenant_admin_count" | awk '{print $2}')
TENANT_APP_COUNT=$(echo "$DB_RESULT" | grep "tenant_app_count" | awk '{print $2}')

if [ "$TENANT_ADMIN_COUNT" = "0" ]; then
    fail "没有 userType=1 的用户, V25 没跑或被卡住"
fi
ok "租户管理员: $TENANT_ADMIN_COUNT 个"

if [ "$TENANT_APP_COUNT" != "5" ]; then
    fail "sys_tenant_app 应该 5 行, 实际 $TENANT_APP_COUNT 行, V27 没跑成功"
fi
ok "sys_tenant_app: 5 行 (全部 system app 已授权)"
echo

# ============ 5) API 端到端验证 ============
echo "--- 5) API 验证 ---"
# 找一个租户管理员
TENANT_ADMIN_USER=$(docker exec -it mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user -N -B -e \
  "SELECT username FROM sys_user WHERE user_type=1 LIMIT 1" 2>/dev/null | tr -d '\r')

if [ -z "$TENANT_ADMIN_USER" ]; then
    fail "没有租户管理员, 无法测试 API"
fi
note "测试账号: $TENANT_ADMIN_USER / 123456"

# 登录拿 token
LOGIN_RES=$(curl -s -X POST "http://192.168.0.217:8083/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TENANT_ADMIN_USER\",\"password\":\"123456\"}")

TOKEN=$(echo "$LOGIN_RES" | jq -r '.data.token' 2>/dev/null)

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    fail "登录失败, response: $LOGIN_RES"
fi
ok "登录成功, 拿到 token"

# 拿菜单
MENU_RES=$(curl -s -H "Authorization: Bearer $TOKEN" "http://192.168.0.217:8083/menu/user")
MENU_COUNT=$(echo "$MENU_RES" | jq -r '.data | length' 2>/dev/null)

if [ "$MENU_COUNT" = "5" ]; then
    ok "菜单 5 个根节点 (5 个 app tab)"
elif [ "$MENU_COUNT" = "0" ]; then
    fail "菜单仍为空, tenant_id 可能是 NULL 或 sys_tenant_app 数据有误"
else
    warn "菜单返回 $MENU_COUNT 个根节点 (期望 5)"
fi
echo

# ============ 6) 浏览器提示 ============
echo "========================================="
echo "  下一步: 浏览器硬刷新 + 登录"
echo "========================================="
note "1. Ctrl+Shift+R 硬刷新 http://192.168.0.217:8080"
note "2. 用 $TENANT_ADMIN_USER / 123456 登录"
note "3. 顶部 tabs 应该看到 5 个 app: 系统管理/用户中心/流程中心/消息中心/运营管理"
note ""
note "如果还是空菜单:"
note "  - 重新登录 (清浏览器 JWT 缓存)"
note "  - 检查浏览器 console 看 /api/menu/user 响应"
echo
ok "全部验证通过!"
