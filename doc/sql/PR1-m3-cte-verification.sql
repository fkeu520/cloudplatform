-- ============================================
-- PR1 M3 端到端测试: 验证 MySQL 8 WITH RECURSIVE CTE
-- 配套: doc/项目进度.md v7.1 §7.1 (PR1 H2 mock 桩 + 真 MySQL 8)
-- ============================================

-- 清理 (幂等)
DELETE FROM sys_dept WHERE id BETWEEN 99001 AND 99004;

-- 插入测试 dept 树 (4 节点, 跨 3 层)
--   99001 (顶级, parent=0)
--   99002 (子A, parent=99001)
--   99003 (子B, parent=99001)
--   99004 (孙AA, parent=99002)
INSERT INTO sys_dept (id, org_id, parent_id, name, code, sort, status, deleted) VALUES
    (99001, 999, 0,     'PR1-test-T1',  'PR1-T1', 1, 1, 0),
    (99002, 999, 99001, 'PR1-test-T2',  'PR1-T2', 2, 1, 0),
    (99003, 999, 99001, 'PR1-test-T3',  'PR1-T3', 3, 1, 0),
    (99004, 999, 99002, 'PR1-test-T4',  'PR1-T4', 4, 1, 0);

-- 验证插入
SELECT '=== STEP 1: 插入完成, 4 节点 dept 树 ===' AS step;
SELECT id, parent_id, name FROM sys_dept WHERE id BETWEEN 99001 AND 99004 ORDER BY id;

-- ============================================
-- STEP 2: CTE 端到端 SQL 验证
-- ============================================

-- CTE SQL 1: 起点 99001 (顶级) → 应返回 99001, 99002, 99003, 99004 (全部)
SELECT '=== STEP 2-1: CTE 起点=99001 (顶级) ===' AS test;
SELECT '期望: 99001, 99002, 99003, 99004' AS expected;
WITH RECURSIVE dept_tree AS (
    SELECT id FROM sys_dept WHERE id = 99001 AND deleted = 0
    UNION ALL
    SELECT d.id FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT id FROM dept_tree ORDER BY id;

-- CTE SQL 2: 起点 99002 (子A) → 应返回 99002, 99004 (子A + 孙AA)
SELECT '=== STEP 2-2: CTE 起点=99002 (子A, 含本部门) ===' AS test;
SELECT '期望: 99002, 99004' AS expected;
WITH RECURSIVE dept_tree AS (
    SELECT id FROM sys_dept WHERE id = 99002 AND deleted = 0
    UNION ALL
    SELECT d.id FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT id FROM dept_tree ORDER BY id;

-- CTE SQL 3: 起点 99003 (子B, 无子) → 应返回 99003 (只有本部门)
SELECT '=== STEP 2-3: CTE 起点=99003 (子B, 无子部门) ===' AS test;
SELECT '期望: 99003' AS expected;
WITH RECURSIVE dept_tree AS (
    SELECT id FROM sys_dept WHERE id = 99003 AND deleted = 0
    UNION ALL
    SELECT d.id FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT id FROM dept_tree ORDER BY id;

-- CTE SQL 4: 起点 99004 (孙AA, 无子) → 应返回 99004
SELECT '=== STEP 2-4: CTE 起点=99004 (孙AA, 无子) ===' AS test;
SELECT '期望: 99004' AS expected;
WITH RECURSIVE dept_tree AS (
    SELECT id FROM sys_dept WHERE id = 99004 AND deleted = 0
    UNION ALL
    SELECT d.id FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT id FROM dept_tree ORDER BY id;

-- ============================================
-- STEP 3: 软删除过滤验证
-- ============================================
UPDATE sys_dept SET deleted = 1 WHERE id = 99003;
SELECT '=== STEP 3-1: deleted=1 软删除 99003, CTE 应过滤 ===' AS test;
SELECT '期望: 99001, 99002, 99004 (99003 已软删除, 不出现)' AS expected;
WITH RECURSIVE dept_tree AS (
    SELECT id FROM sys_dept WHERE id = 99001 AND deleted = 0
    UNION ALL
    SELECT d.id FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
    WHERE d.deleted = 0
)
SELECT id FROM dept_tree ORDER BY id;
UPDATE sys_dept SET deleted = 0 WHERE id = 99003;

-- ============================================
-- STEP 4: 清理测试数据
-- ============================================
DELETE FROM sys_dept WHERE id BETWEEN 99001 AND 99004;
SELECT '=== STEP 4: 清理完成, 4 测试 dept 已删除 ===' AS cleanup;
SELECT COUNT(*) AS test_dept_remaining FROM sys_dept WHERE id BETWEEN 99001 AND 99004;
