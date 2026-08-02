package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_ai_usage")
public class NovelAiUsage extends BaseEntity {
    private Long projectId;
    private Long totalTokens;
    private Double estimatedCost;
    private Integer apiCalls;
    private Integer cacheHitRate;
}