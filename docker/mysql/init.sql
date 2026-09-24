-- 2026-06-04 修复: 补充 platform_message 库的 GRANT, 否则 platform-message
-- 服务启动后访问 platform_message 库会被拒绝 (500)。
-- 关联问题: KNOWN_ISSUES.md #11
-- 2026-09-24 修复: 补充 platform_kefu 库的创建与授权, kefu-service 连接 MYSQL_DATABASE=platform_kefu
-- 但 init.sql 未创建该库, 导致首次 clean-deploy 启动失败。
CREATE DATABASE IF NOT EXISTS platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS platform_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS platform_nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS platform_kefu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 给 platform 用户授权四个库 (与 docker-compose.yml MYSQL_USER 保持一致)
GRANT ALL PRIVILEGES ON `platform`.*         TO 'platform'@'%';
GRANT ALL PRIVILEGES ON `platform_message`.* TO 'platform'@'%';
GRANT ALL PRIVILEGES ON `platform_nacos`.*   TO 'platform'@'%';
GRANT ALL PRIVILEGES ON `platform_kefu`.*    TO 'platform'@'%';
FLUSH PRIVILEGES;

USE platform;
