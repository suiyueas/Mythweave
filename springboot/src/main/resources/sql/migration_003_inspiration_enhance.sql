-- =====================================================
-- Migration 003: 灵感素材表扩展字段
-- 新增关联章节、来源、高亮、已使用等字段
-- =====================================================

ALTER TABLE novel_inspiration
    ADD COLUMN IF NOT EXISTS chapter_id   BIGINT      DEFAULT NULL COMMENT '关联章节ID' AFTER tags,
    ADD COLUMN IF NOT EXISTS source       VARCHAR(20) DEFAULT 'manual' COMMENT '来源: manual/ai' AFTER chapter_id,
    ADD COLUMN IF NOT EXISTS is_highlight TINYINT(1)  DEFAULT 0 COMMENT '是否高亮' AFTER source,
    ADD COLUMN IF NOT EXISTS is_used      TINYINT(1)  DEFAULT 0 COMMENT '是否已使用' AFTER is_highlight,
    ADD COLUMN IF NOT EXISTS used_time    DATETIME    DEFAULT NULL COMMENT '使用时间' AFTER is_used;

-- 新增索引
ALTER TABLE novel_inspiration
    ADD INDEX IF NOT EXISTS idx_ins_type (type),
    ADD INDEX IF NOT EXISTS idx_ins_chapter (chapter_id),
    ADD INDEX IF NOT EXISTS idx_ins_used (is_used),
    ADD INDEX IF NOT EXISTS idx_ins_highlight (is_highlight);
