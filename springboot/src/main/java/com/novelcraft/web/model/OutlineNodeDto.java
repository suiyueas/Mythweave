package com.novelcraft.web.model;

import lombok.Data;

/**
 * 大纲章节节点数据传输对象
 * 用于批量保存 AI 生成的大纲节点
 */
@Data
public class OutlineNodeDto {
    /** 章节标题 */
    private String title;
    /** 章节摘要/描述 */
    private String description;
    /** 关键事件 */
    private String keyEvent;
    /** 幕内序号 */
    private Integer sortOrder;
    /** 节点类型: volume/chapter/scene */
    private String type;
    /** 状态: draft/pending/completed */
    private String status;
    /** 预估字数 */
    private Integer estimatedWords;
}
