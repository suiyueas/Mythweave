package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import com.novelcraft.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 风格Agent：负责句式、词汇、修辞、叙事视角一致性分析
 */
@Slf4j
@Component
public class StyleAgent extends BaseAgent {

    protected StyleAgent(DeepSeekClient deepSeekClient) {
        super(deepSeekClient);
    }

    @Override
    public String getAgentKey() {
        return "style";
    }

    @Override
    public String getAgentName() {
        return "风格";
    }

    @Override
    public AgentResult analyze(AgentContext context) {
        String goldSamples = context.getGoldSamples() != null ? context.getGoldSamples() : "暂无黄金样本";
        String targetText = context.getChapterContent() != null ? context.getChapterContent() : "";

        String prompt = PromptTemplates.STYLE_AGENT
                .replace("{goldSamples}", goldSamples)
                .replace("{targetText}", targetText);

        if (context.getProjectTitle() != null) {
            prompt = "【作品】" + context.getProjectTitle() + "\n\n" + prompt;
        }

        return execute("你是一位文学风格分析专家，擅长对比句式、词汇、修辞和叙事视角。", prompt, 0.7, 4096);
    }
}