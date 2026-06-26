#!/bin/bash
# ================================================================
# V44 apply script - 在 217 上手动执行 (Flyway 禁用)
#
# 用途: 防御性补齐 sys_room 的 floor_id + area_id 列
# 触发: RoomService.page() 报 "Unknown column 'floor_id' in 'where clause'"
#       (V38/V41 没 apply 到 217 的脏数据恢复)
#
# 用法:
#   1. cd /opt/platform
#   2. git pull
#   3. bash scripts/diag/apply-v44-on-217.sh
#
# 步骤:
#   1. 应用 V44 SQL (幂等, 不会破坏已存在的列/索引)
#   2. 标记 flyway_schema_history success=1
#   3. 验证: floor_id/area_id 列与索引存在, room 表有 floorId 关联
# ================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/../../code/platform-server/park-space/src/main/resources/db/migration/V44__ensure_room_tree_columns.sql"

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ V44 SQL not found at: $SQL_FILE"
    echo "请先 cd /opt/platform && git pull"
    exit 1
fi

echo "================================================"
echo "  V44 apply: 补齐 sys_room 4 级树字段"
echo "  SQL: $SQL_FILE"
echo "================================================"

# 应用 V44 SQL
echo ""
echo "[1/3] 应用 V44 SQL (幂等, 重复执行安全)..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 --default-character-set=utf8mb4 platform < "$SQL_FILE"

# 验证
echo ""
echo "[2/3] 验证列与索引..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
SELECT 'sys_room floor_id 列'  AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND COLUMN_NAME='floor_id';
SELECT 'sys_room area_id 列'   AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND COLUMN_NAME='area_id';
SELECT 'sys_room idx_floor_id' AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND INDEX_NAME='idx_floor_id';
SELECT 'sys_room idx_room_area'AS check_name, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA='platform' AND TABLE_NAME='sys_room' AND INDEX_NAME='idx_room_area';
SELECT 'room 总数'             AS check_name, COUNT(*) AS cnt FROM sys_room WHERE deleted=0;
SELECT 'room 有 floor_id'      AS check_name, COUNT(*) AS cnt FROM sys_room WHERE deleted=0 AND floor_id IS NOT NULL;
SELECT 'room 有 area_id'       AS check_name, COUNT(*) AS cnt FROM sys_room WHERE deleted=0 AND area_id  IS NOT NULL;
SELECT 'V44 flyway 标记'       AS check_name, version, success FROM flyway_schema_history WHERE version='44';
"

echo ""
echo "[3/3] 重启 park-space 让 RoomService 重新加载..."
docker compose restart park-space
sleep 8
docker ps --filter "name=platform-park-space" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "================================================"
echo "  ✅ V44 apply 完成"
echo ""
echo "  验证 (浏览器侧):"
echo "    1. 强刷浏览器 (Ctrl+Shift+R)"
echo "    2. admin 登录"
echo "    3. 左侧 '房间管理' 树应见 4 级: 园区→分区→楼栋→楼层"
echo "    4. 选某个楼层, 右侧表格应能加载, 不再报 Unknown column"
echo ""
echo "  如果仍有问题, 查看 park-space 日志:"
echo "    docker logs platform-park-space --tail 50"
echo "================================================"
