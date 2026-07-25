package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_plot_knowledge_graph")
public class NovelPlotKnowledgeGraph extends BaseEntity {
    private Long projectId;
    private String nodeType;
    private Long sourceId;
    private Long targetId;
    private String relationLabel;
}
