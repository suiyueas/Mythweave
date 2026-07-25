-- =====================================================
-- Data Sync Migration 002: Sync Project Stats & Backfill Writing Logs
-- 
-- Fixes two issues:
-- 1. novel_project 表的 word_count 和 chapter_count 与实际章节不一致
-- 2. novel_writing_log 表缺少已有章节的记录（导致热力图/今日字数/本周累计为 0）
-- 
-- 在运行之前，请务必先确保 novel_chapter 表的数据是正确的（已保存的最新内容）。
-- =====================================================

-- =====================================================
-- 1. 同步 novel_project 的字数和章节数
-- =====================================================
UPDATE novel_project p
JOIN (
    SELECT
        project_id,
        COALESCE(SUM(word_count), 0) AS real_words,
        COUNT(*) AS real_chapters
    FROM novel_chapter
    WHERE deleted = 0
    GROUP BY project_id
) c ON p.id = c.project_id
SET
    p.word_count    = c.real_words,
    p.chapter_count = c.real_chapters,
    p.update_time   = NOW()
WHERE p.deleted = 0;

-- =====================================================
-- 2. 回填缺失的写作日志
-- =====================================================
INSERT INTO novel_writing_log (project_id, chapter_id, date, word_count, writing_duration, create_time, update_time, deleted)
SELECT
    c.project_id,
    c.id AS chapter_id,
    DATE(c.create_time) AS date,
    c.word_count,
    30 AS writing_duration,
    c.create_time,
    c.create_time AS update_time,
    0 AS deleted
FROM novel_chapter c
WHERE
    c.deleted = 0
    AND c.word_count > 0
    AND NOT EXISTS (
        SELECT 1 FROM novel_writing_log w
        WHERE w.project_id = c.project_id
          AND w.chapter_id = c.id
          AND w.deleted = 0
    );

-- =====================================================
-- 3. 验证
-- =====================================================
-- 检查 novel_project 统计
-- SELECT id, title, word_count, chapter_count FROM novel_project WHERE deleted = 0;

-- 检查 writing_log 记录数
-- SELECT COUNT(*) AS log_count FROM novel_writing_log WHERE deleted = 0;

-- 检查是否有章节仍缺少写作日志
-- SELECT COUNT(*) AS missing_logs FROM novel_chapter c
-- WHERE c.deleted = 0 AND c.word_count > 0
--   AND NOT EXISTS (
--     SELECT 1 FROM novel_writing_log w
--     WHERE w.project_id = c.project_id AND w.chapter_id = c.id AND w.deleted = 0
--   );
