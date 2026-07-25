package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_outline")
public class NovelOutline extends BaseEntity {
    private Long projectId;
    private Long parentId;
    /** 所属幕: first_act/second_act/third_act */
    private String act;
    private String title;
    private String description;
    private String type;
    /** 状态: draft(草稿)/pending(待修改)/completed(已完成) */
    private String nodeStatus;
    /** 关联章节ID */
    private Long chapterId;
    /** 是否为核心情节点 */
    private Boolean isKeyEvent;
    /** 幕内序号 */
    private Integer nodeNumber;
    private Integer sortOrder;
    private Integer estimatedWords;
}
