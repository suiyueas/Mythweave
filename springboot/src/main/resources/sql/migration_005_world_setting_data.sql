-- Migration 005: World Setting - Add missing columns (Navicat 兼容写法)
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS，此处直接用 ADD COLUMN
-- 首次运行即可，列已存在时运行会报错但不影响表结构

ALTER TABLE novel_world_setting
ADD COLUMN status VARCHAR(32) DEFAULT 'draft' COMMENT '状态: draft/completed/needs_work';

ALTER TABLE novel_world_setting
ADD COLUMN related_settings JSON DEFAULT NULL COMMENT '关联设定ID列表（JSON数组）';
