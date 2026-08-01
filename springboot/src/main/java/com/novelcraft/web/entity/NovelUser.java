package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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
    /** 角色: admin-管理员 user-普通用户 */
    private String role;
    /** VIP等级 0-普通 1-白银 2-黄金 3-钻石 */
    private Integer vipLevel;
    /** VIP到期时间，null 表示未开通 */
    private LocalDateTime vipExpireAt;
    /** 最近一次VIP购买时间 */
    private LocalDateTime vipPurchasedAt;
}
