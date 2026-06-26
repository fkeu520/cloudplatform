-- V50: park-space 旧菜单 icon 字段修正 (2026-06-26)
--
-- 背景:
--   park-space V35 创建了菜单 113/114/115/116, icon 字段写的是 Element Plus 图标名
--   ('Notebook' / 'Lock' / 'Connection' / 'Files').
--   platform-user V36 重建了菜单 (id=200-230 段), 把 icon 改成了 FA class 名
--   ('fas fa-notebook' 等), Layout.vue 第 341 行 `icon.startsWith('fa')` 检测通过 → 正常显示.
--
--   但 park-space 自己的 V36-V49 都没动过 113/116 这几个旧菜单,
--   217 上 park-space 模块独立部署时, 这几个菜单仍带着 Element Plus 名.
--   前端 Layout.vue 查 faIconMap 表, 'Notebook' → 'fas fa-book', 'Files' → 'fas fa-file',
--   理论上应该能渲染 — 但 FA7 已重命名/弃用部分图标, 且前端图标策略不一致.
--
-- 修复:
--   统一这两个菜单 icon 为 FA class 名, 与 platform-user V36 重建后的 211/209 保持一致.
--
-- 根因分类: ON DUPLICATE KEY UPDATE 不更新指定列外的其他列 → 存量数据保留旧值.
-- 教训已记入 KNOWN_ISSUES #32 (扩展: 同样模式适用于 menu 表所有 icon 字段).
--
-- 验证:
--   SELECT id, name, icon FROM sys_menu WHERE id IN (113, 116);
--   应返回 'fas fa-notebook' 和 'fas fa-files'.

-- ========== 1. 房源用途 (113) ==========
UPDATE `sys_menu`
SET `icon` = 'fas fa-notebook'
WHERE `id` = 113
  AND `icon` != 'fas fa-notebook';

-- ========== 2. 拆分合并 (116) ==========
UPDATE `sys_menu`
SET `icon` = 'fas fa-files'
WHERE `id` = 116
  AND `icon` != 'fas fa-files';

-- ========== 3. 顺手: 114/115 也同步, 避免下次再问 ==========
UPDATE `sys_menu`
SET `icon` = 'fas fa-lock'
WHERE `id` = 114
  AND `icon` != 'fas fa-lock';

UPDATE `sys_menu`
SET `icon` = 'fas fa-plug'
WHERE `id` = 115
  AND `icon` != 'fas fa-plug';

-- ========== 4. 验证 ==========
SELECT `id`, `name`, `icon`
FROM `sys_menu`
WHERE `id` IN (113, 114, 115, 116)
ORDER BY `id`;