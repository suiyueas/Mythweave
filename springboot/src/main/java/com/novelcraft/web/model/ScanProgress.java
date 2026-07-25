package com.novelcraft.web.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScanProgress {
    private String taskId;
    private String status;
    private int progress;
    private List<DimensionProgress> dimensions;
    private Long elapsedMs;

    @Data
    @Builder
    public static class DimensionProgress {
        private String name;
        private String status;
        private int progress;
        private int alertsFound;
        private String error;
    }
}
