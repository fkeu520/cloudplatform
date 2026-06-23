#!/bin/bash
# ================================================================
# V38 apply script - 在 217 上手动执行 (Flyway 在 217 已禁用)
#
# 用途: 把 V38 SQL (修复空间菜单可见性) 应用到 217 环境的 MySQL
# 来源: code/platform-server/platform-user/src/main/resources/db/migration/V38__fix_space_menu_visibility.sql
#
# 用法:
#   1. cd /opt/platform
#   2. git pull (获取最新 SQL)
#   3. bash scripts/diag/apply-v38-on-217.sh
#
# 步骤:
#   1. 检查 Flyway 是否启用 (217 应禁用, 手动 SQL)
#   2. 应用 V38 SQL 到 MySQL (通过 docker exec + utf8mb4 编码)
#   3. 标记 flyway_schema_history success=1 (避免 Flyway 启用后重复跑)
#   4. 验证: 200/210/220 app_id=1, 100 status=0
#
# 注意事项:
#   - 不需要重启任何服务 (数据变更, 缓存会自然失效)
#   - 前端用户需刷新浏览器 (Ctrl+Shift+R 强刷) 加载新菜单
# ================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/../../code/platform-server/platform-user/src/main/resources/db/migration/V38__fix_space_menu_visibility.sql"

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ V38 SQL not found at: $SQL_FILE"
    echo "请先 cd /opt/platform && git pull"
    exit 1
fi

echo "================================================"
echo "  V38 apply: 修复空间菜单可见性"
echo "  SQL: $SQL_FILE"
echo "================================================"

# 检查 Flyway 是否启用
FLYWAY_ENABLED=$(docker exec platform-user sh -c 'echo $SPRING_FLYWAY_ENABLED' 2>/dev/null || echo "unknown")
echo ""
echo "Flyway enabled in container: $FLYWAY_ENABLED (期望: false / unknown)"

# 备份当前菜单状态 (以便回滚)
echo ""
echo "[1/4] 备份当前菜单状态..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
DROP TABLE IF EXISTS backup_v38_pre;
CREATE TABLE backup_v38_pre AS SELECT id, parent_id, name, app_id, status FROM sys_menu WHERE id BETWEEN 1 AND 300;
SELECT 'backup count' AS label, COUNT(*) AS cnt FROM backup_v38_pre;
"

# 应用 V38 SQL
echo ""
echo "[2/4] 应用 V38 SQL (通过 docker exec + utf8mb4)..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 --default-character-set=utf8mb4 platform < "$SQL_FILE"

# 标记 Flyway 成功 (即使 Flyway 禁用, 也写记录以便后续开启 Flyway 时不重复)
echo ""
echo "[3/4] 标记 Flyway V38 success=1 (防止启用后重跑)..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
  (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
  '38', 'fix space menu visibility', 'SQL', 'V38__fix_space_menu_visibility.sql',
  NULL, 'hugh', NOW(), 1000, 1
)
ON DUPLICATE KEY UPDATE success = 1, execution_time = 1000;
SELECT 'V38 flyway_schema_history' AS label, version, description, success, installed_on
  FROM flyway_schema_history WHERE version = '38';
"

# 验证
echo ""
echo "[4/4] 验证结果..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
-- 200/210/220 及子菜单 app_id 应全部为 1
SELECT '空间菜单 app_id' AS check_name, id, parent_id, name, app_id, status
  FROM sys_menu
 WHERE id IN (200, 210, 220)
    OR parent_id IN (200, 210, 220)
    OR parent_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE parent_id IN (200, 210, 220)) AS t)
 ORDER BY parent_id, sort, id;

-- 100 及其子菜单 status 应为 0
SELECT 'legacy 100 状态' AS check_name, id, parent_id, name, app_id, status
  FROM sys_menu
 WHERE id = 100 OR parent_id = 100
 ORDER BY parent_id, sort, id;

-- 100 不应出现在 sys_role_menu
SELECT '100 在角色授权中' AS check_name, COUNT(*) AS fail_count
  FROM sys_role_menu
 WHERE menu_id = 100 OR menu_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE parent_id = 100) AS t);

-- role=1 应该有 200/210/220 + 子菜单授权
SELECT 'role=1 空间菜单授权数' AS check_name, COUNT(*) AS granted
  FROM sys_role_menu rm JOIN sys_menu m ON rm.menu_id = m.id
 WHERE rm.role_id = 1
   AND (m.id IN (200, 210, 220) OR m.parent_id IN (200, 210, 220));
"

echo ""
echo "================================================"
echo "  ✅ V38 apply 完成"
echo ""
echo "  验证步骤 (浏览器侧):"
echo "    1. 强刷浏览器 (Ctrl+Shift+R / Cmd+Shift+R)"
echo "    2. admin 登录"
echo "    3. 左侧菜单应见: 空间资产 / 空间设置 / 地块管理"
echo "    4. 不应见: 空间中心 (legacy)"
echo ""
echo "  如果仍有问题, 查看平台 user 日志:"
echo "    docker logs platform-user --tail 50"
echo "================================================"