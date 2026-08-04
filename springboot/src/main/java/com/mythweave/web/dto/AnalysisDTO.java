package com.mythweave.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalysisDTO {
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

    public String getChapterDisplay() {
        if (chapterTitle != null && !chapterTitle.isEmpty()) {
            return "第" + chapterIndex + "章 " + chapterTitle;
        }
        return "第" + chapterIndex + "章";
    }
}