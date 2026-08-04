package com.mythweave.web.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 基础实体类
 * 
 * 所有业务实体类的基类，提供公共字段和通用功能
 * 
 * 包含字段：
 * - id: 主键（自增类型）
 * - createTime: 创建时间（插入时自动填充）
 * - updateTime: 更新时间（插入和更新时自动填充）
 * - deleted: 逻辑删除标记（0-未删除，1-已删除）
 * 
 * 使用MyBatis-Plus的逻辑删除功能，调用deleteById时不会真正删除数据
 * 而是设置deleted字段为1，通过selectById查询时会自动过滤已删除记录
 */
@Data
public abstract class BaseEntity implements Serializable {
    
    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    @TableLogic(value = "0", delval = "1")
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}