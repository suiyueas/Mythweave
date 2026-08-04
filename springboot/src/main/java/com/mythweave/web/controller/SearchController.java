package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.model.SearchResultDTO;
import com.mythweave.web.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 全局搜索控制器
 */
@Tag(name = "全局搜索")
@RestController
@RequestMapping("/api/projects/{projectId}/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 全局搜索
     * 
     * @param projectId 项目ID
     * @param keyword   搜索关键词（支持空格分隔的多关键词）
     * @return 搜索结果列表
     */
    @Operation(summary = "全局搜索")
    @GetMapping
    public R<List<SearchResultDTO>> globalSearch(
            @PathVariable Long projectId,
            @RequestParam String keyword) {
        return R.ok(searchService.globalSearch(projectId, keyword));
    }

    /**
     * 快速搜索（返回前10条结果，用于搜索框下拉建议）
     * 
     * @param projectId 项目ID
     * @param keyword   搜索关键词
     * @return 搜索结果列表（最多10条）
     */
    @Operation(summary = "快速搜索")
    @GetMapping("/quick")
    public R<List<SearchResultDTO>> quickSearch(
            @PathVariable Long projectId,
            @RequestParam String keyword) {
        List<SearchResultDTO> results = searchService.globalSearch(projectId, keyword);
        if (results.size() > 10) {
            results = results.subList(0, 10);
        }
        return R.ok(results);
    }
}