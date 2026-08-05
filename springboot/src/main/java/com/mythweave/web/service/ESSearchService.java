package com.mythweave.web.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.mythweave.web.model.ContextDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ES混合检索服务：kNN语义向量 + BM25关键词加权融合
 * 内置连接熔断：ES 不可用时所有检索直接降级返回空结果，不影响 AI 创作主链路
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ESSearchService {

    private final ElasticsearchClient esClient;
    private final EsConnectionState esState;

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
            } else {
                esState.markUnavailable("ping 返回失败");
            }
        } catch (Exception e) {
            esState.markUnavailable(e.getMessage());
        }
    }

    /**
     * 混合检索：kNN向量搜索 + BM25关键词
     */
    public List<ContextDocument> hybridSearch(Long novelId, double[] queryVector, String keyword, int topK) throws IOException {
        if (!esState.isUsable()) {
            log.warn("ES 不可用，混合检索降级返回空结果（{}）", esState.getLastError());
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
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.term(t -> t.field("novelId").value(novelId)))
                                    .should(sh -> sh.match(m -> m.field("chunkText").query(keyword).boost(0.3f)))
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
            log.warn("ES 混合检索失败，已熔断降级：{}", e.getMessage());
            return List.of();
        }
    }

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

    private List<Float> toFloatList(double[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (double v : array) list.add((float) v);
        return list;
    }
}
