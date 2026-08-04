package com.mythweave.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ContentGenerateRequest {
    private Integer chapterIndex;
    private String title;
    private String direction;
    private String existingContent;
    private String style;

    @Min(value = 100, message = "目标字数不能少于100字")
    @Max(value = 50000, message = "目标字数不能超过50000字")
    private Integer targetWords = 2000;

    public Integer getChapterIndex() { return chapterIndex; }
    public void setChapterIndex(Integer chapterIndex) { this.chapterIndex = chapterIndex; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getExistingContent() { return existingContent; }
    public void setExistingContent(String existingContent) { this.existingContent = existingContent; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public Integer getTargetWords() { return targetWords; }
    public void setTargetWords(Integer targetWords) { this.targetWords = targetWords; }
}