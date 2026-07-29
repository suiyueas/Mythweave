package com.novelcraft.web.controller;

import com.novelcraft.web.dto.AnalysisDTO;
import com.novelcraft.web.dto.OrchestratorRequest;
import com.novelcraft.web.dto.OrchestratorResponse;
import com.novelcraft.web.entity.NovelAnalysis;
import com.novelcraft.web.service.AgentOrchestratorService;
import com.novelcraft.web.service.AnalysisService;
import com.novelcraft.web.utils.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/projects/{projectId}/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AgentOrchestratorService agentOrchestratorService;

    @PostMapping("/orchestrate")
    public R<AnalysisDTO> orchestrateAndSave(@PathVariable Long projectId,
                                             @RequestBody OrchestratorRequest request) {
        log.info("📨 收到综合分析请求, projectId={}, chapterTitle={}",
                projectId, request.getChapterTitle());

        try {
            OrchestratorResponse response = agentOrchestratorService.orchestrate(projectId, request);

            NovelAnalysis saved = analysisService.saveAnalysis(
                    projectId,
                    null,
                    request.getChapterTitle(),
                    request.getChapterIndex(),
                    response
            );

            AnalysisDTO dto = convertToDTO(saved);
            dto.setSummary(response.getSummary());

            log.info("✅ 综合分析完成并保存, id={}", saved.getId());
            return R.ok(dto);
        } catch (Exception e) {
            log.error("❌ 综合分析失败: {}", e.getMessage(), e);
            return R.fail("分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public R<List<AnalysisDTO>> getHistory(@PathVariable Long projectId) {
        log.info("📋 获取分析历史, projectId={}", projectId);
        List<AnalysisDTO> history = analysisService.getAnalysisHistory(projectId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return R.ok(history);
    }

    @GetMapping("/{id}")
    public R<AnalysisDTO> getById(@PathVariable Long projectId, @PathVariable Long id) {
        log.info("🔍 获取分析详情, id={}", id);
        return analysisService.getAnalysisById(id)
                .map(this::convertToDTO)
                .map(R::ok)
                .orElse(R.fail("分析记录不存在"));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        log.info("🗑️ 删除分析记录, id={}", id);
        analysisService.deleteAnalysis(id);
        return R.ok();
    }

    private AnalysisDTO convertToDTO(NovelAnalysis analysis) {
        AnalysisDTO dto = new AnalysisDTO();
        dto.setId(analysis.getId());
        dto.setProjectId(analysis.getProjectId());
        dto.setChapterId(analysis.getChapterId());
        dto.setChapterTitle(analysis.getChapterTitle());
        dto.setChapterIndex(analysis.getChapterIndex());
        dto.setEditorResult(analysis.getEditorResult());
        dto.setCharacterResult(analysis.getCharacterResult());
        dto.setStyleResult(analysis.getStyleResult());
        dto.setReaderResult(analysis.getReaderResult());
        dto.setSummary(analysis.getSummary());
        dto.setTotalCostMs(analysis.getTotalCostMs());
        dto.setCreateTime(analysis.getCreateTime());
        return dto;
    }
}