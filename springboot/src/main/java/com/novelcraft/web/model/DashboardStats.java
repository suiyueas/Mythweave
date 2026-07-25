package com.novelcraft.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘统计概览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Integer todayWords;
    private Integer weekWords;
    private Integer totalWords;
    private Integer writingDuration;
    private Integer chapterCount;
    private Integer targetWords;
    private String bestHours;
    private Double avgSpeed;

    public static DashboardStats empty() {
        return DashboardStats.builder()
                .todayWords(0)
                .weekWords(0)
                .totalWords(0)
                .writingDuration(0)
                .chapterCount(0)
                .targetWords(0)
                .bestHours("--")
                .avgSpeed(0.0)
                .build();
    }
}
