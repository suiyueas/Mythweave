package com.mythweave.web.dto;

import lombok.Data;

/**
 * 协调者请求参数
 */
@Data
public class OrchestratorRequest {
    private String chapterContent;
    private String chapterTitle;
    private Integer chapterIndex;
    private String goldSamples;
    private String readerType;
}