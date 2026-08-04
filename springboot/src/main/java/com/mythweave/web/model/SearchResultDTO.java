package com.mythweave.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 全局搜索结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDTO {
    
    /**
     * 搜索结果ID
     */
    private Long id;
    
    /**
     * 内容类型：chapter/character/world/outline/plot/inspiration/foreshadowing
     */
    private String type;
    
    /**
     * 类型中文标签
     */
    private String typeLabel;
    
    /**
     * 结果标题
     */
    private String title;
    
    /**
     * 结果描述/摘要
     */
    private String description;
    
    /**
     * 匹配的关键词
     */
    private String keyword;
    
    /**
     * 匹配的字段类型：title/name/content/description/personality
     */
    private String matchField;
    
    /**
     * 匹配优先级（1-5，数字越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 匹配内容片段（高亮显示用）
     */
    private String snippet;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 更新时间
     */
    private String updateTime;
    
    /**
     * 关联ID（如章节ID、人物ID等）
     */
    private Long relatedId;
    
    /**
     * 关联类型（如volume、chapter等）
     */
    private String relatedType;
}
