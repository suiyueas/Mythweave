package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_foreshadowing")
public class NovelForeshadowing extends BaseEntity {
    private Long projectId;
    @NotBlank(message = "伏笔名称不能为空")
    private String name;
    private String description;
    private Long chapterId;
    private String status;
    private Integer passedChapters;
    private String severity;
    private Long resolvedChapterId;
}
