package com.mythweave.web.model;

import lombok.Builder;
import lombok.Data;

/**
 * 哨兵告警统计信息
 * 
 * 用于汇总作品中各类哨兵告警的数量统计
 * 帮助作者快速了解当前作品存在的问题分布
 * 
 * 按问题类型分类：
 * - foreshadowing: 伏笔相关问题
 * - logic: 逻辑一致性问题
 * - character: 角色塑造问题
 * - rhythm: 节奏韵律问题
 * 
 * 按处理状态分类：
 * - resolved: 已处理的告警
 * - unresolved: 未处理的告警
 */
@Data
@Builder
public class SentinelStats {
    
    /** 告警总数 */
    private long total;
    
    /** 伏笔相关问题数 */
    private long foreshadowing;
    
    /** 逻辑一致性问题数 */
    private long logic;
    
    /** 角色塑造问题数 */
    private long character;
    
    /** 节奏韵律问题数 */
    private long rhythm;
    
    /** 已处理的告警数 */
    private long resolved;
    
    /** 未处理的告警数 */
    private long unresolved;
}