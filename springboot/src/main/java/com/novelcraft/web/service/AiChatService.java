package com.novelcraft.web.service;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.config.AiProperties;
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
    private final AiProperties aiProperties;

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
     * 流式对话（增强版：应用完整作品上下文）
     */
    public void streamChat(Long projectId, String novelTitle, String genre,
                            String currentChapter, String context, String userMessage,
                            double temperature, int maxTokens, Consumer<String> onToken) throws IOException {
        log.info("开始构建Chat Prompt, novelTitle={}, genre={}", novelTitle, genre);

        String worldSettings = buildWorldSettingsContext(projectId);
        String outline = buildOutlineContext(projectId);
        String characters = buildCharactersContext(projectId);
        String foreshadowings = buildForeshadowingsContext(projectId);

        String prompt = PromptTemplates.CHAT
                .replace("{novelTitle}", novelTitle != null ? novelTitle : "")
                .replace("{genre}", genre != null ? genre : "")
                .replace("{worldSettings}", worldSettings)
                .replace("{outline}", outline)
                .replace("{characters}", characters)
                .replace("{foreshadowings}", foreshadowings)
                .replace("{currentChapter}", currentChapter != null ? currentChapter : "")
                .replace("{userMessage}", userMessage);

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
     * 构建世界观上下文
     */
    private String buildWorldSettingsContext(Long projectId) {
        try {
            List<NovelWorldSetting> worlds = worldSettingMapper.selectByProjectId(projectId);
            if (worlds == null || worlds.isEmpty()) {
                return "（暂无世界观设定）";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (NovelWorldSetting w : worlds) {
                if (count >= 3) break;
                if (w.getName() != null) {
                    sb.append("- ").append(w.getName());
                    if (w.getCategory() != null) sb.append("（").append(w.getCategory()).append("）");
                    sb.append("\n");
                }
                count++;
            }
            return sb.length() > 0 ? sb.toString().trim() : "（暂无世界观设定）";
        } catch (Exception e) {
            log.warn("加载世界观失败: {}", e.getMessage());
            return "（暂无世界观设定）";
        }
    }

    /**
     * 构建大纲上下文
     */
    private String buildOutlineContext(Long projectId) {
        try {
            List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
            if (outlines == null || outlines.isEmpty()) {
                return "（暂无大纲）";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (NovelOutline o : outlines) {
                if (count >= 3) break;
                if (o.getTitle() != null) {
                    sb.append("- ").append(o.getTitle());
                    if (o.getDescription() != null && !o.getDescription().isEmpty()) {
                        String desc = o.getDescription();
                        if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
                        sb.append("：").append(desc);
                    }
                    sb.append("\n");
                }
                count++;
            }
            return sb.length() > 0 ? sb.toString().trim() : "（暂无大纲）";
        } catch (Exception e) {
            log.warn("加载大纲失败: {}", e.getMessage());
            return "（暂无大纲）";
        }
    }

    /**
     * 构建人物上下文
     */
    private String buildCharactersContext(Long projectId) {
        try {
            List<NovelCharacter> characters = characterMapper.selectByProjectId(projectId);
            if (characters == null || characters.isEmpty()) {
                return "（暂无人物设定）";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (NovelCharacter c : characters) {
                if (count >= 5) break;
                if (c.getName() != null) {
                    sb.append("- ").append(c.getName());
                    if (c.getRole() != null) sb.append("（").append(c.getRole()).append("）");
                    if (c.getPersonality() != null && !c.getPersonality().isEmpty()) {
                        sb.append("：性格").append(c.getPersonality());
                    }
                    sb.append("\n");
                }
                count++;
            }
            return sb.length() > 0 ? sb.toString().trim() : "（暂无人物设定）";
        } catch (Exception e) {
            log.warn("加载人物失败: {}", e.getMessage());
            return "（暂无人物设定）";
        }
    }

    /**
     * 构建伏笔上下文
     */
    private String buildForeshadowingsContext(Long projectId) {
        try {
            List<NovelForeshadowing> foreshadowings = foreshadowingMapper.selectByProjectId(projectId);
            if (foreshadowings == null || foreshadowings.isEmpty()) {
                return "（暂无伏笔）";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (NovelForeshadowing f : foreshadowings) {
                if (count >= 5) break;
                if (f.getName() != null) {
                    sb.append("- ").append(f.getName());
                    if (f.getStatus() != null) {
                        sb.append("[").append(getForeshadowingStatus(f.getStatus())).append("]");
                    }
                    if (f.getDescription() != null && !f.getDescription().isEmpty()) {
                        sb.append("：").append(f.getDescription());
                    }
                    sb.append("\n");
                }
                count++;
            }
            return sb.length() > 0 ? sb.toString().trim() : "（暂无伏笔）";
        } catch (Exception e) {
            log.warn("加载伏笔失败: {}", e.getMessage());
            return "（暂无伏笔）";
        }
    }

    private String getForeshadowingStatus(String status) {
        if (status == null) return "未设置";
        return switch (status) {
            case "pending" -> "待埋设";
            case "developing" -> "发展中";
            case "resolved" -> "已回收";
            case "abandoned" -> "已放弃";
            default -> status;
        };
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
     * 多层防御：完整 Prompt 尝试 → 轻量 Prompt 重试 → 大纲/哨兵降级
     */
    public String generateTitle(Long projectId, Map<String, Object> params) throws IOException {
        Exception lastError = null;
        // 尝试 1：完整上下文 Prompt
        try {
            String title = generateTitleInternal(projectId, params);
            if (isValidTitle(title)) {
                return title;
            }
            lastError = new IllegalStateException("标题无效: " + title);
            log.warn("[标题生成] 首次生成标题无效: {}", title);
        } catch (Exception e) {
            lastError = e;
            log.warn("[标题生成] 首次尝试失败: {}", e.getMessage());
        }
        // 尝试 2-3：轻量 Prompt（去掉上下文，提高成功率）
        for (int attempt = 2; attempt <= 3; attempt++) {
            try {
                String title = generateTitleLight(projectId, params);
                if (isValidTitle(title)) {
                    return title;
                }
                log.warn("[标题生成] 第{}次重试标题无效: {}", attempt, title);
            } catch (Exception e) {
                log.warn("[标题生成] 第{}次重试失败: {}", attempt, e.getMessage());
            }
        }
        log.error("AI生成标题多次失败，使用降级方案: {}", lastError != null ? lastError.getMessage() : "未知原因");
        return generateTitleFallback(projectId, params);
    }

    /**
     * AI 生成章节标题（内部实现）
     */
    private String generateTitleInternal(Long projectId, Map<String, Object> params) throws IOException {
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
              .append("第 ").append(chapterIndex).append(" 章创作一个章节标题。\n\n");

        // 精简输入：仅提供近期标题作为风格示例（不列禁用词，避免模型过度分析）
        if (existingTitles != null && !existingTitles.isEmpty()) {
            List<String> recent = existingTitles.stream()
                    .skip(Math.max(0, existingTitles.size() - 3))
                    .toList();
            prompt.append("【近期标题风格示例】：").append(String.join("｜", recent)).append("\n");
        }

        // 大纲：仅取前 3 个章节节点作为元素参考
        List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
        log.info("[标题生成] 项目{}查询到大纲{}条", projectId, outlines != null ? outlines.size() : 0);
        if (outlines != null && !outlines.isEmpty()) {
            List<String> nodeTitles = outlines.stream()
                    .filter(o -> !"volume".equals(o.getType()))
                    .filter(o -> o.getTitle() != null && !o.getTitle().isEmpty())
                    .limit(3)
                    .map(NovelOutline::getTitle)
                    .toList();
            if (!nodeTitles.isEmpty()) {
                prompt.append("【大纲关键节点】：").append(String.join("、", nodeTitles)).append("\n");
            }
        }

        // 世界观关键词（最多 3 个）
        List<NovelWorldSetting> worlds = worldSettingMapper.selectByProjectId(projectId);
        log.info("[标题生成] 项目{}查询到世界观{}条", projectId, worlds != null ? worlds.size() : 0);
        if (worlds != null && !worlds.isEmpty()) {
            List<String> keywords = new java.util.ArrayList<>();
            for (NovelWorldSetting w : worlds) {
                if (w.getName() != null) keywords.add(w.getName());
                if (keywords.size() >= 3) break;
            }
            if (!keywords.isEmpty()) {
                prompt.append("【世界观元素】：").append(String.join("、", keywords)).append("\n");
            }
        }

        // 人物（最多 3 个）
        List<NovelCharacter> chars = characterMapper.selectByProjectId(projectId);
        log.info("[标题生成] 项目{}查询到人物{}条", projectId, chars != null ? chars.size() : 0);
        if (chars != null && !chars.isEmpty()) {
            List<String> names = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(chars.size(), 3); i++) {
                if (chars.get(i).getName() != null) names.add(chars.get(i).getName());
            }
            if (!names.isEmpty()) {
                prompt.append("【人物】：").append(String.join("、", names)).append("\n");
            }
        }

        // 待回收伏笔（最多 2 个）
        List<NovelForeshadowing> foreshadowings = foreshadowingMapper.selectByProjectId(projectId);
        if (foreshadowings != null) {
            List<NovelForeshadowing> pending = foreshadowings.stream()
                    .filter(f -> "pending".equals(f.getStatus()) || "developing".equals(f.getStatus()))
                    .limit(2)
                    .toList();
            log.info("[标题生成] 项目{}查询到待回收伏笔{}条", projectId, pending.size());
            if (!pending.isEmpty()) {
                List<String> names = pending.stream()
                        .map(f -> f.getName() != null ? f.getName() : "")
                        .filter(s -> !s.isEmpty())
                        .toList();
                if (!names.isEmpty()) {
                    prompt.append("【伏笔】：").append(String.join("、", names)).append("\n");
                }
            }
        }

        // 哨兵约束
        if (sentinelHints != null && !sentinelHints.isEmpty()) {
            prompt.append("【创作要点】：").append(String.join("；", sentinelHints.stream().limit(2).toList())).append("\n");
        }

        // 用户方向
        prompt.append("【方向】：").append(direction).append("\n");

        // 强制输出格式：只返回标题文本，禁止任何推理/解释/前缀
        prompt.append("\n【输出要求】\n");
        prompt.append("1. 直接输出 5-8 字的标题文本，只输出这一个标题，不要任何多余内容；\n");
        prompt.append("2. 严禁输出任何推理过程、分析、解释、思考或自我评价；\n");
        prompt.append("3. 严禁添加引号、书名号、序号、冒号等任何前缀或后缀；\n");
        prompt.append("4. 标题需融入世界观或大纲元素，与近期标题风格一致，但避免重复用词。\n");
        prompt.append("\n标题：");

        // 标题任务固定小 token，并设置 stop 序列提前终止，
        // 避免模型将预算消耗在推理过程上导致 finish_reason=length 截断
        String fullPrompt = prompt.toString();
        log.info("[标题生成] 项目{}最终Prompt长度{}字，前150字：{}", projectId, fullPrompt.length(), fullPrompt.substring(0, Math.min(150, fullPrompt.length())));
        String reply = deepSeekClient.chat("你是一位专业小说作家。只输出标题本身，绝不输出任何推理过程或解释。",
                fullPrompt, 0.7, 2048, List.of("\n\n"));
        log.info("[标题生成] 项目{} AI原始回复：{}", projectId, reply);
        String title = cleanTitle(reply);
        int retry = 0;
        while ((TitleDeduplicator.isDuplicate(title, existingTitles) || !isValidTitle(title)) && retry < 3) {
            retry++;
            log.warn("AI 生成的标题「{}」与已有标题相似或无效，第 {} 次重试...", title, retry);
            String retryPrompt = buildStrictTitlePrompt(projectTitle, chapterIndex, title, existingTitles, direction, retry);
            String retryReply = deepSeekClient.chat("你是一位专业小说作家。只输出标题本身，绝不输出任何推理过程或解释。",
                    retryPrompt, 0.7, 2048, List.of("\n\n"));
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
     * 清理 AI 返回的标题文本（去除前缀、引号与推理残留）
     */
    private String cleanTitle(String raw) {
        if (raw == null) return "";
        String cleaned = raw
                .replaceAll("^[\"「《]|[\"」》]$", "")
                .replaceAll("\\n.*$", "")
                .trim()
                .replaceAll("^[0-9]+\\.", "")  // 去掉 "1. " 前缀
                .replaceAll("^第.+章[：:]?\\s*", "")  // 去掉 "第X章：" 前缀
                .replaceAll("^(标题|章节标题|标题为)[：:]?\\s*", "")  // 去掉 "标题：" 前缀
                .replaceAll("^(好的|好的，|抱歉|不好意思|作为|直接输出)[，：:]?\\s*", "")  // 词穷自否定残留
                .trim();
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        return cleaned;
    }

    /**
     * 构建更严格的重试 Prompt（当第一次生成重复时）
     * 精简输入：仅提供重复标题与近期标题，避免模型过度分析
     */
    private String buildStrictTitlePrompt(String projectTitle, int chapterIndex,
                                           String duplicateTitle, List<String> existingTitles,
                                           String direction, int retryCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业小说作家，为小说《").append(projectTitle).append("》")
          .append("第 ").append(chapterIndex).append(" 章创作标题。\n\n");
        sb.append("你刚才的标题「").append(duplicateTitle)
          .append("」与已有标题重复，请换一个不重复的标题。\n\n");
        sb.append("【已有标题】：");
        if (existingTitles != null && !existingTitles.isEmpty()) {
            List<String> recent = existingTitles.stream()
                    .skip(Math.max(0, existingTitles.size() - 3))
                    .toList();
            sb.append(String.join("｜", recent));
        }
        sb.append("\n");
        if (direction != null && !direction.isEmpty()) {
            sb.append("【方向】：").append(direction).append("\n");
        }
        sb.append("\n【输出要求】\n");
        sb.append("直接输出 5-8 字标题文本，只输出标题本身；\n");
        sb.append("严禁输出推理过程、分析、解释、引号、书名号、序号或冒号。\n");
        sb.append("\n标题：");
        return sb.toString();
    }

    /**
     * 轻量 Prompt 标题生成（重试用）：去掉上下文，只保留核心指令，提高成功率
     */
    private String generateTitleLight(Long projectId, Map<String, Object> params) throws IOException {
        NovelProject project = projectMapper.selectById(projectId);
        String projectTitle = project != null ? project.getTitle() : "未命名作品";
        Integer chapterIndex = params.get("chapterIndex") != null
                ? ((Number) params.get("chapterIndex")).intValue() : 1;
        String direction = params.get("direction") != null
                ? params.get("direction").toString() : "";

        StringBuilder sb = new StringBuilder();
        sb.append("为小说《").append(projectTitle).append("》第 ").append(chapterIndex).append(" 章创作一个 5-8 字章节标题。\n");
        if (direction != null && !direction.isEmpty()) {
            sb.append("创作方向：").append(direction).append("\n");
        }
        sb.append("直接输出标题，禁止任何推理、解释、分析、思考过程、引号、序号或冒号。\n");
        sb.append("标题：");

        String reply = deepSeekClient.chat("立即输出标题本身，不要思考也不要解释。",
                sb.toString(), 0.7, 2048, List.of("\n\n"));
        return cleanTitle(reply);
    }

    /**
     * 校验标题有效性：长度、标点、推理残留等
     */
    private boolean isValidTitle(String title) {
        if (title == null) return false;
        String t = title.trim();
        if (t.isEmpty() || t.length() < 2 || t.length() > 20) return false;
        // 含标点/符号视为无效（标题应为纯文本）
        if (t.matches(".*[：:，。！？、；;,.!?\\[\\]（）()\\-—].*")) return false;
        // 推理/自否定残留词
        if (t.contains("思考") || t.contains("分析") || t.contains("抱歉") || t.contains("不好意思")
                || t.contains("好的") || t.contains("标题") || t.contains("让我") || t.contains("直接")) {
            return false;
        }
        return true;
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
     * 构建伏笔回收上下文（按紧急程度排序）
     * 只包含未回收且接近回收期限的伏笔
     */
    private String buildForeshadowingRecoveryPrompt(Long projectId, int currentChapter, int maxEntries) {
        List<NovelForeshadowing> urgent = foreshadowingMapper.selectUrgentByProject(projectId, currentChapter);
        if (urgent == null || urgent.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【伏笔回收任务】（重要！）\n");
        sb.append("当前有 ").append(urgent.size()).append(" 条伏笔已到回收时机，请在生成本章内容时以合理、自然的方式回收至少一条：\n\n");

        int count = 0;
        for (NovelForeshadowing f : urgent) {
            if (count >= maxEntries) break;
            int buriedIn = f.getChapterId() != null ? f.getChapterId().intValue() : 1;
            int passed = currentChapter - buriedIn;
            String urgency = passed >= 3 ? "⚠️紧急" : (passed >= 1 ? "📍届时" : "🔜即将");

            sb.append("- ").append(urgency).append(" 伏笔「").append(f.getName() != null ? f.getName() : "").append("」");
            sb.append("（埋于第").append(buriedIn).append("章，已过").append(passed).append("章）");
            if (f.getDescription() != null && !f.getDescription().isEmpty()) {
                sb.append("\n  描述：").append(f.getDescription());
            }
            sb.append("\n");
            count++;
        }

        sb.append("\n回收要求：\n");
        sb.append("1. 以自然、流畅的方式将伏笔融入本章情节\n");
        sb.append("2. 可通过角色对话、回忆、事件等方式揭示伏笔\n");
        sb.append("3. 不要生硬地为了回收而回收，保持故事连贯性\n");
        sb.append("4. 如果无法在本章自然回收，可适当铺垫但不要忽略\n\n");

        return sb.toString();
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

        String basePrompt = String.format(PromptTemplates.GENERATE_CONTENT,
                projectTitle,
                chapterIndex,
                prevEnding,
                title != null ? title : "未命名章节",
                direction != null ? direction : "延续故事主线，推动情节发展",
                targetWords != null ? targetWords : 2000);

        // 注入伏笔回收上下文
        String foreshadowingContext = buildForeshadowingRecoveryPrompt(projectId, chapterIndex, 3);
        String prompt = foreshadowingContext.isEmpty() ? basePrompt
                : basePrompt + "\n\n" + foreshadowingContext + "\n请在遵循上述要求的同时，自然地融入伏笔回收。";

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

        // 6. 伏笔（按紧迫程度排序）
        List<NovelForeshadowing> foreshadowings = foreshadowingMapper.selectUrgentByProject(projectId, chapterIndex);
        if (foreshadowings != null && !foreshadowings.isEmpty()) {
            List<NovelForeshadowing> pending = foreshadowings.stream().filter(f -> f.getStatus() != null &&
                    (f.getStatus().equals("pending") || f.getStatus().equals("developing"))).toList();
            if (!pending.isEmpty()) {
                prompt.append("【伏笔回收任务】（重要！）\n");
                prompt.append("以下伏笔已埋设一段时间，请在本章中以自然方式回收至少一条：\n\n");
                int count = 0;
                for (NovelForeshadowing f : pending) {
                    if (count >= 5) break;
                    int buriedIn = f.getChapterId() != null ? f.getChapterId().intValue() : 1;
                    int passed = chapterIndex - buriedIn;
                    String urgency = passed >= 3 ? "⚠️紧急" : (passed >= 1 ? "📍届时" : "🔜即将");
                    prompt.append("- ").append(urgency).append(" 伏笔「").append(f.getName() != null ? f.getName() : "").append("」");
                    prompt.append("（埋于第").append(buriedIn).append("章，已过").append(passed).append("章）");
                    if (f.getDescription() != null && !f.getDescription().isEmpty()) {
                        prompt.append("\n  描述：").append(f.getDescription());
                    }
                    prompt.append("\n");
                    count++;
                }
                prompt.append("\n回收要求：自然融入情节，可通过对话、回忆、事件等方式揭示\n\n");
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
        prompt.append("4. 【重要】积极回收上述伏笔，以自然方式揭示或推进伏笔情节\n");
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
    public String expand(Long projectId, String currentContent, String direction, String style, Integer chapterIndex) throws IOException {
        String prompt = PromptTemplates.EXPAND
                .replace("{currentContent}", currentContent != null ? currentContent : "")
                .replace("{direction}", direction != null ? direction : "延续故事主线，丰富细节")
                .replace("{style}", style != null ? style : "自然流畅");

        // 注入伏笔回收上下文
        if (chapterIndex != null && chapterIndex > 0) {
            String foreshadowingContext = buildForeshadowingRecoveryPrompt(projectId, chapterIndex, 2);
            if (!foreshadowingContext.isEmpty()) {
                prompt = prompt + "\n\n" + foreshadowingContext + "\n请在扩写时自然地融入伏笔回收。";
            }
        }

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

    /**
     * AI 生成标题降级方案（当 AI 调用失败时使用）
     * 优先级：大纲节点标题（与原文关联）→ 哨兵建议 → 已有标题意象 → 方向描述
     */
    private String generateTitleFallback(Long projectId, Map<String, Object> params) {
        Integer chapterIndex = params.get("chapterIndex") != null
                ? ((Number) params.get("chapterIndex")).intValue() : 1;
        @SuppressWarnings("unchecked")
        List<String> existingTitles = (List<String>) params.get("existingTitles");
        @SuppressWarnings("unchecked")
        List<String> sentinelHints = (List<String>) params.get("sentinelHints");

        String direction = params.get("direction") != null
                ? params.get("direction").toString() : "";

        String fallbackTitle = "第" + chapterIndex + "章";

        // 1) 优先使用大纲节点标题（AI 大纲已生成章节标题，与原文直接关联）
        try {
            List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
            if (outlines != null && !outlines.isEmpty()) {
                List<NovelOutline> nodes = outlines.stream()
                        .filter(o -> !"volume".equals(o.getType()))
                        .filter(o -> o.getTitle() != null && !o.getTitle().isEmpty())
                        .sorted((a, b) -> Integer.compare(
                                a.getSortOrder() != null ? a.getSortOrder() : 0,
                                b.getSortOrder() != null ? b.getSortOrder() : 0))
                        .toList();
                if (!nodes.isEmpty()) {
                    int idx = Math.min(Math.max(chapterIndex - 1, 0), nodes.size() - 1);
                    String nodeTitle = nodes.get(idx).getTitle();
                    log.info("降级标题（基于大纲节点）：{}", nodeTitle);
                    return nodeTitle;
                }
            }
        } catch (Exception e) {
            log.warn("降级标题-读取大纲失败: {}", e.getMessage());
        }

        // 2) 尝试从哨兵建议中提取关键词
        if (sentinelHints != null && !sentinelHints.isEmpty()) {
            String hint = sentinelHints.get(0);
            String keyword = hint.replaceAll("[^\\u4e00-\\u9fa5]", "").trim();
            if (keyword.length() > 0) {
                keyword = keyword.length() > 6 ? keyword.substring(0, 6) : keyword;
                fallbackTitle = "第" + chapterIndex + "章 · " + keyword;
                log.info("降级标题（基于哨兵建议）：{}", fallbackTitle);
                return fallbackTitle;
            }
        }

        // 3) 如果有已有标题，尝试提取意象词生成递进式标题
        if (existingTitles != null && !existingTitles.isEmpty()) {
            Set<String> keywords = TitleDeduplicator.extractAllKeywords(existingTitles);
            if (!keywords.isEmpty()) {
                String lastKeyword = keywords.iterator().next();
                fallbackTitle = "第" + chapterIndex + "章 · " + lastKeyword;
                log.info("降级标题（基于已有标题意象）：{}", fallbackTitle);
                return fallbackTitle;
            }
        }

        // 4) 最后尝试从方向描述中提取关键词（过滤无意义的默认指令词）
        if (direction != null && !direction.isEmpty()) {
            String keyword = direction.replaceAll("[^\\u4e00-\\u9fa5]", "").trim();
            if (keyword.length() > 0) {
                keyword = keyword.length() > 6 ? keyword.substring(0, 6) : keyword;
                // 过滤默认的通用指令词，避免生成与原文无关的标题
                boolean generic = keyword.startsWith("延续主线") || keyword.startsWith("推动情节")
                        || keyword.startsWith("延续故事") || keyword.startsWith("延续剧情");
                if (!generic) {
                    fallbackTitle = "第" + chapterIndex + "章 · " + keyword;
                    log.info("降级标题（基于方向描述）：{}", fallbackTitle);
                    return fallbackTitle;
                }
            }
        }

        log.info("降级标题（默认）：{}", fallbackTitle);
        return fallbackTitle;
    }
}