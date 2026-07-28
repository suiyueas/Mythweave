package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.dto.AppendForeshadowRequest;
import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelChapterVersion;
import com.novelcraft.web.entity.NovelVolume;
import com.novelcraft.web.service.ChapterService;
import com.novelcraft.web.service.ForeshadowAppendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "章节管理")
@RestController
@RequestMapping("/api/projects/{projectId}/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final ForeshadowAppendService foreshadowAppendService;

    // ── 分卷 ──

    @Operation(summary = "获取分卷列表")
    @GetMapping("/volumes")
    public R<List<NovelVolume>> listVolumes(@PathVariable Long projectId) {
        return R.ok(chapterService.listVolumes(projectId));
    }

    @Operation(summary = "创建分卷")
    @PostMapping("/volumes")
    public R<NovelVolume> createVolume(@PathVariable Long projectId, @RequestBody NovelVolume volume) {
        volume.setProjectId(projectId);
        return R.ok(chapterService.createVolume(volume));
    }

    // ── 章节 ──

    @Operation(summary = "获取章节列表")
    @GetMapping
    public R<List<NovelChapter>> listChapters(@PathVariable Long projectId) {
        return R.ok(chapterService.listChapters(projectId));
    }

    @Operation(summary = "获取章节详情")
    @GetMapping("/{chapterId}")
    public R<NovelChapter> getChapter(@PathVariable Long projectId, @PathVariable Long chapterId) {
        return R.ok(chapterService.getChapter(chapterId));
    }

    @Operation(summary = "创建章节")
    @PostMapping
    public R<NovelChapter> createChapter(@PathVariable Long projectId, @RequestBody NovelChapter chapter) {
        chapter.setProjectId(projectId);
        return R.ok(chapterService.createChapter(chapter));
    }

    @Operation(summary = "更新章节")
    @PutMapping("/{chapterId}")
    public R<NovelChapter> updateChapter(@PathVariable Long projectId, @PathVariable Long chapterId,
                                          @RequestBody NovelChapter chapter,
                                          @RequestParam(defaultValue = "false") boolean silent) {
        chapter.setId(chapterId);
        return R.ok(chapterService.updateChapter(chapter, silent));
    }

    @Operation(summary = "删除章节")
    @DeleteMapping("/{chapterId}")
    public R<Void> deleteChapter(@PathVariable Long projectId, @PathVariable Long chapterId) {
        chapterService.deleteChapter(chapterId);
        return R.ok();
    }

    /**
     * 修复项目所有章节的 sortOrder（按 id 重新分配连续序号）
     */
    @Operation(summary = "修复章节排序")
    @PutMapping("/fix-sort")
    public R<Integer> fixSortOrder(@PathVariable Long projectId) {
        int fixed = chapterService.fixSortOrder(projectId);
        return R.ok(fixed);
    }

    // ── 版本 ──

    @Operation(summary = "获取章节版本历史")
    @GetMapping("/{chapterId}/versions")
    public R<List<NovelChapterVersion>> listVersions(@PathVariable Long projectId, @PathVariable Long chapterId) {
        return R.ok(chapterService.listVersions(chapterId));
    }

    // ── 伏笔追加补写 ──

    @Operation(summary = "伏笔追加补写")
    @PostMapping("/{chapterId}/append-foreshadow")
    public R<Map<String, Object>> appendForeshadow(@PathVariable Long projectId,
                                                    @PathVariable Long chapterId,
                                                    @RequestBody AppendForeshadowRequest request) {
        request.setProjectId(projectId);
        Map<String, Object> result = foreshadowAppendService.appendForeshadowing(request);
        return R.ok(result);
    }
}