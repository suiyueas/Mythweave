package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelWritingLog;
import com.novelcraft.web.model.HeatmapData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NovelWritingLogMapper extends BaseMapper<NovelWritingLog> {
    List<NovelWritingLog> selectByDateRange(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    Integer sumWordsByDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);
    Integer sumWordsBetween(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    Integer sumDurationByDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);
    String getBestHours(@Param("projectId") Long projectId, @Param("since") LocalDate since);
    List<HeatmapData> getDailyWords(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    List<NovelWritingLog> selectRecentByProject(@Param("projectId") Long projectId, @Param("limit") int limit);
    List<HeatmapData> getWeeklyTrend(@Param("projectId") Long projectId);
}