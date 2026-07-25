-- =====================================================
-- Migration 008: Fix Act Distribution for Three-Act Structure
--
-- 问题：第三幕节点（紫晶沙漠的预言、最后的色彩盛宴、巨人的终章、新纪元之光）
-- 被错误地归入第二幕，导致幕区错乱。
--
-- 原因：migration_007 仅处理 act='first_act' 的节点，未重新分配已设置错误 act 的节点。
--
-- 修复方案：重置所有节点的 act 字段，按 sort_order 重新均匀分布到三幕。
-- 执行前请备份数据！
-- =====================================================

-- 0. 确认当前数据状态（可选，执行前先查看）
-- SELECT act, COUNT(*) as cnt FROM novel_outline WHERE deleted = 0 GROUP BY act;

-- 1. 创建临时序号表（按 sort_order 排序）
DROP TEMPORARY TABLE IF EXISTS tmp_act_rank;
CREATE TEMPORARY TABLE tmp_act_rank (
    id BIGINT NOT NULL PRIMARY KEY,
    sort_order_val INT NOT NULL,
    rn INT NOT NULL
);

INSERT INTO tmp_act_rank (id, sort_order_val, rn)
SELECT id, sort_order, ROW_NUMBER() OVER (ORDER BY sort_order ASC) AS rn
FROM novel_outline
WHERE deleted = 0;

-- 2. 计算每幕节点数（总节点数 / 3，向上取整）
SET @total = (SELECT COUNT(*) FROM tmp_act_rank);
SET @per_act = CEIL(IFNULL(@total, 0) / 3.0);

-- 3. 按序号重新分配 act
-- 前 @per_act 个节点 -> first_act
-- 中间 @per_act 个节点 -> second_act
-- 剩余节点 -> third_act
UPDATE novel_outline o
JOIN tmp_act_rank r ON o.id = r.id
SET o.act = CASE
    WHEN r.rn <= @per_act THEN 'first_act'
    WHEN r.rn <= @per_act * 2 THEN 'second_act'
    ELSE 'third_act'
END
WHERE o.deleted = 0;

-- 4. 重建 node_number（幕内序号）
UPDATE novel_outline o
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY act ORDER BY sort_order) AS rn
    FROM novel_outline
    WHERE deleted = 0
) t ON o.id = t.id
SET o.node_number = t.rn
WHERE o.deleted = 0;

-- 5. 清理临时表
DROP TEMPORARY TABLE IF EXISTS tmp_act_rank;

-- 6. 验证修复结果
-- SELECT act, COUNT(*) as cnt, GROUP_CONCAT(title ORDER BY sort_order SEPARATOR ', ') as nodes
-- FROM novel_outline WHERE deleted = 0 GROUP BY act;