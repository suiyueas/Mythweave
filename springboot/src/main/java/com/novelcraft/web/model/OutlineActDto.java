package com.novelcraft.web.model;

import lombok.Data;

import java.util.List;

/**
 * 大纲幕（卷）数据传输对象
 * 用于批量保存 AI 生成的大纲：每个幕包含 act 唯一标识、标题与节点列表
 */
@Data
public class OutlineActDto {
    /** 幕唯一标识，如 first_act / second_act / custom_1 */
    private String act;
    /** 幕标题，如「第一幕：建置」 */
    private String title;
    /** 幕描述（可选） */
    private String description;
    /** 幕排序序号 */
    private Integer sortOrder;
    /** 幕下章节节点 */
    private List<OutlineNodeDto> nodes;
}
