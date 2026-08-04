package com.mythweave.web.service.agent;

import com.mythweave.web.client.DeepSeekClient;
import com.mythweave.web.model.AgentContext;
import com.mythweave.web.model.AgentResult;
import com.mythweave.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 风格Agent（Style Agent）
 * 
 * 职责：
 * - 分析句式结构和表达多样性
 * - 评估词汇选择的专业性和准确性
 * - 检查修辞手法的运用效果
 * - 确保叙事视角的一致性
 * - 对比参考样本的风格特点
 * 
 * 使用文学风格分析专家的视角来分析文本
 * 结合黄金样本进行风格对比评估
 * 
 * 温度参数：0.7（保持分析严谨性的同时允许创造性见解）
 * 最大令牌：4096
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

    /**
     * 执行风格视角的内容分析
     * 
     * @param context 包含目标文本、黄金样本等上下文
     * @return 分析结果，包含句式、词汇、修辞、叙事视角等方面的评价
     */
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