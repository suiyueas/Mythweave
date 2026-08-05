package com.mythweave.web.service;

import com.mythweave.web.client.QianwenEmbeddingClient;
import com.mythweave.web.model.ContextDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Embedding 服务：文本切块 → 千问向量化 → ES 索引
 * ES 不可用时自动跳过索引（含向量化调用），不阻塞章节保存等主流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mythweave.es", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingService {

    private final QianwenEmbeddingClient embeddingClient;
    private final ElasticsearchOperations esOps;
    private final EsConnectionState esState;

    /**
     * 将章节内容切块、向量化并写入ES
     */
    public void indexChapterContent(Long novelId, Long chapterId, String content) throws IOException {
        if (!esState.isUsable()) {
            log.warn("ES 不可用，跳过章节{}内容索引（{}）", chapterId, esState.getLastError());
            return;
        }
        List<String> chunks = splitText(content, 500); // 每500字一切块
        if (chunks.isEmpty()) return;
        List<IndexQuery> queries = new ArrayList<>();

        try {
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                double[] embedding = embeddingClient.embed(chunk);

                ContextDocument doc = new ContextDocument();
                doc.setId(UUID.randomUUID().toString());
                doc.setNovelId(novelId);
                doc.setChapterId(chapterId);
                doc.setChunkType("paragraph");
                doc.setChunkText(chunk);
                doc.setEmbedding(embedding);
                doc.setChunkSeq(i);
                doc.setCreatedAt(LocalDateTime.now());

                IndexQuery query = new IndexQueryBuilder().withId(doc.getId()).withObject(doc).build();
                queries.add(query);
            }

            esOps.bulkIndex(queries, ContextDocument.class);
            log.info("章节{}内容已索引: {} 个文本块", chapterId, chunks.size());
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
            log.warn("章节{}内容索引失败，已降级跳过: {}", chapterId, e.getMessage());
        }
    }

    /**
     * 索引人物/设定/术语等结构化数据
     */
    public void indexEntity(Long novelId, String chunkType, String text) throws IOException {
        if (!esState.isUsable()) {
            log.warn("ES 不可用，跳过[{}]实体索引（{}）", chunkType, esState.getLastError());
            return;
        }
        try {
            double[] embedding = embeddingClient.embed(text);

            ContextDocument doc = new ContextDocument();
            doc.setId(UUID.randomUUID().toString());
            doc.setNovelId(novelId);
            doc.setChunkType(chunkType);
            doc.setChunkText(text);
            doc.setEmbedding(embedding);
            doc.setChunkSeq(0);
            doc.setCreatedAt(LocalDateTime.now());

            IndexQuery query = new IndexQueryBuilder().withId(doc.getId()).withObject(doc).build();
            esOps.index(query, esOps.getIndexCoordinatesFor(ContextDocument.class));
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
            log.warn("[{}]实体索引失败，已降级跳过: {}", chunkType, e.getMessage());
        }
    }

    /**
     * 简单文本切块（按段落边界优先，保证每块不超过maxChars）
     */
    private List<String> splitText(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] paragraphs = text.split("\n\n|\\n(?=\\S)");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() > maxChars && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append("\n");
            current.append(para);

            // 单个超长段落强制切分
            while (current.length() > maxChars) {
                int splitAt = maxChars;
                for (int i = maxChars - 1; i >= maxChars - 50 && i >= 0; i--) {
                    if (current.charAt(i) == '。' || current.charAt(i) == '；' || current.charAt(i) == '\n') {
                        splitAt = i + 1;
                        break;
                    }
                }
                chunks.add(current.substring(0, splitAt).trim());
                current = new StringBuilder(current.substring(splitAt));
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }
}
