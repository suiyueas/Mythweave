package com.novelcraft.web.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * AI 响应清洗工具：从混合文本中提取纯 JSON
 * <p>
 * 处理 AI 返回的常见问题：
 * 1. Markdown 代码块包裹（```json ... ```）
 * 2. 中文前缀/后缀（"好的，..." "这是生成结果：..."）
 * 3. 嵌套/嵌入的 JSON 结构
 * </p>
 */
public final class AIResponseCleaner {

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\[\\s*\\{.*?\\}\\s*\\]|\\{\\s*.*?\\s*\\}",
            Pattern.DOTALL
    );
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AIResponseCleaner() {}

    /**
     * 从 AI 返回的混合文本中提取第一个完整的 JSON（对象或数组）
     *
     * @param rawResponse AI 原始响应
     * @return 提取后的纯 JSON 字符串，若无法提取则返回 null
     */
    public static String extractJson(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return null;
        }

        // 1. 直接尝试解析（最快路径）
        try {
            MAPPER.readTree(rawResponse);
            return rawResponse.trim();
        } catch (Exception ignored) {
            // 继续尝试提取
        }

        // 2. 去除 Markdown 代码块标记
        String cleaned = rawResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // 3. 正则提取 JSON 结构
        Matcher m = JSON_PATTERN.matcher(cleaned);
        if (m.find()) {
            String candidate = m.group();
            if (isValidJson(candidate)) {
                return candidate;
            }
        }

        // 4. 暴力括号匹配提取（适用于 AI 返回大量文本中嵌入的 JSON）
        int start = findJsonStart(cleaned);
        if (start >= 0) {
            String extracted = extractBracketBalanced(cleaned, start);
            if (extracted != null && isValidJson(extracted)) {
                return extracted;
            }
        }

        return null;
    }

    /**
     * 安全解析 JSON 字符串为指定类型
     */
    @SuppressWarnings("unused")
    public static <T> T parseJson(String rawResponse, Class<T> clazz) {
        String json = extractJson(rawResponse);
        if (json == null) {
            throw new IllegalArgumentException(
                    "无法从 AI 响应中提取 JSON。原始响应（前200字）："
                            + safeTruncate(rawResponse, 200));
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败：" + e.getMessage()
                    + "。提取后的 JSON（前200字）：" + safeTruncate(json, 200), e);
        }
    }

    /**
     * 安全解析 JSON 字符串为泛型类型
     */
    @SuppressWarnings("unused")
    public static <T> T parseJson(String rawResponse, TypeReference<T> typeRef) {
        String json = extractJson(rawResponse);
        if (json == null) {
            throw new IllegalArgumentException(
                    "无法从 AI 响应中提取 JSON。原始响应（前200字）："
                            + safeTruncate(rawResponse, 200));
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败：" + e.getMessage()
                    + "。提取后的 JSON（前200字）：" + safeTruncate(json, 200), e);
        }
    }

    /**
     * 简单验证字符串是否为有效 JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ════════════════════════════════════
    // 私有辅助方法
    // ════════════════════════════════════

    private static int findJsonStart(String text) {
        int idx = text.indexOf('{');
        int idxArr = text.indexOf('[');
        if (idx < 0 && idxArr < 0) return -1;
        if (idx < 0) return idxArr;
        if (idxArr < 0) return idx;
        return Math.min(idx, idxArr);
    }

    /** 从指定位置开始，提取括号平衡的内容 */
    private static String extractBracketBalanced(String text, int startIdx) {
        char openChar = text.charAt(startIdx);
        char closeChar = (openChar == '{') ? '}' : ']';
        int depth = 0;
        int endIdx = -1;

        for (int i = startIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == openChar) depth++;
            else if (c == closeChar) depth--;
            if (depth == 0) {
                endIdx = i + 1;
                break;
            }
        }

        if (endIdx > startIdx) {
            return text.substring(startIdx, endIdx);
        }
        return null;
    }

    private static String safeTruncate(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
