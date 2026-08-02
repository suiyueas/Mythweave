package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 作品项目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_project")
public class NovelProject extends BaseEntity {
    private Long userId;
    @NotBlank(message = "作品标题不能为空")
    private String title;
    private String coverUrl;
    private String description;
    private String genre;
    private String subGenre;
    private String status;
    private Integer wordCount;
    private Integer chapterCount;
    private Integer targetWordCount;
    private String tags;
    @JsonProperty("startingTime")
    private String startingWorld;
    private LocalDate plannedCompletionDate;
    private String coreSetting;
    private String worldSettings;
    private String characters;
    private String charactersFormatted;
    private String outlines;
}