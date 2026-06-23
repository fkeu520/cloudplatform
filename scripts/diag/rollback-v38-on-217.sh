#!/bin/bash
# ================================================================
# V38 rollback script - 回滚 V38 修改 (从备份表恢复)
#
# 用途: 如果 apply-v38-on-217.sh 应用后发现问题, 用此脚本回滚
# 前置: 必须先跑过 apply-v38-on-217.sh (它会创建 backup_v38_pre 表)
#
# 用法:
#   cd /opt/platform
#   bash scripts/diag/rollback-v38-on-217.sh
#
# 行为:
#   1. 确认 backup_v38_pre 表存在
#   2. 从 backup_v38_pre 恢复 sys_menu 的 id, parent_id, name, app_id, status
#   3. 重新授权 role=1 (按恢复后的状态)
#   4. 不动 100 (因为 V36 应删但漏删, V38 隐藏它, 回滚后会恢复)
#
# 注意事项:
#   - 不需要重启服务 (数据变更)
#   - 如果 100 的恢复会导致重新出现, 考虑再跑一次隐藏 SQL
#   - 此脚本不会删除 flyway_schema_history 的 V38 记录 (留作审计)
# ================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "================================================"
echo "  V38 rollback: 从 backup_v38_pre 恢复"
echo "================================================"

# 1. 确认 backup 表存在
echo ""
echo "[1/5] 检查 backup_v38_pre 表..."
TABLE_EXISTS=$(docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -N -e "SHOW TABLES LIKE 'backup_v38_pre';" 2>/dev/null | wc -l)
if [ "$TABLE_EXISTS" != "1" ]; then
    echo "❌ backup_v38_pre 表不存在"
    echo "请先跑 apply-v38-on-217.sh 创建备份, 或者从其它渠道恢复"
    exit 1
fi
BACKUP_COUNT=$(docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -N -e "SELECT COUNT(*) FROM backup_v38_pre;" 2>/dev/null)
echo "  backup_v38_pre 行数: $BACKUP_COUNT"

# 2. 备份当前状态 (防止 rollback 出错)
echo ""
echo "[2/5] 备份 rollback 前的状态..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
DROP TABLE IF EXISTS backup_v38_post;
CREATE TABLE backup_v38_post AS SELECT id, parent_id, name, app_id, status FROM sys_menu WHERE id BETWEEN 1 AND 300;
SELECT 'backup_v38_post' AS label, COUNT(*) AS cnt FROM backup_v38_post;
"

# 3. 从 backup 恢复
echo ""
echo "[3/5] 从 backup_v38_pre 恢复 sys_menu..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 --default-character-set=utf8mb4 platform <<'SQL'
-- 删掉 V38 期间 role=1 新增的 200/210/220 子菜单授权, 防止恢复后有多余权限
DELETE FROM sys_role_menu
 WHERE menu_id IN (200, 210, 220)
    OR menu_id IN (SELECT id FROM sys_menu WHERE parent_id IN (200, 210, 220))
    OR menu_id IN (SELECT id FROM sys_menu
                    WHERE parent_id IN (SELECT id FROM sys_menu
                                         WHERE parent_id IN (200, 210, 220)));

-- 恢复 sys_menu 字段
UPDATE sys_menu m, backup_v38_pre b
   SET m.parent_id = b.parent_id,
       m.name = b.name,
       m.app_id = b.app_id,
       m.status = b.status
 WHERE m.id = b.id;

-- 100 的授权 (V36 重组后保留的)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM backup_v38_pre
 WHERE id IN (SELECT menu_id FROM sys_role_menu
              WHERE role_id = 1 AND menu_id BETWEEN 100 AND 116);

SELECT 'rollback 完成' AS status;
SQL

# 4. 验证恢复结果
echo ""
echo "[4/5] 验证恢复结果..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
-- 200 恢复后 app_id 应为 10 (V38 之前的状态)
SELECT '200 app_id 恢复后' AS check_name, id, app_id, status
  FROM sys_menu WHERE id IN (200, 210, 220) ORDER BY id;

-- 100 恢复后 status 应为 1
SELECT '100 status 恢复后' AS check_name, id, status
  FROM sys_menu WHERE id = 100;
"

# 5. 标记 V38 rollback 完成
echo ""
echo "[5/5] 标记 flyway_schema_history V38 为 rolled back..."
docker exec -i platform-mysql mysql -uroot -pcloudhub_root_2026 platform -B -e "
UPDATE flyway_schema_history
   SET description = CONCAT(description, ' (rolled back at ', NOW(), ')'),
       success = 0
 WHERE version = '38';
SELECT version, description, success FROM flyway_schema_history WHERE version = '38';
"

echo ""
echo "================================================"
echo "  ✅ V38 rollback 完成"
echo ""
echo "  验证步骤:"
echo "    1. 浏览器强刷 (Ctrl+Shift+R)"
echo "    2. admin 登录"
echo "    3. 空间资产/空间设置/地块管理 不应见"
echo "    4. 空间中心 (legacy 100) 可能重新出现 (符合 V37 之前状态)"
echo ""
echo "  如果 rollback 后 100 又出现, 重新跑 V38 SQL 即可"
echo "================================================"