-- 用户认证和数据隔离迁移脚本
-- 运行前请备份数据库

-- 1. 为 novel_project 表添加 user_id 字段
ALTER TABLE novel_project ADD COLUMN user_id BIGINT;
ALTER TABLE novel_project ADD INDEX idx_user_id (user_id);

-- 2. 为已有作品设置默认 user_id（如果需要迁移数据）
-- 注意：如果已有数据需要分配给特定用户，请先处理用户数据
-- UPDATE novel_project SET user_id = 1 WHERE user_id IS NULL;

-- 3. 确保 novel_user 表有正确的数据（如果需要测试）
-- INSERT INTO novel_user (username, password, nickname, email, create_time, deleted)
-- VALUES ('test', '$2a$10$...', '测试用户', 'test@example.com', NOW(), 0);