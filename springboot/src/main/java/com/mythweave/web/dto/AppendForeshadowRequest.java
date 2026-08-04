package com.mythweave.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "伏笔追加补写请求")
public class AppendForeshadowRequest {

    @NotNull(message = "伏笔ID不能为空")
    @Schema(description = "伏笔ID")
    private Long foreshadowingId;

    @NotBlank(message = "伏笔标题不能为空")
    @Schema(description = "伏笔标题")
    private String foreshadowingTitle;

    @NotBlank(message = "伏笔描述不能为空")
    @Schema(description = "伏笔描述")
    private String foreshadowingDescription;

    @Schema(description = "章节原文")
    private String originalContent;

    @Schema(description = "插入位置: end(末尾) / cursor(光标) / auto(AI智能)", example = "end")
    private String insertPosition = "end";

    @Schema(description = "光标位置(当insertPosition为cursor时使用)")
    private Integer cursorPosition;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "回收章节ID（由 Controller 从路径参数注入）")
    private Long chapterId;
}