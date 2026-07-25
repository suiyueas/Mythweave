package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelWritingLog;
import com.novelcraft.web.model.HeatmapData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NovelWritingLogMapper extends BaseMapper<NovelWritingLog> {
    @Select("SELECT * FROM novel_writing_log WHERE project_id = #{projectId} AND date BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY date")
    List<NovelWritingLog> selectByDateRange(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 按日期汇总字数 */
    @Select("SELECT COALESCE(SUM(word_count), 0) FROM novel_writing_log WHERE project_id = #{projectId} AND date = #{date} AND deleted = 0")
    Integer sumWordsByDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);

    /** 按日期区间汇总字数 */
    @Select("SELECT COALESCE(SUM(word_count), 0) FROM novel_writing_log WHERE project_id = #{projectId} AND date BETWEEN #{start} AND #{end} AND deleted = 0")
    Integer sumWordsBetween(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 按日期汇总写作时长 */
    @Select("SELECT COALESCE(SUM(writing_duration), 0) FROM novel_writing_log WHERE project_id = #{projectId} AND date = #{date} AND deleted = 0")
    Integer sumDurationByDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);

    /** 获取最佳写作时段（近N天内平均字数最高的时间段） */
    @Select("SELECT CONCAT(\n" +
            "    DATE_FORMAT(MIN(create_time), '%H:00'),\n" +
            "    '-',\n" +
            "    DATE_FORMAT(DATE_ADD(MIN(create_time), INTERVAL 1 HOUR), '%H:00')\n" +
            ") AS best_hours\n" +
            "FROM novel_writing_log\n" +
            "WHERE project_id = #{projectId} AND date >= #{since} AND deleted = 0\n" +
            "GROUP BY DATE(create_time), HOUR(create_time)\n" +
            "ORDER BY AVG(word_count) DESC LIMIT 1")
    String getBestHours(@Param("projectId") Long projectId, @Param("since") LocalDate since);

    /** 按日期聚合字数（热力图用） */
    @Select("SELECT date, COALESCE(SUM(word_count), 0) AS count FROM novel_writing_log " +
            "WHERE project_id = #{projectId} AND date BETWEEN #{start} AND #{end} AND deleted = 0 " +
            "GROUP BY date ORDER BY date")
    List<HeatmapData> getDailyWords(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 获取项目最近写作记录 */
    @Select("SELECT * FROM novel_writing_log WHERE project_id = #{projectId} AND deleted = 0 ORDER BY update_time DESC LIMIT #{limit}")
    List<NovelWritingLog> selectRecentByProject(@Param("projectId") Long projectId, @Param("limit") int limit);

    /** 获取本周趋势数据（最近7天按日期聚合字数） */
    @Select("SELECT date, COALESCE(SUM(word_count), 0) AS count FROM novel_writing_log " +
            "WHERE project_id = #{projectId} AND date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND deleted = 0 " +
            "GROUP BY date ORDER BY date")
    List<HeatmapData> getWeeklyTrend(@Param("projectId") Long projectId);
}
