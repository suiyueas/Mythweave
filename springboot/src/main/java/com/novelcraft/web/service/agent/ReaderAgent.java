package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import com.novelcraft.web.template.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 读者Agent：负责阅读感受、吸引点、疑惑点、期待分析
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