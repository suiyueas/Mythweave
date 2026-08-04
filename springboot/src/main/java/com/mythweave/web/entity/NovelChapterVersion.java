package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
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
    /**
     * 版本号（v1、v2...，映射数据库 version 列）
     * 注：BaseEntity 不提供乐观锁 version，此处为章节历史版本自身的版本号，与乐观锁无关
     */
    private String version;
    private String changeNote;
}
