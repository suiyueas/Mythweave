package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelWorldSetting;
import com.novelcraft.web.service.WorldSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 世界观设定管理控制器（世界观构筑）
 * 
 * 主要功能：
 * - 世界观设定的创建、查询、更新、删除（CRUD）操作
 * - 支持层级结构设定（通过parentId构建父子关系）
 * - 设定分类管理（地理、历史、文化等）
 * - 设定关联关系管理
 * 
 * 所有接口都需要用户登录认证，且操作的设定必须属于当前用户的作品
 */
@Tag(name = "世界观构筑")
@RestController
@RequestMapping("/api/projects/{projectId}/world")
@RequiredArgsConstructor
public class WorldSettingController {

    private final WorldSettingService worldSettingService;

    /**
     * 获取作品的所有世界观设定列表
     * @param projectId 作品ID
     * @return 设定列表（包含层级结构信息）
     */
    @Operation(summary = "设定列表")
    @GetMapping("/settings")
    public R<List<NovelWorldSetting>> listSettings(@PathVariable Long projectId) {
        return R.ok(worldSettingService.listByProjectId(projectId));
    }

    /**
     * 创建新的世界观设定
     * @param projectId 作品ID
     * @param s 设定信息（包含名称、分类、内容、层级等）
     * @return 创建的设定对象
     */
    @Operation(summary = "创建设定")
    @PostMapping("/settings")
    public R<NovelWorldSetting> createSetting(@PathVariable Long projectId, @Valid @RequestBody NovelWorldSetting s) {
        return R.ok(worldSettingService.create(projectId, s));
    }

    /**
     * 更新世界观设定
     * @param id 设定ID
     * @param s 更新后的设定信息
     * @return 更新后的设定对象
     */
    @Operation(summary = "更新设定")
    @PutMapping("/settings/{id}")
    public R<NovelWorldSetting> updateSetting(@PathVariable Long id, @RequestBody NovelWorldSetting s) {
        return R.ok(worldSettingService.update(id, s));
    }

    /**
     * 删除世界观设定
     * @param id 设定ID
     * @return 操作结果
     */
    @Operation(summary = "删除设定")
    @DeleteMapping("/settings/{id}")
    public R<Void> deleteSetting(@PathVariable Long id) {
        worldSettingService.delete(id);
        return R.ok();
    }
}