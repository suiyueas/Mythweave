package com.mythweave.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Elasticsearch 检索模块配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mythweave.es")
public class EsProperties {

    /**
     * 是否启用 ES 向量检索：
     * false 时完全禁用，所有 ES 相关调用直接降级（应用启动与 AI 创作不受影响）
     */
    private boolean enabled = true;

    /**
     * 连接失败后的熔断冷却时间：冷却期内所有 ES 调用直接短路，
     * 冷却结束后自动乐观重试探测一次，恢复后自动回到正常状态
     */
    private Duration retryAfter = Duration.ofSeconds(60);
}
