#!/bin/bash
# 217 一站式部署: app 合并 + Park 模块
# 用法: bash scripts/diag/deploy-consolidate-park-on-217.sh
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  fail "请设置 MYSQL_ROOT_PASSWORD 环境变量"
fi

MYSQL_CMD() { docker exec -i mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$1" -N -B; }

echo "========================================="
echo "  217 部署: consolidate apps + park"
echo "========================================="
echo

# ============ 1) Pull 最新镜像 ============
echo "--- 1) git pull ---"
cd /opt/platform
git pull origin develop
ok "git pull 完成"

echo "--- 2) docker compose pull ---"
docker compose pull park-space || warn "park-space 镜像可能不存在(源码部署)"
ok "pull 完成"

# ============ 2) Flyway 迁移 (V31 sys_park + V32 menu) ============
echo "--- 3) Flyway 自动迁移 (V31+V32) ---"
# park-space 和 platform-user 重启时会自动执行 Flyway
docker compose up -d park-space platform-user
sleep 5
ok "park-space + platform-user 已重启, Flyway 自动执行中"

# ============ 3) 执行 app 合并 SQL ============
echo "--- 4) 执行 consolidate-apps-to-system.sql ---"
MYSQL_CMD cloudhub_user < scripts/diag/consolidate-apps-to-system.sql
ok "app 合并 SQL 执行完毕"

# ============ 4) 重启 platform-user 使新 app/menu 配置生效 ============
echo "--- 5) 重启 platform-user 加载新配置 ---"
docker compose restart platform-user
sleep 3
ok "platform-user 已重启"

# ============ 5) 验证服务健康 ============
echo "--- 6) 验证服务健康 ---"
for svc in platform-user park-space; do
  HEALTH=$(docker inspect --format='{{.State.Status}}' "$svc" 2>/dev/null || echo "missing")
  if [ "$HEALTH" = "running" ]; then ok "$svc running"; else fail "$svc 未运行"; fi
done

# ============ 6) API 验证 ============
echo "--- 7) API 验证 ---"
# 验证 park 端点
PARK_RESULT=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/park/page 2>/dev/null || echo "000")
if [ "$PARK_RESULT" = "200" ] || [ "$PARK_RESULT" = "401" ]; then
  ok "/park/page = $PARK_RESULT (401 表示需要认证, 说明网关路由正常)"
else
  warn "/park/page = $PARK_RESULT (期望 401, 请检查网关路由)"
fi

# 验证 consolidate 效果
echo
echo "--- 8) DB 验证: sys_app 状态 ---"
MYSQL_CMD cloudhub_user -e "SELECT id,app_name,app_code,status FROM sys_app WHERE deleted=0 ORDER BY id;"

echo
echo "--- 9) DB 验证: sys_menu app_id 分布 ---"
MYSQL_CMD cloudhub_user -e "SELECT app_id,COUNT(*) AS cnt FROM sys_menu WHERE deleted=0 AND app_id IS NOT NULL GROUP BY app_id ORDER BY app_id;"

echo
echo "========================================="
ok "全部完成!"
echo
echo "下一步验证:"
echo "  1) 浏览器打开 http://192.168.0.142:8080"
echo "  2) 用 admin/123456 登录"
echo "  3) 检查顶部 tab: 应该只有"系统管理"1 个 tab"
echo "  4) 左侧菜单应该包含: 用户/角色/菜单/组织/部门/岗位/字典/参数/园区/日志 等"
echo "  5) 点击"园区管理"验证 CRUD 页面正常"
echo "========================================="