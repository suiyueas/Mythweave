package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_writing_log")
public class NovelWritingLog extends BaseEntity {
    private Long projectId;
    private Long chapterId;
    private LocalDate date;
    private Integer wordCount;
    private Integer writingDuration;
}
