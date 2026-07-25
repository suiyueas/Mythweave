package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_character")
public class NovelCharacter extends BaseEntity {
    private Long projectId;
    private String name;
    private String role;
    private String type;
    private Integer age;
    private String avatarColor;
    private String description;
    private String personality;
    private String relation;
    private String arcStart;
    private String arcEnd;
    private Integer arcProgress;
    private Integer combat;
    private Integer wisdom;
    private Integer emotion;
    private Integer charm;
    private String lastSeen;
}
