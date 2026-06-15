#!/bin/bash
# ============================================
# 2026-06-15 回滚脚本 (deploy-and-verify-2026-06-15.sh 失败时使用)
# ============================================
# 配套: scripts/ci/deploy-and-verify-2026-06-15.sh
# 用途: 把 platform-workflow 和 platform-container-exporter 回滚到上一个 working 镜像
# 前提: 之前 deploy-and-verify 跑过, 镜像 digest 已记录到 /tmp/platform-deploy-2026-06-15.json
# 用法: bash scripts/ci/rollback-2026-06-15.sh
# ============================================

set -e

# 自 chmod (git pull 不保留 +x, Windows 上更不可能)
chmod +x "$0" 2>/dev/null || true

# 解析项目根目录 (用绝对路径避免 $0 是相对路径时 cd 算错)
SCRIPT_PATH="$(readlink -f "$0" 2>/dev/null || echo "$0")"
PROJECT_ROOT="$(cd "$(dirname "$SCRIPT_PATH")/../.." && pwd)"
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

ok()    { echo -e "${GREEN}  PASS${NC} $1"; }
warn()  { echo -e "${YELLOW}  WARN${NC} $1"; }
fail()  { echo -e "${RED}  FAIL${NC} $1"; }
step()  { echo -e "\n${BLUE}== $1 ==${NC}"; }

# ============================================
# 检查是否有 deploy 状态文件
# ============================================
STATE_FILE="/tmp/platform-deploy-2026-06-15.json"

if [ ! -f "$STATE_FILE" ]; then
    warn "未找到 deploy 状态文件: $STATE_FILE"
    warn "可能 deploy-and-verify-2026-06-15.sh 没跑过, 或文件已删除"
    echo ""
    echo "可手动回滚:"
    echo "  docker tag <old-digest> ghcr.io/fkeu520/cloudplatform/platform-workflow:latest"
    echo "  docker compose up -d platform-workflow"
    echo "  ... 同 container-exporter"
    exit 1
fi

step "读取 deploy 状态"
echo "  状态文件: $STATE_FILE"
cat "$STATE_FILE"
echo ""

# ============================================
# 回滚 platform-workflow
# ============================================
step "回滚 platform-workflow"

OLD_WORKFLOW_DIGEST=$(grep -o '"platform-workflow-old-digest": "[^"]*"' "$STATE_FILE" | cut -d'"' -f4)
if [ -z "$OLD_WORKFLOW_DIGEST" ]; then
    warn "未找到 platform-workflow 旧 digest, 跳过"
else
    echo "  旧 digest: $OLD_WORKFLOW_DIGEST"
    echo "  当前镜像: $($DOCKER_BIN inspect --format='{{index .RepoDigests 0}}' platform-workflow 2>/dev/null)"

    # 检查旧镜像是否还在本地
    if $DOCKER_BIN images --format '{{.Digest}}' | grep -q "$OLD_WORKFLOW_DIGEST"; then
        ok "旧镜像在本地, 直接 tag 即可"
        $DOCKER_BIN tag "$OLD_WORKFLOW_DIGEST" ghcr.io/fkeu520/cloudplatform/platform-workflow:latest
        $DOCKER_BIN compose up -d platform-workflow
        ok "platform-workflow 已回滚"
    else
        warn "旧镜像不在本地, 需要从 ghcr.io 拉 (会覆盖 :latest 引用)"
        echo "    你确认要回滚吗? (yes/no)"
        read -r CONFIRM
        if [ "$CONFIRM" = "yes" ]; then
            # 这里如果直接拉 latest 会拉新镜像, 应该用 digest 拉
            # ghcr.io 支持 @sha256:... 引用
            warn "请手动跑:"
            echo "  docker pull ghcr.io/fkeu520/cloudplatform/platform-workflow@$OLD_WORKFLOW_DIGEST"
            echo "  docker tag <pushed-image-digest> ghcr.io/fkeu520/cloudplatform/platform-workflow:latest"
            echo "  docker compose up -d platform-workflow"
        else
            warn "取消 platform-workflow 回滚"
        fi
    fi
fi

# ============================================
# 回滚 platform-container-exporter
# ============================================
step "回滚 platform-container-exporter"

OLD_EXPORTER_DIGEST=$(grep -o '"platform-container-exporter-old-digest": "[^"]*"' "$STATE_FILE" | cut -d'"' -f4)
if [ -z "$OLD_EXPORTER_DIGEST" ]; then
    warn "未找到 platform-container-exporter 旧 digest, 跳过"
else
    echo "  旧 digest: $OLD_EXPORTER_DIGEST"
    echo "  当前镜像: $($DOCKER_BIN inspect --format='{{index .RepoDigests 0}}' platform-container-exporter 2>/dev/null)"

    if $DOCKER_BIN images --format '{{.Digest}}' | grep -q "$OLD_EXPORTER_DIGEST"; then
        ok "旧镜像在本地, 直接 tag 即可"
        $DOCKER_BIN tag "$OLD_EXPORTER_DIGEST" ghcr.io/fkeu520/cloudplatform/platform-container-exporter:latest
        $DOCKER_BIN compose up -d container-exporter
        ok "platform-container-exporter 已回滚"
    else
        warn "旧镜像不在本地"
        echo "    你确认要回滚吗? (yes/no)"
        read -r CONFIRM
        if [ "$CONFIRM" = "yes" ]; then
            warn "请手动跑:"
            echo "  docker pull ghcr.io/fkeu520/cloudplatform/platform-container-exporter@$OLD_EXPORTER_DIGEST"
            echo "  docker tag <pushed-image-digest> ghcr.io/fkeu520/cloudplatform/platform-container-exporter:latest"
            echo "  docker compose up -d container-exporter"
        else
            warn "取消 platform-container-exporter 回滚"
        fi
    fi
fi

echo ""
ok "回滚流程完成"
echo "建议: 检查服务状态: docker compose ps"
echo "      查看日志: docker logs platform-workflow --tail 50"