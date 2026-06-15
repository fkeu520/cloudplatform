#!/usr/bin/env bash
# scripts/ci/verify-init-bpmn-2026-06-15.sh
# 217 上验证 InitBpmnRunner 是否正常工作 + 诊断 500 错误
# 使用: bash verify-init-bpmn-2026-06-15.sh

set -uo pipefail
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()    { echo -e "${GREEN}[OK]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail()  { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

echo "=========================================="
echo " InitBpmnRunner 217 验证 (2026-06-15)"
echo "=========================================="

# ----- Phase 0: 检查当前镜像 digest -----
echo
echo "--- Phase 0: 当前镜像状态 ---"
DOCKER_BIN="${DOCKER_BIN:-docker}"
WORKFLOW_IMG=$($DOCKER_BIN inspect --format='{{index .RepoDigests 0}}' platform-workflow 2>/dev/null || echo "not found")
echo "  platform-workflow: $WORKFLOW_IMG"

# 应包含 99564dd (InitBpmnRunner 提交)
if echo "$WORKFLOW_IMG" | grep -q "@sha256:"; then
    ok "platform-workflow 使用 sha 锁定镜像"
    if $DOCKER_BIN pull ghcr.io/fkeu520/cloudplatform/platform-workflow:latest 2>&1 | grep -q "Downloaded\|up to date"; then
        ok "可拉取 platform-workflow:latest"
    else
        warn "拉取失败或已最新, 手动检查"
    fi
else
    warn "platform-workflow 没用 sha 锁定, 风险: 部署的可能不是最新代码"
fi

# ----- Phase 1: 检查 InitBpmnRunner 是否启动 -----
echo
echo "--- Phase 1: InitBpmnRunner 启动日志 ---"
INIT_LOGS=$($DOCKER_BIN logs platform-workflow 2>&1 | grep -i "InitBpmn" || echo "")
if [ -z "$INIT_LOGS" ]; then
    fail "InitBpmnRunner 没运行! 镜像可能不是最新, 或启动失败
  修复: $DOCKER_BIN pull ghcr.io/fkeu520/cloudplatform/platform-workflow:latest
        $DOCKER_BIN compose up -d platform-workflow
        $DOCKER_BIN logs platform-workflow | grep -i InitBpmn"
fi
echo "$INIT_LOGS" | tail -10
if echo "$INIT_LOGS" | grep -q "部署成功"; then
    ok "InitBpmnRunner 部署了至少一个流程"
elif echo "$INIT_LOGS" | grep -q "已存在"; then
    warn "InitBpmnRunner 检测到 key 已存在, 跳过 (符合'幂等不覆盖'设计原则)
  若想强制重新部署 init-bpmn 流程, 先 SQL 删除:
    DELETE FROM ACT_RE_PROCDEF WHERE KEY_ = 'leave-approval';
    $DOCKER_BIN compose restart platform-workflow"
else
    warn "InitBpmnRunner 启动但状态不明, 详细日志:"
    $DOCKER_BIN logs platform-workflow 2>&1 | grep -A1 -B1 "InitBpmn" | tail -20
fi

# ----- Phase 2: 列出 ACT_RE_PROCDEF 流程定义 -----
echo
echo "--- Phase 2: 流程定义列表 ---"
$DOCKER_BIN exec -i platform-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123}" \
    -e "SELECT ID_, KEY_, VERSION_, NAME_, DEPLOY_TIME_ FROM act_re_procdef ORDER BY DEPLOY_TIME_ DESC LIMIT 10" 2>&1 | \
    grep -v "Using a password" | head -20

# ----- Phase 3: 诊断 500 错误的 checklist -----
echo
echo "--- Phase 3: 500 错误诊断 checklist ---"
echo "  如果 /workflow/task/{id}/complete 报 500:"
echo "  1. 查 workflow 容器日志:"
echo "       $DOCKER_BIN logs platform-workflow --tail 100 | grep -A 30 'ERROR.*GlobalExceptionHandler'"
echo
echo "  2. 看 stack trace 中的关键字:"
echo "     - 'Unknown property used in expression' → JUEL 缺变量, 见下方 4a"
echo "     - 'SAXParseException' → BPMN XML 不合法, 见下方 4b"
echo "     - 'NCName' → processKey 非法, 见下方 4c"
echo "     - 'cvc-datatype-valid' → 条件表达式类型不匹配, 见下方 4d"
echo
echo "  4a. JUEL 缺变量 (e.g. \${day >= 3})"
echo "      原因: 流程定义含 exclusive gateway, 但启动时没传 condition 引用的变量"
echo "      修复 (A): 业务方启动流程时传 day 变量:"
echo "        POST /api/workflow/instance/start"
echo "        { \"processDefinitionKey\": \"leave-approval\","
echo "          \"variables\": { \"day\": 1 } }"
echo "      修复 (B): 删旧 PROC_DEF 让 InitBpmnRunner 部署无 gateway 的最简版:"
echo "        DELETE FROM ACT_RE_PROCDEF WHERE KEY_ = 'leave-approval';"
echo "        $DOCKER_BIN compose restart platform-workflow"
echo
echo "  4b. SAXParseException"
echo "      原因: BPMN XML 不合法 (含未转义 <>& 或非法 NCName)"
echo "      修复: BpmnDesigner.vue 已加 XML 转义 (commit f481c1a), 重启 + 重新设计"
echo
echo "  4c. NCName 非法"
echo "      原因: processKey 含非法字符 (如纯数字 2121212212)"
echo "      修复: WorkflowDefinitionService 已加 NCName 校验 (commit 569a7a0), 重新设计"
echo
echo "  4d. cvc-datatype-valid"
echo "      原因: conditionExpression 返回类型不匹配 (e.g. \${day} 返回 String, 不是 Boolean)"
echo "      修复: 确保 conditionExpression 返回 boolean, 如 \${day >= 3}"

# ----- Phase 4: 一键完整验证 -----
echo
echo "--- Phase 4: 一键 API 验证 (admin 登录 + 启动流程 + complete) ---"
ADMIN_TOKEN=$(curl -s -X POST http://127.0.0.1:8082/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"123456"}' 2>/dev/null | \
    grep -oP '(?<="token":")[^"]+' | head -1 || echo "")

if [ -z "$ADMIN_TOKEN" ]; then
    warn "admin 登录失败 (auth 服务可能未起), 跳过 API 验证"
    echo "  手动测试:"
    echo "    curl -X POST http://127.0.0.1:8080/api/workflow/instance/start \\"
    echo "      -H 'Content-Type: application/json' \\"
    echo "      -d '{\"processDefinitionKey\":\"leave-approval\",\"businessKey\":\"test-001\",\"userId\":\"admin\"}'"
else
    ok "admin 登录成功, token: ${ADMIN_TOKEN:0:20}..."
    INSTANCE=$(curl -s -X POST http://127.0.0.1:8080/api/workflow/instance/start \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -d '{"processDefinitionKey":"leave-approval","businessKey":"test-init-001","userId":"admin"}')
    echo "  启动结果: $INSTANCE"
fi

echo
echo "=========================================="
echo " 验证完成"
echo "=========================================="
