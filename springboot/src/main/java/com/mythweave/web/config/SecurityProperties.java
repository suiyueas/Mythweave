package com.mythweave.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 安全策略配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mythweave.security")
public class SecurityProperties {

    /**
     * 安全总开关：false 时输入过滤/输出审核/频率限制全部放行
     */
    private boolean enabled = true;

    /**
     * 敏感词库（输入过滤与输出审核共用）。
     * 注意：本平台为小说创作场景，词库宜收敛为明确违规词汇，避免误杀正常创作内容
     */
    private List<String> sensitiveWords = new ArrayList<>();

    /**
     * 敏感词白名单：白名单中的词不会被 DFA 过滤器命中（如"杀青"中的"杀"）
     */
    private List<String> whitelistWords = new ArrayList<>();

    /**
     * 频率限制配置
     */
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        /** 普通用户每分钟限额 */
        private int freeLimit = 30;
        /** VIP 用户每分钟限额 */
        private int vipLimit = 120;
        /** 限流统计窗口（秒） */
        private int windowSeconds = 60;
        /** 连续违规次数达到该阈值时触发熔断 */
        private int violationThreshold = 5;
        /** 熔断冷却时长（秒） */
        private int cooldownSeconds = 300;
    }
}
