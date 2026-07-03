#!/usr/bin/env bash
# =============================================================================
# v8 P0-3 Step-up Token 部署脚本 (Ubuntu 217)
# =============================================================================
# 配套: doc/decision/v8-P0-3-step-up-token.md
# 用法:
#   1. 先在 Windows 端: git push origin develop (等 CI 通过)
#   2. SSH 到 192.168.0.217
#   3. cd /opt/platform
#   4. bash scripts/deploy-p0-3-step-up.sh
#
# 依赖:
#   - Ubuntu 26.04, docker compose v2+, 6 核 14GB 872GB SSD
#   - 数据库已建 (platform/platform123)
#   - Nacos 已跑 (nacos/nacos)
#   - SSH 凭据: hugh@192.168.0.217
#
# 风险: 部署会重启所有容器, 约 1-2 分钟服务不可用
# =============================================================================

set -e

# ANSI colors
if [ -t 1 ]; then
    RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; CYAN=''; NC=''
fi

# === 配置 ===
PROJECT_DIR="/opt/platform"
COMPOSE_FILE="docker-compose.yml"
GIT_BRANCH="develop"
HEALTH_CHECK_TIMEOUT=180  # 3 分钟

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  v8 P0-3 Step-up Token 部署脚本                          ║${NC}"
echo -e "${CYAN}║  $(date '+%Y-%m-%d %H:%M:%S')                                       ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# === 1. 前置检查 ===
echo -e "${YELLOW}[1/6]${NC} 前置检查..."

cd "$PROJECT_DIR" || { echo -e "${RED}✗ 无法进入 $PROJECT_DIR${NC}"; exit 1; }

# 检查 git
if ! git rev-parse --git-dir >/dev/null 2>&1; then
    echo -e "${RED}✗ 当前目录不是 git 仓库${NC}"
    exit 1
fi

# 检查当前分支
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$GIT_BRANCH" ]; then
    echo -e "${YELLOW}⚠ 当前分支是 $CURRENT_BRANCH, 不是 $GIT_BRANCH${NC}"
    echo -e "${YELLOW}  切换到 $GIT_BRANCH? (y/N)${NC}"
    read -r REPLY
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        git checkout "$GIT_BRANCH"
    else
        echo -e "${RED}✗ 取消部署 (期望 $GIT_BRANCH 分支)${NC}"
        exit 1
    fi
fi

# 检查 docker compose
if ! command -v docker >/dev/null 2>&1; then
    echo -e "${RED}✗ docker 未安装${NC}"; exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
    echo -e "${RED}✗ docker compose 未安装${NC}"; exit 1
fi

echo -e "${GREEN}✓ 前置检查通过${NC}"
echo ""

# === 2. 拉最新代码 ===
echo -e "${YELLOW}[2/6]${NC} 拉取最新代码 (git pull)..."
git pull origin "$GIT_BRANCH"
echo -e "${GREEN}✓ 代码已更新${NC}"
echo ""

# === 3. 拉最新镜像 ===
echo -e "${YELLOW}[3/6]${NC} 拉取最新 Docker 镜像..."
docker compose -f "$COMPOSE_FILE" pull
echo -e "${GREEN}✓ 镜像已拉取${NC}"
echo ""

# === 4. 重启服务 ===
echo -e "${YELLOW}[4/6]${NC} 重启所有服务 (docker compose up -d)..."
docker compose -f "$COMPOSE_FILE" up -d
echo -e "${GREEN}✓ 服务已重启${NC}"
echo ""

# === 5. 健康检查 ===
echo -e "${YELLOW}[5/6]${NC} 等待服务健康 (最长 ${HEALTH_CHECK_TIMEOUT}s)..."

SERVICES=("platform-mysql" "platform-redis" "platform-nacos" "platform-auth" "platform-gateway" "platform-user" "platform-ops" "platform-admin" "platform-ops-admin")
for SERVICE in "${SERVICES[@]}"; do
    echo -ne "  ${CYAN}${SERVICE}${NC}: "
    ELAPSED=0
    while [ $ELAPSED -lt $HEALTH_CHECK_TIMEOUT ]; do
        if docker ps --filter "name=${SERVICE}" --filter "health=healthy" --format '{{.Names}}' 2>/dev/null | grep -q "${SERVICE}"; then
            echo -e "${GREEN}healthy${NC}"
            break
        fi
        if docker ps --filter "name=${SERVICE}" --format '{{.Names}}' 2>/dev/null | grep -q "${SERVICE}"; then
            # 容器在跑但还没 health check 结果
            echo -n "."
        else
            echo -n "x"
        fi
        sleep 5
        ELAPSED=$((ELAPSED + 5))
    done
    if [ $ELAPSED -ge $HEALTH_CHECK_TIMEOUT ]; then
        echo -e "${RED}TIMEOUT${NC}"
    fi
done
echo ""

# === 6. P0-3 验证 ===
echo -e "${YELLOW}[6/6]${NC} P0-3 Step-up 端到端验证..."

# 6.1 验证 platform-auth 启动日志含 stepup
echo -n "  验证 platform-auth 启动: "
if docker logs platform-auth 2>&1 | grep -qE "Started.*PlatformAuth|stepup"; then
    echo -e "${GREEN}✓ 启动成功${NC}"
else
    echo -e "${YELLOW}⚠ 启动日志未匹配 (不影响部署, 但建议查看)${NC}"
fi

# 6.2 验证 step-up 端点
echo -n "  验证 /actuator/health: "
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8082/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH" = "200" ]; then
    echo -e "${GREEN}✓ 200 OK${NC}"
else
    echo -e "${RED}✗ HTTP $HEALTH (auth 服务异常)${NC}"
fi

# 6.3 验证登录端点
echo -n "  验证 /auth/public-key: "
KEY=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8082/auth/public-key" 2>/dev/null || echo "000")
if [ "$KEY" = "200" ]; then
    echo -e "${GREEN}✓ 200 OK${NC}"
else
    echo -e "${RED}✗ HTTP $KEY${NC}"
fi

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  部署完成                                                     ║${NC}"
echo -e "${CYAN}║                                                              ║${NC}"
echo -e "${CYAN}║  下一步 (5 端到端验证场景):                                ║${NC}"
echo -e "${CYAN}║  1. 浏览器打开 http://192.168.0.217:8090 (ops-admin)        ║${NC}"
echo -e "${CYAN}║  2. 登录 (admin / 123456)                                   ║${NC}"
echo -e "${CYAN}║  3. 租户管理 -> 删租户 -> 触发 step-up 弹窗             ║${NC}"
echo -e "${CYAN}║  4. 输对密码 -> 删除成功 -> 再次点删除 (弹窗又出现)   ║${NC}"
echo -e "${CYAN}║  5. 改密码 -> 旧 step-up token 失效                       ║${NC}"
echo -e "${CYAN}║                                                              ║${NC}"
echo -e "${CYAN}║  详细文档: doc/decision/v8-P0-3-step-up-token.md 第 10.3 节 ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}部署完成, 请执行 5 端到端验证场景确认功能正常.${NC}"
