-- =====================================================
-- Migration 010: Covering Indexes Optimization
--
-- 功能：创建覆盖索引，减少回表次数，提升列表查询性能
-- =====================================================

-- 1. novel_chapter 覆盖索引
--    覆盖 selectByProjectId 查询的常用字段：project_id, deleted, sort_order, word_count, title, status
--    Extra 中应出现 Using index 表示使用了覆盖索引
CREATE INDEX idx_chapter_list_covering
    ON novel_chapter(project_id, deleted, sort_order, word_count, title, status);

-- 2. novel_chapter 更新时间索引（加速最近章节查询）
CREATE INDEX idx_chapter_update_time
    ON novel_chapter(project_id, deleted, update_time);

-- 3. novel_writing_log 日期覆盖索引（加速仪表盘统计）
CREATE INDEX idx_writing_log_date_covering
    ON novel_writing_log(project_id, date, deleted, word_count, writing_duration);

-- 4. 验证索引
-- EXPLAIN SELECT id, title, word_count, status, sort_order
-- FROM novel_chapter
-- WHERE project_id = 1 AND deleted = 0
-- ORDER BY sort_order;
