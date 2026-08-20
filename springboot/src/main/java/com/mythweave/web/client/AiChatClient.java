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

/**
 * 多供应商 AI 聊天客户端（deepseek / mimo / qwen）
 * 
 * DeepSeek V4 特有能力：
 * - 思考模式（thinking.type=enabled/disabled）与思考强度（reasoning_effort）仅对 deepseek 供应商生效
 * - 思考模式调用失败时自动降级为非思考模式重试一次（同模型，仅 deepseek）
 * - 流式调用透传首字延迟（TTFT）到日志
 */
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
        String thinking = primaryThinking(provider);
        try {
            return chatWithThinking(provider, thinking, systemPrompt, userMessage, temperature, maxTokens, stop);
        } catch (IOException primaryError) {
            if (shouldDegrade(provider, thinking)) {
                log.warn("{} 思考模式调用失败（{}），自动降级为非思考模式重试一次",
                        provider.toUpperCase(), primaryError.getMessage());
                return chatWithThinking(provider, "disabled", systemPrompt, userMessage, temperature, maxTokens, stop);
            }
            throw primaryError;
        }
    }

    private String chatWithThinking(String provider, String thinking, String systemPrompt, String userMessage,
                                    double temperature, int maxTokens, List<String> stop) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel(provider));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        applyThinkingMode(body, provider, thinking);
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
            log.info("{}响应(模型={}, thinking={}): content={}", provider.toUpperCase(), getModel(provider), thinking,
                    content != null ? "\"" + content.substring(0, Math.min(50, content.length())) + "...\"" : "null");

            if (content != null && !content.trim().isEmpty()) {
                return content.trim();
            }
            log.error("{} API 返回空内容, 完整响应: {}", provider.toUpperCase(), responseBody);
            throw new IOException("AI 返回空内容");
        }
    }

    public int chatStream(String provider, String systemPrompt, String userMessage, double temperature,
                          int maxTokens, Consumer<String> onToken) throws IOException {
        String thinking = primaryThinking(provider);
        boolean[] delivered = {false};
        Consumer<String> guarded = token -> {
            delivered[0] = true;
            onToken.accept(token);
        };
        try {
            return chatStreamWithThinking(provider, thinking, systemPrompt, userMessage, temperature, maxTokens, guarded);
        } catch (IOException primaryError) {
            if (shouldDegrade(provider, thinking) && !delivered[0]) {
                log.warn("{} 思考模式流式调用失败且未产出首字（{}），自动降级为非思考模式重试一次",
                        provider.toUpperCase(), primaryError.getMessage());
                return chatStreamWithThinking(provider, "disabled", systemPrompt, userMessage, temperature, maxTokens, onToken);
            }
            throw primaryError;
        }
    }

    /**
     * 单模式流式调用：透传首字延迟（TTFT）到日志，便于观测"首字响应"性能声明
     */
    private int chatStreamWithThinking(String provider, String thinking, String systemPrompt, String userMessage,
                                       double temperature, int maxTokens, Consumer<String> onToken) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel(provider));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        applyThinkingMode(body, provider, thinking);
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

        long startNanos = System.nanoTime();
        long ttftMs = -1;
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
                                if (ttftMs < 0) {
                                    ttftMs = (System.nanoTime() - startNanos) / 1_000_000;
                                    log.info("{}流式首字延迟(模型={}, thinking={}): {}ms",
                                            provider.toUpperCase(), getModel(provider), thinking, ttftMs);
                                }
                                onToken.accept(delta.asText());
                            }
                            JsonNode usage = root.path("usage");
                            if (!usage.isMissingNode()) {
                                if (usage.has("total_tokens")) {
                                    totalTokens = usage.path("total_tokens").asInt();
                                }
                                // 推理 token 统计（思考模式流式响应携带），用于观测推理消耗
                                JsonNode details = usage.path("completion_tokens_details");
                                if (details.has("reasoning_tokens")) {
                                    reasoningTokens = details.path("reasoning_tokens").asInt();
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                log.info("{}流式完成(模型={}, thinking={}): totalTokens={}, reasoningTokens={}, ttftMs={}",
                        provider.toUpperCase(), getModel(provider), thinking, totalTokens, reasoningTokens,
                        ttftMs < 0 ? "无正文输出" : ttftMs);
                return totalTokens;
            }
        }
    }

    /**
     * 设置 DeepSeek V4 思考模式参数（仅 deepseek 供应商）：
     * - thinking.type：enabled=思考模式（默认）/ disabled=非思考模式
     * - reasoning_effort：思考强度，仅思考模式生效（非思考模式传参会报错，故不携带）
     */
    private void applyThinkingMode(Map<String, Object> body, String provider, String thinking) {
        if (!"deepseek".equals(provider)) {
            return;
        }
        body.put("thinking", Map.of("type", thinking));
        if ("enabled".equals(thinking)) {
            String effort = aiProperties.getDeepseek().getReasoningEffort();
            if (effort != null && !effort.isBlank()) {
                body.put("reasoning_effort", effort);
            }
        }
    }

    /** 主调用使用的思考模式（仅 deepseek 生效，配置缺省时按 API 默认 enabled 处理） */
    private String primaryThinking(String provider) {
        if (!"deepseek".equals(provider)) {
            return "disabled";
        }
        String thinking = aiProperties.getDeepseek().getThinking();
        return (thinking == null || thinking.isBlank()) ? "enabled" : thinking.trim();
    }

    /** 降级判定：deepseek 供应商 + 思考模式 + 未禁用降级 */
    private boolean shouldDegrade(String provider, String thinking) {
        if (!"deepseek".equals(provider) || !"enabled".equals(thinking)) {
            return false;
        }
        Boolean degrade = aiProperties.getDeepseek().getDegradeOnFailure();
        return degrade == null || degrade;
    }

    public String getProviderFromModel(String model) {
        if (model == null) return "deepseek";
        if (model.toLowerCase().contains("mimo")) return "mimo";
        if (model.toLowerCase().contains("qwen") || model.toLowerCase().contains("tongyi")) return "qwen";
        return "deepseek";
    }
}
