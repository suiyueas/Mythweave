package com.mythweave.benchmark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 评测集数据结构（对应 benchmark/sample-data.json）
 */
public class BenchmarkData {

    private List<SeedDoc> corpus;
    private List<EvalQuery> queries;

    public List<SeedDoc> getCorpus() { return corpus; }
    public void setCorpus(List<SeedDoc> corpus) { this.corpus = corpus; }
    public List<EvalQuery> getQueries() { return queries; }
    public void setQueries(List<EvalQuery> queries) { this.queries = queries; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeedDoc {
        private String id;
        private String chunkType;
        private String text;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getChunkType() { return chunkType; }
        public void setChunkType(String chunkType) { this.chunkType = chunkType; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EvalQuery {
        private String id;
        private String category;
        private String query;
        private List<String> relevantIds;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public List<String> getRelevantIds() { return relevantIds; }
        public void setRelevantIds(List<String> relevantIds) { this.relevantIds = relevantIds; }
    }
}