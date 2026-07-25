-- =====================================================
-- Migration 009: Add chapter_id to sentinel_alert and sentinel_status to chapter
--
-- 功能：支持哨兵告警与具体章节关联，支持章节级别的哨兵状态追踪
-- =====================================================

-- 1. sentinel_alert 表增加 chapter_id 字段（可为空，标记告警关联的具体章节）
ALTER TABLE novel_sentinel_alert
    ADD COLUMN chapter_id BIGINT DEFAULT NULL COMMENT '关联章节ID（可为null表示全局告警）' AFTER project_id;

-- 2. chapter 表增加 sentinel_status 字段（JSON，存储该章节最近一次巡查的告警状态）
ALTER TABLE novel_chapter
    ADD COLUMN sentinel_status JSON DEFAULT NULL COMMENT '哨兵状态：{"passed": true, "alerts": [], "checkedAt": "2026-07-26T10:00:00"}' AFTER version;

-- 3. 创建索引加速按章节查询告警
-- 注：原计划使用 CREATE INDEX ... WHERE（部分索引），但 MySQL 8.0 不支持该语法，故改为普通索引
CREATE INDEX idx_sentinel_alert_chapter ON novel_sentinel_alert(chapter_id);

-- 4. 验证表结构
-- DESCRIBE novel_sentinel_alert;
-- DESCRIBE novel_chapter;