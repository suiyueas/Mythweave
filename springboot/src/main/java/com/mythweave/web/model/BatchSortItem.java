package com.mythweave.web.model;

import lombok.Data;

/**
 * 批量排序项（拖拽落地）
 */
@Data
public class BatchSortItem {
    private Long id;
    private Long projectId;
    private Integer sortOrder;
    private Integer nodeNumber;
}
