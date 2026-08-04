package com.mythweave.web.service.agent;

import com.mythweave.web.client.DeepSeekClient;
import com.mythweave.web.model.AgentContext;
import com.mythweave.web.model.AgentResult;
import com.mythweave.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 人物Agent（Character Agent）
 * 
 * 职责：
 * - 分析角色行为的一致性和可信度
 * - 评估角色对话风格是否符合人物设定
 * - 分析角色弧光的进展情况
 * - 检测角色互动关系的演变
 * - 识别人物塑造方面的问题
 * 
 * 使用人物塑造专家的视角来分析文本
 * 结合作品的角色设定档案进行综合评估
 * 
 * 温度参数：0.7（保持分析客观性的同时允许一定灵活性）
 * 最大令牌：4096
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

    /**
     * 执行人物视角的内容分析
     * 
     * @param context 包含章节内容、角色设定档案等上下文
     * @return 分析结果，包含角色行为一致性、对话风格、角色弧光等方面的评价
     */
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