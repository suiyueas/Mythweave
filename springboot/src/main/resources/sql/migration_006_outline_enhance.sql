-- =====================================================
-- Migration 006: Outline Enhancement - Add Act, Status, Chapter Relation, Key Event
-- 
-- 为 novel_outline 新增字段，支持智能大纲模块的幕分组、
-- 状态管理、关联章节和关键事件标记功能。
-- =====================================================

-- 1. 新增 act 字段 - 所属幕
ALTER TABLE novel_outline
    ADD COLUMN act VARCHAR(20) DEFAULT 'first_act' COMMENT '所属幕: first_act/second_act/third_act' AFTER parent_id;

-- 2. 新增 node_status 字段 - 节点状态
ALTER TABLE novel_outline
    ADD COLUMN node_status VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft(草稿)/pending(待修改)/completed(已完成)' AFTER type;

-- 3. 新增 chapter_id 字段 - 关联章节ID
ALTER TABLE novel_outline
    ADD COLUMN chapter_id BIGINT DEFAULT NULL COMMENT '关联章节ID' AFTER node_status;

-- 4. 新增 is_key_event 字段 - 核心情节点标记
ALTER TABLE novel_outline
    ADD COLUMN is_key_event TINYINT(1) DEFAULT 0 COMMENT '是否为核心情节点 0-否 1-是' AFTER chapter_id;

-- 5. 新增 node_number 字段 - 幕内序号
ALTER TABLE novel_outline
    ADD COLUMN node_number INT DEFAULT 0 COMMENT '幕内序号' AFTER is_key_event;
