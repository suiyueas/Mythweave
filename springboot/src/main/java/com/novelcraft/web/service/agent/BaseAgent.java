package com.novelcraft.web.service.agent;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.model.AgentContext;
import com.novelcraft.web.model.AgentResult;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Agent基类
 * 
 * 提供所有专业写作Agent的通用能力：
 * - 调用DeepSeek API执行AI分析
 * - 重试机制（最多2次重试）
 * - 超时控制（120秒，防止推理型模型长时间无响应）
 * - 执行耗时统计
 * - 角色档案和伏笔信息的格式化构建
 * 
 * 设计模式：模板方法模式
 * 子类只需实现getAgentKey()、getAgentName()和analyze()方法
 * 通用执行逻辑由基类提供
 * 
 * 重试策略：
 * - 首次失败后等待1秒再试
 * - 第二次失败后等待2秒再试
 * - 两次都失败则返回失败结果
 */
@Slf4j
public abstract class BaseAgent implements WritingAgent {

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 2;
    /** 超时时间（秒）：推理型模型在正式输出前可能消耗20-60秒推理，120秒确保足够 */
    private static final long TIMEOUT_SECONDS = 120;

    protected final DeepSeekClient deepSeekClient;
    /** 线程池用于异步执行AI调用 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    protected BaseAgent(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    /**
     * 执行AI分析请求
     * 
     * @param systemPrompt 系统提示词（定义AI角色）
     * @param userPrompt 用户提示词（包含需要分析的内容）
     * @param temperature 温度参数（0-1，越高越有创造性）
     * @param maxTokens 最大生成令牌数
     * @return AgentResult 分析结果或失败信息
     */
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

    /**
     * 构建角色档案格式化字符串
     * 
     * @param context 包含角色列表的上下文
     * @return 格式化后的角色档案字符串
     */
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

    /**
     * 构建伏笔信息格式化字符串
     * 
     * @param context 包含伏笔列表的上下文
     * @return 格式化后的伏笔信息字符串
     */
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