package com.mythweave.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 混合检索权重配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mythweave.search")
public class SearchConfig {

    /** 向量检索权重（kNN cosine similarity），范围 [0,1]，推荐 0.7 */
    private double weightVector = 0.7;

    /** BM25 关键词检索权重，范围 [0,1]，推荐 0.3 */
    private double weightBm25 = 0.3;

    /** kNN 检索候选集倍数（numCandidates = topK * candidateMultiplier） */
    private int candidateMultiplier = 2;
}
