#!/bin/bash
# ================================================================
# V45 apply script - 在 217 上手动执行 (Flyway 禁用)
#
# 用途: 修复 V44 第 6 步 flyway_schema_history INSERT 报 MySQL 1093 的问题
# 触发: 217 上 apply-v44-on-217.sh 跑到 INSERT history 步骤时失败
#
# 用法:
#   1. cd /opt/platform
#   2. git pull
#   3. bash scripts/diag/apply-v45-on-217.sh
#
# 步骤:
#   1. 防御性重检 sys_room 列/索引 (幂等, 不会破坏已就位的数据)
#   2. 标记 V44 + V45 的 flyway history success=1 (用派生表绕开 MySQL 1093)
#   3. 验证 5 项 (列/索引/数据回填/Flyway 标记)
#   4. 重启 park-space (让 RoomService 重新加载, 加载新 V45)
# ================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/../../code/platform-server/park-space/src/main/resources/db/migration/V45__fix_v44_flyway_history.sql"

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ V45 SQL not found at: $SQL_FILE"
    echo "请先 cd /opt/platform && git pull"
    exit 1
fi

echo "================================================"
echo "  V45 apply: 修复 V44 flyway history MySQL 1093"
echo "  SQL: $SQL_FILE"
echo "================================================"

# 应用 V45 SQL
echo ""
echo "[1/4] 应用 V45 SQL (幂等, 重复执行安全)..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 --default-character-set=utf8mb4 platform < "$SQL_FILE"

# 验证
echo ""
echo "[2/4] 验证 sys_room 状态..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
SELECT 'sys_room floor_id 列'  AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND COLUMN_NAME='floor_id';
SELECT 'sys_room area_id 列'   AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND COLUMN_NAME='area_id';
SELECT 'sys_room idx_floor_id' AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND INDEX_NAME='idx_floor_id';
SELECT 'sys_room idx_room_area'AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND INDEX_NAME='idx_room_area';
SELECT 'V44 history'           AS check_name, success FROM flyway_schema_history WHERE version='44';
SELECT 'V45 history'           AS check_name, success FROM flyway_schema_history WHERE version='45';
"

# 重启 park-space
echo ""
echo "[3/4] 重启 park-space 加载新镜像 (含 V45)..."
docker compose restart park-space
sleep 8
docker ps --filter "name=platform-park-space" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "[4/4] 验证完成"
echo "================================================"
echo "  ✅ V45 apply 完成"
echo ""
echo "  验证 (浏览器侧):"
echo "    1. 强刷浏览器 (Ctrl+Shift+R)"
echo "    2. admin 登录"
echo "    3. 左侧 '房间管理' 树应见 4 级: 园区→分区→楼栋→楼层"
echo "    4. 选楼层后右侧表格应能加载, 不再报 Unknown column"
echo ""
echo "  如果仍有问题, 查看 park-space 日志:"
echo "    docker logs platform-park-space --tail 50"
echo "================================================"
