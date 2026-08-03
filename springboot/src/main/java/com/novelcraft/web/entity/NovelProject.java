package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 作品项目实体类
 * 
 * 对应数据库表：novel_project
 * 
 * 核心属性：
 * - userId: 所属用户ID（必填）
 * - title: 作品标题（必填）
 * - coverUrl: 封面图片URL
 * - description: 作品简介
 * - genre/subGenre: 作品类型和子类型
 * - status: 作品状态（如草稿、进行中、已完成等）
 * - wordCount: 总字数（自动统计）
 * - chapterCount: 章节数（自动统计）
 * - targetWordCount: 目标字数
 * - plannedCompletionDate: 计划完成日期
 * 
 * 内容管理字段：
 * - coreSetting: 核心设定
 * - worldSettings: 世界观设定（JSON格式）
 * - characters: 角色信息（JSON格式）
 * - charactersFormatted: 格式化后的角色信息
 * - outlines: 大纲信息（JSON格式）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_project")
public class NovelProject extends BaseEntity {
    /** 所属用户ID */
    private Long userId;
    
    /** 作品标题 */
    @NotBlank(message = "作品标题不能为空")
    private String title;
    
    /** 封面图片URL */
    private String coverUrl;
    
    /** 作品简介 */
    private String description;
    
    /** 作品类型（如玄幻、都市、科幻等） */
    private String genre;
    
    /** 作品子类型 */
    private String subGenre;
    
    /** 作品状态 */
    private String status;
    
    /** 总字数（自动统计） */
    private Integer wordCount;
    
    /** 章节数（自动统计） */
    private Integer chapterCount;
    
    /** 目标字数 */
    private Integer targetWordCount;
    
    /** 作品标签（逗号分隔） */
    private String tags;
    
    /** 作品起始时间/年代设定 */
    @JsonProperty("startingTime")
    private String startingWorld;
    
    /** 计划完成日期 */
    private LocalDate plannedCompletionDate;
    
    /** 核心设定 */
    private String coreSetting;
    
    /** 世界观设定（JSON格式存储） */
    private String worldSettings;
    
    /** 角色信息（JSON格式存储） */
    private String characters;
    
    /** 格式化后的角色信息（用于前端展示） */
    private String charactersFormatted;
    
    /** 大纲信息（JSON格式存储） */
    private String outlines;
}