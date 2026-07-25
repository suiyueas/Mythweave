package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelInspiration;
import com.novelcraft.web.mapper.NovelInspirationMapper;
import com.novelcraft.web.service.InspirationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "素材灵感库")
@RestController
@RequestMapping("/api/projects/{projectId}/inspirations")
@RequiredArgsConstructor
public class InspirationController {
    private final NovelInspirationMapper mapper;
    private final InspirationService inspirationService;

    @GetMapping public R<List<NovelInspiration>> list(@PathVariable Long projectId) { return R.ok(mapper.selectByProjectId(projectId)); }
    @PostMapping public R<NovelInspiration> create(@PathVariable Long projectId, @RequestBody NovelInspiration i) { i.setProjectId(projectId); mapper.insert(i); return R.ok(i); }
    @PutMapping("/{id}") public R<NovelInspiration> update(@PathVariable Long id, @RequestBody NovelInspiration i) { i.setId(id); mapper.updateById(i); return R.ok(mapper.selectById(id)); }
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id) { mapper.deleteById(id); return R.ok(); }

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
