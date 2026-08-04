package com.mythweave.web.service.agent;

import com.mythweave.web.client.DeepSeekClient;
import com.mythweave.web.model.AgentContext;
import com.mythweave.web.model.AgentResult;
import com.mythweave.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 编辑Agent（Editor Agent）
 * 
 * 职责：
 * - 分析章节的叙事结构和节奏把控
 * - 检查逻辑一致性和情节连贯性
 * - 评估伏笔的铺设与回收情况
 * - 提供章节结构优化建议
 * 
 * 使用编辑专家的视角来分析文本
 * 结合作品的伏笔信息进行综合评估
 * 
 * 温度参数：0.7（保持一定创造性的同时确保分析逻辑性）
 * 最大令牌：4096
 */
@Slf4j
@Component
public class EditorAgent extends BaseAgent {

    protected EditorAgent(DeepSeekClient deepSeekClient) {
        super(deepSeekClient);
    }

    @Override
    public String getAgentKey() {
        return "editor";
    }

    @Override
    public String getAgentName() {
        return "编辑";
    }

    /**
     * 执行编辑视角的内容分析
     * 
     * @param context 包含章节内容、伏笔信息等上下文
     * @return 分析结果，包含叙事节奏、逻辑一致性、伏笔管理等方面的评价
     */
    @Override
    public AgentResult analyze(AgentContext context) {
        String chapterContent = context.getChapterContent() != null ? context.getChapterContent() : "";
        String foreshadowingInfo = buildForeshadowingInfo(context);

        String prompt = PromptTemplates.EDITOR_AGENT
                .replace("{chapterContent}", chapterContent)
                + "\n\n【伏笔信息】\n" + foreshadowingInfo;

        if (context.getProjectTitle() != null) {
            prompt = "【作品】" + context.getProjectTitle() + "\n\n" + prompt;
        }

        return execute("你是一位资深小说编辑，擅长分析叙事节奏、逻辑一致性和伏笔管理。", prompt, 0.7, 4096);
    }
}