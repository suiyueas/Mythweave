package com.mythweave.web.service.agent;

import com.mythweave.web.model.AgentContext;
import com.mythweave.web.model.AgentResult;

/**
 * 写作Agent接口
 * 
 * 定义所有专业写作Agent的统一接口
 * 不同的Agent实现负责不同的专业分析领域：
 * - EditorAgent：章节编辑质量分析
 * - CharacterAgent：角色塑造分析
 * - StyleAgent：文风特点分析
 * - ReaderAgent：读者体验分析
 */
public interface WritingAgent {
    
    /**
     * 执行分析任务
     * @param context Agent执行所需的上下文信息（项目、章节、角色等）
     * @return Agent分析结果
     */
    AgentResult analyze(AgentContext context);

    /**
     * 获取Agent的唯一标识键
     * @return Agent标识键（如 "editor"、"character" 等）
     */
    String getAgentKey();

    /**
     * 获取Agent的显示名称
     * @return Agent显示名称（用于日志和界面展示）
     */
    String getAgentName();
}