package com.mythweave.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mythweave.web.client.DeepSeekClient;
import com.mythweave.web.dto.AppendForeshadowRequest;
import com.mythweave.web.entity.NovelForeshadowing;
import com.mythweave.web.mapper.NovelChapterMapper;
import com.mythweave.web.mapper.NovelForeshadowingMapper;
import com.mythweave.web.template.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForeshadowAppendService {

    private final DeepSeekClient deepSeekClient;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final NovelChapterMapper chapterMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* 锚点引号模式：'XXX' / "XXX" / “XXX” / 「XXX」 / 『XXX』 */
    private static final Pattern ANCHOR_PATTERN =
            Pattern.compile("['\"“”『』「」]([^'\"“”『』「」]{2,40})['\"“”『』「」]");

    public Map<String, Object> appendForeshadowing(AppendForeshadowRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        String originalContent = request.getOriginalContent();
        String insertPosition = request.getInsertPosition();
        Integer cursorPosition = request.getCursorPosition();

        String insertedContent;
        int insertIndex;
        String locateMode = "manual";
        boolean isFallback = false;
        String fallbackReason = "";

        if ("auto".equals(insertPosition)) {
            LocateResult locate = locateAutoInsertIndex(request, originalContent);
            insertIndex = locate.index;
            locateMode = locate.mode;
            isFallback = locate.isFallback;
            fallbackReason = locate.reason;
        } else if ("cursor".equals(insertPosition) && cursorPosition != null) {
            insertIndex = Math.max(0, Math.min(cursorPosition, originalContent.length()));
        } else {
            insertIndex = originalContent.length();
        }

        String appendPrompt = PromptTemplates.APPEND_FORESHADOWING
                .replace("{foreshadowingTitle}", request.getForeshadowingTitle())
                .replace("{foreshadowingDescription}", request.getForeshadowingDescription())
                .replace("{originalContent}", originalContent);

        try {
            // 生成补写内容：推理型模型会先消耗大量推理 token，预算给足 8192；
            // 失败后使用轻量上下文重试（仅插入位置前后文），降低推理负担
            insertedContent = generateInsertedContent(request, appendPrompt, originalContent, insertIndex);

            insertedContent = insertedContent.trim();
            if (insertedContent.startsWith("\"") && insertedContent.endsWith("\"")) {
                insertedContent = insertedContent.substring(1, insertedContent.length() - 1);
            }

            String fullContent = originalContent.substring(0, insertIndex)
                    + insertedContent
                    + originalContent.substring(insertIndex);

            result.put("insertedContent", insertedContent);
            result.put("insertPosition", insertIndex);
            result.put("fullContent", fullContent);
            result.put("tokenUsed", insertedContent.length() / 2);
            result.put("locateMode", locateMode);
            result.put("isFallback", isFallback);
            result.put("fallbackReason", fallbackReason);

            NovelForeshadowing foreshadowing = foreshadowingMapper.selectById(request.getForeshadowingId());
            if (foreshadowing != null && !"resolved".equals(foreshadowing.getStatus())) {
                foreshadowing.setStatus("resolved");
                // 回收章节必须为实际章节ID（由 Controller 从路径注入），不能用 projectId
                if (request.getChapterId() != null) {
                    foreshadowing.setResolvedChapterId(request.getChapterId());
                }
                foreshadowingMapper.updateById(foreshadowing);
            }

            log.info("伏笔追加成功: foreshadowingId={}, insertedLen={}, locateMode={}",
                    request.getForeshadowingId(), insertedContent.length(), locateMode);

        } catch (IOException e) {
            log.error("伏笔追加失败: {}", e.getMessage(), e);
            throw new RuntimeException("伏笔追加失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 生成伏笔补写内容（含失败重试）
     * 首次使用完整章节上下文；失败后降级为插入位置前后文（轻量 prompt），再失败才抛出
     */
    private String generateInsertedContent(AppendForeshadowRequest request, String fullPrompt,
                                           String originalContent, int insertIndex) throws IOException {
        try {
            return deepSeekClient.chat(
                    "你是一位专业小说作家，擅长在情节中自然地融入伏笔。直接输出补写段落，不要任何推理过程、思考或解释。",
                    fullPrompt,
                    0.7,
                    8192
            );
        } catch (IOException e) {
            log.warn("伏笔追加首次生成失败（推理耗尽/无正文），使用轻量上下文重试: {}", e.getMessage());
            // 轻量重试：只提供插入位置前后的上下文片段，缩小输入、降低模型推理负担
            String retryPrompt = buildAppendRetryPrompt(request, originalContent, insertIndex);
            return deepSeekClient.chat(
                    "你是一位专业小说作家。禁止任何推理、分析或解释，直接输出补写段落。",
                    retryPrompt,
                    0.6,
                    8192
            );
        }
    }

    /** 构建轻量重试 Prompt：仅含伏笔信息与插入位置前后 200 字 */
    private String buildAppendRetryPrompt(AppendForeshadowRequest request, String originalContent, int insertIndex) {
        if (originalContent == null || originalContent.isEmpty()) {
            originalContent = "（无上下文）";
        }
        int start = Math.max(0, insertIndex - 200);
        int end = Math.min(originalContent.length(), insertIndex + 200);
        String around = originalContent.substring(start, end);
        String desc = request.getForeshadowingDescription() != null
                ? request.getForeshadowingDescription() : "";
        return """
                请为以下伏笔写一段 100-300 字的补写内容，插入到给定位置（前后文如下）。
                直接输出补写段落，不要任何分析、推理、解释、前缀或标注。

                【伏笔】
                标题：%s
                描述：%s

                【插入位置前后文】
                ...%s...

                直接输出补写内容：
                """.formatted(
                request.getForeshadowingTitle(),
                desc,
                around);
    }

    // ════════════════════════════════════
    // AI 定位插入位置（多级降级策略）
    // L1: 结构化 JSON 定位（position 锚点 + reason）
    // L2: 自由文本锚点提取（"在'XXX'之后"）
    // L3: 关键词段落相似度匹配（基于伏笔描述与章节内容）
    // L4: 保底插入章节结尾并标记降级
    // ════════════════════════════════════
    private LocateResult locateAutoInsertIndex(AppendForeshadowRequest request, String content) {
        if (content == null || content.isEmpty()) {
            return LocateResult.fallback(0, "章节内容为空");
        }

        // L1: AI 结构化 JSON 定位
        String reply = null;
        try {
            String positionPrompt = buildPositionPrompt(request, content);
            reply = deepSeekClient.chat(
                    "你是专业小说编辑。只输出 JSON，不要任何解释、推理过程或多余内容。",
                    positionPrompt, 0.3, 256, List.of("\n\n"));
            int idx = parseJsonAnchor(reply, content);
            if (idx >= 0) {
                log.info("[伏笔定位] JSON 定位成功: index={}", idx);
                return LocateResult.ok(idx, "ai_json");
            }
            log.warn("[伏笔定位] JSON 解析失败，尝试锚点提取。AI回复: {}", reply);
        } catch (Exception e) {
            log.warn("[伏笔定位] AI 定位调用失败: {}", e.getMessage());
        }

        // L2: 从 AI 回复（或原文）中提取锚点关键词
        if (reply != null) {
            int idx = extractAnchor(reply, content);
            if (idx >= 0) {
                log.info("[伏笔定位] 锚点提取成功: index={}", idx);
                return LocateResult.ok(idx, "ai_anchor");
            }
        }

        // L3: 关键词段落相似度匹配（伏笔描述 vs 章节内容）
        int idx = findBestMatchIndex(content, request.getForeshadowingDescription());
        if (idx >= 0) {
            log.info("[伏笔定位] 语义匹配成功: index={}", idx);
            return LocateResult.ok(idx, "semantic");
        }

        // L4: 保底插入章节结尾（非全局末尾），标记降级供前端提示
        log.warn("[伏笔定位] 全部定位方式失败，保底插入章节结尾");
        return LocateResult.fallback(content.length(), "AI 无法确定精确插入位置，已插入章节末尾，请预览后手动调整");
    }

    /** 构建定位 Prompt：强制结构化 JSON 输出 */
    private String buildPositionPrompt(AppendForeshadowRequest request, String content) {
        String desc = request.getForeshadowingDescription() != null
                ? request.getForeshadowingDescription() : "";
        return """
                你是专业小说编辑。请分析以下章节内容，为伏笔选择最佳插入位置。

                【伏笔】
                标题：%s
                描述：%s

                【章节内容】
                %s

                【输出要求】
                只输出一个紧凑 JSON 对象（不要空行、不要任何解释或推理）：
                {
                  "position": "在'（章节原文中真实存在的一句原话）'之后"，或"在'...'之前"，
                  "reason": "一句话说明插入理由"
                }

                注意：引号内的锚点必须是章节内容中逐字存在的原文片段，长度 4-20 字。
                """.formatted(
                request.getForeshadowingTitle(),
                desc,
                content);
    }

    /** L1: 解析 JSON 中的 position 锚点描述 */
    private int parseJsonAnchor(String raw, String content) {
        if (raw == null) return -1;
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end <= start) return -1;
            JsonNode node = objectMapper.readTree(raw.substring(start, end + 1));
            String position = node.path("position").asText("");
            if (position.isEmpty()) return -1;
            return resolveAnchor(position, content);
        } catch (Exception e) {
            return -1;
        }
    }

    /** L2: 从任意文本中提取"在'XXX'之后/之前"锚点 */
    private int extractAnchor(String text, String content) {
        if (text == null) return -1;
        return resolveAnchor(text, content);
    }

    /** 解析锚点描述（在'XXX'之后/之前）为字符偏移量，支持多个引号片段逐一尝试 */
    private int resolveAnchor(String positionDesc, String content) {
        if (positionDesc == null || positionDesc.isEmpty()) return -1;
        boolean isBefore = positionDesc.contains("之前") || positionDesc.contains("前面")
                || positionDesc.contains("开头") || positionDesc.contains("起始");
        boolean isAfter = positionDesc.contains("之后") || positionDesc.contains("后面")
                || positionDesc.contains("末尾") || positionDesc.contains("结尾");

        Matcher m = ANCHOR_PATTERN.matcher(positionDesc);
        while (m.find()) {
            String anchor = m.group(1);
            if (anchor == null || anchor.isEmpty()) continue;

            int idx = content.indexOf(anchor);
            if (idx < 0) {
                // 容错：AI 可能对锚点做了微调（增减末字），尝试逐字缩短匹配
                for (int cut = 1; cut <= 2 && anchor.length() - cut >= 2; cut++) {
                    String sub = anchor.substring(0, anchor.length() - cut);
                    idx = content.indexOf(sub);
                    if (idx >= 0) {
                        anchor = sub;
                        break;
                    }
                }
            }
            if (idx < 0) continue;

            int base = isBefore ? idx : idx + anchor.length();
            return Math.max(0, Math.min(base, content.length()));
        }
        return -1;
    }

    /** L3: 基于伏笔描述关键词与段落包含度的相似度匹配 */
    private int findBestMatchIndex(String content, String foreshadowDesc) {
        if (content == null || foreshadowDesc == null || foreshadowDesc.isEmpty()) return -1;
        String[] paras = content.split("\\n+");
        if (paras.length < 2) return -1;

        // 从伏笔描述提取 2-4 字中文关键词
        String descClean = foreshadowDesc.replaceAll("[^\\u4e00-\\u9fa5]", "");
        if (descClean.length() < 2) return -1;
        Set<String> keywords = new LinkedHashSet<>();
        for (int i = 0; i < descClean.length() && keywords.size() < 40; i++) {
            for (int j = i + 2; j <= Math.min(i + 4, descClean.length()) && keywords.size() < 40; j++) {
                keywords.add(descClean.substring(i, j));
            }
        }

        int bestIdx = -1;
        int bestScore = 0;
        int offset = 0;
        for (String para : paras) {
            String p = para.trim();
            if (!p.isEmpty()) {
                int score = 0;
                for (String kw : keywords) {
                    if (p.contains(kw)) score++;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestIdx = offset + p.length();
                }
            }
            offset += para.length() + 1;
        }
        if (bestScore <= 0) return -1;
        return Math.min(bestIdx, content.length());
    }

    /** 定位结果（含降级标记） */
    private static class LocateResult {
        final int index;
        final String mode;
        final boolean isFallback;
        final String reason;

        private LocateResult(int index, String mode, boolean isFallback, String reason) {
            this.index = index;
            this.mode = mode;
            this.isFallback = isFallback;
            this.reason = reason;
        }

        static LocateResult ok(int index, String mode) {
            return new LocateResult(index, mode, false, "");
        }

        static LocateResult fallback(int index, String reason) {
            return new LocateResult(index, "fallback_end", true, reason);
        }
    }
}
