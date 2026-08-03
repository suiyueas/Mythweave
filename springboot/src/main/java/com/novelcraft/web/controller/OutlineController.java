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

/**
 * 大纲管理控制器（故事结构）
 * 
 * 主要功能：
 * - 大纲的创建、查询、更新、删除（CRUD）操作
 * - 幕与节点管理（第一幕/第二幕/第三幕）
 * - 大纲节点批量操作（排序、状态更新、删除）
 * - AI生成大纲的批量保存
 * - 幕区分布修复
 * 
 * 支持拖拽排序和跨幕拖拽功能
 * 所有接口都需要用户登录认证
 */
@Tag(name = "故事结构")
@RestController
@RequestMapping("/api/projects/{projectId}/outline")
@RequiredArgsConstructor
public class OutlineController {
    
    private final NovelOutlineMapper mapper;
    private final OutlineService outlineService;

    /**
     * 获取作品的大纲列表（包含所有幕和节点）
     * @param projectId 作品ID
     * @return 大纲节点列表（按sortOrder排序）
     */
    @Operation(summary = "获取大纲树")
    @GetMapping
    public R<List<NovelOutline>> list(@PathVariable Long projectId) {
        return R.ok(mapper.selectByProjectId(projectId));
    }

    /**
     * 批量保存幕与节点（用于AI生成的大纲整体保存）
     * 
     * 支持任意数量的幕和节点，一次性保存到数据库
     * 通常在AI生成完整大纲后调用此接口批量保存
     * 
     * @param projectId 作品ID
     * @param acts 幕列表（每个幕包含多个节点）
     * @return 保存结果（包含幕数量和节点数量）
     */
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

    /**
     * 创建大纲节点
     * @param projectId 作品ID
     * @param node 节点信息
     * @return 创建的节点对象
     */
    @Operation(summary = "创建大纲节点")
    @PostMapping
    public R<NovelOutline> create(@PathVariable Long projectId, @RequestBody NovelOutline node) {
        node.setProjectId(projectId);
        mapper.insert(node);
        return R.ok(node);
    }

    /**
     * 更新大纲节点
     * @param projectId 作品ID
     * @param id 节点ID
     * @param node 更新后的节点信息
     * @return 更新后的节点对象
     */
    @Operation(summary = "更新大纲节点")
    @PutMapping("/{id}")
    public R<NovelOutline> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody NovelOutline node) {
        node.setId(id);
        mapper.updateById(node);
        return R.ok(mapper.selectById(id));
    }

    /**
     * 删除大纲节点
     * @param projectId 作品ID
     * @param id 节点ID
     * @return 操作结果
     */
    @Operation(summary = "删除大纲节点")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        mapper.deleteById(id);
        return R.ok();
    }

    /**
     * 批量更新节点排序（用于拖拽排序落地）
     * 
     * @param projectId 作品ID
     * @param items 排序项列表（包含节点ID和新的sortOrder）
     * @return 操作结果
     */
    @Operation(summary = "批量更新排序（拖拽落地）")
    @PutMapping("/batch-sort")
    public R<Void> batchSort(@PathVariable Long projectId, @RequestBody List<BatchSortItem> items) {
        for (BatchSortItem item : items) {
            item.setProjectId(projectId);
        }
        mapper.updateBatchSort(items);
        return R.ok();
    }

    /**
     * 批量更新节点排序和幕归属（用于跨幕拖拽）
     * 
     * @param projectId 作品ID
     * @param items 排序项列表（包含节点ID、新的sortOrder和act）
     * @return 操作结果
     */
    @Operation(summary = "批量更新排序和幕归属（跨幕拖拽）")
    @PutMapping("/batch-sort-act")
    public R<Void> batchSortAct(@PathVariable Long projectId, @RequestBody List<BatchSortActItem> items) {
        for (BatchSortActItem item : items) {
            item.setProjectId(projectId);
        }
        mapper.updateBatchSortAndAct(items);
        return R.ok();
    }

    /**
     * 批量更新节点状态
     * 
     * @param projectId 作品ID
     * @param body 包含ids（节点ID列表）和status（新状态）
     * @return 操作结果
     */
    @Operation(summary = "批量更新状态")
    @PutMapping("/batch-status")
    public R<Void> batchStatus(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        List<Long> ids = convertIds(body.get("ids"));
        String status = (String) body.get("status");
        mapper.updateBatchStatus(projectId, ids, status);
        return R.ok();
    }

    /**
     * 批量删除大纲节点
     * 
     * @param projectId 作品ID
     * @param body 包含ids（要删除的节点ID列表）
     * @return 操作结果
     */
    @Operation(summary = "批量删除")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        List<Long> ids = convertIds(body.get("ids"));
        mapper.deleteBatch(projectId, ids);
        return R.ok();
    }

    /**
     * 修复幕区分布
     * 
     * 将所有节点按sortOrder重新均匀分配到三幕
     * 用于在大量节点sortOrder混乱时重新整理幕区分布
     * 
     * @param projectId 作品ID
     * @return 修复结果（包含受影响节点数量）
     */
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