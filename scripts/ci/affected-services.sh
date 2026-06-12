#!/usr/bin/env bash
# =====================================================
# 扫描 git commit 范围改动了哪些 platform service
# 用法: bash scripts/ci/affected-services.sh <from-sha> [to-sha]
# 默认: from=HEAD~10, to=HEAD
# 输出: 改动的 service 列表 (例如: platform-message platform-admin)
# =====================================================
# 2026-06-12 翻车复盘: 我之前回答用户"只拉 platform-message"是错的
# e33286b 改过 platform-admin/src/api/workflow.ts + Layout.vue
# 但用户从那个 commit 之后没拉过 admin 镜像, 浏览器调旧 API 报 404
# 这个脚本自动扫所有 commit 改的 service, 避免漏拉
# =====================================================

set -e

FROM="${1:-HEAD~10}"
TO="${2:-HEAD}"

# Service 识别规则: 路径前缀映射
# 后端服务: code/platform-server/<service>/...
# 前端服务: code/<service>/...
declare -A SERVICE_MAP=(
  ["code/platform-server/platform-user/"]="platform-user"
  ["code/platform-server/platform-auth/"]="platform-auth"
  ["code/platform-server/platform-gateway/"]="platform-gateway"
  ["code/platform-server/platform-workflow/"]="platform-workflow"
  ["code/platform-server/platform-message/"]="platform-message"
  ["code/platform-server/platform-ops/"]="platform-ops"
  ["code/platform-server/platform-common/"]="platform-common"
  ["code/platform-admin/"]="platform-admin"
  ["code/platform-ops-admin/"]="platform-ops-admin"
)

# CI 自身改动: .github/workflows/ 不需要 pull 任何 service
# .githooks/ 也不需要 (本地 hook)
# docker-compose.yml: 改了要 docker compose up -d 整体重启, 单独标记
# README/docs: 不需要 pull

CHANGED_FILES=$(git diff --name-only "$FROM" "$TO" 2>/dev/null || git log --name-only --pretty=format: "$FROM..$TO")

if [ -z "$CHANGED_FILES" ]; then
  echo "ERROR: 无法获取 $FROM..$TO 的 diff, 请检查 sha 是否正确" >&2
  exit 1
fi

AFFECTED=()
INFRA_CHANGED=0
CI_CHANGED=0

while IFS= read -r file; do
  [ -z "$file" ] && continue
  matched=0
  for prefix in "${!SERVICE_MAP[@]}"; do
    if [[ "$file" == "$prefix"* ]]; then
      svc="${SERVICE_MAP[$prefix]}"
      if [[ ! " ${AFFECTED[@]} " =~ " $svc " ]]; then
        AFFECTED+=("$svc")
      fi
      matched=1
      break
    fi
  done
  # 基础设施变更
  if [[ "$matched" == "0" ]]; then
    case "$file" in
      docker-compose.yml|docker-compose.*.yml|docker/*/Dockerfile|docker/*/Dockerfile.*)
        INFRA_CHANGED=1
        ;;
      .github/workflows/*|.github/*)
        CI_CHANGED=1
        ;;
    esac
  fi
done <<< "$CHANGED_FILES"

echo "=== 改动扫描 ==="
echo "  范围: $FROM..$TO"
echo "  变更文件数: $(echo "$CHANGED_FILES" | grep -c .)"
echo
echo "=== 受影响的 service (需要 docker compose pull + up -d) ==="
if [ ${#AFFECTED[@]} -eq 0 ]; then
  echo "  (无 service 代码改动)"
else
  for svc in "${AFFECTED[@]}"; do
    echo "  - $svc"
  done
fi

echo
echo "=== 其他变更 ==="
[ "$INFRA_CHANGED" -eq 1 ] && echo "  - 基础设施 (docker-compose / Dockerfile) 改了 → 需整体重启"
[ "$CI_CHANGED" -eq 1 ] && echo "  - CI 配置改了 → 下次 push 才会生效"

echo
echo "=== 217 部署命令 ==="
if [ ${#AFFECTED[@]} -eq 0 ]; then
  echo "  # 本次无 service 改动, 无需 pull"
elif [ "$INFRA_CHANGED" -eq 1 ]; then
  echo "  cd /opt/platform"
  echo "  docker compose pull"
  echo "  docker compose up -d"
else
  services=$(printf " %s" "${AFFECTED[@]}")
  echo "  cd /opt/platform"
  echo "  docker compose pull${services}"
  echo "  docker compose up -d${services}"
fi

echo
echo "=== 提醒 ==="
echo "  ⚠ 浏览器硬刷新 Ctrl+Shift+R 清缓存"
echo "  ⚠ 前端 service (platform-admin / platform-ops-admin) 改了必须 pull, 否则浏览器调旧 API"
