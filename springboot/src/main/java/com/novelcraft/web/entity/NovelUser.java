package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_user")
public class NovelUser extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String bio;
    private String avatar;
    private Boolean emailVerified;
}
