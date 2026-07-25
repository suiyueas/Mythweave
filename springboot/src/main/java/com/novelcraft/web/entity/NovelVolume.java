package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分卷�?
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_volume")
public class NovelVolume extends BaseEntity {
    private Long projectId;
    private String title;
    private String description;
    private Integer sortOrder;
}
