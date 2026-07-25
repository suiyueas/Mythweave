-- =====================================================
-- Cleanup: Remove old duplicate test notifications
--
-- 问题：notification 表中存在 7月21日 的重复测试数据
-- 内容均为 "自动巡查报告提示"，且全已标记为已处理
--
-- 执行前请备份数据！
-- =====================================================

-- 1. 查看当前通知数据状态（执行前先确认）
-- SELECT type, COUNT(*) as cnt, MIN(create_time), MAX(create_time)
-- FROM novel_sentinel_alert
-- WHERE deleted = 0
-- GROUP BY type;

-- 2. 删除旧的、重复的、已处理的占位通知（7月21日的测试数据）
DELETE FROM novel_sentinel_alert
WHERE create_time LIKE '2026-07-21%'
  AND title = '自动巡查报告提示'
  AND resolved = 1
  AND deleted = 0;

-- 3. 如果上述 SQL 无匹配，尝试更宽松的清理（删除所有内容为空的重复通知）
-- DELETE FROM novel_sentinel_alert
-- WHERE title = '自动巡查报告提示'
--   AND description IS NULL
--   AND resolved = 1
--   AND deleted = 0;

-- 4. 验证清理结果
-- SELECT type, COUNT(*) as cnt FROM novel_sentinel_alert WHERE deleted = 0 GROUP BY type;