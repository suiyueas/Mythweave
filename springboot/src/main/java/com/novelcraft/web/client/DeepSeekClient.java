package com.novelcraft.web.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcraft.web.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 客户端（非流式 + 流式SSE）
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final AiProperties aiProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        AiProperties.DeepSeek config = aiProperties.getDeepseek();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout())
                .readTimeout(config.getReadTimeout())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 非流式调用
     */
    public String chat(String systemPrompt, String userMessage, double temperature, int maxTokens) throws IOException {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getDeepseek().getModel(),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Request request = new Request.Builder()
                .url(aiProperties.getDeepseek().getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + aiProperties.getDeepseek().getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DeepSeek API error: " + response.code() + " " + response.message());
            }
            JsonNode root = objectMapper.readTree(response.body().string());
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }

    /**
     * 流式SSE调用，通过回调逐块返回，返回总 token 消耗量
     */
    public int chatStream(String systemPrompt, String userMessage, double temperature,
                           int maxTokens, java.util.function.Consumer<String> onToken) throws IOException {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getDeepseek().getModel(),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Request request = new Request.Builder()
                .url(aiProperties.getDeepseek().getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + aiProperties.getDeepseek().getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DeepSeek API error: " + response.code());
            }
            String line;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body().byteStream()))) {
                int totalTokens = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                        String json = line.substring(6);
                        try {
                            JsonNode root = objectMapper.readTree(json);
                            JsonNode delta = root.path("choices").get(0).path("delta").path("content");
                            if (!delta.isMissingNode() && !delta.asText().isEmpty()) {
                                onToken.accept(delta.asText());
                            }
                            JsonNode usage = root.path("usage");
                            if (!usage.isMissingNode() && usage.has("total_tokens")) {
                                totalTokens = usage.path("total_tokens").asInt();
                            }
                        } catch (Exception ignored) {
                            // skip parse errors
                        }
                    }
                }
                return totalTokens;
            }
        }
    }
}
