package com.mythweave.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mythweave.web.mapper.NovelChapterMapper;
import com.mythweave.web.mapper.NovelProjectMapper;
import com.mythweave.web.mapper.NovelWritingLogMapper;
import com.mythweave.web.model.DashboardStats;
import com.mythweave.web.model.HeatmapData;
import com.mythweave.web.model.RecentActivity;
import com.mythweave.web.entity.NovelChapter;
import com.mythweave.web.entity.NovelWritingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 仪表盘缓存服务
 * 为仪表盘统计数据提供 Redis 缓存，减少数据库查询次数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelWritingLogMapper writingLogMapper;

    private static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");
    private static final String KEY_STATS = "dashboard:stats:%d";
    private static final String KEY_HEATMAP = "dashboard:heatmap:%d";
    private static final String KEY_WEEKLY = "dashboard:weekly:%d";
    private static final String KEY_RECENT = "dashboard:recent:%d";
    private static final long STATS_TTL = 5;
    private static final long HEATMAP_TTL = 60;
    private static final long WEEKLY_TTL = 2;
    private static final long RECENT_TTL = 2;

    // ==================== 缓存读取 ====================

    /**
     * 获取仪表盘统计数据（带缓存）
     */
    public DashboardStats getStats(Long projectId) {
        String key = String.format(KEY_STATS, projectId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                DashboardStats stats = objectMapper.readValue(cached, DashboardStats.class);
                log.debug("仪表盘缓存命中, projectId: {}", projectId);
                return stats;
            } catch (Exception e) {
                log.warn("仪表盘缓存解析失败, projectId: {}", projectId);
            }
        }
        log.debug("仪表盘缓存未命中, projectId: {}", projectId);
        DashboardStats stats = loadStatsFromDB(projectId);
        if (stats != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(stats), STATS_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("仪表盘缓存写入失败, projectId: {}", projectId);
            }
        }
        return stats;
    }

    /**
     * 获取热力图数据（带缓存）
     */
    public List<HeatmapData> getHeatmap(Long projectId) {
        String key = String.format(KEY_HEATMAP, projectId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                List<HeatmapData> data = objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, HeatmapData.class));
                log.debug("热力图缓存命中, projectId: {}", projectId);
                return data;
            } catch (Exception e) {
                log.warn("热力图缓存解析失败, projectId: {}", projectId);
            }
        }
        List<HeatmapData> data = loadHeatmapFromDB(projectId);
        if (data != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), HEATMAP_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("热力图缓存写入失败, projectId: {}", projectId);
            }
        }
        return data;
    }

    /**
     * 获取本周趋势（带缓存）
     */
    public List<HeatmapData> getWeeklyTrend(Long projectId) {
        String key = String.format(KEY_WEEKLY, projectId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                List<HeatmapData> data = objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, HeatmapData.class));
                log.debug("趋势图缓存命中, projectId: {}", projectId);
                return data;
            } catch (Exception e) {
                log.warn("趋势图缓存解析失败, projectId: {}", projectId);
            }
        }
        List<HeatmapData> data = loadWeeklyTrendFromDB(projectId);
        if (data != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), WEEKLY_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("趋势图缓存写入失败, projectId: {}", projectId);
            }
        }
        return data;
    }

    /**
     * 获取最近活动（带缓存）
     */
    public List<RecentActivity> getRecentActivities(Long projectId, int limit) {
        String key = String.format(KEY_RECENT, projectId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                List<RecentActivity> data = objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, RecentActivity.class));
                log.debug("活动列表缓存命中, projectId: {}", projectId);
                return data;
            } catch (Exception e) {
                log.warn("活动列表缓存解析失败, projectId: {}", projectId);
            }
        }
        List<RecentActivity> data = loadRecentActivitiesFromDB(projectId, limit);
        if (data != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), RECENT_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("活动列表缓存写入失败, projectId: {}", projectId);
            }
        }
        return data;
    }

    // ==================== 缓存失效 ====================

    /**
     * 失效项目的所有仪表盘缓存（章节保存、设定更新时调用）
     */
    public void invalidate(Long projectId) {
        List<String> keys = Arrays.asList(
                String.format(KEY_STATS, projectId),
                String.format(KEY_HEATMAP, projectId),
                String.format(KEY_WEEKLY, projectId),
                String.format(KEY_RECENT, projectId)
        );
        redisTemplate.delete(keys);
        log.debug("仪表盘缓存已失效, projectId: {}", projectId);
    }

    // ==================== 数据库加载（兜底） ====================

    private DashboardStats loadStatsFromDB(Long projectId) {
        var project = projectMapper.selectById(projectId);
        if (project == null) return DashboardStats.empty();

        LocalDate today = LocalDate.now(ZONE_CN);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        Integer todayWords = writingLogMapper.sumWordsByDate(projectId, today);
        if (todayWords == null || todayWords == 0) {
            todayWords = chapterMapper.sumWordCountByProjectAndDate(projectId, today);
        }
        if (todayWords == null) todayWords = 0;

        Integer weekWords = writingLogMapper.sumWordsBetween(projectId, weekStart, today);
        if (weekWords == null || weekWords == 0) {
            weekWords = chapterMapper.sumWordCountByProjectAndDateRange(projectId, weekStart, today);
        }
        if (weekWords == null) weekWords = 0;

        Integer duration = writingLogMapper.sumDurationByDate(projectId, today);
        if (duration == null) duration = 0;

        Integer chapterCount = chapterMapper.countByProject(projectId);
        if (chapterCount == null) chapterCount = 0;

        String bestHours = writingLogMapper.getBestHours(projectId, today.minusDays(30));
        if (bestHours == null || bestHours.isEmpty()) bestHours = "--";

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

    private List<HeatmapData> loadHeatmapFromDB(Long projectId) {
        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        List<HeatmapData> dbData = writingLogMapper.getDailyWords(projectId, yearStart, today);
        Map<LocalDate, Integer> wordMap = new HashMap<>();
        if (dbData != null) {
            for (HeatmapData d : dbData) {
                if (d.getDate() != null) {
                    wordMap.merge(d.getDate(), d.getCount() != null ? d.getCount() : 0, Integer::sum);
                }
            }
        }
        try {
            List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);
            for (NovelChapter ch : chapters) {
                if (ch.getUpdateTime() != null && ch.getWordCount() != null && ch.getWordCount() > 0) {
                    LocalDate chDate = ch.getUpdateTime().toLocalDate();
                    if (!chDate.isBefore(yearStart)) {
                        wordMap.merge(chDate, ch.getWordCount(), (old, newVal) -> old > 0 ? old : newVal);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("热力图兜底查询失败: {}", e.getMessage());
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<HeatmapData> result = new ArrayList<>();
        LocalDate cursor = yearStart;
        while (!cursor.isAfter(today)) {
            Integer count = wordMap.getOrDefault(cursor, 0);
            result.add(HeatmapData.builder().date(cursor).label(cursor.format(fmt)).count(count).isFuture(false).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<HeatmapData> loadWeeklyTrendFromDB(Long projectId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<HeatmapData> dbData = writingLogMapper.getWeeklyTrend(projectId);
        Map<LocalDate, Integer> wordMap = new HashMap<>();
        if (dbData != null) {
            for (HeatmapData d : dbData) {
                if (d.getDate() != null) {
                    wordMap.merge(d.getDate(), d.getCount() != null ? d.getCount() : 0, Integer::sum);
                }
            }
        }
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
            log.warn("趋势图兜底查询失败: {}", e.getMessage());
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<HeatmapData> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Integer count = wordMap.getOrDefault(date, 0);
            result.add(HeatmapData.builder().date(date).label(date.format(fmt)).count(count).isFuture(false).build());
        }
        return result;
    }

    private List<RecentActivity> loadRecentActivitiesFromDB(Long projectId, int limit) {
        List<RecentActivity> activities = new ArrayList<>();
        List<NovelChapter> recentChapters = chapterMapper.selectRecentByProject(projectId, limit);
        for (NovelChapter ch : recentChapters) {
            activities.add(RecentActivity.ofChapter(ch));
        }
        List<NovelWritingLog> recentLogs = writingLogMapper.selectRecentByProject(projectId, limit);
        for (NovelWritingLog log : recentLogs) {
            activities.add(RecentActivity.ofLog(log));
        }
        activities.sort((a, b) -> {
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return b.getTime().compareTo(a.getTime());
        });
        if (activities.size() > limit) return activities.subList(0, limit);
        return activities;
    }

    private Double calcAvgSpeed(Long projectId, LocalDate since) {
        List<NovelWritingLog> logs = writingLogMapper.selectByDateRange(projectId, since, LocalDate.now());
        int totalWords = logs.stream().mapToInt(l -> l.getWordCount() != null ? l.getWordCount() : 0).sum();
        int totalMinutes = logs.stream().mapToInt(l -> l.getWritingDuration() != null ? l.getWritingDuration() : 0).sum();
        if (totalMinutes == 0) return 0.0;
        return Math.round(totalWords * 60.0 / totalMinutes * 10.0) / 10.0;
    }
}
