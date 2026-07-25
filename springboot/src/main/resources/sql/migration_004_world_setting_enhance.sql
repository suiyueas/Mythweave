-- Migration 004: World Setting Enhancement
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS

ALTER TABLE novel_world_setting
ADD COLUMN status VARCHAR(32) DEFAULT 'draft' COMMENT 'Status: draft, completed, needs_work';

ALTER TABLE novel_world_setting
ADD COLUMN related_settings JSON COMMENT 'Related setting IDs as JSON array';