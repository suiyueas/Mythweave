package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import com.novelcraft.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 读者Agent（Reader Agent）
 * 
 * 职责：
 * - 模拟真实读者的阅读体验
 * - 评估章节的吸引力和可读性
 * - 识别读者可能产生的疑惑点
 * - 分析读者的期待感和悬念设置
 * - 评估情感共鸣和代入感
 * 
 * 使用目标读者群体的视角来分析文本
 * 可以指定不同的读者类型（如普通读者、资深读者、悬疑爱好者等）
 * 
 * 温度参数：0.7（保持评价客观性的同时允许个性化见解）
 * 最大令牌：4096
 */
@Slf4j
@Component
public class ReaderAgent extends BaseAgent {

    protected ReaderAgent(DeepSeekClient deepSeekClient) {
        super(deepSeekClient);
    }

    @Override
    public String getAgentKey() {
        return "reader";
    }

    @Override
    public String getAgentName() {
        return "读者";
    }

    /**
     * 执行读者视角的内容分析
     * 
     * @param context 包含章节内容、读者类型等上下文
     * @return 分析结果，包含阅读感受、吸引点、疑惑点、期待感等方面的评价
     */
    @Override
    public AgentResult analyze(AgentContext context) {
        String chapterContent = context.getChapterContent() != null ? context.getChapterContent() : "";
        String readerType = context.getReaderType() != null ? context.getReaderType() : "普通";

        String prompt = PromptTemplates.READER_AGENT
                .replace("{readerType}", readerType)
                .replace("{chapterContent}", chapterContent);

        if (context.getProjectTitle() != null) {
            prompt = "【作品】" + context.getProjectTitle() + "\n\n" + prompt;
        }

        return execute("你是一位资深读者，擅长从读者视角分析阅读感受和吸引力。", prompt, 0.7, 4096);
    }
}