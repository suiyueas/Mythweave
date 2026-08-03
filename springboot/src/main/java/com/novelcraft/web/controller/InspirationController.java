package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelInspiration;
import com.novelcraft.web.mapper.NovelInspirationMapper;
import com.novelcraft.web.service.InspirationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 灵感管理控制器（素材灵感库）
 * 
 * 主要功能：
 * - 创作灵感的CRUD管理
 * - AI灵感生成（根据关键词生成创作灵感）
 * - 灵感分类、标签、高亮标记管理
 * - 灵感使用状态追踪
 * 
 * 灵感池帮助作者收集和管理创作过程中的随机创意
 * 支持AI辅助生成灵感内容
 * 
 * 所有接口都需要用户登录认证
 */
@Tag(name = "素材灵感库")
@RestController
@RequestMapping("/api/projects/{projectId}/inspirations")
@RequiredArgsConstructor
public class InspirationController {
    
    private final NovelInspirationMapper mapper;
    private final InspirationService inspirationService;

    /** 获取作品的所有灵感列表 */
    @GetMapping public R<List<NovelInspiration>> list(@PathVariable Long projectId) { return R.ok(mapper.selectByProjectId(projectId)); }
    
    /** 创建新灵感 */
    @PostMapping public R<NovelInspiration> create(@PathVariable Long projectId, @Valid @RequestBody NovelInspiration i) { i.setProjectId(projectId); mapper.insert(i); return R.ok(i); }
    
    /** 更新灵感信息 */
    @PutMapping("/{id}") public R<NovelInspiration> update(@PathVariable Long id, @RequestBody NovelInspiration i) { i.setId(id); mapper.updateById(i); return R.ok(mapper.selectById(id)); }
    
    /** 删除灵感 */
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id) { mapper.deleteById(id); return R.ok(); }

    /**
     * AI生成灵感
     * 
     * 根据用户提供的关键词，使用AI生成相关的创作灵感
     * 可以指定灵感类型、标签等
     * 
     * @param projectId 作品ID
     * @param body 包含keywords（关键词）等参数
     * @return AI生成的灵感列表
     */
    @PostMapping("/ai-generate")
    public R<List<InspirationService.InspirationItem>> aiGenerate(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        String keywords = body.getOrDefault("keywords", "");
        if (keywords.isBlank()) {
            return R.badRequest("关键词不能为空");
        }
        return R.ok(inspirationService.aiGenerate(keywords));
    }
}