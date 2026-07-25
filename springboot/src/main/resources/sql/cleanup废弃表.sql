-- =====================================================
-- NovelCraft AI 小说创作平台 - 废弃表清理脚本
-- 删除未使用的数据库表
-- 执行前请确保已备份数据库
-- 执行时间：2026年7月
-- =====================================================

USE novelcraft;

-- =====================================================
-- 1. 删除 novel_character_relation 表
-- 说明：人物关系表，代码中未使用
-- 原因：只有实体类和Mapper定义，没有在Service或Controller中被调用
-- =====================================================
DROP TABLE IF EXISTS novel_character_relation;

-- =====================================================
-- 2. 删除 novel_style_profile 表
-- 说明：风格档案表，代码中未使用
-- 原因：只有实体类和Mapper定义，没有在Service或Controller中被调用
-- =====================================================
DROP TABLE IF EXISTS novel_style_profile;

-- =====================================================
-- 清理完成
-- =====================================================
SELECT '废弃表清理完成' AS status;

-- =====================================================
-- 清理说明：
-- 已删除的代码文件：
-- 1. src/main/java/.../entity/NovelCharacterRelation.java
-- 2. src/main/java/.../entity/NovelStyleProfile.java
-- 3. src/main/java/.../mapper/NovelCharacterRelationMapper.java
-- 4. src/main/java/.../mapper/NovelStyleProfileMapper.java
--
-- 已更新的文件：
-- 1. init_novelcraft.sql - 删除了废弃表定义，更新了表编号
-- =====================================================