package com.novelcraft.web.service;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.template.PromptTemplates;
import com.novelcraft.web.entity.*;
import com.novelcraft.web.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.novelcraft.web.utils.TitleDeduplicator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final DeepSeekClient deepSeekClient;
    private final NovelAiSessionMapper sessionMapper;
    private final NovelCharacterMapper characterMapper;
    private final NovelOutlineMapper outlineMapper;
    private final NovelWorldSettingMapper worldSettingMapper;
    private final NovelPlotThreadMapper plotThreadMapper;
    private final NovelInspirationMapper inspirationMapper;
    private final NovelChapterMapper chapterMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final NovelProjectMapper projectMapper;

    /**
     * 流式续写
     */
    public void streamContinueWriting(Long projectId, String context, String existingText,
                                       double temperature, int maxTokens, Consumer<String> onToken) throws IOException {
        String prompt = PromptTemplates.CONTINUE_WRITING
                .replace("{context}", context != null ? context : "")
                .replace("{existingText}", existingText);
        int tokensUsed = deepSeekClient.chatStream("你是一位专业小说作家", prompt, temperature, maxTokens, onToken);

        // 保存会话记录
        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("writing");
        session.setRole("assistant");
        session.setContent(prompt);
        session.setTokensUsed(tokensUsed);
        sessionMapper.insert(session);
    }

    private String sanitizeContext(String context) {
        if (context == null || context.isBlank()) return "";
        // agent 名不是有效上下文，过滤掉
        String trimmed = context.trim();
        if ("editor".equalsIgnoreCase(trimmed) || "character".equalsIgnoreCase(trimmed)
                || "style".equalsIgnoreCase(trimmed) || "reader".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
    }

    /**
     * 流式对话
     */
    public void streamChat(Long projectId, String novelTitle, String genre,
                            String currentChapter, String context, String userMessage,
                            double temperature, int maxTokens, Consumer<String> onToken) throws IOException {
        log.info("开始构建Chat Prompt, novelTitle={}, genre={}", novelTitle, genre);
        String prompt = PromptTemplates.CHAT
                .replace("{novelTitle}", novelTitle != null ? novelTitle : "")
                .replace("{genre}", genre != null ? genre : "")
                .replace("{currentChapter}", currentChapter != null ? currentChapter : "")
                .replace("{userMessage}", userMessage);
        // context 为空或不含实质内容时，移除模板中的占位段
        String ctx = sanitizeContext(context);
        if (ctx.isEmpty()) {
            prompt = prompt.replace("\n\n【相关上下文】\n{context}\n\n", "\n\n");
        } else {
            prompt = prompt.replace("{context}", ctx);
        }
        log.info("Chat prompt构建完成, 长度={}, 前200字={}", prompt.length(),
                prompt.substring(0, Math.min(200, prompt.length())));

        int tokensUsed = deepSeekClient.chatStream("你是一位AI写作助手", prompt, temperature, maxTokens, onToken);
        log.info("DeepSeek调用完成, tokensUsed={}", tokensUsed);
    }

    /**
     * 非流式对话
     */
    public String chat(Long projectId, String userMessage) throws IOException {
        String reply = deepSeekClient.chat("你是一位AI写作助手", userMessage, 0.7, 4096);

        // 保存会话记录
        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("chat");
        session.setRole("assistant");
        session.setContent(reply);
        session.setTokensUsed(0);
        sessionMapper.insert(session);

        return reply;
    }

    /**
     * AI 生成章节标题（上下文感知版）
     */
    public String generateTitle(Long projectId, Map<String, Object> params) throws IOException {
        NovelProject project = projectMapper.selectById(projectId);
        String projectTitle = project != null ? project.getTitle() : "未命名作品";

        Integer chapterIndex = params.get("chapterIndex") != null
                ? ((Number) params.get("chapterIndex")).intValue() : 1;
        String direction = params.get("direction") != null
                ? params.get("direction").toString() : "延续主线剧情";
        @SuppressWarnings("unchecked")
        List<String> existingTitles = (List<String>) params.get("existingTitles");
        @SuppressWarnings("unchecked")
        List<String> sentinelHints = (List<String>) params.get("sentinelHints");

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业小说作家，正在为小说《").append(projectTitle).append("》")
              .append("的第 ").append(chapterIndex).append(" 章创作标题。\n\n");

        // 1. 已有标题风格分析
        prompt.append("【已有章节标题】\n");
        if (existingTitles != null && !existingTitles.isEmpty()) {
            int max = Math.min(existingTitles.size(), 10);
            for (int i = 0; i < max; i++) {
                prompt.append("第").append(i + 1).append("章：").append(existingTitles.get(i)).append("\n");
            }
            String style = analyzeTitleStyle(existingTitles);
            prompt.append("\n【风格分析】").append(style).append("\n");
        } else {
            prompt.append("（暂无已有标题，请自由发挥）\n");
        }
        prompt.append("\n");

        // 2. 大纲
        List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
        if (outlines != null && !outlines.isEmpty()) {
            prompt.append("【大纲结构】\n");
            for (NovelOutline o : outlines) {
                prompt.append("- ").append(o.getTitle() != null ? o.getTitle() : "");
                if (o.getDescription() != null && !o.getDescription().isEmpty()) {
                    prompt.append("：").append(o.getDescription());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        // 3. 世界观关键词
        List<NovelWorldSetting> worlds = worldSettingMapper.selectByProjectId(projectId);
        if (worlds != null && !worlds.isEmpty()) {
            List<String> keywords = new java.util.ArrayList<>();
            for (NovelWorldSetting w : worlds) {
                if (w.getName() != null) keywords.add(w.getName());
            }
            if (!keywords.isEmpty()) {
                prompt.append("【世界观关键词】").append(String.join("、", keywords.stream().limit(5).toList()))
                      .append("\n（标题应融入这些世界观元素）\n\n");
            }
        }

        // 4. 人物
        List<NovelCharacter> chars = characterMapper.selectByProjectId(projectId);
        if (chars != null && !chars.isEmpty()) {
            prompt.append("【本章涉及人物】\n");
            int max = Math.min(chars.size(), 5);
            for (int i = 0; i < max; i++) {
                NovelCharacter c = chars.get(i);
                prompt.append("- ").append(c.getName() != null ? c.getName() : "")
                      .append("（").append(c.getRole() != null ? c.getRole() : "未知").append("）\n");
            }
            prompt.append("\n");
        }

        // 5. 情节
        List<NovelPlotThread> threads = plotThreadMapper.selectByProjectId(projectId);
        if (threads != null && !threads.isEmpty()) {
            prompt.append("【情节状态】\n");
            for (NovelPlotThread t : threads) {
                prompt.append("- ").append(t.getName() != null ? t.getName() : "")
                      .append(" 进度").append(t.getProgress() != null ? t.getProgress() : 0).append("%\n");
            }
            prompt.append("\n");
        }

        // 6. 伏笔
        List<NovelForeshadowing> foreshadowings = foreshadowingMapper.selectByProjectId(projectId);
        if (foreshadowings != null) {
            List<NovelForeshadowing> pending = foreshadowings.stream()
                    .filter(f -> "pending".equals(f.getStatus()) || "developing".equals(f.getStatus()))
                    .toList();
            if (!pending.isEmpty()) {
                prompt.append("【待回收伏笔】\n");
                for (NovelForeshadowing f : pending) {
                    prompt.append("- ").append(f.getName() != null ? f.getName() : "");
                    if (f.getDescription() != null && !f.getDescription().isEmpty()) {
                        prompt.append("：").append(f.getDescription());
                    }
                    prompt.append("\n");
                }
                prompt.append("（标题可暗示即将回收的伏笔）\n\n");
            }
        }

        // 7. 用户方向
        prompt.append("【用户要求】").append(direction != null ? direction : "延续主线剧情").append("\n\n");

        // 8. 哨兵告警约束（创作指导）
        if (sentinelHints != null && !sentinelHints.isEmpty()) {
            prompt.append("【🔍 智能哨兵创作约束】\n");
            prompt.append("以下哨兵告警信息需要在本章中处理，标题应体现这些关键点：\n");
            for (int i = 0; i < sentinelHints.size(); i++) {
                prompt.append((i + 1)).append(". ").append(sentinelHints.get(i)).append("\n");
            }
            prompt.append("（标题应体现或暗示上述关键剧情点）\n\n");
        }

        // 9. 核心意象词禁用列表（明确标记为 "禁止使用"
        Set<String> usedKeywords = TitleDeduplicator.extractAllKeywords(existingTitles);
        if (!usedKeywords.isEmpty()) {
            prompt.append("【⚠️ 已使用的核心意象词（严禁重复使用）】\n");
            prompt.append("以下词汇已在已有标题中出现，新标题中严禁使用：\n");
            prompt.append(String.join("、", usedKeywords)).append("\n\n");
        }

        // 10. 去重指令（最强约束）
        prompt.append("【⚠️ 去重约束（最高优先级）】\n");
        prompt.append("1. 生成的标题必须与上述所有已有标题**完全不同**，包括同义或相似表达\n");
        prompt.append("2. 严禁使用上述已使用的核心意象词\n");
        prompt.append("3. 标题应暗示本章核心事件或情感，与大纲节点紧密关联\n");
        prompt.append("4. 优先使用全新的意象词，形成递进而非重复\n\n");

        // 11. 生成要求
        prompt.append("【生成要求】\n");
        prompt.append("1. 标题 5-8 个字，与已有标题风格保持一致（意象化、诗意）\n");
        prompt.append("2. 标题必须与大纲节点的主题紧密相关，但表达方式不能重复\n");
        prompt.append("3. 融入世界观元素，暗示本章核心事件或情感\n");
        prompt.append("4. 与上一章标题形成叙事递进\n");
        prompt.append("5. 如果章节涉及新人物或新冲突，标题应体现这一点\n");
        prompt.append("6. 如果存在待回收伏笔，标题可做暗示\n");
        prompt.append("\n请只返回标题文本，不要任何解释或前缀：");

        // 12. 去重校验：最多重试 3 次
        String reply = deepSeekClient.chat("你是一位专业小说作家", prompt.toString(), 0.7, 256);
        String title = cleanTitle(reply);
        int retry = 0;
        while (TitleDeduplicator.isDuplicate(title, existingTitles) && retry < 3) {
            retry++;
            log.warn("AI 生成的标题「{}」与已有标题相似，第 {} 次重试...", title, retry);
            String retryPrompt = buildStrictTitlePrompt(projectTitle, chapterIndex, title, existingTitles, direction, retry);
            String retryReply = deepSeekClient.chat("你是一位专业小说作家", retryPrompt, 0.7, 256);
            title = cleanTitle(retryReply);
        }
        // 兜底：如果 3 次重试仍重复，追加后缀
        if (TitleDeduplicator.isDuplicate(title, existingTitles)) {
            log.warn("标题「{}」经 3 次重试仍重复，追加后缀", title);
            title = title + "·新篇";
        }
        log.info("标题去重校验通过: {}", title);

        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("title");
        session.setRole("assistant");
        session.setContent(title);
        session.setTokensUsed(0);
        sessionMapper.insert(session);

        return title;
    }

    /**
     * 清理 AI 返回的标题文本
     */
    private String cleanTitle(String raw) {
        if (raw == null) return "";
        return raw
                .replaceAll("^[\"「《]|[\"」》]$", "")
                .replaceAll("\\n.*$", "")
                .trim()
                .replaceAll("^[0-9]+\\.", "")  // 去掉 "1. " 前缀
                .replaceAll("^第.+章[：:]?\\s*", "")  // 去掉 "第X章：" 前缀
                .trim();
    }

    /**
     * 构建更严格的重试 Prompt（当第一次生成重复时）
     */
    private String buildStrictTitlePrompt(String projectTitle, int chapterIndex,
                                           String duplicateTitle, List<String> existingTitles,
                                           String direction, int retryCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业小说作家，正在为小说《").append(projectTitle).append("》")
          .append("的第 ").append(chapterIndex).append(" 章创作标题。\n\n");
        sb.append("⚠️ 第 ").append(retryCount).append(" 次重试：你刚才生成了「").append(duplicateTitle)
          .append("」，该标题与已有章节标题重复！\n\n");
        sb.append("【已有标题列表（严禁重复）】\n");
        if (existingTitles != null) {
            for (int i = 0; i < existingTitles.size(); i++) {
                sb.append("第").append(i + 1).append("章：").append(existingTitles.get(i)).append("\n");
            }
        }
        sb.append("\n【⚠️ 已使用的核心意象词（严禁使用）】\n");
        Set<String> usedKw = TitleDeduplicator.extractAllKeywords(existingTitles);
        sb.append(String.join("、", usedKw)).append("\n\n");
        sb.append("请重新生成一个全新的标题，要求：\n");
        sb.append("1. 不与上述任何标题重复，包括同义或相似表达\n");
        sb.append("2. 严禁使用上述核心意象词，必须使用全新词汇\n");
        sb.append("3. 标题必须与大纲节点紧密关联\n");
        sb.append("4. 只返回标题文本，不要任何解释或前缀\n");
        if (direction != null && !direction.isEmpty()) {
            sb.append("5. 方向：").append(direction).append("\n");
        }
        sb.append("\n请返回标题：");
        return sb.toString();
    }

    private String analyzeTitleStyle(List<String> titles) {
        if (titles == null || titles.isEmpty()) return "无参考";
        List<String> recent = titles.stream().skip(Math.max(0, titles.size() - 5)).toList();
        double avgLen = recent.stream().mapToInt(s -> s.length()).average().orElse(6);
        java.util.Map<String, Integer> freq = new java.util.HashMap<>();
        for (String t : recent) {
            for (char c : t.toCharArray()) {
                if (c > 127) freq.merge(String.valueOf(c), 1, (a, b) -> a + b);
            }
        }
        List<String> top = freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(5)
                .map(e -> e.getKey()).toList();
        String pattern = "意象化表达";
        if (recent.stream().anyMatch(t -> t.contains("的"))) pattern = "意象关联式";
        if (recent.stream().anyMatch(t -> t.contains("，"))) pattern = "对仗意象式";
        return String.format("近%d章平均%.1f字；高频意象词：%s；修辞：%s",
                recent.size(), avgLen, String.join("、", top), pattern);
    }

    /**
     * 获取上一章结尾内容（最后500字），用于章节衔接
     */
    private String getPreviousChapterEnding(Long projectId, int chapterIndex) {
        if (chapterIndex <= 1) return "";
        NovelChapter prev = chapterMapper.selectBySortOrder(projectId, chapterIndex - 1);
        if (prev == null || prev.getContent() == null || prev.getContent().isEmpty()) return "";
        String content = prev.getContent();
        int start = Math.max(0, content.length() - 500);
        return content.substring(start);
    }

    /**
     * AI 流式生成章节内容（带章节衔接上下文）
     */
    public void streamGenerateContent(Long projectId, Integer chapterIndex, String title,
                                       String direction, String existingContent, String style,
                                       Integer targetWords,
                                       Consumer<String> onToken) throws IOException {
        // 获取作品标题
        NovelProject project = projectMapper.selectById(projectId);
        String projectTitle = project != null ? project.getTitle() : "未命名作品";

        // 获取上一章结尾用于衔接
        String prevEnding = getPreviousChapterEnding(projectId, chapterIndex);
        if (prevEnding.isEmpty()) {
            prevEnding = "（第一章，无上一章内容）";
        }

        String prompt = String.format(PromptTemplates.GENERATE_CONTENT,
                projectTitle,
                chapterIndex,
                prevEnding,
                title != null ? title : "未命名章节",
                direction != null ? direction : "延续故事主线，推动情节发展",
                targetWords != null ? targetWords : 2000);

        int tokensUsed = deepSeekClient.chatStream("你是一位专业小说作家", prompt, 0.8, 4096, onToken);

        // 保存会话记录
        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("content");
        session.setRole("assistant");
        session.setContent(prompt);
        session.setTokensUsed(tokensUsed);
        sessionMapper.insert(session);
    }

    /**
     * AI 润色
     */
    public String polish(Long projectId, String text, String style, String targetLength) throws IOException {
        String styleType = style != null ? style : "自然流畅";
        String prompt = PromptTemplates.POLISH
                .replace("{styleType}", styleType)
                .replace("{originalText}", text != null ? text : "");

        if (targetLength != null && !targetLength.equals("保持原长度")) {
            prompt += "\n\n【额外要求】目标长度：" + targetLength + "。";
        }

        String reply = deepSeekClient.chat("你是一位专业的文字编辑", prompt, 0.5, 4096);

        // 保存会话记录
        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("polish");
        session.setRole("assistant");
        session.setContent(reply);
        session.setTokensUsed(0);
        sessionMapper.insert(session);

        return reply;
    }

    /**
     * AI 协同创作：基于完整上下文生成章节
     */
    public String generateChapter(Long projectId, Map<String, Object> params) throws IOException {
        NovelProject project = projectMapper.selectById(projectId);
        String projectTitle = project != null ? project.getTitle() : "未命名作品";

        Integer chapterIndex = params.get("chapterIndex") != null
                ? ((Number) params.get("chapterIndex")).intValue() : 1;
        String currentContent = params.get("currentContent") != null
                ? params.get("currentContent").toString() : "";
        String direction = params.get("direction") != null
                ? params.get("direction").toString() : "";
        int targetWords = params.get("targetWords") != null
                ? ((Number) params.get("targetWords")).intValue() : 2000;

        // 获取上一章结尾用于衔接
        String prevEnding = getPreviousChapterEnding(projectId, chapterIndex);

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业小说作家，正在创作小说《").append(projectTitle).append("》。\n\n");

        // 注入上一章结尾（最高优先级衔接上下文）
        if (!prevEnding.isEmpty()) {
            prompt.append("【⚠️ 上一章结尾（必须严格承接，保持人物状态、情感基调、场景连续性）】\n");
            prompt.append(prevEnding).append("\n\n");
        }

        // 1. 世界观
        List<NovelWorldSetting> worlds = worldSettingMapper.selectByProjectId(projectId);
        if (worlds != null && !worlds.isEmpty()) {
            prompt.append("【世界观设定】\n");
            for (NovelWorldSetting w : worlds) {
                String cat = w.getCategory() != null ? w.getCategory() : "";
                String name = w.getName() != null ? w.getName() : "";
                String content = w.getContent() != null ? w.getContent() : "";
                if (!content.isEmpty()) {
                    prompt.append(cat).append(" - ").append(name).append("：")
                          .append(content.length() > 200 ? content.substring(0, 200) + "..." : content).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 2. 人物
        List<NovelCharacter> chars = characterMapper.selectByProjectId(projectId);
        if (chars != null && !chars.isEmpty()) {
            prompt.append("【人物设定】\n");
            for (NovelCharacter c : chars) {
                prompt.append("- ").append(c.getName() != null ? c.getName() : "")
                      .append("（").append(c.getRole() != null ? c.getRole() : "未知")
                      .append("）：").append(c.getPersonality() != null ? c.getPersonality() : "");
                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    String desc = c.getDescription().length() > 100 ? c.getDescription().substring(0, 100) + "..." : c.getDescription();
                    prompt.append(" 背景：").append(desc);
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        // 3. 大纲
        List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
        if (outlines != null && !outlines.isEmpty()) {
            prompt.append("【大纲结构】\n");
            for (NovelOutline o : outlines) {
                prompt.append("- ").append(o.getTitle() != null ? o.getTitle() : "");
                if (o.getDescription() != null && !o.getDescription().isEmpty()) {
                    prompt.append("：").append(o.getDescription());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        // 4. 情节
        List<NovelPlotThread> threads = plotThreadMapper.selectByProjectId(projectId);
        if (threads != null && !threads.isEmpty()) {
            prompt.append("【情节引擎】\n");
            for (NovelPlotThread t : threads) {
                prompt.append("- ").append(t.getName() != null ? t.getName() : "")
                      .append(" 进度：").append(t.getProgress() != null ? t.getProgress() : 0).append("%");
                if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                    prompt.append(" ").append(t.getDescription());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        // 5. 灵感
        List<NovelInspiration> inspirations = inspirationMapper.selectByProjectId(projectId);
        if (inspirations != null && !inspirations.isEmpty()) {
            prompt.append("【灵感素材】\n");
            int max = Math.min(inspirations.size(), 5);
            for (int i = 0; i < max; i++) {
                NovelInspiration insp = inspirations.get(i);
                String c = insp.getContent() != null ? insp.getContent() : "";
                prompt.append("- ").append(c.length() > 150 ? c.substring(0, 150) + "..." : c).append("\n");
            }
            prompt.append("\n");
        }

        // 6. 伏笔
        List<NovelForeshadowing> foreshadowings = foreshadowingMapper.selectByProjectId(projectId);
        if (foreshadowings != null && !foreshadowings.isEmpty()) {
            long pending = foreshadowings.stream().filter(f -> f.getStatus() != null &&
                    (f.getStatus().equals("pending") || f.getStatus().equals("developing"))).count();
            if (pending > 0) {
                prompt.append("【待回收伏笔】\n");
                foreshadowings.stream().filter(f -> f.getStatus() != null &&
                        (f.getStatus().equals("pending") || f.getStatus().equals("developing"))).forEach(f -> {
                    prompt.append("- ").append(f.getName() != null ? f.getName() : "");
                    if (f.getDescription() != null && !f.getDescription().isEmpty()) {
                        prompt.append("：").append(f.getDescription());
                    }
                    prompt.append("\n");
                });
                prompt.append("\n");
            }
        }

        // 7. 前文
        List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);
        if (chapters != null && !chapters.isEmpty()) {
            int start = Math.max(0, chapters.size() - 3);
            prompt.append("【前文内容（最近章节）】\n");
            for (int i = start; i < chapters.size(); i++) {
                NovelChapter ch = chapters.get(i);
                String title = ch.getTitle() != null ? ch.getTitle() : "未命名";
                String content = ch.getContent() != null ? ch.getContent() : "";
                String summary = content.length() > 300 ? content.substring(0, 300) + "..." : content;
                prompt.append("第").append(i + 1).append("章 ").append(title).append("\n").append(summary).append("\n\n");
            }
        }

        // 8. 当前已有内容
        if (currentContent != null && !currentContent.isEmpty()) {
            prompt.append("【当前章节已有内容】\n");
            String c = currentContent.length() > 500 ? currentContent.substring(0, 500) + "..." : currentContent;
            prompt.append(c).append("\n\n");
        }

        // 9. 用户额外指令
        if (direction != null && !direction.isEmpty()) {
            prompt.append("【用户额外要求】\n");
            prompt.append(direction).append("\n\n");
        }

        // 10. 写作指令
        prompt.append("【写作要求】\n");
        prompt.append("1. 开头必须从上一章结尾处自然延续，不要跳跃时间或场景\n");
        prompt.append("2. 人物状态、情感、场景保持连贯，与上一章结尾无缝衔接\n");
        prompt.append("3. 请严格遵循以上世界观、人物设定进行创作\n");
        prompt.append("4. 推动主线进度，呼应已有伏笔\n");
        prompt.append("5. 保持人物性格一致\n");
        prompt.append("6. 目标字数：约 ").append(targetWords).append(" 字\n");
        prompt.append("\n【强制格式要求】\n");
        prompt.append("1. 每个自然段结束后必须换行\n");
        prompt.append("2. 段落之间必须有空行（即两个换行）\n");
        prompt.append("3. 每段文字不超过 200 字\n");
        prompt.append("4. 所有对话必须用「」括起来，并独立成行（前后各有空行）\n");
        prompt.append("5. 场景转换时用「---」分隔（前后各空行）\n");
        prompt.append("6. 使用感官描写（视觉/听觉/触觉/嗅觉）\n");
        prompt.append("7. 【重要】不要添加任何结构标签（如\"开篇场景\"、\"发展\"、\"高潮\"、\"结尾\"等），直接输出正文\n");
        prompt.append("8. 无需输出标题，直接开始写正文内容\n");

        String reply = deepSeekClient.chat("你是一位专业小说作家", prompt.toString(), 0.7, 8192);

        // 保存会话记录
        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("cowrite");
        session.setRole("assistant");
        session.setContent(reply);
        session.setTokensUsed(0);
        sessionMapper.insert(session);

        return reply;
    }

    /**
     * AI 扩写
     */
    public String expand(Long projectId, String currentContent, String direction, String style) throws IOException {
        String prompt = PromptTemplates.EXPAND
                .replace("{currentContent}", currentContent != null ? currentContent : "")
                .replace("{direction}", direction != null ? direction : "延续故事主线，丰富细节")
                .replace("{style}", style != null ? style : "自然流畅");

        String reply = deepSeekClient.chat("你是一位专业小说作家", prompt, 0.7, 8192);

        // 保存会话记录
        NovelAiSession session = new NovelAiSession();
        session.setProjectId(projectId);
        session.setSessionType("expand");
        session.setRole("assistant");
        session.setContent(reply);
        session.setTokensUsed(0);
        sessionMapper.insert(session);

        return reply;
    }
}