package com.novelcraft.web.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcraft.web.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 客户端
 * 
 * 提供与DeepSeek大语言模型的交互能力：
 * - 非流式调用：适用于需要完整返回结果的场景
 * - 流式SSE调用：适用于需要实时展示生成内容的场景（如AI写作助手）
 * 
 * 配置项（从AiProperties读取）：
 * - baseUrl: DeepSeek API地址
 * - apiKey: API密钥
 * - model: 使用的模型名称
 * - connectTimeout: 连接超时时间
 * - readTimeout: 读取超时时间
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
     * 非流式调用（无停止序列）
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param temperature 温度参数（控制随机性，0-2，越高越随机）
     * @param maxTokens 最大生成token数
     * @return AI生成的完整回复
     * @throws IOException 调用失败时抛出
     */
    public String chat(String systemPrompt, String userMessage, double temperature, int maxTokens) throws IOException {
        return chat(systemPrompt, userMessage, temperature, maxTokens, null);
    }

    /**
     * 非流式调用
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param temperature 温度参数
     * @param maxTokens 最大生成token数
     * @param stop 停止序列列表（如 ["\\n\\n"]），命中后提前终止生成，可为null
     * @return AI生成的完整回复
     * @throws IOException 调用失败时抛出
     */
    public String chat(String systemPrompt, String userMessage, double temperature, int maxTokens, List<String> stop) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getDeepseek().getModel());
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        if (stop != null && !stop.isEmpty()) {
            body.put("stop", stop);
        }
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        Request request = new Request.Builder()
                .url(aiProperties.getDeepseek().getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + aiProperties.getDeepseek().getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("DeepSeek API HTTP错误: code={}, message={}, body={}", response.code(), response.message(), responseBody);
                throw new IOException("DeepSeek API error: " + response.code() + " " + response.message() + ", body: " + responseBody);
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || choices.isEmpty() || choices.get(0) == null) {
                log.error("DeepSeek API 响应中无choices: {}", responseBody);
                throw new IOException("DeepSeek API 响应格式错误：无choices");
            }
            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode()) {
                log.error("DeepSeek API 响应中无message: {}", responseBody);
                throw new IOException("DeepSeek API 响应格式错误：无message");
            }

            String content = message.path("content").asText(null);
            String reasoningContent = message.path("reasoning_content").asText(null);
            String finishReason = choices.get(0).path("finish_reason").asText();

            log.info("DeepSeek响应: finish_reason={}, content={}, reasoning_content长度={}",
                    finishReason, content != null ? "\"" + content.substring(0, Math.min(50, content.length())) + "...\"" : "null", reasoningContent != null ? reasoningContent.length() : 0);

            if (content != null && !content.trim().isEmpty()) {
                return content.trim();
            }
            // content 为空：推理过程（reasoning_content）不可作为业务结果，
            // 明确抛错以触发上层降级/重试，避免把推理文字当作正文使用
            // 日志用 WARN：多数调用方有降级/重试机制，属可恢复场景，业务层失败时另有 ERROR 日志
            if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
                log.warn("AI 仅返回推理内容（无有效正文），finish_reason={}，已丢弃推理内容", finishReason);
                throw new IOException("AI 仅返回推理内容，无有效正文 (finish_reason=" + finishReason + ")");
            }
            log.error("DeepSeek API 返回空内容, 完整响应: {}", responseBody);
            throw new IOException("AI 返回空内容");
        }
    }

    /**
     * 流式SSE调用，通过回调逐块返回
     * 
     * 适用于需要实时展示AI生成内容的场景
     * 通过回调函数onToken逐个 token 地返回生成内容
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param temperature 温度参数
     * @param maxTokens 最大生成token数
     * @param onToken 令牌回调函数，实时接收生成的token
     * @return 总token消耗量
     * @throws IOException 调用失败时抛出
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