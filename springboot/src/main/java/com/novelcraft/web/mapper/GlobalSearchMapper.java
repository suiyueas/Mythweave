package com.novelcraft.web.mapper;

import com.novelcraft.web.model.SearchResultDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 全局搜索Mapper
 */
@Mapper
public interface GlobalSearchMapper {

    /**
     * 搜索章节
     */
    @Select("<script>" +
            "SELECT " +
            "  'chapter' as type, " +
            "  '📑 章节' as typeLabel, " +
            "  id, " +
            "  title, " +
            "  content as description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN title = #{keyword} THEN 1 " +
            "    WHEN title LIKE CONCAT('%', #{keyword}, '%') THEN 2 " +
            "    WHEN content LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  CASE " +
            "    WHEN title LIKE CONCAT('%', #{keyword}, '%') THEN 'title' " +
            "    WHEN content LIKE CONCAT('%', #{keyword}, '%') THEN 'content' " +
            "    ELSE 'content' " +
            "  END as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_chapter " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND (title LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "</script>")
    List<SearchResultDTO> searchChapters(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /**
     * 搜索人物
     */
    @Select("<script>" +
            "SELECT " +
            "  'character' as type, " +
            "  '👤 人物' as typeLabel, " +
            "  id, " +
            "  name as title, " +
            "  description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN name = #{keyword} THEN 1 " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 2 " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    WHEN personality LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  CASE " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 'name' " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 'description' " +
            "    WHEN personality LIKE CONCAT('%', #{keyword}, '%') THEN 'personality' " +
            "    ELSE 'description' " +
            "  END as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_character " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR description LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR personality LIKE CONCAT('%', #{keyword}, '%')) " +
            "</script>")
    List<SearchResultDTO> searchCharacters(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /**
     * 搜索世界观
     */
    @Select("<script>" +
            "SELECT " +
            "  'world' as type, " +
            "  '🌍 世界观' as typeLabel, " +
            "  id, " +
            "  name as title, " +
            "  content as description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN name = #{keyword} THEN 1 " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 2 " +
            "    WHEN content LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  CASE " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 'name' " +
            "    WHEN content LIKE CONCAT('%', #{keyword}, '%') THEN 'content' " +
            "    ELSE 'content' " +
            "  END as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_world_setting " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "</script>")
    List<SearchResultDTO> searchWorldSettings(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /**
     * 搜索大纲
     */
    @Select("<script>" +
            "SELECT " +
            "  'outline' as type, " +
            "  '📋 大纲' as typeLabel, " +
            "  id, " +
            "  title, " +
            "  description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN title = #{keyword} THEN 1 " +
            "    WHEN title LIKE CONCAT('%', #{keyword}, '%') THEN 2 " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 5 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  CASE " +
            "    WHEN title LIKE CONCAT('%', #{keyword}, '%') THEN 'title' " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 'description' " +
            "    ELSE 'description' " +
            "  END as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_outline " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND (title LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</script>")
    List<SearchResultDTO> searchOutlines(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /**
     * 搜索情节线
     */
    @Select("<script>" +
            "SELECT " +
            "  'plot' as type, " +
            "  '🎯 情节' as typeLabel, " +
            "  id, " +
            "  name as title, " +
            "  description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN name = #{keyword} THEN 1 " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 2 " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  CASE " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 'name' " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 'description' " +
            "    ELSE 'description' " +
            "  END as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_plot_thread " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</script>")
    List<SearchResultDTO> searchPlotThreads(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /**
     * 搜索灵感
     */
    @Select("<script>" +
            "SELECT " +
            "  'inspiration' as type, " +
            "  '💡 灵感' as typeLabel, " +
            "  id, " +
            "  content as title, " +
            "  content as description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN content = #{keyword} THEN 1 " +
            "    WHEN content LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  'content' as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_inspiration " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND content LIKE CONCAT('%', #{keyword}, '%') " +
            "</script>")
    List<SearchResultDTO> searchInspirations(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /**
     * 搜索伏笔
     */
    @Select("<script>" +
            "SELECT " +
            "  'foreshadowing' as type, " +
            "  '🔗 伏笔' as typeLabel, " +
            "  id, " +
            "  name as title, " +
            "  description, " +
            "  #{keyword} as keyword, " +
            "  CASE " +
            "    WHEN name = #{keyword} THEN 1 " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 2 " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END as priority, " +
            "  CASE " +
            "    WHEN name LIKE CONCAT('%', #{keyword}, '%') THEN 'name' " +
            "    WHEN description LIKE CONCAT('%', #{keyword}, '%') THEN 'description' " +
            "    ELSE 'description' " +
            "  END as matchField, " +
            "  create_time as createTime, " +
            "  update_time as updateTime " +
            "FROM novel_foreshadowing " +
            "WHERE project_id = #{projectId} " +
            "  AND deleted = 0 " +
            "  AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</script>")
    List<SearchResultDTO> searchForeshadowings(@Param("projectId") Long projectId, @Param("keyword") String keyword);
}
