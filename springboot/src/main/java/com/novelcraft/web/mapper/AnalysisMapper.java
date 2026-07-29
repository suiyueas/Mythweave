package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AnalysisMapper extends BaseMapper<NovelAnalysis> {

    @Select("SELECT * FROM novel_analysis WHERE project_id = #{projectId} ORDER BY create_time DESC")
    List<NovelAnalysis> findByProjectIdOrderByCreateTimeDesc(@Param("projectId") Long projectId);

    @Select("SELECT * FROM novel_analysis WHERE project_id = #{projectId} AND chapter_id = #{chapterId} ORDER BY create_time DESC")
    List<NovelAnalysis> findByProjectIdAndChapterIdOrderByCreateTimeDesc(@Param("projectId") Long projectId, @Param("chapterId") Long chapterId);

    @Select("DELETE FROM novel_analysis WHERE project_id = #{projectId}")
    void deleteByProjectId(@Param("projectId") Long projectId);
}