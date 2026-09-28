-- =============================================================================
-- V65: 下线 kefu「知识导入」菜单 (与「知识库」重复)
--
-- 背景:
--   智能问答模块里存在两个功能完全重叠的文档上传入口, 都调用同一个后端接口
--   POST /api/kefu/docs/upload:
--     511 知识库   → views/kefu/Knowledge.vue (platform-kefu-frontend)
--     519 知识导入 → views/kefu/Import.vue   (platform-kefu-frontend)
--
--   逐项比对后确认 519 是纯重复且更不可靠的一个:
--     1. Import.vue 额外提交 category / auto_vectorize 两个表单字段, 但后端
--        upload_document() 签名只有 `file: UploadFile`, FastAPI 静默丢弃二者,
--        所以页面上的「政策/流程/FAQ/业务实体」分类下拉框实际不生效 (数据表
--        里没有 category 列可写), 属于误导性 UI。
--     2. Import.vue 的 accept 声明 .xlsx/.xls/.csv, 但后端
--        config.py allowed_extensions = "pdf,docx,md,txt", document_processor
--        也只实现 pdf/docx/md/txt; 选 Excel/CSV 必然被 400 拒绝。
--     3. 两者的「文档列表 / 导入历史」都走 listDocs(), 是同一份数据。
--     4. Knowledge.vue 的 accept 与后端白名单一致, 是可信的那个。
--
-- 决策: 保留 511 知识库, 下线 519 知识导入。
--
-- 依赖: V57 (注册 517-521), V1 (sys_menu / sys_role_menu)
-- =============================================================================

-- ========== 1. 软删除菜单 519 ==========
-- 沿用本项目 sys_menu.deleted 软删除惯例 (菜单查询普遍带 deleted = 0 过滤),
-- 保留行以便回滚与审计。
UPDATE `sys_menu`
SET `deleted`     = 1,
    `status`      = 0,
    `update_time` = NOW()
WHERE `id` = 519
  AND `deleted` = 0;

-- ========== 2. 清理角色-菜单授权关联 ==========
-- 菜单已下线, 继续授权没有意义; 顺带清掉可避免后续误判权限来源。
DELETE FROM `sys_role_menu` WHERE `menu_id` = 519;

-- ========== 3. 备注该菜单已下线, 便于后人排查 ==========
-- 若 sys_menu 存在 remark 列则写入; 该列在部分版本不存在, 故用 information_schema
-- 守卫避免迁移在老库上失败。
SET @has_remark := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_menu' AND column_name = 'remark'
);
SET @sql := IF(@has_remark > 0,
    "UPDATE `sys_menu` SET `remark` = '2026-09-28 下线: 与「知识库」(511) 功能重复, 统一使用知识库上传文档' WHERE `id` = 519",
    "DO 0");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========== 4. 校验 ==========
-- 预期: 下线后 510 目录下可见子菜单为 517 会话管理 / 518 FAQ管理 / 511 知识库 /
--       512 数据源 / 513 设置 / 520 数据源 / 521 评估, 且不再含 519。
SELECT m.id, m.name, m.path, m.deleted
FROM `sys_menu` m
WHERE m.parent_id = 510 AND m.deleted = 0
ORDER BY m.sort;
