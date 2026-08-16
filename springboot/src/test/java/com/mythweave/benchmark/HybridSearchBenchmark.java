package com.mythweave.benchmark;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mythweave.web.MythweaveApplication;
import com.mythweave.web.client.QianwenEmbeddingClient;
import com.mythweave.web.config.AiProperties;
import com.mythweave.web.model.ContextDocument;
import com.mythweave.web.service.ESSearchService;
import com.mythweave.web.service.EsConnectionState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实验①+②：混合检索 Recall@5 对比与 P95 延迟
 *
 * 运行前提（与本机开发环境一致）：
 *  - Elasticsearch 8.x 已启动（可访问 application.yml 配置的地址）
 *  - 千问 Embedding API Key 已配置（QIANWEN_API_KEY）
 *
 * 运行方式（不参与默认 mvn test）：
 *  mvn test -Dtest=HybridSearchBenchmark -DfailIfNoTests=false
 *
 * 产物：docs/benchmark-results/hybrid-search.md
 */
@SpringBootTest(classes = MythweaveApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HybridSearchBenchmark {

    private static final int TOP_K = 5;
    private static final int LATENCY_WARMUP = 10;
    private static final int LATENCY_ITERATIONS =
            Integer.parseInt(System.getProperty("benchmark.latency.iterations", "200"));

    @Autowired
    private ESSearchService esSearchService;
    @Autowired
    private QianwenEmbeddingClient embeddingClient;
    @Autowired
    private ElasticsearchOperations esOps;
    @Autowired
    private EsConnectionState esState;
    @Autowired
    private AiProperties aiProperties;
    @Autowired
    private ElasticsearchClient esClient;

    private BenchmarkData data;
    private final Map<String, ContextDocument> corpusDocs = new HashMap<>();
    private final Map<String, double[]> queryVectors = new HashMap<>();

    @BeforeAll
    void setUp() throws Exception {
        data = BenchmarkUtils.loadBenchmarkData();
        assertNotNull(data.getCorpus(), "评测集 corpus 为空");
        assertNotNull(data.getQueries(), "评测集 queries 为空");
        assertFalse(data.getCorpus().isEmpty(), "评测集 corpus 为空");
        assertFalse(data.getQueries().isEmpty(), "评测集 queries 为空");

        assertEsAvailable();
        seedCorpus();
        embedQueries();
        System.out.println("评测集就绪: " + data.getCorpus().size() + " 份设定文档, "
                + data.getQueries().size() + " 条评测问题 (novelId=" + BenchmarkUtils.BENCH_NOVEL_ID + ")");
    }

    private void assertEsAvailable() throws Exception {
        if (!esState.isUsable()) {
            try {
                if (esClient.ping().value()) {
                    esState.markAvailable();
                }
            } catch (Exception e) {
                // 继续向下走，给出明确报错
            }
        }
        assertTrue(esState.isUsable(),
                "Elasticsearch 不可用。请先启动 ES 并确认 application.yml 配置正确，再运行本基准测试。");
    }

    private void seedCorpus() throws Exception {
        esClient.deleteByQuery(d -> d
                .index("novel_context")
                .query(q -> q.term(t -> t.field("novelId").value(BenchmarkUtils.BENCH_NOVEL_ID))));

        List<IndexQuery> queries = new ArrayList<>();
        for (BenchmarkData.SeedDoc doc : data.getCorpus()) {
            double[] embedding = embeddingClient.embed(doc.getText());
            ContextDocument cd = new ContextDocument();
            cd.setId(doc.getId());
            cd.setNovelId(BenchmarkUtils.BENCH_NOVEL_ID);
            cd.setChunkType(doc.getChunkType());
            cd.setChunkText(doc.getText());
            cd.setEmbedding(embedding);
            cd.setChunkSeq(0);
            cd.setCreatedAt(LocalDateTime.now());
            queries.add(new IndexQueryBuilder().withId(cd.getId()).withObject(cd).build());
            corpusDocs.put(doc.getId(), cd);
        }
        esOps.bulkIndex(queries, ContextDocument.class);
        esOps.indexOps(ContextDocument.class).refresh();
        System.out.println("种子数据已索引: " + queries.size() + " 份文档（幂等覆盖，可重复运行）");
    }

    private void embedQueries() throws Exception {
        for (BenchmarkData.EvalQuery q : data.getQueries()) {
            queryVectors.put(q.getId(), embeddingClient.embed(q.getQuery()));
        }
    }

    @Test
    void recallBenchmark() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# 实验①：混合检索 Recall@5 对比\n\n");
        sb.append(BenchmarkUtils.envHeader());
        sb.append("- Elasticsearch 版本: ").append(esClient.info().version().number()).append("\n");
        sb.append("- Embedding 模型: ").append(aiProperties.getQianwen().getModel())
                .append(" (").append(aiProperties.getQianwen().getDimensions()).append("维)\n");
        sb.append("- 检索参数: topK=").append(TOP_K)
                .append(", kNN numCandidates=").append(TOP_K * 2)
                .append(", BM25 match boost=0.3（hybrid 生产参数）\n");
        sb.append("- 评测集: 自建 44 份玄幻设定文档 + 40 条标注问题（语义/关键词/混合三类），见 ")
                .append("springboot/src/test/resources/benchmark/sample-data.json\n\n");

        Map<String, double[]> strategyPerCategory = new HashMap<>();
        Map<String, Double> strategyOverall = new HashMap<>();
        List<String> strategies = List.of("vector", "bm25", "hybrid");

        sb.append("## Recall@5 汇总\n\n");
        sb.append("| 策略 | 语义类(14条) | 关键词类(13条) | 混合类(13条) | 整体(40条) |\n");
        sb.append("|---|---|---|---|---|\n");

        for (String s : strategies) {
            double[] perCat = new double[3];
            double total = 0;
            for (BenchmarkData.EvalQuery q : data.getQueries()) {
                double r = recallFor(q, s);
                int catIdx = switch (q.getCategory()) {
                    case "semantic" -> 0;
                    case "keyword" -> 1;
                    default -> 2;
                };
                perCat[catIdx] += r;
                total += r;
            }
            for (int i = 0; i < 3; i++) perCat[i] /= countByCategory(data.getQueries(), i);
            double overall = total / data.getQueries().size();
            strategyPerCategory.put(s, perCat);
            strategyOverall.put(s, overall);
            sb.append("| ").append(strategyName(s)).append(" | ")
                    .append(BenchmarkUtils.fmt(perCat[0] * 100)).append("% | ")
                    .append(BenchmarkUtils.fmt(perCat[1] * 100)).append("% | ")
                    .append(BenchmarkUtils.fmt(perCat[2] * 100)).append("% | ")
                    .append(BenchmarkUtils.fmt(overall * 100)).append("% |\n");
        }
        double vector = strategyOverall.get("vector");
        double hybrid = strategyOverall.get("hybrid");
        double bm25 = strategyOverall.get("bm25");
        sb.append("\n> 结论：混合检索整体 Recall@5 为 **").append(BenchmarkUtils.fmt(hybrid * 100))
                .append("%**，对比纯向量 **").append(BenchmarkUtils.fmt(vector * 100))
                .append("%** 提升约 **").append(BenchmarkUtils.fmt((hybrid - vector) / vector * 100))
                .append("%**；对比纯 BM25 **").append(BenchmarkUtils.fmt(bm25 * 100)).append("%**。\n");

        sb.append("\n## 逐条明细\n\n");
        sb.append("| 问题 | 类别 | 相关文档数 | vector@5 | bm25@5 | hybrid@5 |\n");
        sb.append("|---|---|---|---|---|---|\n");
        for (BenchmarkData.EvalQuery q : data.getQueries()) {
            sb.append("| ").append(q.getId()).append(" ").append(q.getQuery()).append(" | ")
                    .append(q.getCategory()).append(" | ")
                    .append(q.getRelevantIds().size()).append(" | ")
                    .append(recallFor(q, "vector")).append(" | ")
                    .append(recallFor(q, "bm25")).append(" | ")
                    .append(recallFor(q, "hybrid")).append(" |\n");
        }

        Path report = BenchmarkUtils.writeReport("hybrid-search.md", sb.toString());
        System.out.println("报告已生成: " + report.toAbsolutePath());

        System.out.println("\n===== Recall@5 汇总 =====");
        System.out.printf("%-10s %10s %10s %10s %10s%n", "策略", "语义类", "关键词类", "混合类", "整体");
        for (String s : strategies) {
            double[] perCat = strategyPerCategory.get(s);
            System.out.printf("%-10s %9.1f%% %9.1f%% %9.1f%% %9.1f%%%n", strategyName(s),
                    perCat[0] * 100, perCat[1] * 100, perCat[2] * 100, strategyOverall.get(s) * 100);
        }
    }

    @Test
    void latencyBenchmark() throws Exception {
        BenchmarkData.EvalQuery q = data.getQueries().get(0);
        double[] vector = queryVectors.get(q.getId());
        assertNotNull(vector, "查询向量缺失");

        for (int i = 0; i < LATENCY_WARMUP; i++) {
            esSearchService.hybridSearch(BenchmarkUtils.BENCH_NOVEL_ID, vector, q.getQuery(), TOP_K);
        }
        long[] samples = new long[LATENCY_ITERATIONS];
        for (int i = 0; i < LATENCY_ITERATIONS; i++) {
            long start = System.nanoTime();
            esSearchService.hybridSearch(BenchmarkUtils.BENCH_NOVEL_ID, vector, q.getQuery(), TOP_K);
            samples[i] = (System.nanoTime() - start) / 1_000_000;
        }
        long p50 = BenchmarkUtils.percentileMs(samples, 50);
        long p95 = BenchmarkUtils.percentileMs(samples, 95);
        long p99 = BenchmarkUtils.percentileMs(samples, 99);
        double avg = 0;
        for (long s : samples) avg += s;
        avg /= samples.length;
        double qps = 1000.0 / avg;

        StringBuilder sb = new StringBuilder();
        sb.append("# 实验②：混合检索延迟（P50/P95/P99）\n\n");
        sb.append(BenchmarkUtils.envHeader());
        sb.append("- 样本: 预热 ").append(LATENCY_WARMUP).append(" 次后连续 ").append(LATENCY_ITERATIONS)
                .append(" 次 hybridSearch（topK=").append(TOP_K).append("）\n");
        sb.append("- 查询样例: ").append(q.getQuery()).append("\n\n");
        sb.append("| 指标 | 数值 |\n|---|---|\n");
        sb.append("| P50 | ").append(p50).append(" ms |\n");
        sb.append("| P95 | ").append(p95).append(" ms |\n");
        sb.append("| P99 | ").append(p99).append(" ms |\n");
        sb.append("| 平均 | ").append(BenchmarkUtils.fmt(avg)).append(" ms |\n");
        sb.append("| QPS | ").append(BenchmarkUtils.fmt(qps)).append(" |\n");

        Path report = BenchmarkUtils.writeReport("hybrid-search-latency.md", sb.toString());
        System.out.println("报告已生成: " + report.toAbsolutePath());
        System.out.printf("%n===== 延迟统计（%d 次） =====%nP50=%dms  P95=%dms  P99=%dms  平均=%.1fms  QPS=%.1f%n",
                samples.length, p50, p95, p99, avg, qps);
    }

    private double recallFor(BenchmarkData.EvalQuery q, String strategy) throws Exception {
        Set<String> relevant = new LinkedHashSet<>(q.getRelevantIds());
        List<ContextDocument> retrieved = switch (strategy) {
            case "vector" -> esSearchService.vectorSearch(
                    BenchmarkUtils.BENCH_NOVEL_ID, queryVectors.get(q.getId()), TOP_K);
            case "bm25" -> esSearchService.bm25Search(
                    BenchmarkUtils.BENCH_NOVEL_ID, q.getQuery(), TOP_K);
            default -> esSearchService.hybridSearch(
                    BenchmarkUtils.BENCH_NOVEL_ID, queryVectors.get(q.getId()), q.getQuery(), TOP_K);
        };
        long hits = retrieved.stream()
                .filter(d -> d.getId() != null && relevant.contains(d.getId()))
                .count();
        return (double) hits / relevant.size();
    }

    private int countByCategory(List<BenchmarkData.EvalQuery> queries, int catIdx) {
        int n = 0;
        for (BenchmarkData.EvalQuery q : queries) {
            int idx = switch (q.getCategory()) {
                case "semantic" -> 0;
                case "keyword" -> 1;
                default -> 2;
            };
            if (idx == catIdx) n++;
        }
        return n;
    }

    private String strategyName(String s) {
        return switch (s) {
            case "vector" -> "纯向量 kNN";
            case "bm25" -> "纯 BM25";
            default -> "混合 kNN+BM25";
        };
    }
}