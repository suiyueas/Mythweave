package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.VipOrder;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VIP 订单 Mapper
 * 提供订单的数据库查询和更新操作
 */
@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {

    /**
     * 查询用户的待支付订单（用于支付去重）
     *
     * @param userId 用户ID
     * @param planId 套餐ID
     * @param now    当前时间，用于判断订单是否过期
     * @return 待支付订单，不存在返回 null
     */
    @Select("SELECT * FROM vip_order WHERE user_id = #{userId} AND plan_id = #{planId} AND status = 'pending' AND expire_at > #{now} LIMIT 1")
    VipOrder findPendingOrder(@Param("userId") Long userId, @Param("planId") String planId, @Param("now") LocalDateTime now);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单对象，不存在返回 null
     */
    @Select("SELECT * FROM vip_order WHERE order_no = #{orderNo} LIMIT 1")
    VipOrder findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询用户最近一笔已支付订单
     *
     * @param userId 用户ID
     * @return 最近已支付订单，不存在返回 null
     */
    @Select("SELECT * FROM vip_order WHERE user_id = #{userId} AND status = 'paid' ORDER BY paid_at DESC LIMIT 1")
    VipOrder findLatestPaidOrder(@Param("userId") Long userId);

    /**
     * 将所有已过期的待支付订单标记为已过期
     * 定时清理任务调用，防止过期订单占用资源
     *
     * @param now 当前时间
     * @return 更新记录数
     */
    @Update("UPDATE vip_order SET status = 'expired' WHERE status = 'pending' AND expire_at < #{now}")
    int expireOrders(@Param("now") LocalDateTime now);

    /**
     * 查询用户所有订单（按创建时间倒序）
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    @Select("SELECT * FROM vip_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<VipOrder> findByUserId(@Param("userId") Long userId);
}