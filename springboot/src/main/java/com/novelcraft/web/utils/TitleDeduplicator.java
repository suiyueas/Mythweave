package com.novelcraft.web.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 标题去重工具 — 提取核心意象词、检测重复标题
 */
public class TitleDeduplicator {

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "在", "和", "与", "之", "其", "而", "以", "于",
            "我", "你", "他", "她", "它", "们", "这", "那", "有", "无", "不",
            "一", "个", "中", "上", "下", "里", "后", "前", "时", "到", "从"
    );

    /**
     * 从多个标题中提取所有核心意象词（用于 Prompt 禁用列表）
     */
    public static Set<String> extractAllKeywords(List<String> titles) {
        Set<String> all = new HashSet<>();
        if (titles == null) return all;
        for (String title : titles) {
            all.addAll(extractKeywords(title));
        }
        return all;
    }

    /**
     * 提取单个标题中的核心意象词（2-4字实词片段）
     */
    public static Set<String> extractKeywords(String title) {
        Set<String> keywords = new HashSet<>();
        if (title == null) return keywords;
        String cleaned = title.replaceAll("[^\u4e00-\u9fa5]", "");
        if (cleaned.isEmpty()) return keywords;

        // 提取 2-4 字连续片段作为候选关键词
        for (int i = 0; i < cleaned.length(); i++) {
            for (int j = i + 2; j <= Math.min(i + 4, cleaned.length()); j++) {
                String token = cleaned.substring(i, j);
                if (!STOP_WORDS.contains(token) && !token.matches(".*[a-zA-Z0-9].*")) {
                    keywords.add(token);
                }
            }
        }
        return keywords;
    }

    /**
     * 检查新标题是否与已有标题重复（核心词重叠 ≥ 2 个即视为重复）
     */
    public static boolean isDuplicate(String newTitle, List<String> existingTitles) {
        if (newTitle == null || existingTitles == null || existingTitles.isEmpty()) return false;
        Set<String> newKw = extractKeywords(newTitle);
        for (String existing : existingTitles) {
            Set<String> existKw = extractKeywords(existing);
            long overlap = newKw.stream().filter(existKw::contains).count();
            if (overlap >= 2) return true;
        }
        return false;
    }
}
