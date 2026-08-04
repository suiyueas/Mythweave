package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节实体类
 * 
 * 对应数据库表：novel_chapter
 * 
 * 章节是作品的核心内容单元，每个章节包含：
 * - 标题和正文内容
 * - 状态管理（草稿、发布等）
 * - 版本控制（支持章节历史版本回溯）
 * - 排序顺序（用于章节排序）
 * 
 * 章节删除采用逻辑删除（@TableLogic），数据可恢复
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_chapter")
public class NovelChapter extends BaseEntity {
    /** 所属作品ID */
    private Long projectId;
    
    /** 章节标题 */
    @NotBlank(message = "章节标题不能为空")
    private String title;
    
    /** 章节正文内容 */
    private String content;
    
    /** 章节状态（如draft-草稿、published-已发布） */
    private String status;
    
    /** 章节字数 */
    private Integer wordCount;
    
    /** 章节排序顺序（数字越小越靠前） */
    private Integer sortOrder;
    
    /**
     * 章节业务版本号（v1、v2...，映射数据库 version 列）
     * 注：BaseEntity 不提供乐观锁 version，此处为章节自身的版本号，与乐观锁无关
     */
    private String version;
    
    /** 前一个版本的ID（用于版本历史追溯） */
    private Long prevVersionId;
}