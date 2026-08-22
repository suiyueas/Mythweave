package com.mythweave.web.service.agent;

import com.mythweave.web.client.DeepSeekClient;
import com.mythweave.web.dto.OrchestratorRequest;
import com.mythweave.web.dto.OrchestratorResponse;
import com.mythweave.web.mapper.NovelChapterMapper;
import com.mythweave.web.mapper.NovelCharacterMapper;
import com.mythweave.web.mapper.NovelForeshadowingMapper;
import com.mythweave.web.mapper.NovelProjectMapper;
import com.mythweave.web.model.AgentContext;
import com.mythweave.web.model.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * 协调者Agent
 * 
 * 负责协调多个专业Agent协作完成复杂分析任务：
 * - Editor Agent（编辑代理）：分析章节结构、节奏、伏笔运用等
 * - Character Agent（角色代理）：分析角色发展、性格一致性、角色弧线等
 * - Style Agent（风格代理）：分析文笔风格、语言表达、描写手法等
 * - Reader Agent（读者代理）：模拟读者视角，评估阅读体验、情感共鸣等
 * 
 * 协作流程：
 * 1. 构建统一的上下文信息（项目信息、章节内容等）
 * 2. 并行调度四个Agent进行各自的专业分析
 * 3. 汇总各Agent的分析结果
 * 4. 生成综合性的写作建议
 * 
 * 采用线程池实现四个Agent的并行执行，提高响应速度
 * 设置120秒超时机制，防止某个Agent阻塞导致整体无响应
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorAgent {

    /** 协调者Agent超时时间（秒） */
    private static final long ORCHESTRATOR_TIMEOUT_SECONDS = 120;

    private final EditorAgent editorAgent;
    private final CharacterAgent characterAgent;
    private final StyleAgent styleAgent;
    private final ReaderAgent readerAgent;
    private final DeepSeekClient deepSeekClient;
    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelCharacterMapper characterMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;

    /** 固定线程池：4个Agent并行 + 1个协调线程，避免CachedThreadPool无界创建 */
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    /**
     * 执行多Agent协作分析
     * 
     * 四个Agent并行执行：
     * 1. Editor Agent：分析章节编辑质量
     * 2. Character Agent：分析角色塑造
     * 3. Style Agent：分析文风特点
     * 4. Reader Agent：模拟读者体验
     * 
     * 最后汇总各Agent结果，生成综合性建议
     * 
     * @param projectId 项目ID
     * @param request 协调请求（包含章节ID等参数）
     * @return 协调响应（包含各Agent的分析结果和综合建议）
     */
    public OrchestratorResponse orchestrate(Long projectId, OrchestratorRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("🤖 多Agent协作开始, projectId={}", projectId);

        AgentResult editorResult = null;
        AgentResult characterResult = null;
        AgentResult styleResult = null;
        AgentResult readerResult = null;
        String summary = null;

        CompletableFuture<AgentResult> editorCf = null;
        CompletableFuture<AgentResult> characterCf = null;
        CompletableFuture<AgentResult> styleCf = null;
        CompletableFuture<AgentResult> readerCf = null;

        try {
            AgentContext context = buildContext(projectId, request);
            AgentContext snapshot = context.snapshot();

            // 四个 Agent 并行提交，各自独立超时（BaseAgent 内部 120s）
            editorCf = CompletableFuture.supplyAsync(() -> {
                log.info("📝 Editor Agent 开始分析...");
                AgentResult r = editorAgent.analyze(snapshot);
                log.info("✅ Editor Agent 完成");
                return r;
            }, executor);

            characterCf = CompletableFuture.supplyAsync(() -> {
                log.info("👤 Character Agent 开始分析...");
                AgentResult r = characterAgent.analyze(snapshot);
                log.info("✅ Character Agent 完成");
                return r;
            }, executor);

            styleCf = CompletableFuture.supplyAsync(() -> {
                log.info("🎨 Style Agent 开始分析...");
                AgentResult r = styleAgent.analyze(snapshot);
                log.info("✅ Style Agent 完成");
                return r;
            }, executor);

            readerCf = CompletableFuture.supplyAsync(() -> {
                log.info("📖 Reader Agent 开始分析...");
                AgentResult r = readerAgent.analyze(snapshot);
                log.info("✅ Reader Agent 完成");
                return r;
            }, executor);

            // 全局超时：allOf 等待所有完成，任一超时触发 TimeoutException 进入 catch
            CompletableFuture.allOf(editorCf, characterCf, styleCf, readerCf)
                    .orTimeout(ORCHESTRATOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();

            // 逐个获取结果（已完成的拿正常值，超时/异常的拿 null）
            editorResult = getCfResult(editorCf);
            characterResult = getCfResult(characterCf);
            styleResult = getCfResult(styleCf);
            readerResult = getCfResult(readerCf);

            log.info("📋 开始生成综合建议...");
            summary = generateSummaryWithTimeout(editorResult, characterResult, styleResult, readerResult, snapshot);

        } catch (Exception e) {
            log.error("❌ 多Agent协作执行失败: {}", e.getMessage(), e);
            cancelCfs(editorCf, characterCf, styleCf, readerCf);
        }

        long totalCostMs = System.currentTimeMillis() - startTime;
        boolean success = editorResult != null && characterResult != null && styleResult != null && readerResult != null;
        log.info("🤖 多Agent协作{}，总耗时: {}ms", success ? "完成" : "部分完成", totalCostMs);

        return OrchestratorResponse.builder()
                .editorResult(editorResult)
                .characterResult(characterResult)
                .styleResult(styleResult)
                .readerResult(readerResult)
                .summary(summary)
                .totalCostMs(totalCostMs)
                .success(success)
                .errorMessage(success ? null : "部分Agent执行超时或失败")
                .build();
    }

    /** 安全获取 CompletableFuture 结果，异常/超时返回 null */
    private AgentResult getCfResult(CompletableFuture<AgentResult> cf) {
        if (cf == null) return null;
        try {
            return cf.isDone() && !cf.isCompletedExceptionally() ? cf.getNow(null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void cancelCfs(CompletableFuture<?>... cfs) {
        for (CompletableFuture<?> cf : cfs) {
            if (cf != null && !cf.isDone()) {
                cf.cancel(true);
            }
        }
    }

    private String generateSummaryWithTimeout(AgentResult editor, AgentResult character,
                                             AgentResult style, AgentResult reader,
                                             AgentContext context) {
        Future<String> future = executor.submit(() -> generateSummary(editor, character, style, reader, context));
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("⚠️ 生成综合建议超时，使用默认提示");
            return "综合建议生成超时，请查看各Agent的单独分析结果。";
        } catch (Exception e) {
            log.error("生成综合建议失败: {}", e.getMessage());
            return "综合建议生成失败，请查看各Agent的单独分析结果。";
        }
    }

    private AgentContext buildContext(Long projectId, OrchestratorRequest request) {
        AgentContext context = new AgentContext();
        context.setProjectId(projectId);
        context.setChapterContent(request.getChapterContent());
        context.setChapterTitle(request.getChapterTitle());
        context.setChapterIndex(request.getChapterIndex());
        context.setGoldSamples(request.getGoldSamples());
        context.setReaderType(request.getReaderType());

        if (projectId != null) {
            var project = projectMapper.selectById(projectId);
            if (project != null) {
                context.setProjectTitle(project.getTitle());
                context.setGenre(project.getGenre());
            }

            try {
                var characters = characterMapper.selectByProjectId(projectId);
                if (characters != null) {
                    context.setCharacters(characters.stream()
                            .map(c -> {
                                AgentContext.CharacterInfo info = new AgentContext.CharacterInfo();
                                info.setId(c.getId());
                                info.setName(c.getName());
                                info.setRole(c.getRole());
                                info.setPersonality(c.getPersonality() != null ? c.getPersonality() : "");
                                info.setBackground(c.getDescription() != null ? c.getDescription() : "");
                                String arc = (c.getArcStart() != null ? c.getArcStart() : "") + "→" + (c.getArcEnd() != null ? c.getArcEnd() : "");
                                info.setArc(arc.isEmpty() || arc.equals("→") ? "未设置" : arc);
                                return info;
                            })
                            .toList());
                }
            } catch (Exception e) {
                log.warn("加载人物列表失败: {}", e.getMessage());
            }

            try {
                var foreshadowings = foreshadowingMapper.selectByProjectId(projectId);
                if (foreshadowings != null) {
                    context.setForeshadowings(foreshadowings.stream()
                            .map(fs -> {
                                AgentContext.ForeshadowingInfo info = new AgentContext.ForeshadowingInfo();
                                info.setId(fs.getId());
                                info.setName(fs.getName());
                                info.setDescription(fs.getDescription());
                                info.setChapterId(fs.getChapterId() != null ? fs.getChapterId().intValue() : null);
                                info.setResolvedChapterId(fs.getResolvedChapterId() != null ? fs.getResolvedChapterId().intValue() : null);
                                info.setStatus(fs.getStatus() != null ? fs.getStatus() : "pending");
                                return info;
                            })
                            .toList());
                }
            } catch (Exception e) {
                log.warn("加载伏笔列表失败: {}", e.getMessage());
            }
        }

        return context;
    }

    private String generateSummary(AgentResult editor, AgentResult character,
                                   AgentResult style, AgentResult reader,
                                   AgentContext context) {
        // 统计成功/失败的 Agent
        List<String> successAgents = new java.util.ArrayList<>();
        List<String> failedAgents = new java.util.ArrayList<>();
        if (editor != null && editor.isSuccess()) successAgents.add("编辑");
        else failedAgents.add("编辑");
        if (character != null && character.isSuccess()) successAgents.add("人物");
        else failedAgents.add("人物");
        if (style != null && style.isSuccess()) successAgents.add("风格");
        else failedAgents.add("风格");
        if (reader != null && reader.isSuccess()) successAgents.add("读者");
        else failedAgents.add("读者");

        String editorContent = (editor != null && editor.isSuccess() && editor.getContent() != null) ? editor.getContent() : "（未完成）";
        String characterContent = (character != null && character.isSuccess() && character.getContent() != null) ? character.getContent() : "（未完成）";
        String styleContent = (style != null && style.isSuccess() && style.getContent() != null) ? style.getContent() : "（未完成）";
        String readerContent = (reader != null && reader.isSuccess() && reader.getContent() != null) ? reader.getContent() : "（未完成）";

        String prompt = String.format("""
                你是小说主编。综合以下分析给出 1-3 条核心建议（每条 50 字内）。
                成功完成的分析：[%s]；未完成的标记为"（未完成）"请忽略。
                编辑：%s
                人物：%s
                风格：%s
                读者：%s
                """,
                String.join("、", successAgents),
                editorContent, characterContent, styleContent, readerContent);

        try {
            return deepSeekClient.chat("你是资深小说主编，擅长提炼核心建议。",
                    prompt, 0.5, 512);
        } catch (Exception e) {
            log.error("生成综合建议失败: {}", e.getMessage());
            return "综合建议生成失败，请查看各Agent的单独分析结果。";
        }
    }
}