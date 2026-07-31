package com.novelcraft.web.model;

import lombok.Data;

/**
 * 批量排序+幕变更项（跨幕拖拽）
 */
@Data
public class BatchSortActItem {
    private Long id;
    private Long projectId;
    private Long parentId;
    private String act;
    private Integer sortOrder;
    private Integer nodeNumber;
}
