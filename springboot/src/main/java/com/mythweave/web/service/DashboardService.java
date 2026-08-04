package com.mythweave.web.service;

import com.mythweave.web.entity.NovelChapter;
import com.mythweave.web.entity.NovelProject;
import com.mythweave.web.entity.NovelWritingLog;
import com.mythweave.web.mapper.NovelChapterMapper;
import com.mythweave.web.mapper.NovelProjectMapper;
import com.mythweave.web.mapper.NovelWritingLogMapper;
import com.mythweave.web.model.DashboardStats;
import com.mythweave.web.model.HeatmapData;
import com.mythweave.web.model.RecentActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelWritingLogMapper writingLogMapper;

    private static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");

    /**
     * 获取仪表盘统计概览
     */
    public DashboardStats getStats(Long projectId) {
        NovelProject project = projectMapper.selectById(projectId);
        if (project == null) {
            log.debug("Dashboard stats: project {} not found", projectId);
            return DashboardStats.empty();
        }

        LocalDate today = LocalDate.now(ZONE_CN);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        // 今日字数：优先从 writing_log 查询，无数据时从 chapter 表兜底
        Integer todayWords = writingLogMapper.sumWordsByDate(projectId, today);
        if (todayWords == null || todayWords == 0) {
            todayWords = chapterMapper.sumWordCountByProjectAndDate(projectId, today);
        }
        if (todayWords == null) todayWords = 0;

        // 本周累计：优先从 writing_log 查询，无数据时从 chapter 表兜底
        Integer weekWords = writingLogMapper.sumWordsBetween(projectId, weekStart, today);
        if (weekWords == null || weekWords == 0) {
            weekWords = chapterMapper.sumWordCountByProjectAndDateRange(projectId, weekStart, today);
        }
        if (weekWords == null) weekWords = 0;

        // 今日写作时长（分钟）
        Integer duration = writingLogMapper.sumDurationByDate(projectId, today);
        if (duration == null) duration = 0;

        // 章节数
        Integer chapterCount = chapterMapper.countByProject(projectId);
        if (chapterCount == null) chapterCount = 0;

        // 最佳写作时段（近30天）
        String bestHours = writingLogMapper.getBestHours(projectId, today.minusDays(30));
        if (bestHours == null || bestHours.isEmpty()) bestHours = "--";

        // 平均时速（近7天）
        Double avgSpeed = calcAvgSpeed(projectId, today.minusDays(6));

        return DashboardStats.builder()
                .todayWords(todayWords)
                .weekWords(weekWords)
                .totalWords(project.getWordCount() != null ? project.getWordCount() : 0)
                .writingDuration(duration)
                .chapterCount(chapterCount)
                .targetWords(project.getTargetWordCount() != null ? project.getTargetWordCount() : 0)
                .bestHours(bestHours)
                .avgSpeed(avgSpeed)
                .build();
    }

    /**
     * 获取热力图数据（近 365 天，按年份展示）
     * 优先从 writing_log 获取，不足时从 chapter 表兜底
     */
    public List<HeatmapData> getHeatmap(Long projectId) {
        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);

        // 1. 从 writing_log 获取实际数据
        List<HeatmapData> dbData = writingLogMapper.getDailyWords(projectId, yearStart, today);
        Map<LocalDate, Integer> wordMap = new java.util.HashMap<>();
        if (dbData != null) {
            for (HeatmapData d : dbData) {
                if (d.getDate() != null) {
                    wordMap.merge(d.getDate(), d.getCount() != null ? d.getCount() : 0, Integer::sum);
                }
            }
        }

        // 2. 兜底：从 chapter 表补充 writing_log 中缺失的日期
        try {
            List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);
            Map<LocalDate, Integer> chapterDateMap = new java.util.HashMap<>();
            for (NovelChapter ch : chapters) {
                if (ch.getUpdateTime() != null && ch.getWordCount() != null && ch.getWordCount() > 0) {
                    LocalDate chDate = ch.getUpdateTime().toLocalDate();
                    if (!chDate.isBefore(yearStart)) {
                        chapterDateMap.merge(chDate, ch.getWordCount(), Integer::sum);
                    }
                }
            }
            // 只补充 writing_log 中 count=0 的日期
            for (Map.Entry<LocalDate, Integer> e : chapterDateMap.entrySet()) {
                wordMap.merge(e.getKey(), e.getValue(), (old, newVal) -> old > 0 ? old : newVal);
            }
        } catch (Exception e) {
            log.warn("热力图 chapter 兜底查询失败: {}", e.getMessage());
        }

        // 3. 生成全年的日期列表
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<HeatmapData> result = new ArrayList<>();
        LocalDate cursor = yearStart;
        while (!cursor.isAfter(today)) {
            Integer count = wordMap.getOrDefault(cursor, 0);
            result.add(HeatmapData.builder()
                    .date(cursor)
                    .label(cursor.format(fmt))
                    .count(count)
                    .isFuture(false)
                    .build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * 获取最近活动
     */
    public List<RecentActivity> getRecentActivities(Long projectId, int limit) {
        List<RecentActivity> activities = new ArrayList<>();

        // 最近更新的章节
        List<NovelChapter> recentChapters = chapterMapper.selectRecentByProject(projectId, limit);
        for (NovelChapter ch : recentChapters) {
            activities.add(RecentActivity.ofChapter(ch));
        }

        // 最近写作记录
        List<NovelWritingLog> recentLogs = writingLogMapper.selectRecentByProject(projectId, limit);
        for (NovelWritingLog log : recentLogs) {
            activities.add(RecentActivity.ofLog(log));
        }

        // 按时间倒序，取前 limit 条
        activities.sort((a, b) -> {
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return b.getTime().compareTo(a.getTime());
        });

        if (activities.size() > limit) {
            return activities.subList(0, limit);
        }
        return activities;
    }

    /**
     * 获取本周趋势数据（最近7天按日期聚合字数）
     * 优先从 writing_log 获取，不足时从 chapter 表兜底
     */
    public List<HeatmapData> getWeeklyTrend(Long projectId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        // 1. 从 writing_log 获取
        List<HeatmapData> dbData = writingLogMapper.getWeeklyTrend(projectId);
        Map<LocalDate, Integer> wordMap = new java.util.HashMap<>();
        if (dbData != null) {
            for (HeatmapData d : dbData) {
                if (d.getDate() != null) {
                    wordMap.merge(d.getDate(), d.getCount() != null ? d.getCount() : 0, Integer::sum);
                }
            }
        }

        // 2. 兜底：从 chapter 表补充 writing_log 中缺失的日期
        try {
            List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);
            for (NovelChapter ch : chapters) {
                if (ch.getUpdateTime() != null && ch.getWordCount() != null && ch.getWordCount() > 0) {
                    LocalDate chDate = ch.getUpdateTime().toLocalDate();
                    if (!chDate.isBefore(weekStart)) {
                        wordMap.merge(chDate, ch.getWordCount(), (old, newVal) -> old > 0 ? old : newVal);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("趋势图 chapter 兜底查询失败: {}", e.getMessage());
        }

        // 3. 生成最近7天的数据
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<HeatmapData> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Integer count = wordMap.getOrDefault(date, 0);
            result.add(HeatmapData.builder()
                    .date(date)
                    .label(date.format(fmt))
                    .count(count)
                    .isFuture(false)
                    .build());
        }
        return result;
    }

    /**
     * 计算近N天平均时速（字/小时）
     */
    private Double calcAvgSpeed(Long projectId, LocalDate since) {
        List<NovelWritingLog> logs = writingLogMapper.selectByDateRange(projectId, since, LocalDate.now());
        int totalWords = logs.stream().mapToInt(l -> l.getWordCount() != null ? l.getWordCount() : 0).sum();
        int totalMinutes = logs.stream().mapToInt(l -> l.getWritingDuration() != null ? l.getWritingDuration() : 0).sum();
        if (totalMinutes == 0) return 0.0;
        return Math.round(totalWords * 60.0 / totalMinutes * 10.0) / 10.0;
    }
}
