-- =====================================================
-- Migration 007: Redistribute existing outline nodes across three acts
-- 
-- 将存量大纲节点（act = 'first_act'）按 sort_order 均匀分布到三幕。
-- 使用临时序号表 + 窗口函数，兼容 MySQL 8.0。
-- =====================================================

SET @row_num = 0;

-- 1. 创建临时序号表
DROP TEMPORARY TABLE IF EXISTS tmp_outline_rank;
CREATE TEMPORARY TABLE tmp_outline_rank (
    id BIGINT NOT NULL PRIMARY KEY,
    `rank` INT NOT NULL
);

-- 2. 按 sort_order 写入序号
INSERT INTO tmp_outline_rank (id, `rank`)
SELECT id, @row_num := @row_num + 1 AS `rank`
FROM novel_outline
WHERE deleted = 0 AND act = 'first_act'
ORDER BY sort_order ASC;

-- 3. 计算每幕节点数
SET @total = (SELECT COUNT(*) FROM tmp_outline_rank);
SET @per_act = CEIL(IFNULL(@total, 0) / 3.0);

-- 4. 更新 act（使用临时表驱动）
UPDATE novel_outline o
JOIN tmp_outline_rank r ON o.id = r.id
SET o.act = CASE
    WHEN r.`rank` <= @per_act THEN 'first_act'
    WHEN r.`rank` <= @per_act * 2 THEN 'second_act'
    ELSE 'third_act'
END;

-- 5. 使用窗口函数 ROW_NUMBER() 重建 node_number（幕内序号）
UPDATE novel_outline o
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY act ORDER BY sort_order) AS rn
    FROM novel_outline
    WHERE deleted = 0
) t ON o.id = t.id
SET o.node_number = t.rn
WHERE o.deleted = 0;

-- 6. 清理临时表
DROP TEMPORARY TABLE IF EXISTS tmp_outline_rank;
