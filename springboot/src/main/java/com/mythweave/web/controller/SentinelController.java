package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.entity.NovelSentinelAlert;
import com.mythweave.web.entity.NovelSentinelCheckLog;
import com.mythweave.web.model.ScanProgress;
import com.mythweave.web.model.SentinelStats;
import com.mythweave.web.service.SentinelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "智能哨兵")
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class SentinelController {

    private final SentinelService sentinelService;

    // ═══ 通知接口（兼容现有前端） ═══

    @Operation(summary = "获取通知列表")
    @GetMapping("/notifications")
    public R<Map<String, Object>> listNotifications(@PathVariable Long projectId,
                                                     @RequestParam(defaultValue = "all") String status,
                                                     @RequestParam(defaultValue = "20") int limit) {
        String type = "all";
        if ("unread".equals(status)) type = "all";
        List<NovelSentinelAlert> list = sentinelService.getAlerts(projectId, type, status, limit);
        long unreadCount = 0;
        for (NovelSentinelAlert a : list) {
            if (!a.getResolved()) unreadCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("unreadCount", unreadCount);
        return R.ok(result);
    }

    @Operation(summary = "标记单个通知已读")
    @PutMapping("/notifications/{id}/read")
    public R<Void> markNotificationRead(@PathVariable Long projectId, @PathVariable Long id) {
        sentinelService.resolveAlert(projectId, id);
        return R.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/notifications/read-all")
    public R<Void> markAllNotificationsRead(@PathVariable Long projectId) {
        List<NovelSentinelAlert> unresolved = sentinelService.getAlerts(projectId, "all", "unresolved", 9999);
        for (NovelSentinelAlert a : unresolved) {
            sentinelService.resolveAlert(projectId, a.getId());
        }
        return R.ok();
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/notifications/{id}")
    public R<Void> deleteNotification(@PathVariable Long projectId, @PathVariable Long id) {
        sentinelService.ignoreAlert(projectId, id);
        return R.ok();
    }

    // ═══ 哨兵接口 ═══

    @Operation(summary = "获取告警统计")
    @GetMapping("/sentinel/stats")
    public R<SentinelStats> stats(@PathVariable Long projectId) {
        return R.ok(sentinelService.getStats(projectId));
    }

    @Operation(summary = "获取告警列表")
    @GetMapping("/sentinel/alerts")
    public R<Map<String, Object>> listAlerts(@PathVariable Long projectId,
                                              @RequestParam(defaultValue = "all") String type,
                                              @RequestParam(defaultValue = "all") String status,
                                              @RequestParam(defaultValue = "20") int limit) {
        String queryStatus = "all".equals(status) ? null : status;
        List<NovelSentinelAlert> list = sentinelService.getAlerts(projectId, type, queryStatus, limit);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return R.ok(result);
    }

    @Operation(summary = "处理告警")
    @PutMapping("/sentinel/alerts/{id}/resolve")
    public R<Void> resolveAlert(@PathVariable Long projectId, @PathVariable Long id) {
        sentinelService.resolveAlert(projectId, id);
        return R.ok();
    }

    @Operation(summary = "忽略告警")
    @PutMapping("/sentinel/alerts/{id}/ignore")
    public R<Void> ignoreAlert(@PathVariable Long projectId, @PathVariable Long id) {
        sentinelService.ignoreAlert(projectId, id);
        return R.ok();
    }

    @Operation(summary = "删除单条告警")
    @DeleteMapping("/sentinel/alerts/{id}")
    public R<Void> deleteAlert(@PathVariable Long projectId, @PathVariable Long id) {
        sentinelService.deleteAlert(projectId, id);
        return R.ok();
    }

    @Operation(summary = "清空所有已处理告警")
    @DeleteMapping("/sentinel/alerts/resolved")
    public R<Void> clearResolvedAlerts(@PathVariable Long projectId) {
        sentinelService.clearResolvedAlerts(projectId);
        return R.ok();
    }

    @Operation(summary = "执行巡查（异步，四维并行扫描）")
    @PostMapping("/sentinel/scan")
    public R<Map<String, Object>> scan(@PathVariable Long projectId) {
        String taskId = UUID.randomUUID().toString();
        sentinelService.startFullScan(projectId, taskId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", "running");
        result.put("message", "四维并行巡查已启动");
        return R.ok(result);
    }

    @Operation(summary = "获取巡查进度")
    @GetMapping("/sentinel/scan/{taskId}/progress")
    public R<ScanProgress> getScanProgress(@PathVariable Long projectId, @PathVariable String taskId) {
        ScanProgress progress = sentinelService.getProgress(taskId);
        if (progress == null) {
            return R.fail("巡查任务不存在或已过期");
        }
        return R.ok(progress);
    }

    @Operation(summary = "获取巡查日志")
    @GetMapping("/sentinel/logs")
    public R<List<NovelSentinelCheckLog>> getLogs(@PathVariable Long projectId,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return R.ok(sentinelService.getRecentLogs(projectId, limit));
    }

    @Operation(summary = "轻量级单章节检查（编辑器用，不保存告警）")
    @GetMapping("/sentinel/check-chapter/{chapterId}")
    public R<List<NovelSentinelAlert>> checkChapterLightweight(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestParam(required = false) String content) {
        List<NovelSentinelAlert> alerts = sentinelService.checkChapterLightweight(chapterId, content);
        return R.ok(alerts);
    }
}