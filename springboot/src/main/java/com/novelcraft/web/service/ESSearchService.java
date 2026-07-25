package com.novelcraft.web.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.novelcraft.web.model.ContextDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ES混合检索服务：kNN语义向量 + BM25关键词加权融合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ESSearchService {

    private final ElasticsearchClient esClient;

    /**
     * 混合检索：kNN向量搜索 + BM25关键词
     */
    public List<ContextDocument> hybridSearch(Long novelId, double[] queryVector, String keyword, int topK) throws IOException {
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
    }

    /**
     * 纯语义向量检索
     */
    public List<ContextDocument> vectorSearch(Long novelId, double[] queryVector, int topK) throws IOException {
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
    }

    private List<Float> toFloatList(double[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (double v : array) list.add((float) v);
        return list;
    }
}
