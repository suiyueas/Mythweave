package com.mythweave.web.common.enums;

import lombok.Getter;

/**
 * 作品状态
 */
@Getter
public enum ProjectStatus {
    DRAFT("大纲中"),
    SERIALIZING("连载中"),
    COMPLETED("已完成"),
    PAUSED("暂停"),
    ARCHIVED("已归档");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }
}
