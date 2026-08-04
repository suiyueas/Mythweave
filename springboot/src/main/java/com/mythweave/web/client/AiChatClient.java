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
        return chat(provider, systemPrompt, userMessage, temperature, maxTokens, null, (Integer) null);
    }

    public String chat(String provider, String systemPrompt, String userMessage, double temperature, int maxTokens, List<String> stop) throws IOException {
        return chat(provider, systemPrompt, userMessage, temperature, maxTokens, stop, (Integer) null);
    }

    /**
     * 非流式调用（带推理 token 上限）
     * 
     * @param maxReasoningTokens 推理模型思维链 token 上限（max_reasoning_tokens），
     *                           限制推理长度以保证正文 token 配额，null 时回退到配置默认值
     */
    public String chat(String provider, String systemPrompt, String userMessage, double temperature, int maxTokens, Integer maxReasoningTokens) throws IOException {
        return chat(provider, systemPrompt, userMessage, temperature, maxTokens, (List<String>) null, maxReasoningTokens);
    }

    public String chat(String provider, String systemPrompt, String userMessage, double temperature, int maxTokens, List<String> stop, Integer maxReasoningTokens) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel(provider));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        applyMaxReasoningTokens(body, provider, maxReasoningTokens);
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
        return chatStream(provider, systemPrompt, userMessage, temperature, maxTokens, onToken, null);
    }

    /**
     * 流式SSE调用（带推理 token 上限）
     * 
     * @param maxReasoningTokens 推理模型思维链 token 上限（max_reasoning_tokens），
     *                           限制推理长度以保证正文 token 配额，null 时回退到配置默认值
     */
    public int chatStream(String provider, String systemPrompt, String userMessage, double temperature,
                          int maxTokens, Consumer<String> onToken, Integer maxReasoningTokens) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel(provider));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        applyMaxReasoningTokens(body, provider, maxReasoningTokens);
        body.put("stream", true);
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
            if (!response.isSuccessful()) {
                throw new IOException(provider.toUpperCase() + " API error: " + response.code());
            }
            String line;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body().byteStream()))) {
                int totalTokens = 0;
                int reasoningTokens = 0;
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
                            if (!usage.isMissingNode()) {
                                if (usage.has("total_tokens")) {
                                    totalTokens = usage.path("total_tokens").asInt();
                                }
                                // 推理 token 统计（deepseek-reasoner 流式响应携带），用于观测推理消耗
                                JsonNode details = usage.path("completion_tokens_details");
                                if (details.has("reasoning_tokens")) {
                                    reasoningTokens = details.path("reasoning_tokens").asInt();
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                log.info("{}流式完成: totalTokens={}, reasoningTokens={}", provider.toUpperCase(), totalTokens, reasoningTokens);
                return totalTokens;
            }
        }
    }

    /**
     * 为 DeepSeek 推理模型设置 max_reasoning_tokens：独立限制思维链长度，
     * 防止推理过长耗尽 max_tokens 预算导致正文（content）被截断或缺失。
     * 该参数仅 deepseek-reasoner 等推理模型支持，非推理模型传参会报错，故仅对 deepseek 且模型名为 reasoner 时生效。
     * 显式传入的参数优先，否则回退到配置默认值。
     */
    private void applyMaxReasoningTokens(Map<String, Object> body, String provider, Integer maxReasoningTokens) {
        if (!"deepseek".equals(provider)) {
            return;
        }
        if (maxReasoningTokens == null) {
            maxReasoningTokens = aiProperties.getDeepseek().getMaxReasoningToken();
        }
        String model = getModel(provider);
        boolean reasoningModel = model != null && model.toLowerCase().contains("reasoner");
        if (reasoningModel && maxReasoningTokens != null && maxReasoningTokens > 0) {
            body.put("max_reasoning_tokens", maxReasoningTokens);
            log.debug("已设置 max_reasoning_tokens={}（模型={}）", maxReasoningTokens, model);
        }
    }

    public String getProviderFromModel(String model) {
        if (model == null) return "deepseek";
        if (model.toLowerCase().contains("mimo")) return "mimo";
        if (model.toLowerCase().contains("qwen") || model.toLowerCase().contains("tongyi")) return "qwen";
        return "deepseek";
    }
}