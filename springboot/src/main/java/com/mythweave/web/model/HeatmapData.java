package com.mythweave.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 写作热力图数据（单日）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapData {
    private LocalDate date;
    private String label;
    private Integer count;
    private Boolean isFuture;
}
