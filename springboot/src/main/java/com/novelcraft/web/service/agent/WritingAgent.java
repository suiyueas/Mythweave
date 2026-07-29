package com.novelcraft.web.service.agent;

import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;

/**
 * 写作Agent接口
 */
public interface WritingAgent {
    /**
     * 执行分析
     */
    AgentResult analyze(AgentContext context);

    /**
     * 获取Agent标识
     */
    String getAgentKey();

    /**
     * 获取Agent显示名称
     */
    String getAgentName();
}