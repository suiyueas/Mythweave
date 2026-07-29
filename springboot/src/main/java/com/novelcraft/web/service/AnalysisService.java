package com.novelcraft.web.service;

import com.novelcraft.web.dto.OrchestratorResponse;
import com.novelcraft.web.entity.NovelAnalysis;
import com.novelcraft.web.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;

    @Transactional
    public NovelAnalysis saveAnalysis(Long projectId, Long chapterId, String chapterTitle,
                                      Integer chapterIndex, OrchestratorResponse response) {
        NovelAnalysis analysis = new NovelAnalysis();
        analysis.setProjectId(projectId);
        analysis.setChapterId(chapterId);
        analysis.setChapterTitle(chapterTitle);
        analysis.setChapterIndex(chapterIndex);
        analysis.setEditorResult(response.getEditorResult() != null ? response.getEditorResult().getContent() : null);
        analysis.setCharacterResult(response.getCharacterResult() != null ? response.getCharacterResult().getContent() : null);
        analysis.setStyleResult(response.getStyleResult() != null ? response.getStyleResult().getContent() : null);
        analysis.setReaderResult(response.getReaderResult() != null ? response.getReaderResult().getContent() : null);
        analysis.setSummary(response.getSummary());
        analysis.setTotalCostMs(response.getTotalCostMs());

        analysisRepository.insert(analysis);
        log.info("保存综合分析记录, id={}, projectId={}, chapter={}", analysis.getId(), projectId, chapterTitle);
        return analysis;
    }

    public List<NovelAnalysis> getAnalysisHistory(Long projectId) {
        return analysisRepository.findByProjectIdOrderByCreateTimeDesc(projectId);
    }

    public NovelAnalysis getAnalysisById(Long id) {
        return analysisRepository.selectById(id);
    }

    public List<NovelAnalysis> getChapterAnalysis(Long projectId, Long chapterId) {
        return analysisRepository.findByProjectIdAndChapterIdOrderByCreateTimeDesc(projectId, chapterId);
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        analysisRepository.deleteById(id);
        log.info("删除综合分析记录, id={}", id);
    }

    @Transactional
    public void deleteProjectAnalysis(Long projectId) {
        analysisRepository.deleteByProjectId(projectId);
        log.info("删除项目所有分析记录, projectId={}", projectId);
    }
}