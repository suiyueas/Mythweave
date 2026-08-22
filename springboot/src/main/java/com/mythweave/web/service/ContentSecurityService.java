package com.mythweave.web.service;

import com.mythweave.web.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * AI 输入安全过滤：
 * 1. 敏感词库匹配（DFA 确定有限自动态机，O(n) 扫描，可配置白名单排除正常创作词汇）
 * 2. 越狱（Jailbreak）检测（硬拦截）
 * 3. Prompt 注入攻击检测（硬拦截）
 * mythweave.security.enabled=false 时全部放行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSecurityService {

    private final SecurityProperties securityProperties;
    private volatile DfaFilter dfaFilter;

    @PostConstruct
    public void init() {
        dfaFilter = buildFilter();
        log.info("输入敏感词 DFA 过滤器初始化完成，词库 {} 条，白名单 {} 条",
                dfaFilter.size(), securityProperties.getWhitelistWords() != null
                        ? securityProperties.getWhitelistWords().size() : 0);
    }

    /**
     * 热更新 DFA 过滤器：运营修改配置后调用此方法即可刷新，无需重启服务
     */
    public synchronized void reload() {
        dfaFilter = buildFilter();
        log.info("输入敏感词 DFA 过滤器已热更新，词库 {} 条", dfaFilter.size());
    }

    private DfaFilter buildFilter() {
        return DfaFilter.build(
                securityProperties.getSensitiveWords(),
                securityProperties.getWhitelistWords()
        );
    }

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

        // 1. 敏感词检测（DFA 确定有限自动态机，O(n) 全量扫描，时间复杂度与词库大小无关）
        if (dfaFilter.containsAny(lower)) {
            log.warn("输入触发敏感词过滤: {}", dfaFilter.scan(lower));
            return SecurityCheckResult.blocked("输入包含敏感内容，请调整后重试");
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
