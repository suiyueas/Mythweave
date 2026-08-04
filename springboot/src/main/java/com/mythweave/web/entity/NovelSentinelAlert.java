package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_sentinel_alert")
public class NovelSentinelAlert extends BaseEntity {
    private Long projectId;
    private Long chapterId;
    private String type;
    private String title;
    private String description;
    private String severity;
    private String suggestion;
    private Boolean resolved;
}