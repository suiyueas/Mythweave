package com.mythweave.web.service;

import com.mythweave.web.mapper.GlobalSearchMapper;
import com.mythweave.web.model.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final GlobalSearchMapper globalSearchMapper;

    /**
     * 全局搜索
     * 
     * @param projectId 项目ID
     * @param keyword   搜索关键词（支持空格分隔的多关键词）
     * @return 搜索结果列表
     */
    public List<SearchResultDTO> globalSearch(Long projectId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 处理多关键词（空格分隔）
        String[] keywords = keyword.trim().split("\\s+");
        List<SearchResultDTO> allResults = new ArrayList<>();

        for (String kw : keywords) {
            if (kw.isEmpty()) continue;
            
            List<SearchResultDTO> results = searchByKeyword(projectId, kw);
            allResults.addAll(results);
        }

        // 去重（基于id和type）
        List<SearchResultDTO> uniqueResults = allResults.stream()
                .collect(Collectors.toMap(
                        r -> r.getId() + "_" + r.getType(),
                        r -> r,
                        (existing, replacement) -> {
                            // 保留优先级更高的结果
                            return existing.getPriority() <= replacement.getPriority() ? existing : replacement;
                        }
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        // 按优先级排序，然后按更新时间降序
        uniqueResults.sort(Comparator
                .<SearchResultDTO, Integer>comparing(r -> r.getPriority())
                .thenComparing(Comparator.comparing((SearchResultDTO r) -> r.getUpdateTime()).reversed()));

        // 限制返回数量
        if (uniqueResults.size() > 50) {
            uniqueResults = uniqueResults.subList(0, 50);
        }

        return uniqueResults;
    }

    /**
     * 根据单个关键词搜索
     */
    private List<SearchResultDTO> searchByKeyword(Long projectId, String keyword) {
        List<SearchResultDTO> results = new ArrayList<>();

        // 搜索章节
        try {
            List<SearchResultDTO> chapters = globalSearchMapper.searchChapters(projectId, keyword);
            results.addAll(chapters);
        } catch (Exception e) {
            log.warn("搜索章节失败: {}", e.getMessage());
        }

        // 搜索人物
        try {
            List<SearchResultDTO> characters = globalSearchMapper.searchCharacters(projectId, keyword);
            results.addAll(characters);
        } catch (Exception e) {
            log.warn("搜索人物失败: {}", e.getMessage());
        }

        // 搜索世界观
        try {
            List<SearchResultDTO> worlds = globalSearchMapper.searchWorldSettings(projectId, keyword);
            results.addAll(worlds);
        } catch (Exception e) {
            log.warn("搜索世界观失败: {}", e.getMessage());
        }

        // 搜索大纲
        try {
            List<SearchResultDTO> outlines = globalSearchMapper.searchOutlines(projectId, keyword);
            results.addAll(outlines);
        } catch (Exception e) {
            log.warn("搜索大纲失败: {}", e.getMessage());
        }

        // 搜索情节线
        try {
            List<SearchResultDTO> plots = globalSearchMapper.searchPlotThreads(projectId, keyword);
            results.addAll(plots);
        } catch (Exception e) {
            log.warn("搜索情节线失败: {}", e.getMessage());
        }

        // 搜索灵感
        try {
            List<SearchResultDTO> inspirations = globalSearchMapper.searchInspirations(projectId, keyword);
            results.addAll(inspirations);
        } catch (Exception e) {
            log.warn("搜索灵感失败: {}", e.getMessage());
        }

        // 搜索伏笔
        try {
            List<SearchResultDTO> foreshadowings = globalSearchMapper.searchForeshadowings(projectId, keyword);
            results.addAll(foreshadowings);
        } catch (Exception e) {
            log.warn("搜索伏笔失败: {}", e.getMessage());
        }

        // 生成摘要片段
        for (SearchResultDTO result : results) {
            generateSnippet(result, keyword);
        }

        return results;
    }

    /**
     * 生成搜索结果摘要
     */
    private void generateSnippet(SearchResultDTO result, String keyword) {
        String content = result.getDescription();
        if (content == null || content.isEmpty()) {
            result.setSnippet("");
            return;
        }

        // 如果内容较短，直接返回
        if (content.length() <= 200) {
            result.setSnippet(content);
            return;
        }

        // 查找关键词位置
        int keywordIndex = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (keywordIndex == -1) {
            // 如果关键词不在描述中，返回前200字符
            result.setSnippet(content.substring(0, 200) + "...");
            return;
        }

        // 截取关键词前后各100字符
        int start = Math.max(0, keywordIndex - 100);
        int end = Math.min(content.length(), keywordIndex + keyword.length() + 100);
        
        String snippet = "";
        if (start > 0) {
            snippet += "...";
        }
        snippet += content.substring(start, end);
        if (end < content.length()) {
            snippet += "...";
        }
        
        result.setSnippet(snippet);
    }
}