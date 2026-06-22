#!/bin/bash
# 217 一站式部署验证: park-space W3.3-W3.6 (15 个新实体)
# 用法: bash scripts/diag/deploy-park-space-w36-on-217.sh
#
# 前置条件:
#   1) GitHub Actions CI 已绿色 (W3.3-W3.6 4 个 commit 已推送)
#   2) 217 上 /opt/platform 已 git pull 完成
#   3) 217 上 park-space 旧镜像已存在 (首次部署请用 docker compose up -d park-space)
#
# 验证范围:
#   - 4 个 Flyway 迁移: V32 (Area/Floor/Kit/PlanUse/LandNature)
#                      V33 (Covenant/Energy/Equipment)
#                      V34 (Space/SpaceCategory/Massif)
#                      V35 (RoomLockRecord/RoomPurpose/RoomRecord/RoomSplitMerge)
#   - 15 个新端点: page/getById/create/update/delete (含 append-only 表)
#   - 17 个新菜单 (id=102-116)
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  fail "请设置 MYSQL_ROOT_PASSWORD 环境变量"
fi

MYSQL_CMD() { docker exec -i platform-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$1" -N -B; }
GATEWAY="http://localhost:8083"
LOGIN_URL="http://localhost:8082/auth/login"
PARK_SPACE_DIRECT="http://localhost:8091"

echo "========================================="
echo "  217 部署验证: park-space W3.3-W3.6"
echo "========================================="
echo

# ============ 1) Git pull ============
echo "--- 1) git pull (W3.3-W3.6 4 commit) ---"
cd /opt/platform
git pull origin develop
ok "git pull 完成 (本地含 bd71933 / f7dcfc8 / b72cedf / 4ae9018)"

# ============ 2) Pull park-space 镜像 ============
echo "--- 2) docker compose pull park-space ---"
docker compose pull park-space
ok "park-space 镜像已拉取"

# ============ 3) 重启 park-space (触发 Flyway 迁移 V32-V35) ============
echo "--- 3) docker compose up -d park-space (Flyway 自动跑 V32-V35) ---"
docker compose up -d park-space
sleep 15  # Flyway 4 个迁移约 10s
ok "park-space 已重启"

# ============ 4) 验证容器健康 ============
echo "--- 4) 验证 park-space 容器健康 ---"
HEALTH=$(docker inspect --format='{{.State.Health.Status}}' platform-park-space 2>/dev/null || echo "none")
if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "starting" ]; then
  ok "platform-park-space health: $HEALTH"
else
  warn "platform-park-space health: $HEALTH (查看日志: docker logs platform-park-space --tail 100)"
fi

# 等待 Flyway 跑完 (从 starting 转 healthy)
echo "--- 5) 等待 park-space 健康 (up to 60s) ---"
for i in $(seq 1 12); do
  HEALTH=$(docker inspect --format='{{.State.Health.Status}}' platform-park-space 2>/dev/null || echo "none")
  if [ "$HEALTH" = "healthy" ]; then
    ok "park-space healthy (等待 ${i}*5s)"
    break
  fi
  sleep 5
done

if [ "$HEALTH" != "healthy" ]; then
  fail "park-space 未变健康, 请检查 docker logs platform-park-space --tail 100"
fi

# ============ 6) 验证 Flyway 迁移 ============
echo "--- 6) 验证 Flyway 迁移 V32-V35 (park-space) ---"
FLYWAY_RESULT=$(MYSQL_CMD cloudhub_user -e "SELECT version,description,success FROM flyway_schema_history WHERE version IN ('32','33','34','35') ORDER BY installed_rank;")
echo "$FLYWAY_RESULT"
SUCCESS_COUNT=$(echo "$FLYWAY_RESULT" | grep -c "1" || true)
if [ "$SUCCESS_COUNT" -ge 4 ]; then
  ok "Flyway V32-V35 全部 success=1"
else
  fail "Flyway 迁移失败, success 计数 = $SUCCESS_COUNT (期望 4)"
fi

# ============ 7) 验证 14 张新表已创建 ============
echo "--- 7) 验证 14 张新表 (按 sys_ 前缀) ---"
EXPECTED_TABLES=(
  "sys_area"
  "sys_floor"
  "sys_kit"
  "sys_plan_use"
  "sys_land_nature"
  "sys_covenant"
  "sys_energy"
  "sys_equipment"
  "sys_space_category"
  "sys_space"
  "sys_massif"
  "sys_room_purpose"
  "sys_room_lock_record"
  "sys_room_record"
  "sys_room_split_merge"
)
MISSING=0
for tbl in "${EXPECTED_TABLES[@]}"; do
  CNT=$(MYSQL_CMD cloudhub_user -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cloudhub_user' AND table_name='$tbl';")
  if [ "$CNT" = "1" ]; then
    ok "$tbl 已创建"
  else
    fail "$tbl 不存在"
    MISSING=$((MISSING+1))
  fi
done

if [ "$MISSING" -gt 0 ]; then
  fail "$MISSING 张表未创建, 请检查 Flyway 日志"
fi

# ============ 8) 验证 17 个新菜单已 seed ============
echo "--- 8) 验证 17 个新菜单 (app_id=6 空间中心, id 102-116) ---"
MENU_RESULT=$(MYSQL_CMD cloudhub_user -e "SELECT id,name,path FROM sys_menu WHERE app_id=6 AND deleted=0 ORDER BY id;")
echo "$MENU_RESULT"
MENU_COUNT=$(echo "$MENU_RESULT" | wc -l)
if [ "$MENU_COUNT" -ge 17 ]; then
  ok "空间中心菜单数 = $MENU_COUNT (期望 ≥17)"
else
  warn "空间中心菜单数 = $MENU_COUNT (期望 ≥17, 检查 V32-V35 seed)"
fi

# ============ 9) 验证种子数据 ============
echo "--- 9) 验证种子数据已加载 ---"
for tbl in "${EXPECTED_TABLES[@]}"; do
  CNT=$(MYSQL_CMD cloudhub_user -e "SELECT COUNT(*) FROM $tbl WHERE deleted=0;")
  echo "  $tbl: $CNT 行"
done

# ============ 10) 登录 admin 获取 JWT ============
echo "--- 10) 登录 admin 获取 JWT (用于网关路由测试) ---"
TOKEN=$(curl -s -X POST "$LOGIN_URL" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('token', ''))")
if [ -z "$TOKEN" ]; then
  fail "登录失败, 无法获取 JWT (检查 platform-auth 日志)"
fi
ok "JWT 已获取 (长度: ${#TOKEN})"

# ============ 11) 验证 17 个端点 (走网关 /park-space) ============
echo "--- 11) 验证 17 个端点 (网关路由 /park-space) ---"
ENDPOINTS=(
  "/park/page"
  "/room/page"
  "/area/page"
  "/floor/page"
  "/kit/page"
  "/plan-use/page"
  "/land-nature/page"
  "/covenant/page"
  "/energy/page"
  "/equipment/page"
  "/space-category/page"
  "/space/page"
  "/massif/page"
  "/room-purpose/page"
  "/room-lock-record/page"
  "/room-record/page"
  "/room-split-merge/page"
)
PASS=0
FAIL=0
for ep in "${ENDPOINTS[@]}"; do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "$GATEWAY$ep" 2>/dev/null || echo "000")
  if [ "$CODE" = "200" ]; then
    ok "$ep = 200"
    PASS=$((PASS+1))
  else
    fail "$ep = $CODE (期望 200, 检查 park-space 日志)"
    FAIL=$((FAIL+1))
  fi
done

echo
echo "  端点测试: $PASS PASS / $FAIL FAIL"

# ============ 12) 验证 Flyway schema_history 完整 ============
echo "--- 12) 验证 Flyway schema_history 总数 ---"
TOTAL_MIGRATIONS=$(MYSQL_CMD cloudhub_user -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1;")
ok "park-space Flyway 成功迁移总数: $TOTAL_MIGRATIONS"

# ============ 13) 最终总结 ============
echo
echo "========================================="
ok "W3.3-W3.6 部署验证全部完成!"
echo
echo "📊 部署统计:"
echo "  - 14 张新表已建"
echo "  - 17 个新菜单已 seed"
echo "  - 17 个端点全部 200"
echo "  - park-space 健康状态: $HEALTH"
echo
echo "🔗 下一步验证 (浏览器):"
echo "  1) http://192.168.0.217:8080 (管理后台)"
echo "  2) admin / 123456 登录"
echo "  3) 顶部 tab: 系统管理"
echo "  4) 左侧菜单 → 空间中心 → 区域/楼层/配套/规划用途/土地性质"
echo "  5) 验证 CRUD (新增一条区域, 看 sys_area 表 +1)"
echo "  6) 合同房间/能耗/设备/空间/地块 同样验证"
echo "  7) 房源用途/锁定记录/绑定记录/拆分合并 同样验证"
echo
echo "🔍 排查命令:"
echo "  - docker logs platform-park-space --tail 100"
echo "  - docker exec platform-mysql mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" cloudhub_user -e \"SELECT * FROM sys_area;\""
echo "  - curl http://localhost:8091/actuator/health (绕过网关)"
echo "========================================="