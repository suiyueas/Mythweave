package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import com.novelcraft.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 编辑Agent：负责叙事节奏、逻辑一致性、伏笔管理分析
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