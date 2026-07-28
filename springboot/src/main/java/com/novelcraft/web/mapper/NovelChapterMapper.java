package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NovelChapterMapper extends BaseMapper<NovelChapter> {

    /**
     * 查询项目的章节列表（利用覆盖索引，避免回表）
     * 仅返回列表展示所需的字段
     */
    @Select("SELECT id, project_id, volume_id, title, status, word_count, sort_order, version, " +
            "sentinel_status, prev_version_id, create_time, update_time " +
            "FROM novel_chapter " +
            "WHERE project_id = #{projectId} AND deleted = 0 " +
            "ORDER BY sort_order")
    List<NovelChapter> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM novel_chapter WHERE volume_id = #{volumeId} AND deleted = 0 ORDER BY sort_order")
    List<NovelChapter> selectByVolumeId(@Param("volumeId") Long volumeId);

    // 显式按 id + deleted=0 查询，绕过 MyBatis-Plus 全局逻辑删除的不确定性
    @Select("SELECT * FROM novel_chapter WHERE id = #{id} AND deleted = 0")
    NovelChapter selectByIdWithDeleted(@Param("id") Long id);

    // 显式逻辑删除，方法名不以 delete 开头避免 MyBatis-Plus 拦截器劫持
    @Update("UPDATE novel_chapter SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int markDeletedById(@Param("id") Long id);

    /** 统计项目章节数 */
    @Select("SELECT COUNT(*) FROM novel_chapter WHERE project_id = #{projectId} AND deleted = 0")
    Integer countByProject(@Param("projectId") Long projectId);

    /** 获取最近更新的章节 */
    @Select("SELECT * FROM novel_chapter WHERE project_id = #{projectId} AND deleted = 0 ORDER BY update_time DESC LIMIT #{limit}")
    List<NovelChapter> selectRecentByProject(@Param("projectId") Long projectId, @Param("limit") int limit);

    /** 统计项目总字数 */
    @Select("SELECT COALESCE(SUM(word_count), 0) FROM novel_chapter WHERE project_id = #{projectId} AND deleted = 0")
    Integer sumWordCountByProject(@Param("projectId") Long projectId);

    /** 获取最大排序序号（用于计算章节总数和伏笔位置） */
    @Select("SELECT MAX(sort_order) FROM novel_chapter WHERE project_id = #{projectId} AND deleted = 0")
    Integer selectMaxSortOrder(@Param("projectId") Long projectId);

    /** 获取最大排序序号（别名，供 Service 调用） */
    @Select("SELECT MAX(sort_order) FROM novel_chapter WHERE project_id = #{projectId} AND deleted = 0")
    Integer getMaxSortOrder(@Param("projectId") Long projectId);

    /** 按 sort_order 获取指定章节（用于获取上一章内容做衔接） */
    @Select("SELECT * FROM novel_chapter WHERE project_id = #{projectId} AND sort_order = #{sortOrder} AND deleted = 0")
    NovelChapter selectBySortOrder(@Param("projectId") Long projectId, @Param("sortOrder") int sortOrder);

    /** 按日期汇总指定项目的字数（兜底用，当 writing_log 无数据时）
     * 同时匹配 create_time 和 update_time，确保当日新增和当日编辑的章节都被计入 */
    @Select("SELECT COALESCE(SUM(word_count), 0) FROM novel_chapter " +
            "WHERE project_id = #{projectId} AND deleted = 0 " +
            "AND (DATE(create_time) = #{date} OR DATE(update_time) = #{date})")
    Integer sumWordCountByProjectAndDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);

    /** 按日期区间汇总指定项目的字数（兜底用，当 writing_log 无数据时）
     * 同时匹配 create_time 和 update_time */
    @Select("SELECT COALESCE(SUM(word_count), 0) FROM novel_chapter " +
            "WHERE project_id = #{projectId} AND deleted = 0 " +
            "AND ((DATE(create_time) BETWEEN #{start} AND #{end}) OR (DATE(update_time) BETWEEN #{start} AND #{end}))")
    Integer sumWordCountByProjectAndDateRange(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 获取所有已发布的章节（按 sort_order 排序），用于智能哨兵检测 */
    @Select("SELECT * FROM novel_chapter WHERE project_id = #{projectId} AND deleted = 0 AND status = 'COMPLETED' ORDER BY sort_order")
    List<NovelChapter> selectPublishedChapters(@Param("projectId") Long projectId);
}