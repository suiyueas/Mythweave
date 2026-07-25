package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_inspiration")
public class NovelInspiration extends BaseEntity {
    private Long projectId;
    private String type;
    private String content;
    private String tags;
    private Long chapterId;
    private String source;
    private Boolean isHighlight;
    private Boolean isUsed;
    private LocalDateTime usedTime;
}
