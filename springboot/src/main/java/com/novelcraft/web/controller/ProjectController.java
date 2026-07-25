package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelProject;
import com.novelcraft.web.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "作品项目管理")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "创建作品")
    @PostMapping
    public R<NovelProject> create(@RequestAttribute("userId") Long userId, @RequestBody NovelProject project) {
        project.setUserId(userId);
        return R.ok(projectService.create(project));
    }

    @Operation(summary = "更新作品")
    @PutMapping("/{id}")
    public R<NovelProject> update(@RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody NovelProject project) {
        project.setId(id);
        project.setUserId(userId);
        return R.ok(projectService.update(project));
    }

    @Operation(summary = "删除作品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    @Operation(summary = "获取作品详情")
    @GetMapping("/{id}")
    public R<NovelProject> getById(@PathVariable Long id) {
        return R.ok(projectService.getById(id));
    }

    @Operation(summary = "获取用户作品列表")
    @GetMapping
    public R<List<NovelProject>> list(@RequestAttribute("userId") Long userId) {
        return R.ok(projectService.listByUserId(userId));
    }

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