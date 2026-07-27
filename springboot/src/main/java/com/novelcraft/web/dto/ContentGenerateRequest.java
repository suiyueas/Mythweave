package com.novelcraft.web.dto;

public class ContentGenerateRequest {
    private Integer chapterIndex;
    private String title;
    private String direction;
    private String existingContent;
    private String style;
    private Integer targetWords;

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