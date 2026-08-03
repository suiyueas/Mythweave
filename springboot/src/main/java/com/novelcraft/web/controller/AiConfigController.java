package com.novelcraft.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelAiConfig;
import com.novelcraft.web.entity.NovelAiPreset;
import com.novelcraft.web.entity.NovelAiUsage;
import com.novelcraft.web.mapper.NovelAiConfigMapper;
import com.novelcraft.web.mapper.NovelAiPresetMapper;
import com.novelcraft.web.mapper.NovelAiUsageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI策略配置")
@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AiConfigController {
    private final NovelAiConfigMapper configMapper;
    private final NovelAiPresetMapper presetMapper;
    private final NovelAiUsageMapper usageMapper;

    @Operation(summary = "获取AI配置")
    @GetMapping("/config")
    public R<NovelAiConfig> get(@PathVariable Long projectId) {
        NovelAiConfig config = configMapper.selectByProjectId(projectId);
        if (config == null) {
            config = new NovelAiConfig();
            config.setProjectId(projectId);
            config.setTemperature(0.7);
            config.setTopP(0.9);
            config.setMaxTokens(4096);
            config.setStylePreset("默认");
            configMapper.insert(config);
        }
        return R.ok(config);
    }

    @Operation(summary = "更新AI配置")
    @PutMapping("/config")
    public R<NovelAiConfig> update(@PathVariable Long projectId, @RequestBody NovelAiConfig config) {
        NovelAiConfig exist = configMapper.selectByProjectId(projectId);
        if (exist != null) {
            config.setId(exist.getId());
            configMapper.updateById(config);
        } else {
            config.setProjectId(projectId);
            configMapper.insert(config);
        }
        return R.ok(configMapper.selectByProjectId(projectId));
    }

    @Operation(summary = "获取AI用量统计")
    @GetMapping("/usage")
    public R<NovelAiUsage> getUsage(@PathVariable Long projectId) {
        NovelAiUsage usage = usageMapper.selectByProjectId(projectId);
        if (usage == null) {
            usage = new NovelAiUsage();
            usage.setProjectId(projectId);
            usage.setTotalTokens(0L);
            usage.setEstimatedCost(0.0);
            usage.setApiCalls(0);
            usage.setCacheHitRate(0);
            usageMapper.insert(usage);
        }
        return R.ok(usage);
    }

    @Operation(summary = "获取写作风格预设列表")
    @GetMapping("/presets")
    public R<List<NovelAiPreset>> getPresets(@PathVariable Long projectId) {
        List<NovelAiPreset> presets = presetMapper.selectList(
            new LambdaQueryWrapper<NovelAiPreset>()
                .eq(NovelAiPreset::getProjectId, projectId)
                .eq(NovelAiPreset::getDeleted, 0)
                .orderByAsc(NovelAiPreset::getIsDefault)
        );
        if (presets.isEmpty()) {
            presets = getDefaultPresets(projectId);
        }
        return R.ok(presets);
    }

    @Operation(summary = "创建写作风格预设")
    @PostMapping("/presets")
    public R<NovelAiPreset> createPreset(@PathVariable Long projectId, @RequestBody NovelAiPreset preset) {
        preset.setProjectId(projectId);
        preset.setIsDefault(false);
        presetMapper.insert(preset);
        return R.ok(preset);
    }

    @Operation(summary = "更新写作风格预设")
    @PutMapping("/presets/{presetId}")
    public R<NovelAiPreset> updatePreset(
            @PathVariable Long projectId,
            @PathVariable Long presetId,
            @RequestBody NovelAiPreset preset) {
        preset.setId(presetId);
        preset.setProjectId(projectId);
        presetMapper.updateById(preset);
        return R.ok(presetMapper.selectById(presetId));
    }

    @Operation(summary = "删除写作风格预设")
    @DeleteMapping("/presets/{presetId}")
    public R<Void> deletePreset(@PathVariable Long projectId, @PathVariable Long presetId) {
        NovelAiPreset preset = presetMapper.selectById(presetId);
        if (preset != null && preset.getIsDefault()) {
            return R.fail("默认预设不能删除");
        }
        presetMapper.deleteById(presetId);
        return R.ok();
    }

    private List<NovelAiPreset> getDefaultPresets(Long projectId) {
        NovelAiPreset default1 = new NovelAiPreset();
        default1.setProjectId(projectId);
        default1.setName("默认风格");
        default1.setDescription("平衡型 · 适合大多数场景");
        default1.setTemperature(0.7);
        default1.setTopP(0.9);
        default1.setMaxTokens(4096);
        default1.setIsDefault(true);
        presetMapper.insert(default1);

        NovelAiPreset fast = new NovelAiPreset();
        fast.setProjectId(projectId);
        fast.setName("网文快节奏");
        fast.setDescription("短段落 · 高密度冲突 · 快速推进");
        fast.setTemperature(0.85);
        fast.setTopP(0.95);
        fast.setMaxTokens(2048);
        fast.setIsDefault(false);
        presetMapper.insert(fast);

        NovelAiPreset literary = new NovelAiPreset();
        literary.setProjectId(projectId);
        literary.setName("文艺细腻");
        literary.setDescription("长描写 · 心理刻画深入 · 意境营造");
        literary.setTemperature(0.6);
        literary.setTopP(0.85);
        literary.setMaxTokens(4096);
        literary.setIsDefault(false);
        presetMapper.insert(literary);

        NovelAiPreset suspense = new NovelAiPreset();
        suspense.setProjectId(projectId);
        suspense.setName("悬疑紧张");
        suspense.setDescription("悬念铺垫 · 节奏紧凑 · 情节反转");
        suspense.setTemperature(0.75);
        suspense.setTopP(0.9);
        suspense.setMaxTokens(3072);
        suspense.setIsDefault(false);
        presetMapper.insert(suspense);

        return presetMapper.selectList(
            new LambdaQueryWrapper<NovelAiPreset>()
                .eq(NovelAiPreset::getProjectId, projectId)
                .eq(NovelAiPreset::getDeleted, 0)
                .orderByAsc(NovelAiPreset::getIsDefault)
        );
    }
}