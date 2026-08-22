package com.mythweave.web.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mythweave.web.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 千问 Embedding 客户端
 */
@Slf4j
@Component
public class QianwenEmbeddingClient {

    private final AiProperties aiProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public QianwenEmbeddingClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 文本向量化（1024维），失败时指数退避重试最多 2 次
     */
    public double[] embed(String text) throws IOException {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getQianwen().getModel(),
                "input", Map.of("texts", List.of(text)),
                "parameters", Map.of("text_type", "document")
        );

        int maxRetries = 2;
        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Request request = new Request.Builder()
                        .url(aiProperties.getQianwen().getBaseUrl() + aiProperties.getQianwen().getEmbeddingEndpoint())
                        .header("Authorization", "Bearer " + aiProperties.getQianwen().getApiKey())
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Qianwen Embedding error: " + response.code());
                    }
                    JsonNode root = objectMapper.readTree(response.body().string());
                    JsonNode embeddings = root.path("output").path("embeddings").get(0).path("embedding");
                    double[] result = new double[embeddings.size()];
                    for (int i = 0; i < embeddings.size(); i++) {
                        result[i] = embeddings.get(i).asDouble();
                    }
                    return result;
                }
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delay = (long) Math.pow(2, attempt) * 500;
                    log.warn("千问 Embedding 第{}次调用失败，{}ms 后重试: {}", attempt + 1, delay, e.getMessage());
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw lastException != null ? lastException : new IOException("千问 Embedding 调用失败");
    }
}
