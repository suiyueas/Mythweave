package com.mythweave.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "mythweave.ai")
public class AiProperties {
    private DeepSeek deepseek = new DeepSeek();
    private Qianwen qianwen = new Qianwen();
    private Mimo mimo = new Mimo();

    @Data
    public static class DeepSeek {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-v4-flash";
        private Integer maxToken = 4096;
        /**
         * 推理模型（deepseek-reasoner）思维链 token 上限（max_reasoning_tokens）：
         * 推理与正文共享 max_tokens 预算，推理过长会耗尽预算导致正文被截断（finish_reason=length）。
         * 独立限制推理长度后，剩余预算自动留给正文。仅对推理模型生效，非推理模型自动忽略。
         * 建议设为 maxToken 的 50%~70%（如 8192 总预算配 4096）。null 表示不限制。
         */
        private Integer maxReasoningToken = 4096;
        private Double temperature = 0.7;
        private Double topP = 0.9;
        private Duration streamTimeout = Duration.ofSeconds(120);
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(120);
    }

    @Data
    public static class Qianwen {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String embeddingEndpoint = "/service/embeddings/text-embedding/text-embedding";
        private String model = "text-embedding-v3";
        private Integer dimensions = 1024;
    }

    @Data
    public static class Mimo {
        private String apiKey;
        private String baseUrl = "https://api.mimoai.cn/v1";
        private String model = "mimo-2.5-pro";
        private Integer maxToken = 4096;
        private Double temperature = 0.7;
        private Double topP = 0.9;
        private Duration streamTimeout = Duration.ofSeconds(120);
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(120);
    }
}