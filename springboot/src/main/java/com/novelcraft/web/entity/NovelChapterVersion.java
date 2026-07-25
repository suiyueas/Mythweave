package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节历史版本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_chapter_version")
public class NovelChapterVersion extends BaseEntity {
    private Long chapterId;
    private String content;
    private Integer wordCount;
    private String version;
    private String changeNote;
}
