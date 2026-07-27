package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.*;
import com.novelcraft.web.mapper.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@Slf4j
@Tag(name = "情节引擎")
@RestController
@RequestMapping("/api/projects/{projectId}/plot")
@RequiredArgsConstructor
public class PlotController {
    private final NovelPlotThreadMapper threadMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final NovelPlotKnowledgeGraphMapper kgMapper;

    @GetMapping("/threads") public R<List<NovelPlotThread>> listThreads(@PathVariable Long projectId) { return R.ok(threadMapper.selectByProjectId(projectId)); }
    @PostMapping("/threads") public R<NovelPlotThread> createThread(@PathVariable Long projectId, @RequestBody NovelPlotThread t) { t.setProjectId(projectId); threadMapper.insert(t); return R.ok(t); }
    @PutMapping("/threads/{id}") public R<NovelPlotThread> updateThread(@PathVariable Long id, @RequestBody NovelPlotThread t) { t.setId(id); threadMapper.updateById(t); return R.ok(threadMapper.selectById(id)); }
    @DeleteMapping("/threads/{id}") public R<Void> deleteThread(@PathVariable Long id) { threadMapper.deleteById(id); return R.ok(); }

    @GetMapping("/foreshadowing") public R<List<NovelForeshadowing>> listForeshadowing(@PathVariable Long projectId) { return R.ok(foreshadowingMapper.selectByProjectId(projectId)); }
    @GetMapping("/foreshadowing/urgent") public R<List<NovelForeshadowing>> listUrgentForeshadowing(@PathVariable Long projectId, @RequestParam(defaultValue = "999") Integer currentChapter) { return R.ok(foreshadowingMapper.selectUrgentByProject(projectId, currentChapter)); }
    @PostMapping("/foreshadowing") public R<NovelForeshadowing> createForeshadowing(@PathVariable Long projectId, @RequestBody NovelForeshadowing f) { f.setProjectId(projectId); foreshadowingMapper.insert(f); return R.ok(f); }
    @PutMapping("/foreshadowing/{id}") public R<NovelForeshadowing> updateForeshadowing(@PathVariable Long id, @RequestBody NovelForeshadowing f) { f.setId(id); foreshadowingMapper.updateById(f); return R.ok(foreshadowingMapper.selectById(id)); }

    @GetMapping("/kg") public R<List<NovelPlotKnowledgeGraph>> listKG(@PathVariable Long projectId) {
        try {
            List<NovelPlotKnowledgeGraph> list = kgMapper.selectByProjectId(projectId);
            return R.ok(list != null ? list : Collections.emptyList());
        } catch (Exception e) {
            log.warn("KG查询失败(projectId={}): {}", projectId, e.getMessage());
            return R.ok(Collections.emptyList());
        }
    }
    @PostMapping("/kg") public R<NovelPlotKnowledgeGraph> createKG(@PathVariable Long projectId, @RequestBody NovelPlotKnowledgeGraph k) { k.setProjectId(projectId); kgMapper.insert(k); return R.ok(k); }
}