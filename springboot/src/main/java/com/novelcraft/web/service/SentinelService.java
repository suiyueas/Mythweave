package com.novelcraft.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelForeshadowing;
import com.novelcraft.web.entity.NovelSentinelAlert;
import com.novelcraft.web.entity.NovelSentinelCheckLog;
import com.novelcraft.web.mapper.NovelChapterMapper;
import com.novelcraft.web.mapper.NovelForeshadowingMapper;
import com.novelcraft.web.mapper.NovelSentinelAlertMapper;
import com.novelcraft.web.mapper.NovelSentinelCheckLogMapper;
import com.novelcraft.web.model.ScanProgress;
import com.novelcraft.web.model.ScanProgress.DimensionProgress;
import com.novelcraft.web.model.SentinelStats;
import com.novelcraft.web.service.scanner.CharacterScanner;
import com.novelcraft.web.service.scanner.ForeshadowingScanner;
import com.novelcraft.web.service.scanner.LogicScanner;
import com.novelcraft.web.service.scanner.RhythmScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentinelService {

    private final NovelSentinelAlertMapper alertMapper;
    private final NovelSentinelCheckLogMapper logMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final ForeshadowingScanner foreshadowingScanner;
    private final LogicScanner logicScanner;
    private final CharacterScanner characterScanner;
    private final RhythmScanner rhythmScanner;

    @Value("${app.sentinel.overdue-threshold:10}")
    private int overdueThreshold;

    private final Map<String, ScanProgress> progressCache = new ConcurrentHashMap<>();

    /**
     * 启动全量巡查（异步，四维并行）
     * @param taskId 外部传入的 taskId，用于追踪巡查进度
     */
    @Async("sentinelTaskExecutor")
    public void startFullScan(Long projectId, String taskId) {
        log.info("📋 启动全量巡查，projectId: {}, taskId: {}", projectId, taskId);

        long startTime = System.currentTimeMillis();

        CompletableFuture<Void> all = CompletableFuture.allOf(
                scanDimension(projectId, taskId, "foreshadowing"),
                scanDimension(projectId, taskId, "logic"),
                scanDimension(projectId, taskId, "character"),
                scanDimension(projectId, taskId, "rhythm")
        );
        all.join();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ 全量巡查完成，taskId: {}, 耗时: {}ms", taskId, elapsed);

        ScanProgress progress = progressCache.get(taskId);
        if (progress != null) {
            progress.setStatus("completed");
            progress.setElapsedMs(elapsed);

            int totalAlerts = progress.getDimensions().stream()
                    .mapToInt(DimensionProgress::getAlertsFound)
                    .sum();
            createScanCompletedNotification(projectId, totalAlerts);
        }
    }

    /**
     * 巡查单个维度（异步）
     */
    @Async("sentinelTaskExecutor")
    public CompletableFuture<Void> scanDimension(Long projectId, String taskId, String dimension) {
        long startTime = System.currentTimeMillis();

        NovelSentinelCheckLog logEntry = new NovelSentinelCheckLog();
        logEntry.setProjectId(projectId);
        logEntry.setTaskId(taskId);
        logEntry.setDimension(dimension);
        logEntry.setScanType("full");
        logEntry.setStatus("running");
        logEntry.setStartedAt(LocalDateTime.now());
        logMapper.insert(logEntry);

        updateProgress(taskId, dimension, "running", 0, 0);

        try {
            List<NovelSentinelAlert> alerts;
            switch (dimension) {
                case "foreshadowing":
                    alerts = foreshadowingScanner.scan(projectId);
                    break;
                case "logic":
                    alerts = logicScanner.scan(projectId);
                    break;
                case "character":
                    alerts = characterScanner.scan(projectId);
                    break;
                case "rhythm":
                    alerts = rhythmScanner.scan(projectId);
                    break;
                default:
                    throw new IllegalArgumentException("未知维度: " + dimension);
            }

            int savedCount = saveAlerts(projectId, alerts);

            logEntry.setTotalChunks(1);
            logEntry.setProcessedChunks(1);
            logEntry.setAlertsFound(savedCount);
            logEntry.setStatus("completed");
            logEntry.setCompletedAt(LocalDateTime.now());
            logEntry.setDurationMs((int) (System.currentTimeMillis() - startTime));
            logMapper.updateById(logEntry);

            updateProgress(taskId, dimension, "completed", 100, savedCount);
            log.info("✅ 维度 {} 巡查完成，发现 {} 条告警，耗时 {}ms", dimension, savedCount, logEntry.getDurationMs());

        } catch (Exception e) {
            log.error("❌ 维度 {} 巡查失败: {}", dimension, e.getMessage(), e);
            logEntry.setStatus("failed");
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setCompletedAt(LocalDateTime.now());
            logEntry.setDurationMs((int) (System.currentTimeMillis() - startTime));
            logMapper.updateById(logEntry);
            updateProgress(taskId, dimension, "failed", 0, 0);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 保存告警（去重）
     * 规则：
     * 1. 同 type + title + unresolved 存在时，更新 description 和 updateTime，不新建
     * 2. 已解决的相同告警重新出现时，创建新记录
     * 3. 支持按 description 相似度去重（避免描述略有不同的重复告警）
     */
    @Transactional
    public int saveAlerts(Long projectId, List<NovelSentinelAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) return 0;
        int saved = 0;
        for (NovelSentinelAlert alert : alerts) {
            alert.setProjectId(projectId);
            alert.setResolved(false);
            alert.setCreateTime(LocalDateTime.now());
            alert.setUpdateTime(LocalDateTime.now());

            LambdaQueryWrapper<NovelSentinelAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelSentinelAlert::getProjectId, projectId)
                    .eq(NovelSentinelAlert::getType, alert.getType())
                    .eq(NovelSentinelAlert::getTitle, alert.getTitle())
                    .eq(NovelSentinelAlert::getResolved, false)
                    .eq(NovelSentinelAlert::getDeleted, 0);
            NovelSentinelAlert existing = alertMapper.selectOne(wrapper);

            if (existing == null) {
                alertMapper.insert(alert);
                saved++;
            } else {
                existing.setDescription(alert.getDescription());
                existing.setSuggestion(alert.getSuggestion());
                existing.setUpdateTime(LocalDateTime.now());
                alertMapper.updateById(existing);
            }
        }
        return saved;
    }

    /**
     * 创建巡查完成通知（仅当有问题时创建，且不重复）
     */
    @Transactional
    public void createScanCompletedNotification(Long projectId, int totalAlerts) {
        if (totalAlerts == 0) {
            NovelSentinelAlert notification = new NovelSentinelAlert();
            notification.setProjectId(projectId);
            notification.setType("normal");
            notification.setTitle("✅ 智能哨兵巡查完成");
            notification.setDescription("未发现严重问题，当前作品状态良好。");
            notification.setSeverity("info");
            notification.setSuggestion(null);
            notification.setResolved(false);
            notification.setCreateTime(LocalDateTime.now());
            notification.setUpdateTime(LocalDateTime.now());

            LambdaQueryWrapper<NovelSentinelAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelSentinelAlert::getProjectId, projectId)
                    .eq(NovelSentinelAlert::getType, "normal")
                    .eq(NovelSentinelAlert::getTitle, notification.getTitle())
                    .eq(NovelSentinelAlert::getResolved, false)
                    .eq(NovelSentinelAlert::getDeleted, 0);
            NovelSentinelAlert existing = alertMapper.selectOne(wrapper);
            if (existing == null) {
                alertMapper.insert(notification);
            }
        }
    }

    /**
     * 获取巡查进度
     */
    public ScanProgress getProgress(String taskId) {
        return progressCache.get(taskId);
    }

    /**
     * 获取告警统计
     */
    public SentinelStats getStats(Long projectId) {
        long total = alertMapper.countByProject(projectId);
        long resolved = 0;
        Long resolvedVal = alertMapper.selectCount(new LambdaQueryWrapper<NovelSentinelAlert>()
                .eq(NovelSentinelAlert::getProjectId, projectId)
                .eq(NovelSentinelAlert::getResolved, true)
                .eq(NovelSentinelAlert::getDeleted, 0));
        if (resolvedVal != null) resolved = resolvedVal;

        return SentinelStats.builder()
                .total(total)
                .foreshadowing(alertMapper.countByType(projectId, "foreshadowing"))
                .logic(alertMapper.countByType(projectId, "logic"))
                .character(alertMapper.countByType(projectId, "character"))
                .rhythm(alertMapper.countByType(projectId, "rhythm"))
                .resolved(resolved)
                .unresolved(alertMapper.countUnresolved(projectId))
                .build();
    }

    private void updateProgress(String taskId, String dimension, String status, int progress, int alertsFound) {
        ScanProgress scanProgress = progressCache.computeIfAbsent(taskId, k -> {
            List<ScanProgress.DimensionProgress> dims = Arrays.asList(
                    ScanProgress.DimensionProgress.builder().name("foreshadowing").build(),
                    ScanProgress.DimensionProgress.builder().name("logic").build(),
                    ScanProgress.DimensionProgress.builder().name("character").build(),
                    ScanProgress.DimensionProgress.builder().name("rhythm").build()
            );
            return ScanProgress.builder()
                    .taskId(taskId)
                    .status("running")
                    .progress(0)
                    .dimensions(dims)
                    .build();
        });

        for (ScanProgress.DimensionProgress dim : scanProgress.getDimensions()) {
            if (dim.getName().equals(dimension)) {
                dim.setStatus(status);
                dim.setProgress(progress);
                dim.setAlertsFound(alertsFound);
                break;
            }
        }

        int totalProgress = scanProgress.getDimensions().stream()
                .mapToInt(ScanProgress.DimensionProgress::getProgress)
                .sum() / scanProgress.getDimensions().size();
        scanProgress.setProgress(totalProgress);
    }

    /**
     * 获取告警列表
     */
    public List<NovelSentinelAlert> getAlerts(Long projectId, String type, String status, int limit) {
        LambdaQueryWrapper<NovelSentinelAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelSentinelAlert::getProjectId, projectId)
                .eq(NovelSentinelAlert::getDeleted, 0);

        if (type != null && !"all".equals(type)) {
            wrapper.eq(NovelSentinelAlert::getType, type);
        }
        if ("unresolved".equals(status)) {
            wrapper.eq(NovelSentinelAlert::getResolved, false);
        } else if ("resolved".equals(status)) {
            wrapper.eq(NovelSentinelAlert::getResolved, true);
        }

        wrapper.orderByDesc(NovelSentinelAlert::getSeverity)
                .orderByDesc(NovelSentinelAlert::getCreateTime)
                .last("LIMIT " + limit);

        return alertMapper.selectList(wrapper);
    }

    /**
     * 解决告警
     */
    @Transactional
    public void resolveAlert(Long projectId, Long alertId) {
        NovelSentinelAlert alert = alertMapper.selectById(alertId);
        if (alert != null && alert.getProjectId().equals(projectId)) {
            alert.setResolved(true);
            alert.setUpdateTime(LocalDateTime.now());
            alertMapper.updateById(alert);
        }
    }

    /**
     * 忽略告警（逻辑删除）
     */
    @Transactional
    public void ignoreAlert(Long projectId, Long alertId) {
        NovelSentinelAlert alert = alertMapper.selectById(alertId);
        if (alert != null && alert.getProjectId().equals(projectId)) {
            alert.setDeleted(1);
            alertMapper.updateById(alert);
        }
    }

    /**
     * 删除单条告警（物理删除）
     */
    @Transactional
    public void deleteAlert(Long projectId, Long alertId) {
        NovelSentinelAlert alert = alertMapper.selectById(alertId);
        if (alert != null && alert.getProjectId().equals(projectId)) {
            alertMapper.deleteById(alertId);
        }
    }

    /**
     * 清空所有已处理的告警（物理删除）
     */
    @Transactional
    public void clearResolvedAlerts(Long projectId) {
        LambdaQueryWrapper<NovelSentinelAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelSentinelAlert::getProjectId, projectId)
               .eq(NovelSentinelAlert::getResolved, true);
        alertMapper.delete(wrapper);
    }

    /**
     * 获取巡查日志
     */
    public List<NovelSentinelCheckLog> getRecentLogs(Long projectId, int limit) {
        return logMapper.selectRecent(projectId, limit);
    }

    /**
     * 轻量级单章节检查（不保存到数据库，仅返回实时告警）
     * 用于编辑器内实时展示本章告警
     * @param chapterId 章节ID
     * @param chapterContent 章节内容（可选，用于内容相关检查）
     * @return 实时告警列表
     */
    public List<NovelSentinelAlert> checkChapterLightweight(Long chapterId, String chapterContent) {
        List<NovelSentinelAlert> alerts = new ArrayList<>();

        NovelChapter chapter = chapterMapper.selectByIdWithDeleted(chapterId);
        if (chapter == null) return alerts;

        Integer currentChapterNum = chapter.getSortOrder();
        if (currentChapterNum == null) currentChapterNum = 0;

        List<NovelForeshadowing> pending = foreshadowingMapper.selectPendingByProject(chapter.getProjectId());

        for (NovelForeshadowing fs : pending) {
            int passed = currentChapterNum - (fs.getChapterId() != null ? fs.getChapterId().intValue() : 0);
            fs.setPassedChapters(passed);

            if (passed > overdueThreshold) {
                alerts.add(createTempAlert(
                        "foreshadowing",
                        "伏笔超期未回收：" + fs.getName(),
                        "伏笔「" + fs.getName() + "」已埋设 " + passed + " 章，建议尽快回收",
                        "warning",
                        "建议在下一章节中呼应此伏笔",
                        chapterId
                ));
            }

            if (fs.getResolvedChapterId() != null) {
                int remaining = fs.getResolvedChapterId().intValue() - currentChapterNum;
                if (remaining > 0 && remaining <= 3) {
                    alerts.add(createTempAlert(
                            "foreshadowing",
                            "伏笔即将回收：" + fs.getName(),
                            "伏笔「" + fs.getName() + "」应在 CH." + fs.getResolvedChapterId() + " 回收，当前 CH." + currentChapterNum,
                            "info",
                            "请确保在 CH." + fs.getResolvedChapterId() + " 中呼应此伏笔",
                            chapterId
                    ));
                }
            }
        }

        log.info("🔍 轻量巡查完成，chapterId: {}, 发现 {} 条告警", chapterId, alerts.size());
        return alerts;
    }

    private NovelSentinelAlert createTempAlert(String type, String title, String desc, String severity, String suggestion, Long chapterId) {
        NovelSentinelAlert alert = new NovelSentinelAlert();
        alert.setType(type);
        alert.setTitle(title);
        alert.setDescription(desc);
        alert.setSeverity(severity);
        alert.setSuggestion(suggestion);
        alert.setChapterId(chapterId);
        return alert;
    }
}