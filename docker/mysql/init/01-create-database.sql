-- 创建数据库（如果application.yml配置了自动建表，此步骤可选）
CREATE DATABASE IF NOT EXISTS nianhua DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授权给nianhua用户（如果使用非root用户）
-- GRANT ALL PRIVILEGES ON nianhua.* TO 'nianhua'@'%';
-- FLUSH PRIVILEGES;
