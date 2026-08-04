package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.model.DashboardStats;
import com.mythweave.web.model.HeatmapData;
import com.mythweave.web.model.RecentActivity;
import com.mythweave.web.service.DashboardCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@Tag(name = "写作仪表盘")
@RestController
@RequestMapping("/api/projects/{projectId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardCacheService dashboardCacheService;

    @Operation(summary = "获取统计概览（缓存5分钟）")
    @GetMapping("/stats")
    public R<DashboardStats> getStats(@PathVariable Long projectId) {
        try {
            DashboardStats stats = dashboardCacheService.getStats(projectId);
            if (stats == null) stats = DashboardStats.empty();
            return R.ok(stats);
        } catch (Exception e) {
            log.warn("Dashboard stats查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(DashboardStats.empty());
        }
    }

    @Operation(summary = "获取写作热力图数据（缓存1小时）")
    @GetMapping("/heatmap")
    public R<List<HeatmapData>> getHeatmap(@PathVariable Long projectId) {
        try {
            List<HeatmapData> data = dashboardCacheService.getHeatmap(projectId);
            return R.ok(data != null ? data : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Heatmap查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(Collections.emptyList());
        }
    }

    @Operation(summary = "获取最近活动（缓存2分钟）")
    @GetMapping("/activities")
    public R<List<RecentActivity>> getRecentActivities(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<RecentActivity> data = dashboardCacheService.getRecentActivities(projectId, limit);
            return R.ok(data != null ? data : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Activities查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(Collections.emptyList());
        }
    }

    @Operation(summary = "获取本周写作趋势（缓存2分钟）")
    @GetMapping("/weekly-trend")
    public R<List<HeatmapData>> getWeeklyTrend(@PathVariable Long projectId) {
        try {
            List<HeatmapData> data = dashboardCacheService.getWeeklyTrend(projectId);
            return R.ok(data != null ? data : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Weekly trend查询失败(projectId={})", projectId, e);
            return R.ok(Collections.emptyList());
        }
    }
}
