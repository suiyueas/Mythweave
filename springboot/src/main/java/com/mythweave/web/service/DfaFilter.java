package com.mythweave.web.service;

import java.util.*;

/**
 * DFA（确定有限自动态机）敏感词过滤器
 * 所有敏感词构建为 Trie 树，输入文本 O(n) 扫描即可命中全部匹配，时间复杂度与词库大小无关
 * 支持白名单：白名单中的词不会被命中（如"杀青"中的"杀"）
 */
public class DfaFilter {

    private final Map<Character, TrieNode> roots = new HashMap<>();
    private final Set<String> whitelist = new HashSet<>();
    private volatile boolean ready = false;

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        String word = null; // 非 null 表示此处为一个完整词的结尾
    }

    public static DfaFilter build(List<String> words, List<String> whitelistWords) {
        DfaFilter filter = new DfaFilter();
        if (whitelistWords != null) {
            for (String w : whitelistWords) {
                if (w != null && !w.isBlank()) {
                    filter.whitelist.add(w.toLowerCase());
                }
            }
        }
        if (words != null) {
            for (String word : words) {
                if (word == null || word.isBlank()) continue;
                String lower = word.toLowerCase().trim();
                if (lower.isEmpty()) continue;
                filter.addWord(lower);
            }
        }
        filter.ready = true;
        return filter;
    }

    private void addWord(String word) {
        Map<Character, TrieNode> current = roots;
        for (char c : word.toCharArray()) {
            current = current.computeIfAbsent(c, k -> new TrieNode()).children;
        }
        current.put('\0', new TrieNode()); // 终止符标记词尾
        current.get('\0').word = word;
    }

    /**
     * 扫描文本，返回所有命中的敏感词（去重）
     */
    public List<String> scan(String text) {
        if (!ready || text == null || text.isBlank()) return List.of();

        String lower = text.toLowerCase();
        Set<String> hits = new LinkedHashSet<>();

        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = roots.get(lower.charAt(i));
            if (node == null) continue;

            TrieNode current = node;
            for (int j = i + 1; j < lower.length(); j++) {
                TrieNode next = current.children.get(lower.charAt(j));
                if (next == null) break;
                current = next;

                TrieNode terminator = current.children.get('\0');
                if (terminator != null && terminator.word != null) {
                    String matched = terminator.word;
                    // 白名单跳过
                    if (!whitelist.contains(matched)) {
                        hits.add(matched);
                    }
                }
            }
        }

        return new ArrayList<>(hits);
    }

    /**
     * 快速检查：文本中是否包含任何敏感词（比 scan 快，找到第一个即返回）
     */
    public boolean containsAny(String text) {
        if (!ready || text == null || text.isBlank()) return false;

        String lower = text.toLowerCase();

        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = roots.get(lower.charAt(i));
            if (node == null) continue;

            TrieNode current = node;
            for (int j = i + 1; j < lower.length(); j++) {
                TrieNode next = current.children.get(lower.charAt(j));
                if (next == null) break;
                current = next;

                TrieNode terminator = current.children.get('\0');
                if (terminator != null && terminator.word != null
                        && !whitelist.contains(terminator.word)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isReady() {
        return ready;
    }

    public int size() {
        return roots.size();
    }
}
