package com.novelcraft.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.template.PromptTemplates;
import com.novelcraft.web.entity.*;
import com.novelcraft.web.mapper.*;
import com.novelcraft.web.utils.AIResponseCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.transaction.annotation.Transactional;

/**
 * AI 先导式小说创作系统 — 工作流编排服务
 * 6阶段：世界观 → 人物 → 大纲 → 情节引擎 → 灵感素材 → 持续写作
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NovelSetupService {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;
    private final NovelProjectMapper projectMapper;
    private final NovelWorldSettingMapper worldSettingMapper;
    private final NovelCharacterMapper characterMapper;
    private final NovelOutlineMapper outlineMapper;
    private final NovelPlotThreadMapper plotThreadMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final NovelInspirationMapper inspirationMapper;

    // 内存中的任务状态存储
    private final Map<String, GenerationTask> taskStore = new ConcurrentHashMap<>();

    // ════════════════════════════════════
    // 内部类：生成任务
    // ════════════════════════════════════
    public static class GenerationTask {
        public String id;
        public Long projectId;
        public String status; // generating | completed | failed
        public int progress;
        public List<StepStatus> steps;
        public String error;
        public Map<String, Object> context;
    }

    public static class StepStatus {
        public String name;
        public String status;
        public String detail;
    }

    // ════════════════════════════════════
    // API 1: 触发全套设定生成
    // ════════════════════════════════════
    public String generateFullSetup(Long projectId, Map<String, Object> params) {
        String taskId = "setup_" + UUID.randomUUID().toString().substring(0, 8);

        GenerationTask task = new GenerationTask();
        task.id = taskId;
        task.projectId = projectId;
        task.status = "generating";
        task.progress = 0;
        task.steps = List.of(
            newStep("world"), newStep("characters"),
            newStep("outline"), newStep("plot"), newStep("inspirations")
        );
        task.context = new HashMap<>(params);
        task.context.put("projectId", projectId);

        taskStore.put(taskId, task);

        // 异步执行
        Thread.ofVirtual().start(() -> runGeneration(taskId));

        return taskId;
    }

    // ════════════════════════════════════
    // API 2: 查询生成进度
    // ════════════════════════════════════
    public GenerationTask getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }

    // ════════════════════════════════════
    // API 3: 获取完整设定结果
    // ════════════════════════════════════
    public Map<String, Object> getFullSetup(Long projectId) {
        log.info("📥 获取完整设定，projectId: {} (类型: {})", projectId, projectId.getClass().getSimpleName());
        Map<String, Object> result = new LinkedHashMap<>();

        NovelProject project = projectMapper.selectById(projectId);
        result.put("project", project);
        log.info("  📖 project: {}", project != null ? project.getTitle() : "null");

        List<NovelWorldSetting> worlds = worldSettingMapper.selectByProjectId(projectId);
        result.put("worldSettings", worlds);
        log.info("  🌍 worldSettings: {} 条", worlds != null ? worlds.size() : 0);

        List<NovelCharacter> characters = characterMapper.selectByProjectId(projectId);
        result.put("characters", characters);
        log.info("  👤 characters: {} 条", characters != null ? characters.size() : 0);

        List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
        result.put("outlines", outlines);
        log.info("  📋 outlines: {} 条", outlines != null ? outlines.size() : 0);

        List<NovelPlotThread> threads = plotThreadMapper.selectByProjectId(projectId);
        result.put("plotThreads", threads);
        log.info("  🎯 plotThreads: {} 条", threads != null ? threads.size() : 0);

        List<NovelForeshadowing> foreshadowings = foreshadowingMapper.selectByProjectId(projectId);
        result.put("foreshadowings", foreshadowings);
        log.info("  🪝 foreshadowings: {} 条", foreshadowings != null ? foreshadowings.size() : 0);

        List<NovelInspiration> inspirations = inspirationMapper.selectByProjectId(projectId);
        result.put("inspirations", inspirations);
        log.info("  💡 inspirations: {} 条", inspirations != null ? inspirations.size() : 0);

        return result;
    }

    // ════════════════════════════════════
    // API 4-8: 分步独立模块生成（分步引导式）
    // ════════════════════════════════════

    /** 步骤1：生成世界观 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateWorld(Long projectId, Map<String, Object> params) throws IOException {
        // 清除旧数据
        worldSettingMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelWorldSetting>()
                .eq("project_id", projectId));

        String prompt = PromptTemplates.SETUP_WORLD
                .replace("{title}", str(params, "title"))
                .replace("{genre}", str(params, "genre"))
                .replace("{inspiration}", str(params, "inspiration"))
                .replace("{style}", str(params, "style"));
        String direction = str(params, "direction");
        if (!direction.isEmpty()) {
            prompt = prompt + "\n\n【用户额外要求】：" + direction;
        }

        String rawJson = callAI(prompt, 0.8, 4096);
        saveWorldSettings(projectId, rawJson);

        Map<String, Object> parsed = parseWorldForPreview(rawJson);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("parsed", parsed);
        result.put("rawText", rawJson);
        result.put("summary", summarize(rawJson, 200));
        return result;
    }

    /** 步骤2：生成人物群像 */
    public Map<String, Object> generateCharacters(Long projectId, Map<String, Object> params) throws IOException {
        characterMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelCharacter>()
                .eq("project_id", projectId));

        String worldText = str(params, "worldRaw");
        String prompt = PromptTemplates.SETUP_CHARACTERS
                .replace("{title}", str(params, "title"))
                .replace("{genre}", str(params, "genre"))
                .replace("{inspiration}", str(params, "inspiration"))
                .replace("{world}", worldText.isEmpty() ? "（暂无世界观数据）" : summarize(worldText, 800))
                .replace("{style}", str(params, "style"));
        String direction = str(params, "direction");
        if (!direction.isEmpty()) {
            prompt = prompt + "\n\n【用户额外要求】：" + direction;
        }

        String rawJson = callAI(prompt, 0.8, 4096);
        saveCharacters(projectId, rawJson);

        Map<String, Object> parsed = parseCharactersForPreview(rawJson);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("parsed", parsed);
        result.put("rawText", rawJson);
        result.put("summary", "已生成 " + str(parsed, "count") + " 位核心人物");
        return result;
    }

    /** 步骤3：生成大纲结构 */
    public Map<String, Object> generateOutline(Long projectId, Map<String, Object> params) throws IOException {
        outlineMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelOutline>()
                .eq("project_id", projectId));

        String worldText = str(params, "worldRaw");
        String charText = str(params, "charactersRaw");
        String prompt = PromptTemplates.SETUP_OUTLINE
                .replace("{title}", str(params, "title"))
                .replace("{genre}", str(params, "genre"))
                .replace("{inspiration}", str(params, "inspiration"))
                .replace("{world}", worldText.isEmpty() ? "（暂无世界观数据）" : summarize(worldText, 500))
                .replace("{characters}", charText.isEmpty() ? "（暂无人物数据）" : summarize(charText, 500))
                .replace("{style}", str(params, "style"))
                .replace("{targetChapters}", str(params, "targetChapters"));
        String direction = str(params, "direction");
        if (!direction.isEmpty()) {
            prompt = prompt + "\n\n【用户额外要求】：" + direction;
        }

        String rawJson = callAI(prompt, 0.7, 8192);
        saveOutlines(projectId, rawJson);

        Map<String, Object> parsed = parseOutlineForPreview(rawJson);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("parsed", parsed);
        result.put("rawText", rawJson);
        result.put("summary", "共 " + str(parsed, "totalChapters") + " 章，" + str(parsed, "actCount") + " 幕");
        return result;
    }

    /** 步骤4：生成情节引擎 */
    public Map<String, Object> generatePlot(Long projectId, Map<String, Object> params) throws IOException {
        plotThreadMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelPlotThread>()
                .eq("project_id", projectId));
        foreshadowingMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelForeshadowing>()
                .eq("project_id", projectId));

        String outlineText = str(params, "outlineRaw");
        String charText = str(params, "charactersRaw");
        String prompt = PromptTemplates.SETUP_PLOT
                .replace("{title}", str(params, "title"))
                .replace("{genre}", str(params, "genre"))
                .replace("{outline}", outlineText.isEmpty() ? "（暂无大纲数据）" : summarize(outlineText, 600))
                .replace("{characters}", charText.isEmpty() ? "（暂无人物数据）" : summarize(charText, 400))
                .replace("{style}", str(params, "style"));
        String direction = str(params, "direction");
        if (!direction.isEmpty()) {
            prompt = prompt + "\n\n【用户额外要求】：" + direction;
        }

        String rawJson = callAI(prompt, 0.7, 4096);
        savePlotEngine(projectId, rawJson);

        Map<String, Object> parsed = parsePlotForPreview(rawJson);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("parsed", parsed);
        result.put("rawText", rawJson);
        result.put("summary", str(parsed, "threadCount") + " 条线，" + str(parsed, "foreshadowingCount") + " 个伏笔");
        return result;
    }

    /** 步骤5：生成灵感素材 */
    public Map<String, Object> generateInspirations(Long projectId, Map<String, Object> params) throws IOException {
        inspirationMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelInspiration>()
                .eq("project_id", projectId));

        String worldText = str(params, "worldRaw");
        String charText = str(params, "charactersRaw");
        String outlineText = str(params, "outlineRaw");
        String plotText = str(params, "plotRaw");
        String prompt = PromptTemplates.SETUP_INSPIRATIONS
                .replace("{title}", str(params, "title"))
                .replace("{genre}", str(params, "genre"))
                .replace("{world}", worldText.isEmpty() ? "（暂无世界数据）" : summarize(worldText, 300))
                .replace("{characters}", charText.isEmpty() ? "（暂无人物数据）" : summarize(charText, 300))
                .replace("{outline}", outlineText.isEmpty() ? "（暂无大纲数据）" : summarize(outlineText, 300))
                .replace("{plot}", plotText.isEmpty() ? "（暂无情节数据）" : summarize(plotText, 300))
                .replace("{style}", str(params, "style"));
        String direction = str(params, "direction");
        if (!direction.isEmpty()) {
            prompt = prompt + "\n\n【用户额外要求】：" + direction;
        }

        String rawJson = callAI(prompt, 0.8, 3072);
        saveInspirations(projectId, rawJson);

        Map<String, Object> parsed = parseInspirationsForPreview(rawJson);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("parsed", parsed);
        result.put("rawText", rawJson);
        result.put("summary", "共 " + str(parsed, "count") + " 条灵感素材");
        return result;
    }

    // ════════════════════════════════════
    // 核心生成流程（一体式，保留兼容）
    // ════════════════════════════════════
    private void runGeneration(String taskId) {
        GenerationTask task = taskStore.get(taskId);
        if (task == null) return;

        Map<String, Object> ctx = task.context;
        Long projectId = task.projectId;

        try {
            // ── 阶段1：世界观 ──
            updateStep(task, "world", "generating", "正在构建世界观...");
            String worldPrompt = PromptTemplates.SETUP_WORLD
                .replace("{title}", str(ctx, "title"))
                .replace("{genre}", str(ctx, "genre"))
                .replace("{inspiration}", str(ctx, "inspiration"))
                .replace("{style}", str(ctx, "style"));
            String worldJson = callAI(worldPrompt, 0.8, 4096);
            ctx.put("worldRaw", worldJson);
            saveWorldSettings(projectId, worldJson);
            updateStep(task, "world", "completed", "世界观已生成");
            task.progress = 20;

            // ── 阶段2：人物 ──
            updateStep(task, "characters", "generating", "正在生成人物群像...");
            String charPrompt = PromptTemplates.SETUP_CHARACTERS
                .replace("{title}", str(ctx, "title"))
                .replace("{genre}", str(ctx, "genre"))
                .replace("{inspiration}", str(ctx, "inspiration"))
                .replace("{world}", summarize(worldJson, 800))
                .replace("{style}", str(ctx, "style"));
            String charJson = callAI(charPrompt, 0.8, 4096);
            ctx.put("charactersRaw", charJson);
            saveCharacters(projectId, charJson);
            updateStep(task, "characters", "completed", "人物群像已生成");
            task.progress = 40;

            // ── 阶段3：大纲 ──
            updateStep(task, "outline", "generating", "正在生成大纲结构...");
            String outlinePrompt = PromptTemplates.SETUP_OUTLINE
                .replace("{title}", str(ctx, "title"))
                .replace("{genre}", str(ctx, "genre"))
                .replace("{inspiration}", str(ctx, "inspiration"))
                .replace("{world}", summarize(worldJson, 500))
                .replace("{characters}", summarize(charJson, 500))
                .replace("{style}", str(ctx, "style"))
                .replace("{targetChapters}", str(ctx, "targetChapters"));
            String outlineJson = callAI(outlinePrompt, 0.7, 8192);
            ctx.put("outlineRaw", outlineJson);
            saveOutlines(projectId, outlineJson);
            updateStep(task, "outline", "completed", "大纲已生成");
            task.progress = 60;

            // ── 阶段4：情节引擎 ──
            updateStep(task, "plot", "generating", "正在生成情节线与伏笔...");
            String plotPrompt = PromptTemplates.SETUP_PLOT
                .replace("{title}", str(ctx, "title"))
                .replace("{genre}", str(ctx, "genre"))
                .replace("{outline}", summarize(outlineJson, 600))
                .replace("{characters}", summarize(charJson, 400))
                .replace("{style}", str(ctx, "style"));
            String plotJson = callAI(plotPrompt, 0.7, 4096);
            ctx.put("plotRaw", plotJson);
            savePlotEngine(projectId, plotJson);
            updateStep(task, "plot", "completed", "情节引擎已生成");
            task.progress = 80;

            // ── 阶段5：灵感素材 ──
            updateStep(task, "inspirations", "generating", "正在生成灵感素材库...");
            String inspPrompt = PromptTemplates.SETUP_INSPIRATIONS
                .replace("{title}", str(ctx, "title"))
                .replace("{genre}", str(ctx, "genre"))
                .replace("{world}", summarize(worldJson, 300))
                .replace("{characters}", summarize(charJson, 300))
                .replace("{outline}", summarize(outlineJson, 300))
                .replace("{plot}", summarize(plotJson, 300))
                .replace("{style}", str(ctx, "style"));
            String inspJson = callAI(inspPrompt, 0.8, 3072);
            ctx.put("inspirationsRaw", inspJson);
            saveInspirations(projectId, inspJson);
            updateStep(task, "inspirations", "completed", "灵感素材库已生成");
            task.progress = 100;

            task.status = "completed";

        } catch (Exception e) {
            log.error("AI 设定生成失败", e);
            task.status = "failed";
            task.error = e.getMessage();
        }
    }

    /**
     * 回退保存：当 AI 响应 JSON 解析失败时，为每个类别都保存原始文本
     */
    private void saveWorldSettingsFallback(Long projectId, String json) {
        String content = json != null ? json : "";
        insertSetting(projectId, "时代背景", "时代背景", content, 1);
        insertSetting(projectId, "地理版图", "地理版图", content, 1);
        insertSetting(projectId, "历史年表", "历史年表", content, 1);
        insertSetting(projectId, "力量体系", "力量体系", content, 1);
        insertSetting(projectId, "核心规则", "核心规则", content, 1);
    }

    private void insertSetting(Long projectId, String name, String category, String content, Integer level) {
        NovelWorldSetting s = new NovelWorldSetting();
        s.setProjectId(projectId);
        s.setName(name);
        s.setCategory(category);
        s.setContent(content);
        s.setLevel(level);
        worldSettingMapper.insert(s);
    }

    // ════════════════════════════════════
    // 分步生成 — 预览数据解析
    // ════════════════════════════════════

    private Map<String, Object> parseWorldForPreview(String rawJson) {
        Map<String, Object> preview = new LinkedHashMap<>();
        try {
            String cleaned = AIResponseCleaner.extractJson(rawJson);
            if (cleaned != null) {
                JsonNode root = objectMapper.readTree(cleaned);
                preview.put("era", root.path("era").asText(""));
                preview.put("geography", root.path("geography").asText(""));
                preview.put("history", root.path("history").asText(""));
                preview.put("powerSystem", root.path("powerSystem").asText(""));
                JsonNode factions = root.path("factions");
                int factionCount = factions.isArray() ? factions.size() : 0;
                preview.put("factionCount", factionCount);
                List<Map<String, String>> factionList = new ArrayList<>();
                if (factions.isArray()) {
                    for (JsonNode f : factions) {
                        Map<String, String> faction = new LinkedHashMap<>();
                        faction.put("name", f.path("name").asText(""));
                        faction.put("description", f.path("description").asText(""));
                        faction.put("goal", f.path("goal").asText(""));
                        factionList.add(faction);
                    }
                }
                preview.put("factionList", factionList);
                preview.put("uniqueRules", root.path("uniqueRules").asText(""));
            }
        } catch (Exception e) {
            log.warn("解析世界观预览失败", e);
        }
        return preview;
    }

    private Map<String, Object> parseCharactersForPreview(String rawJson) {
        Map<String, Object> preview = new LinkedHashMap<>();
        try {
            String cleaned = AIResponseCleaner.extractJson(rawJson);
            if (cleaned != null) {
                JsonNode root = objectMapper.readTree(cleaned);
                JsonNode chars = root.path("characters");
                int count = chars.isArray() ? chars.size() : 0;
                preview.put("count", count);
                List<Map<String, String>> list = new ArrayList<>();
                if (chars.isArray()) {
                    for (JsonNode c : chars) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("name", c.path("name").asText(""));
                        item.put("role", c.path("role").asText(""));
                        item.put("personality", c.path("personality").asText(""));
                        list.add(item);
                    }
                }
                preview.put("characters", list);
            }
        } catch (Exception e) {
            log.warn("解析人物预览失败", e);
        }
        return preview;
    }

    private Map<String, Object> parseOutlineForPreview(String rawJson) {
        Map<String, Object> preview = new LinkedHashMap<>();
        try {
            String cleaned = AIResponseCleaner.extractJson(rawJson);
            if (cleaned != null) {
                JsonNode root = objectMapper.readTree(cleaned);
                JsonNode acts = root.path("acts");
                int actCount = acts.isArray() ? acts.size() : 0;
                int totalChapters = 0;
                if (acts.isArray()) {
                    for (JsonNode act : acts) {
                        JsonNode chapters = act.path("chapters");
                        if (chapters.isArray()) {
                            totalChapters += chapters.size();
                        }
                    }
                }
                preview.put("actCount", actCount);
                preview.put("totalChapters", totalChapters);
                preview.put("template", root.path("template").asText("three-act"));
            }
        } catch (Exception e) {
            log.warn("解析大纲预览失败", e);
        }
        return preview;
    }

    private Map<String, Object> parsePlotForPreview(String rawJson) {
        Map<String, Object> preview = new LinkedHashMap<>();
        try {
            String cleaned = AIResponseCleaner.extractJson(rawJson);
            if (cleaned != null) {
                JsonNode root = objectMapper.readTree(cleaned);
                int threadCount = 1; // main thread
                JsonNode subs = root.path("subThreads");
                if (subs.isArray()) threadCount += subs.size();
                preview.put("threadCount", threadCount);
                preview.put("mainThreadTitle", root.path("mainThread").path("title").asText(""));
                JsonNode fs = root.path("foreshadowing");
                int foreshadowingCount = fs.isArray() ? fs.size() : 0;
                preview.put("foreshadowingCount", foreshadowingCount);
            }
        } catch (Exception e) {
            log.warn("解析情节预览失败", e);
        }
        return preview;
    }

    private Map<String, Object> parseInspirationsForPreview(String rawJson) {
        Map<String, Object> preview = new LinkedHashMap<>();
        try {
            String cleaned = AIResponseCleaner.extractJson(rawJson);
            if (cleaned != null) {
                JsonNode root = objectMapper.readTree(cleaned);
                JsonNode items = root.path("items");
                int count = items.isArray() ? items.size() : 0;
                preview.put("count", count);
            }
        } catch (Exception e) {
            log.warn("解析灵感预览失败", e);
        }
        return preview;
    }

    // ════════════════════════════════════
    // 辅助方法
    // ════════════════════════════════════
    private String callAI(String prompt, double temp, int maxTokens) throws IOException {
        String raw = deepSeekClient.chat("你是一位专业的小说创作助手", prompt, temp, maxTokens);
        log.debug("AI 原始响应（前200字）: {}", raw != null ? raw.substring(0, Math.min(200, raw.length())) : "null");
        return raw;
    }

    private String str(Map<String, Object> ctx, String key) {
        Object v = ctx.get(key);
        return v != null ? v.toString() : "";
    }

    /** 截取摘要（取前N字符） */
    private String summarize(String text, int maxLen) {
        if (text == null || text.isEmpty()) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /** AI 关系类型映射为英文 */
    @SuppressWarnings("unused")
    private String mapRelationType(String aiType) {
        if (aiType == null) return "other";
        return switch (aiType) {
            case "盟友" -> "ally";
            case "敌对" -> "enemy";
            case "恋人" -> "lover";
            case "师徒" -> "mentor";
            case "父子", "母子", "兄弟", "姐妹" -> "family";
            case "对手", "宿敌" -> "rival";
            default -> "other";
        };
    }

    private StepStatus newStep(String name) {
        StepStatus s = new StepStatus();
        s.name = name;
        s.status = "pending";
        s.detail = "等待生成";
        return s;
    }

    private void updateStep(GenerationTask task, String name, String status, String detail) {
        task.steps.stream().filter(s -> s.name.equals(name)).findFirst().ifPresent(s -> {
            s.status = status;
            s.detail = detail;
        });
    }

    // ════════════════════════════════════
    // 数据持久化
    // ════════════════════════════════════

    /** AI 世界观字段 → 中文显示名映射表（未映射的字段直接使用原字段名） */
    private static final Map<String, String> WORLD_FIELD_MAP = Map.ofEntries(
        Map.entry("era", "时代背景"),
        Map.entry("geography", "地理版图"),
        Map.entry("history", "历史年表"),
        Map.entry("powerSystem", "力量体系"),
        Map.entry("magicSystem", "力量体系"),
        Map.entry("factions", "政治势力"),
        Map.entry("politics", "政治势力"),
        Map.entry("uniqueRules", "核心规则"),
        Map.entry("culture", "文化社会"),
        Map.entry("technology", "科技文明"),
        Map.entry("races", "种族设定"),
        Map.entry("gods", "信仰神明"),
        Map.entry("ecology", "生态环境")
    );

    /**
     * 保存世界观：遍历 AI 返回的 JSON 顶层字段，为每个字段创建独立的 world_setting 记录
     * - 文本字段：content 直接存储纯文本
     * - 数组/对象字段：content 存储序列化后的 JSON 字符串
     * - category 使用映射后的中文名（未映射的字段使用原字段名）
     * 任何新字段（如 ecology、magicSystem）都会被自动识别，无需修改代码
     */
    private void saveWorldSettings(Long projectId, String json) {
        log.info("========== 开始保存世界观 ==========");
        log.info("projectId: {}", projectId);
        try {
            String cleaned = AIResponseCleaner.extractJson(json);
            if (cleaned == null) {
                log.warn("世界观 JSON 提取失败，回退到原始文本存储。");
                throw new RuntimeException("无法从AI响应中提取世界观JSON");
            }
            log.info("世界观 JSON 提取成功，前200字: {}", cleaned.substring(0, Math.min(200, cleaned.length())));
            JsonNode root = objectMapper.readTree(cleaned);

            // 1. 删除该项目的旧记录（幂等，重新生成时保证不残留）
            worldSettingMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelWorldSetting>()
                    .eq("project_id", projectId));

            // 2. 遍历顶层字段，每个字段生成一条独立记录
            List<NovelWorldSetting> settings = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();
                if (value == null || value.isNull()) continue;

                String displayName = WORLD_FIELD_MAP.getOrDefault(key, key);
                // 文本字段直接存原文，数组/对象字段序列化为 JSON 字符串
                String content = value.isTextual() ? value.asText() : objectMapper.writeValueAsString(value);

                NovelWorldSetting ws = new NovelWorldSetting();
                ws.setProjectId(projectId);
                ws.setName(displayName);
                ws.setCategory(displayName);
                ws.setContent(content);
                ws.setLevel(1);
                settings.add(ws);
            }

            // 3. 逐个插入（每个独立 try-catch，一个失败不影响其他）
            int ok = 0;
            for (NovelWorldSetting s : settings) {
                try {
                    worldSettingMapper.insert(s);
                    ok++;
                    log.info("✅ 已插入模块: {} (category={})", s.getName(), s.getCategory());
                } catch (Exception e) {
                    log.error("❌ 插入模块失败: {} - {}", s.getName(), e.getMessage());
                }
            }
            log.info("========== 世界观保存完成，成功 {}/{} 个模块 ==========", ok, settings.size());
        } catch (Exception e) {
            log.error("保存世界观失败！异常类型: {}, 消息: {}", e.getClass().getName(), e.getMessage());
            // Fallback: 独立执行，不抛异常
            try {
                saveWorldSettingsFallback(projectId, json);
                log.info("✅ Fallback 保存完成");
            } catch (Exception fe) {
                log.error("❌ Fallback 也失败了: {}", fe.getMessage());
            }
        }
    }

    private void saveCharacters(Long projectId, String json) {
        try {
            String cleaned = AIResponseCleaner.extractJson(json);
            if (cleaned == null) {
                log.warn("人物 JSON 提取失败。原始（前200字）: {}", json != null ? json.substring(0, Math.min(200, json.length())) : "null");
                throw new RuntimeException("无法提取人物JSON");
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode chars = root.path("characters");

            if (chars.isArray()) {
                for (JsonNode c : chars) {
                    NovelCharacter ch = new NovelCharacter();
                    ch.setProjectId(projectId);
                    ch.setName(c.path("name").asText(""));
                    ch.setRole(c.path("role").asText(""));
                    ch.setAge(c.path("age").asInt(0));
                    ch.setPersonality(c.path("personality").asText(""));
                    ch.setDescription(c.path("background").asText(""));
                    ch.setRelation("动机：" + c.path("motivation").asText("")
                        + "\n弧光：" + c.path("arc").asText(""));
                    characterMapper.insert(ch);
                }
            }
        } catch (Exception e) {
            log.warn("保存人物失败，JSON前100字: {}", json != null ? json.substring(0, Math.min(100, json.length())) : "null", e);
        }
    }

    private void saveOutlines(Long projectId, String json) {
        try {
            String cleaned = AIResponseCleaner.extractJson(json);
            if (cleaned == null) {
                log.warn("大纲 JSON 提取失败。原始（前200字）: {}", json != null ? json.substring(0, Math.min(200, json.length())) : "null");
                throw new RuntimeException("无法提取大纲JSON");
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode acts = root.path("acts");
            int sort = 0;
            if (acts.isArray()) {
                for (JsonNode act : acts) {
                    // 保存幕标题
                    NovelOutline actNode = new NovelOutline();
                    actNode.setProjectId(projectId);
                    actNode.setTitle(act.path("name").asText("第X幕"));
                    actNode.setDescription(act.path("theme").asText("") + "\n冲突：" + act.path("conflict").asText(""));
                    actNode.setType("volume");
                    actNode.setSortOrder(sort++);
                    outlineMapper.insert(actNode);
                    Long parentId = actNode.getId();

                    // 保存章节
                    JsonNode chapters = act.path("chapters");
                    if (chapters.isArray()) {
                        for (JsonNode ch : chapters) {
                            NovelOutline node = new NovelOutline();
                            node.setProjectId(projectId);
                            node.setParentId(parentId);
                            node.setTitle(ch.path("title").asText(""));
                            node.setDescription(ch.path("summary").asText("")
                                + "\n关键事件：" + ch.path("keyEvent").asText(""));
                            node.setType("chapter");
                            node.setSortOrder(sort++);
                            outlineMapper.insert(node);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("保存大纲失败，JSON前100字: {}", json != null ? json.substring(0, Math.min(100, json.length())) : "null", e);
        }
    }

    private void savePlotEngine(Long projectId, String json) {
        try {
            String cleaned = AIResponseCleaner.extractJson(json);
            if (cleaned == null) {
                log.warn("情节引擎 JSON 提取失败。原始（前200字）: {}", json != null ? json.substring(0, Math.min(200, json.length())) : "null");
                throw new RuntimeException("无法提取情节JSON");
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode main = root.path("mainThread");
            NovelPlotThread mainThread = new NovelPlotThread();
            mainThread.setProjectId(projectId);
            mainThread.setName(main.path("title").asText("主线"));
            mainThread.setDescription(main.path("description").asText(""));
            mainThread.setType("main");
            mainThread.setProgress(0);
            plotThreadMapper.insert(mainThread);

            // 支线
            JsonNode subs = root.path("subThreads");
            if (subs.isArray()) {
                for (JsonNode sub : subs) {
                    NovelPlotThread st = new NovelPlotThread();
                    st.setProjectId(projectId);
                    st.setName(sub.path("title").asText(""));
                    st.setDescription(sub.path("description").asText(""));
                    st.setType("sub");
                    st.setProgress(0);
                    st.setChapters(sub.path("relatedChapters").toString());
                    plotThreadMapper.insert(st);
                }
            }

            // 伏笔
            JsonNode fs = root.path("foreshadowing");
            if (fs.isArray()) {
                for (JsonNode f : fs) {
                    NovelForeshadowing nf = new NovelForeshadowing();
                    nf.setProjectId(projectId);
                    nf.setName(f.path("title").asText(""));
                    nf.setDescription(f.path("hint").asText(""));
                    nf.setChapterId(f.path("buriedAt").asLong(0L));
                    nf.setResolvedChapterId(f.path("revealAt").asLong(0L));
                    nf.setStatus("pending");
                    nf.setSeverity("core".equals(f.path("importance").asText("")) ? "critical" : "normal");
                    foreshadowingMapper.insert(nf);
                }
            }
        } catch (Exception e) {
            log.warn("保存情节引擎失败，JSON前100字: {}", json != null ? json.substring(0, Math.min(100, json.length())) : "null", e);
        }
    }

    private void saveInspirations(Long projectId, String json) {
        try {
            String cleaned = AIResponseCleaner.extractJson(json);
            if (cleaned == null) {
                log.warn("灵感素材 JSON 提取失败。原始（前200字）: {}", json != null ? json.substring(0, Math.min(200, json.length())) : "null");
                throw new RuntimeException("无法提取灵感JSON");
            }
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    NovelInspiration insp = new NovelInspiration();
                    insp.setProjectId(projectId);
                    insp.setType(item.path("category").asText(""));
                    insp.setContent(item.path("content").asText(""));
                    insp.setTags(item.path("usageHint").asText(""));
                    inspirationMapper.insert(insp);
                }
            }
        } catch (Exception e) {
            log.warn("保存灵感素材失败，JSON前100字: {}", json != null ? json.substring(0, Math.min(100, json.length())) : "null", e);
        }
    }
}