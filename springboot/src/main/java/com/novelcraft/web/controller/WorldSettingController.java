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

@Tag(name = "世界观构筑")
@RestController
@RequestMapping("/api/projects/{projectId}/world")
@RequiredArgsConstructor
public class WorldSettingController {

    private final WorldSettingService worldSettingService;

    @Operation(summary = "设定列表")
    @GetMapping("/settings")
    public R<List<NovelWorldSetting>> listSettings(@PathVariable Long projectId) {
        return R.ok(worldSettingService.listByProjectId(projectId));
    }

    @Operation(summary = "创建设定")
    @PostMapping("/settings")
    public R<NovelWorldSetting> createSetting(@PathVariable Long projectId, @Valid @RequestBody NovelWorldSetting s) {
        return R.ok(worldSettingService.create(projectId, s));
    }

    @Operation(summary = "更新设定")
    @PutMapping("/settings/{id}")
    public R<NovelWorldSetting> updateSetting(@PathVariable Long id, @RequestBody NovelWorldSetting s) {
        return R.ok(worldSettingService.update(id, s));
    }

    @Operation(summary = "删除设定")
    @DeleteMapping("/settings/{id}")
    public R<Void> deleteSetting(@PathVariable Long id) {
        worldSettingService.delete(id);
        return R.ok();
    }
}
