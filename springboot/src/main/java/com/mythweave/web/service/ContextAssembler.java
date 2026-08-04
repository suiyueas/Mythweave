package com.mythweave.web.service;

import com.mythweave.web.client.QianwenEmbeddingClient;
import com.mythweave.web.model.ContextDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文自动装配器
 * 光标位置 → ES混合检索Top-K相关内容 → 组装为结构化System Prompt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAssembler {

    private final ESSearchService esSearchService;
    private final QianwenEmbeddingClient embeddingClient;

    /**
     * 根据当前写作位置，检索并组装上下文
     */
    public String assembleContext(Long novelId, String currentText, int topK) throws IOException {
        if (currentText == null || currentText.isEmpty()) {
            return "";
        }
        // 用当前文本做语义搜索
        double[] queryVector = embeddingClient.embed(currentText);
        List<ContextDocument> results = esSearchService.hybridSearch(novelId, queryVector, currentText, topK);

        if (results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【相关前文内容】\n");
        for (int i = 0; i < results.size(); i++) {
            ContextDocument doc = results.get(i);
            sb.append("--- 片段").append(i + 1).append(" (").append(doc.getChunkType()).append(") ---\n");
            sb.append(doc.getChunkText()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 装配续写用的完整System Prompt上下文
     */
    public String assembleForContinueWriting(Long novelId, String cursorText, String existingText, int topK) throws IOException {
        String context = assembleContext(novelId, cursorText, topK);
        if (context.isEmpty()) return "";

        return context + "\n【当前已写内容】\n" + (existingText != null ? existingText : "");
    }

    /**
     * 语义搜索：自然语言查询全书
     */
    public List<String> semanticSearch(Long novelId, String query, int topK) throws IOException {
        double[] queryVector = embeddingClient.embed(query);
        List<ContextDocument> results = esSearchService.vectorSearch(novelId, queryVector, topK);
        return results.stream()
                .map(doc -> "[" + doc.getChunkType() + "] " + doc.getChunkText())
                .collect(Collectors.toList());
    }
}
