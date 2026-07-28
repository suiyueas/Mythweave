-- =====================================================
-- 恢复被覆盖的章节内容
-- 从 novel_chapter_version 表的最新版本中恢复 content
-- =====================================================

-- 1. 从最新版本恢复章节 3（赤红裂谷低语，v8）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 3
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 3);

-- 2. 章节 4（绝望之城跪求，v1）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 4
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 4);

-- 3. 章节 5（赤红裂谷的誓言，v1）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 5
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 5);

-- 4. 章节 6（先祖之瞳低语，v1）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 6
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 6);

-- 5. 章节 7（虚空裂隙蔓延，v6）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 7
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 7);

-- 6. 章节 10（人类反叛之焰，v15）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 10
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 10);

-- 7. 章节 11（血色祭坛觉醒，v14）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 11
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 11);

-- 8. 章节 12（金色腐化之影，v2）
UPDATE novel_chapter nc
  JOIN novel_chapter_version nv ON nc.id = nv.chapter_id
  SET nc.content = nv.content,
      nc.word_count = nv.word_count,
      nc.version = nv.version
WHERE nc.id = 12
  AND nv.id = (SELECT MAX(id) FROM novel_chapter_version WHERE chapter_id = 12);

-- 验证恢复结果
SELECT id, title, LENGTH(content) AS content_len, word_count, version
FROM novel_chapter WHERE deleted = 0 ORDER BY id;
