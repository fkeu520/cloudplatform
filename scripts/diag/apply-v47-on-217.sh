#!/bin/bash
# ================================================================
# Apply V47 on 217: 更新 building_structure + room_structure 字典
#
# 用法:
#   ./scripts/diag/apply-v47-on-217.sh
#
# 前置:
#   1. 已 git pull 最新代码到 /opt/platform
#   2. park-space MySQL 账号有 DDL 权限
# ================================================================
set -euo pipefail

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_HOST="${MYSQL_HOST:-192.168.0.217}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
PARK_SPACE_DB="${PARK_SPACE_DB:-cloudhub_park_space}"

SCRIPT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SQL_FILE="${SCRIPT_DIR}/code/platform-server/park-space/src/main/resources/db/migration/V47__update_building_room_structure_dicts.sql"

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ V47 SQL not found at: $SQL_FILE"
    echo "   Did you forget to git pull?"
    exit 1
fi

echo "==> Applying V47 on $PARK_SPACE_DB (update dict labels) ..."
read -r -s -p "Enter MySQL password: " MYSQL_PASS
echo
mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" -h"$MYSQL_HOST" -P"$MYSQL_PORT" "$PARK_SPACE_DB" < "$SQL_FILE"

echo "==> Done. V47 applied to $PARK_SPACE_DB."