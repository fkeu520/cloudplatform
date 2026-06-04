-- 2026-06-04 修复: 补充 platform_message 库的 GRANT, 否则 platform-message
-- 服务启动后访问 platform_message 库会被拒绝 (500)。
-- 关联问题: KNOWN_ISSUES.md #11
CREATE DATABASE IF NOT EXISTS platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS platform_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS platform_nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 给 platform 用户授权三个库 (与 docker-compose.yml MYSQL_USER 保持一致)
GRANT ALL PRIVILEGES ON `platform`.*         TO 'platform'@'%';
GRANT ALL PRIVILEGES ON `platform_message`.* TO 'platform'@'%';
GRANT ALL PRIVILEGES ON `platform_nacos`.*   TO 'platform'@'%';
FLUSH PRIVILEGES;

USE platform;
