package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.model.DashboardStats;
import com.novelcraft.web.model.HeatmapData;
import com.novelcraft.web.model.RecentActivity;
import com.novelcraft.web.service.DashboardService;
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

    private final DashboardService dashboardService;

    @Operation(summary = "获取统计概览")
    @GetMapping("/stats")
    public R<DashboardStats> getStats(@PathVariable Long projectId) {
        try {
            return R.ok(dashboardService.getStats(projectId));
        } catch (Exception e) {
            log.warn("Dashboard stats查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(DashboardStats.empty());
        }
    }

    @Operation(summary = "获取写作热力图数据")
    @GetMapping("/heatmap")
    public R<List<HeatmapData>> getHeatmap(@PathVariable Long projectId) {
        try {
            return R.ok(dashboardService.getHeatmap(projectId));
        } catch (Exception e) {
            log.warn("Heatmap查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(Collections.emptyList());
        }
    }

    @Operation(summary = "获取最近活动")
    @GetMapping("/activities")
    public R<List<RecentActivity>> getRecentActivities(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            return R.ok(dashboardService.getRecentActivities(projectId, limit));
        } catch (Exception e) {
            log.warn("Activities查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(Collections.emptyList());
        }
    }

    @Operation(summary = "获取本周写作趋势")
    @GetMapping("/weekly-trend")
    public R<List<HeatmapData>> getWeeklyTrend(@PathVariable Long projectId) {
        try {
            return R.ok(dashboardService.getWeeklyTrend(projectId));
        } catch (Exception e) {
            log.warn("Weekly trend查询失败(projectId={})", projectId, e);
            return R.ok(Collections.emptyList());
        }
    }
}
