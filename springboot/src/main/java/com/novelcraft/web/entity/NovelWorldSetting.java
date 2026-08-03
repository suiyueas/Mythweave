package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.novelcraft.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 世界观设定实体类
 * 
 * 对应数据库表：novel_world_setting
 * 
 * 用于存储作品的世界观相关设定，支持层级结构：
 * - 支持多级分类（通过parentId实现父子关联）
 * - 设定分类：如地理、历史、文化、政治、军事等
 * - 层级深度控制（level字段）
 * - 关联设定（relatedSettings）用于建立设定之间的关联关系
 * 
 * 使用JSON类型处理器存储关联设定列表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "novel_world_setting", autoResultMap = true)
public class NovelWorldSetting extends BaseEntity {
    /** 所属作品ID */
    private Long projectId;
    
    /** 设定名称 */
    @NotBlank(message = "设定名称不能为空")
    private String name;
    
    /** 设定分类（如geography-地理、history-历史、culture-文化等） */
    private String category;
    
    /** 层级深度（1-顶级、2-二级，以此类推） */
    private Integer level;
    
    /** 父级设定ID（用于构建层级结构，null表示顶级设定） */
    private Long parentId;
    
    /** 设定内容详情 */
    private String content;
    
    /** 设定状态（如active-启用、archived-归档） */
    private String status;
    
    /** 关联的设定ID列表（JSON格式存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedSettings;
}