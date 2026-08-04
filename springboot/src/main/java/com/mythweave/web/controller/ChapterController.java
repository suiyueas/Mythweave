package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.dto.AppendForeshadowRequest;
import com.mythweave.web.entity.NovelChapter;
import com.mythweave.web.entity.NovelChapterVersion;
import com.mythweave.web.entity.NovelProject;
import com.mythweave.web.service.ChapterService;
import com.mythweave.web.service.ForeshadowAppendService;
import com.mythweave.web.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 章节管理控制器
 * 
 * 主要功能：
 * - 章节的创建、查询、更新、删除（CRUD）操作
 * - 章节版本历史管理（查看版本、恢复旧版本）
 * - 章节排序修复
 * - 伏笔追加补写功能（AI辅助）
 * 
 * 所有接口都需要用户登录认证，且操作的作品必须属于当前用户
 */
@Tag(name = "章节管理")
@RestController
@RequestMapping("/api/projects/{projectId}/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final ForeshadowAppendService foreshadowAppendService;
    private final ProjectService projectService;

    // ── 章节 CRUD ──

    /**
     * 获取指定作品的章节列表
     * @param projectId 作品ID
     * @return 章节列表（按sortOrder排序）
     */
    @Operation(summary = "获取章节列表")
    @GetMapping
    public R<List<NovelChapter>> listChapters(@PathVariable Long projectId) {
        return R.ok(chapterService.listChapters(projectId));
    }

    /**
     * 获取章节详细信息
     * @param userId 当前登录用户ID
     * @param projectId 作品ID
     * @param chapterId 章节ID
     * @return 章节详情（包含正文内容）
     */
    @Operation(summary = "获取章节详情")
    @GetMapping("/{chapterId}")
    public R<NovelChapter> getChapter(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long projectId, @PathVariable Long chapterId) {
        NovelChapter chapter = chapterService.getChapter(chapterId);
        if (!chapter.getProjectId().equals(projectId)) {
            throw new com.mythweave.web.common.BusinessException(403, "章节不属于该作品");
        }
        NovelProject project = projectService.getById(projectId);
        if (!project.getUserId().equals(userId)) {
            throw new com.mythweave.web.common.BusinessException(403, "无权访问该作品");
        }
        return R.ok(chapter);
    }

    /**
     * 创建新章节
     * @param projectId 作品ID
     * @param chapter 章节信息（标题、状态等）
     * @return 创建的章节对象
     */
    @Operation(summary = "创建章节")
    @PostMapping
    public R<NovelChapter> createChapter(@PathVariable Long projectId, @Valid @RequestBody NovelChapter chapter) {
        chapter.setProjectId(projectId);
        return R.ok(chapterService.createChapter(chapter));
    }

    /**
     * 更新章节内容
     * @param projectId 作品ID
     * @param chapterId 章节ID
     * @param chapter 更新后的章节信息
     * @param silent 静默更新标记（true时不触发AI分析和写作统计更新）
     * @return 更新后的章节对象
     */
    @Operation(summary = "更新章节")
    @PutMapping("/{chapterId}")
    public R<NovelChapter> updateChapter(@PathVariable Long projectId, @PathVariable Long chapterId,
                                          @RequestBody NovelChapter chapter,
                                          @RequestParam(defaultValue = "false") boolean silent) {
        chapter.setId(chapterId);
        return R.ok(chapterService.updateChapter(chapter, silent));
    }

    /**
     * 删除章节（逻辑删除）
     * @param projectId 作品ID
     * @param chapterId 章节ID
     * @return 操作结果
     */
    @Operation(summary = "删除章节")
    @DeleteMapping("/{chapterId}")
    public R<Void> deleteChapter(@PathVariable Long projectId, @PathVariable Long chapterId) {
        chapterService.deleteChapter(chapterId);
        return R.ok();
    }

    /**
     * 修复章节排序
     * 
     * 当章节的sortOrder出现断裂或不连续时，按ID重新分配连续的序号
     * 用于修复手动调整顺序导致的问题
     * 
     * @param projectId 作品ID
     * @return 被修复的章节数量
     */
    @Operation(summary = "修复章节排序")
    @PutMapping("/fix-sort")
    public R<Integer> fixSortOrder(@PathVariable Long projectId) {
        int fixed = chapterService.fixSortOrder(projectId);
        return R.ok(fixed);
    }

    // ── 版本历史 ──

    /**
     * 获取章节的版本历史列表
     * @param userId 当前登录用户ID
     * @param projectId 作品ID
     * @param chapterId 章节ID
     * @return 版本列表（包含版本号、创建时间等信息，不包含正文）
     */
    @Operation(summary = "获取章节版本历史")
    @GetMapping("/{chapterId}/versions")
    public R<List<NovelChapterVersion>> listVersions(@RequestAttribute("userId") Long userId,
                                                      @PathVariable Long projectId, @PathVariable Long chapterId) {
        validateProjectOwnership(userId, projectId);
        return R.ok(chapterService.listVersions(chapterId));
    }

    /**
     * 获取章节特定版本的详细信息（包含正文）
     * 用于版本预览或恢复到指定版本
     *
     * @param userId 当前登录用户ID
     * @param projectId 作品ID
     * @param chapterId 章节ID
     * @param versionId 版本ID
     * @return 版本详情（包含完整正文内容）
     */
    @Operation(summary = "获取章节版本详情（含正文，用于预览/恢复）")
    @GetMapping("/{chapterId}/versions/{versionId}")
    public R<NovelChapterVersion> getVersionDetail(@RequestAttribute("userId") Long userId,
                                                    @PathVariable Long projectId, @PathVariable Long chapterId,
                                                    @PathVariable Long versionId) {
        validateProjectOwnership(userId, projectId);
        return R.ok(chapterService.getVersionDetail(chapterId, versionId));
    }

    private void validateProjectOwnership(Long userId, Long projectId) {
        NovelProject project = projectService.getById(projectId);
        if (!project.getUserId().equals(userId)) {
            throw new com.mythweave.web.common.BusinessException(403, "无权访问该作品");
        }
    }

    // ── 伏笔追加补写 ──

    /**
     * 伏笔追加补写
     * 
     * AI功能：根据已有的伏笔设置，在章节末尾追加符合伏笔发展的内容
     * 用于在写完章节后发现需要呼应之前的伏笔时的辅助创作
     * 
     * @param projectId 作品ID
     * @param chapterId 章节ID
     * @param request 补写请求（包含伏笔ID、期望长度等参数）
     * @return AI生成的内容（可能包含新追加的伏笔内容）
     */
    @Operation(summary = "伏笔追加补写")
    @PostMapping("/{chapterId}/append-foreshadow")
    public R<Map<String, Object>> appendForeshadow(@PathVariable Long projectId,
                                                    @PathVariable Long chapterId,
                                                    @RequestBody AppendForeshadowRequest request) {
        request.setProjectId(projectId);
        request.setChapterId(chapterId);
        Map<String, Object> result = foreshadowAppendService.appendForeshadowing(request);
        return R.ok(result);
    }
}