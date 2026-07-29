package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Agent基类，提供通用能力
 */
@Slf4j
public abstract class BaseAgent implements WritingAgent {

    private static final int MAX_RETRIES = 2;
    private static final long TIMEOUT_SECONDS = 30;

    protected final DeepSeekClient deepSeekClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    protected BaseAgent(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    protected AgentResult execute(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Future<String> future = executor.submit(() ->
                    deepSeekClient.chat(systemPrompt, userPrompt, temperature, maxTokens)
                );

                String response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                long costMs = System.currentTimeMillis() - startTime;
                log.info("Agent[{}] 执行完成，耗时: {}ms (第{}次尝试)", getAgentKey(), costMs, attempt);
                return AgentResult.success(getAgentKey(), getAgentName(), response, costMs);

            } catch (TimeoutException e) {
                lastException = e;
                log.warn("Agent[{}] 第{}次尝试超时 ({}秒)", getAgentKey(), attempt, TIMEOUT_SECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastException = e;
                log.error("Agent[{}] 执行被中断", getAgentKey(), e);
                break;

            } catch (ExecutionException e) {
                lastException = e;
                log.warn("Agent[{}] 第{}次尝试失败: {}", getAgentKey(), attempt, e.getCause().getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        long costMs = System.currentTimeMillis() - startTime;
        String errorMsg = lastException != null ? lastException.getMessage() : "未知错误";
        log.error("Agent[{}] 执行最终失败，已尝试{}次: {}", getAgentKey(), MAX_RETRIES, errorMsg);
        return AgentResult.failure(getAgentKey(), getAgentName(), "执行失败: " + errorMsg);
    }

    protected String buildCharacterProfile(AgentContext context) {
        if (context.getCharacters() == null || context.getCharacters().isEmpty()) {
            return "暂无人物档案";
        }
        StringBuilder sb = new StringBuilder();
        for (AgentContext.CharacterInfo charInfo : context.getCharacters()) {
            sb.append("【").append(charInfo.getName()).append("】\n");
            sb.append("  角色: ").append(charInfo.getRole()).append("\n");
            sb.append("  性格: ").append(charInfo.getPersonality()).append("\n");
            sb.append("  背景: ").append(charInfo.getBackground()).append("\n");
            sb.append("  成长弧: ").append(charInfo.getArc()).append("\n\n");
        }
        return sb.toString();
    }

    protected String buildForeshadowingInfo(AgentContext context) {
        if (context.getForeshadowings() == null || context.getForeshadowings().isEmpty()) {
            return "暂无伏笔信息";
        }
        StringBuilder sb = new StringBuilder();
        for (AgentContext.ForeshadowingInfo fs : context.getForeshadowings()) {
            sb.append("【").append(fs.getName()).append("】");
            if (fs.getChapterId() != null) {
                sb.append(" (埋于第").append(fs.getChapterId()).append("章)");
            }
            if (fs.getResolvedChapterId() != null) {
                sb.append(" → 计划在第").append(fs.getResolvedChapterId()).append("章回收");
            }
            sb.append("\n  描述: ").append(fs.getDescription());
            sb.append("\n  状态: ").append(fs.getStatus()).append("\n\n");
        }
        return sb.toString();
    }
}