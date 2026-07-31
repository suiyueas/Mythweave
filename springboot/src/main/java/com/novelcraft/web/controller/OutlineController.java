package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.*;
import com.novelcraft.web.mapper.*;
import com.novelcraft.web.model.BatchSortActItem;
import com.novelcraft.web.model.BatchSortItem;
import com.novelcraft.web.model.OutlineActDto;
import com.novelcraft.web.service.OutlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Tag(name = "故事结构")
@RestController
@RequestMapping("/api/projects/{projectId}/outline")
@RequiredArgsConstructor
public class OutlineController {
    private final NovelOutlineMapper mapper;
    private final OutlineService outlineService;

    @Operation(summary = "获取大纲树")
    @GetMapping
    public R<List<NovelOutline>> list(@PathVariable Long projectId) {
        return R.ok(mapper.selectByProjectId(projectId));
    }

    @Operation(summary = "批量保存幕与节点（AI 生成的大纲整体保存，支持任意数量的幕）")
    @PostMapping("/acts")
    public R<Map<String, Object>> saveActs(@PathVariable Long projectId, @RequestBody List<OutlineActDto> acts) {
        if (acts == null || acts.isEmpty()) {
            return R.fail("acts 不能为空");
        }
        int savedNodes = outlineService.saveOutlineActs(projectId, acts);
        return R.ok(Map.of(
            "actCount", acts.size(),
            "savedNodes", savedNodes
        ));
    }

    @Operation(summary = "创建大纲节点")
    @PostMapping
    public R<NovelOutline> create(@PathVariable Long projectId, @RequestBody NovelOutline node) {
        node.setProjectId(projectId);
        mapper.insert(node);
        return R.ok(node);
    }

    @Operation(summary = "更新大纲节点")
    @PutMapping("/{id}")
    public R<NovelOutline> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody NovelOutline node) {
        node.setId(id);
        mapper.updateById(node);
        return R.ok(mapper.selectById(id));
    }

    @Operation(summary = "删除大纲节点")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        mapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "批量更新排序（拖拽落地）")
    @PutMapping("/batch-sort")
    public R<Void> batchSort(@PathVariable Long projectId, @RequestBody List<BatchSortItem> items) {
        for (BatchSortItem item : items) {
            item.setProjectId(projectId);
        }
        mapper.updateBatchSort(items);
        return R.ok();
    }

    @Operation(summary = "批量更新排序和幕归属（跨幕拖拽）")
    @PutMapping("/batch-sort-act")
    public R<Void> batchSortAct(@PathVariable Long projectId, @RequestBody List<BatchSortActItem> items) {
        for (BatchSortActItem item : items) {
            item.setProjectId(projectId);
        }
        mapper.updateBatchSortAndAct(items);
        return R.ok();
    }

    @Operation(summary = "批量更新状态")
    @PutMapping("/batch-status")
    public R<Void> batchStatus(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        List<Long> ids = convertIds(body.get("ids"));
        String status = (String) body.get("status");
        mapper.updateBatchStatus(projectId, ids, status);
        return R.ok();
    }

    @Operation(summary = "批量删除")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        List<Long> ids = convertIds(body.get("ids"));
        mapper.deleteBatch(projectId, ids);
        return R.ok();
    }

    @Operation(summary = "修复幕区分布（将所有节点按 sort_order 重新均匀分配到三幕）")
    @PostMapping("/fix-act-distribution")
    public R<Map<String, Object>> fixActDistribution(@PathVariable Long projectId) {
        int affected = outlineService.fixActDistribution(projectId);
        return R.ok(Map.of(
            "message", "幕区分布已修复",
            "affectedNodes", affected
        ));
    }

    /** 安全转换 ids 参数，兼容 Integer/Long/String */
    @SuppressWarnings("unchecked")
    private List<Long> convertIds(Object raw) {
        if (raw == null) return new ArrayList<>();
        List<Object> list = (List<Object>) raw;
        return list.stream().map(id -> {
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return Long.parseLong((String) id);
            throw new IllegalArgumentException("无法转换 ID 类型: " + id.getClass());
        }).collect(Collectors.toList());
    }
}