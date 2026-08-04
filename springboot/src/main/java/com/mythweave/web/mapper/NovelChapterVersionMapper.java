package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelChapterVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelChapterVersionMapper extends BaseMapper<NovelChapterVersion> {
    List<NovelChapterVersion> selectByChapterId(Long chapterId);

    /**
     * 查询章节版本摘要列表（不加载 content 大字段，按 id 倒序走索引）
     */
    List<NovelChapterVersion> selectSummaryByChapterId(@Param("chapterId") Long chapterId, @Param("limit") int limit);

    /**
     * 查询单个版本详情（含 content 大字段，用于预览/恢复）
     */
    NovelChapterVersion selectDetailById(@Param("id") Long id);
}