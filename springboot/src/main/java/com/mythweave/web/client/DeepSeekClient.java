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
 * - model: 使用的模型名称（V4 仅 deepseek-v4-flash / deepseek-v4-pro）
 * - connectTimeout: 连接超时时间
 * - readTimeout: 读取超时时间
 * 
 * 思考模式（V4 API，原 deepseek-reasoner/chat 已退役合并为单一模型的模式开关）：
 * - thinking.type=enabled（默认）：思考模式，响应携带 reasoning_content
 * - thinking.type=disabled：非思考模式（原 deepseek-chat 等价）
 * - reasoning_effort=low/high/max：思考强度，仅思考模式生效
 * - 思考模式调用失败时自动降级为非思考模式重试一次（同模型）
 * 
 * 重试机制：
 * - 当遇到 5xx 错误、网络超时、连接问题时自动重试
 * - 默认重试 3 次，指数退避间隔（1s, 2s, 4s）
 * - 流式场景下仅当首个 token 尚未产出时才降级（避免重复输出）
 */
@Slf4j
@Component
public class DeepSeekClient {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final AiProperties aiProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        AiProperties.DeepSeek config = aiProperties.getDeepseek();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout())
                .readTimeout(config.getReadTimeout())
                .retryOnConnectionFailure(true)
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
        return chat(systemPrompt, userMessage, temperature, maxTokens, (List<String>) null);
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
        String thinking = primaryThinking();
        try {
            return chatWithThinking(thinking, systemPrompt, userMessage, temperature, maxTokens, stop);
        } catch (IOException primaryError) {
            if (shouldDegrade(thinking)) {
                log.warn("思考模式调用失败（{}），自动降级为非思考模式重试一次", primaryError.getMessage());
                return chatWithThinking("disabled", systemPrompt, userMessage, temperature, maxTokens, stop);
            }
            throw primaryError;
        }
    }

    /**
     * 单模式非流式调用（带自动重试）：重试 + 空内容/仅推理内容判定均在本方法内完成，
     * 外层负责思考/非思考模式降级编排
     */
    private String chatWithThinking(String thinking, String systemPrompt, String userMessage,
                                    double temperature, int maxTokens, List<String> stop) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getDeepseek().getModel());
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        applyThinkingMode(body, thinking);
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

        IOException lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    // 5xx 服务器错误或网络问题：可重试
                    if (response.code() >= 500 || response.code() == 429) {
                        lastException = new IOException("DeepSeek API HTTP错误: code=" + response.code() + ", message=" + response.message() + ", body=" + responseBody);
                        if (attempt < MAX_RETRIES) {
                            long delay = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                            log.warn("DeepSeek API 请求失败 (attempt {}/{}), {}ms 后重试: code={}, message={}",
                                    attempt + 1, MAX_RETRIES + 1, delay, response.code(), response.message());
                            Thread.sleep(delay);
                            continue;
                        }
                        log.error("DeepSeek API HTTP错误 (已重试{}次): code={}, message={}, body={}",
                                MAX_RETRIES, response.code(), response.message(), responseBody);
                        throw lastException;
                    }
                    // 4xx 客户端错误：不重试
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

                log.info("DeepSeek响应(thinking={}): finish_reason={}, content={}, reasoning_content长度={}",
                        thinking, finishReason,
                        content != null ? "\"" + content.substring(0, Math.min(50, content.length())) + "...\"" : "null",
                        reasoningContent != null ? reasoningContent.length() : 0);

                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
                // content 为空：思考过程（reasoning_content）不可作为业务结果，
                // 明确抛错以触发上层降级/重试，避免把推理文字当作正文使用
                // 日志用 WARN：多数调用方有降级/重试机制，属可恢复场景，业务层失败时另有 ERROR 日志
                if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
                    log.warn("AI 仅返回推理内容（无有效正文），finish_reason={}，已丢弃推理内容", finishReason);
                    throw new IOException("AI 仅返回推理内容，无有效正文 (finish_reason=" + finishReason + ")");
                }
                log.error("DeepSeek API 返回空内容, 完整响应: {}", responseBody);
                throw new IOException("AI 返回空内容");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("重试被中断", e);
            } catch (IOException e) {
                // 网络超时、连接失败等：可重试
                if (attempt < MAX_RETRIES) {
                    long delay = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                    lastException = e;
                    log.warn("DeepSeek API 请求异常 (attempt {}/{}), {}ms 后重试: {}",
                            attempt + 1, MAX_RETRIES + 1, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        throw lastException != null ? lastException : new IOException("DeepSeek API 调用失败，已重试 " + MAX_RETRIES + " 次");
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
        String thinking = primaryThinking();
        boolean[] delivered = {false};
        java.util.function.Consumer<String> guarded = token -> {
            delivered[0] = true;
            onToken.accept(token);
        };
        try {
            return chatStreamWithThinking(thinking, systemPrompt, userMessage, temperature, maxTokens, guarded);
        } catch (IOException primaryError) {
            if (shouldDegrade(thinking) && !delivered[0]) {
                log.warn("思考模式流式调用失败且未产出首字（{}），自动降级为非思考模式重试一次", primaryError.getMessage());
                return chatStreamWithThinking("disabled", systemPrompt, userMessage, temperature, maxTokens, onToken);
            }
            throw primaryError;
        }
    }

    /**
     * 单模式流式调用：透传首字延迟（TTFT）到日志，便于观测"首字响应"性能声明
     */
    private int chatStreamWithThinking(String thinking, String systemPrompt, String userMessage, double temperature,
                                       int maxTokens, java.util.function.Consumer<String> onToken) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getDeepseek().getModel());
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        applyThinkingMode(body, thinking);
        body.put("stream", true);
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

        long startNanos = System.nanoTime();
        long ttftMs = -1;
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DeepSeek API error: " + response.code());
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
                                    log.info("DeepSeek流式首字延迟(模型={}, thinking={}): {}ms",
                                            aiProperties.getDeepseek().getModel(), thinking, ttftMs);
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
                            // skip parse errors
                        }
                    }
                }
                log.info("DeepSeek流式完成(模型={}, thinking={}): totalTokens={}, reasoningTokens={}, ttftMs={}",
                        aiProperties.getDeepseek().getModel(), thinking, totalTokens, reasoningTokens,
                        ttftMs < 0 ? "无正文输出" : ttftMs);
                return totalTokens;
            }
        }
    }

    /**
     * 设置 V4 思考模式参数：
     * - thinking.type：enabled=思考模式（默认）/ disabled=非思考模式
     * - reasoning_effort：思考强度，仅思考模式生效（非思考模式传参会报错，故不携带）
     */
    private void applyThinkingMode(Map<String, Object> body, String thinking) {
        body.put("thinking", Map.of("type", thinking));
        if ("enabled".equals(thinking)) {
            String effort = aiProperties.getDeepseek().getReasoningEffort();
            if (effort != null && !effort.isBlank()) {
                body.put("reasoning_effort", effort);
            }
        }
    }

    /** 主调用使用的思考模式（配置缺省时按 API 默认 enabled 处理） */
    private String primaryThinking() {
        String thinking = aiProperties.getDeepseek().getThinking();
        return (thinking == null || thinking.isBlank()) ? "enabled" : thinking.trim();
    }

    /** 降级判定：主调用为思考模式且未禁用降级时生效 */
    private boolean shouldDegrade(String thinking) {
        if (!"enabled".equals(thinking)) {
            return false;
        }
        Boolean degrade = aiProperties.getDeepseek().getDegradeOnFailure();
        return degrade == null || degrade;
    }
}
