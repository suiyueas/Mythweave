package com.mythweave.web.common.enums;

import lombok.Getter;

/**
 * 章节状态
 */
@Getter
public enum ChapterStatus {
    DRAFT("草稿"),
    COMPLETED("完成"),
    NEED_REVISION("需修改"),
    LOCKED("已锁定");

    private final String label;

    ChapterStatus(String label) {
        this.label = label;
    }
}
