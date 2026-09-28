-- =============================================================================
-- V66: 修正 kefu 511-513 菜单 type (叶子菜单被误标为目录)
--
-- 背景:
--   V56 注册 kefu 菜单时, 511 对话 / 512 知识库 / 513 仪表盘 三条记录的 type
--   是按设计写入的 2 (叶子菜单)。但 142 上现网数据显示三者 type 均为 1:
--
--     510 智能问答  /kefu/           Layout            type=1  (目录, 正确)
--     511 对话      /kefu/chat      kefu/Chat         type=1  (错, 应为 2)
--     512 知识库    /kefu/Knowledge kefu/Knowledge    type=1  (错, 应为 2)
--     513 仪表盘    /kefu/Dashboard kefu/Dashboard    type=1  (错, 应为 2)
--     517-521        (V57 注册)                        type=2  (正确)
--
--   type=1 表示目录, type=2 表示叶子菜单。三个页面被当成目录后, 菜单树里它们
--   不再是可点击的页面入口, 点击不会触发路由跳转, 因而对应的
--   Knowledge.vue 根本没有挂载 —— 这与现象吻合: platform-kefu 容器日志中
--   /api/kefu/docs/* 出现 0 次 (连文档列表 GET 都没有), 而同期
--   sessions / messages / data_sources / dashboard 均有正常请求。
--
--   同类问题本项目已修过两次, 均为 type 误设:
--     V64: UPDATE sys_menu SET type=2 WHERE id IN (421,422,423) AND type=1;
--     V57: 明确声明 517-521 用 type=2, 但未回头修正 V56 建的 511-513。
--   推测 511-513 是被更早的手工 SQL 或数据修复脚本改写为 1 的。
--
-- 幂等性: 条件里同时限定 parent_id=510 AND type=1, 只修正确属误设的三条,
--          重复执行不会产生副作用; 若已被其他迁移修好则不匹配任何行。
--
-- 依赖: V56 (注册 510-516)
-- =============================================================================

-- ========== 1. 修正 type ==========
UPDATE `sys_menu`
SET `type`         = 2,
    `update_time`  = NOW()
WHERE `id` IN (511, 512, 513)
  AND `parent_id` = 510
  AND `type`      = 1
  AND `deleted`   = 0;

-- ========== 2. 备注, 便于后人排查 ==========
-- sys_menu.remark 并非所有版本都有, 故先探测列是否存在再动态执行。
SET @has_remark := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_menu' AND column_name = 'remark'
);
SET @sql := IF(@has_remark > 0,
    "UPDATE `sys_menu` SET `remark` = '2026-09-28 修正: 叶子菜单被误设为目录(type=1), 页面无法点击进入' WHERE `id` IN (511,512,513) AND `parent_id` = 510",
    "DO 0");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========== 3. 校验 ==========
-- 预期: 510 为唯一的 type=1 (目录), 511/512/513 与 517-521 均为 type=2。
--       514-516 是挂在 512 下的按钮权限 (type=3), 不在本查询的 parent_id 范围内。
SELECT m.id, m.name, m.path, m.type, m.perms
FROM `sys_menu` m
WHERE m.id = 510 OR m.parent_id = 510
ORDER BY m.type, m.sort, m.id;
