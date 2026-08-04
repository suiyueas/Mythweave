package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_analysis")
public class NovelAnalysis extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long chapterId;
    private String chapterTitle;
    private Integer chapterIndex;
    private String editorResult;
    private String characterResult;
    private String styleResult;
    private String readerResult;
    private String summary;
    private Long totalCostMs;
    private LocalDateTime createTime;
}