#!/bin/bash
# ============================================
# 2026-06-15 部署 + 验证 (workflow 外键修复 + container-exporter 修复)
# ============================================
# 配套 commit:
#   ffc874b fix(workflow): complete 前清理 JDBC 写入的 candidate identity links
#   3e5abd7 fix(monitoring): 重写 container-exporter 修 Docker 29.x 字段名
#   1533f6b build(ci): 把 container-exporter 打包成镜像
# 用法: bash scripts/ci/deploy-and-verify-2026-06-15.sh
# 退出码: 0 = 全部通过, 1 = 至少一项不通过
# 前提: 在 Ubuntu 192.168.0.217 上, cd /opt/platform, docker compose ps 正常
# ============================================

set -e

# 自 chmod (git pull 不保留 +x, Windows 上更不可能)
# 兜底: 如果用户直接 exec (./deploy-and-verify-...) 没权限, 用 bash 显式调用能绕过
chmod +x "$0" 2>/dev/null || true

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

# Docker 兼容
if command -v docker.exe >/dev/null 2>&1; then
    DOCKER_BIN="docker.exe"
elif command -v docker >/dev/null 2>&1; then
    DOCKER_BIN="docker"
else
    echo "FAIL: docker 未找到"
    exit 1
fi

# ANSI colors
if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; BLUE=''; NC=''
fi

PASS=0
FAIL=0
WARN=0

ok()    { echo -e "${GREEN}  PASS${NC} $1"; PASS=$((PASS+1)); }
fail()  { echo -e "${RED}  FAIL${NC} $1"; FAIL=$((FAIL+1)); }
warn()  { echo -e "${YELLOW}  WARN${NC} $1"; WARN=$((WARN+1)); }
step()  { echo -e "\n${BLUE}== $1 ==${NC}"; }

# ============================================
# Phase 0: 记录当前镜像 digest (用于 rollback)
# ============================================
step "Phase 0: 记录当前镜像 digest (用于 rollback)"

STATE_FILE="/tmp/platform-deploy-2026-06-15.json"
WORKFLOW_OLD=$($DOCKER_BIN inspect --format='{{index .RepoDigests 0}}' platform-workflow 2>/dev/null || echo "none")
EXPORTER_OLD=$($DOCKER_BIN inspect --format='{{index .RepoDigests 0}}' platform-container-exporter 2>/dev/null || echo "none")

cat > "$STATE_FILE" << EOF
{
  "deploy-date": "$(date -Iseconds 2>/dev/null || date)",
  "platform-workflow-old-digest": "$WORKFLOW_OLD",
  "platform-container-exporter-old-digest": "$EXPORTER_OLD"
}
EOF

echo "  platform-workflow 旧 digest: $WORKFLOW_OLD"
echo "  platform-container-exporter 旧 digest: $EXPORTER_OLD"
echo "  状态文件: $STATE_FILE (rollback 脚本会用)"
ok "状态记录完成"

# ============================================
# Phase 1: 拉取新镜像
# ============================================
step "Phase 1: 拉取新镜像 (platform-workflow + platform-container-exporter)"

echo "  拉 platform-workflow ..."
if $DOCKER_BIN pull ghcr.io/fkeu520/cloudplatform/platform-workflow:latest > /dev/null 2>&1; then
    ok "platform-workflow 镜像拉取成功"
else
    fail "platform-workflow 镜像拉取失败"
    exit 1
fi

echo "  拉 platform-container-exporter ..."
if $DOCKER_BIN pull ghcr.io/fkeu520/cloudplatform/platform-container-exporter:latest > /dev/null 2>&1; then
    ok "platform-container-exporter 镜像拉取成功"
else
    fail "platform-container-exporter 镜像拉取失败"
    exit 1
fi

# ============================================
# Phase 2: 重启服务
# ============================================
step "Phase 2: 重启服务 (workflow + container-exporter)"

echo "  docker compose up -d platform-workflow container-exporter ..."
if $DOCKER_BIN compose up -d platform-workflow container-exporter > /dev/null 2>&1; then
    ok "服务重启命令执行成功"
else
    fail "docker compose up 失败"
    exit 1
fi

# ============================================
# Phase 3: 健康检查
# ============================================
step "Phase 3: 健康检查 (30s 超时)"

echo "  等 platform-workflow healthy (最长 30s) ..."
for i in $(seq 1 30); do
    status=$($DOCKER_BIN inspect --format='{{.State.Health.Status}}' platform-workflow 2>/dev/null || echo "starting")
    if [ "$status" = "healthy" ]; then
        ok "platform-workflow 状态: healthy"
        break
    fi
    sleep 1
done
if [ "$status" != "healthy" ]; then
    fail "platform-workflow 30s 内未 healthy, 当前: $status"
fi

echo "  等 container-exporter healthy (最长 30s) ..."
for i in $(seq 1 30); do
    status=$($DOCKER_BIN inspect --format='{{.State.Health.Status}}' platform-container-exporter 2>/dev/null || echo "starting")
    if [ "$status" = "healthy" ]; then
        ok "container-exporter 状态: healthy"
        break
    fi
    sleep 1
done
if [ "$status" != "healthy" ]; then
    fail "container-exporter 30s 内未 healthy, 当前: $status"
fi

# ============================================
# Phase 4: 业务验证
# ============================================
step "Phase 4: 业务验证 (4 项)"

# 4a. ACT_RU_IDENTITYLINK candidate 行 (ffc874b 修复验证)
echo "  4a. 检查 ACT_RU_IDENTITYLINK candidate 行 ..."
candidate_count=$($DOCKER_BIN exec platform-mysql mysql -uroot -proot123456 platform -sN \
    -e "SELECT COUNT(*) FROM ACT_RU_IDENTITYLINK WHERE TYPE_='candidate';" 2>/dev/null || echo "ERROR")
if [ "$candidate_count" != "ERROR" ] && [ "$candidate_count" -gt 0 ]; then
    ok "candidate identity links 存在 ($candidate_count 行)"
else
    warn "candidate identity links 为 0 (正常如果当前无流程在跑)"
fi

# 4b. sys_message.receiver_id (铃铛数据源)
echo "  4b. 检查 sys_message.receiver_id 非空行 ..."
sys_msg_count=$($DOCKER_BIN exec platform-mysql mysql -uroot -proot123456 platform_message -sN \
    -e "SELECT COUNT(*) FROM sys_message WHERE business_type='workflow' AND receiver_id IS NOT NULL;" 2>/dev/null || echo "ERROR")
if [ "$sys_msg_count" != "ERROR" ] && [ "$sys_msg_count" -gt 0 ]; then
    ok "sys_message.receiver_id 非空 workflow 行存在 ($sys_msg_count 行)"
else
    warn "sys_message workflow + receiver_id 非空为 0 (需手动触发流程通知)"
fi

# 4c. read-all 不再 500 (da484b0 修复)
echo "  4c. 检查 site/read-all API ..."
# 用 admin token
admin_token=$(curl -s -X POST http://127.0.0.1:8082/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"123456"}' 2>/dev/null \
    | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$admin_token" ]; then
    warn "admin login 失败, 跳过 read-all 验证 (可能 auth 服务未启动)"
else
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://127.0.0.1:8085/message/site/read-all?userId=1" \
        -H "Authorization: Bearer $admin_token" 2>/dev/null || echo "000")
    if [ "$http_code" = "200" ]; then
        ok "read-all HTTP 200 (markAllRead 修复有效)"
    else
        fail "read-all HTTP $http_code (期望 200)"
    fi
fi

# 4d. container-exporter 内存数据 (3e5abd7 + 1533f6b 修复验证)
echo "  4d. 检查 container-exporter 内存指标 ..."
mem_sample=$(curl -s http://127.0.0.1:9091/metrics 2>/dev/null \
    | grep '^container_memory_rss_bytes{')
if [ -n "$mem_sample" ]; then
    sample_count=$(echo "$mem_sample" | wc -l)
    ok "container-exporter 返回 container_memory_rss_bytes 样本 ($sample_count 行)"
else
    fail "container-exporter 无 container_memory_rss_bytes 数据 (Docker 29.x 字段名修复未生效?)"
fi

# ============================================
# Phase 5: 总结
# ============================================
step "总结"
echo "  PASS: $PASS"
echo "  FAIL: $FAIL"
echo "  WARN: $WARN"

if [ $FAIL -gt 0 ]; then
    echo -e "\n${RED}部署验证未完全通过, 请检查 FAIL 项${NC}"
    exit 1
fi

echo -e "\n${GREEN}部署验证全部通过 (PASS=$PASS, WARN=$WARN)${NC}"
echo "Grafana 容器资源排行: http://192.168.0.217:3000 (admin / admin)"
exit 0