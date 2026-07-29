package com.novelcraft.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent分析结果封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {
    private String agent;
    private String agentName;
    private String content;
    private boolean success;
    private String errorMessage;
    private long costMs;

    public static AgentResult success(String agent, String agentName, String content, long costMs) {
        return AgentResult.builder()
                .agent(agent)
                .agentName(agentName)
                .content(content)
                .success(true)
                .costMs(costMs)
                .build();
    }

    public static AgentResult failure(String agent, String agentName, String errorMessage) {
        return AgentResult.builder()
                .agent(agent)
                .agentName(agentName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}