package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户设置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_user_settings")
public class NovelUserSettings extends BaseEntity {
    private Long userId;
    private String settingKey;
    private String settingValue;
}
