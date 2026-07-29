package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import com.novelcraft.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 人物Agent：负责人物行为一致性、对话风格、弧光进度分析
 */
@Slf4j
@Component
public class CharacterAgent extends BaseAgent {

    protected CharacterAgent(DeepSeekClient deepSeekClient) {
        super(deepSeekClient);
    }

    @Override
    public String getAgentKey() {
        return "character";
    }

    @Override
    public String getAgentName() {
        return "人物";
    }

    @Override
    public AgentResult analyze(AgentContext context) {
        String chapterContent = context.getChapterContent() != null ? context.getChapterContent() : "";
        String characterProfile = buildCharacterProfile(context);

        String prompt = PromptTemplates.CHARACTER_AGENT
                .replace("{characterProfile}", characterProfile)
                .replace("{chapterContent}", chapterContent);

        if (context.getProjectTitle() != null) {
            prompt = "【作品】" + context.getProjectTitle() + "\n\n" + prompt;
        }

        return execute("你是一位人物塑造专家，擅长分析角色行为一致性和对话风格。", prompt, 0.7, 4096);
    }
}