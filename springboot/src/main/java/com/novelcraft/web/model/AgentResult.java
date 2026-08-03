package com.novelcraft.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent分析结果封装
 * 
 * 用于统一封装各专业Agent的分析结果
 * 包含Agent标识、分析内容、执行状态、耗时等信息
 * 
 * 使用Builder模式创建实例，提供成功和失败两种便捷工厂方法
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {
    
    /** Agent标识键（如"editor"、"character"） */
    private String agent;
    
    /** Agent显示名称（如"编辑"、"人物"） */
    private String agentName;
    
    /** 分析结果内容 */
    private String content;
    
    /** 执行是否成功 */
    private boolean success;
    
    /** 错误信息（失败时填充） */
    private String errorMessage;
    
    /** 执行耗时（毫秒） */
    private long costMs;

    /**
     * 创建成功结果
     * 
     * @param agent Agent标识键
     * @param agentName Agent显示名称
     * @param content 分析结果内容
     * @param costMs 执行耗时
     * @return 成功状态的AgentResult
     */
    public static AgentResult success(String agent, String agentName, String content, long costMs) {
        return AgentResult.builder()
                .agent(agent)
                .agentName(agentName)
                .content(content)
                .success(true)
                .costMs(costMs)
                .build();
    }

    /**
     * 创建失败结果
     * 
     * @param agent Agent标识键
     * @param agentName Agent显示名称
     * @param errorMessage 错误信息
     * @return 失败状态的AgentResult
     */
    public static AgentResult failure(String agent, String agentName, String errorMessage) {
        return AgentResult.builder()
                .agent(agent)
                .agentName(agentName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}