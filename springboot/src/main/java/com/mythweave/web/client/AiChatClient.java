package com.mythweave.web.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mythweave.web.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class AiChatClient {

    private final AiProperties aiProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiChatClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .readTimeout(java.time.Duration.ofSeconds(120))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private String getBaseUrl(String provider) {
        return switch (provider) {
            case "mimo" -> aiProperties.getMimo().getBaseUrl();
            case "qwen" -> aiProperties.getQianwen().getBaseUrl();
            default -> aiProperties.getDeepseek().getBaseUrl();
        };
    }

    private String getApiKey(String provider) {
        return switch (provider) {
            case "mimo" -> aiProperties.getMimo().getApiKey();
            case "qwen" -> aiProperties.getQianwen().getApiKey();
            default -> aiProperties.getDeepseek().getApiKey();
        };
    }

    private String getModel(String provider) {
        return switch (provider) {
            case "mimo" -> aiProperties.getMimo().getModel();
            case "qwen" -> aiProperties.getQianwen().getModel();
            default -> aiProperties.getDeepseek().getModel();
        };
    }

    public String chat(String provider, String systemPrompt, String userMessage, double temperature, int maxTokens) throws IOException {
        return chat(provider, systemPrompt, userMessage, temperature, maxTokens, null);
    }

    public String chat(String provider, String systemPrompt, String userMessage, double temperature, int maxTokens, List<String> stop) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel(provider));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        if (stop != null && !stop.isEmpty()) {
            body.put("stop", stop);
        }
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        String baseUrl = getBaseUrl(provider);
        String apiKey = getApiKey(provider);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("{} API HTTP错误: code={}, message={}, body={}", provider.toUpperCase(), response.code(), response.message(), responseBody);
                throw new IOException(provider.toUpperCase() + " API error: " + response.code() + " " + response.message() + ", body: " + responseBody);
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || choices.isEmpty() || choices.get(0) == null) {
                log.error("{} API 响应中无choices: {}", provider.toUpperCase(), responseBody);
                throw new IOException(provider.toUpperCase() + " API 响应格式错误：无choices");
            }
            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode()) {
                log.error("{} API 响应中无message: {}", provider.toUpperCase(), responseBody);
                throw new IOException(provider.toUpperCase() + " API 响应格式错误：无message");
            }

            String content = message.path("content").asText(null);
            log.info("{}响应: content={}", provider.toUpperCase(), content != null ? "\"" + content.substring(0, Math.min(50, content.length())) + "...\"" : "null");

            if (content != null && !content.trim().isEmpty()) {
                return content.trim();
            }
            log.error("{} API 返回空内容, 完整响应: {}", provider.toUpperCase(), responseBody);
            throw new IOException("AI 返回空内容");
        }
    }

    public int chatStream(String provider, String systemPrompt, String userMessage, double temperature,
                          int maxTokens, Consumer<String> onToken) throws IOException {
        Map<String, Object> body = Map.of(
                "model", getModel(provider),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        String baseUrl = getBaseUrl(provider);
        String apiKey = getApiKey(provider);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(provider.toUpperCase() + " API error: " + response.code());
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
                        }
                    }
                }
                return totalTokens;
            }
        }
    }

    public String getProviderFromModel(String model) {
        if (model == null) return "deepseek";
        if (model.toLowerCase().contains("mimo")) return "mimo";
        if (model.toLowerCase().contains("qwen") || model.toLowerCase().contains("tongyi")) return "qwen";
        return "deepseek";
    }
}