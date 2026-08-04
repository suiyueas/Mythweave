package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_ai_config")
public class NovelAiConfig extends BaseEntity {
    private Long projectId;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private String stylePreset;
    private String customPrompt;
}
