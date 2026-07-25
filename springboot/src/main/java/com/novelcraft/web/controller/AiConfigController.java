package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelAiConfig;
import com.novelcraft.web.mapper.NovelAiConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI策略配置")
@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AiConfigController {
    private final NovelAiConfigMapper mapper;

    @Operation(summary = "获取AI配置")
    @GetMapping("/config")
    public R<NovelAiConfig> get(@PathVariable Long projectId) {
        NovelAiConfig config = mapper.selectByProjectId(projectId);
        if (config == null) {
            config = new NovelAiConfig();
            config.setProjectId(projectId);
            config.setTemperature(0.7);
            config.setTopP(0.9);
            config.setMaxTokens(4096);
            config.setStylePreset("默认");
            mapper.insert(config);
        }
        return R.ok(config);
    }

    @PutMapping("/config")
    public R<NovelAiConfig> update(@PathVariable Long projectId, @RequestBody NovelAiConfig config) {
        NovelAiConfig exist = mapper.selectByProjectId(projectId);
        if (exist != null) {
            config.setId(exist.getId());
            mapper.updateById(config);
        } else {
            config.setProjectId(projectId);
            mapper.insert(config);
        }
        return R.ok(mapper.selectByProjectId(projectId));
    }
}
