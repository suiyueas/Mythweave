package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.service.NovelSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 先导式小说创作系统 — 设定生成控制器
 */
@Slf4j
@Tag(name = "AI先导创作")
@RestController
@RequestMapping("/api/projects/{projectId}/ai/setup")
@RequiredArgsConstructor
public class NovelSetupController {

    private final NovelSetupService setupService;

    /**
     * 触发全套设定生成
     */
    @Operation(summary = "触发AI全套设定生成")
    @PostMapping("/generate")
    public R<Map<String, Object>> generateSetup(@PathVariable Long projectId,
                                                 @RequestBody Map<String, Object> params) {
        try {
            String taskId = setupService.generateFullSetup(projectId, params);
            return R.ok(Map.of(
                "taskId", taskId,
                "estimatedSeconds", 180,
                "message", "AI 设定生成已启动，请通过 /status 接口查询进度"
            ));
        } catch (Exception e) {
            log.error("触发设定生成失败", e);
            return R.fail("生成启动失败: " + e.getMessage());
        }
    }

    /**
     * 查询生成进度
     */
    @Operation(summary = "查询设定生成进度")
    @GetMapping("/status")
    public R<Map<String, Object>> getStatus(@PathVariable Long projectId,
                                             @RequestParam String taskId) {
        NovelSetupService.GenerationTask task = setupService.getTaskStatus(taskId);
        if (task == null) {
            return R.fail("任务不存在或已过期");
        }

        Map<String, Object> result = Map.of(
            "status", task.status,
            "progress", task.progress,
            "steps", task.steps,
            "error", task.error != null ? task.error : ""
        );
        return R.ok(result);
    }

    /**
     * 获取完整设定数据
     */
    @Operation(summary = "获取完整AI设定")
    @GetMapping("")
    public R<Map<String, Object>> getSetup(@PathVariable Long projectId) {
        try {
            Map<String, Object> setup = setupService.getFullSetup(projectId);
            return R.ok(setup);
        } catch (Exception e) {
            log.error("获取设定失败", e);
            return R.fail("获取设定失败: " + e.getMessage());
        }
    }

    /**
     * 单模块重新生成
     */
    @Operation(summary = "单模块重新生成")
    @PostMapping("/regenerate")
    public R<Map<String, Object>> regenerate(@PathVariable Long projectId,
                                              @RequestBody Map<String, String> body) {
        try {
            String module = body.getOrDefault("module", "");
            String feedback = body.getOrDefault("feedback", "");

            // 重新触发生成（带上反馈调整）
            String taskId = setupService.generateFullSetup(projectId, Map.of(
                "title", body.getOrDefault("title", ""),
                "genre", body.getOrDefault("genre", ""),
                "inspiration", body.getOrDefault("inspiration", ""),
                "style", body.getOrDefault("style", ""),
                "regenerate", module,
                "feedback", feedback
            ));

            return R.ok(Map.of(
                "taskId", taskId,
                "module", module,
                "message", module + " 模块正在重新生成，反馈已采纳"
            ));
        } catch (Exception e) {
            log.error("重新生成失败", e);
            return R.fail("重新生成失败: " + e.getMessage());
        }
    }

    // ════════════════════════════════════
    // 分步引导式 API（新增）
    // ════════════════════════════════════

    /**
     * 步骤1：生成世界观
     */
    @Operation(summary = "分步-生成世界观")
    @PostMapping("/step/world")
    public R<Map<String, Object>> stepWorld(@PathVariable Long projectId,
                                             @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = setupService.generateWorld(projectId, params);
            return R.ok(result);
        } catch (Exception e) {
            log.error("世界观生成失败", e);
            return R.fail("世界观生成失败: " + e.getMessage());
        }
    }

    /**
     * 步骤2：生成人物群像
     */
    @Operation(summary = "分步-生成人物群像")
    @PostMapping("/step/characters")
    public R<Map<String, Object>> stepCharacters(@PathVariable Long projectId,
                                                  @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = setupService.generateCharacters(projectId, params);
            return R.ok(result);
        } catch (Exception e) {
            log.error("人物生成失败", e);
            return R.fail("人物生成失败: " + e.getMessage());
        }
    }

    /**
     * 步骤3：生成大纲结构
     */
    @Operation(summary = "分步-生成大纲结构")
    @PostMapping("/step/outline")
    public R<Map<String, Object>> stepOutline(@PathVariable Long projectId,
                                               @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = setupService.generateOutline(projectId, params);
            return R.ok(result);
        } catch (Exception e) {
            log.error("大纲生成失败", e);
            return R.fail("大纲生成失败: " + e.getMessage());
        }
    }

    /**
     * 步骤4：生成情节引擎
     */
    @Operation(summary = "分步-生成情节引擎")
    @PostMapping("/step/plot")
    public R<Map<String, Object>> stepPlot(@PathVariable Long projectId,
                                            @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = setupService.generatePlot(projectId, params);
            return R.ok(result);
        } catch (Exception e) {
            log.error("情节生成失败", e);
            return R.fail("情节生成失败: " + e.getMessage());
        }
    }

    /**
     * 步骤5：生成灵感素材
     */
    @Operation(summary = "分步-生成灵感素材")
    @PostMapping("/step/inspirations")
    public R<Map<String, Object>> stepInspirations(@PathVariable Long projectId,
                                                    @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = setupService.generateInspirations(projectId, params);
            return R.ok(result);
        } catch (Exception e) {
            log.error("灵感生成失败", e);
            return R.fail("灵感生成失败: " + e.getMessage());
        }
    }
}
