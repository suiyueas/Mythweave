package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP 订单实体
 * 记录用户的VIP购买订单信息
 */
@Data
@TableName("vip_order")
public class VipOrder {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，唯一标识 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 套餐ID：monthly（月度）/ permanent（永久） */
    private String planId;

    /** 套餐名称 */
    private String planName;

    /** 订单金额 */
    private BigDecimal amount;

    /**
     * 订单状态：
     * - pending: 待支付
     * - paid: 已支付
     * - cancelled: 已取消
     * - expired: 已过期
     */
    private String status;

    /**
     * 支付渠道：
     * - alipay: 支付宝
     * - wechat: 微信支付
     */
    private String payChannel;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 订单过期时间（创建后30分钟） */
    private LocalDateTime expireAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}