package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_plot_thread")
public class NovelPlotThread extends BaseEntity {
    private Long projectId;
    @NotBlank(message = "情节线名称不能为空")
    private String name;
    private String type;
    private Integer progress;
    private String color;
    private String chapters;
    private String description;
}
