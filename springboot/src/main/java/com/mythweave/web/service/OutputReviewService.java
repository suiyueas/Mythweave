package com.mythweave.web.service;

import com.mythweave.web.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * AI 输出二次审核：
 * 1. 敏感词检测（DFA 确定有限自动态机，O(n) 扫描，白名单排除"杀青"等正常词，命中即截断）
 * 2. 循环/复读检测（连续相同行、单字符长串复读，命中即截断）
 * 采用软截断：检测到违规后由调用方停止转发后续 token 并发送截断提示。
 * mythweave.security.enabled=false 时全部放行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutputReviewService {

    private final SecurityProperties securityProperties;
    private volatile DfaFilter dfaFilter;

    @PostConstruct
    public void init() {
        dfaFilter = DfaFilter.build(
                securityProperties.getSensitiveWords(),
                securityProperties.getWhitelistWords()
        );
        log.info("输出敏感词 DFA 过滤器初始化完成，词库 {} 条", dfaFilter.size());
    }

    /**
     * 热更新 DFA 过滤器（与 ContentSecurityService 共享同一份词库）
     */
    public synchronized void reload() {
        dfaFilter = DfaFilter.build(
                securityProperties.getSensitiveWords(),
                securityProperties.getWhitelistWords()
        );
        log.info("输出敏感词 DFA 过滤器已热更新，词库 {} 条", dfaFilter.size());
    }

    /** 单字符连续复读阈值（如 "哈哈哈..." 长串，≥8 视为异常） */
    private static final Pattern REPEAT_CHAR_PATTERN = Pattern.compile("(.)\\1{7,}");

    /** 连续相同行达到该次数视为循环 */
    private static final int MAX_REPEAT_LINE = 5;

    /**
     * 输出内容审核
     *
     * @return pass-放行 / truncate-截断（含原因）
     */
    public OutputReviewResult reviewOutput(String content) {
        if (!securityProperties.isEnabled()) {
            return OutputReviewResult.pass();
        }
        if (content == null || content.isBlank()) {
            return OutputReviewResult.pass();
        }

        String lower = content.toLowerCase();

        // 1. 敏感词检测（DFA 确定有限自动态机，O(n) 全量扫描）
        if (dfaFilter.containsAny(lower)) {
            log.warn("输出触发敏感词过滤，已截断: {}", dfaFilter.scan(lower));
            return OutputReviewResult.truncate("生成内容包含敏感信息，已停止输出");
        }

        // 2. 循环/复读检测
        if (detectLoop(content)) {
            log.warn("输出检测到循环复读，已截断");
            return OutputReviewResult.truncate("检测到内容重复，已停止生成");
        }

        return OutputReviewResult.pass();
    }

    /**
     * 循环检测：连续相同行 ≥N 或单字符长串复读
     * 仅检测"完全相同的行"与"单字符超长复读"，避免误杀正常创作中的感叹/语气词
     */
    private boolean detectLoop(String content) {
        String[] lines = content.split("\n");
        String lastLine = null;
        int repeatCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.equals(lastLine)) {
                repeatCount++;
                if (repeatCount >= MAX_REPEAT_LINE) {
                    return true;
                }
            } else {
                repeatCount = 1;
                lastLine = trimmed;
            }
        }

        return REPEAT_CHAR_PATTERN.matcher(content).find();
    }

    public static class OutputReviewResult {
        private final boolean passed;
        private final boolean truncated;
        private final String reason;

        public OutputReviewResult(boolean passed, boolean truncated, String reason) {
            this.passed = passed;
            this.truncated = truncated;
            this.reason = reason;
        }

        public static OutputReviewResult pass() {
            return new OutputReviewResult(true, false, null);
        }

        public static OutputReviewResult truncate(String reason) {
            return new OutputReviewResult(false, true, reason);
        }

        public boolean isPassed() { return passed; }
        public boolean isTruncated() { return truncated; }
        public String getReason() { return reason; }
    }
}
