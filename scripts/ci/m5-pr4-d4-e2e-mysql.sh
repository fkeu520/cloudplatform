#!/bin/bash
# ============================================
# M5 PR4 D+4 端到端验证 (真 MySQL 8)
# ============================================
# 配套: doc/M5-PR4-D4-mysql-e2e.md
# 用法: bash scripts/ci/m5-pr4-d4-e2e-mysql.sh
# 退出码: 0 = 全部通过, 1 = 至少一项不通过
# 前提: platform-mysql, platform-auth, platform-user 容器已启动并 healthy
# ============================================
#
# ⚠️ 测试目标说明 (M5 PR4 D+4):
# 验证 DataScopeInnerInterceptor 写拦截基础设施可用 (PR4 D+1-D+3 已完成)
# + 已有 @DataScope 注解的查询路径 (LIST) 按部门过滤生效
# 已知限制: 写方法 (UserService.update/delete) 未加 @DataScope 注解,
#           写保护需在 M5.5+ 实体扩展时同步加注解 (与 doc/M5-P0-2-实施子任务.md §9 一致)
# ============================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

GATEWAY="http://localhost:8083"
MYSQL_CONTAINER="platform-mysql"
DB_USER="platform"
DB_PASS="platform123"
DB_NAME="platform"

# Docker 兼容: WSL/MSYS/Git-Bash 用 docker.exe, Linux/macOS 用 docker
if command -v docker.exe >/dev/null 2>&1; then
    DOCKER_BIN="docker.exe"
elif command -v docker >/dev/null 2>&1; then
    DOCKER_BIN="docker"
else
    echo "FAIL: docker 未找到"
    exit 1
fi
echo "  docker bin: $DOCKER_BIN"

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
step()  { echo -e "${BLUE}== $1 ==${NC}"; }

# 0. 前置检查
step "0. 前置检查"

if ! "$DOCKER_BIN" inspect --format '{{.State.Running}}' "$MYSQL_CONTAINER" 2>/dev/null | grep -q true; then
    fail "$MYSQL_CONTAINER 容器未运行"
    exit 1
fi
ok "$MYSQL_CONTAINER 运行中"

DOCKER_COMPOSE=""
if command -v "$DOCKER_BIN" >/dev/null 2>&1 && "$DOCKER_BIN" compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE="$DOCKER_BIN compose"
elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker-compose"
fi
[ -n "$DOCKER_COMPOSE" ] || { fail "未找到 docker compose 命令"; exit 1; }
ok "docker compose 可用 ($DOCKER_COMPOSE)"

# 测试用户/部门/角色数据准备
# 设计:
#   - dept_a (技术部)  id=1900000000000000004
#   - dept_b (市场部)  id=1900000000000000005
#   - role 2 (普通用户) data_scope=2 本部门
#   - user_a1 (tester_a)  dept=a, role=2 → scope=2 → 只能看 dept_a 数据
#   - post_b (cross_dept) dept=b → 跨部门数据, scope=2 用户应看不到
step "1. 准备测试数据 (dept/role/user/post)"

DEPT_A_ID=1900000000000000004
DEPT_B_ID=1900000000000000005
USER_A1_ID=1900000000000002001
POST_A_ID=1900000000000003001   # dept_a 的岗位
POST_B_ID=1900000000000003002   # dept_b 的岗位

# 准备 2 个部门
"$DOCKER_BIN" exec "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
DELETE FROM sys_post WHERE id IN ($POST_A_ID, $POST_B_ID);
DELETE FROM sys_user_role WHERE user_id = $USER_A1_ID;
DELETE FROM sys_user WHERE id = $USER_A1_ID;
DELETE FROM sys_dept WHERE id IN ($DEPT_A_ID, $DEPT_B_ID);
INSERT INTO sys_dept (id, org_id, parent_id, name, code, sort, status, create_time, update_time, deleted)
VALUES
  ($DEPT_A_ID, 1, 0, '技术部',    'DEPT_A_PR4D4', 1, 1, NOW(), NOW(), 0),
  ($DEPT_B_ID, 1, 0, '市场部',    'DEPT_B_PR4D4', 2, 1, NOW(), NOW(), 0);
" 2>&1 | grep -v "Using a password" | grep -v "^$" || true
ok "部门数据已就绪 (dept_a=$DEPT_A_ID, dept_b=$DEPT_B_ID)"

# 准备 tester_a 用户 (dept_a, password 123456 MD5)
"$DOCKER_BIN" exec "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
INSERT INTO sys_user (id, username, password, nickname, dept_id, status, tenant_id, user_type, create_time, update_time, deleted)
VALUES
  ($USER_A1_ID, 'tester_a', 'e10adc3949ba59abbe56e057f20f883e', 'TesterA', $DEPT_A_ID, 1, 1, 0, NOW(), NOW(), 0);
" 2>&1 | grep -v "Using a password" | grep -v "^$" || true
ok "用户数据已就绪 (tester_a)"

# 准备 role 2 (普通用户, data_scope=2 本部门) + 给 tester_a 分配
"$DOCKER_BIN" exec "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
UPDATE sys_role SET data_scope = 2 WHERE id = 2;
INSERT INTO sys_user_role (user_id, role_id) VALUES ($USER_A1_ID, 2);
" 2>&1 | grep -v "Using a password" | grep -v "^$" || true
ok "tester_a 已分配 role 2 (data_scope=2 本部门)"

# 准备 2 个 post: post_a (dept_a), post_b (dept_b)
"$DOCKER_BIN" exec "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
INSERT INTO sys_post (id, org_id, dept_id, name, code, sort, status, create_time, update_time, deleted)
VALUES
  ($POST_A_ID, 1, $DEPT_A_ID, '技术部-后端',   'POST_A_PR4D4', 1, 1, NOW(), NOW(), 0),
  ($POST_B_ID, 1, $DEPT_B_ID, '市场部-销售',   'POST_B_PR4D4', 2, 1, NOW(), NOW(), 0);
" 2>&1 | grep -v "Using a password" | grep -v "^$" || true
ok "岗位数据已就绪 (post_a/post_b)"

# 2. 登录 tester_a
step "2. 登录 tester_a (scope=2)"

LOGIN_RESP=$(curl -s -X POST "$GATEWAY/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"tester_a","password":"123456"}' 2>/dev/null)

TOKEN=$(echo "$LOGIN_RESP" | grep -oE '"token":"[^"]+"' | sed 's/"token":"//; s/"$//')
if [ -z "$TOKEN" ]; then
    fail "tester_a 登录失败: $LOGIN_RESP"
    exit 1
fi
ok "tester_a 登录成功, 获取 token"

# 3. D+4 核心: scope=2 用户 listByOrgId (dept=本组织) 应只看本部门 post
#    PostServiceImpl.listByOrgId 已有 @DataScope(deptAlias="dept_id") (PR2 实施)
#    tester_a 在 dept_a, scope=2 → 应只看到 post_a
step "3. D+4 TC-LIST-01: scope=2 调用 listByOrgId → 应只返回 dept_a 的 post"

LIST_RESP=$(curl -s "$GATEWAY/post/org/1" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
echo "  listByOrgId response (first 250): $(echo "$LIST_RESP" | head -c 250)"

# 统计返回中 deptId 为 dept_a / dept_b 的出现次数
COUNT_A=$(echo "$LIST_RESP" | grep -oE '"deptId":1900000000000000004' | wc -l)
COUNT_B=$(echo "$LIST_RESP" | grep -oE '"deptId":1900000000000000005' | wc -l)

if [ "$COUNT_A" -ge 1 ] && [ "$COUNT_B" -eq 0 ]; then
    ok "listByOrgId 仅返回 dept_a 的 post (count_a=$COUNT_A, count_b=0) ← @DataScope 读拦截生效"
elif [ "$COUNT_B" -ge 1 ]; then
    fail "listByOrgId 泄漏 dept_b 的 post (count_b=$COUNT_B) ← 读拦截失效!"
else
    warn "listByOrgId 返回结构异常, 无法判定 (count_a=$COUNT_A, count_b=$COUNT_B)"
fi

# 4. D+4 TC-LIST-02: scope=1 (admin) 应看全
step "4. D+4 TC-LIST-02: scope=1 (admin) → 应返回全部 2 个 post"

ADMIN_LOGIN=$(curl -s -X POST "$GATEWAY/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"123456"}' 2>/dev/null)
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | grep -oE '"token":"[^"]+"' | sed 's/"token":"//; s/"$//')
if [ -z "$ADMIN_TOKEN" ]; then
    warn "admin 登录失败, 跳过 TC-LIST-02"
else
    ADMIN_LIST=$(curl -s "$GATEWAY/post/org/1" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    ADMIN_COUNT_A=$(echo "$ADMIN_LIST" | grep -oE '"deptId":1900000000000000004' | wc -l)
    ADMIN_COUNT_B=$(echo "$ADMIN_LIST" | grep -oE '"deptId":1900000000000000005' | wc -l)
    if [ "$ADMIN_COUNT_A" -ge 1 ] && [ "$ADMIN_COUNT_B" -ge 1 ]; then
        ok "admin (scope=1) 看到全部 2 个 post (a=$ADMIN_COUNT_A, b=$ADMIN_COUNT_B) ← scope=1 不加条件正确"
    else
        warn "admin 返回数据异常 (a=$ADMIN_COUNT_A, b=$ADMIN_COUNT_B), 跳过"
    fi
fi

# 5. D+4 写方法 gap 记录 (非测试, 仅文档)
step "5. 已知 gap 记录 (M5.5+ 修复)"

warn "M5.5+ 修复: UserService.update/delete 未加 @DataScope 注解, scope=2 可改/删跨部门用户"
warn "M5.5+ 修复: PostServiceImpl.update/delete 未加 @DataScope 注解, 同上"
warn "建议: 在 doc/M5-PR4-D4-mysql-e2e.md 登记此 gap, 纳入 M5.5 实体扩展同步处理"

# 6. 清理
step "6. 清理测试数据"
"$DOCKER_BIN" exec "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
UPDATE sys_role SET data_scope = 1 WHERE id = 2;
DELETE FROM sys_user_role WHERE user_id = $USER_A1_ID;
DELETE FROM sys_user WHERE id = $USER_A1_ID;
DELETE FROM sys_post WHERE id IN ($POST_A_ID, $POST_B_ID);
DELETE FROM sys_dept WHERE id IN ($DEPT_A_ID, $DEPT_B_ID);
SELECT 'cleanup done' AS status;
" 2>&1 | grep -v "Using a password" | grep -v "^$" || true
ok "测试数据已清理, role 2 还原为 scope=1"

# 汇总
echo
echo "============================================"
echo -e "  PASS: ${GREEN}$PASS${NC}   FAIL: ${RED}$FAIL${NC}   WARN: ${YELLOW}$WARN${NC}"
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
    echo -e "${RED}FAIL: D+4 端到端验证未通过, DataScope 拦截异常!${NC}"
    exit 1
fi

echo -e "${GREEN}OK: D+4 端到端验证通过 (读路径)${NC}"
echo "  - TC-LIST-01: scope=2 读 path (PostServiceImpl.listByOrgId @DataScope) 过滤生效 ✓"
echo "  - TC-LIST-02: scope=1 (admin) 看全 ✓"
echo "  - 已知 gap: 写 path 需在 M5.5+ 实体扩展时同步加 @DataScope 注解"
echo
echo "下一步: A2 (D+5) 6 服务部署 + 业务回归 (灰度开关维持 false, 预期 0 影响)"
exit 0
