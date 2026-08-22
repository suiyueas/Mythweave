package com.mythweave.web.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.mythweave.web.config.SearchConfig;
import com.mythweave.web.model.ContextDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * ES混合检索服务：kNN语义向量 + BM25关键词加权融合
 * 融合策略：分别执行 kNN 和 BM25 检索 → min-max 归一化各自得分 → 按可配置权重加权求和 → 按融合分重排
 * 内置连接熔断：ES 不可用时所有检索直接降级返回空结果，不影响 AI 创作主链路
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mythweave.es", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ESSearchService {

    private final ElasticsearchClient esClient;
    private final EsConnectionState esState;
    private final SearchConfig searchConfig;
    private final EsPendingWriteBuffer pendingWriteBuffer;
    private final EmbeddingService embeddingService;

    /**
     * 启动后异步探测 ES 连通性，不阻塞应用启动；
     * 探测失败自动进入熔断冷却，冷却结束由首次调用乐观重试恢复
     */
    @PostConstruct
    public void init() {
        if (!esState.isEnabled()) {
            log.warn("Elasticsearch 已通过 mythweave.es.enabled=false 禁用，向量检索关闭");
            return;
        }
        Thread.ofVirtual().name("es-probe").start(this::probeHealth);
    }

    /**
     * 探测 ES 连通性（单线程并发安全）
     */
    private void probeHealth() {
        if (!esState.tryBeginProbe()) return;
        try {
            if (esClient.ping().value()) {
                esState.markAvailable();
                // ES 恢复后自动重放缓冲区（故障期间未索引的章节/实体）
                if (!pendingWriteBuffer.isEmpty()) {
                    int replayed = pendingWriteBuffer.replay(embeddingService);
                    log.info("ES 恢复，已重放缓冲区 {} 条写入请求", replayed);
                }
            } else {
                esState.markUnavailable("ping 返回失败");
            }
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
        }
    }

    /**
     * 混合检索：kNN向量搜索 + BM25关键词，min-max 归一化后按可配置权重加权融合
     */
    public List<ContextDocument> hybridSearch(Long novelId, double[] queryVector, String keyword, int topK) throws IOException {
        if (!esState.isUsable()) {
            log.warn("ES 不可用，混合检索降级返回空结果（{}）", esState.getLastError());
            return List.of();
        }
        try {
            int numCandidates = topK * searchConfig.getCandidateMultiplier();

            // 1. 分别执行 kNN 和 BM25 检索，各自返回 topK 候选（多取以覆盖不同排序）
            Map<String, HitScore> knnScores = searchKnn(novelId, queryVector, numCandidates);
            Map<String, HitScore> bm25Scores = searchBM25(novelId, keyword, numCandidates);

            // 2. 合并文档 ID 并集
            Set<String> allIds = new HashSet<>(knnScores.keySet());
            allIds.addAll(bm25Scores.keySet());

            // 3. min-max 归一化
            double[] knnRaw = allIds.stream().mapToDouble(id -> knnScores.containsKey(id) ? knnScores.get(id).score : 0).toArray();
            double[] bm25Raw = allIds.stream().mapToDouble(id -> bm25Scores.containsKey(id) ? bm25Scores.get(id).score : 0).toArray();
            double[] knnNorm = minMaxNormalize(knnRaw);
            double[] bm25Norm = minMaxNormalize(bm25Raw);

            // 4. 加权融合，按融合分降序排列取 topK
            double alpha = searchConfig.getWeightVector();
            double beta = searchConfig.getWeightBm25();
            List<ScoredDoc> merged = new ArrayList<>();
            int idx = 0;
            for (String id : allIds) {
                double finalScore = alpha * knnNorm[idx] + beta * bm25Norm[idx];
                HitScore knnHit = knnScores.get(id);
                HitScore bm25Hit = bm25Scores.get(id);
                ContextDocument doc = knnHit != null ? knnHit.doc : bm25Hit.doc;
                merged.add(new ScoredDoc(doc, finalScore, knnHit != null ? knnHit.score : 0, bm25Hit != null ? bm25Hit.score : 0));
                idx++;
            }
            merged.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));

            List<ContextDocument> results = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, merged.size()); i++) {
                ScoredDoc sd = merged.get(i);
                results.add(sd.doc);
                if (log.isDebugEnabled()) {
                    log.debug("混合检索[{}] 文档={} 融合分={:.4f} 向量原始={:.4f} BM25原始={:.4f}",
                            i, sd.doc.getId(), sd.finalScore, sd.knnRaw, sd.bm25Raw);
                }
            }
            log.info("混合检索完成：查询文档ID {} 个，融合返回 top{}", allIds.size(), results.size());
            return results;
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
            log.warn("ES 混合检索失败，已熔断降级：{}", e.getMessage());
            return List.of();
        }
    }

    /** kNN 向量检索，返回 docId → HitScore 映射 */
    private Map<String, HitScore> searchKnn(Long novelId, double[] queryVector, int numCandidates) throws IOException {
        KnnSearch knn = KnnSearch.of(k -> k
                .field("embedding")
                .queryVector(toFloatList(queryVector))
                .k(numCandidates)
                .numCandidates(numCandidates * 2)
        );
        SearchRequest request = SearchRequest.of(s -> s
                .index("novel_context")
                .knn(List.of(knn))
                .query(q -> q.term(t -> t.field("novelId").value(novelId)))
                .size(numCandidates)
        );
        SearchResponse<ContextDocument> response = esClient.search(request, ContextDocument.class);
        Map<String, HitScore> scores = new LinkedHashMap<>();
        for (Hit<ContextDocument> hit : response.hits().hits()) {
            if (hit.source() != null) {
                scores.put(hit.id(), new HitScore(hit.source(), hit.score() != null ? hit.score() : 0));
            }
        }
        return scores;
    }

    /** BM25 关键词检索，返回 docId → HitScore 映射 */
    private Map<String, HitScore> searchBM25(Long novelId, String keyword, int numCandidates) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index("novel_context")
                .query(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("novelId").value(novelId)))
                        .must(m -> m.match(mm -> mm.field("chunkText").query(keyword)))
                ))
                .size(numCandidates)
        );
        SearchResponse<ContextDocument> response = esClient.search(request, ContextDocument.class);
        Map<String, HitScore> scores = new LinkedHashMap<>();
        for (Hit<ContextDocument> hit : response.hits().hits()) {
            if (hit.source() != null) {
                scores.put(hit.id(), new HitScore(hit.source(), hit.score() != null ? hit.score() : 0));
            }
        }
        return scores;
    }

    /** min-max 归一化：将原始分数组映射到 [0,1] */
    private double[] minMaxNormalize(double[] raw) {
        if (raw.length == 0) return raw;
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (double v : raw) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double range = max - min;
        if (range == 0) {
            Arrays.fill(raw, 1.0);
            return raw;
        }
        double[] normalized = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            normalized[i] = (raw[i] - min) / range;
        }
        return normalized;
    }

    private record HitScore(ContextDocument doc, double score) {}
    private record ScoredDoc(ContextDocument doc, double finalScore, double knnRaw, double bm25Raw) {}

    /**
     * 纯语义向量检索
     */
    public List<ContextDocument> vectorSearch(Long novelId, double[] queryVector, int topK) throws IOException {
        if (!esState.isUsable()) {
            log.warn("ES 不可用，向量检索降级返回空结果（{}）", esState.getLastError());
            return List.of();
        }
        try {
            KnnSearch knn = KnnSearch.of(k -> k
                    .field("embedding")
                    .queryVector(toFloatList(queryVector))
                    .k(topK)
                    .numCandidates(topK * 2)
            );

            SearchRequest request = SearchRequest.of(s -> s
                    .index("novel_context")
                    .knn(List.of(knn))
                    .query(q -> q.term(t -> t.field("novelId").value(novelId)))
                    .size(topK)
            );

            SearchResponse<ContextDocument> response = esClient.search(request, ContextDocument.class);
            List<ContextDocument> results = new ArrayList<>();
            for (Hit<ContextDocument> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
            }
            return results;
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
            log.warn("ES 向量检索失败，已熔断降级：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 纯 BM25 关键词检索（混合检索的对照基线，亦可作为无向量场景的兜底检索）
     */
    public List<ContextDocument> bm25Search(Long novelId, String keyword, int topK) throws IOException {
        if (!esState.isUsable()) {
            log.warn("ES 不可用，BM25 检索降级返回空结果（{}）", esState.getLastError());
            return List.of();
        }
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index("novel_context")
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.term(t -> t.field("novelId").value(novelId)))
                                    .must(m -> m.match(mm -> mm.field("chunkText").query(keyword)))
                            )
                    )
                    .size(topK)
            );

            SearchResponse<ContextDocument> response = esClient.search(request, ContextDocument.class);
            List<ContextDocument> results = new ArrayList<>();
            for (Hit<ContextDocument> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
            }
            return results;
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
            log.warn("ES BM25 检索失败，已熔断降级：{}", e.getMessage());
            return List.of();
        }
    }

    private List<Float> toFloatList(double[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (double v : array) list.add((float) v);
        return list;
    }
}
