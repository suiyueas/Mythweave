package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.dto.OrchestratorRequest;
import com.novelcraft.web.dto.OrchestratorResponse;
import com.novelcraft.web.service.AgentOrchestratorService;
import com.novelcraft.web.service.AnalysisService;
import com.novelcraft.web.entity.NovelAnalysis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 多Agent协作控制器
 */
@Slf4j
@Tag(name = "多Agent协作")
@RestController
@RequestMapping("/api/projects/{projectId}/agent")
@RequiredArgsConstructor
public class AgentOrchestratorController {

    private final AgentOrchestratorService orchestratorService;
    private final AnalysisService analysisService;

    @Operation(summary = "多Agent协作分析", description = "并行调用编辑、人物、风格、读者4个Agent，生成综合分析报告并自动保存")
    @PostMapping("/orchestrate")
    public R<OrchestratorResponse> orchestrate(@PathVariable Long projectId,
                                               @RequestBody OrchestratorRequest request) {
        log.info("📨 收到多Agent协作请求, projectId={}, chapterTitle={}",
                projectId, request.getChapterTitle());

        try {
            OrchestratorResponse response = orchestratorService.orchestrate(projectId, request);

            if (response.isSuccess()) {
                analysisService.saveAnalysis(projectId, null, request.getChapterTitle(),
                        request.getChapterIndex(), response);
                log.info("✅ 多Agent协作成功并保存, 总耗时={}ms", response.getTotalCostMs());
                return R.ok(response);
            } else {
                log.error("❌ 多Agent协作失败: {}", response.getErrorMessage());
                return R.fail(response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("❌ 多Agent协作异常: {}", e.getMessage(), e);
            return R.fail("服务异常: " + e.getMessage());
        }
    }
}