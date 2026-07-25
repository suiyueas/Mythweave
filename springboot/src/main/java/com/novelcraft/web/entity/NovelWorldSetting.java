package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "novel_world_setting", autoResultMap = true)
public class NovelWorldSetting extends BaseEntity {
    private Long projectId;
    private String name;
    private String category;
    private Integer level;
    private Long parentId;
    private String content;
    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedSettings;
}
