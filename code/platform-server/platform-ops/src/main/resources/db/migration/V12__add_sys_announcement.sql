-- ================================================================
-- V12: 新增 sys_announcement 表 (首页工作台公告卡片数据源)
--
-- W3 设计:
-- - 全局公告 (app_code IS NULL): 所有用户可见, 显示在工作台首页
-- - 应用公告 (app_code='xxx'): 仅在该 app tab 下可见
-- - 优先级 priority DESC, publish_time DESC
-- - expire_time: 可空, 过期不显示 (应用层过滤, 不靠定时任务)
-- - tenant_id: 多租户隔离 (NULL = 全租户可见)
--
-- W3 阶段范围:
-- - 建表 + 3 条 seed (1 全局 + 1 platform + 1 park-space)
-- - SysAnnouncement entity + Service + Controller + /announcement/recent 端点
-- - 后续 W4: 完整 CRUD + UI 管理页 (App.vue 类似)
-- ================================================================

CREATE TABLE IF NOT EXISTS `sys_announcement` (
    `id` bigint NOT NULL COMMENT '主键 (雪花 ID)',
    `title` varchar(200) NOT NULL COMMENT '公告标题',
    `content` text COMMENT '公告内容 (Markdown 或纯文本)',
    `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型: 0=平台公告 1=业务公告 2=维护通知',
    `app_code` varchar(50) DEFAULT NULL COMMENT '限定应用 (NULL=全局), 关联 sys_app.app_code',
    `priority` int NOT NULL DEFAULT '0' COMMENT '优先级 (越大越靠前)',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0=启用 1=禁用',
    `publish_time` datetime DEFAULT NULL COMMENT '发布时间 (NULL=立即)',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间 (NULL=永不过期)',
    `tenant_id` bigint DEFAULT NULL COMMENT '租户ID (NULL=全租户)',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记 (0=正常 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_app_code_status` (`app_code`, `status`, `deleted`),
    KEY `idx_priority_time` (`priority` DESC, `publish_time` DESC, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告 (首页工作台数据源)';

-- ================================================================
-- Seed: 3 条示例公告
-- ================================================================

-- 1. 全局公告: v7.5 平台升级
INSERT INTO `sys_announcement` (`id`, `title`, `content`, `type`, `app_code`, `priority`, `status`, `publish_time`)
VALUES (
    1900000000000000101,
    '【v7.5 平台升级】新增首页工作台 + 顶部菜单动态权限',
    'v7.5 上线: 1) 首页工作台(待办/消息/快捷入口/公告) 2) 顶部菜单按用户权限动态渲染 3) csyh 园区业务融合 Phase -1 W2 完成 (park-common 模块就绪)',
    0,
    NULL,
    100,
    0,
    '2026-06-15 19:00:00'
);

-- 2. platform 应用公告: 菜单结构调整
INSERT INTO `sys_announcement` (`id`, `title`, `content`, `type`, `app_code`, `priority`, `status`, `publish_time`)
VALUES (
    1900000000000000102,
    '【系统管理】菜单结构调整 W3 启动',
    'W3 阶段将把现有单列菜单改为 "顶部 tabs + 左侧菜单" 上左两栏布局。顶部 tab 按用户角色/权限动态显示,无权限的人看不到对应顶部菜单。详情见 doc/plan/菜单结构调整-w3.md',
    0,
    'system',
    50,
    0,
    '2026-06-15 18:30:00'
);

-- 3. park-space 应用公告: 业务融合占位 (csyh Phase 0 即将启动)
INSERT INTO `sys_announcement` (`id`, `title`, `content`, `type`, `app_code`, `priority`, `status`, `publish_time`)
VALUES (
    1900000000000000103,
    '【空间中心】csyh 业务融合 Phase 0 即将启动',
    '园区业务融合 Phase 0: 空间中心 (park-space) 模块即将上线,提供房源/楼宇/楼层/工位 CRUD + 平面图展示。当前 W2 公共层已完成,正在准备 Phase 0 业务翻译。',
    1,
    'park-space',
    30,
    0,
    '2026-06-15 18:00:00'
);