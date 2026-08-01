package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_chapter")
public class NovelChapter extends BaseEntity {
    private Long projectId;
    @NotBlank(message = "章节标题不能为空")
    private String title;
    private String content;
    private String status;
    private Integer wordCount;
    private Integer sortOrder;
    private String version;
    private Long prevVersionId;
}
