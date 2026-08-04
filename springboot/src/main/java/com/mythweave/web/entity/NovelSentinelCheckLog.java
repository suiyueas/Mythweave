package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("novel_sentinel_check_log")
public class NovelSentinelCheckLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String taskId;
    private String dimension;
    private String scanType;
    private Integer totalChunks;
    private Integer processedChunks;
    private Integer alertsFound;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private Integer durationMs;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
