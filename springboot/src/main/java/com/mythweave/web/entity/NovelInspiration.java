package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 灵感记录实体类
 * 
 * 对应数据库表：novel_inspiration
 * 
 * 灵感是创作过程中的随机创意记录：
 * - 内容管理：灵感正文内容
 * - 分类标签：通过type和tags进行分类管理
 * - 来源追踪：记录灵感来源（AI生成/用户原创/其他）
 * - 使用状态：isUsed和usedTime追踪灵感是否已被使用
 * - 高亮标记：isHighlight用于标记重要灵感
 * - 关联章节：可关联到具体章节
 * 
 * 支持创作过程中的灵感收集与管理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_inspiration")
public class NovelInspiration extends BaseEntity {
    /** 所属作品ID */
    private Long projectId;
    
    /** 灵感类型（如character-角色灵感/plot-情节灵感/world-世界观灵感等） */
    private String type;
    
    /** 灵感内容 */
    @NotBlank(message = "灵感内容不能为空")
    private String content;
    
    /** 标签列表（逗号分隔） */
    private String tags;
    
    /** 关联的章节ID */
    private Long chapterId;
    
    /** 灵感来源: ai-AI生成/user-用户原创/other-其他 */
    private String source;
    
    /** 是否为高亮灵感（高亮灵感会在灵感池中优先展示） */
    private Boolean isHighlight;
    
    /** 是否已被使用 */
    private Boolean isUsed;
    
    /** 使用时间 */
    private LocalDateTime usedTime;
}