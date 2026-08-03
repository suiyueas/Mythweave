package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户信息实体类
 * 
 * 对应数据库表：novel_user
 * 
 * 用户是系统的最顶层实体，支持：
 * - 用户注册与登录（用户名、密码）
 * - 用户个人信息管理（昵称、邮箱、手机、简介、头像）
 * - VIP会员体系（多级别VIP、到期时间追踪）
 * - 权限管理（普通用户、管理员）
 * - 邮箱验证状态追踪
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_user")
public class NovelUser extends BaseEntity {
    /** 用户名（登录凭证） */
    private String username;
    
    /** 密码（加密存储） */
    private String password;
    
    /** 昵称（展示用） */
    private String nickname;
    
    /** 电子邮箱 */
    private String email;
    
    /** 手机号码 */
    private String phone;
    
    /** 个人简介 */
    private String bio;
    
    /** 头像URL */
    private String avatar;
    
    /** 邮箱是否已验证 */
    private Boolean emailVerified;
    
    /** 角色: admin-管理员 user-普通用户 */
    private String role;
    
    /** VIP等级 0-普通 1-白银 2-黄金 3-钻石 */
    private Integer vipLevel;
    
    /** VIP到期时间，null表示未开通或已到期 */
    private LocalDateTime vipExpireAt;
    
    /** 最近一次VIP购买时间 */
    private LocalDateTime vipPurchasedAt;
}