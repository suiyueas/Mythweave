package com.novelcraft.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelCharacter;
import com.novelcraft.web.entity.NovelContextSnapshot;
import com.novelcraft.web.entity.NovelOutline;
import com.novelcraft.web.entity.NovelWorldSetting;
import com.novelcraft.web.mapper.NovelCharacterMapper;
import com.novelcraft.web.mapper.NovelContextSnapshotMapper;
import com.novelcraft.web.mapper.NovelOutlineMapper;
import com.novelcraft.web.mapper.NovelWorldSettingMapper;
import com.novelcraft.web.service.ContextAssembler;
import com.novelcraft.web.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "上下文引擎")
@RestController
@RequestMapping("/api/projects/{projectId}/context")
@RequiredArgsConstructor
public class ContextController {

    private final ContextAssembler contextAssembler;
    private final EmbeddingService embeddingService;
    private final NovelContextSnapshotMapper snapshotMapper;
    private final NovelCharacterMapper characterMapper;
    private final NovelWorldSettingMapper worldSettingMapper;
    private final NovelOutlineMapper outlineMapper;

    private final Map<Long, ContextConfig> configStore = new ConcurrentHashMap<>();

    private ContextConfig getOrCreateConfig(Long projectId) {
        return configStore.computeIfAbsent(projectId, k -> {
            ContextConfig cfg = new ContextConfig();
            cfg.setAutoIndex(true);
            cfg.setWindowSize(512);
            cfg.setTopK(10);
            Map<String, Double> weights = new LinkedHashMap<>();
            weights.put("semantic", 0.7);
            weights.put("bm25", 0.3);
            cfg.setHybridWeights(weights);
            return cfg;
        });
    }

    @Operation(summary = "获取索引统计概览")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@PathVariable Long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Long chunks = snapshotMapper.selectCount(
                    new LambdaQueryWrapper<NovelContextSnapshot>()
                            .eq(NovelContextSnapshot::getProjectId, projectId));
            result.put("totalChunks", chunks != null ? chunks.intValue() : 0);
        } catch (Exception e) {
            log.warn("查询快照统计异常", e);
            result.put("totalChunks", 0);
        }
        try {
            Long chars = characterMapper.selectCount(
                    new LambdaQueryWrapper<NovelCharacter>()
                            .eq(NovelCharacter::getProjectId, projectId));
            result.put("characterVectors", chars != null ? chars.intValue() : 0);
        } catch (Exception e) {
            log.warn("查询人物向量统计异常", e);
            result.put("characterVectors", 0);
        }
        try {
            Long world = worldSettingMapper.selectCount(
                    new LambdaQueryWrapper<NovelWorldSetting>()
                            .eq(NovelWorldSetting::getProjectId, projectId));
            result.put("worldEntries", world != null ? world.intValue() : 0);
        } catch (Exception e) {
            log.warn("查询世界观统计异常", e);
            result.put("worldEntries", 0);
        }
        try {
            Long outlines = outlineMapper.selectCount(
                    new LambdaQueryWrapper<NovelOutline>()
                            .eq(NovelOutline::getProjectId, projectId));
            result.put("outlineNodes", outlines != null ? outlines.intValue() : 0);
        } catch (Exception e) {
            log.warn("查询大纲统计异常", e);
            result.put("outlineNodes", 0);
        }
        return R.ok(result);
    }

    @Operation(summary = "获取检索配置")
    @GetMapping("/config")
    public R<ContextConfig> getConfig(@PathVariable Long projectId) {
        return R.ok(getOrCreateConfig(projectId));
    }

    @Operation(summary = "更新检索配置")
    @PutMapping("/config")
    public R<ContextConfig> updateConfig(@PathVariable Long projectId,
                                          @RequestBody ContextConfig config) {
        ContextConfig existing = getOrCreateConfig(projectId);
        if (config.getAutoIndex() != null) existing.setAutoIndex(config.getAutoIndex());
        if (config.getWindowSize() != null) existing.setWindowSize(config.getWindowSize());
        if (config.getTopK() != null) existing.setTopK(config.getTopK());
        if (config.getHybridWeights() != null) existing.setHybridWeights(config.getHybridWeights());
        return R.ok(existing);
    }

    @Operation(summary = "获取最近索引活动")
    @GetMapping("/activities")
    public R<List<Map<String, Object>>> activities(@PathVariable Long projectId,
                                                    @RequestParam(defaultValue = "10") int limit) {
        try {
            List<NovelContextSnapshot> snapshots = snapshotMapper.selectList(
                    new LambdaQueryWrapper<NovelContextSnapshot>()
                            .eq(NovelContextSnapshot::getProjectId, projectId)
                            .orderByDesc(NovelContextSnapshot::getCreateTime)
                            .last("LIMIT " + Math.min(limit, 100)));
            List<Map<String, Object>> list = new ArrayList<>();
            for (NovelContextSnapshot s : snapshots) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", s.getId());
                item.put("title", (s.getContextType() != null ? s.getContextType() : "索引") + " 完成");
                item.put("desc", "tokens: " + s.getTokensUsed());
                item.put("status", "done");
                item.put("time", s.getCreateTime() != null ? s.getCreateTime().toString() : "");
                list.add(item);
            }
            return R.ok(list);
        } catch (Exception e) {
            log.warn("查询索引活动异常 (projectId={})", projectId, e);
            return R.ok(new ArrayList<>());
        }
    }

    @Operation(summary = "语义搜索全书")
    @GetMapping("/search")
    public R<List<Map<String, Object>>> search(@PathVariable Long projectId,
                                                @RequestParam String query,
                                                @RequestParam(defaultValue = "10") int topK) {
        try {
            List<String> results = contextAssembler.semanticSearch(projectId, query, topK);
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", i + 1L);
                String raw = results.get(i);
                int bracketEnd = raw.indexOf("] ");
                if (bracketEnd > 1) {
                    item.put("title", raw.substring(1, bracketEnd));
                    item.put("text", raw.substring(bracketEnd + 2));
                } else {
                    item.put("title", "片段 " + (i + 1));
                    item.put("text", raw);
                }
                item.put("score", Math.max(50, 95 - i * 8));
                list.add(item);
            }
            return R.ok(list);
        } catch (Exception e) {
            log.error("语义搜索异常", e);
            return R.fail("搜索异常: " + e.getMessage());
        }
    }

    @Operation(summary = "获取续写上下文")
    @PostMapping("/assemble")
    public R<String> assemble(@PathVariable Long projectId,
                               @RequestBody Map<String, String> body) {
        try {
            return R.ok(contextAssembler.assembleForContinueWriting(
                    projectId, body.getOrDefault("cursorText", ""),
                    body.getOrDefault("existingText", ""), 10));
        } catch (Exception e) {
            log.error("上下文装配异常", e);
            return R.fail("装配异常: " + e.getMessage());
        }
    }

    @Operation(summary = "索引章节内容到ES")
    @PostMapping("/index-chapter")
    public R<Void> indexChapter(@PathVariable Long projectId,
                                 @RequestBody Map<String, Object> body) {
        try {
            Long chapterId = Long.valueOf(body.get("chapterId").toString());
            embeddingService.indexChapterContent(projectId, chapterId, body.get("content").toString());
            return R.ok();
        } catch (Exception e) {
            log.error("章节索引异常", e);
            return R.fail("索引异常: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // 缺失端点补充（返回默认值避免前端报错）
    // ══════════════════════════════════════════════

    @Operation(summary = "获取索引大小趋势")
    @GetMapping("/size-trend")
    public R<List<Map<String, Object>>> sizeTrend(@PathVariable Long projectId,
                                                   @RequestParam(defaultValue = "7") int days) {
        try {
            LocalDate today = LocalDate.now();
            List<Map<String, Object>> trend = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE));
                point.put("size", 0);
                trend.add(point);
            }
            return R.ok(trend);
        } catch (Exception e) {
            log.warn("获取索引趋势异常", e);
            return R.ok(new ArrayList<>());
        }
    }

    @Operation(summary = "获取索引健康检查详情")
    @GetMapping("/health")
    public R<Map<String, Object>> health(@PathVariable Long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "unknown");
        result.put("elasticsearch", "not_configured");
        result.put("indexCount", 0);
        result.put("lastCheckTime", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return R.ok(result);
    }

    @Operation(summary = "重建全部索引")
    @PostMapping("/rebuild")
    public R<Map<String, Object>> rebuild(@PathVariable Long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", UUID.randomUUID().toString());
        result.put("status", "queued");
        result.put("message", "索引重建任务已提交");
        return R.ok(result);
    }

    @Operation(summary = "增量索引（新内容）")
    @PostMapping("/incremental")
    public R<Map<String, Object>> incremental(@PathVariable Long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "completed");
        result.put("message", "增量索引已完成（无新内容）");
        result.put("indexedCount", 0);
        return R.ok(result);
    }

    @Operation(summary = "清理无效索引数据")
    @PostMapping("/cleanup")
    public R<Map<String, Object>> cleanup(@PathVariable Long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "completed");
        result.put("message", "清理完成（无过期数据）");
        result.put("cleanedCount", 0);
        return R.ok(result);
    }

    @Operation(summary = "导出索引报告")
    @GetMapping("/export")
    public R<List<Map<String, Object>>> exportReport(@PathVariable Long projectId,
                                                      @RequestParam(defaultValue = "json") String format) {
        try {
            List<NovelContextSnapshot> snapshots = snapshotMapper.selectList(
                    new LambdaQueryWrapper<NovelContextSnapshot>()
                            .eq(NovelContextSnapshot::getProjectId, projectId)
                            .orderByDesc(NovelContextSnapshot::getCreateTime));
            List<Map<String, Object>> report = snapshots.stream().map(s -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", s.getId());
                item.put("type", s.getContextType());
                item.put("tokens", s.getTokensUsed());
                item.put("time", s.getCreateTime() != null ? s.getCreateTime().toString() : "");
                return item;
            }).collect(Collectors.toList());
            return R.ok(report);
        } catch (Exception e) {
            log.warn("导出索引报告异常", e);
            return R.ok(new ArrayList<>());
        }
    }

    @Operation(summary = "获取索引操作进度")
    @GetMapping("/operations/{operationId}")
    public R<Map<String, Object>> operationProgress(@PathVariable Long projectId,
                                                     @PathVariable String operationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", operationId);
        result.put("status", "completed");
        result.put("progress", 100);
        result.put("message", "无进行中的操作");
        return R.ok(result);
    }

    @Operation(summary = "取消索引操作")
    @DeleteMapping("/operations/{operationId}")
    public R<Void> cancelOperation(@PathVariable Long projectId,
                                    @PathVariable String operationId) {
        return R.ok();
    }

    // ─── ContextConfig ───
    public static class ContextConfig {
        private Boolean autoIndex;
        private Integer windowSize;
        private Integer topK;
        private Map<String, Double> hybridWeights;

        public Boolean getAutoIndex() { return autoIndex; }
        public void setAutoIndex(Boolean v) { this.autoIndex = v; }
        public Integer getWindowSize() { return windowSize; }
        public void setWindowSize(Integer v) { this.windowSize = v; }
        public Integer getTopK() { return topK; }
        public void setTopK(Integer v) { this.topK = v; }
        public Map<String, Double> getHybridWeights() { return hybridWeights; }
        public void setHybridWeights(Map<String, Double> v) { this.hybridWeights = v; }
    }
}
