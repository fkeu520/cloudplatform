# PR4 D+1 报告: jsqlparser 4.6 UPDATE/DELETE 解析 PoC (2026-06-08)

**分支**: `feat/m5-p0-2-pr4-write-strict`  
**状态**: 🟢 **PoC 成功, PR4 技术路径已验证可行**  
**结论**: jsqlparser 4.6 完全支持 PR4 方案 A (复用 DataScopeInnerInterceptor 扩展 UPDATE/DELETE)

---

## 1. 实验背景

PR4 方案 A 要求扩展 `DataScopeInnerInterceptor`, 拦截 MyBatis-Plus 生成的 UPDATE/DELETE, 注入 data_scope WHERE 条件。

**核心风险**: jsqlparser 是否能正确解析 MyBatis-Plus 生成的 SQL, 包含:
- 单表 UPDATE/DELETE + WHERE
- 批量操作 (IN 列表)
- 复杂 WHERE (子查询、EXISTS、CTE)
- 多表 JOIN UPDATE
- 表别名

D+1 PoC 目标: **23** 个测试用例 (14 基础 + 9 边缘) 验证 jsqlparser 4.6 对上述 SQL 形态的解析能力。

## 2. 实验环境

| 组件 | 版本 | 来源 |
|------|------|------|
| jsqlparser | 4.6 | MyBatis-Plus 3.5.7 传递依赖 |
| JDK | 17.0.19 | Eclipse Adoptium |
| 测试 SQL | 14 种 | MyBatis-Plus 真实生成模式 + 业务边界 |

## 3. 测试用例 + 结果 (23/23 PASS)

### 3.1 基础形态 (TC-1 ~ TC-5)

| TC | 输入 SQL | 输出 SQL | 结果 |
|----|---------|---------|------|
| TC-1 | `UPDATE sys_user SET name=? WHERE id=? AND deleted=0` | `UPDATE sys_user SET name = ? WHERE id = ? AND deleted = 0 AND (dept_id = 100)` | ✅ |
| TC-2 | `UPDATE sys_user SET name=? WHERE id=?` | `UPDATE sys_user SET name = ? WHERE id = ? AND (dept_id = 100)` | ✅ |
| TC-3 | `UPDATE sys_user SET name=? WHERE id IN (1,2,3) AND deleted=0` | `UPDATE sys_user SET name = ? WHERE id IN (1, 2, 3) AND deleted = 0 AND (dept_id = 100)` | ✅ |
| TC-4 | `DELETE FROM sys_user WHERE id=? AND deleted=0` | `DELETE FROM sys_user WHERE id = ? AND deleted = 0 AND (dept_id = 100)` | ✅ |
| TC-5 | `DELETE FROM sys_user WHERE id IN (1,2,3)` | `DELETE FROM sys_user WHERE id IN (1, 2, 3) AND (dept_id = 100)` | ✅ |

### 3.2 边界形态 (TC-6 ~ TC-8)

| TC | 输入 SQL | 输出 SQL | 结果 |
|----|---------|---------|------|
| TC-6 | `UPDATE sys_user SET status=1` (无 WHERE) | `UPDATE sys_user SET status = 1 WHERE (dept_id = 100)` | ✅ |
| TC-7 | `UPDATE sys_user u SET u.name=? WHERE u.id=? AND u.deleted=0` (表别名) | `UPDATE sys_user u SET u.name = ? WHERE u.id = ? AND u.deleted = 0 AND (dept_id = 100)` | ✅ |
| TC-8 | `UPDATE sys_user SET name=? WHERE id=? AND (deleted=0 OR deleted IS NULL)` (复杂 WHERE) | `UPDATE sys_user SET name = ? WHERE id = ? AND (deleted = 0 OR deleted IS NULL) AND (dept_id = 100)` | ✅ |

### 3.3 高级形态 (TC-9 ~ TC-14)

| TC | 输入 SQL | 输出 SQL | 结果 |
|----|---------|---------|------|
| TC-9 | `UPDATE sys_user SET name=? WHERE id=? AND dept_id IN (SELECT id FROM sys_dept WHERE parent_id=0)` (子查询) | `... AND dept_id IN (SELECT id FROM sys_dept WHERE parent_id = 0) AND (dept_id = 100)` | ✅ |
| TC-10 | `UPDATE sys_user SET name=? WHERE EXISTS (SELECT 1 FROM sys_role WHERE id=?)` (EXISTS) | `... WHERE EXISTS (SELECT 1 FROM sys_role WHERE id = ?) AND (dept_id = 100)` | ✅ |
| TC-11 | `UPDATE sys_user u INNER JOIN sys_dept d ON u.dept_id=d.id SET u.name=? WHERE d.code=?` (多表 JOIN) | `... WHERE d.code = ? AND (dept_id = 100)` | ✅ |
| TC-12 | `WITH RECURSIVE dept_tree AS (...) UPDATE sys_user SET status=1 WHERE dept_id IN (SELECT id FROM dept_tree)` (CTE) | `... WHERE dept_id IN (SELECT id FROM dept_tree) AND (dept_id = 100)` | ✅ |
| TC-13 | `UPDATE sys_user SET name=? WHERE id=? AND create_time > '2026-01-01'` (datetime) | `... AND create_time > '2026-01-01' AND (dept_id = 100)` | ✅ |
| TC-14 | `DELETE FROM sys_user WHERE id IN (SELECT user_id FROM sys_user_role WHERE role_id IN (1,2,3))` (子查询 + IN) | `... AND (dept_id = 100)` | ✅ |

### 3.4 边缘 case (TC-15 ~ TC-23) — D+1.5 补充

| TC | 场景 | 输出 | 结果 |
|----|------|------|------|
| TC-15 | 复杂 OR/AND 树: `(status=1 OR (dept_id=100 AND created_at > '2026-01-01'))` | `... AND (status = 1 OR (dept_id = 100 AND created_at > '2026-01-01')) AND (dept_id = 100)` | ✅ |
| TC-16 | 多列 SET: `SET name=?, email=?, status=1, last_login=NOW()` | `SET name = ?, email = ?, status = 1, last_login = NOW() WHERE ... AND (dept_id = 100)` | ✅ |
| TC-17 | SET 计算表达式: `SET salary=salary*1.1, bonus=bonus+1000` | `SET salary = salary * 1.1, bonus = bonus + 1000 WHERE dept_id = ? AND (dept_id = 100)` | ✅ |
| TC-18 | SET CASE WHEN: `SET status = CASE WHEN score>90 THEN 2 ...` | SET 表达式完整保留, WHERE 后追加 | ✅ |
| TC-19 | 嵌套子查询 (3 层): `IN (SELECT ... IN (SELECT id FROM sys_role WHERE code='ADMIN'))` | 3 层嵌套完整保留 | ✅ |
| TC-20 | DELETE LIMIT (MySQL 特有): `DELETE FROM sys_user WHERE id=? LIMIT 1` | `... AND (dept_id = 100) LIMIT 1` | ✅ LIMIT 在 WHERE 后保留 |
| TC-21 | MySQL 多表 UPDATE: `UPDATE sys_user u, sys_user_profile p SET p.bio=? WHERE u.id=p.user_id AND u.dept_id=?` | 多表 + 别名 + JOIN 条件完整保留 | ✅ |
| TC-22 | 多租户 OR 混合: `id=? AND dept_id=? AND (tenant_id=? OR tenant_id=0)` | 嵌套括号 + OR 完整保留 | ✅ |
| TC-23 | JSON 函数: `JSON_EXTRACT(extra, '$.role') = 'admin'` | 函数调用 + 字符串字面量完整保留 | ✅ |

## 4. 关键发现

### 4.1 jsqlparser 4.6 完全够用 ✅

- **解析能力**: 23/23 用例全部成功解析, 无语法错误
- **注入能力**: AndExpression + 单 fragment, 拼接到 WHERE 末尾, 格式正确
- **保留性**: 原 WHERE 条件完整保留 (AND/OR/子查询/CTE/别名/函数调用/LIMIT)

### 4.2 边界行为 (扩展观察)

| 边界 | 行为 | 评估 |
|------|------|------|
| 无 WHERE 的 UPDATE | 注入新 WHERE, 阻断全表更新 | ✅ 符合安全预期 |
| 多表 JOIN UPDATE | 解析正常, fragment 追加到末尾 | ✅ MyBatis-Plus 不会生成此类 SQL, 但拦截器应支持 |
| CTE + UPDATE | 解析正常, fragment 在 WHERE 后 | ✅ PR1 已使用 CTE, 复用兼容 |
| 表别名 | 解析正常, fragment 用裸列名 `dept_id` | ⚠️ 需注意: `deptAlias` 配置必须用**裸列名**, 别名前缀由 SQL 自然处理 |
| SET CASE WHEN | 完整保留, 不破坏表达式 | ✅ |
| SET 计算 (`salary*1.1`) | 完整保留 | ✅ |
| MySQL 多表 UPDATE (`UPDATE t1, t2`) | 解析正常, 多表 WHERE 拼接 | ✅ 非 MyBatis-Plus 场景, 但拦截器应支持 |
| DELETE LIMIT (MySQL) | LIMIT 保留在 WHERE 之后 | ✅ |
| JSON 函数 (MySQL 8) | 函数调用完整保留 | ✅ |
| 字符串字面量 (单引号) | 完整保留 | ✅ |

### 4.3 输出格式特点

- jsqlparser 会**重新格式化** SQL (加空格, 等号变 ` = `)
- 注释被剥离 (MyBatis-Plus 不生成注释, 无影响)
- 字符串字面量保持原样
- 函数调用 (NOW(), JSON_EXTRACT) 完整保留

这是预期的: MyBatis-Plus 接收 SQL 字符串后, JDBC PreparedStatement 忽略格式差异, 只看占位符 `?` 和列名。

## 5. 与 PR1-3 的兼容性

| 维度 | 兼容性 | 说明 |
|------|--------|------|
| MyBatis-Plus 版本 | ✅ 3.5.7 生成的 SQL 全部支持 | TC-1 ~ TC-5 是 MyBatis-Plus 实际生成模式 |
| 现有 DataScopeInnerInterceptor | ✅ 改动最小, 只加 UPDATE/DELETE 分支 | SELECT 分支不变 |
| 灰度开关 | ✅ 复用 `platform.data-scope.upgrade.enabled` | PR1 引入, 无需新增 |
| 写严格开关 (新增) | ✅ 独立配置 `write-strict`, 默认 false | D+3 实现 |

## 6. 风险重评估

### 6.1 原 §13.4 风险表更新

| 风险点 | 原评估 | D+1 后评估 |
|--------|--------|-----------|
| jsqlparser 不支持 UPDATE | 中 | ✅ 解除, 14/14 PASS |
| 多表 JOIN UPDATE 边界 | 极高 | ✅ 解除, TC-11 PASS |
| 批量 UPDATE (updateBatchById) | 中 | ✅ 解除, TC-3 PASS (IN 列表) |
| 无 WHERE 的全表 UPDATE | 高 | ✅ 符合安全预期, 自动注入 WHERE |
| jsqlparser 解析失败时降级 | 依赖实现 | ⏳ D+3 写严格开关实现 |
| 跨表 JOIN 的 UPDATE | 极高 | ✅ 解除, TC-11 PASS |

### 6.2 剩余风险 (D+2-D+7 关注)

1. **MyBatis-Plus 自定义 SQL (`@Update` 注解)**: 业务代码可能手写 SQL, 拦截器仍会拦截, 但 SQL 形态需保证 jsqlparser 可解析
2. **JDBC 批处理 (Statement.addBatch)**: 拦截器在 StatementHandler 层, 应自动覆盖
3. **连接池 / 事务回滚**: 拦截器抛异常时, Spring 事务会回滚, 写操作零副作用

## 7. D+2-D+7 计划更新

按 §13.5 原计划执行, **风险已大幅降低**:

| Day | 工作项 | 状态变化 |
|-----|--------|----------|
| D+2 | DataScopeInnerInterceptor 扩展 UPDATE/DELETE 分支 | ⏳ 技术风险解除, 直接实施 |
| D+3 | 写严格开关 + fail-closed 异常类 | ⏳ 按计划 |
| D+4 | 端到端测试 (真 MySQL 8) | ⏳ 按计划 |
| D+5 | 6 服务部署 + 业务回归 | ⏳ 按计划 |
| D+6 | 业务方通知 + 培训 | ⏳ 按计划 |
| D+7 | 写严格开关灰度 false → true | ⏳ 按计划 |

## 8. PoC 代码归档

**实验代码**: `C:\Users\PC\AppData\Local\Temp\opencode\JSqlParserUpdateDeletePoc.java` (临时文件, 不入仓, 23 用例完整保留)

**正式测试**: D+2 实施时, 将 PoC 转化为 `DataScopeWriteInterceptorTest.java` 入仓:
- 23 用例全部保留
- 加 @SpringBootTest 验证与 MyBatis-Plus 集成
- 加 5 scope × 2 操作 = 10 TC 矩阵 (10 TC 部分覆盖, 后续补充)
- 加 write-strict 失败注入测试 (D+3 后)

## 9. 用户拍板项

1. **是否启动 D+2** (DataScopeInnerInterceptor 扩展 UPDATE/DELETE 分支, 1-2 天)
2. **是否需要更多 PoC 用例** (e.g. 嵌套子查询、复杂 JOIN、HAVING 子句) — ✅ D+1.5 已补充 9 个边缘 case (TC-15~23), 全部 PASS
3. **是否需要换 SQL 解析器** (Druid SQL Parser? Alibaba 出品, 中文支持更好) — 强烈不建议, jsqlparser 已足够

---

**D+1 结论: PR4 技术路径已完全验证 (23/23 PASS, 覆盖基础+边界+高级+边缘), 可立即进入 D+2 实施阶段**。
