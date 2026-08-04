package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大纲节点实体类
 * 
 * 对应数据库表：novel_outline
 * 
 * 大纲是作品的故事结构规划，支持幕-章节层级：
 * - 幕（Act）：first_act（第一幕）/second_act（第二幕）/third_act（第三幕）
 * - 节点类型（type）：chapter-章节点/plot-情节节点/scene-场景节点
 * - 节点状态：draft（草稿）/pending（待修改）/completed（已完成）
 * - 核心事件标记：isKeyEvent标识是否为故事核心情节点
 * - 关联章节：nodeStatus关联到具体章节ID
 * 
 * 支持排序和预估字数管理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_outline")
public class NovelOutline extends BaseEntity {
    /** 所属作品ID */
    private Long projectId;
    
    /** 父节点ID（用于构建大纲层级结构） */
    private Long parentId;
    
    /** 所属幕: first_act（第一幕）/second_act（第二幕）/third_act（第三幕） */
    private String act;
    
    /** 大纲节点标题 */
    @NotBlank(message = "大纲节点标题不能为空")
    private String title;
    
    /** 大纲节点描述/详细内容 */
    private String description;
    
    /** 节点类型: chapter-章节点/plot-情节节点/scene-场景节点 */
    private String type;
    
    /** 状态: draft（草稿）/pending（待修改）/completed（已完成） */
    private String nodeStatus;
    
    /** 关联的章节ID */
    private Long chapterId;
    
    /** 是否为核心情节点（核心事件会被AI重点关注） */
    private Boolean isKeyEvent;
    
    /** 幕内序号（同一幕内的排序） */
    private Integer nodeNumber;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 预估字数 */
    private Integer estimatedWords;
}