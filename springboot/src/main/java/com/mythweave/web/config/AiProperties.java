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
        /**
         * 思考模式（V4 API：thinking.type）：
         * enabled = 思考模式（对应原 deepseek-reasoner，响应携带 reasoning_content）；
         * disabled = 非思考模式（对应原 deepseek-chat）。
         * 默认 enabled，与 V4 API 默认一致。
         */
        private String thinking = "enabled";
        /**
         * 思考强度（reasoning_effort）：low / high / max，仅思考模式生效。
         * 思考与正文共享 max_tokens 预算，思考强度越高推理越长，越容易耗尽预算导致正文截断（finish_reason=length），
         * 长文生成场景建议用 low 或 high，把预算留给正文。
         */
        private String reasoningEffort = "high";
        /**
         * 思考模式调用失败（网络/5xx/仅推理无正文/空内容等）时，
         * 自动降级为非思考模式（同模型）重试一次，保证创作链路不中断。默认 true。
         */
        private Boolean degradeOnFailure = true;
        private Integer maxToken = 4096;
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