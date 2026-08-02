package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_ai_preset")
public class NovelAiPreset extends BaseEntity {
    private Long projectId;
    private String name;
    private String description;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Boolean isDefault;
}