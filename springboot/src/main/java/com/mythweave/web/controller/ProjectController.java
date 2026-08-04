package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.entity.NovelProject;
import com.mythweave.web.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 作品项目管理控制器
 * 
 * 主要功能：
 * - 作品的创建、查询、更新、删除（CRUD）操作
 * - 用户作品列表管理
 * - 作品统计数据同步
 * 
 * 所有接口都需要用户登录认证，用户ID通过请求属性获取
 */
@Slf4j
@Tag(name = "作品项目管理")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 创建新作品
     * @param userId 当前登录用户ID（从请求属性中获取）
     * @param project 作品信息（包含标题、类型、描述等）
     * @return 创建成功的作品对象
     */
    @Operation(summary = "创建作品")
    @PostMapping
    public R<NovelProject> create(@RequestAttribute("userId") Long userId, @Valid @RequestBody NovelProject project) {
        project.setUserId(userId);
        return R.ok(projectService.create(project));
    }

    /**
     * 更新作品信息
     * @param userId 当前登录用户ID
     * @param id 作品ID
     * @param project 更新后的作品信息
     * @return 更新后的作品对象
     */
    @Operation(summary = "更新作品")
    @PutMapping("/{id}")
    public R<NovelProject> update(@RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody NovelProject project) {
        project.setId(id);
        project.setUserId(userId);
        return R.ok(projectService.update(project));
    }

    /**
     * 删除作品（包含关联数据的级联删除）
     * @param userId 当前登录用户ID
     * @param id 要删除的作品ID
     * @return 操作结果
     */
    @Operation(summary = "删除作品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    /**
     * 获取作品详情
     * @param userId 当前登录用户ID
     * @param id 作品ID
     * @return 作品详细信息
     */
    @Operation(summary = "获取作品详情")
    @GetMapping("/{id}")
    public R<NovelProject> getById(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        NovelProject project = projectService.getById(id);
        if (!project.getUserId().equals(userId)) {
            throw new com.mythweave.web.common.BusinessException(403, "无权访问该作品");
        }
        return R.ok(project);
    }

    /**
     * 获取当前用户的所有作品列表
     * @param userId 当前登录用户ID
     * @return 用户作品列表
     */
    @Operation(summary = "获取用户作品列表")
    @GetMapping
    public R<List<NovelProject>> list(@RequestAttribute("userId") Long userId) {
        return R.ok(projectService.listByUserId(userId));
    }

    /**
     * 同步用户所有作品的统计数据
     * 用于修复因历史操作导致的作品统计数据（章节数、字数等）不一致问题
     * @param userId 当前登录用户ID
     * @return 被同步统计的作品数量
     */
    @Operation(summary = "同步用户所有作品的统计（修复历史数据不一致）")
    @PostMapping("/_sync-stats")
    public R<Integer> syncStats(@RequestAttribute("userId") Long userId) {
        try {
            int count = projectService.syncAllProjectStats(userId);
            return R.ok(count);
        } catch (Exception e) {
            log.warn("统计同步失败(projectId={})：{}", userId, e.getMessage());
            return R.ok(0);
        }
    }
}