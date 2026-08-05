package com.mythweave.web.service;

import com.mythweave.web.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * AI 输入安全过滤：
 * 1. 敏感词库匹配（可配置，硬拦截）
 * 2. 越狱（Jailbreak）检测（硬拦截）
 * 3. Prompt 注入攻击检测（硬拦截）
 * mythweave.security.enabled=false 时全部放行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSecurityService {

    private final SecurityProperties securityProperties;

    /** 越狱检测：诱导模型无视系统限制的指令模式 */
    private static final Pattern JAILBREAK_PATTERN = Pattern.compile(
            "忽略\\s*(之前|所有|之前限制)|" +
            "ignore\\s*(previous|all|previous\\s+instructions)|" +
            "你(现在|从现在起|现在就)是?[一个款]?(无限制|没有约束|不受限制|没有任何限制)|" +
            "假设你(可以|能够).{0,20}(做任何|无视|摆脱)|" +
            "disregard\\s*(all|previous|policy)|" +
            "jailbreak|" +
            "DAN\\s*(do|anything|now)|" +
            "角色扮演.{0,10}无限制|" +
            "你现在是.{1,20}，不是.{1,10}",
            Pattern.CASE_INSENSITIVE);

    /** 注入攻击检测：试图覆盖系统指令/输出内部格式的输入 */
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(system|prompt|指令|system\\s+prompt)\\s*[:：=].{0,40}|" +
            "\\{system\\}|\\{prompt\\}|\\[prompt\\]|" +
            "<<[^>]*>>|" +
            "现在你(是|扮演).{1,20}，不是",
            Pattern.CASE_INSENSITIVE);

    /**
     * 输入安全检查
     *
     * @return pass-放行 / blocked-拦截（含原因）
     */
    public SecurityCheckResult checkInput(String userInput) {
        if (!securityProperties.isEnabled()) {
            return SecurityCheckResult.pass();
        }
        if (userInput == null || userInput.isBlank()) {
            return SecurityCheckResult.pass();
        }

        String lower = userInput.toLowerCase();

        // 1. 敏感词检测（配置词库）
        for (String word : securityProperties.getSensitiveWords()) {
            if (word != null && !word.isBlank() && lower.contains(word.toLowerCase())) {
                log.warn("输入触发敏感词过滤: {}", maskSensitive(word));
                return SecurityCheckResult.blocked("输入包含敏感内容，请调整后重试");
            }
        }

        // 2. 越狱检测
        if (JAILBREAK_PATTERN.matcher(lower).find()) {
            log.warn("输入触发越狱检测: {}", truncateForLog(userInput));
            return SecurityCheckResult.blocked("检测到异常指令，请重新描述您的问题");
        }

        // 3. 注入攻击检测
        if (INJECTION_PATTERN.matcher(lower).find()) {
            log.warn("输入触发注入攻击检测: {}", truncateForLog(userInput));
            return SecurityCheckResult.blocked("检测到异常指令格式，请重新描述");
        }

        return SecurityCheckResult.pass();
    }

    /** 敏感词脱敏展示（保留首尾字符） */
    private String maskSensitive(String word) {
        if (word == null || word.length() <= 2) return "***";
        return word.charAt(0) + "***" + word.charAt(word.length() - 1);
    }

    private String truncateForLog(String text) {
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    public static class SecurityCheckResult {
        private final boolean passed;
        private final boolean blocked;
        private final String reason;

        public SecurityCheckResult(boolean passed, boolean blocked, String reason) {
            this.passed = passed;
            this.blocked = blocked;
            this.reason = reason;
        }

        public static SecurityCheckResult pass() {
            return new SecurityCheckResult(true, false, null);
        }

        public static SecurityCheckResult blocked(String reason) {
            return new SecurityCheckResult(false, true, reason);
        }

        public boolean isPassed() { return passed; }
        public boolean isBlocked() { return blocked; }
        public String getReason() { return reason; }
    }
}
